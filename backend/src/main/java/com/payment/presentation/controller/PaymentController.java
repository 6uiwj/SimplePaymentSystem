package com.payment.presentation.controller;

import com.payment.application.dto.*;
import com.payment.application.service.PaymentService;
import com.payment.domain.paymentlog.model.PaymentLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PaymentController - Presentation Layer
 * 
 * 역할:
 * - HTTP 요청/응답 처리
 * - 입력 검증 (@Valid)
 * - 응답 직렬화
 * 
 * 엔드포인트:
 * 1. POST /payments - 결제 요청 생성
 * 2. POST /payments/{paymentKey}/approve - 결제 승인
 * 3. GET /payments/{paymentKey} - 결제 상태 조회
 * 4. POST /payments/{paymentKey}/retry - 결제 재시도
 * 5. GET /payments/{paymentKey}/history - 결제 이력 조회
 * 
 * 이유:
 * - 도메인 로직과 HTTP 처리를 분리
 * - 같은 비즈니스 로직을 다양한 클라이언트(Web, Mobile)에 제공 가능
 * - 향후 gRPC, WebSocket 등으로 확장 용이
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * 결제 요청 생성
     * 
     * API: POST /payments
     * 
     * 요청 흐름:
     * 1. 클라이언트: 상품 선택 시 이 엔드포인트 호출
     * 2. 서버: 결제 정보 저장, idempotencyKey 등록
     * 3. 클라이언트: 반환된 redirectUrl로 결제 창 오픈
     * 4. 클라이언트: 결제 완료 후 approve 엔드포인트 호출
     * 
     * 멱등성:
     * - 같은 idempotencyKey로 중복 요청 시 동일한 결과 반환
     * - 네트워크 장애로 인한 재시도 안전
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentInitializeResponse>> initializePayment(
            @Valid @RequestBody CreatePaymentRequest request) {
        
        log.info("결제 초기화 요청: orderId={}, amount={}", request.orderId(), request.amount());
        
        try {
            PaymentInitializeResponse response = paymentService.initializePayment(request);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "결제 요청이 생성되었습니다"));
                
        } catch (Exception e) {
            log.error("결제 초기화 실패", e);
            throw e;
        }
    }
    
    /**
     * 결제 승인
     * 
     * API: POST /payments/{paymentKey}/approve
     * 
     * 요청 흐름:
     * 1. 사용자가 결제 창에서 결제 정보 입력
     * 2. 클라이언트가 토스페이먼츠 API로 결제 토큰 획득
     * 3. 클라이언트가 이 엔드포인트에 토큰과 함께 호출
     * 4. 서버가 토스페이먼츠 최종 승인 API 호출
     * 
     * 응답:
     * - 성공: Payment 객체와 상태
     * - 실패: 실패 이유와 함께 반환 (클라이언트가 재시도 결정)
     */
    @PostMapping("/{paymentKey}/approve")
    public ResponseEntity<ApiResponse<PaymentResponse>> approvePayment(
            @PathVariable String paymentKey,
            @RequestParam String paymentMethodKey) {
        
        log.info("결제 승인 요청: paymentKey={}", paymentKey);
        
        try {
            PaymentResponse response = paymentService.approvePayment(paymentKey, paymentMethodKey);
            return ResponseEntity.ok(ApiResponse.of(response, "결제가 처리되었습니다"));
            
        } catch (Exception e) {
            log.error("결제 승인 실패: paymentKey={}, error={}", paymentKey, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 결제 상태 조회
     * 
     * API: GET /payments/{paymentKey}
     * 
     * 용도:
     * - 클라이언트가 결제 상태를 주기적으로 조회
     * - 모바일 앱 백그라운드에서 상태 확인
     * - 안드로이드/iOS 네이티브 결제와 서버 상태 동기화
     */
    @GetMapping("/{paymentKey}")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @PathVariable String paymentKey) {
        
        log.info("결제 상태 조회: paymentKey={}", paymentKey);
        
        try {
            PaymentStatusResponse response = paymentService.getPaymentStatus(paymentKey);
            return ResponseEntity.ok(ApiResponse.of(response));
            
        } catch (Exception e) {
            log.error("결제 조회 실패", e);
            throw e;
        }
    }
    
    /**
     * 결제 재시도
     * 
     * API: POST /payments/{paymentKey}/retry
     * 
     * 언제 사용:
     * - 카드사와의 통신 오류로 실패한 경우
     * - 이전 결제가 FAIL 상태일 때
     * - 최대 3회까지만 가능
     * 
     * 주의:
     * - 같은 idempotencyKey로 재시도하면 중복 결제 방지
     * - 새로운 paymentMethodKey 필요 (새로운 결제 수단)
     */
    @PostMapping("/{paymentKey}/retry")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(
            @PathVariable String paymentKey,
            @RequestParam String paymentMethodKey) {
        
        log.info("결제 재시도: paymentKey={}", paymentKey);
        
        try {
            PaymentResponse response = paymentService.retryPayment(paymentKey, paymentMethodKey);
            return ResponseEntity.ok(ApiResponse.of(response, "결제 재시도를 진행했습니다"));
            
        } catch (Exception e) {
            log.error("결제 재시도 실패", e);
            throw e;
        }
    }
    
    /**
     * 결제 이력 조회
     * 
     * API: GET /payments/{paymentKey}/history
     * 
     * 반환 정보:
     * - CREATED: 결제 생성
     * - REQUESTED: 결제 요청 전송
     * - SUCCESS: 결제 성공
     * - FAIL: 결제 실패
     * - RETRY: 재시도
     * - WEBHOOK_RECEIVED: 웹훅 수신
     * 
     * 용도:
     * - 고객 지원: 결제 흐름 추적
     * - 분석: 결제 과정 최적화
     * - 개발: 디버깅
     */
    @GetMapping("/{paymentKey}/history")
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> getPaymentHistory(
            @PathVariable String paymentKey) {
        
        log.info("결제 이력 조회: paymentKey={}", paymentKey);
        
        try {
            List<PaymentLog> history = paymentService.getPaymentHistory(paymentKey);
            List<PaymentHistoryResponse> responses = history.stream()
                .map(log -> new PaymentHistoryResponse(
                    log.getEventType().name(),
                    log.getEventType().getDescription(),
                    log.getDescription(),
                    log.getResponseCode(),
                    log.getResponseMessage(),
                    log.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.of(responses));
            
        } catch (Exception e) {
            log.error("결제 이력 조회 실패", e);
            throw e;
        }
    }
}

/**
 * 결제 이력 응답 DTO
 */
record PaymentHistoryResponse(
        String eventType,
        String eventTypeDescription,
        String description,
        String responseCode,
        String responseMessage,
        java.time.LocalDateTime createdAt
) {
}
