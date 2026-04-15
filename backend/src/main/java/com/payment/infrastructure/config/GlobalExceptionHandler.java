package com.payment.infrastructure.config;

import com.payment.application.dto.ErrorResponse;
import com.payment.common.exception.PaymentException;
import com.payment.common.exception.PaymentNotFoundException;
import com.payment.common.exception.DuplicatePaymentException;
import com.payment.common.exception.InvalidPaymentStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - 전역 예외 처리
 * 
 * 역할:
 * - 모든 컨트롤러에서 발생하는 예외를 일관되게 처리
 * - 표준 에러 응답 형식 제공
 * - 로깅 처리
 * 
 * 이유:
 * - 모든 엔드포인트에서 동일한 에러 형식 사용
 * - 클라이언트가 예상 가능한 에러 응답 처리 가능
 * - 스택 트레이스가 노출되지 않음 (보안)
 * - 중복된 예외 처리 코드 제거
 * 
 * 처리 흐름:
 * 1. Controller에서 예외 발생
 * 2. @ExceptionHandler 매칭
 * 3. 적절한 응답 반환
 * 4. 클라이언트에 전달
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 결제 관련 예외 처리
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(
            PaymentNotFoundException e,
            WebRequest request) {
        
        log.warn("결제를 찾을 수 없음: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            e.getMessage(),
            "PAYMENT_NOT_FOUND",
            LocalDateTime.now()
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse);
    }
    
    /**
     * 중복 결제 요청
     */
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePaymentException(
            DuplicatePaymentException e,
            WebRequest request) {
        
        log.warn("중복 결제 요청: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            e.getMessage(),
            "DUPLICATE_PAYMENT",
            LocalDateTime.now()
        );
        
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(errorResponse);
    }
    
    /**
     * 유효하지 않은 결제 상태
     */
    @ExceptionHandler(InvalidPaymentStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPaymentStatusException(
            InvalidPaymentStatusException e,
            WebRequest request) {
        
        log.warn("유효하지 않은 결제 상태: {}", e.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            e.getMessage(),
            "INVALID_PAYMENT_STATUS",
            LocalDateTime.now()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse);
    }
    
    /**
     * 모든 PaymentException 처리
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException e,
            WebRequest request) {
        
        log.error("결제 시스템 오류", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "결제 처리 중 오류가 발생했습니다",
            "PAYMENT_ERROR",
            LocalDateTime.now()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }
    
    /**
     * 입력 검증 실패
     * @Valid 검증에서 필드 오류 발생 시
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException e,
            WebRequest request) {
        
        log.warn("입력 검증 실패");
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", HttpStatus.BAD_REQUEST.value());
        response.put("message", "입력 검증에 실패했습니다");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    /**
     * 예상하지 못한 모든 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception e,
            WebRequest request) {
        
        log.error("예상치 못한 시스템 오류", e);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "시스템 오류가 발생했습니다. 관리자에게 문의해주세요",
            "SYSTEM_ERROR",
            LocalDateTime.now()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse);
    }
}
