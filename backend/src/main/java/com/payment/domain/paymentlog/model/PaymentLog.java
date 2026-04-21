package com.payment.domain.paymentlog.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * PaymentLog 엔티티 - Event Log
 * 
 * 역할:
 * - 모든 결제 관련 이벤트/상태 변경을 기록
 * - 감시(Audit) 목적: 결제 이력 추적
 * - 디버깅: 결제 흐름을 따로 추적 가능
 * 
 * 이유:
 * - Payment 엔티티는 현재 상태만 저장 (최신 값)
 * - 하지만 비즈니스에서는 모든 상태 변경 이력이 필요
 * - 금융 거래 특성상 감시 추적이 매우 중요 (컴플라이언스, 분석)
 * - DDD의 Event Sourcing 패턴과 유사
 */
@Entity
@Table(name = "payment_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 결제 ID - Payment 엔티티와의 연결
     * 외래키이지만 명시적으로 FK 제약을 설정하지 않음
     * (이벤트 기반 아키텍처에서 Payment 삭제 후에도 로그 유지)
     */
    @Column(nullable = false)
    private Long paymentId;
    
    /**
     * 결제 키 - 조회 편의성을 위해 복사 저장
     */
    @Column(nullable = false)
    private String paymentKey;
    
    /**
     * 로그 이벤트 타입
     * CREATED: 결제 생성
     * REQUESTED: 결제 요청 전송
     * SUCCESS: 결제 성공
     * FAIL: 결제 실패
     * RETRY: 재시도
     * WEBHOOK_RECEIVED: 웹훅 수신
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentLogEventType eventType;
    
    /**
     * 로그 이벤트 설명
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 응답 코드 (외부 API 응답 코드)
     */
    private String responseCode;
    
    /**
     * 응답 메시지 (외부 API 응답 메시지)
     */
    @Column(columnDefinition = "TEXT")
    private String responseMessage;
    
    /**
     * 로그 생성 시간
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 팩토리 메서드
     */
    public static PaymentLog createLog(
            Long paymentId,
            String paymentKey,
            PaymentLogEventType eventType,
            String description) {
        
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.paymentId = paymentId;
        paymentLog.paymentKey = paymentKey;
        paymentLog.eventType = eventType;
        paymentLog.description = description;
        paymentLog.createdAt = LocalDateTime.now();
        return paymentLog;
    }
    
    /**
     * API 응답 정보 추가
     */
    public static PaymentLog createLogWithResponse(
            Long paymentId,
            String paymentKey,
            PaymentLogEventType eventType,
            String description,
            String responseCode,
            String responseMessage) {
        
        PaymentLog paymentLog = createLog(paymentId, paymentKey, eventType, description);
        paymentLog.responseCode = responseCode;
        paymentLog.responseMessage = responseMessage;
        return paymentLog;
    }
    
    /**
     * 이벤트 타입
     */
    public enum PaymentLogEventType {
        CREATED("생성"),
        REQUESTED("요청"),
        SUCCESS("성공"),
        FAIL("실패"),
        RETRY("재시도"),
        WEBHOOK_RECEIVED("웹훅 수신");
        
        private final String description;
        
        PaymentLogEventType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
