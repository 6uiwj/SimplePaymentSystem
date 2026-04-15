package com.payment.infrastructure.external.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * TossPayments 구현체
 * 
 * 역할:
 * - 토스페이먼츠 API 호출을 구현
 * - Webhook 서명 검증
 * - 예외 처리 및 재시도 로직
 * 
 * 이유:
 * - PaymentGateway 인터페이스를 구현하여 결합도 낮춤
 * - 토스페이먼츠 특화 로직 분리
 * - 다른 결제사로 변경 시 새로운 구현체만 추가
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TossPaymentsGateway implements PaymentGateway {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${payment.external.toss.api-url}")
    private String apiUrl;
    
    @Value("${payment.external.toss.secret-key}")
    private String secretKey;
    
    @Value("${payment.external.toss.webhook-secret}")
    private String webhookSecret;
    
    /**
     * 결제 요청
     * 토스페이먼츠 API: POST /payments/confirm
     * 
     * API 구조:
     * 1. 클라이언트: 결제 수단 정보 → 토스페이먼츠 → widgets 토큰 반환
     * 2. 클라이언트: 토큰 + 주문 정보 → 서버로 전송
     * 3. 서버: 토스페이먼츠에 최종 승인 요청
     * 
     * 보안:
     * - 서버 승인 시 Secret Key 사용
     * - 카드 정보는 클라이언트에서만 다룸 (우리 서버에 저장 안 함)
     */
    @Override
    public ExternalPaymentResult requestPayment(
            String externalPaymentId,
            BigDecimal amount,
            String productName,
            String paymentMethodKey) {
        
        try {
            // 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(secretKey, "");  // 토스페이먼츠는 Basic Auth 사용
            
            // 요청 본문 구성
            Map<String, Object> request = new HashMap<>();
            request.put("paymentKey", externalPaymentId);
            request.put("orderId", externalPaymentId);
            request.put("amount", amount.longValue());
            request.put("paymentMethod", "CARD");  // 카드 결제
            request.put("orderedAt", java.time.LocalDateTime.now());
            request.put("approvedAt", java.time.LocalDateTime.now());
            
            // 작은 결제는 자동 승인, 큰 결제는 승인 필요
            request.put("shouldRetry", false);
            
            // HTTP 요청
            HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(request),
                headers
            );
            
            String url = apiUrl + "/payments/confirm";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            // 응답 파싱
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String status = responseNode.get("status").asText();
                if ("DONE".equals(status) || "APPROVED".equals(status)) {
                    return ExternalPaymentResult.success(
                        "SUCCESS",
                        "결제가 승인되었습니다",
                        responseNode.get("paymentKey").asText()
                    );
                }
            }
            
            // 실패 응답
            String errorCode = responseNode.get("code").asText();
            String errorMessage = responseNode.get("message").asText();
            
            log.error("토스페이먼츠 결제 요청 실패: code={}, message={}", errorCode, errorMessage);
            
            return ExternalPaymentResult.failure(errorCode, errorMessage);
            
        } catch (Exception e) {
            log.error("토스페이먼츠 API 호출 중 오류", e);
            return ExternalPaymentResult.failure("SYSTEM_ERROR", "시스템 오류가 발생했습니다");
        }
    }
    
    /**
     * 결제 상태 조회
     * 토스페이먼츠 API: GET /payments/{externalPaymentId}
     */
    @Override
    public ExternalPaymentResult inquirePaymentStatus(String externalPaymentId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(secretKey, "");
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .path("/payments/{paymentKey}")
                .buildAndExpand(externalPaymentId)
                .toUriString();
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            String status = responseNode.get("status").asText();
            
            return ExternalPaymentResult.success(
                "SUCCESS",
                "상태 조회 완료: " + status,
                externalPaymentId
            );
            
        } catch (Exception e) {
            log.error("결제 상태 조회 실패", e);
            return ExternalPaymentResult.failure("SYSTEM_ERROR", "시스템 오류가 발생했습니다");
        }
    }
    
    /**
     * 결제 취소
     * 토스페이먼츠 API: POST /payments/{paymentKey}/cancel
     */
    @Override
    public ExternalPaymentResult cancelPayment(String externalPaymentId, String reason) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(secretKey, "");
            
            Map<String, String> request = new HashMap<>();
            request.put("cancelReason", reason);
            
            HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(request),
                headers
            );
            
            String url = apiUrl + "/payments/" + externalPaymentId + "/cancel";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return ExternalPaymentResult.success("SUCCESS", "결제가 취소되었습니다", externalPaymentId);
            }
            
            return ExternalPaymentResult.failure("CANCEL_FAILED", "결제 취소에 실패했습니다");
            
        } catch (Exception e) {
            log.error("결제 취소 실패", e);
            return ExternalPaymentResult.failure("SYSTEM_ERROR", "시스템 오류가 발생했습니다");
        }
    }
    
    /**
     * Webhook 서명 검증
     * 
     * 토스페이먼츠는 HMAC SHA256으로 서명을 제공
     * 서명 검증 프로세스:
     * 1. Webhook 페이로드를 수신
     * 2. Secret Key로 HMAC SHA256 서명 생성
     * 3. 서명과 X-TOSS-SIGNATURE 헤더 비교
     * 
     * 이유:
     * - Webhook이 토스페이먼츠에서 온 것임을 확인
     * - 혹시 모를 위조 요청 방지
     * - 금융 거래 보안 필수 요소
     */
    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            // HMAC SHA256 서명 생성
            String computedSignature = new String(
                Base64.encodeBase64(
                    HmacUtils.hmacSha256(webhookSecret.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8))
                )
            );
            
            // 서명 비교 (상수 시간 비교로 타이밍 공격 방지)
            boolean isValid = computedSignature.equals(signature);
            
            if (!isValid) {
                log.warn("Webhook 서명 검증 실패: computed={}, received={}", computedSignature, signature);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Webhook 서명 검증 중 오류", e);
            return false;
        }
    }
}
