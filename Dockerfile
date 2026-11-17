# Spring Boot 이미지. Railway는 저장소 루트의 Dockerfile을 자동으로 사용합니다.
#   docker build -t agency-be .
#   필요한 환경변수는 README의 "배포 (Railway)" 참고

# ---- build: JDK 17 + Gradle Wrapper로 bootJar ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# 의존성 레이어 캐시: 소스만 바뀌면 의존성을 다시 받지 않음
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- run: JRE 17 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
# SPRING_PROFILES_ACTIVE=prod: 기본 프로필 local(개발용 DB·시드·JWT 키)로 뜨지 않게 함.
# prod용 yml은 따로 없고, 설정은 모두 환경변수로 받습니다. 포트는 Railway가 주는 PORT를 씁니다.
ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
