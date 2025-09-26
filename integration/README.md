# Integration Package

The integration layer that orchestrates chess RL training by combining the chess engine, neural networks, and RL algorithms into a cohesive training system. This package provides the main entry points for training, evaluation, and interactive play.

## Purpose

The integration package serves as the orchestration layer that:

- **Combines Components**: Integrates chess engine, neural networks, and RL framework
- **Training Pipeline**: Provides multi-process self-play training with automatic fallback
- **Environment Wrapper**: Adapts chess games for reinforcement learning
- **Evaluation Tools**: Compares agents against baselines and other models
- **Configuration Management**: Centralized configuration system
- **CLI Interface**: Command-line interface for all operations

## Key Classes

### TrainingPipeline
The main orchestrator for self-play training with built-in checkpointing and evaluation.

```kotlin
// Create and run training pipeline
val config = ChessRLConfig(
    gamesPerCycle = 20,
    maxCycles = 100,
    maxConcurrentGames = 4,
    hiddenLayers = listOf(512, 256, 128)
)

val pipeline = TrainingPipeline(config)
pipeline.initialize()

// Run training
runBlocking {
    val results = pipeline.runTraining()
    println("Training completed: ${results.totalGames} games played")
}
```

### ChessEnvironment
RL environment wrapper that converts chess games into the standard RL interface.

```kotlin
// Create chess environment
val environment = ChessEnvironment(config)

// Standard RL loop
var state = environment.reset()
while (!environment.isDone()) {
    val legalActions = environment.getLegalActions()
    val action = agent.selectAction(state, legalActions)
    
    val stepResult = environment.step(action)
    state = stepResult.nextState
}
```

### BaselineEvaluator
Evaluation tools for comparing agents against heuristic and minimax baselines.

```kotlin
// Evaluate against minimax
val evaluator = BaselineEvaluator(config)
val results = evaluator.evaluateAgainstMinimax(
    agent = trainedAgent,
    games = 100,
    depth = 2
)

println("Win rate: ${results.winRate * 100}%")
```

### ChessRLConfig
Centralized configuration system with essential parameters only.

```kotlin
// Create configuration
val config = ChessRLConfig(
    // Neural Network
    hiddenLayers = listOf(768, 512, 256),
    learningRate = 0.0005,
    optimizer = "adam",
    batchSize = 64,
    
    // RL Training
    explorationRate = 0.1,
    gamma = 0.99,
    targetUpdateFrequency = 100,
    
    // Self-Play
    gamesPerCycle = 50,
    maxConcurrentGames = 8,
    maxStepsPerGame = 120,
    
    // Rewards
    winReward = 1.0,
    lossReward = -1.0,
    drawReward = -0.2
)
```

## Usage Examples

### Complete Training Workflow

```kotlin
import com.chessrl.integration.*
import com.chessrl.integration.config.*

// Configure training
val config = ChessRLConfig(
    gamesPerCycle = 20,
    maxCycles = 50,
    maxConcurrentGames = 4,
    hiddenLayers = listOf(512, 256, 128),
    learningRate = 0.001,
    optimizer = "adam",
    checkpointDirectory = "checkpoints/experiment1"
)

// Initialize training pipeline
val pipeline = TrainingPipeline(config)
require(pipeline.initialize()) { "Failed to initialize training pipeline" }

// Run training with progress monitoring
runBlocking {
    val results = pipeline.runTraining()
    
    println("Training Results:")
    println("- Total games: ${results.totalGames}")
    println("- Total cycles: ${results.totalCycles}")
    println("- Final win rate: ${results.finalWinRate}")
    println("- Best model saved to: ${results.bestModelPath}")
}
```

### Agent Evaluation

```kotlin
// Load trained agent
val agent = ChessAgent.loadFromFile("checkpoints/best_model.json")

// Evaluate against different baselines
val evaluator = BaselineEvaluator(config)

// Against heuristic baseline
val heuristicResults = evaluator.evaluateAgainstHeuristic(agent, games = 100)
println("vs Heuristic: ${heuristicResults.winRate * 100}% win rate")

// Against minimax depth-2
val minimaxResults = evaluator.evaluateAgainstMinimax(agent, games = 100, depth = 2)
println("vs Minimax-2: ${minimaxResults.winRate * 100}% win rate")

// Against mixed opponents (heuristic + minimax d1/d2)
val mixedResults = evaluator.evaluateAgainstMixedOpponents(agent, games = 100)
println("vs Mixed: ${mixedResults.winRate * 100}% win rate")

// Head-to-head comparison
val agent2 = ChessAgent.loadFromFile("checkpoints/model2.json")
val h2hResults = evaluator.compareAgents(agent, agent2, games = 100)
println("Head-to-head: ${h2hResults.winRate * 100}% win rate")
```

