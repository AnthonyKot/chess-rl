# Developer Guide

## Overview

This developer guide provides comprehensive information for developers working on the Chess RL Bot system, including architecture details, development setup, coding patterns, and extension guidelines.

## Table of Contents

1. [Development Environment Setup](#development-environment-setup)
2. [Architecture Deep Dive](#architecture-deep-dive)
3. [Core Components](#core-components)
4. [Development Patterns](#development-patterns)
5. [Testing Strategy](#testing-strategy)
6. [Performance Optimization](#performance-optimization)
7. [Contributing Guidelines](#contributing-guidelines)
8. [Debugging and Troubleshooting](#debugging-and-troubleshooting)

## Development Environment Setup

### Prerequisites

- **Kotlin Multiplatform**: Version 1.9.x or later
- **Gradle**: Version 8.x or later
- **JDK**: Version 17 or later for JVM targets
- **Native toolchain**: For native compilation (platform-specific)

### Project Structure

```
chess-rl-bot/
├── chess-engine/          # Core chess game logic and board representation
├── nn-package/           # Neural network implementation and training
├── rl-framework/         # Reinforcement learning algorithms and environment
├── integration/          # System integration and training pipelines
├── docs/                # Documentation
└── src/                 # Main application entry points
```

### Building the Project

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Build native binaries
./gradlew linkReleaseExecutableNative

# Run performance benchmarks
./benchmark-performance.sh
```

## Architecture Deep Dive

### Module Dependencies

The system follows a layered architecture:

1. **chess-engine**: Foundation layer providing chess game mechanics
2. **nn-package**: Neural network layer for decision making
3. **rl-framework**: Reinforcement learning layer for training algorithms
4. **integration**: Orchestration layer combining all components

### Key Design Principles

- **Multiplatform Compatibility**: All core logic runs on JVM and Native targets
- **Modular Design**: Clear separation of concerns between modules
- **Performance First**: Optimized for training and inference speed
- **Testability**: Comprehensive test coverage with integration tests

## Core Components

### Chess Engine (`chess-engine/`)

**Core Classes:**
- `ChessBoard`: Board state representation and move execution
- `MoveValidation`: Legal move validation and generation
- `GameStateDetection`: Checkmate, stalemate, and draw detection
- `PGNParser`: Chess game notation parsing and export

**Key Features:**
- Efficient bitboard representation
- Fast move generation and validation
- Complete chess rule implementation
- PGN import/export support

### Neural Network (`nn-package/`)

**Core Classes:**
- `NeuralNetwork`: Main network implementation with forward/backward pass
- `SyntheticDatasets`: Training data generation utilities
- `ValidationRunner`: Model validation and testing framework

**Architecture:**
- Fully connected layers with configurable depth
- Multiple activation functions (ReLU, Sigmoid, Tanh)
- Advanced optimizers (Adam, RMSprop, SGD with momentum)
- Batch processing support

### RL Framework (`rl-framework/`)

**Core Interfaces:**
- `Environment`: RL environment abstraction
- `ExperienceReplay`: Experience buffer management
- `ExplorationStrategy`: Exploration vs exploitation strategies
- `RLAlgorithms`: Core RL algorithm implementations

**Supported Algorithms:**
- Deep Q-Network (DQN)
- Policy Gradient methods
- Actor-Critic variants

### Integration Layer (`integration/`)

**Key Components:**
- `ChessAgent`: Main agent combining chess engine with RL
- `ChessEnvironment`: Chess-specific RL environment
- `SelfPlaySystem`: Self-play training infrastructure
- `TrainingPipeline`: End-to-end training orchestration

## Development Patterns

### Error Handling

Use sealed classes for structured error handling:

```kotlin
sealed class ChessResult<out T> {
    data class Success<T>(val value: T) : ChessResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : ChessResult<Nothing>()
}
```

### Configuration Management

Use data classes for configuration:

```kotlin
data class TrainingConfig(
    val learningRate: Double = 0.001,
    val batchSize: Int = 32,
    val episodes: Int = 1000,
    val explorationRate: Double = 0.1
)
```

### Logging and Metrics

Implement structured logging for debugging:

```kotlin
class MetricsCollector {
    fun recordTrainingMetrics(episode: Int, reward: Double, loss: Double) {
        println("Episode $episode: Reward=$reward, Loss=$loss")
    }
}
```

## Testing Strategy

### Test Categories

1. **Unit Tests**: Individual component testing
2. **Integration Tests**: Cross-module functionality
3. **Performance Tests**: Benchmarking and optimization
4. **End-to-End Tests**: Complete training pipeline validation

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :chess-engine:test
./gradlew :nn-package:test
./gradlew :rl-framework:test
./gradlew :integration:test

# Run performance benchmarks
./gradlew :integration:test --tests "*PerformanceBenchmarkTest"
```

### Test Data Management

Use synthetic data generation for consistent testing:

```kotlin
object TestDataGenerator {
    fun generateChessPosition(): ChessBoard { /* ... */ }
    fun generateTrainingBatch(size: Int): List<TrainingExample> { /* ... */ }
}
```

## Performance Optimization

### Profiling Tools

- Use built-in timing utilities in `PlatformTime`
- Monitor memory usage during training
- Track neural network inference speed

### Optimization Strategies

1. **Batch Processing**: Process multiple positions simultaneously
2. **Native Compilation**: Use native targets for production
3. **Memory Management**: Reuse objects where possible
4. **Parallel Processing**: Leverage multicore systems

### Benchmarking

Run performance comparisons:

```bash
# Compare JVM vs Native performance
./benchmark-jvm-only.sh
./integration/native-performance-test.sh

# Full performance comparison
./run-performance-comparison.sh
```

## Contributing Guidelines

### Code Style

- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Keep functions focused and small

### Pull Request Process

1. Create feature branch from `main`
2. Implement changes with tests
3. Run full test suite
4. Update documentation if needed
5. Submit PR with clear description

### Commit Messages

Use conventional commit format:
```
feat: add new chess position evaluation
fix: resolve memory leak in training loop
docs: update API documentation
test: add integration tests for self-play
```

## Debugging and Troubleshooting

### Common Issues

**Training Not Converging:**
- Check learning rate (try 0.001 - 0.01)
- Verify reward function implementation
- Monitor exploration vs exploitation balance

**Performance Issues:**
- Profile with native compilation
- Check batch sizes and memory usage
- Monitor garbage collection in JVM

**Chess Engine Bugs:**
- Validate move generation with known positions
- Test edge cases (castling, en passant, promotion)
- Compare with reference implementations

### Debugging Tools

The integration module provides debugging utilities:

```kotlin
// Enable detailed logging
val debugger = TrainingDebugger()
debugger.enableVerboseLogging()

// Monitor training progress
val monitor = PerformanceMonitor()
monitor.startMonitoring()
```

### Logging Configuration

Enable debug logging for specific components:

```kotlin
// In your main function or test setup
System.setProperty("chess.debug", "true")
System.setProperty("nn.debug", "true")
System.setProperty("rl.debug", "true")
```

---

For more specific information, see:
- [API Documentation](../api/README.md)
- [Architecture Overview](../architecture/README.md)
- [Training Guide](../training/README.md)
- [Troubleshooting Guide](../troubleshooting/README.md)