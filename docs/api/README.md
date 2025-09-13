# API Reference Documentation

## Overview

This document provides comprehensive API documentation for the Chess RL Bot system, including usage examples and integration guides for all major components.

## Core Packages

### Neural Network Package (`nn-package`)

#### NeuralNetwork Interface

The core neural network interface providing complete training capabilities.

```kotlin
interface NeuralNetwork {
    // Core operations
    fun forward(input: DoubleArray): DoubleArray
    fun backward(target: DoubleArray): DoubleArray
    fun backwardWithGradients(outputGradients: DoubleArray): DoubleArray
    
    // Training operations
    fun train(dataset: Dataset, epochs: Int, batchSize: Int): TrainingHistory
    fun evaluate(testData: Dataset): EvaluationMetrics
    fun predict(input: DoubleArray): DoubleArray
    
    // Configuration
    fun setOptimizer(optimizer: Optimizer)
    fun setLossFunction(lossFunction: LossFunction)
    fun addRegularization(regularization: Regularization)
    
    // Model management
    fun getWeights(): NetworkWeights
    fun setWeights(weights: NetworkWeights)
    fun clone(): NeuralNetwork
    fun save(path: String)
    fun load(path: String)
}
```

**Usage Example**:
```kotlin
// Create a neural network for chess position evaluation
val network = DenseNeuralNetwork(
    inputSize = 776,
    hiddenLayers = listOf(512, 256, 128),
    outputSize = 4096,
    activationFunction = ReLU()
)

// Configure training
network.setOptimizer(AdamOptimizer(learningRate = 0.001))
network.setLossFunction(HuberLoss())

// Train the network
val dataset = ChessDataset(experiences)
val history = network.train(dataset, epochs = 100, batchSize = 64)

// Make predictions
val chessPosition = encodeChessPosition(board)
val actionProbabilities = network.predict(chessPosition)
```

#### Optimizer Classes

**AdamOptimizer**
```kotlin
class AdamOptimizer(
    private val learningRate: Double = 0.001,
    private val beta1: Double = 0.9,
    private val beta2: Double = 0.999,
    private val epsilon: Double = 1e-8
) : Optimizer
```

**RMSpropOptimizer**
```kotlin
class RMSpropOptimizer(
    private val learningRate: Double = 0.001,
    private val decay: Double = 0.9,
    private val epsilon: Double = 1e-8
) : Optimizer
```

**SGDOptimizer**
```kotlin
class SGDOptimizer(
    private val learningRate: Double = 0.01,
    private val momentum: Double = 0.0
) : Optimizer
```

#### Loss Functions

**MSELoss** - Mean Squared Error for regression tasks
```kotlin
class MSELoss : LossFunction {
    override fun computeLoss(predicted: DoubleArray, target: DoubleArray): Double
    override fun computeGradient(predicted: DoubleArray, target: DoubleArray): DoubleArray
}
```

**CrossEntropyLoss** - For classification tasks
```kotlin
class CrossEntropyLoss : LossFunction
```

**HuberLoss** - Robust loss function for RL (combines MSE and MAE)
```kotlin
class HuberLoss(private val delta: Double = 1.0) : LossFunction
```

### Chess Engine Package (`chess-engine`)

#### ChessBoard Class

Core chess board representation with complete rule implementation.

```kotlin
data class ChessBoard(
    private val pieces: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    private val gameState: GameState = GameState()
) {
    // Move operations
    fun makeMove(move: Move): MoveResult
    fun undoMove(): Boolean
    fun getAllValidMoves(color: PieceColor): List<Move>
    
    // Game state queries
    fun isInCheck(color: PieceColor): Boolean
    fun isCheckmate(color: PieceColor): Boolean
    fun isStalemate(color: PieceColor): Boolean
    fun isDraw(): DrawType?
    
    // Position representation
    fun toFEN(): String
    fun fromFEN(fen: String): Boolean
    fun copy(): ChessBoard
    
    // Piece operations
    fun getPiece(position: Position): Piece?
    fun setPiece(position: Position, piece: Piece?)
    fun isEmpty(position: Position): Boolean
}
```

