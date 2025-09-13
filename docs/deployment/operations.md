# Operations Guide - Monitoring, Maintenance, and Troubleshooting

## Overview

This guide covers operational procedures for running the Chess RL Bot system in production, including monitoring, maintenance, troubleshooting, and performance optimization.

## System Monitoring

### 1. Health Monitoring

#### Health Check Endpoints
```kotlin
// Health check implementation
class HealthCheckService {
    fun getSystemHealth(): SystemHealth {
        return SystemHealth(
            overall = determineOverallHealth(),
            components = mapOf(
                "neural_network" to checkNeuralNetwork(),
                "chess_engine" to checkChessEngine(),
                "rl_framework" to checkRLFramework(),
                "training_pipeline" to checkTrainingPipeline(),
                "memory" to checkMemoryUsage(),
                "disk" to checkDiskSpace()
            ),
            metrics = getCurrentMetrics(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun checkNeuralNetwork(): ComponentHealth {
        return try {
            val testInput = DoubleArray(776) { 0.0 }
            val network = getCurrentNetwork()
            val output = network.predict(testInput)
            
            ComponentHealth(
                status = HealthStatus.HEALTHY,
                message = "Neural network responding normally",
                metrics = mapOf(
                    "prediction_time_ms" to measurePredictionTime(),
                    "memory_usage_mb" to getNetworkMemoryUsage()
                )
            )
        } catch (e: Exception) {
            ComponentHealth(
                status = HealthStatus.UNHEALTHY,
                message = "Neural network error: ${e.message}",
                error = e
            )
        }
    }
}
```

#### Health Check Script
```bash
#!/bin/bash
# health-check.sh

HEALTH_URL="http://localhost:8080/health"
LOG_FILE="/var/log/chessrl/health.log"

check_health() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local response=$(curl -s -w "%{http_code}" "$HEALTH_URL")
    local http_code="${response: -3}"
    local body="${response%???}"
    
    if [ "$http_code" = "200" ]; then
        echo "[$timestamp] HEALTHY: $body" >> "$LOG_FILE"
        return 0
    else
        echo "[$timestamp] UNHEALTHY: HTTP $http_code - $body" >> "$LOG_FILE"
        return 1
    fi
}

# Main health check loop
while true; do
    if ! check_health; then
        # Send alert
        echo "Health check failed at $(date)" | mail -s "Chess RL Bot Health Alert" admin@example.com
    fi
    sleep 60
done
```

### 2. Performance Monitoring

#### Key Performance Metrics
```kotlin
data class PerformanceMetrics(
    // Training metrics
    val episodesPerSecond: Double,
    val averageEpisodeLength: Double,
    val trainingLoss: Double,
    val winRate: Double,
    
    // System metrics
    val cpuUsage: Double,
    val memoryUsage: Long,
    val memoryUsagePercent: Double,
    val diskUsage: Long,
    val diskUsagePercent: Double,
    
    // Network metrics
    val predictionTimeMs: Double,
    val batchProcessingTimeMs: Double,
    val experienceBufferSize: Int,
    val experienceBufferUtilization: Double,
    
    // JVM metrics
    val heapUsed: Long,
    val heapMax: Long,
    val gcCount: Long,
    val gcTime: Long
)

class PerformanceMonitor {
    private val metrics = mutableListOf<PerformanceMetrics>()
    
    fun collectMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            episodesPerSecond = calculateEpisodesPerSecond(),
            averageEpisodeLength = calculateAverageEpisodeLength(),
            trainingLoss = getCurrentTrainingLoss(),
            winRate = getCurrentWinRate(),
            cpuUsage = getCPUUsage(),
            memoryUsage = getMemoryUsage(),
            memoryUsagePercent = getMemoryUsagePercent(),
            diskUsage = getDiskUsage(),
            diskUsagePercent = getDiskUsagePercent(),
            predictionTimeMs = getAveragePredictionTime(),
            batchProcessingTimeMs = getAverageBatchProcessingTime(),
            experienceBufferSize = getExperienceBufferSize(),
            experienceBufferUtilization = getExperienceBufferUtilization(),
            heapUsed = getHeapUsed(),
            heapMax = getHeapMax(),
            gcCount = getGCCount(),
            gcTime = getGCTime()
        )
    }
}
```

