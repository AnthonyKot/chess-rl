# Deep Q-Network (DQN) Implementation Guide

A comprehensive guide to the Deep Q-Network implementation in the chess RL system. This document explains the algorithm, implementation details, and practical usage for training competitive chess agents.

## Algorithm Overview

Deep Q-Network (DQN) is a reinforcement learning algorithm that learns an action-value function Q(s, a) estimating the expected discounted return when taking action a in state s and following the current policy thereafter.

### Key Components

**Neural Network Q-Function**: Approximates Q-values for all actions given a state
- Input: 839-dimensional chess position encoding
- Output: 4096 Q-values (one for each possible chess move)
- Architecture: Configurable hidden layers with ReLU activation

**Target Network**: Periodically updated copy of the main network for stable training
- Provides stable targets for Q-learning updates
- Updated every `targetUpdateFrequency` steps (default: 100)
- Prevents moving target problem in neural network training

**Experience Replay**: Stores and samples past experiences for training
- Circular buffer storing (state, action, reward, next_state, done) tuples
- Breaks correlation between consecutive experiences
- Enables efficient batch training

**Action Masking**: Ensures only legal chess moves are considered
- Critical for chess where most of 4096 actions are illegal
- Applied during both action selection and target computation
- Prevents learning from invalid move combinations

## Chess-Specific Implementation

### State Representation
The chess position is encoded as a 839-dimensional vector:
- **Board Representation**: 8×8×12 piece planes (768 dimensions)
- **Castling Rights**: 4 dimensions (KQkq)
- **En Passant**: 1 dimension
- **Turn Indicator**: 1 dimension  
- **Move Counts**: 2 dimensions

### Action Space
Chess moves are mapped to a 4096-dimensional action space:
- **Encoding**: `fromSquare × 64 + toSquare`
- **Promotions**: Handled by matching to legal moves
- **Legal Move Masking**: Only valid moves considered during selection

### Reward Structure
Terminal rewards based on game outcomes:
- **Win**: +1.0
- **Loss**: -1.0
- **Draw**: -0.2 (encourages decisive play)
- **Step Limit Penalty**: -1.0 (encourages efficiency)

## Implementation Architecture

### Core Classes

#### DQNAlgorithm (`rl-framework/RLAlgorithms.kt`)
Main DQN implementation with Q-learning, target updates, and experience replay.

```kotlin
val dqn = DQNAlgorithm(
    stateSize = 839,
    actionSize = 4096,
    hiddenLayers = listOf(512, 256, 128),
    learningRate = 0.001,
    gamma = 0.99,
    targetUpdateFrequency = 100
)
```

#### TrainingPipeline (`integration/TrainingPipeline.kt`)
Orchestrates self-play, experience collection, batch training, and evaluation.

```kotlin
val pipeline = TrainingPipeline(config)
pipeline.initialize()
runBlocking { 
    val results = pipeline.runTraining() 
}
```

#### ChessEnvironment (`integration/ChessEnvironment.kt`)
RL environment wrapper that encodes states, validates moves, and computes rewards.

```kotlin
val environment = ChessEnvironment(config)
val state = environment.reset()
val stepResult = environment.step(action)
```

### Training Process

#### 1. Self-Play Generation
- Multiple concurrent games between agents
- Collects experiences with chess-specific metadata
- Uses multi-process architecture for 3-4x speedup

#### 2. Experience Processing
- Stores experiences in circular replay buffer
- Applies quality filtering and assessment
- Maintains buffer utilization and diversity

#### 3. Batch Training
- Samples random batches from experience buffer
- Computes target Q-values using target network
- Updates main network using Huber loss
- Applies legal action masking throughout

#### 4. Target Network Updates
- Copies main network weights to target network
- Occurs every `targetUpdateFrequency` updates
- Provides training stability

#### 5. Evaluation and Checkpointing
- Regular evaluation against baselines
- Automatic model checkpointing
- Performance tracking and logging

## Practical Usage

### Basic Training

```bash
# Quick development training
./gradlew :integration:run --args="--train --profile fast-debug"

# Production training
./gradlew :integration:run --args="--train --profile long-train --cycles 200"

# Custom parameters
./gradlew :integration:run --args="--train --cycles 50 --learning-rate 0.0005 --seed 12345"
```

### Configuration Parameters

#### Neural Network
```kotlin
hiddenLayers = listOf(512, 256, 128)  // Network architecture
learningRate = 0.001                  // Adam optimizer learning rate
batchSize = 64                        // Training batch size
```

#### RL Algorithm
```kotlin
gamma = 0.99                          // Discount factor
targetUpdateFrequency = 100           // Target network update frequency
maxExperienceBuffer = 50000           // Replay buffer size
explorationRate = 0.1                 // Epsilon-greedy exploration
```

#### Self-Play
```kotlin
gamesPerCycle = 20                    // Games per training cycle
maxConcurrentGames = 4                // Parallel game limit
maxStepsPerGame = 80                  // Maximum moves per game
```

### Monitoring Training Progress

#### Key Metrics
- **Loss**: Huber loss indicating learning progress
- **Policy Entropy**: Measure of action distribution diversity
- **Win Rate**: Performance in self-play evaluation
- **Average Game Length**: Efficiency of play

#### Log Messages
```
🔁 DQN target network synchronized at update=100 (freq=100)
🧩 DQN next-state action masking enabled (provider set)
ℹ️ Training cycle 10/100 completed
   Games: 20, Win rate: 45%, Avg length: 67.3
   Avg reward: 0.1500, Duration: 45s
```