### Interactive Play

```kotlin
// Set up interactive game
val agent = ChessAgent.loadFromFile("checkpoints/best_model.json")
val gameInterface = InteractiveGameInterface(agent, humanColor = PieceColor.WHITE, config)

// Play game with move input/output
gameInterface.playGame()
// Handles move parsing, validation, board display, and game flow
```

### Custom Environment Usage

```kotlin
// Create custom training loop
val environment = ChessEnvironment(config)
val agent = ChessAgent(config)

for (episode in 1..1000) {
    var state = environment.reset()
    var totalReward = 0.0
    
    while (!environment.isDone()) {
        // Get legal actions (important for chess)
        val legalActions = environment.getLegalActions()
        
        // Agent selects action
        val action = agent.selectAction(state, legalActions)
        
        // Execute move
        val stepResult = environment.step(action)
        totalReward += stepResult.reward
        
        // Train agent
        agent.learn(state, action, stepResult.reward, stepResult.nextState, stepResult.done)
        
        state = stepResult.nextState
    }
    
    println("Episode $episode: Reward = $totalReward")
}
```

## API Reference

### TrainingPipeline

#### Constructor
```kotlin
TrainingPipeline(config: ChessRLConfig)
```

#### Key Methods
- `initialize(): Boolean`
  - Initializes all components, returns true if successful
  
- `suspend fun runTraining(): TrainingResults`
  - Runs complete training process with progress logging
  
- `suspend fun runSingleCycle(): CycleResults`
  - Runs one training cycle (useful for custom training loops)
  
- `getSystemHealth(): SystemHealth`
  - Returns current system health and recommendations

### ChessEnvironment

#### Constructor
```kotlin
ChessEnvironment(config: ChessRLConfig)
```

#### Key Methods
- `reset(): DoubleArray`
  - Resets to starting position, returns encoded state
  
- `step(action: Int): StepResult`
  - Executes action, returns next state, reward, done flag
  
- `getLegalActions(): List<Int>`
  - Returns indices of all legal moves
  
- `isDone(): Boolean`
  - Returns true if game is finished
  
- `encodeState(): DoubleArray`
  - Returns 776-dimensional state encoding

### BaselineEvaluator

#### Constructor
```kotlin
BaselineEvaluator(config: ChessRLConfig)
```

#### Key Methods
- `evaluateAgainstHeuristic(agent: ChessAgent, games: Int): EvaluationResults`
  - Evaluates agent against simple heuristic baseline
  
- `evaluateAgainstMinimax(agent: ChessAgent, games: Int, depth: Int): EvaluationResults`
  - Evaluates agent against minimax algorithm
  
- `compareAgents(agentA: ChessAgent, agentB: ChessAgent, games: Int): ComparisonResults`
  - Head-to-head comparison between two agents

### ChessRLConfig

#### Essential Parameters
```kotlin
data class ChessRLConfig(
    // Neural Network Configuration
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    val learningRate: Double = 0.001,
    val batchSize: Int = 64,
    
    // RL Training Configuration  
    val explorationRate: Double = 0.1,
    val targetUpdateFrequency: Int = 100,
    val maxExperienceBuffer: Int = 50000,
    val gamma: Double = 0.99,
    
    // Self-Play Configuration
    val gamesPerCycle: Int = 20,
    val maxConcurrentGames: Int = 4,
    val maxStepsPerGame: Int = 80,
    val maxCycles: Int = 100,
    
    // Reward Structure
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = -0.2,
    val stepLimitPenalty: Double = -1.0,
    
    // System Configuration
    val seed: Long? = null,
    val checkpointDirectory: String = "checkpoints"
)
```

## Integration Points

### Chess Engine Integration
The integration layer adapts the chess engine for RL:

```kotlin
// Chess position encoding (776 dimensions)
// - 8x8x12 board representation (768 dims)
// - Castling rights (4 dims)  
// - En passant (1 dim)
// - Turn indicator (1 dim)
// - Move count (2 dims)

val encodedState = chessEnvironment.encodeState()
// Returns DoubleArray of size 776
```

### Neural Network Integration
Provides chess-specific network architectures:

```kotlin
// Recommended architectures for chess
val smallNetwork = listOf(256, 128)           // Fast training
val balancedNetwork = listOf(512, 256, 128)   // Good performance  
val largeNetwork = listOf(768, 512, 256)      // Maximum capacity

val config = ChessRLConfig(hiddenLayers = balancedNetwork)
```

### RL Framework Integration
Handles action masking and legal move selection:

```kotlin
// Legal action masking ensures only valid moves
val legalActions = environment.getLegalActions()
val qValues = agent.forward(state)

// Select best legal action
val bestAction = legalActions.maxByOrNull { qValues[it] } ?: legalActions.first()
```

## Architecture Features

### Multi-Process Training
Automatic multi-process self-play with fallback to sequential:

```kotlin
// Automatically uses multi-process if maxConcurrentGames > 1
val config = ChessRLConfig(maxConcurrentGames = 8)
val pipeline = TrainingPipeline(config)

// Falls back to sequential if multi-process fails
// Provides 3-4x speedup when working
```

### Structured Logging
Comprehensive logging with consistent format:

```kotlin
// Training progress logging
ChessRLLogger.logTrainingCycle(
    cycle = 10,
    totalCycles = 100,
    gamesPlayed = 20,
    avgReward = 0.15,
    winRate = 0.45,
    duration = 45000L
)
// Output: ℹ️ Training cycle 10/100 completed
//         Games: 20, Win rate: 45%, Avg length: 67.3
//         Avg reward: 0.1500, Duration: 45s
```

### Error Handling
Robust error handling with automatic recovery:

```kotlin
// Automatic error handling in training
SafeExecution.withErrorHandling("self-play game") {
    runSelfPlayGame(gameId, agents, environment)
} ?: run {
    logger.warn("Game $gameId failed, continuing with remaining games")
}
```

## Appendix: Legacy Insights and Best Practices

The following notes summarize validated practices from earlier iterations that remain useful for tuning and troubleshooting.

- Episode termination and tracking
  - Use explicit termination reasons: `GAME_ENDED`, `STEP_LIMIT`, `MANUAL`.
  - Treat step-limit endings as draws with a step-limit penalty (e.g., `-0.5 .. -1.0`).
  - Normalize stalled states (no encodable moves while board is non-terminal) to `MANUAL` plus a small penalty; this prevents “ONGOING” leakage and keeps the learning signal flowing.
  - Log a termination breakdown per cycle to spot issues (too many `STEP_LIMIT` ⇒ increase `maxStepsPerGame` or enable adjudication).

- Adjudication to reduce long/stalled games
  - Training knobs: `trainEarlyAdjudication`, `trainResignMaterialThreshold` (≈12), `trainNoProgressPlies` (≈80–100).
  - Evaluation knobs mirror training (`evalEarlyAdjudication`, etc.). By default the evaluator reuses the profile's exploration rate (0.05 in `eval-only`); override with `--eval-epsilon` if you need greedy play.

- Performance and platform guidance
  - Historically, JVM JIT outperformed native on long/complex runs, while native excelled at very short tasks. Current stack uses JVM; leverage JIT for long training.
  - Batch sizes: 32–128 recommended; 64 is a good default.
  - Experience buffer: size for ~1.5–2.0× (`batchSize × maxBatchesPerCycle`) experiences per cycle.

- Self‑play concurrency and throughput
  - Multi‑process shows gains when per‑game overhead is amortized: increase `gamesPerCycle` and match `maxConcurrentGames` to physical cores.
  - For benchmark runs, keep `evaluationGames=1`, export CSV metrics, and compare games/sec and cycle times.

- Teacher‑guided training
  - Fixed opponent: set `trainOpponentType=minimax|heuristic` and (for minimax) `trainOpponentDepth` (default 2).
  - Useful for bootstrapping early learning; later broaden opponents or alternate colors to avoid overfitting.

