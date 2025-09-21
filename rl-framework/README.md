# RL Framework

A focused reinforcement learning framework implementing Deep Q-Network (DQN) algorithm with experience replay and exploration strategies. Designed specifically for discrete action spaces like chess.

## Purpose

The RL framework provides the core reinforcement learning algorithms for training chess agents. It implements:

- **DQN Algorithm**: Deep Q-Network with target networks and double DQN support
- **Experience Replay**: Circular buffer for storing and sampling experiences
- **Exploration Strategies**: Epsilon-greedy exploration with decay
- **Action Masking**: Ensures only legal actions are selected

## Key Classes

### DQNAlgorithm
The core DQN implementation with target network updates and experience replay.

```kotlin
// Create DQN algorithm
val dqn = DQNAlgorithm(
    stateSize = 776,
    actionSize = 4096,
    hiddenLayers = listOf(512, 256, 128),
    learningRate = 0.001,
    gamma = 0.99,
    targetUpdateFrequency = 100
)

// Training step
val loss = dqn.trainOnBatch(experiences)

// Action selection
val action = dqn.selectAction(state, legalActions, explorationRate)
```

### ExperienceReplay
Circular buffer for storing and sampling training experiences.

```kotlin
// Create experience buffer
val buffer = ExperienceReplay<DoubleArray, Int>(capacity = 50000)

// Store experience
val experience = Experience(
    state = currentState,
    action = selectedAction,
    reward = gameReward,
    nextState = nextState,
    done = gameFinished
)
buffer.add(experience)

// Sample for training
val batch = buffer.sample(batchSize = 64)
```

### EpsilonGreedyExploration
Exploration strategy with configurable decay.

```kotlin
// Create exploration strategy
val exploration = EpsilonGreedyExploration(
    initialEpsilon = 0.1,
    minEpsilon = 0.01,
    decayRate = 0.995
)

// Select action with exploration
val action = if (exploration.shouldExplore()) {
    legalActions.random()
} else {
    selectBestAction(qValues, legalActions)
}

// Update exploration rate
exploration.decay()
```

## Usage Examples

### Basic DQN Training Loop

```kotlin
import com.chessrl.rl.*

// Initialize components
val dqn = DQNAlgorithm(
    stateSize = 776,
    actionSize = 4096,
    hiddenLayers = listOf(512, 256, 128),
    learningRate = 0.001,
    gamma = 0.99,
    targetUpdateFrequency = 100
)

val experienceBuffer = ExperienceReplay<DoubleArray, Int>(50000)
val exploration = EpsilonGreedyExploration(0.1, 0.01, 0.995)

// Training loop
for (episode in 1..1000) {
    var state = environment.reset()
    var totalReward = 0.0
    
    while (!environment.isDone()) {
        // Select action
        val legalActions = environment.getLegalActions()
        val action = dqn.selectAction(state, legalActions, exploration.epsilon)
        
        // Execute action
        val stepResult = environment.step(action)
        totalReward += stepResult.reward
        
        // Store experience
        val experience = Experience(
            state = state,
            action = action,
            reward = stepResult.reward,
            nextState = stepResult.nextState,
            done = stepResult.done
        )
        experienceBuffer.add(experience)
        
        // Train if enough experiences
        if (experienceBuffer.size() >= 1000) {
            val batch = experienceBuffer.sample(64)
            val loss = dqn.trainOnBatch(batch)
        }
        
        state = stepResult.nextState
    }
    
    // Update exploration and target network
    exploration.decay()
    dqn.updateTargetNetwork()
    
    println("Episode $episode: Reward = $totalReward, Epsilon = ${exploration.epsilon}")
}
```

### Action Masking for Legal Moves

```kotlin
// Select action with legal move masking
fun selectLegalAction(
    qValues: DoubleArray,
    legalActions: List<Int>,
    explorationRate: Double
): Int {
    return if (Random.nextDouble() < explorationRate) {
        // Random exploration from legal actions only
        legalActions.random()
    } else {
        // Greedy selection from legal actions only
        legalActions.maxByOrNull { qValues[it] } ?: legalActions.first()
    }
}

// Use in training
val qValues = dqn.forward(state)
val legalActions = chessEnvironment.getLegalActions()
val action = selectLegalAction(qValues, legalActions, exploration.epsilon)
```

### Experience Buffer Management

```kotlin
// Monitor buffer utilization
val utilizationRate = experienceBuffer.size().toDouble() / experienceBuffer.capacity
println("Buffer utilization: ${(utilizationRate * 100).toInt()}%")

// Sample with different batch sizes
val smallBatch = experienceBuffer.sample(32)  // Fast training
val largeBatch = experienceBuffer.sample(128) // Stable training

// Check if ready for training
if (experienceBuffer.size() >= minExperiences) {
    val batch = experienceBuffer.sample(batchSize)
    val loss = dqn.trainOnBatch(batch)
}
```

## API Reference

### DQNAlgorithm

#### Constructor
```kotlin
DQNAlgorithm(
    stateSize: Int,              // Input state dimension
    actionSize: Int,             // Number of possible actions
    hiddenLayers: List<Int>,     // Hidden layer architecture
    learningRate: Double = 0.001, // Learning rate
    gamma: Double = 0.99,        // Discount factor
    targetUpdateFrequency: Int = 100 // Target network update frequency
)
```

#### Key Methods
- `selectAction(state: DoubleArray, legalActions: List<Int>, epsilon: Double): Int`
  - Selects action using epsilon-greedy with legal action masking
  
- `trainOnBatch(experiences: List<Experience<DoubleArray, Int>>): Double`
  - Trains on batch of experiences, returns loss
  
