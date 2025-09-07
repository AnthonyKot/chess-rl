# Design Document

## Overview

The Chess RL Bot system consists of three modular Kotlin packages that work together to enable reinforcement learning for chess. The system is designed for native compilation and local execution with minimal external dependencies.

### Architecture Principles
- **Modular Design**: Three independent packages with clear interfaces
- **Native Performance**: Kotlin/Native compilation for optimal local execution
- **Incremental Development**: Simple components first, complex integrations later
- **Testability**: Each component is independently testable
- **Extensibility**: Chess engine can support human players, RL framework can handle other games

## Architecture

### High-Level System Architecture

```mermaid
graph TB
    subgraph "Chess RL Bot System"
        subgraph "Neural Network Package"
            NN[Neural Network]
            TR[Training Engine]
            AC[Activation Functions]
        end
        
        subgraph "RL Microframework"
            ENV[Environment Interface]
            AGT[Agent Interface]
            ALG[RL Algorithms]
            EXP[Experience Buffer]
        end
        
        subgraph "Chess Implementation"
            BD[Board Representation]
            MV[Move Validation]
            API[Chess API]
            PGN[PGN Parser]
        end
        
        subgraph "Integration Layer"
            CE[Chess Environment]
            CA[Chess Agent]
            SP[Self-Play Engine]
        end
        
        subgraph "Training Interface"
            UI[Training Controller]
            MON[Progress Monitor]
            CFG[Configuration]
        end
    end
    
    NN --> AGT
    API --> ENV
    CE --> SP
    CA --> SP
    UI --> SP
    PGN --> CE
```

### Package Dependencies
- **Neural Network Package**: No dependencies on other packages
- **RL Microframework**: Depends on Neural Network Package
- **Chess Implementation**: No dependencies on other packages  
- **Integration Layer**: Depends on all three packages
- **Training Interface**: Depends on Integration Layer

## Components and Interfaces

### 1. Neural Network Package (`nn-package`)

#### Core Components

**Complete Training-Ready Neural Network**
```kotlin
interface NeuralNetwork {
    // Forward and backward propagation
    fun forward(input: DoubleArray): DoubleArray
    fun backward(target: DoubleArray): DoubleArray  // Returns loss
    fun backwardWithGradients(outputGradients: DoubleArray): DoubleArray  // For RL policy gradients
    
    // Training utilities
    fun train(dataset: Dataset, epochs: Int, batchSize: Int): TrainingHistory
    fun evaluate(testData: Dataset): EvaluationMetrics
    fun predict(input: DoubleArray): DoubleArray
    
    // Optimization and regularization
    fun setOptimizer(optimizer: Optimizer)
    fun setLossFunction(lossFunction: LossFunction)
    fun addRegularization(regularization: Regularization)
    
    // Model management
    fun getWeights(): NetworkWeights
    fun setWeights(weights: NetworkWeights)
    fun clone(): NeuralNetwork  // For target networks in RL
    fun save(path: String)
    fun load(path: String)
}

// Complete training infrastructure
interface Dataset {
    fun getBatch(batchSize: Int): Batch
    fun shuffle()
    fun size(): Int
}

data class Batch(
    val inputs: Array<DoubleArray>,
    val targets: Array<DoubleArray>
)

interface Optimizer {
    fun updateWeights(weights: NetworkWeights, gradients: NetworkWeights, learningRate: Double)
}

// Implementations: SGD, Adam, RMSprop
class AdamOptimizer(
    private val beta1: Double = 0.9,
    private val beta2: Double = 0.999,
    private val epsilon: Double = 1e-8
) : Optimizer

interface LossFunction {
    fun computeLoss(predicted: DoubleArray, target: DoubleArray): Double
    fun computeGradient(predicted: DoubleArray, target: DoubleArray): DoubleArray
}

// Implementations: MSE, CrossEntropy, Huber (for RL)
```

**Layer Implementation**
```kotlin
interface Layer {
    fun forward(input: DoubleArray): DoubleArray
    fun backward(gradient: DoubleArray): DoubleArray
    fun updateWeights(learningRate: Double)
}

class DenseLayer(
    inputSize: Int,
    outputSize: Int,
    activation: ActivationFunction
) : Layer
```

**Activation Functions**
```kotlin
interface ActivationFunction {
    fun activate(x: Double): Double
    fun derivative(x: Double): Double
}

// Implementations: ReLU, Sigmoid, Tanh, Linear
```

#### Key Design Decisions
- **Matrix Operations**: Custom implementation for Kotlin/Native compatibility
- **Weight Initialization**: Xavier/He initialization for stable training
- **Gradient Computation**: Automatic differentiation through backpropagation
- **Memory Management**: Efficient array reuse to minimize allocations

