package com.payment.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 * Payment 엔티티를 API 응답 형식으로 변환
 */
public record PaymentResponse(
        Long id,
        String paymentKey,
        String orderId,
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        String status,
        String productName,
        String customerEmail,
        String customerName,
        Integer retryCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        String failureReason
) {
}