- `updateTargetNetwork()`
  - Copies main network weights to target network
  
- `forward(state: DoubleArray): DoubleArray`
  - Forward pass returning Q-values for all actions

### ExperienceReplay

#### Constructor
```kotlin
ExperienceReplay<StateType, ActionType>(capacity: Int)
```

#### Key Methods
- `add(experience: Experience<StateType, ActionType>)`
  - Adds experience to buffer (overwrites oldest when full)
  
- `sample(batchSize: Int): List<Experience<StateType, ActionType>>`
  - Samples random batch for training
  
- `size(): Int`
  - Returns current number of stored experiences
  
- `isFull(): Boolean`
  - Checks if buffer is at capacity

### EpsilonGreedyExploration

#### Constructor
```kotlin
EpsilonGreedyExploration(
    initialEpsilon: Double,  // Starting exploration rate
    minEpsilon: Double,      // Minimum exploration rate
    decayRate: Double        // Decay factor per step
)
```

#### Key Methods
- `shouldExplore(): Boolean`
  - Returns true if should explore (random action)
  
- `decay()`
  - Decays epsilon by decay rate
  
- `epsilon: Double`
  - Current exploration rate

## Integration Points

### With Neural Network Package
The RL framework uses the neural network package for function approximation:

```kotlin
// DQN uses neural networks internally
val dqn = DQNAlgorithm(stateSize, actionSize, hiddenLayers, learningRate)
// Internally creates: NeuralNetwork(stateSize, hiddenLayers, actionSize)

// Target network is a copy of main network
dqn.updateTargetNetwork() // Copies main network to target network
```

### With Chess Environment
Designed for discrete action spaces like chess:

```kotlin
// Chess-specific usage
val chessStateSize = 776    // Chess position encoding
val chessActionSize = 4096  // All possible chess moves

val chessDQN = DQNAlgorithm(
    stateSize = chessStateSize,
    actionSize = chessActionSize,
    hiddenLayers = listOf(768, 512, 256),
    learningRate = 0.0005,
    gamma = 0.99
)

// Action selection with chess move masking
val legalMoves = chessBoard.getLegalMoves()
val legalActionIndices = legalMoves.map { it.toActionIndex() }
val action = chessDQN.selectAction(state, legalActionIndices, epsilon)
```

## Algorithm Details

### Deep Q-Network (DQN)
The implementation follows the standard DQN algorithm:

1. **Experience Collection**: Store (state, action, reward, next_state, done) tuples
2. **Batch Sampling**: Sample random batch from experience buffer
3. **Target Calculation**: Calculate target Q-values using target network
4. **Loss Computation**: Mean squared error between predicted and target Q-values
5. **Network Update**: Backpropagation to update main network
6. **Target Update**: Periodically copy main network to target network

### Target Network Updates
```kotlin
// Target Q-value calculation
val targetQValues = if (experience.done) {
    experience.reward
} else {
    experience.reward + gamma * targetNetwork.forward(experience.nextState).max()
}
```

### Double DQN Support
Optional double DQN for more stable training:
```kotlin
// Double DQN target calculation
val nextActions = mainNetwork.forward(nextState).argMax()
val targetQValues = experience.reward + gamma * targetNetwork.forward(nextState)[nextActions]
```

## Performance Characteristics

### Training Performance
- **Action Selection**: ~1ms for typical chess network
- **Batch Training**: ~10-20ms for 64-sample batch
- **Memory Usage**: ~100-500MB depending on buffer size
- **Convergence**: Typically 100-1000 episodes for simple tasks

### Hyperparameter Recommendations

#### Conservative (Stable)
```kotlin
learningRate = 0.0005
gamma = 0.99
targetUpdateFrequency = 200
epsilon = 0.05
bufferSize = 50000
```

#### Aggressive (Fast Learning)
```kotlin
learningRate = 0.001
gamma = 0.95
targetUpdateFrequency = 100
epsilon = 0.1
bufferSize = 100000
```

## Testing

The framework includes comprehensive tests:

- ✅ **DQN Learning**: Validates Q-value updates and convergence
- ✅ **Experience Replay**: Buffer operations and sampling
- ✅ **Exploration**: Epsilon-greedy behavior and decay
- ✅ **Action Masking**: Legal action selection
- ✅ **Target Networks**: Proper weight copying and updates

Run tests with:
```bash
./gradlew :rl-framework:test
```

## Implementation Details

### Numerical Stability
- Gradient clipping to prevent exploding gradients
- Proper initialization of Q-networks
- Stable target value computation

### Memory Efficiency
- Circular buffer implementation for experience replay
- Efficient batch sampling without memory leaks
- Proper cleanup of intermediate computations

### Thread Safety
- Experience buffer is thread-safe for concurrent access
- Network updates are atomic operations
- Safe for multi-process training scenarios

## Future Enhancements

Potential algorithm improvements:
- **Prioritized Experience Replay**: Sample important experiences more frequently
- **Dueling DQN**: Separate value and advantage streams
- **Rainbow DQN**: Combination of multiple DQN improvements
- **Distributional RL**: Model full return distribution instead of expected value

## Dependencies

- Kotlin Multiplatform
- Neural Network Package (internal dependency)
- Kotlin Test Framework
- No external dependencies

## Platform Support

- ✅ **JVM**: Full support with optimized performance
- ✅ **Native**: Kotlin/Native compatibility  
- ✅ **Cross-platform**: Consistent behavior across platforms

The RL framework provides a robust foundation for training reinforcement learning agents in discrete action spaces while maintaining simplicity and reliability.