### 2. Chess Implementation (`chess-engine`)

#### Core Components

**Board Representation**
```kotlin
data class ChessBoard(
    private val pieces: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    private val gameState: GameState = GameState()
) {
    fun makeMove(move: Move): MoveResult
    fun getAllValidMoves(color: PieceColor): List<Move>
    fun isInCheck(color: PieceColor): Boolean
    fun isCheckmate(color: PieceColor): Boolean
    fun isStalemate(color: PieceColor): Boolean
    fun toFEN(): String
    fun fromFEN(fen: String)
}
```

**Move Validation**
```kotlin
interface MoveValidator {
    fun isValidMove(board: ChessBoard, move: Move): Boolean
    fun getValidMoves(board: ChessBoard, position: Position): List<Move>
}

// Piece-specific validators: PawnValidator, RookValidator, etc.
```

**Chess API**
```kotlin
interface ChessGame {
    fun startNewGame()
    fun makeMove(move: Move): MoveResult
    fun getCurrentPosition(): ChessBoard
    fun getGameStatus(): GameStatus
    fun getValidMoves(): List<Move>
    fun undoMove()
    fun getGameHistory(): List<Move>
}
```

**PGN Support**
```kotlin
interface PGNParser {
    fun parseGame(pgn: String): List<Move>
    fun parseDatabase(pgnFile: String): List<List<Move>>
    fun gameToFEN(moves: List<Move>): List<String>
}
```

#### Key Design Decisions
- **Board Representation**: 8x8 array for simplicity and performance
- **Move Encoding**: Algebraic notation with internal coordinate system
- **Game State**: Separate tracking of castling rights, en passant, etc.
- **Validation Strategy**: Piece-specific validators with common interface

### 3. RL Microframework (`rl-framework`)

#### Core Components

**Environment Interface**
```kotlin
interface Environment<S, A> {
    fun reset(): S
    fun step(action: A): StepResult<S>
    fun getValidActions(state: S): List<A>
    fun isTerminal(state: S): Boolean
    fun getStateSize(): Int
    fun getActionSize(): Int
}

data class StepResult<S>(
    val nextState: S,
    val reward: Double,
    val done: Boolean,
    val info: Map<String, Any> = emptyMap()
)
```

**Agent Interface**
```kotlin
interface Agent<S, A> {
    fun selectAction(state: S, validActions: List<A>): A
    fun learn(experience: Experience<S, A>)
    fun setExplorationRate(epsilon: Double)
    fun save(path: String)
    fun load(path: String)
}

data class Experience<S, A>(
    val state: S,
    val action: A,
    val reward: Double,
    val nextState: S,
    val done: Boolean
)
```

**RL Algorithms with Detailed Policy Updates**
```kotlin
interface RLAlgorithm<S, A> {
    // Core RL operations
    fun updatePolicy(experiences: List<Experience<S, A>>): PolicyUpdateResult
    fun getActionValues(state: S, validActions: List<A>): Map<A, Double>
    fun getActionProbabilities(state: S, validActions: List<A>): Map<A, Double>
    
    // Training control
    fun setLearningRate(rate: Double)
    fun setExplorationStrategy(strategy: ExplorationStrategy)
    fun getTrainingMetrics(): RLMetrics
}

data class PolicyUpdateResult(
    val loss: Double,
    val gradientNorm: Double,
    val policyEntropy: Double,
    val valueError: Double? = null  // For actor-critic methods
)

class DQNAlgorithm(
    private val qNetwork: NeuralNetwork,
    private val targetNetwork: NeuralNetwork,
    private val experienceReplay: ExperienceReplay<DoubleArray, Int>
) : RLAlgorithm<DoubleArray, Int> {
    
    override fun updatePolicy(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        // Detailed DQN update with target network, experience replay, etc.
        val batch = experienceReplay.sample(batchSize)
        
        // Compute Q-targets using target network
        val qTargets = computeQTargets(batch)
        
        // Train main network
        val trainingBatch = createTrainingBatch(batch, qTargets)
        val trainingResult = qNetwork.train(trainingBatch, epochs = 1, batchSize = batch.size)
        
        // Update target network periodically
        if (shouldUpdateTargetNetwork()) {
            updateTargetNetwork()
        }
        
        return PolicyUpdateResult(
            loss = trainingResult.finalLoss,
            gradientNorm = trainingResult.gradientNorm,
            policyEntropy = calculatePolicyEntropy(batch)
        )
    }
    
    private fun computeQTargets(batch: List<Experience<DoubleArray, Int>>): Array<DoubleArray> {
        // Bellman equation: Q(s,a) = r + γ * max_a' Q(s',a')
        return batch.map { experience ->
            val nextQValues = if (experience.done) {
                doubleArrayOf(experience.reward)
            } else {
                val nextStateValues = targetNetwork.predict(experience.nextState)
                doubleArrayOf(experience.reward + gamma * nextStateValues.maxOrNull()!!)
            }
            nextQValues
        }.toTypedArray()
    }
}

// Policy Gradient Algorithm for comparison/alternative
class PolicyGradientAlgorithm(
    private val policyNetwork: NeuralNetwork,
    private val valueNetwork: NeuralNetwork? = null  // Optional baseline
) : RLAlgorithm<DoubleArray, Int> {
    
    override fun updatePolicy(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        // REINFORCE or Actor-Critic policy update
        val returns = calculateReturns(experiences)
        val baselines = valueNetwork?.let { vNet ->
            experiences.map { vNet.predict(it.state)[0] }
        } ?: List(experiences.size) { 0.0 }
        
        val advantages = returns.zip(baselines) { ret, baseline -> ret - baseline }
        
        // Policy gradient update
        val policyLoss = updatePolicyNetwork(experiences, advantages)
        
        // Value network update (if using baseline)
        val valueLoss = valueNetwork?.let { vNet ->
            updateValueNetwork(experiences, returns)
        } ?: 0.0
        
        return PolicyUpdateResult(
            loss = policyLoss,
            gradientNorm = calculateGradientNorm(),
            policyEntropy = calculatePolicyEntropy(experiences),
            valueError = valueLoss
        )
    }
}
```

