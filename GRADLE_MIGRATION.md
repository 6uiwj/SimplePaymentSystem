# Maven → Gradle 마이그레이션 가이드

## 변경 사항

### 1. 빌드 파일

- ❌ `pom.xml` (XML 기반 Maven)
- ✅ `build.gradle` (Groovy 기반 Gradle)
- ✅ `settings.gradle` (프로젝트 구성)
- ✅ `gradle/wrapper/gradle-wrapper.properties` (Gradle Wrapper)

### 2. 빌드 명령어

#### Maven

```bash
# 빌드
mvn clean package

# 실행
mvn spring-boot:run

# 테스트
mvn test

# 의존성 확인
mvn dependency:tree
```

#### Gradle

```bash
# 빌드 (JAR 생성)
gradle bootJar

# 또는 Wrapper 사용 (로컬)
./gradlew bootJar

# 실행
gradle bootRun

# 또는
./gradlew bootRun

# 테스트
gradle test

# 의존성 확인
gradle dependencies
```

### 3. Docker 이미지 업데이트

#### 이전 (Maven)

```dockerfile
FROM maven:3.8.1-openjdk-17      # 900MB
FROM openjdk:17-slim              # 400MB
```

#### 현재 (Gradle + Alpine)

```dockerfile
FROM gradle:8-jdk17-alpine        # 450MB (더 가볍고 빠름)
FROM openjdk:17-alpine            # 200MB (더 가볍고 안정적)
```

**장점**:

- Alpine은 musl 기반으로 매우 가슍
- 빌드 시간이 더 빠름
- 이미지 크기가 작음

### 4. 로컬 개발 설정

#### 방법 1: Gradle 설치 (권장)

```bash
# Windows (Chocolatey)
choco install gradle

# macOS (Homebrew)
brew install gradle

# Ubuntu/Debian
sudo apt-get install gradle
```

#### 방법 2: Gradle Wrapper 사용 (설치 불필요)

```bash
# Windows
gradlew bootRun

# macOS/Linux
./gradlew bootRun
```

### 5. IDE 설정

#### IntelliJ IDEA

- File → Project Structure
- Gradle 자동 감지
- Build Tool을 Gradle로 설정

#### VS Code

- Extension: Gradle Tasks ($ms-python)
- 또는 터미널에서 직접 실행

### 6. Docker Compose 실행

```bash
# 전체 시스템 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f backend

# 백엔드 빌드 상태 확인
docker-compose logs -f backend | grep -i "started\|error"
```

### 7. Gradle vs Maven 비교

| 항목      | Maven          | Gradle                       |
| --------- | -------------- | ---------------------------- |
| 설정 파일 | pom.xml (XML)  | build.gradle (Groovy/Kotlin) |
| 가독성    | 장황함         | 간결함                       |
| 빌드 속도 | 느림           | 빠름 (캐싱)                  |
| 유연성    | 제한적         | 높음                         |
| 학습곡선  | 완만           | 가파름                       |
| 커뮤니티  | 크고 많은 자료 | 급성장                       |

### 8. 의존성 추가 방법

#### Maven (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

#### Gradle (build.gradle)

```gradle
implementation 'org.springframework.boot:spring-boot-starter-web'
```

### 9. 주의사항

⚠️ **Windows에서 gradlew 실행 권한**

```bash
# 첫 번째 실행 시 권한 문제가 생기면
icacls "backend\gradlew.bat" /grant Everyone:F
```

⚠️ **Docker 빌드 시간**

- 첫 번째 빌드: 5-10분 (의존성 다운로드)
- 이후 빌드: 1-2분 (캐싱)

⚠️ **Gradle 데몬**

```bash
# 백그라운드 프로세스가 실행 중일 때 사용
gradle bootRun

# 데몬 종료
gradle --stop
```

### 10. 문제 해결

#### Gradle 캐시 초기화

```bash
gradle clean
# 또는
rm -rf .gradle build
```

#### 의존성 최신화

```bash
gradle dependencies
```

#### 빌드 재시도

```bash
gradle clean bootJar
```

---

**더 자세한 정보**: https://gradle.org/guides/
