# User Manual - Complete Guide to Chess RL Bot

## Overview

The Chess RL Bot is a production-ready reinforcement learning system that trains chess-playing agents through self-play. This user manual provides comprehensive guidance for using the system, from basic setup to advanced training configurations.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Installation](#installation)
3. [Basic Usage](#basic-usage)
4. [Training Configuration](#training-configuration)
5. [Monitoring and Analysis](#monitoring-and-analysis)
6. [Advanced Features](#advanced-features)
7. [Troubleshooting](#troubleshooting)
8. [FAQ](#faq)

## Quick Start

### 5-Minute Setup

```bash
# 1. Download and extract the Chess RL Bot
wget https://github.com/chess-rl-bot/releases/latest/chess-rl-bot.tar.gz
tar -xzf chess-rl-bot.tar.gz
cd chess-rl-bot

# 2. Run quick setup
./setup.sh

# 3. Start basic training
./train.sh --config=quick-start --episodes=100

# 4. Monitor progress
./monitor.sh
```

### What You'll See

After starting training, you'll see output like:
```
Chess RL Bot - Training Started
Configuration: quick-start
Episodes: 100

Episode 1: Reward=-1.0, Length=23, Win Rate=0.00
Episode 10: Reward=0.0, Length=45, Win Rate=0.10
Episode 50: Reward=1.0, Length=67, Win Rate=0.32
Episode 100: Reward=1.0, Length=89, Win Rate=0.58

Training completed successfully!
Final win rate: 58%
Model saved to: models/trained-agent-100.model
```

## Installation

### System Requirements

**Minimum Requirements**:
- Operating System: macOS 10.14+, Linux (Ubuntu 18.04+), Windows 10+
- Memory: 4GB RAM
- Storage: 2GB free space
- Java: JDK 11+

**Recommended Requirements**:
- Operating System: macOS 12+, Linux (Ubuntu 20.04+)
- Memory: 16GB RAM
- Storage: 50GB free space
- Java: JDK 17+

### Installation Methods

#### Method 1: Binary Release (Recommended)
```bash
# Download latest release
wget https://github.com/chess-rl-bot/releases/latest/chess-rl-bot.tar.gz

# Extract
tar -xzf chess-rl-bot.tar.gz
cd chess-rl-bot

# Run setup
./setup.sh

# Verify installation
./chess-rl-bot --version
```

#### Method 2: Docker Installation
```bash
# Pull Docker image
docker pull chessrlbot/chess-rl-bot:latest

# Run container
docker run -it --name chess-rl-bot \
  -v $(pwd)/data:/app/data \
  -v $(pwd)/models:/app/models \
  chessrlbot/chess-rl-bot:latest

# Start training
docker exec chess-rl-bot ./train.sh --config=default
```

#### Method 3: Build from Source
```bash
# Clone repository
git clone https://github.com/chess-rl-bot/chess-rl-bot.git
cd chess-rl-bot

# Build project
./gradlew build

# Create distribution
./gradlew assembleDist

# Extract and setup
cd build/distributions
tar -xzf chess-rl-bot.tar.gz
cd chess-rl-bot
./setup.sh
```

### Verification

After installation, verify everything works:
```bash
# Check system health
./health-check.sh

# Run quick test
./test.sh --quick

# Verify components
./chess-rl-bot --test-components
```

## Basic Usage

### Starting Your First Training Session

#### 1. Choose a Configuration
```bash
# List available configurations
./list-configs.sh

# Available configurations:
# - quick-start: Fast training for testing (100 episodes)
# - development: Development training (1000 episodes)
# - production: Full production training (50000 episodes)
# - custom: Use custom configuration file
```

#### 2. Start Training
```bash
# Quick start training
./train.sh --config=quick-start

# Development training
./train.sh --config=development --episodes=5000

# Production training with custom settings
./train.sh --config=production \
  --learning-rate=0.001 \
  --batch-size=64 \
  --exploration-rate=0.1
```

#### 3. Monitor Progress
```bash
# Real-time monitoring
./monitor.sh

# Check training status
./status.sh

# View training logs
tail -f logs/training.log
```

### Basic Commands

#### Training Commands
```bash
# Start training
./train.sh [options]

# Resume training from checkpoint
./resume.sh --checkpoint=episode-1000

# Stop training gracefully
./stop.sh

# Pause/resume training
./pause.sh
./resume.sh
```

#### Model Management
```bash
# List saved models
./list-models.sh

# Load specific model
./load-model.sh --model=trained-agent-5000.model

# Export model for deployment
./export-model.sh --model=best-model.model --format=native

# Compare models
./compare-models.sh --model1=agent-1000.model --model2=agent-5000.model
```

#### Analysis Commands
```bash
# Analyze training progress
./analyze.sh --episodes=1000

# Generate training report
./report.sh --output=training-report.html

# Benchmark performance
./benchmark.sh --model=latest

# Play against trained agent
./play.sh --model=trained-agent.model
```

## Training Configuration

### Configuration Files

Configuration files are located in the `config/` directory:

```
config/
├── quick-start.conf      # Fast training for testing
├── development.conf      # Development environment
├── production.conf       # Production training
├── templates/           # Configuration templates
│   ├── minimal.conf     # Minimal configuration
│   ├── balanced.conf    # Balanced performance/quality
│   └── high-perf.conf   # High performance training
└── custom/              # User custom configurations
```

### Basic Configuration Parameters

#### Training Parameters
```properties
# config/my-training.conf

# Basic training settings
training.episodes=10000
training.maxStepsPerEpisode=200
training.learningRate=0.001
training.batchSize=64

# Exploration settings
training.explorationRate=0.1
training.explorationDecay=0.995
training.minExplorationRate=0.01

# Validation and checkpointing
training.validationFrequency=100
training.checkpointFrequency=500
```

#### Network Architecture
```properties
# Network configuration
network.inputSize=776
network.hiddenLayers=[512, 256, 128]
network.outputSize=4096
network.activationFunction=RELU
network.optimizer=ADAM

# Regularization
network.dropout=0.1
network.l2Regularization=0.001
```

#### Performance Settings
```properties
# Performance optimization
performance.useJVMOptimization=true
performance.parallelGames=4
performance.batchProcessingThreads=2

# Memory management
performance.experienceBufferSize=50000
performance.memoryCleanupFrequency=1000
```

### Configuration Examples

#### Quick Testing Configuration
```properties
# config/quick-test.conf
training.episodes=100
training.batchSize=32
training.learningRate=0.003
training.explorationRate=0.2
training.validationFrequency=10

network.hiddenLayers=[256, 128]
performance.parallelGames=1
```

#### Production Configuration
```properties
# config/production.conf
training.episodes=50000
training.batchSize=128
training.learningRate=0.0005
training.explorationRate=0.05
training.validationFrequency=500

network.hiddenLayers=[1024, 512, 256, 128]
network.dropout=0.15
network.l2Regularization=0.01

performance.parallelGames=8
performance.experienceBufferSize=100000
```

### Using Custom Configurations

#### Create Custom Configuration
```bash
# Copy template
cp config/templates/balanced.conf config/custom/my-config.conf

# Edit configuration
nano config/custom/my-config.conf

# Use custom configuration
./train.sh --config=custom/my-config
```

#### Validate Configuration
```bash
# Validate configuration file
./validate-config.sh config/custom/my-config.conf

# Test configuration with dry run
./train.sh --config=custom/my-config --dry-run
```

## Monitoring and Analysis

### Real-Time Monitoring

#### Training Dashboard
```bash
# Start interactive dashboard
./dashboard.sh

# Dashboard shows:
# - Current episode and progress
# - Win rate over time
# - Training loss and metrics
# - System resource usage
# - Recent game analysis
```

#### Command-Line Monitoring
```bash
# Show current status
./status.sh

# Output:
# Training Status: Running
# Current Episode: 2,847 / 10,000
# Win Rate: 67.3%
# Episodes/sec: 5.2
# Memory Usage: 1.2GB / 8GB
# Estimated Time Remaining: 2h 15m
```

#### Log Monitoring
```bash
# Follow training logs
tail -f logs/training.log

# Filter for specific information
grep "Episode" logs/training.log | tail -10
grep "ERROR" logs/training.log
grep "Win Rate" logs/training.log | tail -5
```

### Training Analysis

#### Progress Analysis
```bash
# Analyze training progress
./analyze.sh --episodes=5000

# Generate detailed report
./analyze.sh --episodes=5000 --detailed --output=analysis.html

# Compare different training runs
./compare-runs.sh --run1=experiment-1 --run2=experiment-2
```

#### Performance Metrics
```bash
# Show performance metrics
./metrics.sh

# Performance Metrics:
# - Episodes per second: 5.2
# - Average episode length: 67 moves
# - Memory usage: 1.2GB
# - CPU usage: 45%
# - Training efficiency: 92%
```

#### Game Quality Analysis
```bash
# Analyze recent games
./analyze-games.sh --count=100

# Game Quality Analysis:
# - Average game length: 67 moves
# - Decisive games: 78%
# - Tactical accuracy: 65%
# - Positional understanding: 58%
# - Opening variety: 23 different openings
```

### Visualization Tools

#### Training Curves
```bash
# Generate training curve plots
./plot.sh --type=training-curve --episodes=5000

# Available plot types:
# - training-curve: Win rate and loss over time
# - performance: Episodes per second and resource usage
# - game-length: Average game length over time
# - exploration: Exploration rate decay
```

#### Game Analysis
```bash
# Analyze specific games
./analyze-game.sh --game-id=12345

# Interactive game viewer
./game-viewer.sh --model=trained-agent.model

# Generate game analysis report
./game-report.sh --games=100 --output=game-analysis.html
```

## Advanced Features

### Self-Play Training

#### Basic Self-Play
```bash
# Start self-play training
./train.sh --mode=self-play --agents=2

# Multi-agent self-play
./train.sh --mode=self-play --agents=4 --tournament-style
```

#### Advanced Self-Play Configuration
```properties
# config/self-play.conf
selfPlay.enabled=true
selfPlay.agentPoolSize=4
selfPlay.poolUpdateFrequency=1000
selfPlay.diversityWeight=0.1
selfPlay.tournamentMode=true
```

### Curriculum Learning

#### Enable Curriculum Learning
```bash
# Train with curriculum
./train.sh --curriculum=enabled --phases=3

# Custom curriculum configuration
./train.sh --config=curriculum --curriculum-file=my-curriculum.json
```

#### Curriculum Configuration
```json
{
  "phases": [
    {
      "name": "Endgames",
      "episodes": 5000,
      "positions": "endgame_positions.pgn",
      "success_criteria": 0.8
    },
    {
      "name": "Tactics",
      "episodes": 10000,
      "positions": "tactical_puzzles.pgn",
      "success_criteria": 0.75
    },
    {
      "name": "Full Games",
      "episodes": 35000,
      "positions": "random",
      "success_criteria": 0.65
    }
  ]
}
```

### Model Evaluation

#### Evaluate Against Baselines
```bash
# Evaluate against random player
./evaluate.sh --model=trained-agent.model --opponent=random --games=100

# Evaluate against previous version
./evaluate.sh --model=new-model.model --opponent=old-model.model --games=1000

# Tournament evaluation
./tournament.sh --models=model1.model,model2.model,model3.model --games=500
```

#### Human vs AI Games
```bash
# Play against the AI
./play.sh --model=trained-agent.model --human

# Analyze human vs AI games
./analyze-human-games.sh --games=50
```

### Advanced Configuration

#### Hyperparameter Optimization
```bash
# Run hyperparameter search
./optimize.sh --parameters=learning_rate,batch_size --trials=50

# Use Bayesian optimization
./optimize.sh --method=bayesian --budget=100 --target=win_rate
```

#### Distributed Training
```bash
# Start distributed training coordinator
./distributed-train.sh --workers=4 --coordinator-host=localhost

# Add worker nodes
./add-worker.sh --coordinator=192.168.1.100:8080 --gpu-id=0
```

## Troubleshooting

### Common Issues

#### Training Not Starting
```bash
# Check system requirements
./check-requirements.sh

# Verify configuration
./validate-config.sh config/my-config.conf

# Check logs for errors
tail -50 logs/error.log
```

#### Poor Training Performance
```bash
# Check system resources
./resource-check.sh

# Optimize configuration
./optimize-config.sh --current-config=my-config.conf

# Enable performance monitoring
./train.sh --config=my-config --profile=true
```

#### Memory Issues
```bash
# Check memory usage
./memory-check.sh

# Reduce memory usage
./train.sh --config=my-config --memory-optimized

# Clean up old data
./cleanup.sh --older-than=7days
```

### Diagnostic Tools

#### System Diagnostics
```bash
# Run comprehensive diagnostics
./diagnose.sh

# Check specific components
./diagnose.sh --component=neural-network
./diagnose.sh --component=chess-engine
./diagnose.sh --component=rl-framework
```

#### Performance Diagnostics
```bash
# Profile training performance
./profile.sh --duration=300 --output=profile-report.html

# Benchmark system performance
./benchmark.sh --comprehensive
```

### Getting Help

#### Built-in Help
```bash
# General help
./chess-rl-bot --help

# Command-specific help
./train.sh --help
./analyze.sh --help
./configure.sh --help
```

#### Log Analysis
```bash
# Analyze logs for issues
./analyze-logs.sh --last=24hours

# Generate diagnostic report
./diagnostic-report.sh --output=diagnostic.html
```

## FAQ

### General Questions

**Q: How long does training take?**
A: Training time depends on configuration:
- Quick test (100 episodes): 5-10 minutes
- Development (1000 episodes): 1-2 hours
- Production (50000 episodes): 2-7 days

**Q: How much memory does the system use?**
A: Memory usage varies by configuration:
- Minimal: 500MB - 1GB
- Standard: 1GB - 4GB
- Large-scale: 4GB - 16GB

**Q: Can I pause and resume training?**
A: Yes, use `./pause.sh` to pause and `./resume.sh` to resume training. The system automatically saves checkpoints.

### Configuration Questions

**Q: How do I optimize training speed?**
A: Several approaches:
- Enable JVM optimization: `performance.useJVMOptimization=true`
- Increase parallel games: `performance.parallelGames=4`
- Optimize batch size: `training.batchSize=64`
- Use smaller network: `network.hiddenLayers=[256, 128]`

**Q: How do I improve training quality?**
A: Quality improvements:
- Increase network size: `network.hiddenLayers=[1024, 512, 256]`
- Add regularization: `network.dropout=0.15`
- Use curriculum learning: `--curriculum=enabled`
- Increase training episodes: `training.episodes=50000`

**Q: What's the best configuration for my hardware?**
A: Use the configuration wizard:
```bash
./configure-wizard.sh
# Analyzes your system and recommends optimal settings
```

### Technical Questions

**Q: Can I use GPU acceleration?**
A: Currently, the system is optimized for CPU training with JVM. GPU support is planned for future releases.

**Q: How do I integrate with existing chess engines?**
A: The system provides UCI (Universal Chess Interface) compatibility:
```bash
./uci-bridge.sh --model=trained-agent.model --port=8080
```

**Q: Can I train on custom chess variants?**
A: Yes, but requires code modification. See the [Developer Guide](../developer-guide/README.md) for details.

### Troubleshooting Questions

**Q: Training seems stuck at 50% win rate**
A: This is common in early training. Try:
- Increase exploration: `training.explorationRate=0.2`
- Reduce learning rate: `training.learningRate=0.0005`
- Check for configuration issues: `./validate-config.sh`

**Q: System crashes with OutOfMemoryError**
A: Reduce memory usage:
- Decrease experience buffer: `performance.experienceBufferSize=25000`
- Reduce batch size: `training.batchSize=32`
- Increase JVM heap: `export JAVA_OPTS="-Xmx16g"`

**Q: Training is very slow**
A: Performance optimization:
- Enable JVM optimization: `performance.useJVMOptimization=true`
- Check system resources: `./resource-check.sh`
- Use performance configuration: `./train.sh --config=templates/high-perf`

## Support and Resources

### Documentation
- [Installation Guide](../deployment/installation.md)
- [Configuration Guide](../deployment/configuration.md)
- [API Documentation](../api/README.md)
- [Troubleshooting Guide](../troubleshooting/README.md)

### Community
- GitHub Issues: Report bugs and request features
- Discussions: Ask questions and share experiences
- Contributing: See [Contributing Guide](../development/contributing.md)

### Professional Support
For professional support, training, or custom development:
- Email: support@chess-rl-bot.com
- Documentation: https://docs.chess-rl-bot.com
- Training Services: Custom training and optimization

This user manual provides comprehensive guidance for using the Chess RL Bot system effectively. For additional help, consult the detailed documentation or reach out to the community.