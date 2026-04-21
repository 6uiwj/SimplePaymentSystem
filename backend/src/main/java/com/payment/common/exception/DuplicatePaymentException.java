package com.payment.common.exception;

/**
 * 중복 결제 요청
 */
public class DuplicatePaymentException extends PaymentException {
    
    public DuplicatePaymentException(String message) {
        super(message);
    }
}
