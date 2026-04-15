package com.payment.infrastructure.external.payment;

import lombok.Getter;

/**
 * 외부 결제 API 응답 결과
 */
@Getter
public class ExternalPaymentResult {
    private final boolean success;
    private final String code;
    private final String message;
    private final String paymentId;
    
    public ExternalPaymentResult(boolean success, String code, String message, String paymentId) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.paymentId = paymentId;
    }
    
    public static ExternalPaymentResult success(String code, String message, String paymentId) {
        return new ExternalPaymentResult(true, code, message, paymentId);
    }
    
    public static ExternalPaymentResult failure(String code, String message) {
        return new ExternalPaymentResult(false, code, message, null);
    }
}
