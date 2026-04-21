# 🚀 빠른 시작 가이드

## Docker Compose로 실행 (가장 쉬움)

```bash
# 1. 프로젝트 디렉토리로 이동
cd payment

# 2. 전체 시스템 실행 (자동으로 모든 서비스 시작)
docker-compose up -d

# 3. 서비스 상태 확인
docker-compose ps

# 4. 로그 확인 (실시간)
docker-compose logs -f

# 5. 접속 URL
# - 프론트엔드: http://localhost:5173
# - 백엔드 API: http://localhost:8080/api
# - 데이터베이스: localhost:5432
# - Redis: localhost:6379
```

## 로컬 개발 환경 설정

### 1단계: Gradle 설치

```bash
# Windows (Chocolatey 필요)
choco install gradle

# macOS (Homebrew)
brew install gradle

# Linux
sudo apt-get install gradle

# 설치 확인
gradle --version
```

### 2단계: 데이터베이스 및 Redis 시작

```bash
# PostgreSQL 컨테이너
docker run -d \
  --name payment-db \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=payment_db \
  -p 5432:5432 \
  postgres:15-alpine

# Redis 컨테이너
docker run -d \
  --name payment-redis \
  -p 6379:6379 \
  redis:7-alpine

# 데이터베이스 스키마 초기화
sleep 5
psql -h localhost -U postgres -d payment_db < db/schema.sql
```

### 3단계: 백엔드 실행

```bash
cd backend

# 의존성 다운로드 및 실행
gradle bootRun

# 또는 JAR 빌드 후 실행
gradle bootJar
java -jar build/libs/payment-system-1.0.0.jar
```

### 4단계: 프론트엔드 실행 (다른 터미널)

```bash
cd frontend

# 의존성 설치
npm install

# 또는 yarn
yarn install

# 개발 서버 실행
npm run dev
```

## 자주 묻는 질문

### Q1: Docker 이미지 다운로드 실패

```
ERROR [backend internal] load metadata for docker.io/library/...
```

**해결책**:

```bash
# Docker 캐시 초기화
docker system prune -a

# 다시 시도
docker-compose up -d --pull always
```

### Q2: 포트가 이미 사용 중이면?

```bash
# 포트 확인
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # macOS/Linux

# 진행 중인 컨테이너 중지
docker-compose down

# docker-compose.yml에서 포트 변경
# 8080:8080 → 8081:8080
```

### Q3: Gradle 빌드가 느리면?

```bash
# 최초 빌드는 시간이 걸림 (의존성 다운로드)
# 이후 빌드는 캐시로 빠름

# 캐시 확인
du -sh ~/.gradle    # Linux/macOS
```

### Q4: 데이터베이스 초기화하고 싶으면?

```bash
# 컨테이너 중지 및 제거
docker-compose down -v

# 다시 시작 (자동 초기화)
docker-compose up -d
```

## 테스트

```bash
# 백엔드 단위 테스트
cd backend
gradle test

# 테스트 리포트
gradle test
# 리포트 위치: build/reports/tests/test/index.html
```

## 프로덕션 빌드

```bash
# 1. JAR 파일 생성
cd backend
gradle clean bootJar

# 2. Docker 이미지 빌드
docker build -t payment-system:latest .

# 3. 이미지 실행
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/payment_db \
  -e TOSS_SECRET_KEY=your-key \
  payment-system:latest
```

## 모니터링

```bash
# 로그 확인
docker-compose logs backend -f

# 성능 통계
docker stats

# 컨테이너 리소스 사용량
docker container ls -q | xargs docker inspect | grep -E '"MemoryStats"|"Name"'
```

## 유용한 명령어

```bash
# 모든 컨테이너 중지
docker-compose stop

# 모든 컨테이너 시작
docker-compose start

# 특정 서비스만 재시작
docker-compose restart backend

# 로그 보기 (마지막 100줄)
docker-compose logs -n 100 backend

# Gradle 의존성 트리 보기
cd backend && gradle dependencies --configuration runtimeClasspath
```

---

**더 자세한 정보**: [GRADLE_MIGRATION.md](./GRADLE_MIGRATION.md)
