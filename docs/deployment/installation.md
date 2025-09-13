# Installation Guide

## Overview

This guide covers installation and setup for the Chess RL Bot system in both development and production environments. The system supports JVM (recommended for training) and native compilation (for deployment).

## System Requirements

### Minimum Requirements
- **Operating System**: macOS 10.14+, Linux (Ubuntu 18.04+), Windows 10+
- **Memory**: 4GB RAM (8GB recommended for training)
- **Storage**: 2GB free space (10GB recommended for training data)
- **Java**: JDK 11 or higher (for JVM target)

### Recommended Requirements
- **Operating System**: macOS 12+, Linux (Ubuntu 20.04+)
- **Memory**: 16GB RAM (for large-scale training)
- **Storage**: 50GB free space (for extensive training sessions)
- **CPU**: Multi-core processor (4+ cores recommended)
- **Java**: JDK 17 or higher

## Development Environment Setup

### 1. Prerequisites Installation

#### macOS
```bash
# Install Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java
brew install openjdk@17

# Install Kotlin/Native dependencies
brew install llvm

# Set JAVA_HOME
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17' >> ~/.zshrc
source ~/.zshrc
```

#### Linux (Ubuntu/Debian)
```bash
# Update package list
sudo apt update

# Install Java
sudo apt install openjdk-17-jdk

# Install build tools
sudo apt install build-essential

# Install Kotlin/Native dependencies
sudo apt install llvm-dev

# Set JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

#### Windows
```powershell
# Install Java using Chocolatey
choco install openjdk17

# Install Visual Studio Build Tools
choco install visualstudio2019buildtools

# Set JAVA_HOME
setx JAVA_HOME "C:\Program Files\OpenJDK\openjdk-17"
```

### 2. Project Setup

#### Clone the Repository
```bash
git clone <repository-url>
cd chess-rl-bot
```

#### Verify Installation
```bash
# Check Java installation
java -version
javac -version

# Verify Gradle wrapper
./gradlew --version

# Run initial build
./gradlew build
```

### 3. IDE Setup

#### IntelliJ IDEA (Recommended)
1. Install IntelliJ IDEA Community or Ultimate
2. Install Kotlin plugin (usually pre-installed)
3. Open the project directory
4. Configure JDK in Project Settings
5. Enable Kotlin Multiplatform support

#### VS Code
1. Install VS Code
2. Install Kotlin extension
3. Install Gradle extension
4. Open project folder
5. Configure Java path in settings

## Build Configuration

### 1. Gradle Configuration

The project uses Kotlin Multiplatform with the following targets:

```kotlin
// build.gradle.kts
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}
```

### 2. Build Commands

#### Development Build (JVM)
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :chess-engine:build
./gradlew :nn-package:build
./gradlew :rl-framework:build
./gradlew :integration:build

# Run tests
./gradlew test

# Run specific test suite
./gradlew :integration:test
```

#### Production Build (Native)
```bash
# Build native executable
./gradlew nativeBinaries

# Build optimized release
./gradlew linkReleaseExecutableNative

# Create distribution
./gradlew assembleDist
```

### 3. Performance Optimization

#### JVM Optimization (Recommended for Training)
```bash
# Set JVM options for training
export JAVA_OPTS="-Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run with optimized JVM settings
./gradlew run -Dkotlin.compiler.execution.strategy=in-process
```

#### Native Optimization (For Deployment)
```bash
# Build with optimizations
./gradlew linkReleaseExecutableNative -Pkotlin.native.optimizationMode=OPT
```

## Production Environment Setup

### 1. Server Requirements

#### Minimum Production Server
- **CPU**: 4 cores, 2.5GHz+
- **Memory**: 8GB RAM
- **Storage**: 20GB SSD
- **Network**: 100Mbps connection
- **OS**: Linux (Ubuntu 20.04 LTS recommended)

#### Recommended Production Server
- **CPU**: 8+ cores, 3.0GHz+
- **Memory**: 32GB RAM
- **Storage**: 100GB NVMe SSD
- **Network**: 1Gbps connection
- **OS**: Linux (Ubuntu 22.04 LTS)

### 2. Production Installation

#### Automated Installation Script
```bash
#!/bin/bash
# production-install.sh

set -e

echo "Installing Chess RL Bot - Production Environment"

# Update system
sudo apt update && sudo apt upgrade -y

# Install Java
sudo apt install -y openjdk-17-jdk

# Create application user
sudo useradd -m -s /bin/bash chessrl
sudo mkdir -p /opt/chessrl
sudo chown chessrl:chessrl /opt/chessrl

# Download and extract application
cd /opt/chessrl
sudo -u chessrl wget <release-url>
sudo -u chessrl tar -xzf chess-rl-bot-*.tar.gz

# Set permissions
sudo chmod +x /opt/chessrl/bin/chess-rl-bot

# Create systemd service
sudo tee /etc/systemd/system/chessrl.service > /dev/null <<EOF
[Unit]
Description=Chess RL Bot Training System
After=network.target

[Service]
Type=simple
User=chessrl
WorkingDirectory=/opt/chessrl
ExecStart=/opt/chessrl/bin/chess-rl-bot
Restart=always
RestartSec=10
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
Environment=JAVA_OPTS=-Xmx16g -XX:+UseG1GC

[Install]
WantedBy=multi-user.target
EOF

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable chessrl
sudo systemctl start chessrl

echo "Installation completed successfully!"
```

