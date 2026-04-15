package com.payment.infrastructure.external.payment;

import java.math.BigDecimal;

/**
 * PaymentGateway 인터페이스
 * 
 * 역할:
 * - 외부 결제 API의 실제 구현을 추상화
 * - 비즈니스 로직은 이 인터페이스만 사용
 * - 결제 게이트웨이 변경 시에도 비즈니스 로직 변경 불필요
 * 
 * 이유 (Strategy Pattern):
 * - 여러 결제 게이트웨이를 지원해야 할 때 (토스페이먼츠, 카카오페이 등)
 * - 환경에 따라 다른 구현을 선택 가능
 * - 테스트 시 Mock 구현 사용 가능
 */
public interface PaymentGateway {
    
    /**
     * 결제 요청
     * 
     * @param externalPaymentId 외부 시스템에서 관리할 결제 ID
     * @param amount 결제 금액
     * @param productName 상품명
     * @param paymentMethodKey 결제 수단 (token, card, etc)
     * @return 결제 결과
     */
    ExternalPaymentResult requestPayment(
        String externalPaymentId,
        BigDecimal amount,
        String productName,
        String paymentMethodKey
    );
    
    /**
     * 결제 상태 조회
     * 외부 API에서 특정 결제의 상태를 조회
     * 
     * @param externalPaymentId 외부 결제 ID
     * @return 결제 결과
     */
    ExternalPaymentResult inquirePaymentStatus(String externalPaymentId);
    
    /**
     * 결제 취소
     * 
     * @param externalPaymentId 외부 결제 ID
     * @param reason 취소 사유
     * @return 취소 결과
     */
    ExternalPaymentResult cancelPayment(String externalPaymentId, String reason);
    
    /**
     * Webhook 서명 검증
     * 
     * @param payload Webhook 페이로드
     * @param signature 서명
     * @return 검증 결과
     */
    boolean verifyWebhookSignature(String payload, String signature);
}
