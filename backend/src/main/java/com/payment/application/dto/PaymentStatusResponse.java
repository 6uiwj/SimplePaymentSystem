package com.payment.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 상태 조회 응답
 */
public record PaymentStatusResponse(
        String paymentKey,
        String status,
        String statusDescription,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String failureReason
) {
}
