# Troubleshooting Guide

## Overview

This comprehensive troubleshooting guide covers common issues, diagnostic procedures, and resolution strategies for the Chess RL Bot system. It includes automated diagnostic tools and step-by-step resolution procedures.

## Quick Diagnostic Checklist

### System Health Check
```bash
#!/bin/bash
# quick-health-check.sh

echo "=== Chess RL Bot Quick Health Check ==="
echo "Timestamp: $(date)"
echo

# 1. Check if service is running
if pgrep -f "chess-rl-bot" > /dev/null; then
    echo "✅ Service is running"
else
    echo "❌ Service is not running"
    echo "   Try: sudo systemctl start chessrl"
fi

# 2. Check memory usage
MEMORY_USAGE=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')
if (( $(echo "$MEMORY_USAGE > 90" | bc -l) )); then
    echo "⚠️  High memory usage: ${MEMORY_USAGE}%"
else
    echo "✅ Memory usage: ${MEMORY_USAGE}%"
fi

# 3. Check disk space
DISK_USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 90 ]; then
    echo "⚠️  High disk usage: ${DISK_USAGE}%"
else
    echo "✅ Disk usage: ${DISK_USAGE}%"
fi

# 4. Check recent errors
ERROR_COUNT=$(tail -100 /var/log/chessrl/chess-rl-bot.log | grep -c "ERROR" || echo "0")
if [ "$ERROR_COUNT" -gt 5 ]; then
    echo "⚠️  Recent errors: $ERROR_COUNT"
    echo "   Check: tail -20 /var/log/chessrl/chess-rl-bot.log | grep ERROR"
else
    echo "✅ Recent errors: $ERROR_COUNT"
fi

# 5. Check training progress
LAST_EPISODE=$(tail -10 /var/log/chessrl/chess-rl-bot.log | grep "Episode" | tail -1 | awk '{print $3}' | sed 's/://' || echo "N/A")
echo "📊 Last episode: $LAST_EPISODE"

echo
echo "=== Health Check Complete ==="
```

## Common Issues and Solutions

### 1. Training Issues

#### Issue: Training Not Converging
**Symptoms**:
- Win rate remains around 50% after many episodes
- Training loss not decreasing
- Agent making random-looking moves

**Diagnostic Steps**:
```kotlin
class ConvergenceDiagnostic {
    fun diagnoseConvergenceIssues(trainingHistory: List<TrainingMetrics>): DiagnosisResult {
        val issues = mutableListOf<TrainingIssue>()
        val recommendations = mutableListOf<String>()
        
        // Check learning rate
        val recentLoss = trainingHistory.takeLast(100).map { it.loss }
        if (isLossOscillating(recentLoss)) {
            issues.add(TrainingIssue.LEARNING_RATE_TOO_HIGH)
            recommendations.add("Reduce learning rate by 50%")
        } else if (isLossStagnant(recentLoss)) {
            issues.add(TrainingIssue.LEARNING_RATE_TOO_LOW)
            recommendations.add("Increase learning rate by 2x")
        }
        
        // Check exploration
        val recentWinRate = trainingHistory.takeLast(100).map { it.winRate }
        if (isWinRateStagnant(recentWinRate)) {
            issues.add(TrainingIssue.INSUFFICIENT_EXPLORATION)
            recommendations.add("Increase exploration rate or reset exploration schedule")
        }
        
        // Check network capacity
        if (isNetworkUnderfitting(trainingHistory)) {
            issues.add(TrainingIssue.NETWORK_TOO_SMALL)
            recommendations.add("Increase network size or add layers")
        }
        
        return DiagnosisResult(issues, recommendations)
    }
}
```

**Solutions**:
```bash
# 1. Adjust learning rate
sed -i 's/training.learningRate=0.001/training.learningRate=0.0005/' config/production.conf

# 2. Reset exploration
sed -i 's/training.explorationRate=0.05/training.explorationRate=0.2/' config/production.conf
sed -i 's/training.explorationDecay=0.999/training.explorationDecay=0.995/' config/production.conf

# 3. Increase network size
sed -i 's/network.hiddenLayers=\[256, 128\]/network.hiddenLayers=[512, 256, 128]/' config/production.conf

# 4. Restart training
sudo systemctl restart chessrl
```

#### Issue: Training Loss Exploding
**Symptoms**:
- Training loss suddenly increases dramatically
- NaN values in loss or gradients
- Agent performance degrades rapidly