### Evaluation

```bash
# Evaluate against heuristic baseline
./gradlew :integration:run --args="--evaluate --baseline --games 100"

# Evaluate against minimax depth-2
./gradlew :integration:run --args="--evaluate --baseline --opponent minimax --depth 2"

# Compare two models
./gradlew :integration:run --args="--evaluate --compare --modelA model1.json --modelB model2.json"
```

## Advanced Features

### Double DQN
Reduces overestimation bias by using main network for action selection and target network for evaluation:

```kotlin
// Enable in configuration
doubleDqn = true

// Or via CLI
./gradlew :integration:run --args="--train --double-dqn"
```

### Action Masking Implementation
Critical for chess to ensure only legal moves are considered:

```kotlin
// During action selection
val legalActions = environment.getLegalActions()
val action = dqn.selectAction(state, legalActions, explorationRate)

// During target computation
val nextLegalActions = environment.getLegalActions(nextState)
val maxQValue = nextLegalActions.maxOf { qValues[it] }
```

### Multi-Process Training
Automatic parallel self-play with fallback to sequential:

```kotlin
// Automatically uses multi-process if maxConcurrentGames > 1
val config = ChessRLConfig(maxConcurrentGames = 8)

// Falls back to sequential if multi-process fails
// Provides 3-4x speedup when working
```

## Teacher-Guided Bootstrap

For faster learning, the system supports teacher-guided initialization using minimax:

### 1. Generate Teacher Dataset
```bash
./gradlew :chess-engine:runTeacherCollector -Dargs="--collect --games 50 --depth 2 --topk 5 --tau 1.0 --out data/teacher.ndjson"
```

### 2. Imitation Pretraining
```bash
./gradlew :chess-engine:runImitationTrainer -Dargs="--train-imitation --data data/teacher.ndjson --epochs 3 --batch 64 --lr 0.001 --out data/imitation_qnet.json"
```

### 3. DQN Fine-tuning
```bash
./gradlew :integration:run --args="--train --load data/imitation_qnet.json --cycles 50"
```

## Performance Optimization

### Recommended Architectures

**Small Networks** (fast training):
```kotlin
hiddenLayers = listOf(256, 128)
learningRate = 0.001
```

**Balanced Networks** (good performance):
```kotlin
hiddenLayers = listOf(512, 256, 128)
learningRate = 0.0005
```

**Large Networks** (maximum capacity):
```kotlin
hiddenLayers = listOf(768, 512, 256)
learningRate = 0.0003
```

### Hyperparameter Guidelines

**Conservative (Stable)**:
```kotlin
learningRate = 0.0005
gamma = 0.99
targetUpdateFrequency = 200
explorationRate = 0.05
```

**Aggressive (Fast Learning)**:
```kotlin
learningRate = 0.001
gamma = 0.95
targetUpdateFrequency = 100
explorationRate = 0.1
```

## Troubleshooting

### Common Issues

**High Draw Rate**: 
- Reduce `maxStepsPerGame` to 80-100
- Increase `drawReward` penalty to -0.3
- Enable position-based reward shaping

**Slow Learning**:
- Increase `learningRate` to 0.001
- Reduce `targetUpdateFrequency` to 50
- Use teacher-guided bootstrap

**Training Instability**:
- Reduce `learningRate` to 0.0005
- Increase `targetUpdateFrequency` to 200
- Enable Double DQN

### Verification Checklist

✅ **Action Masking**: Look for masking logs in training output
✅ **Target Updates**: Verify target network sync messages
✅ **Legal Moves**: Ensure `invalid_moves` count is 0 in evaluation
✅ **Learning Progress**: Monitor decreasing loss and changing entropy
✅ **Baseline Performance**: Regular evaluation against minimax

## Implementation Details

### Numerical Stability
- Huber loss for robust training
- Gradient clipping to prevent explosion
- Proper weight initialization
- Action masking prevents invalid Q-value corruption

### Memory Management
- Circular experience buffer prevents memory leaks
- Efficient batch sampling
- Automatic checkpoint cleanup
- Process isolation for concurrent training

### Thread Safety
- Experience buffer is thread-safe
- Network updates are atomic
- Safe for multi-process scenarios
- Proper resource cleanup

## Future Enhancements

### Algorithmic Improvements
- **Prioritized Experience Replay**: Sample important experiences more frequently
- **Dueling DQN**: Separate value and advantage streams
- **Rainbow DQN**: Combination of multiple DQN improvements
- **Distributional RL**: Model full return distribution

### Chess-Specific Enhancements
- **Opening Books**: Initialize with known good openings
- **Endgame Tables**: Perfect play in simple endgames
- **Position Evaluation**: More sophisticated reward shaping
- **Time Control**: Training with chess clocks

## References and Further Reading

- **Original DQN Paper**: "Human-level control through deep reinforcement learning" (Mnih et al., 2015)
- **Double DQN**: "Deep Reinforcement Learning with Double Q-learning" (van Hasselt et al., 2016)
- **Chess RL**: "Mastering Chess and Shogi by Self-Play with a General Reinforcement Learning Algorithm" (Silver et al., 2017)

---

This implementation provides a robust, production-ready DQN system specifically designed for chess training. The combination of proper action masking, stable training procedures, and chess-specific optimizations enables training of competitive chess agents.