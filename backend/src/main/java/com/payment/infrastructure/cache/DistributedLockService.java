package com.payment.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * DistributedLockService - Redis 기반 분산 락
 * 
 * 역할:
 * - 분산된 환경에서 동시성 제어
 * - 중복 결제 방지
 * - 여러 인스턴스에서의 동시 요청 관리
 * 
 * 이유:
 * - 단일 인스턴스에서는 synchronized 사용
 * - 하지만 멀티 인스턴스 환경에서는 Redis 기반 분산 락 필수
 * - 금융거래 특성상 중복 결제는 절대 안 됨
 * 
 * 예시:
 * - 동일한 idempotencyKey로 동시에 2개의 요청
 * - 첫 번째 요청이 락 획득 → 결제 진행
 * - 두 번째 요청은 대기 또는 거절
 * - 첫 번째 완료 후 두 번째 요청은 멱등성 키로 기존 결과 반환
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * 분산 락 획득
     * 
     * @param key 락 키 (고유한 식별자)
     * @param timeoutSeconds 타임아웃 초 (자동으로 락 해제)
     * @return 락 획득 성공 여부
     */
    public boolean acquireLock(String key, long timeoutSeconds) {
        try {
            // 새로운 UUID 생성 (락 소유자 식별)
            String lockValue = UUID.randomUUID().toString();
            
            // Redis SET NX 명령
            // NX: 키가 없을 때만 저장 (원자적 연산)
            // EX: 자동 만료 시간 설정
            Boolean result = redisTemplate.opsForValue().setIfAbsent(
                key,
                lockValue,
                timeoutSeconds,
                TimeUnit.SECONDS
            );
            
            if (Boolean.TRUE.equals(result)) {
                log.debug("락 획득 성공: key={}", key);
                return true;
            }
            
            log.debug("락 획득 실패: key={}", key);
            return false;
            
        } catch (Exception e) {
            log.error("분산 락 획득 중 오류", e);
            return false;
        }
    }
    
    /**
     * 분산 락 해제
     * 
     * @param key 락 키
     */
    public void releaseLock(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("락 해제: key={}", key);
        } catch (Exception e) {
            log.error("분산 락 해제 중 오류", e);
        }
    }
    
    /**
     * 락 존재 여부 확인
     */
    public boolean hasLock(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("락 존재 여부 확인 중 오류", e);
            return false;
        }
    }
}
