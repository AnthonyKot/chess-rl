# Use OpenJDK 21 as base image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle.properties ./

# Copy source code
COPY chess-engine/ chess-engine/
COPY nn-package/ nn-package/
COPY rl-framework/ rl-framework/
COPY integration/ integration/

# Copy config files
COPY config/ config/

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew :integration:build -x test

# Create directory for checkpoints and logs
RUN mkdir -p /app/checkpoints /app/logs

# Set environment variables for cloud training
ENV JAVA_OPTS="-Xmx4g -XX:+UseG1GC"
ENV GRADLE_OPTS="-Xmx2g"

# Default command - can be overridden
CMD ["./gradlew", ":integration:runCli", "--args=--train --config config/train-vs-minimax.yaml --max-steps 200 --games-per-cycle 50 --max-concurrent-games 4"]
