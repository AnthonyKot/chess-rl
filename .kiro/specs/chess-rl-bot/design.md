# Design Document - Chess RL Bot Refactoring

## Overview

This design document provides a systematic step-by-step refactoring plan for the chess RL bot codebase, transforming it from an experimental prototype with accumulated technical debt into a clean, reliable, effective, and configurable system focused on training competitive chess agents.

## Step-by-Step Refactoring Plan

### 1. Inventory & Audit

**Code Structure Mapping:**
```
Current Module Analysis:
├── chess-engine/          # Core chess logic (KEEP - essential)
│   ├── Board representation and game state
│   ├── Move generation and validation  
│   ├── Game termination detection
│   └── FEN/PGN parsing
├── nn-package/           # Neural network implementation (KEEP - essential)
│   ├── Feed-forward network
│   ├── Training algorithms
│   └── Model serialization
├── rl-framework/         # RL algorithms (KEEP - essential)
│   ├── DQN implementation
│   ├── Experience replay buffer
│   └── Exploration strategies
└── integration/          # Chess-RL integration (CONSOLIDATE - bloated)
    ├── 50+ classes with overlapping functionality
    ├── Multiple training controllers and validators
    ├── Experimental features and debug code
    └── Complex monitoring and reporting systems
```

**Flags & Configuration Audit:**
```yaml
# From profiles.yaml analysis - 25+ parameters per profile
USEFUL (keep):
  hiddenLayers: [512, 256, 128]        # Network architecture
  learningRate: 0.001                  # Training speed
  batchSize: 64                        # Training efficiency
  explorationRate: 0.15                # Exploration/exploitation
  maxStepsPerGame: 80                  # Game length control
  maxConcurrentGames: 8                # Parallelism
  winReward: 1.0, lossReward: -1.0     # Basic rewards
  drawReward: -0.2                     # Anti-draw incentive
  stepLimitPenalty: -1.0               # Efficiency incentive

EXPERIMENTAL (remove):
  enablePositionRewards: false         # Unproven benefit
  gameLengthNormalization: false       # Adds complexity
  enableLocalThreefoldDraw: true       # Redundant with engine
  repetitionPenalty: -0.05             # Experimental shaping
  opponentWarmupCycles: 8              # Complex opponent logic
  opponentUpdateStrategy: HISTORICAL   # Overcomplicated
  l2Regularization: 0.0001             # Network implementation detail
  gradientClipping: 1.0                # Should be internal
  autoCleanupOnFinish: true            # System implementation detail
  keepBest: true                       # Always do this
  treatStepLimitAsDraw: true           # Confusing logic

DEAD WEIGHT (remove):
  enableDoubleDQN: true                # Single algorithm choice
  replayType: PRIORITIZED              # Single replay strategy
  checkpointInterval: 5                # Fixed system behavior
  autoCleanupOnFinish: true            # Always cleanup
```

**Workflow Analysis:**
```bash
# Current workflows - many duplicated/experimental
KEEP (essential):
  --train-advanced                     # Main training workflow
  --eval-baseline                      # Agent vs heuristic/minimax
  --eval-h2h                          # Model comparison

CONSOLIDATE:
  --eval-non-nn                       # Merge into baseline eval
  --play-human                        # Simplify to basic play mode

REMOVE (experimental/unused):
  Multiple debugging CLIs
  Performance comparison scripts
  Experimental training variations
  Complex monitoring dashboards
```

### 2. Define Target Architecture

**Core Modules (Must Stay):**

```
chess-rl-bot/
├── chess-engine/                   # Game logic (chess rules, move generation)
│   ├── ChessBoard, ChessGame       # Core game state and rules
│   ├── MoveValidator, MoveGenerator # Legal move handling
│   ├── GameStatus detection        # Checkmate, stalemate, draws
│   └── FEN/PGN support            # Standard chess notation
├── nn-package/                     # NN model (FC now, extensible later)
│   ├── FeedforwardNetwork         # Core neural network
│   ├── Training algorithms        # Backprop, optimizers (Adam, SGD)
│   └── Model serialization        # Save/load trained models
├── rl-framework/                   # RL framework (DQN loop, replay buffer)
│   ├── DQNAlgorithm              # Deep Q-Network implementation
│   ├── ExperienceReplay          # Experience buffer management
│   └── ExplorationStrategy       # Epsilon-greedy exploration
└── integration/                    # Training runner (self-play, optimization)
    ├── ChessEnvironment          # RL environment wrapper
    ├── TrainingPipeline          # Self-play orchestration
    └── BaselineEvaluator         # Evaluation vs minimax/heuristic
```

