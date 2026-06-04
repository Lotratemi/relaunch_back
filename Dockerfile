# syntax=docker/dockerfile:1

# ---- Build stage ----
# Uses the Gradle wrapper bundled in the repo so the Gradle version always
# matches what the project expects (currently 9.3.0).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Cache dependencies: copy only the build scripts + wrapper first.
COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
# Strip Windows CRLF line endings so the wrapper's shebang resolves under sh.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# Copy the rest of the sources and build the runnable distribution.
COPY src src
RUN ./gradlew --no-daemon clean installDist -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN useradd --system --uid 1001 --create-home appuser
USER appuser

# The installDist task lays out an executable + its libs under
# build/install/<rootProject.name> (rootProject.name = "relaunch-back").
COPY --from=build --chown=appuser:appuser /app/build/install/relaunch-back/ ./

# Render injects the port via $PORT; application.yaml reads it (defaults to 8080).
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["./bin/relaunch-back"]
