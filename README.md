# Chess RL Bot

A clean, focused implementation of a chess reinforcement learning bot using Deep Q-Network (DQN). This system has been refactored from an experimental prototype into a reliable, maintainable, and effective training platform focused on training competitive chess agents.

## Architecture

The system follows a clean modular architecture with well-defined responsibilities:

```
chess-rl-bot/
├── chess-engine/     # Chess rules and game logic
├── nn-package/       # Neural network implementation  
├── rl-framework/     # DQN algorithm and experience replay
├── integration/      # Training pipeline and evaluation
└── config/          # Configuration profiles and management
```

### Module Responsibilities

- **chess-engine**: Complete chess implementation with legal move generation, game state detection, and notation support
- **nn-package**: Feed-forward neural networks with training algorithms and model serialization
- **rl-framework**: DQN implementation with experience replay and exploration strategies
- **integration**: Training orchestration, evaluation tools, and CLI interface
- **config**: Centralized configuration system with essential parameters only

## Quick Start

### Training an Agent
```bash
# Development training (fast iteration)
./gradlew :integration:run --args="--train --profile fast-debug"

# Production training (longer, more thorough)
./gradlew :integration:run --args="--train --profile long-train --cycles 100"

# Custom training with specific parameters
./gradlew :integration:run --args="--train --cycles 50 --games-per-cycle 30 --seed 12345"
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

The system uses a centralized configuration approach with essential parameters only. Configuration is managed through:

### Essential Parameters

**Neural Network Configuration:**
- `hiddenLayers`: Network architecture (default: [512, 256, 128])
- `learningRate`: Training speed (default: 0.001)
- `batchSize`: Training batch size (default: 64)

**RL Training Configuration:**
- `explorationRate`: Exploration vs exploitation balance (default: 0.1)
- `targetUpdateFrequency`: How often to update target network (default: 100)
- `maxExperienceBuffer`: Experience replay buffer size (default: 50000)
- `gamma`: Discount factor for future rewards (default: 0.99)

**Self-Play Configuration:**
- `gamesPerCycle`: Games per training cycle (default: 20)
- `maxConcurrentGames`: Parallel game limit (default: 4)
- `maxStepsPerGame`: Maximum moves per game (default: 80)
- `maxCycles`: Total training cycles (default: 100)

**Reward Structure:**
- `winReward`: Reward for winning (default: 1.0)
- `lossReward`: Penalty for losing (default: -1.0)
- `drawReward`: Penalty for draws to encourage decisive play (default: -0.2)
- `stepLimitPenalty`: Penalty for hitting step limit (default: -1.0)

### Configuration Profiles

Three essential profiles are provided:

**fast-debug**: Quick development iterations
```yaml
gamesPerCycle: 5
maxCycles: 10
maxConcurrentGames: 2
maxStepsPerGame: 40
evaluationGames: 20
```

**long-train**: Production training
```yaml
gamesPerCycle: 50
maxCycles: 200
maxConcurrentGames: 8
hiddenLayers: [768, 512, 256]
maxStepsPerGame: 120
```

**eval-only**: Deterministic evaluation
```yaml
evaluationGames: 500
baselineDepth: 3
maxConcurrentGames: 1
seed: 12345
```

### Command Line Options

**Core Commands:**
- `--train`: Start training using TrainingPipeline
- `--evaluate`: Evaluate agent performance
  - `--baseline`: Against heuristic or minimax baseline
  - `--compare`: Head-to-head model comparison
- `--play`: Interactive play against trained agent

**Essential Flags:**
- `--profile <name>`: Use predefined profile (fast-debug, long-train, eval-only)
- `--cycles <n>`: Number of training cycles
- `--games-per-cycle <n>`: Games per training cycle
- `--seed <n>`: Random seed for reproducibility
- `--model <path>`: Path to trained model file
- `--max-concurrent-games <n>`: Parallel game limit

## Performance Characteristics

### Training Performance
- **Development Profile**: ~5-10 minutes for 10 cycles with 5 games each
- **Production Profile**: ~2-4 hours for 200 cycles with 50 games each
- **Concurrent Games**: 3-4x speedup with multi-process self-play (automatic fallback to sequential)
- **Memory Usage**: ~500MB-2GB depending on experience buffer size and network architecture

### Agent Competitiveness
- **Target Performance**: >40% win rate against minimax depth-2
- **Training Progression**: Agents typically show improvement after 20-50 cycles
- **Baseline Comparison**: Trained agents consistently outperform random play and basic heuristics

### System Requirements
- **Minimum**: 4GB RAM, 2 CPU cores, Java 11+
- **Recommended**: 8GB RAM, 4+ CPU cores for concurrent training
- **Storage**: ~100MB for checkpoints per training run

## Reproducibility

For deterministic results, use the `eval-only` profile or set a specific seed:

```bash
# Deterministic training
./gradlew :integration:run --args="--train --seed 12345 --max-concurrent-games 1"

# Deterministic evaluation
./gradlew :integration:run --args="--evaluate --profile eval-only --model best_model.json"
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
```

### Essential Test Coverage
- **Chess Engine**: Legal move generation, game state detection, special moves
- **Neural Network**: XOR learning, gradient updates, model serialization
- **RL Framework**: Q-value updates, experience replay, action selection
- **Integration**: End-to-end training cycles, evaluation workflows

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