**Usage Example**:
```kotlin
// Create a new chess board
val board = ChessBoard()
board.setupStandardPosition()

// Make moves
val move = Move(Position(1, 4), Position(3, 4)) // e2-e4
val result = board.makeMove(move)

if (result.isValid) {
    println("Move successful: ${result.notation}")
    
    // Check game state
    if (board.isCheckmate(PieceColor.BLACK)) {
        println("Checkmate! White wins.")
    }
} else {
    println("Invalid move: ${result.error}")
}

// Get all valid moves
val validMoves = board.getAllValidMoves(PieceColor.WHITE)
println("Valid moves: ${validMoves.size}")

// Export position
val fen = board.toFEN()
println("Position: $fen")
```

#### Move Validation

```kotlin
interface MoveValidator {
    fun isValidMove(board: ChessBoard, move: Move): Boolean
    fun getValidMoves(board: ChessBoard, position: Position): List<Move>
}

// Piece-specific validators
class PawnValidator : MoveValidator
class RookValidator : MoveValidator
class BishopValidator : MoveValidator
class KnightValidator : MoveValidator
class QueenValidator : MoveValidator
class KingValidator : MoveValidator
```

#### PGN Parser

```kotlin
interface PGNParser {
    fun parseGame(pgn: String): ParseResult<List<Move>>
    fun parseDatabase(pgnFile: String): ParseResult<List<List<Move>>>
    fun gameToFEN(moves: List<Move>): List<String>
    fun movesToPGN(moves: List<Move>): String
}

// Usage example
val parser = StandardPGNParser()
val gameResult = parser.parseGame(pgnString)

if (gameResult.isSuccess) {
    val moves = gameResult.value
    println("Parsed ${moves.size} moves")
    
    // Replay the game
    val board = ChessBoard()
    for (move in moves) {
        board.makeMove(move)
    }
} else {
    println("Parse error: ${gameResult.error}")
}
```

### RL Framework Package (`rl-framework`)

#### Environment Interface

Generic environment interface for reinforcement learning.

```kotlin
interface Environment<S, A> {
    fun reset(): S
    fun step(action: A): StepResult<S>
    fun getValidActions(state: S): List<A>
    fun isTerminal(state: S): Boolean
    fun getStateSize(): Int
    fun getActionSize(): Int
    fun render(): String
}

data class StepResult<S>(
    val nextState: S,
    val reward: Double,
    val done: Boolean,
    val info: Map<String, Any> = emptyMap()
)
```

#### Agent Interface

```kotlin
interface Agent<S, A> {
    fun selectAction(state: S, validActions: List<A>): A
    fun learn(experience: Experience<S, A>)
    fun setExplorationRate(epsilon: Double)
    fun getPolicy(state: S): Map<A, Double>
    fun save(path: String)
    fun load(path: String)
}

data class Experience<S, A>(
    val state: S,
    val action: A,
    val reward: Double,
    val nextState: S,
    val done: Boolean,
    val metadata: Map<String, Any> = emptyMap()
)
```

#### DQN Algorithm

```kotlin
class DQNAlgorithm(
    private val qNetwork: NeuralNetwork,
    private val targetNetwork: NeuralNetwork,
    private val experienceReplay: ExperienceReplay<DoubleArray, Int>,
    private val config: DQNConfig = DQNConfig()
) : RLAlgorithm<DoubleArray, Int> {
    
    override fun updatePolicy(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult
    override fun getActionValues(state: DoubleArray, validActions: List<Int>): Map<Int, Double>
    override fun setLearningRate(rate: Double)
    override fun getTrainingMetrics(): RLMetrics
}

data class DQNConfig(
    val gamma: Double = 0.99,
    val targetUpdateFrequency: Int = 100,
    val batchSize: Int = 64,
    val minExperienceSize: Int = 1000
)
```