#### Monitoring Dashboard
```kotlin
class MonitoringDashboard {
    fun displayRealTimeMetrics() {
        val metrics = performanceMonitor.collectMetrics()
        
        println("""
        ╔══════════════════════════════════════════════════════════════╗
        ║                    Chess RL Bot - Live Metrics              ║
        ╠══════════════════════════════════════════════════════════════╣
        ║ Training Performance:                                        ║
        ║   Episodes/sec: ${metrics.episodesPerSecond.format(2)}                           ║
        ║   Win Rate: ${(metrics.winRate * 100).format(1)}%                                ║
        ║   Training Loss: ${metrics.trainingLoss.format(6)}                        ║
        ║   Avg Episode Length: ${metrics.averageEpisodeLength.format(1)}                  ║
        ║                                                              ║
        ║ System Resources:                                            ║
        ║   CPU Usage: ${(metrics.cpuUsage * 100).format(1)}%                              ║
        ║   Memory: ${formatBytes(metrics.memoryUsage)} (${metrics.memoryUsagePercent.format(1)}%)    ║
        ║   Disk: ${formatBytes(metrics.diskUsage)} (${metrics.diskUsagePercent.format(1)}%)          ║
        ║                                                              ║
        ║ Neural Network:                                              ║
        ║   Prediction Time: ${metrics.predictionTimeMs.format(2)}ms                       ║
        ║   Batch Processing: ${metrics.batchProcessingTimeMs.format(2)}ms                 ║
        ║   Experience Buffer: ${metrics.experienceBufferSize} (${metrics.experienceBufferUtilization.format(1)}%) ║
        ║                                                              ║
        ║ JVM:                                                         ║
        ║   Heap: ${formatBytes(metrics.heapUsed)}/${formatBytes(metrics.heapMax)}         ║
        ║   GC Count: ${metrics.gcCount}                                        ║
        ║   GC Time: ${metrics.gcTime}ms                                       ║
        ╚══════════════════════════════════════════════════════════════╝
        """.trimIndent())
    }
}
```

### 3. Log Monitoring

#### Log Analysis Script
```bash
#!/bin/bash
# log-monitor.sh

LOG_FILE="/var/log/chessrl/chess-rl-bot.log"
ERROR_THRESHOLD=10
WARNING_THRESHOLD=50

analyze_logs() {
    local timeframe="1 hour ago"
    local since=$(date -d "$timeframe" '+%Y-%m-%d %H:%M:%S')
    
    echo "Analyzing logs since $since..."
    
    # Count errors and warnings
    local errors=$(grep -c "ERROR" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1))
    local warnings=$(grep -c "WARN" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1))
    
    echo "Errors in last hour: $errors"
    echo "Warnings in last hour: $warnings"
    
    # Check thresholds
    if [ "$errors" -gt "$ERROR_THRESHOLD" ]; then
        echo "ERROR: Too many errors detected ($errors > $ERROR_THRESHOLD)"
        send_alert "High error rate: $errors errors in the last hour"
    fi
    
    if [ "$warnings" -gt "$WARNING_THRESHOLD" ]; then
        echo "WARNING: High warning rate ($warnings > $WARNING_THRESHOLD)"
    fi
    
    # Show recent errors
    echo "Recent errors:"
    grep "ERROR" "$LOG_FILE" | tail -5
}

send_alert() {
    local message="$1"
    echo "$message" | mail -s "Chess RL Bot Alert" admin@example.com
    # Also send to Slack, Discord, etc.
}

# Run analysis
analyze_logs
```

## Maintenance Procedures

### 1. Regular Maintenance Tasks

#### Daily Maintenance
```bash
#!/bin/bash
# daily-maintenance.sh

echo "Starting daily maintenance at $(date)"

# 1. Check system health
./health-check.sh

# 2. Analyze logs
./log-monitor.sh

# 3. Check disk space
df -h | grep -E "(80%|90%|95%)" && echo "WARNING: High disk usage detected"

# 4. Backup training data
./backup-training-data.sh

# 5. Clean old log files
find /var/log/chessrl -name "*.log.*" -mtime +7 -delete

# 6. Update performance metrics
./collect-performance-metrics.sh

echo "Daily maintenance completed at $(date)"
```