#### Key Design Decisions
- **Generic Design**: Type parameters for state and action spaces
- **Experience Replay**: Buffer for storing and sampling experiences with prioritization
- **Target Networks**: Separate target network for stable Q-learning with soft/hard updates
- **Exploration Strategy**: Multiple strategies (epsilon-greedy, UCB, Thompson sampling)
- **Policy Update Validation**: Detailed metrics to verify learning progress
- **Algorithm Flexibility**: Support for both value-based (DQN) and policy-based (REINFORCE) methods

#### RL Training Validation Framework
```kotlin
interface RLValidator {
    fun validatePolicyUpdate(
        beforeMetrics: RLMetrics,
        afterMetrics: RLMetrics,
        updateResult: PolicyUpdateResult
    ): ValidationResult
    
    fun validateConvergence(trainingHistory: List<RLMetrics>): ConvergenceStatus
    fun detectTrainingIssues(metrics: RLMetrics): List<TrainingIssue>
}

data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val recommendations: List<String>
)

enum class TrainingIssue {
    EXPLODING_GRADIENTS,
    VANISHING_GRADIENTS,
    POLICY_COLLAPSE,
    VALUE_OVERESTIMATION,
    EXPLORATION_INSUFFICIENT,
    LEARNING_RATE_TOO_HIGH,
    LEARNING_RATE_TOO_LOW
}

// Specific chess RL validation
class ChessRLValidator : RLValidator {
    fun validateChessSpecificMetrics(
        gameResults: List<GameResult>,
        policyEntropy: Double,
        averageGameLength: Double
    ): ChessValidationResult
}
```

### 4. Integration Layer

#### Chess Environment Implementation
```kotlin
class ChessEnvironment : Environment<DoubleArray, Int> {
    private val chessGame: ChessGame
    private val stateEncoder: ChessStateEncoder
    private val actionDecoder: ChessActionDecoder
    
    override fun reset(): DoubleArray {
        chessGame.startNewGame()
        return stateEncoder.encode(chessGame.getCurrentPosition())
    }
    
    override fun step(action: Int): StepResult<DoubleArray> {
        val move = actionDecoder.decode(action, chessGame.getValidMoves())
        val result = chessGame.makeMove(move)
        return StepResult(
            nextState = stateEncoder.encode(chessGame.getCurrentPosition()),
            reward = calculateReward(result),
            done = chessGame.getGameStatus().isGameOver
        )
    }
}
```

#### State Encoding Strategy
- **Board Representation**: 8x8x12 tensor (6 piece types × 2 colors)
- **Additional Features**: Castling rights, en passant, move count
- **Normalization**: Values scaled to [-1, 1] range
- **Total Input Size**: 768 + 7 = 775 features

#### Action Encoding Strategy
- **Move Representation**: From-square (64) × To-square (64) = 4096 possible moves
- **Promotion Handling**: Additional encoding for pawn promotion pieces
- **Action Masking**: Only valid moves considered during action selection

## Data Models

