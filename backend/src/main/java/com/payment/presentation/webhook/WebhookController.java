package com.payment.presentation.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.application.service.PaymentService;
import com.payment.domain.payment.model.Payment;
import com.payment.domain.payment.repository.PaymentRepository;
import com.payment.domain.paymentlog.model.PaymentLog;
import com.payment.domain.paymentlog.repository.PaymentLogRepository;
import com.payment.infrastructure.external.payment.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * WebhookController - Webhook 처리
 * 
 * 역할:
 * - 외부 결제 API(토스페이먼츠)에서 오는 Webhook 처리
 * - 결제 결과를 받아서 상태 업데이트
 * 
 * Webhook 흐름:
 * 1. 사용자가 결제 창에서 결제 완료
 * 2. 토스페이먼츠가 결제 결과를 우리 서버로 POST
 * 3. 우리 서버: 서명 검증 → 결제 상태 업데이트
 * 
 * 이유:
 * - 결제는 비동기 프로세스
 * - 클라이언트 네트워크 끊김 후에도 결제가 완료될 수 있음
 * - 토스페이먼츠가 최종 결과를 알려줄 때 반응해야 함
 * 
 * 보안:
 * - Webhook 서명 필수 검증 (위조 방지)
 * - X-TOSS-SIGNATURE 헤더로 요청 검증
 * - 요청 본문이 변조되지 않았는지 확인
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final PaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;
    
    /**
     * 토스페이먼츠 Webhook 엔드포인트
     * 
     * API: POST /webhooks/toss-payments
     * 헤더: X-TOSS-SIGNATURE (HMAC SHA256 서명)
     * 
     * 토스페이먼츠는 다음 상황에서 Webhook을 전송:
     * - 결제 승인 완료
     * - 결제 실패
     * - 결제 취소
     * - 정기 결제 등
     * 
     * 페이로드 예시:
     * {
     *   "eventType": "PAYMENT_APPROVED",
     *   "data": {
     *     "paymentKey": "toss_xxxxx",
     *     "orderId": "order_xxxxx",
     *     "amount": 10000,
     *     "status": "DONE"
     *   }
     * }
     */
    @PostMapping("/toss-payments")
    public ResponseEntity<Void> handleTossPaymentWebhook(
            @RequestHeader("X-TOSS-SIGNATURE") String signature,
            @RequestBody String payload) {
        
        try {
            log.info("Webhook 수신: signature={}", signature);
            
            // 1단계: 서명 검증 (매우 중요!)
            if (!paymentGateway.verifyWebhookSignature(payload, signature)) {
                log.warn("Webhook 서명 검증 실패: 위조된 요청으로 의심됨");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 2단계: 페이로드 파싱
            JsonNode webhookData = objectMapper.readTree(payload);
            String eventType = webhookData.get("eventType").asText();
            
            JsonNode data = webhookData.get("data");
            String externalPaymentId = data.get("paymentKey").asText();
            String status = data.get("status").asText();
            String message = data.has("message") ? data.get("message").asText() : "";
            
            // 3단계: 우리 시스템의 결제 조회
            Payment payment = paymentRepository.findByExternalPaymentId(externalPaymentId)
                .orElseGet(() -> {
                    // externalPaymentId로 찾지 못하면 orderId로 시도
                    String orderId = data.get("orderId").asText();
                    return paymentRepository.findByOrderId(orderId)
                        .orElse(null);
                });
            
            if (payment == null) {
                log.error("결제를 찾을 수 없음: externalPaymentId={}", externalPaymentId);
                // 결제를 찾을 수 없어도 200 OK 반환 (재전송 방지)
                return ResponseEntity.ok().build();
            }
            
            // 4단계: 이벤트 타입에 따라 상태 업데이트
            switch (eventType) {
                case "PAYMENT_APPROVED" -> {
                    payment.markAsSuccess();
                    
                    PaymentLog log = PaymentLog.createLogWithResponse(
                        payment.getId(),
                        payment.getPaymentKey(),
                        PaymentLog.PaymentLogEventType.WEBHOOK_RECEIVED,
                        "Webhook으로 결제 승인 확인",
                        "200",
                        "SUCCESS"
                    );
                    paymentLogRepository.save(log);
                    
                    log.info("Webhook: 결제 승인 - paymentKey={}", payment.getPaymentKey());
                }
                
                case "PAYMENT_FAILED" -> {
                    payment.markAsFail(message);
                    
                    PaymentLog failLog = PaymentLog.createLogWithResponse(
                        payment.getId(),
                        payment.getPaymentKey(),
                        PaymentLog.PaymentLogEventType.WEBHOOK_RECEIVED,
                        "Webhook으로 결제 실패 확인",
                        "FAIL",
                        message
                    );
                    paymentLogRepository.save(failLog);
                    
                    log.info("Webhook: 결제 실패 - paymentKey={}, reason={}", 
                             payment.getPaymentKey(), message);
                }
                
                case "PAYMENT_CANCELLED" -> {
                    log.info("Webhook: 결제 취소 - paymentKey={}", payment.getPaymentKey());
                    // 취소 처리 로직 (필요시 구현)
                }
                
                default -> {
                    log.info("Webhook: 미지원 이벤트 타입={}", eventType);
                }
            }
            
            // 데이터베이스에 저장
            paymentRepository.save(payment);
            
            // 5단계: 성공 응답 반환 (200 OK)
            // 토스페이먼츠가 200 응답을 받으면 동일한 Webhook을 재전송하지 않음
            return ResponseEntity.ok().build();
            
        } catch (IOException e) {
            log.error("Webhook 처리 중 JSON 파싱 오류", e);
            // JSON 파싱 오류셔도 200 OK 반환해서 무한 재전송 방지
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Webhook 처리 중 예상치 못한 오류", e);
            // 예외 발생 시에도 200 OK 반환 (토스페이먼츠는 5XX 응답 시 재전송)
            return ResponseEntity.ok().build();
        }
    }
}