**Support Modules (Optional, Can Trim):**

```
├── config/                         # Config/CLI manager
│   ├── ChessRLConfig             # Central configuration
│   ├── ConfigParser              # YAML/JSON parsing
│   └── ProfileManager            # Dev/prod/eval profiles
├── cli/                           # Command-line interface
│   └── ChessRLCLI                # Simplified CLI with essential commands
└── logging/                       # Logging/metrics
    ├── ChessRLLogger             # Structured logging
    └── MetricsCollector          # Essential training metrics
```

**Remove Entirely:**
- Any feature not directly supporting training, evaluation, or debugging
- Complex monitoring dashboards and visualization systems
- Experimental training variations and optimization coordinators
- Debug demos and manual validation interfaces
- Performance optimization experiments
- Multiple redundant training controllers and validators

### 3. Configuration System

**Replace Scattered Flags with Central Config System:**

```kotlin
// Single configuration class replacing 50+ scattered parameters
data class ChessRLConfig(
    // Neural Network (essential)
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    val learningRate: Double = 0.001,
    val batchSize: Int = 64,
    
    // RL Training (essential)
    val explorationRate: Double = 0.1,
    val explorationDecay: Double = 0.995,
    val targetUpdateFrequency: Int = 100,
    val maxExperienceBuffer: Int = 50000,
    
    // Self-Play (essential)
    val gamesPerCycle: Int = 20,
    val maxConcurrentGames: Int = 4,
    val maxStepsPerGame: Int = 80,
    val maxCycles: Int = 100,
    
    // Rewards (essential)
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = -0.2,        // Anti-draw incentive
    val stepLimitPenalty: Double = -1.0,  // Efficiency incentive
    
    // Evaluation (essential)
    val evaluationGames: Int = 100,
    val baselineDepth: Int = 2,
    
    // System (essential)
    val seed: Long? = null,
    val checkpointDirectory: String = "checkpoints"
) {
    fun validate(): List<String> = buildList {
        if (hiddenLayers.isEmpty()) add("Hidden layers cannot be empty")
        if (learningRate <= 0) add("Learning rate must be positive")
        if (gamesPerCycle <= 0) add("Games per cycle must be positive")
        // ... other validations
    }
}
```

**JSON/YAML Config + Kotlin Parser:**

```kotlin
object ConfigParser {
    fun fromYaml(path: String): ChessRLConfig {
        val yaml = Yaml()
        val data = yaml.load<Map<String, Any>>(File(path).readText())
        return ChessRLConfig(
            hiddenLayers = (data["hiddenLayers"] as List<Int>?) ?: listOf(512, 256, 128),
            learningRate = (data["learningRate"] as Double?) ?: 0.001,
            batchSize = (data["batchSize"] as Int?) ?: 64,
            // ... map all essential parameters with defaults
        )
    }
    
    fun fromArgs(args: Array<String>): ChessRLConfig {
        var config = ChessRLConfig() // Start with defaults
        args.toList().windowed(2, 2, partialWindows = false)
            .filter { it[0].startsWith("--") }
            .forEach { (key, value) ->
                config = when (key) {
                    "--learning-rate" -> config.copy(learningRate = value.toDouble())
                    "--games-per-cycle" -> config.copy(gamesPerCycle = value.toInt())
                    "--max-cycles" -> config.copy(maxCycles = value.toInt())
                    "--seed" -> config.copy(seed = value.toLong())
                    else -> config // Ignore unknown flags
                }
            }
        return config
    }
}
```

**Support for Profiles:**

