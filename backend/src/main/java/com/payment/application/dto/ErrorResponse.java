package com.payment.application.dto;

import java.time.LocalDateTime;

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
