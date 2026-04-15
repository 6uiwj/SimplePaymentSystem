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

/**
 * 일반 오류 응답
 */
public record ErrorResponse(
        int code,
        String message,
        String errorType,
        LocalDateTime timestamp
) {
}

/**
 * 성공 응답 래퍼
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message
) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(true, data, null);
    }
    
    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }
}
