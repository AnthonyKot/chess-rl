# Configuration Guide

## Overview

This guide covers comprehensive configuration management for the Chess RL Bot system, including parameter tuning, environment-specific settings, and optimization strategies.

## Configuration Architecture

### Configuration Hierarchy
1. **Default Configuration**: Built-in defaults in code
2. **Environment Configuration**: Environment-specific files
3. **Runtime Configuration**: Command-line arguments and environment variables
4. **Dynamic Configuration**: Runtime adjustments during training

### Configuration Files Structure
```
config/
├── default.conf              # Default settings
├── development.conf           # Development environment
├── production.conf            # Production environment
├── testing.conf              # Testing environment
├── training/
│   ├── quick-test.conf       # Quick testing configuration
│   ├── development.conf      # Development training
│   ├── production.conf       # Production training
│   └── curriculum.conf       # Curriculum learning
└── network/
    ├── small.conf            # Small network architecture
    ├── medium.conf           # Medium network architecture
    └── large.conf            # Large network architecture
```

## Core Configuration Parameters

### 1. Training Configuration

#### Basic Training Parameters
```properties
# config/training/production.conf

# Episode Configuration
training.episodes=50000
training.maxStepsPerEpisode=200
training.warmupEpisodes=1000
training.validationFrequency=500
training.checkpointFrequency=1000

# Learning Parameters
training.learningRate=0.001
training.learningRateDecay=0.99
training.minLearningRate=0.0001
training.batchSize=128
training.miniBatchSize=32

# Exploration Parameters
training.explorationRate=0.1
training.explorationDecay=0.995
training.minExplorationRate=0.01
training.explorationStrategy=EPSILON_GREEDY

# Experience Replay
training.experienceBufferSize=100000
training.minExperienceSize=5000
training.samplingStrategy=UNIFORM
training.prioritizedReplay=false
training.priorityAlpha=0.6
training.priorityBeta=0.4

# Target Network Updates
training.targetUpdateFrequency=500
training.targetUpdateType=HARD
training.softUpdateTau=0.005

# Early Stopping
training.earlyStoppingPatience=100
training.convergenceThreshold=0.001
training.minImprovementEpisodes=50
```

#### Advanced Training Parameters
```properties
# Curriculum Learning
training.curriculum.enabled=true
training.curriculum.phases=3
training.curriculum.phase1.episodes=10000
training.curriculum.phase1.complexity=ENDGAME
training.curriculum.phase2.episodes=20000
training.curriculum.phase2.complexity=TACTICS
training.curriculum.phase3.episodes=20000
training.curriculum.phase3.complexity=FULL_GAME

# Self-Play Configuration
training.selfPlay.enabled=true
training.selfPlay.agentPoolSize=4
training.selfPlay.poolUpdateFrequency=1000
training.selfPlay.diversityWeight=0.1

# Transfer Learning
training.transferLearning.enabled=false
training.transferLearning.pretrainedModel=models/pretrained.model
training.transferLearning.freezeLayers=2
training.transferLearning.fineTuningRate=0.0001
```

### 2. Network Architecture Configuration

#### Standard Network Configurations
```properties
# config/network/small.conf
network.inputSize=776
network.hiddenLayers=[256, 128]
network.outputSize=4096
network.activationFunction=RELU
network.outputActivation=LINEAR

# Optimization
network.optimizer=ADAM
network.adamBeta1=0.9
network.adamBeta2=0.999
network.adamEpsilon=1e-8

# Regularization
network.regularization.l1=0.0
network.regularization.l2=0.001
network.regularization.dropout=0.1
network.regularization.batchNorm=false

# Initialization
network.initialization.type=XAVIER
network.initialization.seed=42
```

```properties
# config/network/large.conf
network.inputSize=776
network.hiddenLayers=[1024, 512, 256, 128]
network.outputSize=4096
network.activationFunction=RELU
network.outputActivation=LINEAR

# Optimization
network.optimizer=ADAM
network.adamBeta1=0.9
network.adamBeta2=0.999
network.adamEpsilon=1e-8

# Regularization
network.regularization.l1=0.0
network.regularization.l2=0.01
network.regularization.dropout=0.2
network.regularization.batchNorm=true

# Initialization
network.initialization.type=HE
network.initialization.seed=42
```

