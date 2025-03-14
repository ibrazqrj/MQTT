# Build-Stage
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /home/app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Runtime-Stage
FROM eclipse-temurin:17
WORKDIR /app
COPY --from=build /home/app/target/MqttJava-1.0-SNAPSHOT-shaded.jar /app/runner.jar
ENTRYPOINT ["java", "-jar", "/app/runner.jar"]