```yaml
# config/profiles.yaml - Replace complex profiles.yaml
profiles:
  fast-debug:
    gamesPerCycle: 5
    maxCycles: 10
    maxConcurrentGames: 2
    maxStepsPerGame: 40
    evaluationGames: 20
    
  long-train:
    gamesPerCycle: 50
    maxCycles: 200
    maxConcurrentGames: 8
    hiddenLayers: [768, 512, 256]
    maxStepsPerGame: 120
    
  eval-only:
    evaluationGames: 500
    baselineDepth: 3
    maxConcurrentGames: 1
    seed: 12345
```

### 4. Testing & Validation

**Unit Tests (Fast, Reliable):**

```kotlin
// Chess engine: legal moves, terminal states, draw conditions
class ChessBoardTest {
    @Test fun `should generate all legal moves for starting position`() {
        val board = ChessBoard.startingPosition()
        val moves = board.getAllValidMoves(PieceColor.WHITE)
        assertEquals(20, moves.size) // 16 pawn + 4 knight moves
    }
    
    @Test fun `should detect checkmate in scholar's mate`() {
        val board = ChessBoard.fromFEN("rnbqkb1r/pppp1ppp/5n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 1 4")
        assertTrue(board.isCheckmate(PieceColor.BLACK))
    }
    
    @Test fun `should handle castling rights correctly`() {
        val board = ChessBoard.startingPosition()
        assertTrue(board.canCastle(PieceColor.WHITE, CastleType.KINGSIDE))
        // Move king, should lose castling rights
        board.makeMove(Move.parse("e1-e2"))
        assertFalse(board.canCastle(PieceColor.WHITE, CastleType.KINGSIDE))
    }
}

// Replay buffer correctness
class ExperienceReplayTest {
    @Test fun `should store and sample experiences correctly`() {
        val buffer = ExperienceReplay<DoubleArray, Int>(capacity = 100)
        val experience = Experience(
            state = doubleArrayOf(1.0, 0.0),
            action = 5,
            reward = 1.0,
            nextState = doubleArrayOf(0.0, 1.0),
            done = true
        )
        
        buffer.add(experience)
        assertEquals(1, buffer.size())
        
        val sampled = buffer.sample(1)
        assertEquals(1, sampled.size)
        assertEquals(experience.action, sampled[0].action)
    }
}

// Reward assignment
class ChessEnvironmentTest {
    @Test fun `should assign correct rewards for game outcomes`() {
        val env = ChessEnvironment(ChessRLConfig())
        
        // Simulate white win
        env.forceGameState(GameStatus.WHITE_WINS)
        val whiteWinReward = env.calculateReward(GameStatus.WHITE_WINS, isWhiteAgent = true)
        assertEquals(1.0, whiteWinReward, 0.001)
        
        // Simulate black win for white agent
        val blackWinReward = env.calculateReward(GameStatus.BLACK_WINS, isWhiteAgent = true)
        assertEquals(-1.0, blackWinReward, 0.001)
    }
}
```

**Integration Tests:**

```kotlin
// One training iteration runs end-to-end
class TrainingIntegrationTest {
    @Test fun `should complete one training cycle successfully`() {
        val config = ChessRLConfig(
            gamesPerCycle = 2,
            maxCycles = 1,
            maxConcurrentGames = 1,
            maxStepsPerGame = 10
        )
        
        val pipeline = TrainingPipeline(config)
        val result = runBlocking { pipeline.runSingleCycle() }
        
        assertTrue(result.isSuccess)
        assertEquals(2, result.gamesPlayed)
        assertTrue(result.experiencesCollected > 0)
    }
}