#### Weekly Maintenance
```bash
#!/bin/bash
# weekly-maintenance.sh

echo "Starting weekly maintenance at $(date)"

# 1. Full system backup
./full-backup.sh

# 2. Performance analysis
./weekly-performance-report.sh

# 3. Model validation
./validate-trained-models.sh

# 4. Update system packages (if needed)
# sudo apt update && sudo apt upgrade -y

# 5. Restart services (if needed)
# sudo systemctl restart chessrl

echo "Weekly maintenance completed at $(date)"
```

### 2. Backup Procedures

#### Training Data Backup
```bash
#!/bin/bash
# backup-training-data.sh

BACKUP_DIR="/backup/chessrl"
DATA_DIR="/opt/chessrl/data"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Backup training data
echo "Backing up training data..."
tar -czf "$BACKUP_DIR/training-data-$TIMESTAMP.tar.gz" "$DATA_DIR"

# Backup models
echo "Backing up models..."
tar -czf "$BACKUP_DIR/models-$TIMESTAMP.tar.gz" /opt/chessrl/models

# Backup configuration
echo "Backing up configuration..."
tar -czf "$BACKUP_DIR/config-$TIMESTAMP.tar.gz" /opt/chessrl/config

# Clean old backups (keep last 30 days)
find "$BACKUP_DIR" -name "*.tar.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_DIR"
```

#### Model Checkpointing
```kotlin
class ModelCheckpointManager {
    private val checkpointDir = "/opt/chessrl/checkpoints"
    
    fun saveCheckpoint(
        episode: Int,
        agent: ChessAgent,
        trainingMetrics: TrainingMetrics
    ) {
        val timestamp = System.currentTimeMillis()
        val checkpointPath = "$checkpointDir/checkpoint-$episode-$timestamp"
        
        // Save model weights
        agent.save("$checkpointPath/model.weights")
        
        // Save training state
        saveTrainingState("$checkpointPath/training.state", trainingMetrics)
        
        // Save metadata
        saveMetadata("$checkpointPath/metadata.json", episode, timestamp, trainingMetrics)
        
        // Clean old checkpoints (keep last 10)
        cleanOldCheckpoints()
    }
    
    fun loadCheckpoint(checkpointPath: String): CheckpointData {
        return CheckpointData(
            modelWeights = loadModelWeights("$checkpointPath/model.weights"),
            trainingState = loadTrainingState("$checkpointPath/training.state"),
            metadata = loadMetadata("$checkpointPath/metadata.json")
        )
    }
    
    private fun cleanOldCheckpoints() {
        val checkpoints = File(checkpointDir).listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?: return
        
        if (checkpoints.size > 10) {
            checkpoints.drop(10).forEach { it.deleteRecursively() }
        }
    }
}
```

### 3. Performance Optimization

#### JVM Tuning
```bash
#!/bin/bash
# jvm-tuning.sh

# Optimal JVM settings for training
export JAVA_OPTS="
    -Xmx32g
    -Xms8g
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:G1HeapRegionSize=16m
    -XX:+UseStringDeduplication
    -XX:+OptimizeStringConcat
    -XX:+UseCompressedOops
    -XX:+UseCompressedClassPointers
    -XX:+UnlockExperimentalVMOptions
    -XX:+UseCGroupMemoryLimitForHeap
    -XX:+PrintGC
    -XX:+PrintGCDetails
    -XX:+PrintGCTimeStamps
    -Xloggc:/var/log/chessrl/gc.log
"

# Start application with optimized settings
java $JAVA_OPTS -jar chess-rl-bot.jar
```

