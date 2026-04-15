package com.payment.application.service;

import com.payment.application.dto.*;
import com.payment.domain.idempotency.model.IdempotencyKey;
import com.payment.domain.idempotency.repository.IdempotencyKeyRepository;
import com.payment.domain.payment.model.Payment;
import com.payment.domain.payment.model.PaymentStatus;
import com.payment.domain.payment.repository.PaymentRepository;
import com.payment.domain.paymentlog.model.PaymentLog;
import com.payment.domain.paymentlog.repository.PaymentLogRepository;
import com.payment.infrastructure.cache.DistributedLockService;
import com.payment.infrastructure.external.payment.PaymentGateway;
import com.payment.infrastructure.external.payment.ExternalPaymentResult;
import com.payment.common.exception.DuplicatePaymentException;
import com.payment.common.exception.PaymentNotFoundException;
import com.payment.common.exception.InvalidPaymentStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PaymentService - Application Layer
 * 
 * 역할:
 * - use case 구현: 결제 생성, 승인, 조회, 재시도 등
 * - Domain Layer와 Infrastructure Layer를 조율
 * - 트랜잭션 관리
 * 
 * 아키텍처:
 * 1. Presentation(Controller) → DTO 검증
 * 2. Application Service → Use case 로직
 * 3. Domain Model → 비즈니스 규칙
 * 4. Infrastructure → 외부 시스템과 통신
 * 
 * 이유:
 * - Use case 별로 서비스를 분리하면 비즈니스 로직이 명확
 * - 트랜잭션과 예외 처리를 한 곳에서 관리
 * - 도메인 엔티티는 순수한 비즈니스 로직만 포함
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {
    
    // Repositories
    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    
    // Infrastructure
    private final PaymentGateway paymentGateway;  // 외부 결제 게이트웨이 (토스페이먼츠)
    private final DistributedLockService distributedLockService;
    
    // Configuration
    @Value("${payment.idempotency.ttl:24}")
    private Integer idempotencyTtl;
    
    /**
     * USE CASE 1: 결제 요청 생성
     * 
     * 프로세스:
     * 1. 중복 결제 확인 (Idempotency Key)
     * 2. 분산 락 획득 (동시성 제어)
     * 3. Payment 엔티티 생성
     * 4. IdempotencyKey 저장
     * 5. PaymentLog 기록
     * 
     * @param request 결제 요청
     * @return 결제 초기화 응답
     * @throws DuplicatePaymentException 중복 결제 요청
     */
    public PaymentInitializeResponse initializePayment(CreatePaymentRequest request) {
        
        // 1단계: 중복 결제 확인 (Idempotency)
        var existingIdempotencyKey = idempotencyKeyRepository.findByKey(request.idempotencyKey());
        if (existingIdempotencyKey.isPresent() && !existingIdempotencyKey.get().isExpired()) {
            log.info("중복 결제 요청 감지: {}", request.idempotencyKey());
            // 기존 결제의 상태와 정보를 반환
            if (existingIdempotencyKey.get().getPaymentId() != null) {
                return buildPaymentInitializeResponse(
                    paymentRepository.findById(existingIdempotencyKey.get().getPaymentId())
                        .orElseThrow(() -> new PaymentNotFoundException("결제를 찾을 수 없습니다"))
                );
            }
        }
        
        // 2단계: 분산 락 획득 (동시성 제어)
        // 같은 idempotencyKey에 대해 동시에 여러 요청이 들어올 때
        // 하나의 요청만 진행하고 나머지는 대기/거절
        String lockKey = "payment:idempotency:" + request.idempotencyKey();
        boolean lockAcquired = distributedLockService.acquireLock(lockKey, 5);  // 5초 타임아웃
        
        if (!lockAcquired) {
            throw new DuplicatePaymentException("결제 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }
        
        try {
            // 3단계: Payment 엔티티 생성
            String paymentKey = UUID.randomUUID().toString();
            Payment payment = Payment.createPayment(
                paymentKey,
                request.orderId(),
                request.idempotencyKey(),
                request.amount(),
                request.productName(),
                request.customerEmail(),
                request.customerName()
            );
            
            // 데이터베이스에 저장
            Payment savedPayment = paymentRepository.save(payment);
            
            // 4단계: IdempotencyKey 저장
            IdempotencyKey idempotencyKey = IdempotencyKey.create(
                request.idempotencyKey(),
                savedPayment.getId(),
                PaymentStatus.READY.toString(),
                idempotencyTtl
            );
            idempotencyKeyRepository.save(idempotencyKey);
            
            // 5단계: PaymentLog 기록
            PaymentLog log = PaymentLog.createLog(
                savedPayment.getId(),
                paymentKey,
                PaymentLog.PaymentLogEventType.CREATED,
                String.format("결제 생성: %s원 (%s)", request.amount(), request.productName())
            );
            paymentLogRepository.save(log);
            
            log.info("결제 생성 완료: paymentKey={}, amount={}", paymentKey, request.amount());
            
            return buildPaymentInitializeResponse(savedPayment);
            
        } finally {
            // 분산 락 해제
            distributedLockService.releaseLock(lockKey);
        }
    }
    
    /**
     * USE CASE 2: 결제 승인 (결제 API 호출)
     * 
     * 프로세스:
     * 1. 결제 정보 조회
     * 2. 상태 검증 (READY만 가능)
     * 3. 외부 결제 게이트웨이 호출
     * 4. 결과에 따라 상태 업데이트
     * 
     * @param paymentKey 결제 키
     * @param paymentMethodKey 결제 수단 토큰
     * @return 결제 응답
     */
    public PaymentResponse approvePayment(String paymentKey, String paymentMethodKey) {
        
        // 1. 결제 정보 조회
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
            .orElseThrow(() -> new PaymentNotFoundException("결제를 찾을 수 없습니다"));
        
        // 2. 상태 검증
        if (payment.getStatus() != PaymentStatus.READY && payment.getStatus() != PaymentStatus.FAIL) {
            throw new InvalidPaymentStatusException(
                String.format("현재 상태 '%s'에서는 승인할 수 없습니다", payment.getStatus())
            );
        }
        
        try {
            // 3. 외부 결제 게이트웨이 호출
            String externalPaymentId = UUID.randomUUID().toString();  // 실제로는 게이트웨이에서 생성
            
            // 상태를 REQUESTED로 변경
            payment.markAsRequested(externalPaymentId);
            
            // 게이트웨이 호출 (토스페이먼츠 API)
            ExternalPaymentResult result = paymentGateway.requestPayment(
                externalPaymentId,
                payment.getAmount(),
                payment.getProductName(),
                paymentMethodKey
            );
            
            // 4. 결과 처리
            if (result.isSuccess()) {
                payment.markAsSuccess();
                
                PaymentLog successLog = PaymentLog.createLogWithResponse(
                    payment.getId(),
                    paymentKey,
                    PaymentLog.PaymentLogEventType.SUCCESS,
                    "결제 승인 완료",
                    result.getCode(),
                    result.getMessage()
                );
                paymentLogRepository.save(successLog);
                
                log.info("결제 성공: paymentKey={}", paymentKey);
                
            } else {
                payment.markAsFail(result.getMessage());
                
                PaymentLog failLog = PaymentLog.createLogWithResponse(
                    payment.getId(),
                    paymentKey,
                    PaymentLog.PaymentLogEventType.FAIL,
                    "결제 승인 실패",
                    result.getCode(),
                    result.getMessage()
                );
                paymentLogRepository.save(failLog);
                
                log.info("결제 실패: paymentKey={}, reason={}", paymentKey, result.getMessage());
            }
            
            Payment savedPayment = paymentRepository.save(payment);
            return convertToResponse(savedPayment);
            
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생: paymentKey={}", paymentKey, e);
            payment.markAsFail("시스템 오류: " + e.getMessage());
            
            PaymentLog errorLog = PaymentLog.createLog(
                payment.getId(),
                paymentKey,
                PaymentLog.PaymentLogEventType.FAIL,
                "결제 처리 중 오류: " + e.getMessage()
            );
            paymentLogRepository.save(errorLog);
            
            paymentRepository.save(payment);
            throw new RuntimeException("결제 처리 실패", e);
        }
    }
    
    /**
     * USE CASE 3: 결제 상태 조회
     * 
     * @param paymentKey 결제 키
     * @return 결제 상태
     */
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String paymentKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
            .orElseThrow(() -> new PaymentNotFoundException("결제를 찾을 수 없습니다"));
        
        return new PaymentStatusResponse(
            payment.getPaymentKey(),
            payment.getStatus().toString(),
            payment.getStatus().getDescription(),
            payment.getAmount(),
            payment.getCreatedAt(),
            payment.getCompletedAt(),
            payment.getFailureReason()
        );
    }
    
    /**
     * USE CASE 4: 결제 재시도
     * 
     * 실패한 결제를 재시도할 때 호출
     * 최대 3회까지만 재시도 가능
     * 
     * @param paymentKey 결제 키
     * @param paymentMethodKey 새로운 결제 수단 토큰
     * @return 재시도 결과
     */
    public PaymentResponse retryPayment(String paymentKey, String paymentMethodKey) {
        Payment payment = paymentRepository.findByPaymentKey(paymentKey)
            .orElseThrow(() -> new PaymentNotFoundException("결제를 찾을 수 없습니다"));
        
        if (!payment.isRetryable()) {
            throw new InvalidPaymentStatusException(
                "재시도할 수 없는 결제입니다. 최대 재시도 횟수를 초과했습니다."
            );
        }
        
        // 상태를 READY로 되돌림
        payment.markForRetry();
        
        PaymentLog retryLog = PaymentLog.createLog(
            payment.getId(),
            paymentKey,
            PaymentLog.PaymentLogEventType.RETRY,
            String.format("결제 재시도 (%d회차)", payment.getRetryCount())
        );
        paymentLogRepository.save(retryLog);
        paymentRepository.save(payment);
        
        // 다시 승인 시도
        return approvePayment(paymentKey, paymentMethodKey);
    }
    
    /**
     * USE CASE 5: 결제 이력 조회
     * 
     * @param paymentKey 결제 키
     * @return 결제 이력 목록
     */
    @Transactional(readOnly = true)
    public java.util.List<PaymentLog> getPaymentHistory(String paymentKey) {
        return paymentLogRepository.findByPaymentKey(paymentKey);
    }
    
    // ===== Helper Methods =====
    
    private PaymentInitializeResponse buildPaymentInitializeResponse(Payment payment) {
        return new PaymentInitializeResponse(
            payment.getPaymentKey(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getProductName(),
            payment.getCustomerEmail(),
            payment.getCustomerName(),
            "https://payment.example.com/checkout?key=" + payment.getPaymentKey()
        );
    }
    
    private PaymentResponse convertToResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getPaymentKey(),
            payment.getOrderId(),
            payment.getIdempotencyKey(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus().toString(),
            payment.getProductName(),
            payment.getCustomerEmail(),
            payment.getCustomerName(),
            payment.getRetryCount(),
            payment.getCreatedAt(),
            payment.getUpdatedAt(),
            payment.getCompletedAt(),
            payment.getFailureReason()
        );
    }
}