// One evaluation vs baseline completes successfully
class EvaluationIntegrationTest {
    @Test fun `should evaluate agent vs baseline successfully`() {
        val config = ChessRLConfig(evaluationGames = 5, baselineDepth = 1)
        val evaluator = BaselineEvaluator(config)
        
        val result = evaluator.evaluateVsBaseline(loadTestAgent())
        
        assertTrue(result.gamesCompleted == 5)
        assertTrue(result.winRate >= 0.0 && result.winRate <= 1.0)
        assertTrue(result.averageGameLength > 0)
    }
}
```

**Regression Tests:**

```kotlin
// Fixed seed self-play → deterministic reward curve snapshot
class RegressionTest {
    @Test fun `should produce deterministic results with fixed seed`() {
        val config = ChessRLConfig(
            seed = 12345L,
            gamesPerCycle = 5,
            maxCycles = 3,
            maxConcurrentGames = 1
        )
        
        val pipeline1 = TrainingPipeline(config)
        val pipeline2 = TrainingPipeline(config)
        
        val result1 = runBlocking { pipeline1.runTraining() }
        val result2 = runBlocking { pipeline2.runTraining() }
        
        // Should produce identical results
        assertEquals(result1.finalReward, result2.finalReward, 0.001)
        assertEquals(result1.totalGames, result2.totalGames)
    }
}
```

### 5. Reliability & Efficiency Improvements

**Parallelism: Coroutines/Structured Concurrency for Self-Play Workers**

```kotlin
class TrainingPipeline(private val config: ChessRLConfig) {
    
    suspend fun runTrainingCycle(): TrainingResult = coroutineScope {
        // Structured concurrency for self-play games
        val gameResults = (1..config.gamesPerCycle).map { gameIndex ->
            async(Dispatchers.Default) {
                runSelfPlayGame(gameIndex)
            }
        }.awaitAll()
        
        // Process experiences sequentially for thread safety
        val experiences = gameResults.flatMap { it.experiences }
        val trainingResult = trainAgent(experiences)
        
        TrainingResult(gameResults, trainingResult)
    }
    
    private suspend fun runSelfPlayGame(gameIndex: Int): GameResult = 
        withContext(Dispatchers.Default) {
            try {
                val environment = ChessEnvironment(config)
                val agent = getCurrentAgent()
                playGame(environment, agent, gameIndex)
            } catch (e: Exception) {
                logger.error("Game $gameIndex failed", e)
                GameResult.failed(gameIndex, e)
            }
        }
}
```

**Logging: Unified Log Format with Levels**

```kotlin
object ChessRLLogger {
    private val logger = LoggerFactory.getLogger("ChessRL")
    
    fun info(message: String, vararg args: Any) {
        logger.info(message, *args)
    }
    
    fun debug(message: String, vararg args: Any) {
        if (logger.isDebugEnabled) {
            logger.debug(message, *args)
        }
    }
    
    fun error(message: String, error: Throwable? = null) {
        if (error != null) {
            logger.error(message, error)
        } else {
            logger.error(message)
        }
    }
    
    // Structured logging for training events
    fun logTrainingCycle(cycle: Int, games: Int, avgReward: Double, duration: Long) {
        info("Cycle {} complete: {} games, avg_reward={:.3f}, duration={}ms", 
             cycle, games, avgReward, duration)
    }
    
    fun logEvaluation(winRate: Double, games: Int, avgLength: Double) {
        info("Evaluation: win_rate={:.1f}%, games={}, avg_length={:.1f}", 
             winRate * 100, games, avgLength)
    }
}
```

**Profiling Hooks: Simple Runtime Stats**

```kotlin
class MetricsCollector {
    private val episodeLengths = mutableListOf<Int>()
    private val replayBufferUtilization = mutableListOf<Double>()
    private val qValueHistograms = mutableListOf<DoubleArray>()
    
    fun recordEpisode(length: Int, finalReward: Double) {
        episodeLengths.add(length)
    }
    
    fun recordBufferUtilization(used: Int, capacity: Int) {
        replayBufferUtilization.add(used.toDouble() / capacity)
    }
    
    fun recordQValues(qValues: DoubleArray) {
        qValueHistograms.add(qValues.copyOf())
    }
    
    fun getStats(): TrainingStats {
        return TrainingStats(
            avgEpisodeLength = episodeLengths.average(),
            avgBufferUtilization = replayBufferUtilization.average(),
            qValueStats = calculateQValueStats()
        )
    }
}
```

### 6. Elimination Pass

**For Each Flag - Decision Matrix:**

```kotlin
// KEEP - Essential for training effectiveness
hiddenLayers: List<Int>                    // Network architecture impacts performance
learningRate: Double                       // Critical training parameter
batchSize: Int                            // Memory/performance tradeoff
explorationRate: Double                   // Exploration/exploitation balance
gamesPerCycle: Int                        // Training data generation
maxConcurrentGames: Int                   // Parallelism control
winReward/lossReward/drawReward: Double   // Basic reward structure
maxStepsPerGame: Int                      // Game length control
seed: Long?                               // Reproducibility for debugging

