package com.payment.domain.paymentlog.repository;

import com.payment.domain.paymentlog.model.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PaymentLog Repository
 * 
 * 결제 이력 조회를 위한 저장소
 */
@Repository
public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {
    
    /**
     * 특정 결제의 모든 로그 조회
     * 결제 흐름 추적을 위해 시간순으로 정렬
     */
    @Query("SELECT pl FROM PaymentLog pl WHERE pl.paymentId = :paymentId ORDER BY pl.createdAt ASC")
    List<PaymentLog> findByPaymentId(@Param("paymentId") Long paymentId);
    
    /**
     * 결제 키로 모든 로그 조회
     */
    @Query("SELECT pl FROM PaymentLog pl WHERE pl.paymentKey = :paymentKey ORDER BY pl.createdAt ASC")
    List<PaymentLog> findByPaymentKey(@Param("paymentKey") String paymentKey);
    
    /**
     * 특정 이벤트 타입의 로그 조회
     * 통계 또는 모니터링 목적
     */
    @Query("SELECT pl FROM PaymentLog pl WHERE pl.eventType = :eventType ORDER BY pl.createdAt DESC")
    List<PaymentLog> findByEventType(@Param("eventType") PaymentLog.PaymentLogEventType eventType);
}
