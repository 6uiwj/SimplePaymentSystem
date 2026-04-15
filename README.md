# Spring Boot 모바일 결제 시스템

Spring Boot 4 + DDD + 4-layered Architecture 기반의 모바일 결제 시스템입니다.

## 📋 목차

- [시스템 아키텍처](#-시스템-아키텍처)
- [기술 스택](#-기술-스택)
- [설치 및 실행](#-설치-및-실행)
- [API 문서](#-api-문서)
- [핵심 개념](#-핵심-개념)
- [결제 흐름](#-결제-흐름)

## 🏗️ 시스템 아키텍처

### 백엔드 구조 (4-Layered + DDD)

```
Presentation Layer (Controller, Webhook)
    ↓
Application Layer (Service, DTO)
    ↓
Domain Layer (Entity, Repository, Value Object)
    ↓
Infrastructure Layer (Database, External API, Cache)
```

### 병렬 계층별 설명

| 계층 | 역할 | 예시 |
|------|------|------|
| **Presentation** | HTTP 요청/응답 처리 | `PaymentController`, `WebhookController` |
| **Application** | Use case 로직, 트랜잭션 | `PaymentService`, DTO |
| **Domain** | 비즈니스 규칙 | `Payment`, `PaymentStatus`, Repository interface |
| **Infrastructure** | 외부 시스템 연동 | `TossPaymentsGateway`, `RedisTemplate`, `JPA` |

### 왜 이 구조인가?

**DDD (Domain-Driven Design)**
- 비즈니스 속성이 도메인 엔티티에 집중
- 도메인 로직이 명확하고 테스트 가능
- 향후 비즈니스 변경에 유연하게 대응

**4-Layered Architecture**
- 각 계층이 독립적 → 기술 변경이 용이 (DB 변경, API 변경 등)
- 단일 책임 원칙 준수
- 테스트 용이

## 🛠️ 기술 스택

### 백엔드
- **Java 17** - 최신 안정 버전, 레코드 타입 사용 가능
- **Spring Boot 4** - 자동 설정, 빠른 개발
- **Spring Data JPA** - ORM, 데이터 영속성
- **Spring Security** - 인증/인가 (확장 가능)
- **PostgreSQL** - RDBMS, 금융거래에 적합
- **Redis** - 분산 락(동시성 제어), 캐싱, Idempotency
- **Lombok** - 보일러플레이트 코드 감소

### 프론트엔드
- **React 18** - 최신 UI 라이브러리
- **Vite** - 빠른 개발 서버
- **Zustand** - 글로벌 상태 관리 (Redux보다 간단)
- **Axios** - HTTP 클라이언트
- **React Router** - SPA 라우팅

### DevOps
- **Docker** - 컨테이너화
- **Docker Compose** - 멀티 컨테이너 오케스트레이션

## 📦 설치 및 실행

### 전제 조건

- Docker & Docker Compose
- 또는 Java 17, Node.js 18, PostgreSQL 15, Redis 7

### 방법 1: Docker Compose (권장)

```bash
# 클론
git clone <repository-url>
cd payment

# 환경 변수 설정
cp .env.example .env
# .env 파일 수정: TOSS_SECRET_KEY 등 설정

# 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# URL
# - 프론트엔드: http://localhost:5173
# - 백엔드: http://localhost:8080/api
# - 데이터베이스: localhost:5432
# - Redis: localhost:6379
```

### 방법 2: 로컬 실행

#### 1. 데이터베이스 및 Redis 실행

```bash
# PostgreSQL
docker run -d \
  --name payment-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=payment_db \
  -p 5432:5432 \
  postgres:15-alpine

# Redis
docker run -d \
  --name payment-redis \
  -p 6379:6379 \
  redis:7-alpine

# DB 스키마 생성
psql -h localhost -U postgres -d payment_db < db/schema.sql
```

#### 2. 백엔드 실행

```bash
cd backend

# Maven 빌드
mvn clean package

# 실행
mvn spring-boot:run
# 또는
java -jar target/payment-system-1.0.0.jar
```

#### 3. 프론트엔드 실행

```bash
cd frontend

# 의존성 설치
npm install

# 개발 서버 실행
npm run dev
```

## 📡 API 문서

### 결제 요청 생성
```
POST /api/payments
Content-Type: application/json

{
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",  // UUID
  "orderId": "order-12345",
  "amount": 10000,
  "productName": "아이스크림",
  "customerEmail": "user@example.com",
  "customerName": "홍길동",
  "customerPhone": "01012345678",
  "paymentMethodKey": "toss-card-key-xxxxx"
}

응답:
{
  "success": true,
  "data": {
    "paymentKey": "550e8400-e29b-41d4-a716-446655440001",
    "orderId": "order-12345",
    "amount": 10000,
    "productName": "아이스크림",
    "redirectUrl": "https://payment.example.com/checkout?key=550e8400-e29b-41d4-a716-446655440001"
  },
  "message": "결제 요청이 생성되었습니다"
}
```

### 결제 상태 조회
```
GET /api/payments/{paymentKey}

응답:
{
  "success": true,
  "data": {
    "paymentKey": "550e8400-e29b-41d4-a716-446655440001",
    "status": "SUCCESS",
    "amount": 10000,
    "createdAt": "2025-04-15T10:30:00",
    "completedAt": "2025-04-15T10:31:00"
  }
}
```

### 결제 이력 조회
```
GET /api/payments/{paymentKey}/history

응답:
{
  "success": true,
  "data": [
    {
      "eventType": "CREATED",
      "description": "결제 생성",
      "createdAt": "2025-04-15T10:30:00"
    },
    {
      "eventType": "REQUESTED",
      "description": "결제 요청 전송",
      "createdAt": "2025-04-15T10:30:05"
    },
    {
      "eventType": "WEBHOOK_RECEIVED",
      "description": "Webhook으로 결제 승인 확인",
      "createdAt": "2025-04-15T10:30:10"
    }
  ]
}
```

## 💡 핵심 개념

### 1. Idempotency (멱등성)

**문제**: 네트워크 장애로 인한 재전송 시 중복 결제 발생

**해결**:
- 클라이언트: UUID로 생성한 `idempotencyKey` 전송
- 서버: 같은 키로 재요청하면 기존 결과 반환
- 24시간 후 자동 만료 (설정 가능)

```java
// 같은 idempotencyKey로 2번 요청 → 동일한 결과 반환
POST /api/payments {idempotencyKey: "abc123"}  // paymentKey: "payment1" 반환
POST /api/payments {idempotencyKey: "abc123"}  // paymentKey: "payment1" 반환 (중복 아님)
```

### 2. 분산 락 (Distributed Lock)

**문제**: 다중 인스턴스 환경에서 동시 요청 시 중복 결제 가능

**해결**:
- Redis의 SET NX 명령 사용
- 첫 번째 요청이 락 획득 → 결제 진행
- 두 번째 요청은 멱등성 키로 기존 결과 반환

```java
// Redis SET NX: atomicity 보장
SET "payment:idempotency:abc123" "uuid-value" NX EX 5
// 성공 시 → 결제 진행
// 실패 시 → 이미 처리 중, 대기/거절
```

### 3. 상태 관리 (State Machine)

결제는 다음과 같이 상태 전환:

```
READY → REQUESTED → SUCCESS
          ↓
          FAIL → READY (재시도)
```

- 유효하지 않은 전환은 예외 발생
- 도메인 엔티티에서 상태 관리

### 4. Webhook (비동기 결과 처리)

**흐름**:
1. 클라이언트 → 서버: 결제 요청
2. 서버 → 토스페이먼츠: 결제 승인 요청
3. 토스페이먼츠 → 서버: Webhook (결과 통지)
4. 서버: 서명 검증 후 상태 업데이트
5. 서버 → 클라이언트: 폴링 또는 푸시로 결과 전달

**서명 검증** (보안):
```
토스페이먼츠 서명 = HMAC-SHA256(Webhook Payload, Secret Key)
서버는 받은 서명과 계산한 서명 비교 → 위조 요청 방지
```

## 🔄 결제 흐름

### 모바일 결제 전체 과정

```
1. 사용자가 상품 선택 (프론트엔드)
   ↓
2. 결제 폼 입력 및 "결제하기" 버튼 클릭
   ↓
3. 클라이언트: UUID로 idempotencyKey 생성
   ↓
4. POST /api/payments 호출
   ├─ 서버: 분산 락 획득
   ├─ 서버: Payment 엔티티 생성 (READY 상태)
   ├─ 서버: IdempotencyKey 저장
   ├─ 서버: PaymentLog 기록
   └─ 응답: paymentKey, redirectUrl 반환
   ↓
5. 클라이언트: redirectUrl로 토스페이먼츠 결제 창 오픈
   ↓
6. 사용자: 카드 정보 입력 및 결제 (토스페이먼츠에서만)
   ↓
7. 토스페이먼츠: 결제 토큰 생성 → 클라이언트로 전달
   ↓
8. 클라이언트: POST /api/payments/{paymentKey}/approve + 토큰
   ├─ 서버: Payment 상태를 REQUESTED로 변경
   ├─ 서버: 토스페이먼츠 API 호출
   ├─ 서버: 결과에 따라 SUCCESS 또는 FAIL 상태로 변경
   ├─ 서버: PaymentLog 기록
   └─ 응답: 최종 결과 반환
   ↓
9. 클라이언트: 결과 페이지 표시
   - 성공: 감사 페이지
   - 실패: 재시도 옵션
   ↓
10. 동시에 토스페이먼츠 → 서버 Webhook으로도 결과 통지
    (클라이언트 네트워크가 끊겨도 결제 상태 반영)
```

## 🔒 보안 기능

### 1. 카드 정보 미저장
- 우리 서버는 카드 정보를 받지 않음
- 토스페이먼츠에서만 처리
- PCI DSS 준수

### 2. Webhook 서명 검증
- 모든 Webhook에 HMAC-SHA256 서명 포함
- 서명 검증으로 위조 요청 방지

### 3. Idempotency Key
- 중복 결제 방지
- 네트워크 장애 시서도 안전

### 4. 분산 락
- 다중 인스턴스에서의 데이터 일관성 보장

## 📊 모니터링

### 대시보드 조회
```bash
# 최근 결제 현황 (뷰 사용)
SELECT * FROM recent_payment_status ORDER BY created_at DESC;

# 실패한 결제 중 재시도 가능한 것
SELECT * FROM payments 
WHERE status = 'FAIL' AND retry_count < 3
ORDER BY updated_at ASC;

# 결제 현황 통계
SELECT status, COUNT(*) as count, SUM(amount) as total_amount
FROM payments
WHERE created_at >= NOW() - INTERVAL '7 days'
GROUP BY status;
```

## 🚀 배포

### 프로덕션 빌드

```bash
# 백엔드
cd backend
mvn clean package -DskipTests
docker build -t payment-backend:latest .

# 프론트엔드
cd frontend
npm run build
docker build -t payment-frontend:latest .
```

### 환경 변수 설정

```bash
# .env (프로덕션)
TOSS_SECRET_KEY=your-actual-secret-key
TOSS_WEBHOOK_SECRET=your-actual-webhook-secret
SPRING_DATASOURCE_PASSWORD=secure-password
SPRING_REDIS_PASSWORD=redis-password
```

## 📝 라이선스

MIT License

## 👨‍💻 개발자

- 이 프로젝트는 Spring Boot DDD 학습 용도로 구성됩니다

---

**질문이나 이슈가 있으면 GitHub Issues를 통해 문의하세요.**
