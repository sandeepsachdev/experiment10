# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache deps
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd -ms /bin/bash spring
USER spring

COPY --from=build /workspace/target/sydney-events.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
# Render injects PORT; Spring reads it via SERVER_PORT
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
