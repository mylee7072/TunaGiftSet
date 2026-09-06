# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1: build the boot jar with the project's own Gradle wrapper (never the
# host's Gradle, never whatever happens to be on the builder machine).
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Copy only what's needed to resolve dependencies first, so this (slow) layer is
# cached and skipped on rebuilds that only touch src/.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now bring in the actual source and build. Tests are skipped here deliberately —
# they're expected to have already been run via `./gradlew test` as part of the normal
# dev workflow (see README); this keeps image builds fast and repeatable rather than
# silently dropping test coverage from the project.
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---------------------------------------------------------------------------
# Stage 2: minimal runtime — JRE only, no JDK/Gradle/source ever ships in this image.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# curl is the only extra package added, solely so the container's own HEALTHCHECK
# (and Compose's) can call /actuator/health without shelling out to anything heavier.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring --home-dir /app spring

COPY --from=build /workspace/build/libs/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD curl -f http://127.0.0.1:8080/actuator/health || exit 1

# JAVA_TOOL_OPTIONS (e.g. -Xmx512m) is picked up automatically by the JVM when set on the
# container — no tuning is hardcoded here, see the completion report for guidance.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