#### Manual Production Installation
```bash
# 1. Create application directory
sudo mkdir -p /opt/chessrl
cd /opt/chessrl

# 2. Download release
wget <release-url>
tar -xzf chess-rl-bot-*.tar.gz

# 3. Set up configuration
cp config/production.conf config/application.conf

# 4. Create startup script
cat > start.sh << 'EOF'
#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export JAVA_OPTS="-Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
./bin/chess-rl-bot
EOF

chmod +x start.sh

# 5. Start application
./start.sh
```

### 3. Docker Installation

#### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN useradd -m -s /bin/bash chessrl

# Set working directory
WORKDIR /app

# Copy application files
COPY --chown=chessrl:chessrl . .

# Set permissions
RUN chmod +x gradlew

# Build application
RUN ./gradlew build

# Switch to application user
USER chessrl

# Expose port (if needed)
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xmx8g -XX:+UseG1GC"

# Start application
CMD ["./gradlew", "run"]
```

#### Docker Compose
```yaml
version: '3.8'

services:
  chessrl:
    build: .
    container_name: chess-rl-bot
    restart: unless-stopped
    environment:
      - JAVA_OPTS=-Xmx8g -XX:+UseG1GC
      - TRAINING_CONFIG=/app/config/production.conf
    volumes:
      - ./data:/app/data
      - ./logs:/app/logs
      - ./config:/app/config
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

#### Docker Commands
```bash
# Build image
docker build -t chess-rl-bot .

# Run container
docker run -d \
  --name chess-rl-bot \
  -p 8080:8080 \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/logs:/app/logs \
  chess-rl-bot

# Using Docker Compose
docker-compose up -d

# View logs
docker logs chess-rl-bot

# Stop container
docker stop chess-rl-bot
```

## Configuration

### 1. Environment Configuration

#### Development Configuration
```properties
# config/development.conf
training.episodes=1000
training.batchSize=32
training.learningRate=0.001
training.explorationRate=0.2

network.hiddenLayers=[256, 128]
network.optimizer=ADAM

logging.level=DEBUG
logging.file=logs/development.log

performance.useJVMOptimization=true
performance.parallelGames=1
```

#### Production Configuration
```properties
# config/production.conf
training.episodes=50000
training.batchSize=128
training.learningRate=0.0005
training.explorationRate=0.1

network.hiddenLayers=[512, 256, 128]
network.optimizer=ADAM

logging.level=INFO
logging.file=logs/production.log

performance.useJVMOptimization=true
performance.parallelGames=4

monitoring.enabled=true
monitoring.metricsInterval=60
```

### 2. System Configuration

#### JVM Configuration
```bash
# Set in environment or startup script
export JAVA_OPTS="
  -Xmx16g
  -Xms4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:G1HeapRegionSize=16m
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
"
```

#### Native Configuration
```bash
# For native builds
export KOTLIN_NATIVE_OPTS="
  -Xmx8g
  -opt
  -g
"
```

## Verification

### 1. Installation Verification

#### Basic Functionality Test
```bash
# Run quick integration test
./gradlew :integration:test --tests QuickIntegrationTest

# Verify all modules
./gradlew build

# Check system resources
./gradlew run --args="--test-system"
```

#### Performance Verification
```bash
# Run performance benchmarks
./gradlew :integration:test --tests PerformanceBenchmarkTest

# Verify JVM optimization
./gradlew run --args="--benchmark-jvm"

# Test native performance
./gradlew linkReleaseExecutableNative
./build/bin/native/releaseExecutable/chess-rl-bot.kexe --benchmark-native
```

### 2. Health Checks

#### System Health Check
```kotlin
// Health check endpoint
class HealthCheck {
    fun checkSystem(): HealthStatus {
        return HealthStatus(
            jvmMemory = checkJVMMemory(),
            diskSpace = checkDiskSpace(),
            neuralNetwork = checkNeuralNetwork(),
            chessEngine = checkChessEngine(),
            rlFramework = checkRLFramework()
        )
    }
}
```

#### Monitoring Setup
```bash
# Create monitoring script
cat > monitor.sh << 'EOF'
#!/bin/bash
while true; do
    echo "$(date): Checking system health..."
    curl -f http://localhost:8080/health || echo "Health check failed"
    sleep 60
done
EOF

chmod +x monitor.sh
./monitor.sh &
```

## Troubleshooting

### Common Installation Issues

#### Java Version Issues
```bash
# Check Java version
java -version

# If wrong version, set JAVA_HOME
export JAVA_HOME=/path/to/java-17

# Verify Gradle uses correct Java
./gradlew -version
```

#### Build Failures
```bash
# Clean build
./gradlew clean build

# Build with debug info
./gradlew build --debug --stacktrace

# Check dependencies
./gradlew dependencies
```

#### Memory Issues
```bash
# Increase Gradle memory
export GRADLE_OPTS="-Xmx4g"

# Increase JVM memory for application
export JAVA_OPTS="-Xmx8g"
```

#### Native Compilation Issues
```bash
# Check native toolchain
./gradlew tasks --all | grep native

# Verify LLVM installation
llvm-config --version

# Clean native build
./gradlew clean linkReleaseExecutableNative
```

### Performance Issues

#### Slow Training
- Verify JVM optimization is enabled
- Check memory allocation
- Monitor CPU usage
- Consider reducing batch size

#### High Memory Usage
- Adjust experience buffer size
- Enable garbage collection tuning
- Monitor memory leaks
- Use memory profiling tools

This installation guide provides comprehensive setup instructions for both development and production environments with detailed troubleshooting guidance.