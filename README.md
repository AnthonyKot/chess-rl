# Chess RL Bot

A clean, focused implementation of a chess reinforcement learning bot built around a Deep Q-Network (DQN) pipeline. The project has evolved from an experimental prototype into a maintainable training platform with multi-process self-play, structured logging, and centralized configuration.

## Architecture

The system follows a clean modular architecture with well-defined responsibilities:

```
chess-rl-bot/
├── chess-engine/     # Chess rules and game logic
├── nn-package/       # Neural network implementation  
├── rl-framework/     # DQN algorithms and replay buffers
├── integration/      # Training pipeline, CLI, evaluation
└── config/           # YAML profiles and overrides
```

### Module Responsibilities

- **chess-engine** – Move generation, rule enforcement, FEN/PGN utilities.
- **nn-package** – Feed-forward network implementation (manual backend).
- **rl-framework** – DQN core (target network, replay, exploration policies).
- **integration** – Self-play orchestration, CLI, baseline evaluation, checkpointing.
- **config** – Profile definitions and centralized parameter parsing.

## Quick Start

### Training an Agent
```bash
# Development training (fast iteration)
./gradlew :integration:run --args="--train --profile fast-debug"

# Production training (longer, more thorough)
./gradlew :integration:run --args="--train --profile long-train --nn dl4j --cycles 200"

# Custom training with specific parameters
./gradlew :integration:run --args="--train --nn dl4j --cycles 50 --games-per-cycle 60 --seed 12345"
```

### Evaluating Performance
```bash
# Evaluate against heuristic baseline
./gradlew :integration:run --args="--evaluate --baseline --games 200"

# Evaluate specific model
./gradlew :integration:run --args="--evaluate --model checkpoints/best_model.json --games 100"

# Compare two models head-to-head
./gradlew :integration:run --args="--evaluate --compare --modelA model1.json --modelB model2.json"
```

### Playing Against Agent
```bash
# Play as white against trained agent
./gradlew :integration:run --args="--play --model checkpoints/best_model.json --as white"

# Play as black (agent plays white)
./gradlew :integration:run --args="--play --model checkpoints/best_model.json --as black"
```

### Getting Help
```bash
./gradlew :integration:run --args="--help"
```

## Configuration System

The system uses a centralized configuration (`ChessRLConfig`) that exposes only impactful knobs. You can override values via profiles or CLI flags.

### Key Parameters & Defaults

| Group | Parameters (defaults) |
|-------|-----------------------|
| **Neural network** | `hiddenLayers = [512,256,128]`, `learningRate = 5e-4`, `optimizer = adam`, `batchSize = 64`, `nnBackend = dl4j` |
| **RL training** | `explorationRate = 0.05`, `targetUpdateFrequency = 200`, `doubleDqn = true`, `gamma = 0.99`, `maxExperienceBuffer = 50_000`, `replayType = UNIFORM` |
| **Self-play** | `gamesPerCycle = 30`, `maxConcurrentGames = 4`, `maxStepsPerGame = 120`, `maxCycles = 100` |
| **Rewards** | `winReward = 1.0`, `lossReward = -1.0`, `drawReward = 0.0`, `stepLimitPenalty = -0.5` |
| **System** | `engine = chesslib`, `seed = null`, `checkpointInterval = 5`, `checkpointDirectory = checkpoints`, `evaluationGames = 20`, `workerHeap = null` |

Optional controls (minimax opponents, adjudication thresholds, logging cadence, checkpoint retention) are available for advanced runs.

### Configuration Profiles

Profiles live in `integration/profiles.yaml`:

- **fast-debug** – quick iteration (10 games × 10 cycles, smaller network, epsilon 0.08).
- **long-train** – production self-play (50 games × 200 cycles, hidden layers 768-512-256, epsilon 0.03, prioritized checkpoints).
- **long-train-mixed** – same as long-train but mixes heuristic/minimax opponents and uses prioritized replay.
- **eval-only** – deterministic evaluation (seeded, exploration 0, 500 evaluation games).

### Command Line Options

**Core commands**

- `--train` – run the training pipeline
- `--evaluate` – evaluation mode (`--baseline` or `--compare`)
- `--play` – interactive match against the agent

**Common flags**

- `--profile <name>` – select a profile (`fast-debug`, `long-train`, `long-train-mixed`, `eval-only`)
- `--nn <backend>` – choose neural-network backend (`dl4j`, `manual`)
- `--engine <backend>` – choose chess engine (`chesslib`, `builtin`)
- `--cycles`, `--games-per-cycle`, `--max-concurrent-games`, `--max-steps` – override self-play settings
- `--learning-rate`, `--batch-size`, `--exploration-rate`, `--target-update-frequency` – tweak training hyperparameters
- `--checkpoint-dir`, `--worker-heap`, `--seed`, `--model`, `--modelA`, `--modelB`

## Performance Characteristics

