package com.payment.domain.idempotency.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * IdempotencyKey 엔티티
 * 
 * 역할:
 * - 중복 결제 방지 메커니즘
 * - 같은 idempotencyKey로 요청 시 동일한 결과 반환
 * 
 * 이유:
 * - 결제는 금융거래이므로 중복 방지가 매우 중요
 * - 네트워크 장애로 인한 재전송 시에도 안전해야 함
 * - 예: 사용자가 "결제" 버튼을 두 번 클릭한 경우
 * - 클라이언트가 동일한 idempotencyKey로 재시도하면 
 *   중복된 결제가 아닌 기존 결과를 반환
 * 
 * 구조:
 * - key: 클라이언트에서 생성한 고유 키 (UUID)
 * - paymentId: 이 키로 이미 결제가 되었다면 그 결제의 ID
 * - status: 결제 상태
 * - 저장 기간: 24시간 (설정에서 조정 가능)
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Idempotency Key 값
     * 클라이언트에서 생성하여 요청 헤더에 전달
     */
    @Column(nullable = false, unique = true, length = 255)
    private String key;
    
    /**
     * 이미 처리된 결제의 ID
     * 같은 키로 다시 요청 시 이 결제를 반환
     */
    @Column(nullable = false)
    private Long paymentId;
    
    /**
     * 결제 상태 (스냅샷)
     * 응답에 포함시킬 결제 상태를 빠르게 확인
     */
    @Column(nullable = false, length = 20)
    private String status;
    
    /**
     * 생성 시간
     * TTL 계산에 사용 (24시간 후 만료)
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /**
     * 저장 시간 (TTL 관련)
     * Redis에서는 EXPIRE 커맨드로 설정
     * DB에서는 배치 작업으로 정리
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * 팩토리 메서드
     * TTL은 설정에서 읽어 올 수 있음 (기본값 24시간)
     */
    public static IdempotencyKey create(String key, Long paymentId, String status, Integer ttlHours) {
        IdempotencyKey idempotencyKey = new IdempotencyKey();
        idempotencyKey.key = key;
        idempotencyKey.paymentId = paymentId;
        idempotencyKey.status = status;
        idempotencyKey.createdAt = LocalDateTime.now();
        idempotencyKey.expiresAt = LocalDateTime.now().plusHours(ttlHours);
        return idempotencyKey;
    }
    
    /**
     * 만료되었는지 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
