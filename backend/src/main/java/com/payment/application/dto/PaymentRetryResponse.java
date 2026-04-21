package com.payment.application.dto;

import java.time.LocalDateTime;

/**
 * 결제 재시도 응답
 */
public record PaymentRetryResponse(
        String paymentKey,
        String status,
        Integer retryCount,
        LocalDateTime nextRetryTime
) {
}