### 3. Chess Environment Configuration

#### Game Rules and Encoding
```properties
# Chess Environment
chess.stateEncoding.type=PIECE_PLANES
chess.stateEncoding.features=776
chess.stateEncoding.normalization=true
chess.stateEncoding.includeHistory=false

chess.actionEncoding.type=FROM_TO_ENCODING
chess.actionEncoding.size=4096
chess.actionEncoding.promotionHandling=SEPARATE_ACTIONS

# Game Rules
chess.rules.enforceTimeLimit=false
chess.rules.timeLimit=300
chess.rules.incrementPerMove=3
chess.rules.drawByRepetition=true
chess.rules.drawBy50MoveRule=true
chess.rules.drawByInsufficientMaterial=true

# Reward Function
chess.rewards.win=1.0
chess.rewards.draw=0.0
chess.rewards.loss=-1.0
chess.rewards.materialWeight=0.0
chess.rewards.positionalWeight=0.0
chess.rewards.timeWeight=0.0
chess.rewards.gameLengthPenalty=0.0
```

#### Position Evaluation
```properties
# Position Evaluation
chess.evaluation.enabled=false
chess.evaluation.materialValues.pawn=1.0
chess.evaluation.materialValues.knight=3.0
chess.evaluation.materialValues.bishop=3.0
chess.evaluation.materialValues.rook=5.0
chess.evaluation.materialValues.queen=9.0
chess.evaluation.materialValues.king=0.0

chess.evaluation.positionalFactors.centerControl=0.1
chess.evaluation.positionalFactors.kingSafety=0.2
chess.evaluation.positionalFactors.pieceActivity=0.1
chess.evaluation.positionalFactors.pawnStructure=0.1
```

### 4. Performance Configuration

#### JVM Performance Settings
```properties
# JVM Configuration
performance.jvm.enabled=true
performance.jvm.heapSize=16g
performance.jvm.gcType=G1GC
performance.jvm.gcPauseTarget=200
performance.jvm.stringDeduplication=true

# Threading
performance.threading.parallelGames=4
performance.threading.batchProcessingThreads=2
performance.threading.experienceCollectionThreads=1

# Memory Management
performance.memory.experienceBufferCleanup=true
performance.memory.cleanupFrequency=1000
performance.memory.memoryThreshold=0.8
performance.memory.forceGCThreshold=0.9
```

#### Native Performance Settings
```properties
# Native Configuration
performance.native.enabled=false
performance.native.optimizationLevel=OPT
performance.native.debugInfo=false
performance.native.linkTimeOptimization=true

# Memory
performance.native.heapSize=8g
performance.native.stackSize=2m
performance.native.gcType=CONCURRENT_MARK_SWEEP
```

### 5. Monitoring and Logging Configuration

#### Logging Configuration
```properties
# Logging
logging.level=INFO
logging.file=logs/chess-rl-bot.log
logging.maxFileSize=100MB
logging.maxFiles=10
logging.pattern=%d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n

# Component-specific logging
logging.chess.level=INFO
logging.neural.level=INFO
logging.rl.level=INFO
logging.training.level=DEBUG
logging.performance.level=INFO

# Console output
logging.console.enabled=true
logging.console.level=INFO
logging.console.colorEnabled=true
```

#### Monitoring Configuration
```properties
# Monitoring
monitoring.enabled=true
monitoring.metricsInterval=60
monitoring.healthCheckInterval=30
monitoring.performanceMetrics=true

# Metrics Collection
monitoring.metrics.training=true
monitoring.metrics.performance=true
monitoring.metrics.memory=true
monitoring.metrics.network=true

# Alerting
monitoring.alerts.enabled=false
monitoring.alerts.email=admin@example.com
monitoring.alerts.thresholds.memoryUsage=0.9
monitoring.alerts.thresholds.trainingStagnation=100
```

## Environment-Specific Configurations

### Development Environment
```properties
# config/development.conf

# Quick iteration settings
training.episodes=1000
training.batchSize=32
training.validationFrequency=50
training.checkpointFrequency=100

# Smaller network for faster training
network.hiddenLayers=[256, 128]
network.regularization.dropout=0.1

# Verbose logging
logging.level=DEBUG
logging.console.enabled=true

# Performance
performance.threading.parallelGames=1
performance.jvm.heapSize=4g

# Monitoring
monitoring.enabled=true
monitoring.metricsInterval=10
```

