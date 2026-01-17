# =========================
# 1) Build stage
# =========================
FROM gradle:8.7-jdk21 AS builder
WORKDIR /workspace

# 모든 파일 복사 및 권한 부여
COPY . .
RUN chmod +x gradlew

# 단일 프로젝트이므로 바로 bootJar 생성
RUN ./gradlew bootJar --no-daemon

# =========================
# 2) Runtime stage
# =========================
FROM amazoncorretto:21-al2023-headless
WORKDIR /app

# 빌드된 jar 파일 복사 (build/libs 폴더에서 직접 가져옴)
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
