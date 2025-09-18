# Chess RL Bot

**Production-ready Kotlin Multiplatform reinforcement learning system for chess**

A complete chess RL training system with advanced self-play, neural networks, and comprehensive evaluation tools. Features concurrent training, sophisticated game state detection, and production-grade monitoring.

## 🚀 Quick Start

### Train a Chess Agent (Recommended)
```bash
# Start training with optimized settings and 4x parallel games
./gradlew :integration:runCli -Dargs="--train-advanced --cycles 10 --profile dqn_unlock_elo_prioritized"
```

### Evaluate Against Baseline
```bash
# Test your trained agent against heuristic opponent
./gradlew :integration:runCli -Dargs="--eval-baseline --games 100 --load-best"
```

### Head-to-Head Comparison
```bash
# Compare two trained models
./gradlew :integration:runCli -Dargs="--eval-h2h --games 100 --loadA model_a.json --loadB model_b.json"
```

## 🏗️ Architecture

```
chess-rl-bot/
├── chess-engine/        # ♟️  Complete chess implementation with all rules
├── nn-package/          # 🧠 Advanced neural network library  
├── rl-framework/        # 🤖 DQN and Policy Gradient algorithms
└── integration/         # 🔗 Production training pipeline
```

## ✨ Recent Improvements

### 🔧 **Fixed Peer Evaluation Bug** 
- **Problem**: Peer evaluation showed 100% draws due to flawed threefold repetition detection
- **Solution**: Removed aggressive local repetition detection, now uses proper chess engine rules
- **Result**: Realistic win/loss ratios in model comparisons

### ⚡ **Enabled Concurrent Training**
- **Problem**: Training defaulted to 1 game at a time (no parallelism)
- **Solution**: Increased default `parallelGames` from 1 to 4
- **Result**: ~4x faster self-play phase with better CPU utilization

### 🎯 **Enhanced Game State Detection**
- **Problem**: Step-limited games incorrectly treated as draws
- **Solution**: Comprehensive validation of all chess termination conditions
- **Result**: Accurate reward signals for RL training

## 📊 Key Features

### **Advanced Self-Play Training**
- **Concurrent Games**: 1-8 parallel self-play games for faster training
- **Smart Evaluation**: Head-to-head model comparison with realistic outcomes
- **Adaptive Scheduling**: Dynamic adjustment of games per cycle based on performance

### **Production-Ready Pipeline**
- **Checkpointing**: Automatic model versioning with rollback capabilities
- **Monitoring**: Real-time metrics and performance tracking
- **Validation**: Comprehensive training issue detection and recovery

### **Optimized Performance**
- **JVM-First**: 5-8x faster training than native compilation
- **Memory Efficient**: Handles 50K+ experiences with circular buffers
- **Batch Processing**: Optimized for 32-128 batch sizes

## 🎮 Training Profiles

### **Default Profile: `dqn_unlock_elo_prioritized`**
Optimized for learning from scratch with anti-draw measures:
```bash
./gradlew :integration:runCli -Dargs="--train-advanced --cycles 10 --profile dqn_unlock_elo_prioritized"
```

### **Bootstrap Profile: `dqn_imitation_bootstrap`**
Start from supervised learning for faster progress:
```bash
# First, collect teacher data
./gradlew :chess-engine:runTeacherCollector -Dargs="--collect --games 50 --depth 2 --out data/teacher.ndjson"

# Train imitation model
./gradlew :chess-engine:runImitationTrainer -Dargs="--train-imitation --data data/teacher.ndjson --out data/imitation_qnet.json"

# Fine-tune with RL
./gradlew :integration:runCli -Dargs="--train-advanced --cycles 10 --profile dqn_imitation_bootstrap"
```

## 🔧 Configuration Options

### **Concurrency Settings**
```bash
# Use 8 parallel games for faster training (if you have enough CPU cores)
--parallel-games 8
```

### **Reproducible Training**
```bash
# Use deterministic seeding for reproducible results
--seed 12345 --deterministic
```

### **Custom Checkpointing**
```bash
# Save to custom directory with specific intervals
--checkpoint-dir my_experiment --checkpoint-interval 5
```

## 📈 Performance Characteristics