#### Memory Optimization
```kotlin
class MemoryOptimizer {
    private val memoryThreshold = 0.8
    private val forceGCThreshold = 0.9
    
    fun optimizeMemoryUsage() {
        val memoryUsage = getMemoryUsagePercent()
        
        when {
            memoryUsage > forceGCThreshold -> {
                println("Memory usage critical (${memoryUsage * 100}%), forcing GC")
                System.gc()
                cleanupExperienceBuffer()
            }
            memoryUsage > memoryThreshold -> {
                println("Memory usage high (${memoryUsage * 100}%), optimizing")
                optimizeExperienceBuffer()
                cleanupTempData()
            }
        }
    }
    
    private fun cleanupExperienceBuffer() {
        // Remove oldest experiences
        experienceBuffer.removeOldest(experienceBuffer.size / 4)
    }
    
    private fun optimizeExperienceBuffer() {
        // Compress experience data
        experienceBuffer.compress()
    }
    
    private fun cleanupTempData() {
        // Clean temporary files and caches
        File("/tmp/chessrl").deleteRecursively()
    }
}
```

## Troubleshooting Guide

### 1. Common Issues and Solutions

#### Training Not Converging
**Symptoms**: Win rate not improving, high loss values
**Diagnosis**:
```kotlin
class ConvergenceAnalyzer {
    fun diagnoseConvergenceIssues(trainingHistory: List<TrainingMetrics>): List<Issue> {
        val issues = mutableListOf<Issue>()
        
        // Check learning rate
        if (isLearningRateTooHigh(trainingHistory)) {
            issues.add(Issue.LEARNING_RATE_TOO_HIGH)
        }
        
        // Check exploration
        if (isExplorationTooLow(trainingHistory)) {
            issues.add(Issue.INSUFFICIENT_EXPLORATION)
        }
        
        // Check network capacity
        if (isNetworkTooSmall(trainingHistory)) {
            issues.add(Issue.NETWORK_TOO_SMALL)
        }
        
        return issues
    }
}
```

**Solutions**:
```bash
# Reduce learning rate
sed -i 's/training.learningRate=0.001/training.learningRate=0.0005/' config/production.conf

# Increase exploration
sed -i 's/training.explorationRate=0.1/training.explorationRate=0.2/' config/production.conf

# Increase network size
sed -i 's/network.hiddenLayers=\[256, 128\]/network.hiddenLayers=[512, 256, 128]/' config/production.conf

# Restart training
sudo systemctl restart chessrl
```

#### High Memory Usage
**Symptoms**: OutOfMemoryError, slow performance
**Diagnosis**:
```bash
# Check memory usage
free -h
ps aux | grep java | head -1

# Check JVM heap usage
jstat -gc $(pgrep java)

# Check experience buffer size
grep "Experience buffer size" /var/log/chessrl/chess-rl-bot.log | tail -1
```

**Solutions**:
```bash
# Increase JVM heap size
export JAVA_OPTS="-Xmx64g"

# Reduce experience buffer size
sed -i 's/training.experienceBufferSize=100000/training.experienceBufferSize=50000/' config/production.conf

# Enable memory optimization
sed -i 's/performance.memory.experienceBufferCleanup=false/performance.memory.experienceBufferCleanup=true/' config/production.conf
```

#### Slow Training Performance
**Symptoms**: Low episodes per second, high CPU usage
**Diagnosis**:
```bash
# Check CPU usage
top -p $(pgrep java)

# Check I/O wait
iostat -x 1 5

# Check training metrics
tail -f /var/log/chessrl/chess-rl-bot.log | grep "Episodes/sec"
```

**Solutions**:
```bash
# Enable JVM optimization
sed -i 's/performance.jvm.enabled=false/performance.jvm.enabled=true/' config/production.conf

# Increase parallel games
sed -i 's/performance.threading.parallelGames=1/performance.threading.parallelGames=4/' config/production.conf

# Reduce batch size for faster updates
sed -i 's/training.batchSize=128/training.batchSize=64/' config/production.conf
```

### 2. Diagnostic Tools

