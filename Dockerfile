# 1. 빌드 단계 (Java 21 기준, 버전이 다르면 수정)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 빌드에 필요한 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# gradlew 실행 권한 부여 및 빌드
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar

# 2. 실행 단계
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드 단계에서 생성된 jar 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 8080 포트 개방 (파싱 서버 포트에 맞춰 수정)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
