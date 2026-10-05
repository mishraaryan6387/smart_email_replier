# ==========================================
# Multi-Stage Dockerfile for Spring Boot Backend
# ==========================================

# Stage 1: Build the JAR package
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Render provides the port in the $PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Tune JVM memory for container environments (e.g. Render 512MB RAM tier)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