**Diagnostic Commands**:
```bash
# Check for NaN values in logs
grep -i "nan\|infinity" /var/log/chessrl/chess-rl-bot.log

# Check gradient norms
grep "Gradient norm" /var/log/chessrl/chess-rl-bot.log | tail -20

# Check learning rate history
grep "Learning rate" /var/log/chessrl/chess-rl-bot.log | tail -10
```

**Solutions**:
```bash
# 1. Enable gradient clipping
sed -i 's/training.gradientClipping=false/training.gradientClipping=true/' config/production.conf
sed -i 's/training.gradientClipValue=1.0/training.gradientClipValue=0.5/' config/production.conf

# 2. Reduce learning rate dramatically
sed -i 's/training.learningRate=0.001/training.learningRate=0.0001/' config/production.conf

# 3. Load from last stable checkpoint
./restore-checkpoint.sh --episode=last_stable

# 4. Restart training
sudo systemctl restart chessrl
```

### 2. Performance Issues

#### Issue: Slow Training Performance
**Symptoms**:
- Low episodes per second (< 2.0)
- High CPU usage without proportional throughput
- Long delays between episodes

**Performance Diagnostic**:
```kotlin
class PerformanceDiagnostic {
    fun diagnosePerformanceIssues(): PerformanceDiagnosis {
        val metrics = collectCurrentMetrics()
        val bottlenecks = mutableListOf<PerformanceBottleneck>()
        
        // Check CPU usage
        if (metrics.cpuUsage > 0.95) {
            bottlenecks.add(PerformanceBottleneck.CPU_BOUND)
        }
        
        // Check memory usage
        if (metrics.memoryUsage > 0.9) {
            bottlenecks.add(PerformanceBottleneck.MEMORY_BOUND)
        }
        
        // Check I/O wait
        if (metrics.ioWait > 0.3) {
            bottlenecks.add(PerformanceBottleneck.IO_BOUND)
        }
        
        // Check GC overhead
        if (metrics.gcTime > 0.1) {
            bottlenecks.add(PerformanceBottleneck.GC_OVERHEAD)
        }
        
        // Check neural network performance
        if (metrics.predictionTime > 50.0) {
            bottlenecks.add(PerformanceBottleneck.SLOW_NEURAL_NETWORK)
        }
        
        return PerformanceDiagnosis(bottlenecks, generateRecommendations(bottlenecks))
    }
}
```

**Solutions**:
```bash
# 1. Enable JVM optimization
sed -i 's/performance.jvm.enabled=false/performance.jvm.enabled=true/' config/production.conf

# 2. Optimize batch size
sed -i 's/training.batchSize=128/training.batchSize=64/' config/production.conf

# 3. Increase parallel games
sed -i 's/performance.threading.parallelGames=1/performance.threading.parallelGames=4/' config/production.conf

# 4. Optimize JVM settings
export JAVA_OPTS="-Xmx16g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 5. Restart with optimizations
sudo systemctl restart chessrl
```

#### Issue: High Memory Usage
**Symptoms**:
- Memory usage > 90%
- OutOfMemoryError exceptions
- Frequent garbage collection

**Memory Diagnostic**:
```bash
# Check memory usage breakdown
jstat -gc $(pgrep java) 1s 5

# Check heap usage
jmap -histo $(pgrep java) | head -20

# Check for memory leaks
jcmd $(pgrep java) GC.run_finalization
jcmd $(pgrep java) VM.gc
```

**Solutions**:
```bash
# 1. Increase heap size
export JAVA_OPTS="-Xmx32g -Xms8g"

# 2. Reduce experience buffer size
sed -i 's/training.experienceBufferSize=100000/training.experienceBufferSize=50000/' config/production.conf

# 3. Enable memory cleanup
sed -i 's/performance.memory.experienceBufferCleanup=false/performance.memory.experienceBufferCleanup=true/' config/production.conf

# 4. Optimize GC settings
export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:G1HeapRegionSize=16m"

# 5. Restart service
sudo systemctl restart chessrl
```

### 3. System Issues

#### Issue: Service Won't Start
**Symptoms**:
- Service fails to start
- Immediate exit after startup
- Port binding errors

**Startup Diagnostic**:
```bash
# Check service status
sudo systemctl status chessrl

# Check startup logs
journalctl -u chessrl -n 50

# Check port availability
netstat -tlnp | grep 8080

# Check file permissions
ls -la /opt/chessrl/
ls -la /opt/chessrl/bin/chess-rl-bot

# Check Java installation
java -version
echo $JAVA_HOME
```

