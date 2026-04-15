package com.payment.domain.payment.repository;

import com.payment.domain.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Payment Repository - Domain Layer
 * 
 * 역할:
 * - Payment 엔티티의 영속성 관리를 위한 인터페이스
 * - DDD에서 Repository는 도메인 객체를 마치 메모리에 있는 컬렉션처럼 다룰 수 있게 한다
 * 
 * 이유:
 * - Spring Data JPA는 구현체를 자동으로 생성 (프록시 패턴)
 * - 비즈니스 로직에서는 이 인터페이스만 의존 → 데이터베이스 기술 변경이 쉬움
 * - 데이터베이스 쿼리를 도메인 언어로 표현
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * 결제 키로 결제 정보 조회
     * 외부 API 요청에서 전달된 paymentKey로 조회
     */
    Optional<Payment> findByPaymentKey(String paymentKey);
    
    /**
     * Idempotency Key로 결제 정보 조회
     * 중복 결제 방지: 같은 idempotencyKey가 이미 존재하면 기존 결과 반환
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * 주문 ID로 결제 정보 조회
     * 비즈니스에서 주문으로 결제 정보 조회
     */
    Optional<Payment> findByOrderId(String orderId);
    
    /**
     * 외부 결제 ID로 결제 정보 조회
     * 토스페이먼츠 웹훅에서 externalPaymentId로 결제 정보를 찾기 위해 필요
     */
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);
    
    /**
     * 특정 상태의 결제 목록 조회
     * 상태별 집계 또는 관리 목적으로 사용
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByStatus(@Param("status") String status);
    
    /**
     * 재시도 필요한 결제 목록 조회
     * 배치 작업에서 실패한 결제의 자동 재시도를 위해 필요
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAIL' AND p.retryCount < 3 ORDER BY p.updatedAt ASC")
    List<Payment> findRetryablePayments();
}