### Core Data Structures

```kotlin
// Chess Domain
data class Position(val rank: Int, val file: Int)
data class Move(val from: Position, val to: Position, val promotion: PieceType? = null)
data class Piece(val type: PieceType, val color: PieceColor)

enum class PieceType { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }
enum class PieceColor { WHITE, BLACK }

// Neural Network Domain
data class NetworkConfig(
    val inputSize: Int,
    val hiddenLayers: List<Int>,
    val outputSize: Int,
    val learningRate: Double,
    val activationFunction: ActivationFunction
)

// RL Domain
data class TrainingConfig(
    val episodes: Int,
    val maxStepsPerEpisode: Int,
    val explorationRate: Double,
    val explorationDecay: Double,
    val batchSize: Int,
    val targetUpdateFrequency: Int
)
```

## Error Handling

### Error Categories and Strategies

**Chess Engine Errors**
- **Invalid Moves**: Return error result with explanation
- **Illegal Positions**: Validate FEN strings and board states
- **PGN Parsing**: Handle malformed notation gracefully

**Neural Network Errors**
- **Numerical Instability**: Gradient clipping and learning rate adjustment
- **Dimension Mismatches**: Runtime validation of input/output sizes
- **Memory Issues**: Efficient tensor operations and cleanup

**RL Training Errors**
- **Convergence Issues**: Early stopping and learning rate scheduling
- **Experience Buffer**: Handle memory limits and data corruption
- **Action Selection**: Fallback to random valid moves

**Integration Errors**
- **State Encoding**: Validate chess position to neural network input conversion
- **Action Decoding**: Ensure neural network outputs map to valid chess moves
- **Component Communication**: Clear error propagation between modules

### Error Recovery Mechanisms
```kotlin
sealed class ChessRLError : Exception() {
    data class InvalidMove(val move: Move, val reason: String) : ChessRLError()
    data class NetworkError(val operation: String, val cause: Throwable) : ChessRLError()
    data class TrainingError(val episode: Int, val cause: Throwable) : ChessRLError()
}

interface ErrorHandler {
    fun handleError(error: ChessRLError): RecoveryAction
}

enum class RecoveryAction { RETRY, SKIP, ABORT, FALLBACK }
```

## Testing Strategy

### Unit Testing Approach

**Chess Engine Testing**
- **Move Validation**: Comprehensive test cases for all piece types
- **Game Rules**: Test checkmate, stalemate, and draw detection
- **PGN Parsing**: Validate against standard chess databases
- **Performance**: Benchmark move generation and validation speed

**Neural Network Testing**
- **Forward Pass**: Verify output dimensions and value ranges
- **Backward Pass**: Test gradient computation with numerical differentiation
- **Training Completeness**: Test full training pipeline with various datasets:
  - XOR problem (classification)
  - Polynomial regression (regression)
  - MNIST subset (if needed for validation)
- **Optimization**: Test different optimizers (SGD, Adam) on known problems
- **Loss Functions**: Validate MSE, CrossEntropy, Huber loss implementations
- **Regularization**: Test L1/L2 regularization and dropout
- **Serialization**: Test save/load functionality with trained models

**RL Framework Testing**
- **Environment Interface**: Test with simple grid world environment
- **Agent Behavior**: Verify exploration/exploitation balance with metrics
- **Experience Replay**: Test buffer operations, sampling, and prioritization
- **Algorithm Correctness**: 
  - DQN on CartPole or simple grid world
  - Policy Gradient on bandit problems
  - Compare learning curves against known benchmarks
- **Policy Update Validation**:
  - Verify Bellman equation implementation
  - Test gradient computation for policy methods
  - Validate target network updates
  - Check exploration strategy effectiveness
- **Training Stability**:
  - Test with different learning rates
  - Verify convergence detection
  - Test recovery from training issues

### Integration Testing

**Component Integration**
- **Chess-RL Integration**: Test state encoding/action decoding
- **Network-Agent Integration**: Verify neural network integration
- **Self-Play Testing**: Run short games to validate complete pipeline

**End-to-End Testing**
- **Training Pipeline**: Run abbreviated training sessions
- **Performance Monitoring**: Verify metrics collection and reporting
- **Error Handling**: Test recovery from various failure scenarios

### Test Data and Fixtures
- **Chess Positions**: Standard tactical puzzles and endgame positions
- **PGN Games**: Small database of master games for validation
- **Neural Network**: Pre-trained weights for regression testing
- **RL Scenarios**: Deterministic environments for algorithm validation

This design provides a solid foundation for incremental development, starting with the simple components and building up to the complex integrations while maintaining modularity and testability throughout.