**Solutions**:
```bash
# 1. Fix permissions
sudo chown -R chessrl:chessrl /opt/chessrl/
sudo chmod +x /opt/chessrl/bin/chess-rl-bot

# 2. Check configuration
./validate-config.sh config/production.conf

# 3. Kill conflicting processes
sudo pkill -f "chess-rl-bot"
sudo fuser -k 8080/tcp

# 4. Start in debug mode
sudo -u chessrl /opt/chessrl/bin/chess-rl-bot --debug

# 5. Check system resources
free -h
df -h
```

#### Issue: Configuration Errors
**Symptoms**:
- Invalid configuration warnings
- Unexpected parameter values
- Configuration parsing errors

**Configuration Validation**:
```kotlin
class ConfigurationValidator {
    fun validateConfiguration(config: Configuration): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate training parameters
        if (config.training.learningRate <= 0 || config.training.learningRate > 1) {
            errors.add("Invalid learning rate: ${config.training.learningRate}")
        }
        
        if (config.training.batchSize <= 0 || config.training.batchSize > 1024) {
            errors.add("Invalid batch size: ${config.training.batchSize}")
        }
        
        if (config.training.explorationRate < 0 || config.training.explorationRate > 1) {
            errors.add("Invalid exploration rate: ${config.training.explorationRate}")
        }
        
        // Validate network parameters
        if (config.network.hiddenLayers.isEmpty()) {
            errors.add("Network must have at least one hidden layer")
        }
        
        if (config.network.hiddenLayers.any { it <= 0 }) {
            errors.add("All hidden layer sizes must be positive")
        }
        
        // Validate performance parameters
        if (config.performance.threading.parallelGames <= 0) {
            errors.add("Parallel games must be positive")
        }
        
        // Generate warnings for suboptimal settings
        if (config.training.batchSize < 16) {
            warnings.add("Small batch size may lead to unstable training")
        }
        
        if (config.training.experienceBufferSize < 1000) {
            warnings.add("Small experience buffer may limit learning")
        }
        
        return ValidationResult(errors.isEmpty(), errors, warnings)
    }
}
```

**Solutions**:
```bash
# 1. Validate configuration
./validate-config.sh config/production.conf

# 2. Reset to default configuration
cp config/default.conf config/production.conf

# 3. Use configuration template
cp config/templates/production.conf config/production.conf

# 4. Check configuration syntax
grep -n "=" config/production.conf | grep -v "^#"
```

### 4. Data Issues

#### Issue: Corrupted Training Data
**Symptoms**:
- Training crashes with data errors
- Inconsistent episode results
- Experience replay errors

**Data Diagnostic**:
```bash
# Check data directory
ls -la /opt/chessrl/data/

# Validate experience files
./validate-experience-data.sh /opt/chessrl/data/experiences/

# Check for corrupted files
find /opt/chessrl/data/ -name "*.dat" -exec file {} \;

# Check disk errors
dmesg | grep -i "error\|fail"
```

**Solutions**:
```bash
# 1. Backup current data
./backup-training-data.sh

# 2. Validate and repair data
./repair-experience-data.sh /opt/chessrl/data/

# 3. Restore from backup if needed
./restore-from-backup.sh --date=yesterday

# 4. Clear corrupted data and restart
rm -rf /opt/chessrl/data/experiences/*
sudo systemctl restart chessrl
```

#### Issue: Model Loading Errors
**Symptoms**:
- Cannot load saved models
- Model format errors
- Version compatibility issues

**Model Diagnostic**:
```bash
# Check model files
ls -la /opt/chessrl/models/

# Validate model format
./validate-model.sh /opt/chessrl/models/latest.model

# Check model metadata
./model-info.sh /opt/chessrl/models/latest.model
```

**Solutions**:
```bash
# 1. Restore from checkpoint
./restore-checkpoint.sh --episode=latest

# 2. Reset to initial model
cp /opt/chessrl/models/initial.model /opt/chessrl/models/current.model

# 3. Rebuild model from scratch
rm /opt/chessrl/models/current.model
sudo systemctl restart chessrl
```

## Diagnostic Tools

### 1. Automated Diagnostic Script

