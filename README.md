# Chess RL Bot

A reinforcement learning chess bot built with Kotlin Multiplatform, featuring modular architecture with separate packages for neural networks, chess engine, RL framework, and integration.

## Performance Note

**JVM Target Recommended**: Based on comprehensive benchmarks, the JVM target significantly outperforms native compilation for neural network operations (2-16x faster). We recommend using JVM for training and development, with native compilation reserved for deployment scenarios requiring smaller memory footprint or no JVM dependency. Further verification with RL and chess-specific workloads is planned.

## Project Structure

```
chess-rl-bot/
├── nn-package/          # Neural Network Package
├── chess-engine/        # Chess Implementation
├── rl-framework/        # RL Microframework  
├── integration/         # Integration Layer
├── src/                 # Main Application
└── .github/workflows/   # CI/CD Configuration
```

## Requirements

- JDK 17 or higher
- Gradle 8.4+ (included via wrapper)
- Native compilation support for your platform

## Building the Project

### Quick build verification
```bash
./verify-build.sh
```

What it does:
- Compiles Kotlin metadata for all modules
- Runs JVM unit tests where available (including `:nn-package:jvmTest`)
- Optionally runs native tests and builds native binaries on macOS if Xcode is available
- Lists built native binaries, when produced

### Build all modules
```bash
./gradlew build
```

### Run tests
```bash
# Neural network package JVM tests (fast, no native toolchain required)
./gradlew :nn-package:jvmTest

# Chess engine JVM tests (if available)
./gradlew :chess-engine:jvmTest

# All tests except native
./gradlew test --exclude-task nativeTest

# Full test suite including native (requires Xcode on macOS)
./gradlew test
```

### Build native executable (requires Xcode on macOS)
```bash
./gradlew nativeBinaries
```

### Benchmark Performance

Compare JVM vs Native performance for the NN package:

```bash
./benchmark-performance.sh
```

Notes:
- JVM benchmark runs on all platforms.
- Native benchmark runs when a native toolchain is available (Xcode on macOS, appropriate toolchains on Linux/Windows).
- Output filters to show only benchmark summaries.

## Platform Requirements

### macOS
- **Xcode Command Line Tools required** for native compilation
- Install with: `xcode-select --install`
- Verify installation: `xcode-select -p`
- For full Xcode: Install from Mac App Store

### Linux
- Native compilation supported with system development tools
- Usually works out of the box on most distributions

### Windows
- Native compilation supported with MinGW
- May require additional setup for some configurations

### CI/CD Considerations
- GitHub Actions workflow: `.github/workflows/build.yml`
- macOS runners: Native steps only run when Xcode is available
- Other platforms: Metadata compilation and JVM tests
- Artifacts: Native binaries from successful macOS builds (when applicable)

## Module Dependencies

- **nn-package**: Standalone neural network library
- **chess-engine**: Standalone chess game implementation  
- **rl-framework**: Depends on nn-package
- **integration**: Depends on all three packages
- **main application**: Depends on integration layer

## Development

Each module has its own test suite and can be developed independently. The modular design allows for:

- Independent testing of each component
- Reusable neural network and RL framework for other projects
- Chess engine that can be extended for human play
- Clear separation of concerns

### Chess Engine Demo

The chess engine includes a comprehensive demo showcasing all features:

```bash
# Run the chess engine demo
./gradlew :chess-engine:runDemo

# Run chess engine tests
./gradlew :chess-engine:jvmTest
```

The demo demonstrates:
- ✅ Board visualization with ASCII art
- ✅ Move execution and history tracking
- ✅ FEN parsing and generation
- ✅ Position analysis and validation
- ✅ Game state management
- ✅ Move highlighting and comparison tools

## Continuous Integration

The project includes GitHub Actions workflow (`.github/workflows/ci.yml`) for:
- **Multi-platform builds**: Linux, macOS, Windows
- **Gradle cache management**: Automatic caching and cleanup
- **Xcode detection**: Graceful handling of macOS runners without Xcode
- **Selective testing**: Native tests only on macOS with Xcode
- **Build validation**: Metadata compilation on all platforms
- **Artifact upload**: Native binaries from successful macOS builds

### CI Build Strategy
- **All platforms**: Kotlin metadata compilation and validation
- **macOS only**: Native tests and binary builds (when Xcode available)
- **Fallback**: Graceful degradation when native tools unavailable
- **Caching**: Gradle dependencies cached for faster builds

## Version Control

The project is initialized with Git and includes a comprehensive .gitignore file that excludes:
- Build artifacts and Gradle cache
- IDE-specific files (IntelliJ, VS Code, Eclipse)
- Platform-specific files (macOS, Windows, Linux)
- Chess RL specific files (models, training data, logs)

```bash
# Check repository status
git status

# View commit history
git log --oneline
```

## Next Steps

This project structure is ready for implementing the chess RL bot according to the task plan. Each module provides the foundation for incremental development of:

1. Basic chess data structures and board representation
2. Neural network implementation with training capabilities
3. RL framework with environment and agent interfaces
4. Integration layer connecting chess with RL
5. Self-play training system
6. Training interface and monitoring tools

The development environment is fully configured with:
- ✅ Modular Kotlin multiplatform project structure
- ✅ Comprehensive build system with Gradle
- ✅ Testing framework setup
- ✅ Continuous integration pipeline
- ✅ Git repository with proper ignore rules
- ✅ Cross-platform build verification