// REMOVE - Experimental/unproven
enablePositionRewards: Boolean            // No clear benefit demonstrated
gameLengthNormalization: Boolean          # Adds complexity, unclear benefit
enableLocalThreefoldDraw: Boolean         # Redundant with chess engine
repetitionPenalty: Double                 # Experimental reward shaping
opponentWarmupCycles: Int                 # Complex opponent management
opponentUpdateStrategy: String            # Overcomplicated
l2Regularization: Double                  # Should be network implementation detail
gradientClipping: Double                  # Should be internal to training
treatStepLimitAsDraw: Boolean            # Confusing game outcome logic
replayType: String                       # Single strategy sufficient
enableDoubleDQN: Boolean                 # Algorithm choice, not config
autoCleanupOnFinish: Boolean             # Always do cleanup
keepBest: Boolean                        # Always keep best model
```

**For Each Workflow - Keep/Remove Decision:**

```bash
# KEEP - Core functionality
--train                                   # Main training workflow
--evaluate                               # Agent vs baseline evaluation  
--play                                   # Human vs agent play

# REMOVE - Experimental/duplicate
--train-advanced                         # Merge into --train
--eval-baseline                          # Merge into --evaluate  
--eval-h2h                              # Merge into --evaluate
--eval-non-nn                           # Merge into --evaluate
Multiple debugging CLIs                  # Remove experimental debug interfaces
Performance comparison scripts           # Remove benchmarking experiments
Complex monitoring dashboards            # Remove over-engineered monitoring
```

### 7. Documentation & Onboarding

**README.md: High-Level Architecture, How to Run Training/Eval**

```markdown
# Chess RL Bot - Refactored

A clean, focused implementation of a chess reinforcement learning bot using DQN.

## Architecture

```
chess-rl-bot/
├── chess-engine/     # Chess rules and game logic
├── nn-package/       # Neural network implementation  
├── rl-framework/     # DQN algorithm and experience replay
├── integration/      # Training pipeline and evaluation
├── config/          # Configuration management
└── cli/             # Command-line interface
```

## Quick Start

```bash
# Train an agent (development profile)
./gradlew run --args="--train --profile fast-debug"

# Train with custom parameters  
./gradlew run --args="--train --cycles 50 --games-per-cycle 20 --seed 12345"

# Evaluate against baseline
./gradlew run --args="--evaluate --model checkpoints/best-model.json"

# Play against trained agent
./gradlew run --args="--play --model checkpoints/best-model.json"
```

## Configuration

The system uses a single configuration class with essential parameters only:

- **Neural Network**: `hiddenLayers`, `learningRate`, `batchSize`
- **RL Training**: `explorationRate`, `targetUpdateFrequency`, `maxExperienceBuffer`  
- **Self-Play**: `gamesPerCycle`, `maxConcurrentGames`, `maxStepsPerGame`
- **Rewards**: `winReward`, `lossReward`, `drawReward`, `stepLimitPenalty`

See `config/profiles.yaml` for predefined profiles.
```

**CONTRIBUTING.md: Coding Standards, Test Strategy**

```markdown
# Contributing Guidelines

## Code Standards

- Use Kotlin idioms and conventions
- Prefer immutable data classes
- Use structured concurrency (coroutines) for parallelism
- Handle errors explicitly with Result types or exceptions
- Write self-documenting code with clear naming

## Test Strategy

### Unit Tests (Fast)
- Chess engine: legal moves, game state detection
- Neural network: training on simple problems (XOR)
- RL algorithms: Q-value updates, experience replay

### Integration Tests  
- Training pipeline: end-to-end training cycles
- Evaluation: agent vs baseline comparison
- Configuration: parsing and validation

### Regression Tests
- Deterministic results with fixed seeds
- Chess rule compliance with complex positions

## Adding Features