```bash
#!/bin/bash
# comprehensive-diagnostic.sh

echo "=== Chess RL Bot Comprehensive Diagnostic ==="
echo "Timestamp: $(date)"
echo "Hostname: $(hostname)"
echo

# System Information
echo "=== System Information ==="
uname -a
echo "CPU cores: $(nproc)"
echo "Memory: $(free -h | grep Mem | awk '{print $2}')"
echo "Disk space: $(df -h / | tail -1 | awk '{print $4}')"
echo

# Java Environment
echo "=== Java Environment ==="
java -version 2>&1
echo "JAVA_HOME: $JAVA_HOME"
echo "JAVA_OPTS: $JAVA_OPTS"
echo

# Service Status
echo "=== Service Status ==="
sudo systemctl status chessrl --no-pager
echo

# Process Information
echo "=== Process Information ==="
ps aux | grep java | head -1
echo

# Memory Usage
echo "=== Memory Usage ==="
free -h
echo
if pgrep java > /dev/null; then
    jstat -gc $(pgrep java) | head -2
fi
echo

# Disk Usage
echo "=== Disk Usage ==="
df -h
echo

# Network Status
echo "=== Network Status ==="
netstat -tlnp | grep java
echo

# Log Analysis
echo "=== Recent Log Analysis ==="
echo "Recent errors:"
tail -100 /var/log/chessrl/chess-rl-bot.log | grep ERROR | tail -5
echo
echo "Recent warnings:"
tail -100 /var/log/chessrl/chess-rl-bot.log | grep WARN | tail -5
echo
echo "Training progress:"
tail -20 /var/log/chessrl/chess-rl-bot.log | grep "Episode" | tail -3
echo

# Configuration Validation
echo "=== Configuration Validation ==="
if [ -f "validate-config.sh" ]; then
    ./validate-config.sh config/production.conf
else
    echo "Configuration validator not found"
fi
echo

# Performance Metrics
echo "=== Performance Metrics ==="
if [ -f "/var/log/chessrl/performance.log" ]; then
    tail -5 /var/log/chessrl/performance.log
else
    echo "Performance log not found"
fi
echo

echo "=== Diagnostic Complete ==="
echo "For detailed analysis, check individual log files:"
echo "  - System log: /var/log/chessrl/chess-rl-bot.log"
echo "  - Performance log: /var/log/chessrl/performance.log"
echo "  - GC log: /var/log/chessrl/gc.log"
```

### 2. Performance Profiler

```kotlin
class SystemProfiler {
    fun profileSystem(duration: Long): ProfileReport {
        val startTime = System.currentTimeMillis()
        val initialMetrics = collectSystemMetrics()
        
        // Collect metrics over time
        val metricsHistory = mutableListOf<SystemMetrics>()
        val interval = 1000L // 1 second
        
        while (System.currentTimeMillis() - startTime < duration) {
            Thread.sleep(interval)
            metricsHistory.add(collectSystemMetrics())
        }
        
        val finalMetrics = collectSystemMetrics()
        
        return ProfileReport(
            duration = duration,
            initialMetrics = initialMetrics,
            finalMetrics = finalMetrics,
            metricsHistory = metricsHistory,
            analysis = analyzePerformance(metricsHistory),
            recommendations = generateRecommendations(metricsHistory)
        )
    }
    
    private fun analyzePerformance(history: List<SystemMetrics>): PerformanceAnalysis {
        return PerformanceAnalysis(
            averageCPU = history.map { it.cpuUsage }.average(),
            peakMemory = history.map { it.memoryUsage }.maxOrNull() ?: 0.0,
            averageEpisodesPerSecond = history.map { it.episodesPerSecond }.average(),
            gcOverhead = history.map { it.gcTime }.sum() / history.size,
            bottlenecks = identifyBottlenecks(history)
        )
    }
}
```

### 3. Log Analysis Tool

```bash
#!/bin/bash
# log-analyzer.sh

LOG_FILE="/var/log/chessrl/chess-rl-bot.log"
ANALYSIS_PERIOD="1 hour"

analyze_logs() {
    local since=$(date -d "$ANALYSIS_PERIOD ago" '+%Y-%m-%d %H:%M:%S')
    
    echo "=== Log Analysis for last $ANALYSIS_PERIOD ==="
    echo "Analysis period: since $since"
    echo
    
    # Error analysis
    echo "=== Error Analysis ==="
    local error_count=$(grep -c "ERROR" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1))
    echo "Total errors: $error_count"
    
    if [ "$error_count" -gt 0 ]; then
        echo "Error breakdown:"
        grep "ERROR" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1) | \
        awk '{print $4}' | sort | uniq -c | sort -nr
        
        echo
        echo "Recent errors:"
        grep "ERROR" "$LOG_FILE" | tail -5
    fi
    echo
    
    # Warning analysis
    echo "=== Warning Analysis ==="
    local warning_count=$(grep -c "WARN" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1))
    echo "Total warnings: $warning_count"
    
    if [ "$warning_count" -gt 0 ]; then
        echo "Warning breakdown:"
        grep "WARN" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1) | \
        awk '{print $4}' | sort | uniq -c | sort -nr
    fi
    echo
    
    # Training progress analysis
    echo "=== Training Progress Analysis ==="
    local episodes=$(grep "Episode" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1) | wc -l)
    echo "Episodes completed: $episodes"
    
    if [ "$episodes" -gt 0 ]; then
        echo "Recent training metrics:"
        grep "Episode" "$LOG_FILE" | tail -5 | while read line; do
            echo "  $line"
        done
    fi
    echo
    
    # Performance analysis
    echo "=== Performance Analysis ==="
    if grep -q "Episodes/sec" "$LOG_FILE"; then
        local avg_eps=$(grep "Episodes/sec" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1) | \
        awk '{print $2}' | awk '{sum+=$1; count++} END {if(count>0) print sum/count; else print 0}')
        echo "Average episodes/sec: $avg_eps"
    fi
    
    if grep -q "Memory usage" "$LOG_FILE"; then
        local max_memory=$(grep "Memory usage" "$LOG_FILE" | tail -n +$(grep -n "$since" "$LOG_FILE" | head -1 | cut -d: -f1) | \
        awk '{print $3}' | sed 's/%//' | sort -nr | head -1)
        echo "Peak memory usage: ${max_memory}%"
    fi
}

# Run analysis
analyze_logs
```