### Testing Environment
```properties
# config/testing.conf

# Minimal settings for tests
training.episodes=100
training.batchSize=16
training.validationFrequency=10
training.checkpointFrequency=50

# Small network
network.hiddenLayers=[64, 32]
network.regularization.dropout=0.0

# Test-specific logging
logging.level=WARN
logging.console.enabled=false
logging.file=logs/test.log

# Minimal performance
performance.threading.parallelGames=1
performance.jvm.heapSize=2g

# No monitoring
monitoring.enabled=false
```

### Production Environment
```properties
# config/production.conf

# Full-scale training
training.episodes=100000
training.batchSize=128
training.validationFrequency=1000
training.checkpointFrequency=5000

# Large network
network.hiddenLayers=[1024, 512, 256, 128]
network.regularization.dropout=0.2
network.regularization.l2=0.01

# Production logging
logging.level=INFO
logging.console.enabled=false
logging.file=logs/production.log

# High performance
performance.threading.parallelGames=8
performance.jvm.heapSize=32g

# Full monitoring
monitoring.enabled=true
monitoring.metricsInterval=60
monitoring.alerts.enabled=true
```

## Configuration Management

### 1. Configuration Loading

#### Configuration Loader
```kotlin
class ConfigurationManager {
    private val configs = mutableMapOf<String, Configuration>()
    
    fun loadConfiguration(environment: String): Configuration {
        val defaultConfig = loadDefaultConfiguration()
        val envConfig = loadEnvironmentConfiguration(environment)
        val runtimeConfig = loadRuntimeConfiguration()
        
        return mergeConfigurations(defaultConfig, envConfig, runtimeConfig)
    }
    
    private fun loadDefaultConfiguration(): Configuration {
        return Configuration.fromResource("config/default.conf")
    }
    
    private fun loadEnvironmentConfiguration(env: String): Configuration {
        return Configuration.fromResource("config/$env.conf")
    }
    
    private fun loadRuntimeConfiguration(): Configuration {
        return Configuration.fromEnvironmentVariables()
            .merge(Configuration.fromCommandLineArgs())
    }
}
```

#### Configuration Validation
```kotlin
class ConfigurationValidator {
    fun validate(config: Configuration): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate training parameters
        if (config.training.learningRate <= 0) {
            errors.add("Learning rate must be positive")
        }
        
        if (config.training.batchSize <= 0) {
            errors.add("Batch size must be positive")
        }
        
        // Validate network architecture
        if (config.network.hiddenLayers.isEmpty()) {
            errors.add("Network must have at least one hidden layer")
        }
        
        // Validate performance settings
        if (config.performance.threading.parallelGames <= 0) {
            errors.add("Parallel games must be positive")
        }
        
        return ValidationResult(errors.isEmpty(), errors)
    }
}
```

### 2. Dynamic Configuration

#### Runtime Configuration Updates
```kotlin
class DynamicConfiguration {
    private val listeners = mutableListOf<ConfigurationListener>()
    
    fun updateLearningRate(newRate: Double) {
        if (newRate > 0) {
            config.training.learningRate = newRate
            notifyListeners("training.learningRate", newRate)
        }
    }
    
    fun updateExplorationRate(newRate: Double) {
        if (newRate in 0.0..1.0) {
            config.training.explorationRate = newRate
            notifyListeners("training.explorationRate", newRate)
        }
    }
    
    fun updateBatchSize(newSize: Int) {
        if (newSize > 0) {
            config.training.batchSize = newSize
            notifyListeners("training.batchSize", newSize)
        }
    }
    
    private fun notifyListeners(parameter: String, value: Any) {
        listeners.forEach { it.onConfigurationChanged(parameter, value) }
    }
}
```

### 3. Configuration Templates

#### Quick Start Templates
```properties
# config/templates/quick-start.conf
# Minimal configuration for getting started quickly

training.episodes=500
training.batchSize=32
training.learningRate=0.003
training.explorationRate=0.2

network.hiddenLayers=[256, 128]
network.optimizer=ADAM

performance.threading.parallelGames=1
performance.jvm.heapSize=4g

logging.level=INFO
logging.console.enabled=true

monitoring.enabled=true
monitoring.metricsInterval=30
```