- Stability and sampling
  - Uniform replay is robust; prioritized replay can help but adds overhead.
  - Monitor gradient norm and entropy to detect exploding gradients or policy collapse.

- Practical defaults
  - `hiddenLayers=[512,256,128]` (single‑thread) or `[768,512,256]` (multi‑process / strong CPU)
  - `batchSize=64`, `explorationRate=0.05–0.1`, `gamma=0.99`, `targetUpdateFrequency=100–200`
  - `maxStepsPerGame=120`, `gamesPerCycle=60–200`, `maxBatchesPerCycle=50`
  - Enable training adjudication with `resign_threshold≈12` and `no_progress_plies≈80–100` during early runs

These insights come from episode-tracking/debugging improvements, performance investigations, and validation passes on the self‑play training pipeline.


### Checkpoint Management
Automatic model checkpointing with metadata:

```kotlin
// Automatic checkpointing during training
// Saves: best_model.json, checkpoint_cycle_N.json
// Includes: model weights, training metadata, performance metrics
```

## Performance Characteristics

### Training Performance
- **Development Profile**: 5-10 minutes for 10 cycles (5 games each)
- **Production Profile**: 2-4 hours for 200 cycles (50 games each)  
- **Multi-Process Speedup**: 3-4x with 4+ concurrent games
- **Memory Usage**: 500MB-2GB depending on configuration

### State Encoding Performance
- **Position Encoding**: ~0.1ms per position
- **Action Decoding**: ~0.05ms per action
- **Legal Move Generation**: ~1-5ms depending on position

### Evaluation Performance
- **Baseline Evaluation**: ~1-2 seconds per game
- **Model Comparison**: ~2-3 seconds per game
- **Batch Evaluation**: Linear scaling with game count

## Testing

The integration package includes comprehensive tests:

- ✅ **End-to-End Training**: Complete training cycles
- ✅ **Environment Interface**: Chess RL environment wrapper
- ✅ **Evaluation Tools**: Baseline and model comparison
- ✅ **Configuration System**: Parameter validation and profiles
- ✅ **Error Handling**: Recovery mechanisms and fallbacks
- ✅ **Multi-Process**: Concurrent training and sequential fallback

Run tests with:
```bash
./gradlew :integration:test
```

## CLI Interface

The package provides a simplified command-line interface:

```bash
# Training
./gradlew :integration:run --args="--train --profile fast-debug"
./gradlew :integration:run --args="--train --cycles 50 --games-per-cycle 20"
./gradlew :integration:run --args="--train --profile long-train-mixed --train-opponent random"

# Evaluation  
./gradlew :integration:run --args="--evaluate --baseline --games 100"
./gradlew :integration:run --args="--evaluate --baseline --opponent random --games 100"
./gradlew :integration:run --args="--evaluate --compare --modelA model1.json --modelB model2.json"

# Interactive play
./gradlew :integration:run --args="--play --model best_model.json --as white"
```

## Configuration Profiles

Four essential profiles are provided:

### fast-debug
Quick development iterations:
```yaml
gamesPerCycle: 5
maxCycles: 10
maxConcurrentGames: 2
maxStepsPerGame: 40
```

### long-train  
Production training:
```yaml
gamesPerCycle: 50
maxCycles: 200
maxConcurrentGames: 8
hiddenLayers: [768, 512, 256]
drawReward: -0.05
stepLimitPenalty: -0.6
```

### long-train-mixed
Curriculum that mixes heuristic and shallow minimax opponents during self-play:
```yaml
gamesPerCycle: 50
maxCycles: 200
maxConcurrentGames: 8
hiddenLayers: [768, 512, 256]
trainOpponentType: random
checkpointDirectory: "checkpoints/long-train-mixed"
```

### eval-only
Deterministic evaluation:
```yaml
evaluationGames: 500
seed: 12345
maxConcurrentGames: 1
```

## Dependencies

- Chess Engine Package
- Neural Network Package  
- RL Framework Package
- Kotlin Coroutines
- Kotlin Test Framework

## Platform Support

- ✅ **JVM**: Full support with optimized performance
- ✅ **Native**: Kotlin/Native compatibility
- ✅ **Cross-platform**: Consistent behavior across platforms

The integration package provides a complete, production-ready system for training chess RL agents with clean interfaces, robust error handling, and comprehensive evaluation tools.
