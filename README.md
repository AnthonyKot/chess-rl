# Chess RL Bot

A reinforcement learning chess bot built with Kotlin/Native, featuring modular architecture with separate packages for neural networks, chess engine, RL framework, and integration.

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

- JDK 11 or higher
- Gradle 8.4+ (included via wrapper)
- Native compilation support for your platform

## Building the Project

### Quick build verification
```bash
./verify-build.sh
```

### Build all modules (requires Xcode on macOS)
```bash
./gradlew build
```

### Compile Kotlin code only
```bash
./gradlew compileKotlinMetadata
```

### Run tests
```bash
./gradlew test --exclude-task nativeTest  # Skip native tests if Xcode not available
./gradlew test                            # Full test suite (requires Xcode on macOS)
```

### Build native executable (requires Xcode on macOS)
```bash
./gradlew nativeBinaries
```

### Run the application (requires native build)
```bash
./gradlew run
```

## Platform Requirements

### macOS
- Xcode and Command Line Tools must be installed for native compilation
- Install with: `xcode-select --install`
- For full Xcode: Install from Mac App Store

### Linux/Windows
- Native compilation should work out of the box
- Requires appropriate system development tools

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

The project includes GitHub Actions workflow for:
- Multi-platform builds (Linux, macOS, Windows)
- Automated testing
- Native compilation verification

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