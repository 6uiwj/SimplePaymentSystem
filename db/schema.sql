-- Payment System Database Schema
-- PostgreSQL based on DDD + 4-layered architecture

-- 1. PAYMENTS 테이블 (결제 - Aggregate Root)
CREATE TABLE payments (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,
    
    -- 결제 고유 식별자 (출장 API 응답용)
    payment_key VARCHAR(255) NOT NULL UNIQUE,
    
    -- 비즈니스 주문 ID
    order_id VARCHAR(255) NOT NULL,
    
    -- 중복 결제 방지 키 (Idempotency)
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    
    -- 결제 금액 (정확성: 소수점 2자리까지)
    amount NUMERIC(19, 2) NOT NULL,
    
    -- 통화 코드
    currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    
    -- 결제 상태 (READY, REQUESTED, SUCCESS, FAIL)
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    
    -- 상품명
    product_name VARCHAR(255) NOT NULL,
    
    -- 구매자 정보
    customer_email VARCHAR(255) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20),
    
    -- 외부 결제 API ID (토스페이먼츠)
    external_payment_id VARCHAR(255) UNIQUE,
    
    -- 실패 사유
    failure_reason TEXT,
    
    -- 재시도 횟수
    retry_count INT NOT NULL DEFAULT 0,
    
    -- 타임스탐프
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- 인덱스 (조회 성능)
    UNIQUE(payment_key),
    UNIQUE(idempotency_key),
    UNIQUE(external_payment_id),
    INDEX idx_payment_status (status),
    INDEX idx_payment_order_id (order_id),
    INDEX idx_payment_created_at (created_at DESC),
    INDEX idx_payment_retry (status, retry_count),
    
    -- 제약조건 (데이터 무결성)
    CONSTRAINT chk_amount CHECK (amount > 0),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0 AND retry_count <= 3),
    CONSTRAINT chk_status CHECK (status IN ('READY', 'REQUESTED', 'SUCCESS', 'FAIL'))
);

-- 2. PAYMENT_LOGS 테이블 (결제 이력 - Event Log)
CREATE TABLE payment_logs (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,
    
    -- Payment 외래키
    payment_id BIGINT NOT NULL,
    
    -- 결제 키 (조회 편의성)
    payment_key VARCHAR(255) NOT NULL,
    
    -- 이벤트 타입
    -- CREATED, REQUESTED, SUCCESS, FAIL, RETRY, WEBHOOK_RECEIVED
    event_type VARCHAR(30) NOT NULL,
    
    -- 이벤트 설명
    description TEXT,
    
    -- 외부 API 응답 코드
    response_code VARCHAR(50),
    
    -- 외부 API 응답 메시지
    response_message TEXT,
    
    -- 타임스탐프
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 인덱스
    INDEX idx_payment_log_payment_id (payment_id),
    INDEX idx_payment_log_payment_key (payment_key),
    INDEX idx_payment_log_event_type (event_type),
    INDEX idx_payment_log_created_at (created_at DESC),
    
    -- 외래키 (선택적: 데이터 삭제 시 로그는 유지하고 싶으면 제거)
    -- FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
    
    -- 제약조건
    CONSTRAINT chk_event_type CHECK (event_type IN 
        ('CREATED', 'REQUESTED', 'SUCCESS', 'FAIL', 'RETRY', 'WEBHOOK_RECEIVED')
    )
);

-- 3. IDEMPOTENCY_KEYS 테이블 (중복 결제 방지)
CREATE TABLE idempotency_keys (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,
    
    -- Idempotency Key 값 (클라이언트에서 생성한 고유값)
    key VARCHAR(255) NOT NULL UNIQUE,
    
    -- 이 키로 이미 처리된 결제 ID
    payment_id BIGINT NOT NULL,
    
    -- 결제 상태 스냅샷
    status VARCHAR(20) NOT NULL,
    
    -- 생성 시간
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 만료 시간 (24시간 후 자동 만료)
    expires_at TIMESTAMP NOT NULL,
    
    -- 인덱스
    INDEX idx_idempotency_key (key),
    INDEX idx_idempotency_expires_at (expires_at),
    
    -- 외래키
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

-- 4. 뷰: 최근 결제 현황 (관리자 대시보드용)
CREATE VIEW recent_payment_status AS
SELECT 
    p.id,
    p.payment_key,
    p.order_id,
    p.amount,
    p.status,
    p.product_name,
    p.customer_name,
    p.customer_email,
    p.retry_count,
    p.created_at,
    p.completed_at,
    COUNT(pl.id) as log_count
FROM payments p
LEFT JOIN payment_logs pl ON p.id = pl.payment_id
WHERE p.created_at >= NOW() - INTERVAL '7 days'
GROUP BY p.id
ORDER BY p.created_at DESC;

-- 5. 인덱스 생성 정책
-- 혹시 MySQL 대신 다른 DB를 사용하는 경우: 수정 필요

-- 재시도 필요한 결제 조회용 인덱스 (배치 작업)
CREATE INDEX idx_retry_candidates ON payments(status, retry_count, updated_at)
WHERE status = 'FAIL' AND retry_count < 3;

-- 시간별 결제액 집계용 인덱스
CREATE INDEX idx_success_payments ON payments(status, created_at)
WHERE status = 'SUCCESS';

-- 6. 초기 데이터
-- 없음 (시스템 시작 시 자동 생성)

-- 7. 마이그레이션 히스토리
-- 이 파일이 초기 스키마입니다.
-- 향후 변경 사항은 V002, V003... 파일로 관리