### Training Performance
- **fast-debug**: ~10 minutes for 10 cycles × 10 games.
- **long-train**: 2–4 hours for 200 cycles × 50 games (with DL4J backend on 8-core machine).
- **Parallelism**: Multi-process self-play yields ~3–4× speed-up; automatically falls back to sequential if spawning fails.
- **Memory**: 0.5–2 GB depending on replay size and network width.

### Agent Competitiveness
- **Baseline**: Trained agents consistently beat random and basic heuristic opponents.
- **Goal**: 40%+ win rate against minimax depth‑2 after full long-train run.
- **Progress**: Expect noticeable improvement after 30–60 cycles; curriculum and epsilon schedule can accelerate this.

### System Requirements
- **Minimum**: 4 GB RAM, 2 CPU cores, Java 11+
- **Recommended**: 8+ GB RAM, 4+ cores for parallel self-play (`--worker-heap` to bound per-process memory)
- **Storage**: ~100 MB per set of checkpoints

## Reproducibility

Use the `eval-only` profile or set a master seed with single-threaded play:

```bash
# Deterministic training run
./gradlew :integration:run --args="--train --profile fast-debug --seed 12345 --max-concurrent-games 1"

# Deterministic evaluation
./gradlew :integration:run --args="--evaluate --profile eval-only --model checkpoints/best_model.json"
```

## Development and Testing

### Building the Project
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :integration:build
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :chess-engine:test
./gradlew :nn-package:test
./gradlew :rl-framework:test
./gradlew :integration:test

# Run RL4J-gated tests (requires RL4J enabled)
./gradlew :integration:test -PenableRL4J=true
```

### Essential Test Coverage
- **Chess Engine**: Legal move generation, game state detection, special moves
- **Neural Network**: XOR learning, gradient updates, model serialization
- **RL Framework**: Q-value updates, experience replay, action selection
- **Integration**: End-to-end training cycles, evaluation workflows

### RL4J Backend (Optional)

RL4J support requires additional dependencies. To enable it locally:

1. Install a JDK (11 or newer) and ensure `JAVA_HOME` is configured.
2. Run Gradle tasks with the `enableRL4J` flag, for example:
   ```bash
   ./gradlew :integration:compileKotlin -PenableRL4J=true
   ./gradlew :integration:test -PenableRL4J=true
   ```
3. The RL4J test suite (smoke training, checkpoint round-trip, policy exposure) runs only when the flag is set; otherwise those tests skip automatically.

You can also persist the flag by adding `enableRL4J=true` to `gradle.properties` when working with the RL4J backend regularly.

## Package Details

### Chess Engine (`chess-engine`)
Complete chess implementation with full rule support:
- **Complete Rules**: All chess rules including castling, en passant, promotion
- **Game State Detection**: Checkmate, stalemate, draw detection  
- **Notation Support**: FEN parsing/generation, PGN import/export
- **Move Validation**: Legal move generation and validation

### Neural Network Package (`nn-package`)
Feed-forward neural network implementation:
- **Network Architecture**: Configurable hidden layers with ReLU activation
- **Training Algorithms**: Backpropagation with Adam optimizer
- **Model Serialization**: Save/load trained models in JSON format
- **Batch Processing**: Efficient batch training support

### RL Framework (`rl-framework`)
Deep Q-Network reinforcement learning implementation:
- **DQN Algorithm**: Deep Q-Network with target networks
- **Experience Replay**: Circular buffer for experience storage
- **Exploration Strategies**: Epsilon-greedy exploration with decay
- **Action Masking**: Ensures only legal moves are selected

### Integration Layer (`integration`)
Training orchestration and evaluation tools:
- **TrainingPipeline**: Multi-process self-play training with automatic fallback
- **ChessEnvironment**: RL environment wrapper for chess games
- **BaselineEvaluator**: Evaluation against heuristic and minimax opponents
- **Configuration System**: Centralized configuration management

## Dependencies

### External Libraries

**chesslib (bhlangonijr/chesslib) v1.3.3**
- **Purpose**: Primary chess engine backend (default runtime engine)
- **License**: Apache-2.0 (compatible with this project)
- **Repository**: https://github.com/bhlangonijr/chesslib
- **Usage**: Runs by default; override with `--engine builtin` to fall back to the handcrafted engine

The chesslib dependency provides an alternative chess rule implementation that can be used alongside the built-in engine for cross-validation and testing purposes. Both engines implement the same ChessEngineAdapter interface, ensuring identical behavior from the RL training perspective.

## Contributing

This project follows clean architecture principles:
- **Modular Design**: Each package has clear responsibilities and interfaces
- **Comprehensive Testing**: Unit and integration tests for all components
- **Error Handling**: Robust error handling with graceful degradation
- **Documentation**: Clear inline documentation and usage examples

### Code Standards
- Use Kotlin idioms and conventions
- Prefer immutable data classes
- Handle errors explicitly with proper exception handling
- Write self-documenting code with clear naming

### Adding Features
1. Ensure the feature directly supports training competitive chess agents
2. Add configuration parameters only if they significantly impact performance  
3. Write tests that validate core functionality
4. Update documentation with clear examples

---

**Ready to train your chess agent?** Start with the quick start commands above! 🚀