**Usage Example**:
```kotlin
// Create DQN agent
val qNetwork = DenseNeuralNetwork(776, listOf(512, 256), 4096)
val targetNetwork = qNetwork.clone()
val experienceReplay = CircularExperienceReplay<DoubleArray, Int>(50000)

val dqn = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay)

// Training loop
for (episode in 1..1000) {
    var state = environment.reset()
    var totalReward = 0.0
    
    while (!environment.isTerminal(state)) {
        val validActions = environment.getValidActions(state)
        val action = agent.selectAction(state, validActions)
        val result = environment.step(action)
        
        val experience = Experience(
            state = state,
            action = action,
            reward = result.reward,
            nextState = result.nextState,
            done = result.done
        )
        
        agent.learn(experience)
        state = result.nextState
        totalReward += result.reward
    }
    
    println("Episode $episode: Reward = $totalReward")
}
```

### Integration Package (`integration`)

#### ChessEnvironment

Chess-specific environment implementation for RL.

```kotlin
class ChessEnvironment : Environment<DoubleArray, Int> {
    private val chessGame: ChessGame
    private val stateEncoder: ChessStateEncoder
    private val actionDecoder: ChessActionDecoder
    private val rewardCalculator: ChessRewardCalculator
    
    override fun reset(): DoubleArray {
        chessGame.startNewGame()
        return stateEncoder.encode(chessGame.getCurrentPosition())
    }
    
    override fun step(action: Int): StepResult<DoubleArray> {
        val validMoves = chessGame.getValidMoves()
        val move = actionDecoder.decode(action, validMoves)
        val result = chessGame.makeMove(move)
        
        val nextState = stateEncoder.encode(chessGame.getCurrentPosition())
        val reward = rewardCalculator.calculateReward(result)
        val done = chessGame.getGameStatus().isGameOver
        
        return StepResult(nextState, reward, done)
    }
    
    override fun getValidActions(state: DoubleArray): List<Int> {
        val validMoves = chessGame.getValidMoves()
        return validMoves.map { actionDecoder.encode(it) }
    }
}
```

#### ChessAgent

Chess-specific RL agent implementation.

```kotlin
class ChessAgent(
    private val neuralNetwork: NeuralNetwork,
    private val rlAlgorithm: RLAlgorithm<DoubleArray, Int>,
    private val explorationStrategy: ExplorationStrategy = EpsilonGreedyStrategy()
) : Agent<DoubleArray, Int> {
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val actionValues = rlAlgorithm.getActionValues(state, validActions)
        return explorationStrategy.selectAction(actionValues, validActions)
    }
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        rlAlgorithm.addExperience(experience)
        
        if (rlAlgorithm.canUpdate()) {
            val batch = rlAlgorithm.sampleBatch()
            val updateResult = rlAlgorithm.updatePolicy(batch)
            
            // Log training metrics
            logTrainingMetrics(updateResult)
        }
    }
    
    fun evaluatePosition(board: ChessBoard): PositionEvaluation {
        val state = stateEncoder.encode(board)
        val actionValues = neuralNetwork.predict(state)
        
        return PositionEvaluation(
            evaluation = actionValues.maxOrNull() ?: 0.0,
            bestMoves = getBestMoves(actionValues, board.getAllValidMoves(board.currentPlayer))
        )
    }
}
```

#### Training Pipeline

```kotlin
class ChessTrainingPipeline(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment,
    private val validator: TrainingValidator,
    private val config: TrainingConfig
) {
    
    fun trainAgent(): TrainingResult {
        val trainingHistory = mutableListOf<EpisodeResult>()
        
        for (episode in 1..config.episodes) {
            val episodeResult = runEpisode()
            trainingHistory.add(episodeResult)
            
            // Validate training progress
            if (episode % config.validationFrequency == 0) {
                val validationResult = validator.validateTraining(agent, trainingHistory)
                if (validationResult.hasIssues()) {
                    handleTrainingIssues(validationResult)
                }
            }
            
            // Save checkpoint
            if (episode % config.checkpointFrequency == 0) {
                saveCheckpoint(episode, agent)
            }
        }
        
        return TrainingResult(trainingHistory, validator.getFinalReport())
    }
    
    private fun runEpisode(): EpisodeResult {
        var state = environment.reset()
        val experiences = mutableListOf<Experience<DoubleArray, Int>>()
        var totalReward = 0.0
        var steps = 0
        
        while (!environment.isTerminal(state) && steps < config.maxStepsPerEpisode) {
            val validActions = environment.getValidActions(state)
            val action = agent.selectAction(state, validActions)
            val result = environment.step(action)
            
            val experience = Experience(
                state = state,
                action = action,
                reward = result.reward,
                nextState = result.nextState,
                done = result.done
            )
            
            experiences.add(experience)
            agent.learn(experience)
            
            state = result.nextState
            totalReward += result.reward
            steps++
        }
        
        return EpisodeResult(
            episode = currentEpisode,
            totalReward = totalReward,
            steps = steps,
            experiences = experiences,
            gameOutcome = determineGameOutcome()
        )
    }
}
```

