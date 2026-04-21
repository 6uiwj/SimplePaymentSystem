package com.payment.common.exception;

/**
 * PaymentException - 결제 기반 예외
 */
public abstract class PaymentException extends RuntimeException {
    
    public PaymentException(String message) {
        super(message);
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
