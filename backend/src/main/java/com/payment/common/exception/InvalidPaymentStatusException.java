package com.payment.common.exception;

/**
 * 유효하지 않은 결제 상태
 */
public class InvalidPaymentStatusException extends PaymentException {
    
    public InvalidPaymentStatusException(String message) {
        super(message);
    }
}