## Configuration Classes

### Training Configuration

```kotlin
data class TrainingConfig(
    val episodes: Int = 1000,
    val maxStepsPerEpisode: Int = 200,
    val batchSize: Int = 64,
    val learningRate: Double = 0.001,
    val explorationRate: Double = 0.1,
    val explorationDecay: Double = 0.995,
    val targetUpdateFrequency: Int = 100,
    val experienceBufferSize: Int = 50000,
    val validationFrequency: Int = 100,
    val checkpointFrequency: Int = 500,
    val earlyStoppingPatience: Int = 50
)
```

### Network Configuration

```kotlin
data class NetworkConfig(
    val inputSize: Int = 776,
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    val outputSize: Int = 4096,
    val activationFunction: ActivationFunction = ReLU(),
    val optimizer: OptimizerType = OptimizerType.ADAM,
    val regularization: RegularizationType = RegularizationType.L2,
    val dropoutRate: Double = 0.1,
    val l2Regularization: Double = 0.001
)
```

## Error Handling

### Exception Hierarchy

```kotlin
sealed class ChessRLError : Exception() {
    data class InvalidMove(val move: Move, val reason: String) : ChessRLError()
    data class NetworkError(val operation: String, val cause: Throwable) : ChessRLError()
    data class TrainingError(val episode: Int, val cause: Throwable) : ChessRLError()
    data class ConfigurationError(val parameter: String, val value: Any) : ChessRLError()
    data class StateEncodingError(val board: ChessBoard, val cause: Throwable) : ChessRLError()
    data class ActionDecodingError(val actionIndex: Int, val cause: Throwable) : ChessRLError()
}
```

### Error Recovery

```kotlin
interface ErrorHandler {
    fun handleError(error: ChessRLError): RecoveryAction
    fun logError(error: ChessRLError, context: Map<String, Any>)
}

enum class RecoveryAction {
    RETRY,           // Retry the operation
    SKIP,            // Skip this step and continue
    ABORT,           // Abort the current operation
    FALLBACK,        // Use fallback strategy
    RESET_EPISODE,   // Reset the current episode
    ADJUST_PARAMETERS // Adjust training parameters
}
```

## Integration Examples

### Complete Training Setup

```kotlin
fun setupChessRLTraining(): ChessTrainingPipeline {
    // Create neural network
    val network = DenseNeuralNetwork(
        inputSize = 776,
        hiddenLayers = listOf(512, 256, 128),
        outputSize = 4096,
        activationFunction = ReLU()
    )
    network.setOptimizer(AdamOptimizer(0.001))
    network.setLossFunction(HuberLoss())
    
    // Create RL algorithm
    val targetNetwork = network.clone()
    val experienceReplay = CircularExperienceReplay<DoubleArray, Int>(50000)
    val dqn = DQNAlgorithm(network, targetNetwork, experienceReplay)
    
    // Create chess components
    val environment = ChessEnvironment()
    val agent = ChessAgent(network, dqn)
    val validator = ChessTrainingValidator()
    
    // Create training pipeline
    val config = TrainingConfig(
        episodes = 10000,
        batchSize = 64,
        learningRate = 0.001,
        explorationRate = 0.1
    )
    
    return ChessTrainingPipeline(agent, environment, validator, config)
}

// Run training
fun main() {
    val pipeline = setupChessRLTraining()
    val result = pipeline.trainAgent()
    
    println("Training completed:")
    println("Episodes: ${result.totalEpisodes}")
    println("Final win rate: ${result.finalWinRate}")
    println("Average game length: ${result.averageGameLength}")
}
```

This API documentation provides comprehensive coverage of all major components with practical usage examples and integration patterns.