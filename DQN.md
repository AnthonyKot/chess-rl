# Deep Q-Network (DQN) Implementation Guide

A comprehensive guide to the Deep Q-Network implementation in the chess RL system. This document explains the algorithm, implementation details, and practical usage for training competitive chess agents.

## Algorithm Overview

Deep Q-Network (DQN) is a reinforcement learning algorithm that learns an action-value function Q(s, a) estimating the expected discounted return when taking action a in state s and following the current policy thereafter.

### Key Components

**Neural Network Q-Function**: Approximates Q-values for all actions given a state
- Input: 839-dimensional chess position encoding
- Output: 4096 Q-values (one for each possible chess move)
- Architecture: Configurable hidden layers with ReLU activation

**Target Network**: Periodically updated copy of the main network for stability
- Provides stable targets for Q-learning updates
- Updated every `targetUpdateFrequency` optimiser steps (default: 200 in production profiles)
- Prevents moving-target drift in the neural network

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
Terminal rewards and penalties:
- **Win**: +1.0
- **Loss**: -1.0
- **Draw**: 0.0 by default (long-train profiles apply -0.05 to discourage early draws)
- **Step Limit Penalty**: -0.5 (production profiles increase to -0.6)

## Implementation Architecture

### Core Classes

#### DQNAlgorithm (`rl-framework/RLAlgorithms.kt`)
Main DQN implementation with Q-learning, target updates, and experience replay.

```kotlin
val dqn = DQNAlgorithm(
    stateSize = 839,
    actionSize = 4096,
    hiddenLayers = listOf(512, 256, 128),
    learningRate = 5e-4,
    gamma = 0.99,
    targetUpdateFrequency = 200,
    doubleDqn = true
)
```

#### TrainingPipeline (`integration/TrainingPipeline.kt`)
Orchestrates self-play, experience collection, batch training, and evaluation.

```kotlin
val pipeline = TrainingPipeline(config)
if (pipeline.initialize()) {
    val results = pipeline.runTraining()
    println("Training complete: ${results.totalGamesPlayed} games")
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

# Production training (DL4J backend, multi-process self-play)
./gradlew :integration:run --args="--train --profile long-train --nn dl4j --cycles 200"

# Custom parameters
./gradlew :integration:run --args="--train --nn dl4j --cycles 60 --games-per-cycle 60 --seed 12345"
```

### Configuration Parameters

#### Neural Network
```kotlin
hiddenLayers = listOf(512, 256, 128)  // Balanced network
learningRate = 0.0005                 // Stable Adam learning rate
batchSize = 64                        // Batch size
```

#### RL Algorithm
```kotlin
gamma = 0.99                          // Discount factor
targetUpdateFrequency = 200           // Target sync cadence
maxExperienceBuffer = 50000           // Replay buffer size
explorationRate = 0.05                // Epsilon-greedy exploration
doubleDqn = true                      // Overestimation mitigation
```

#### Self-Play
```kotlin
gamesPerCycle = 30                    // Games per training cycle
maxConcurrentGames = 4                // Parallel game limit
maxStepsPerGame = 120                 // Maximum moves per game
```

### Monitoring Training Progress

#### Key Metrics
- **Loss**: Huber loss indicating learning progress
- **Policy Entropy**: Measure of action distribution diversity
- **Win Rate**: Performance in self-play evaluation
- **Average Game Length**: Efficiency of play

#### Log Messages
```
🔁 DQN target network synchronized at update=200 (freq=200)
🛡️ Legal-action mask applied (4096 → 28)
ℹ️ Training cycle 10/200 completed
   Games: 30, Win rate: 38%, Avg length: 112.4
   Avg reward: -0.0020, Duration: 3m 12s
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
Reduces overestimation bias by using the online network for action selection and the target network for evaluation:

```kotlin
doubleDqn = true          // default in profiles
```

### Action Masking
Ensures only legal moves contribute to policies and TD targets:

```kotlin
val legal = environment.getValidActions(state)
val action = policy.selectAction(qValues, legal)

val nextLegal = environment.getValidActions(stepResult.nextState)
val maskedMax = nextLegal.maxOf { nextQ[it] }
```

### Multi-Process Training
Self-play games run in worker JVMs when `maxConcurrentGames > 1`, yielding ~3–4× throughput. Use `--worker-heap <size>` to cap per-worker memory usage. The pipeline automatically falls back to sequential play if workers fail to start.

### Prioritized Replay (optional)
Set `replayType = PRIORITIZED` to bias sampling toward high-error transitions. The `long-train-mixed` profile enables this and mixes in heuristic/minimax opponents to diversify experience.

## Performance Guidelines

### Recommended Architectures

**Small Networks** (fast iteration):
```kotlin
hiddenLayers = listOf(256, 128)
learningRate = 0.001
```

**Balanced Networks** (default long-run):
```kotlin
hiddenLayers = listOf(512, 256, 128)
learningRate = 5e-4
```

**Large Networks** (maximum capacity):
```kotlin
hiddenLayers = listOf(768, 512, 256)
learningRate = 0.0003
```

### Hyperparameter Guidelines

**Conservative (Stable)**:
```kotlin
learningRate = 5e-4
gamma = 0.99
targetUpdateFrequency = 300
explorationRate = 0.03
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
- Lower `maxStepsPerGame` to ~100
- Apply a small negative `drawReward` (e.g., -0.05)
- Randomise starting FENs to inject sharper positions

**Slow Learning**:
- Raise `learningRate` toward 1e-3
- Reduce `targetUpdateFrequency` to 100–150
- Start with higher exploration (0.12) and decay gradually

**Training Instability**:
- Drop `learningRate` to 5e-4
- Increase `targetUpdateFrequency` to 200–300
- Keep Double DQN and gradient clipping (±1.0) enabled

### Verification Checklist

✅ **Action Masking**: Look for masking logs in training output
✅ **Target Updates**: Verify target network sync messages
✅ **Legal Moves**: Ensure `invalid_moves` count is 0 in evaluation
✅ **Learning Progress**: Monitor decreasing loss and changing entropy
✅ **Opponent Baseline**: Regular evaluation against heuristic/minimax opponents

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
