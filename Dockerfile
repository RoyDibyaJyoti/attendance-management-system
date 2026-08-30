# ==============================================================================
# AMCS Production Dockerfile
# Multi-stage, minimal attack-surface, non-root Java 21 container
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build & Package Artifact
# ------------------------------------------------------------------------------
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable fat JAR without running tests (tests run in CI)
COPY src ./src
RUN mvn package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install wget for healthcheck if not present and create dedicated non-root user
RUN apk add --no-cache wget && \
    addgroup -S -g 10001 amcs && \
    adduser -S -u 10001 -G amcs -s /sbin/nologin amcs

WORKDIR /app

# Copy packaged fat JAR from builder stage
COPY --from=builder --chown=amcs:amcs /build/target/attendance-domain-*.jar /app/app.jar

# Enforce non-root execution
USER amcs:amcs

# Application port
EXPOSE 8080

# Production container healthcheck probing Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=25s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Production JVM flags for low memory footprint and container awareness
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "/app/app.jar"]