#### High Performance Template
```properties
# config/templates/high-performance.conf
# Optimized for maximum training performance

training.episodes=50000
training.batchSize=256
training.learningRate=0.0005
training.explorationRate=0.05

network.hiddenLayers=[1024, 512, 256, 128]
network.optimizer=ADAM
network.regularization.dropout=0.15

performance.threading.parallelGames=8
performance.jvm.heapSize=32g
performance.jvm.gcType=G1GC

logging.level=WARN
logging.console.enabled=false

monitoring.enabled=true
monitoring.metricsInterval=120
```

## Parameter Tuning Guide

### 1. Learning Rate Tuning

#### Learning Rate Schedule
```properties
# Fixed learning rate
training.learningRate=0.001

# Exponential decay
training.learningRateDecay=0.99
training.minLearningRate=0.0001

# Step decay
training.learningRateSchedule=STEP
training.learningRateSteps=[10000, 25000, 40000]
training.learningRateMultipliers=[0.5, 0.2, 0.1]

# Adaptive learning rate
training.adaptiveLearningRate=true
training.adaptiveLearningRatePatience=1000
training.adaptiveLearningRateFactor=0.5
```

#### Learning Rate Guidelines
- **Start with**: 0.001 (Adam), 0.01 (SGD)
- **Too high**: Loss oscillates or increases
- **Too low**: Very slow convergence
- **Optimal**: Steady decrease in loss with occasional plateaus

### 2. Exploration Tuning

#### Exploration Strategies
```properties
# Epsilon-greedy
training.explorationStrategy=EPSILON_GREEDY
training.explorationRate=0.1
training.explorationDecay=0.995
training.minExplorationRate=0.01

# Boltzmann exploration
training.explorationStrategy=BOLTZMANN
training.temperature=1.0
training.temperatureDecay=0.99
training.minTemperature=0.1

# UCB exploration
training.explorationStrategy=UCB
training.ucbConstant=2.0
```

#### Exploration Guidelines
- **High exploration** (0.2-0.5): Early training, complex environments
- **Medium exploration** (0.1-0.2): Standard training
- **Low exploration** (0.01-0.1): Fine-tuning, exploitation phase

### 3. Network Architecture Tuning

#### Architecture Guidelines
```properties
# Small network (fast training, less capacity)
network.hiddenLayers=[128, 64]

# Medium network (balanced)
network.hiddenLayers=[512, 256, 128]

# Large network (high capacity, slower training)
network.hiddenLayers=[1024, 512, 256, 128]

# Very deep network (experimental)
network.hiddenLayers=[512, 512, 256, 256, 128, 128]
```

#### Architecture Selection Criteria
- **Problem complexity**: More complex → larger network
- **Training data**: More data → larger network
- **Training time**: Limited time → smaller network
- **Overfitting**: Reduce size or add regularization

### 4. Batch Size Tuning

#### Batch Size Guidelines
```properties
# Small batch (more updates, noisier gradients)
training.batchSize=32

# Medium batch (balanced)
training.batchSize=64

# Large batch (fewer updates, stabler gradients)
training.batchSize=128

# Very large batch (requires careful tuning)
training.batchSize=256
```

#### Batch Size Selection
- **Memory constraints**: Smaller batch if limited memory
- **Training stability**: Larger batch for more stable training
- **Convergence speed**: Smaller batch often converges faster
- **Parallelization**: Larger batch better for parallel processing

## Configuration Best Practices

### 1. Environment Separation
- Use separate configurations for development, testing, and production
- Never use production configurations in development
- Validate configurations before deployment

### 2. Parameter Documentation
- Document all configuration parameters
- Include valid ranges and default values
- Explain the impact of each parameter

### 3. Configuration Versioning
- Version control all configuration files
- Tag configurations with release versions
- Maintain backward compatibility when possible

### 4. Security Considerations
- Never store sensitive information in configuration files
- Use environment variables for secrets
- Restrict access to production configurations

### 5. Performance Monitoring
- Monitor the impact of configuration changes
- Use A/B testing for parameter optimization
- Keep performance baselines for comparison

This comprehensive configuration guide provides detailed parameter tuning guidance and best practices for optimal system performance.