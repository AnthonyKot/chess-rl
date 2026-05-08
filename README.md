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

#### AlphaZero (recommended — MCTS + dual-head network)
```bash
# Fast iteration — verify the pipeline, ~10 min on a laptop
./gradlew :integration:run --args="--train --profile alphazero-fast"

# Production AlphaZero run — 200 MCTS simulations per move
./gradlew :integration:run --args="--train --profile alphazero-train"

# Custom AlphaZero with explicit flags
./gradlew :integration:run --args="--train --nn alphazero --mcts-simulations 100 --mcts-c-puct 1.5 --cycles 50"
```

#### DQN (legacy — kept for comparison)
```bash
# Development training (fast iteration)
./gradlew :integration:run --args="--train --profile fast-debug"

# Production training (longer, more thorough)
./gradlew :integration:run --args="--train --profile long-train --cycles 200"

# Training with the legacy DL4J backend instead of RL4J
./gradlew :integration:run --args="--train --profile long-train --nn dl4j --cycles 200"

# Custom training with specific parameters
./gradlew :integration:run --args="--train --cycles 50 --games-per-cycle 60 --seed 12345"
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
| **Neural network** | `hiddenLayers = [512,256,128]`, `learningRate = 5e-4`, `optimizer = adam`, `batchSize = 64`, `nnBackend = rl4j` |
| **RL training (DQN)** | `explorationRate = 0.05` (floor), `initialExplorationRate = 0.35`, `explorationDecaySteps = auto`, `targetUpdateFrequency = 200`, `doubleDqn = true`, `gamma = 0.99`, `maxExperienceBuffer = 50_000`, `replayType = UNIFORM` |
| **AlphaZero / MCTS** | `mctsSimulations = 100`, `cPuct = 1.5`, `mctsTemperature = 1.0`, `mctsRecentGamesBuffer = 500`, `mctsDirichletAlpha = 0.3`, `mctsDirichletEpsilon = 0.25` |
| **Self-play** | `gamesPerCycle = 30`, `maxConcurrentGames = 4`, `maxStepsPerGame = 120`, `maxCycles = 100` |
| **Rewards** | `winReward = 1.0`, `lossReward = -1.0`, `drawReward = 0.0`, `stepLimitPenalty = -0.5` |
| **System** | `engine = chesslib`, `seed = null`, `checkpointInterval = 5`, `checkpointDirectory = checkpoints`, `evaluationGames = 20`, `workerHeap = null` |

Optional controls (minimax opponents, adjudication thresholds, logging cadence, checkpoint retention) are available for advanced runs.

### Configuration Profiles

Profiles live in `integration/profiles.yaml`:

- **alphazero-fast** – MCTS quick start (10 games × 20 cycles, 50 simulations/move, network 256-128). Good for verifying the pipeline and watching early learning.
- **alphazero-train** – MCTS production run (20 games × 200 cycles, 200 simulations/move, network 512-256, buffer 1000 games). Expected to exceed 800 ELO vs the DQN baseline.
- **fast-debug** – DQN quick iteration (10 games × 10 cycles, smaller network, epsilon floor 0.08).
- **long-train** – DQN production self-play (50 games × 200 cycles, 768-512-256 network, minimax-softmax sparring).
- **long-train-mixed** – DQN with mixed heuristic/minimax opponents and prioritized replay.
- **eval-only** – deterministic evaluation (seeded, exploration 0, 500 evaluation games).

For RL4J these profile values are mapped directly into the RL4J builders. A typical
`long-train` configuration shows up at runtime as:

```
ChessRLConfig(
  hiddenLayers=[768, 512, 256], learningRate=3e-4, optimizer=adam, batchSize=64,
  nnBackend=RL4J, explorationRate=0.03, targetUpdateFrequency=300, gamma=0.99,
  maxExperienceBuffer=100000, gamesPerCycle=50, maxConcurrentGames=8,
  maxStepsPerGame=150, drawReward=-0.05, stepLimitPenalty=-0.6,
  checkpointInterval=5, checkpointDirectory=checkpoints/long-train, ...)
