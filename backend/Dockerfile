# =========================
# 1단계: 빌드
# =========================
FROM gradle:8.5-jdk17-alpine AS builder

WORKDIR /build

# gradlew와 build.gradle 권한 설정
COPY . .
RUN chmod +x gradlew 2>/dev/null || true

# 의존성 캐싱
RUN gradle dependencies -x test --no-daemon || true

# Gradle 빌드 (verbose 모드 줄임)
RUN gradle bootJar -x test --no-daemon --warn || gradle bootJar -x test --no-daemon

# =========================
# 2단계: 실행
# =========================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# jar 복사
COPY --from=builder /build/build/libs/*.jar app.jar

# 포트
EXPOSE 8080

# 헬스체크 제거 (복잡성 줄임)
CMD ["java", "-jar", "app.jar"]

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]