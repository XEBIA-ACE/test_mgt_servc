# ============================================================
# Multi-stage Dockerfile for User Management Service
# ============================================================

# ---- Stage 1: Build ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build

# Copy Maven wrapper and POM first (enables Docker layer caching for dependencies)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (this layer is cached as long as pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code and build
COPY src ./src
RUN ./mvnw package -DskipTests -B --no-transfer-progress

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Add a non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Install curl for health check
RUN apk add --no-cache curl

# Copy the built JAR from the build stage
COPY --from=build /build/target/*.jar app.jar

# Set correct ownership
RUN chown -R appuser:appgroup /app

# Run as non-root
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