```

`RL4JConfigurationMapper` in turn maps those fields to `QLConfiguration` (batch
size, replay size, epsilon start/floor/decay, target-update frequency, `maxEpochStep = maxStepsPerGame`)
and to the dense network factory (hidden layer sizes, learning rate). Checkpoints
land in the profile’s `checkpointDirectory` as `model_cycle_XXX.json` every
`checkpointInterval` cycles.

### Command Line Options

**Core commands**

- `--train` – run the training pipeline
- `--evaluate` – evaluation mode (`--baseline` or `--compare`)
- `--play` – interactive match against the agent

**Common flags**

- `--profile <name>` – select a profile (`alphazero-fast`, `alphazero-train`, `fast-debug`, `long-train`, `long-train-mixed`, `eval-only`)
- `--nn <backend>` – choose neural-network backend (`alphazero`, `rl4j`, `dl4j`, `manual`)

**AlphaZero-specific flags** (only used when `--nn alphazero`)

- `--mcts-simulations N` – MCTS simulations per move (default 100; 50 = fast, 400 = very strong)
- `--mcts-c-puct F` – UCB exploration constant (default 1.5; lower = trust search more, higher = trust policy prior more)
- `--mcts-temperature F` – action sampling temperature (1.0 = diverse training moves, 0.0 = greedy eval)
- `--mcts-recent-games N` – rolling training buffer size in samples (default 500)
- `--engine <backend>` – choose chess engine (`chesslib`, `builtin`)
- `--cycles`, `--games-per-cycle`, `--max-concurrent-games`, `--max-steps` – override self-play settings
- `--learning-rate`, `--batch-size`, `--exploration-rate`, `--initial-exploration-rate`, `--exploration-decay-steps`, `--target-update-frequency` – tweak training hyperparameters
- `--checkpoint-dir`, `--worker-heap`, `--seed`, `--model`, `--modelA`, `--modelB`
- `--train-opponent`, `--train-opponent-depth`, `--train-opponent-temperature` – configure sparring opponents (`self|minimax|minimax-softmax|heuristic|random`)
- `--train-opponent`, `--train-opponent-depth`, `--train-opponent-temperature` – configure sparring opponents (`self|minimax|minimax-softmax|heuristic|random`)

## AlphaZero Backend

### Why AlphaZero instead of DQN

The DQN implementation plateaus around 100–300 ELO for three structural reasons:

1. **No lookahead.** DQN picks moves reactively from a single forward pass. Even a 400-ELO human looks 2–3 moves ahead.
2. **Ill-posed action space.** The network predicts Q-values over all 4096 from-to combinations, but only ~28 are legal. Capacity is wasted on illegal moves.
3. **Sparse credit assignment.** A win or loss signal at move 60 must propagate back through 60 Bellman updates to reach move 1.

AlphaZero solves all three with Monte Carlo Tree Search (MCTS):

- **Search replaces lookahead** — 100–200 simulations per move explores the tree, so tactical blunders get caught before a move is played.
- **Policy head outputs only over legal moves** — no 4096-action problem.
- **Value head at every position** — the network directly estimates who's winning, so every board state produces a training signal, not just terminal positions.

Expected ELO gain over DQN with the same training budget: **+500–800 ELO**.

### Architecture

```
Input (839-dim board)
        │
  Shared trunk [839 → 512 → 256, ReLU]
        │
   ┌────┴────┐
Policy head  Value head
[256→4096]  [256→1, tanh]
(prior P)   (position V ∈ [-1,1])
```

### How MCTS uses the network

Each simulation walks the tree selecting moves by UCB-PUCT:

```
score(action) = Q(s,a) + c_puct × P(s,a) × √N_parent / (1 + N(s,a))
```

- **Q** is the average outcome seen so far via this action (exploitation)
- **P** is the policy head's prior probability (initial guidance)
- The fraction shrinks as a move is visited more, redirecting exploration elsewhere

After all simulations, the visit distribution `π` becomes the policy training target. The value target `z` is the actual game outcome (+1 win, −1 loss, 0 draw) from each position's player's perspective.

**Loss** = cross_entropy(π, policy_head) + MSE(z, value_head)

### Dirichlet noise

At the root node during self-play, Dirichlet noise is mixed into the policy priors:

```
prior = (1 − ε) × P(s,a) + ε × Dirichlet(α)
```

This ensures the agent tries moves the policy initially rates low, preventing premature convergence. `mctsDirichletAlpha=0.3` and `mctsDirichletEpsilon=0.25` (AlphaZero defaults for chess).

### Temperature schedule

- **First 30 moves:** temperature = 1.0 → actions sampled proportional to visit counts (diverse training data)
- **After move 30:** temperature = 0.0 → always play the most-visited move (decisive play)
- **Evaluation:** always temperature = 0.0

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

# RL4J-specific tests live under :integration:test and run automatically when the backend is enabled
```

### Essential Test Coverage
- **Chess Engine**: Legal move generation, game state detection, special moves
- **Neural Network**: XOR learning, gradient updates, model serialization
- **RL Framework**: Q-value updates, experience replay, action selection
- **Integration**: End-to-end training cycles, evaluation workflows

### RL4J Backend (default)

RL4J dependencies are bundled by default (see `gradle.properties`). To slim down
the dependency footprint you can set `enableRL4J=false` before building. With
RL4J enabled:

1. Install a JDK (11 or newer) and ensure `JAVA_HOME` is configured.
2. Run training as usual – RL4J will be selected automatically unless you pass
   `--nn manual` or `--nn dl4j`:
   ```bash
   ./gradlew :integration:run --args="--train --profile long-train"
   ```
   Checkpoints land in the profile's `checkpointDirectory`
   (e.g. `checkpoints/long-train/model_cycle_050.json`).
3. The RL4J test suite (smoke training, checkpoint round-trip, policy exposure)
   runs whenever the RL4J classes are on the classpath; the tests skip
   themselves automatically if you disable the backend.

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
