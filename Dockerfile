# Spring Boot 이미지 (SPRING_PROFILES_ACTIVE=prod)
#   docker build -t agency-be .
#   필요한 환경변수는 .env.prod.example 참고

# ---- build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# 의존성 레이어 캐시: 소스만 바뀌면 의존성을 다시 받지 않음
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
ENV TZ=Asia/Seoul \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
