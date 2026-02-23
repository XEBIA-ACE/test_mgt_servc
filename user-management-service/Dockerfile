# ============================================================
# Multi-stage Dockerfile for User Management Service
# ============================================================
# Stage 1: Build
# Stage 2: Runtime (minimal JRE image)
# ============================================================

# ----- Stage 1: Build -----
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Cache Maven dependencies separately from application code
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B 2>/dev/null || true

# Copy source and build (skip tests — run them in CI before building the image)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -DskipTests package --no-transfer-progress 2>/dev/null || \
    mvn  -B -DskipTests package --no-transfer-progress

# ----- Stage 2: Runtime -----
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /workspace/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Drop privileges
USER appuser

# JVM tuning for containers:
#   - UseContainerSupport  : respect cgroup memory/CPU limits
#   - MaxRAMPercentage     : use up to 75% of available container RAM for the JVM heap
#   - ExitOnOutOfMemoryError: fail fast instead of degrading silently
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
