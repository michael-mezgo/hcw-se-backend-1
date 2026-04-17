# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM --platform=$BUILDPLATFORM gradle:8.14-jdk21 AS builder

WORKDIR /app

# Cache dependencies before copying the full source
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon

# Build the fat JAR
COPY src ./src
RUN gradle buildFatJar --no-daemon

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the fat JAR produced by the Ktor Gradle plugin
COPY --from=builder /app/build/libs/*-all.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