#### System Diagnostics
```bash
#!/bin/bash
# system-diagnostics.sh

echo "=== Chess RL Bot System Diagnostics ==="
echo "Timestamp: $(date)"
echo

echo "=== System Information ==="
uname -a
echo "CPU cores: $(nproc)"
echo "Memory: $(free -h | grep Mem | awk '{print $2}')"
echo "Disk space: $(df -h / | tail -1 | awk '{print $4}')"
echo

echo "=== Java Information ==="
java -version
echo "JAVA_HOME: $JAVA_HOME"
echo "JAVA_OPTS: $JAVA_OPTS"
echo

echo "=== Process Information ==="
ps aux | grep java | head -1
echo

echo "=== Memory Usage ==="
free -h
echo

echo "=== Disk Usage ==="
df -h
echo

echo "=== Network Connections ==="
netstat -tlnp | grep java
echo

echo "=== Recent Errors ==="
tail -20 /var/log/chessrl/chess-rl-bot.log | grep ERROR
echo

echo "=== Training Status ==="
tail -5 /var/log/chessrl/chess-rl-bot.log | grep "Episode"
```

#### Performance Profiling
```kotlin
class PerformanceProfiler {
    fun profileTrainingSession(duration: Long): ProfileReport {
        val startTime = System.currentTimeMillis()
        val initialMetrics = collectMetrics()
        
        // Wait for profiling duration
        Thread.sleep(duration)
        
        val endTime = System.currentTimeMillis()
        val finalMetrics = collectMetrics()
        
        return ProfileReport(
            duration = endTime - startTime,
            initialMetrics = initialMetrics,
            finalMetrics = finalMetrics,
            performance = calculatePerformance(initialMetrics, finalMetrics),
            bottlenecks = identifyBottlenecks(initialMetrics, finalMetrics),
            recommendations = generateRecommendations(initialMetrics, finalMetrics)
        )
    }
    
    private fun identifyBottlenecks(
        initial: PerformanceMetrics,
        final: PerformanceMetrics
    ): List<Bottleneck> {
        val bottlenecks = mutableListOf<Bottleneck>()
        
        if (final.cpuUsage > 0.9) {
            bottlenecks.add(Bottleneck.HIGH_CPU_USAGE)
        }
        
        if (final.memoryUsagePercent > 0.8) {
            bottlenecks.add(Bottleneck.HIGH_MEMORY_USAGE)
        }
        
        if (final.predictionTimeMs > 100) {
            bottlenecks.add(Bottleneck.SLOW_NEURAL_NETWORK)
        }
        
        return bottlenecks
    }
}
```

### 3. Emergency Procedures

#### Service Recovery
```bash
#!/bin/bash
# emergency-recovery.sh

echo "Starting emergency recovery procedure..."

# 1. Stop the service
sudo systemctl stop chessrl

# 2. Check for core dumps
ls -la /var/crash/

# 3. Backup current state
./backup-training-data.sh

# 4. Clear temporary files
rm -rf /tmp/chessrl/*

# 5. Reset to last known good configuration
cp config/production.conf.backup config/production.conf

# 6. Start with safe configuration
export JAVA_OPTS="-Xmx8g -XX:+UseG1GC"
sudo systemctl start chessrl

# 7. Monitor startup
tail -f /var/log/chessrl/chess-rl-bot.log
```

#### Data Recovery
```bash
#!/bin/bash
# data-recovery.sh

BACKUP_DIR="/backup/chessrl"
RECOVERY_DIR="/opt/chessrl/recovery"

echo "Starting data recovery..."

# 1. Create recovery directory
mkdir -p "$RECOVERY_DIR"

# 2. Find latest backup
LATEST_BACKUP=$(ls -t "$BACKUP_DIR"/training-data-*.tar.gz | head -1)

if [ -n "$LATEST_BACKUP" ]; then
    echo "Restoring from: $LATEST_BACKUP"
    tar -xzf "$LATEST_BACKUP" -C "$RECOVERY_DIR"
    
    # 3. Validate recovered data
    ./validate-training-data.sh "$RECOVERY_DIR"
    
    if [ $? -eq 0 ]; then
        echo "Data recovery successful"
        # Move recovered data to production
        mv "$RECOVERY_DIR"/* /opt/chessrl/data/
    else
        echo "Data recovery failed - data corrupted"
        exit 1
    fi
else
    echo "No backup found for recovery"
    exit 1
fi
```

This comprehensive operations guide provides detailed procedures for monitoring, maintaining, and troubleshooting the Chess RL Bot system in production environments.