1. Ensure the feature directly supports training competitive chess agents
2. Add configuration parameters only if they significantly impact performance
3. Write tests that validate core functionality
4. Update documentation with clear examples
```

**Inline Docstrings for Major Classes**

```kotlin
/**
 * Core training pipeline that orchestrates self-play games and agent training.
 * 
 * Uses structured concurrency to run multiple self-play games in parallel,
 * then trains the agent on collected experiences using DQN algorithm.
 * 
 * @param config Training configuration with essential parameters
 */
class TrainingPipeline(private val config: ChessRLConfig) {
    
    /**
     * Runs a complete training cycle: self-play → experience collection → training.
     * 
     * @return TrainingResult with game outcomes and training metrics
     */
    suspend fun runTrainingCycle(): TrainingResult
    
    /**
     * Runs the complete training process for the configured number of cycles.
     * Automatically saves checkpoints and tracks best performing models.
     */
    suspend fun runTraining(): TrainingResult
}

/**
 * Chess environment wrapper for reinforcement learning.
 * 
 * Converts chess game state to neural network input format and handles
 * action decoding from network outputs to legal chess moves.
 * 
 * @param config Configuration containing reward structure and game parameters
 */
class ChessEnvironment(private val config: ChessRLConfig) : Environment<DoubleArray, Int> {
    
    /**
     * Encodes current chess position as neural network input.
     * Uses 8x8x12 board representation plus additional features.
     * 
     * @return 776-dimensional state vector
     */
    fun encodeState(): DoubleArray
    
    /**
     * Decodes neural network output to legal chess move.
     * Applies action masking to ensure only valid moves are selected.
     * 
     * @param actionIndex Network output index
     * @return Legal chess move or random valid move if invalid
     */
    fun decodeAction(actionIndex: Int): Move
}
```

## Implementation Roadmap

### Phase 1: Audit and Planning (1-2 days)
1. **Complete inventory** of all flags, configs, and classes
2. **Categorize components** into keep/consolidate/remove
3. **Create migration plan** for essential functionality
4. **Set up new module structure** with clean boundaries

### Phase 2: Core Module Cleanup (3-4 days)  
1. **Consolidate integration package** - merge redundant classes
2. **Create central configuration system** - replace scattered flags
3. **Implement essential test suite** - remove flaky tests
4. **Clean up build scripts** - remove experimental scripts

### Phase 3: Training Pipeline Refactor (2-3 days)
1. **Implement coroutine-based training pipeline** 
2. **Add structured logging and metrics collection**
3. **Create simplified CLI with essential commands**
4. **Validate training pipeline works end-to-end**

### Phase 4: Validation and Documentation (1-2 days)
1. **Run regression tests** - ensure chess rules still work
2. **Validate training effectiveness** - agents can beat baseline
3. **Update documentation** - README, contributing guidelines
4. **Performance comparison** - before/after metrics

## Success Metrics

### Quantitative Goals
- **Code Reduction**: 40-50% fewer lines of code
- **Config Simplification**: <20 essential parameters (from 50+)
- **Test Reliability**: <2% flaky test failures (from ~15%)
- **Training Performance**: Maintain or improve training speed
- **Agent Competitiveness**: >40% win rate vs minimax depth-2

### Qualitative Goals  
- **Maintainability**: Clear module boundaries, consistent style
- **Reliability**: Deterministic results, robust error handling
- **Usability**: Simple CLI, clear documentation, easy onboarding
- **Focus**: Every feature contributes to training competitive agents

### Validation Checklist
- [ ] Chess engine passes all rule validation tests
- [ ] Neural network trains successfully on XOR problem  
- [ ] DQN algorithm works on simple test environments
- [ ] Training pipeline completes cycles without crashes
- [ ] Baseline evaluation produces consistent results
- [ ] Trained agents achieve >40% win rate vs minimax depth-2
- [ ] System produces identical results with fixed seeds
- [ ] All essential CLI commands work correctly
- [ ] Configuration parsing handles all profiles
- [ ] Documentation accurately reflects simplified system

This systematic refactoring approach will transform the chess RL codebase into a clean, focused system that reliably trains competitive chess agents while eliminating unnecessary complexity and technical debt.