# Build Stage
FROM node:18-alpine AS builder

WORKDIR /build

# 의존성 설치
COPY package*.json ./
RUN npm ci

# 소스 복사 및 빌드
COPY . .
RUN npm run build

# Runtime Stage
FROM node:18-alpine

WORKDIR /app

# 의존성 설치 (production only)
COPY package*.json ./
RUN npm ci --only=production

# 빌드 결과 복사
COPY --from=builder /build/dist ./dist

# 개발 서버 실행
EXPOSE 5173

CMD ["npm", "run", "dev"]
