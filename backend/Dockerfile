# Build Stage
FROM maven:3.8.1-openjdk-17 AS builder

WORKDIR /build

# Copy Maven 설정 및 소스
COPY pom.xml .
COPY src ./src

# Maven 빌드
RUN mvn clean package -DskipTests

# Runtime Stage
FROM openjdk:17-slim

WORKDIR /app

# 빌드 결과 복사
COPY --from=builder /build/target/payment-system-1.0.0.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
