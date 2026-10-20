# Build context: repository root (needs the shared quoteApi module)
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle gradle.properties ./
COPY quoteApi quoteApi
COPY portfolioService portfolioService
RUN gradle :portfolioService:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/portfolioService/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