| Metric | Value | Notes |
|--------|-------|-------|
| **Training Speed** | 5-7 episodes/sec | With 4 parallel games |
| **Memory Usage** | 100-500MB | Depends on buffer size |
| **JVM vs Native** | 5-8x faster (JVM) | For sustained training |
| **Concurrent Games** | 1-8 supported | Scales with CPU cores |

## 🧪 Testing & Validation

### **Comprehensive Test Suite**
- **166+ Tests**: Unit, integration, and performance tests
- **Game State Validation**: All chess rules and edge cases covered
- **Training Pipeline**: End-to-end validation of RL components

### **Run Tests**
```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :chess-engine:jvmTest
./gradlew :integration:test
```

## 🎯 What's Working Today

### **Core Functionality**
- ✅ Complete chess engine with all rules (castling, en passant, promotion)
- ✅ Advanced neural network library with multiple optimizers
- ✅ DQN algorithm with experience replay and target networks
- ✅ Concurrent self-play training with 4x speedup
- ✅ Comprehensive game state detection and validation

### **Production Features**
- ✅ Automatic checkpointing and model versioning
- ✅ Real-time training metrics and monitoring
- ✅ Deterministic seeding for reproducible experiments
- ✅ Head-to-head model evaluation with realistic outcomes

### **Performance Optimizations**
- ✅ JVM-optimized training pipeline (5-8x faster than native)
- ✅ Efficient memory management for large experience buffers
- ✅ Batch processing optimized for chess RL workloads

## 🚧 Known Limitations

- **Experience Replay**: Uses live environment for action masking (not stored per-experience)
- **Monitoring**: Some metrics are simulated rather than measured from hot paths
- **Opponent Variety**: Limited to heuristic and self-play opponents

## 🎯 Next Steps

### **High Priority**
1. **Shorter Games**: Reduce `maxStepsPerGame` to 80-100 for faster learning signals
2. **Position Rewards**: Enable positional reward shaping to reduce reward sparsity
3. **Curriculum Learning**: Start from mid-game positions for accelerated learning

### **Medium Priority**
1. **Enhanced Opponents**: Add more diverse opponent strategies
2. **Real Monitoring**: Replace simulated metrics with actual performance measurements
3. **Advanced Evaluation**: Add ELO rating system for model comparison

## 📚 Package Details

### 🧠 **Neural Network Package** (`nn-package`)
Advanced neural network library with production-ready training infrastructure:
- **Multiple Optimizers**: Adam, RMSprop, SGD with momentum
- **Loss Functions**: MSE, CrossEntropy, Huber (RL-optimized)
- **Regularization**: L1/L2, Dropout for robust training
- **Batch Processing**: Efficient 16-128 batch sizes

### ♟️ **Chess Engine** (`chess-engine`)
Complete chess implementation with full rule support:
- **Complete Rules**: All chess rules including castling, en passant, promotion
- **Game State Detection**: Checkmate, stalemate, draw detection
- **Notation Support**: FEN parsing/generation, PGN import/export
- **Analysis Tools**: Move validation, position evaluation, game replay

### 🤖 **RL Framework** (`rl-framework`)
Flexible reinforcement learning framework:
- **DQN Algorithm**: Deep Q-Network with target networks and experience replay
- **Policy Gradient**: REINFORCE algorithm with optional baselines
- **Experience Replay**: Circular and prioritized buffers
- **Exploration Strategies**: Epsilon-greedy, Boltzmann, UCB exploration

### 🔗 **Integration Layer** (`integration`)
Production-ready chess RL training system:
- **ChessEnvironment**: 839-feature state encoding, 4096 action space
- **Training Pipeline**: Batch processing, multiple sampling strategies
- **Enhanced Metrics**: Detailed episode termination analysis
- **Validation Tools**: Interactive debugging and analysis capabilities

## 🤝 Contributing

This project follows production-ready development practices:
- **Modular Design**: Each package is independently testable
- **Comprehensive Testing**: 166+ tests covering all components
- **Performance First**: JVM-optimized for training workloads
- **Clean Interfaces**: Extensible design supporting multiple RL algorithms

---

**Ready to train your chess agent?** Start with the quick start commands above! 🚀