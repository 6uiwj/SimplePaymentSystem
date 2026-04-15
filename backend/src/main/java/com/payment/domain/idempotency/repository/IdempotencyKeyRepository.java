package com.payment.domain.idempotency.repository;

import com.payment.domain.idempotency.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * IdempotencyKey Repository
 * 
 * 중복 결제 방지를 위한 저장소
 */
@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    
    /**
     * Idempotency Key 값으로 조회
     * 중복 요청 감지 시 호출
     */
    Optional<IdempotencyKey> findByKey(String key);
}