## Recovery Procedures

### 1. Emergency Recovery

```bash
#!/bin/bash
# emergency-recovery.sh

echo "=== Emergency Recovery Procedure ==="
echo "Timestamp: $(date)"

# 1. Stop all services
echo "Stopping services..."
sudo systemctl stop chessrl
pkill -f "chess-rl-bot"

# 2. Backup current state
echo "Backing up current state..."
./backup-training-data.sh emergency

# 3. Check system resources
echo "Checking system resources..."
free -h
df -h

# 4. Clear temporary files
echo "Clearing temporary files..."
rm -rf /tmp/chessrl/*
rm -rf /opt/chessrl/temp/*

# 5. Validate configuration
echo "Validating configuration..."
if ! ./validate-config.sh config/production.conf; then
    echo "Configuration invalid, restoring backup..."
    cp config/production.conf.backup config/production.conf
fi

# 6. Start with minimal configuration
echo "Starting with safe configuration..."
cp config/templates/minimal.conf config/current.conf
export JAVA_OPTS="-Xmx4g -XX:+UseG1GC"

# 7. Start service
echo "Starting service..."
sudo systemctl start chessrl

# 8. Monitor startup
echo "Monitoring startup..."
sleep 10
if sudo systemctl is-active chessrl > /dev/null; then
    echo "✅ Service started successfully"
else
    echo "❌ Service failed to start"
    journalctl -u chessrl -n 20
fi

echo "=== Emergency Recovery Complete ==="
```

### 2. Data Recovery

```bash
#!/bin/bash
# data-recovery.sh

BACKUP_DIR="/backup/chessrl"
RECOVERY_DIR="/opt/chessrl/recovery"

echo "=== Data Recovery Procedure ==="

# 1. Create recovery directory
mkdir -p "$RECOVERY_DIR"

# 2. Find available backups
echo "Available backups:"
ls -la "$BACKUP_DIR"/*.tar.gz | tail -10

# 3. Select backup to restore
read -p "Enter backup filename (or 'latest' for most recent): " BACKUP_FILE

if [ "$BACKUP_FILE" = "latest" ]; then
    BACKUP_FILE=$(ls -t "$BACKUP_DIR"/*.tar.gz | head -1)
fi

if [ ! -f "$BACKUP_DIR/$BACKUP_FILE" ]; then
    echo "❌ Backup file not found: $BACKUP_FILE"
    exit 1
fi

# 4. Extract backup
echo "Extracting backup: $BACKUP_FILE"
tar -xzf "$BACKUP_DIR/$BACKUP_FILE" -C "$RECOVERY_DIR"

# 5. Validate recovered data
echo "Validating recovered data..."
if ./validate-training-data.sh "$RECOVERY_DIR"; then
    echo "✅ Data validation successful"
    
    # 6. Stop service and restore data
    sudo systemctl stop chessrl
    cp -r "$RECOVERY_DIR"/* /opt/chessrl/data/
    sudo systemctl start chessrl
    
    echo "✅ Data recovery complete"
else
    echo "❌ Data validation failed"
    echo "Backup may be corrupted"
    exit 1
fi
```

This comprehensive troubleshooting guide provides systematic approaches to diagnosing and resolving common issues in the Chess RL Bot system, with automated tools and step-by-step procedures for effective problem resolution.