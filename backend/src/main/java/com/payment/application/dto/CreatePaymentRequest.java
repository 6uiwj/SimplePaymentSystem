package com.payment.application.dto;

import java.math.BigDecimal;

/**
 * 결제 요청 DTO
 * Presentation Layer에서 들어오는 요청을 담는 객체
 * 
 * DTO 사용 이유:
 * - API 요청 형식과 내부 도메인 모델을 분리
 * - API 변경 시 내부 도메인 영향 최소화
 * - 검증(Validation)을 한 곳에서 처리
 */
public record CreatePaymentRequest(
        /**
         * 클라이언트에서 생성한 고유 키
         * 중복 결제 방지에 사용
         * UUID 형식 권장
         */
        String idempotencyKey,
        
        /**
         * 주문 ID
         * 비즈니스 도메인의 주문 시스템과 연계
         */
        String orderId,
        
        /**
         * 결제 금액
         * 단위: 원 (KRW)
         */
        BigDecimal amount,
        
        /**
         * 상품명
         */
        String productName,
        
        /**
         * 구매자 이메일
         */
        String customerEmail,
        
        /**
         * 구매자 이름
         */
        String customerName,
        
        /**
         * 구매자 전화번호
         */
        String customerPhone,
        
        /**
         * 결제 승인 요청 Token
         * 클라이언트에서 토스페이먼츠 API로 받은 토큰
         */
        String paymentMethodKey
) {
    /**
     * 커스텀 검증 로직 (필요 시)
     */
    public CreatePaymentRequest {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey는 필수입니다");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId는 필수입니다");
        }
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount는 0보다 커야 합니다");
        }
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("customerEmail은 필수입니다");
        }
    }
}
