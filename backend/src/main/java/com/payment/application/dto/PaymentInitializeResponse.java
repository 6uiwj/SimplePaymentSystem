package com.payment.application.dto;

import java.math.BigDecimal;

/**
 * 결제 요청 응답
 * 승인 요청 전 단계에서 반환하는 응답
 */
public record PaymentInitializeResponse(
        String paymentKey,
        String orderId,
        BigDecimal amount,
        String productName,
        String customerEmail,
        String customerName,
        /**
         * 클라이언트가 결제 프로세스를 진행할 URL
         */
        String redirectUrl
) {
}
