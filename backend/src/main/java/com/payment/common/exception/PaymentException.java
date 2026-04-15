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

/**
 * 결제를 찾을 수 없음
 */
class PaymentNotFoundException extends PaymentException {
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
}

/**
 * 중복 결제 요청
 */
class DuplicatePaymentException extends PaymentException {
    
    public DuplicatePaymentException(String message) {
        super(message);
    }
}

/**
 * 유효하지 않은 결제 상태
 */
class InvalidPaymentStatusException extends PaymentException {
    
    public InvalidPaymentStatusException(String message) {
        super(message);
    }
}
