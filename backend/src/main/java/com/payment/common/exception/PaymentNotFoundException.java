package com.payment.common.exception;

/**
 * 결제를 찾을 수 없음
 */
public class PaymentNotFoundException extends PaymentException {
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
