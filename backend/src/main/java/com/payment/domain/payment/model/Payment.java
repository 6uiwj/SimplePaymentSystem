package com.payment.domain.payment.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Payment 엔티티 - DDD Aggregate Root
 * 
 * 역할:
 * - 결제 정보의 중심이 되는 엔티티
 * - 비즈니스 규칙을 포함: 상태 전환, 중복 방지, 금액 검증
 * - 카드 정보는 저장하지 않음 (보안 요구사항)
 * - 모든 결제 상태 변경은 이 엔티티의 메서드를 통해서만 진행
 * 
 * 이유:
 * - DDD에서 Aggregate Root는 일관성 경계(Consistency Boundary)를 관리
 * - 비즈니스 로직이 엔티티에 있으므로 도메인 규칙이 명확함
 * - 외부에서 직접 상태 변경 불가능 → 데이터 무결성 보장
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 결제 키 - 외부에서 참조할 고유 식별자
     * UUID로 생성하여 분산 환경에서 중복 회피
     */
    @Column(nullable = false, unique = true)
    private String paymentKey;
    
    /**
     * 주문 ID - 비즈니스 도메인의 주문 시스템과 연계
     */
    @Column(nullable = false)
    private String orderId;
    
    /**
     * Idempotency Key - 중복 결제 방지
     * 같은 idempotencyKey로 요청 시 동일한 결과 반환
     * 네트워크 장애로 인한 재시도에 안전함
     */
    @Column(nullable = false, unique = true)
    private String idempotencyKey;
    
    /**
     * 결제 금액 - BigDecimal은 금융 계산의 정확성을 보장
     * Double 대신 BigDecimal을 사용하는 이유: 부동소수점 오차 제거
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    /**
     * 결제 통화 - 기본값 KRW
     */
    @Column(nullable = false)
    private String currency = "KRW";
    
    /**
     * 결제 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.READY;
    
    /**
     * 상품 설명
     */
    @Column(nullable = false)
    private String productName;
    
    /**
     * 구매자 이메일
     */
    @Column(nullable = false)
    private String customerEmail;
    
    /**
     * 구매자 이름
     */
    @Column(nullable = false)
    private String customerName;
    
    /**
     * 구매자 전화번호
     */
    private String customerPhone;
    
    /**
     * 외부 결제 API에서 반환한 결제 ID (토스페이먼츠: paymentKey)
     * 외부 API와의 매핑을 위해 저장
     */
    private String externalPaymentId;
    
    /**
     * 실패 이유
     */
    private String failureReason;
    
    /**
     * 재시도 횟수
     */
    @Column(nullable = false)
    private Integer retryCount = 0;
    
    /**
     * 결제 생성 시간
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 결제 수정 시간
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    /**
     * 결제 완료 시간
     */
    private LocalDateTime completedAt;
    
    // ===== 팩토리 메서드 (생성) =====
    
    /**
     * 새로운 결제 생성
     * DDD의 팩토리 패턴: 유효한 결제 객체만 생성
     * 
     * @param paymentKey 결제 키
     * @param orderId 주문 ID
     * @param idempotencyKey 중복 방지 키
     * @param amount 결제 금액
     * @param productName 상품 이름
     * @param customerEmail 구매자 이메일
     * @param customerName 구매자 이름
     * @return 새로 생성된 Payment 엔티티
     * @throws IllegalArgumentException 유효하지 않은 금액일 경우
     */
    public static Payment createPayment(
            String paymentKey,
            String orderId,
            String idempotencyKey,
            BigDecimal amount,
            String productName,
            String customerEmail,
            String customerName) {
        
        // 비즈니스 규칙: 결제 금액은 0보다 커야 함
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
        }
        
        Payment payment = new Payment();
        payment.paymentKey = paymentKey;
        payment.orderId = orderId;
        payment.idempotencyKey = idempotencyKey;
        payment.amount = amount;
        payment.productName = productName;
        payment.customerEmail = customerEmail;
        payment.customerName = customerName;
        payment.status = PaymentStatus.READY;
        payment.retryCount = 0;
        payment.createdAt = LocalDateTime.now();
        payment.updatedAt = LocalDateTime.now();
        
        return payment;
    }
    
    // ===== 비즈니스 메서드 (상태 전환) =====
    
    /**
     * 결제 요청 상태로 변경
     * DDD 원칙: 상태 전환 규칙을 엔티티 메서드에 구현
     * 
     * 이유:
     * - READY 상태에서만 REQUESTED로 전환 가능
     * - 외부 API로 요청을 보냈을 때 호출
     */
    public void markAsRequested(String externalPaymentId) {
        if (!status.canTransitionTo(PaymentStatus.REQUESTED)) {
            throw new IllegalStateException(
                String.format("상태 '%s'에서 REQUESTED로 전환할 수 없습니다", status)
            );
        }
        
        this.status = PaymentStatus.REQUESTED;
        this.externalPaymentId = externalPaymentId;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 결제 성공 처리
     * Webhook에서 호출되거나 상태 조회 후 성공 확인 시 호출
     */
    public void markAsSuccess() {
        if (!status.canTransitionTo(PaymentStatus.SUCCESS)) {
            throw new IllegalStateException(
                String.format("상태 '%s'에서 SUCCESS로 전환할 수 없습니다", status)
            );
        }
        
        this.status = PaymentStatus.SUCCESS;
        this.updatedAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 결제 실패 처리
     * 재시도 가능/불가능 여부와 함께 호출
     * 
     * @param failureReason 실패 이유
     */
    public void markAsFail(String failureReason) {
        if (!status.canTransitionTo(PaymentStatus.FAIL)) {
            throw new IllegalStateException(
                String.format("상태 '%s'에서 FAIL로 전환할 수 없습니다", status)
            );
        }
        
        this.status = PaymentStatus.FAIL;
        this.failureReason = failureReason;
        this.updatedAt = LocalDateTime.now();
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 재시도 준비 - FAIL 상태에서 READY로 되돌림
     * 재시도 횟수를 증가시킴
     */
    public void markForRetry() {
        if (this.status != PaymentStatus.FAIL) {
            throw new IllegalStateException("실패 상태인 결제만 재시도할 수 있습니다");
        }
        
        this.status = PaymentStatus.READY;
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
        this.failureReason = null;
        this.externalPaymentId = null;
    }
    
    /**
     * 재시도 가능 여부 확인
     */
    public boolean isRetryable() {
        return this.status == PaymentStatus.FAIL && this.retryCount < 3;
    }
}
