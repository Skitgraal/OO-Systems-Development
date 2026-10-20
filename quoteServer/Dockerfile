# Build context: repository root (needs the shared quoteApi module)
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY settings.gradle build.gradle gradle.properties ./
COPY quoteApi quoteApi
COPY quoteServer quoteServer
RUN gradle :quoteServer:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/quoteServer/build/libs/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
