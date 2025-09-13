# Contributing Guide

## Overview

Welcome to the Chess RL Bot project! This guide provides comprehensive information for contributors, including development setup, coding standards, contribution workflows, and community guidelines.

## Getting Started

### Prerequisites

Before contributing, ensure you have:
- **Java 17+** installed and configured
- **Git** for version control
- **IntelliJ IDEA** or **VS Code** (recommended IDEs)
- **Basic knowledge** of Kotlin, machine learning, and chess
- **Understanding** of reinforcement learning concepts

### Development Environment Setup

#### 1. Fork and Clone the Repository
```bash
# Fork the repository on GitHub, then clone your fork
git clone https://github.com/yourusername/chess-rl-bot.git
cd chess-rl-bot

# Add upstream remote
git remote add upstream https://github.com/original/chess-rl-bot.git
```

#### 2. Set Up Development Environment
```bash
# Verify Java installation
java -version
javac -version

# Build the project
./gradlew build

# Run tests to ensure everything works
./gradlew test

# Run a quick integration test
./gradlew :integration:test --tests QuickIntegrationTest
```

#### 3. IDE Configuration

**IntelliJ IDEA Setup**:
1. Open the project directory in IntelliJ IDEA
2. Configure Project SDK to Java 17+
3. Enable Kotlin plugin (usually pre-installed)
4. Import Gradle project settings
5. Configure code style (see Code Style section)

**VS Code Setup**:
1. Install Kotlin extension
2. Install Gradle extension
3. Open project folder
4. Configure Java path in settings

### Project Structure Understanding

```
chess-rl-bot/
├── chess-engine/          # Chess game implementation
│   ├── src/commonMain/     # Core chess logic
│   ├── src/commonTest/     # Chess engine tests
│   └── build.gradle.kts    # Chess engine build config
├── nn-package/            # Neural network implementation
│   ├── src/commonMain/     # Neural network core
│   ├── src/commonTest/     # Neural network tests
│   └── build.gradle.kts    # Neural network build config
├── rl-framework/          # Reinforcement learning framework
│   ├── src/commonMain/     # RL algorithms and interfaces
│   ├── src/commonTest/     # RL framework tests
│   └── build.gradle.kts    # RL framework build config
├── integration/           # Integration layer
│   ├── src/commonMain/     # Chess RL integration
│   ├── src/commonTest/     # Integration tests
│   └── build.gradle.kts    # Integration build config
├── docs/                  # Documentation
├── config/                # Configuration files
└── build.gradle.kts       # Root build configuration
```

## Contribution Types

### 1. Bug Fixes
- **Scope**: Fix existing functionality issues
- **Process**: Issue → Branch → Fix → Test → PR
- **Requirements**: Reproduce bug, add test case, verify fix

### 2. Feature Enhancements
- **Scope**: Improve existing features
- **Process**: Discussion → Design → Implementation → Review
- **Requirements**: Design document, comprehensive tests, documentation

### 3. New Features
- **Scope**: Add new capabilities
- **Process**: RFC → Design → Implementation → Integration
- **Requirements**: RFC document, design review, extensive testing

### 4. Performance Improvements
- **Scope**: Optimize system performance
- **Process**: Benchmark → Optimize → Validate → Document
- **Requirements**: Before/after benchmarks, performance tests

### 5. Documentation
- **Scope**: Improve project documentation
- **Process**: Identify gaps → Write → Review → Update
- **Requirements**: Clear, accurate, well-structured content

## Development Workflow

### 1. Issue-Based Development

#### Creating Issues
```markdown
## Bug Report Template
**Description**: Brief description of the bug
**Steps to Reproduce**: 
1. Step 1
2. Step 2
3. Step 3

**Expected Behavior**: What should happen
**Actual Behavior**: What actually happens
**Environment**: OS, Java version, etc.
**Logs**: Relevant log excerpts

## Feature Request Template
**Description**: Brief description of the feature
**Motivation**: Why is this feature needed?
**Proposed Solution**: How should it work?
**Alternatives**: Other approaches considered
**Additional Context**: Any other relevant information
```

#### Working on Issues
```bash
# 1. Assign yourself to the issue
# 2. Create a branch
git checkout -b feature/issue-123-add-ppo-algorithm

# 3. Make your changes
# 4. Commit with descriptive messages
git commit -m "feat: implement PPO algorithm for improved training stability

- Add PPOAlgorithm class with clipped objective
- Implement advantage calculation and policy updates
- Add comprehensive unit tests
- Update documentation

Fixes #123"
```

### 2. Branch Naming Conventions

```bash
# Feature branches
feature/issue-number-short-description
feature/123-add-ppo-algorithm

# Bug fix branches
bugfix/issue-number-short-description
bugfix/456-fix-memory-leak

# Documentation branches
docs/issue-number-short-description
docs/789-update-api-documentation

# Performance branches
perf/issue-number-short-description
perf/101-optimize-batch-processing
```

### 3. Commit Message Guidelines

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```bash
# Format
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]

# Types
feat:     # New feature
fix:      # Bug fix
docs:     # Documentation changes
style:    # Code style changes (formatting, etc.)
refactor: # Code refactoring
perf:     # Performance improvements
test:     # Adding or updating tests
chore:    # Maintenance tasks

# Examples
feat(rl): implement PPO algorithm for better training stability
fix(chess): resolve castling validation bug in edge cases
docs(api): update neural network API documentation
perf(nn): optimize matrix multiplication for 20% speed improvement
test(integration): add comprehensive self-play training tests
```

## Code Standards

### 1. Kotlin Coding Style

#### General Principles
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful names for variables, functions, and classes
- Keep functions small and focused (< 50 lines preferred)
- Use immutable data structures when possible
- Prefer composition over inheritance

#### Code Style Configuration
```kotlin
// .editorconfig
root = true

[*.{kt,kts}]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true
max_line_length = 120

# IntelliJ IDEA code style settings
// File → Settings → Editor → Code Style → Kotlin
// Import scheme: docs/kotlin-code-style.xml
```

#### Naming Conventions
```kotlin
// Classes: PascalCase
class NeuralNetwork
class ChessEnvironment
class TrainingPipeline

// Functions and variables: camelCase
fun trainAgent()
val learningRate = 0.001
var currentEpisode = 0

// Constants: SCREAMING_SNAKE_CASE
const val MAX_EPISODES = 10000
const val DEFAULT_BATCH_SIZE = 64

// Packages: lowercase with dots
package com.chessrl.nn
package com.chessrl.chess
```

#### Documentation Standards
```kotlin
/**
 * Trains a chess RL agent using the specified configuration.
 *
 * This function implements a complete training pipeline including:
 * - Experience collection through self-play
 * - Batch processing and neural network updates
 * - Training validation and progress monitoring
 *
 * @param config Training configuration parameters
 * @param episodes Number of training episodes to run
 * @param validationFrequency How often to run validation (in episodes)
 * @return Training result with metrics and final model
 * @throws TrainingException if training fails due to configuration or system issues
 *
 * @sample
 * ```kotlin
 * val config = TrainingConfig(learningRate = 0.001, batchSize = 64)
 * val result = trainAgent(config, episodes = 1000, validationFrequency = 100)
 * println("Final win rate: ${result.finalWinRate}")
 * ```
 */
fun trainAgent(
    config: TrainingConfig,
    episodes: Int,
    validationFrequency: Int = 100
): TrainingResult {
    // Implementation
}
```

### 2. Testing Standards

#### Test Structure
```kotlin
class NeuralNetworkTest {
    
    @Test
    fun `forward propagation should produce correct output dimensions`() {
        // Given
        val network = DenseNeuralNetwork(
            inputSize = 776,
            hiddenLayers = listOf(512, 256),
            outputSize = 4096
        )
        val input = DoubleArray(776) { Random.nextDouble() }
        
        // When
        val output = network.forward(input)
        
        // Then
        assertEquals(4096, output.size)
        assertTrue(output.all { it.isFinite() })
    }
    
    @Test
    fun `training should converge on simple XOR problem`() {
        // Given
        val network = createSmallNetwork()
        val xorDataset = createXORDataset()
        
        // When
        val result = network.train(xorDataset, epochs = 1000, batchSize = 4)
        
        // Then
        assertTrue(result.finalLoss < 0.1, "Network should converge on XOR problem")
        assertTrue(result.converged, "Training should reach convergence")
    }
}
```

#### Test Categories
```kotlin
// Unit tests: Test individual components
class ChessBoardTest
class DenseLayerTest
class DQNAlgorithmTest

// Integration tests: Test component interactions
class ChessRLIntegrationTest
class TrainingPipelineTest
class SelfPlaySystemTest

// Performance tests: Validate performance characteristics
class NeuralNetworkPerformanceTest
class ChessEnginePerformanceTest
class TrainingThroughputTest

// End-to-end tests: Test complete workflows
class CompleteTrainingE2ETest
class SelfPlayTrainingE2ETest
```

#### Test Naming Conventions
```kotlin
// Format: methodName_should_expectedBehavior_when_condition
fun forward_should_produceCorrectDimensions_when_validInputProvided()
fun makeMove_should_returnError_when_invalidMoveAttempted()
fun train_should_converge_when_sufficientDataProvided()

// BDD style (preferred for complex scenarios)
fun `should train successfully when valid configuration provided`()
fun `should detect checkmate when king is trapped`()
fun `should improve win rate when training progresses`()
```

### 3. Performance Guidelines

#### Memory Management
```kotlin
// Prefer immutable data structures
data class TrainingConfig(
    val learningRate: Double,
    val batchSize: Int,
    val episodes: Int
)

// Use object pooling for frequently created objects
class ExperiencePool {
    private val pool = mutableListOf<Experience>()
    
    fun acquire(): Experience = pool.removeLastOrNull() ?: Experience()
    fun release(experience: Experience) {
        experience.reset()
        pool.add(experience)
    }
}

// Avoid unnecessary allocations in hot paths
fun processExperiences(experiences: List<Experience>) {
    // Reuse arrays instead of creating new ones
    val stateBuffer = DoubleArray(776)
    val actionBuffer = IntArray(4096)
    
    for (experience in experiences) {
        experience.state.copyInto(stateBuffer)
        // Process without additional allocations
    }
}
```

#### Computational Efficiency
```kotlin
// Use appropriate data structures
val validMoves = HashSet<Move>() // O(1) lookup
val moveHistory = ArrayList<Move>() // O(1) append

// Optimize critical paths
inline fun fastMatrixMultiply(a: DoubleArray, b: DoubleArray): DoubleArray {
    // Optimized implementation for performance-critical operations
}

// Profile and benchmark performance-critical code
@Test
fun benchmarkNeuralNetworkPerformance() {
    val network = createLargeNetwork()
    val input = createRandomInput()
    
    val startTime = System.nanoTime()
    repeat(1000) {
        network.forward(input)
    }
    val endTime = System.nanoTime()
    
    val averageTimeMs = (endTime - startTime) / 1_000_000.0 / 1000
    assertTrue(averageTimeMs < 10.0, "Forward pass should be under 10ms on average")
}
```

## Pull Request Process

### 1. Pre-PR Checklist

Before submitting a pull request:

```bash
# 1. Ensure your branch is up to date
git fetch upstream
git rebase upstream/main

# 2. Run all tests
./gradlew test

# 3. Run code quality checks
./gradlew detekt
./gradlew ktlintCheck

# 4. Build the project
./gradlew build

# 5. Run integration tests
./gradlew :integration:test

# 6. Update documentation if needed
# 7. Add/update tests for your changes
```

### 2. PR Template

```markdown
## Description
Brief description of the changes made.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Code refactoring

## Related Issues
Fixes #123
Relates to #456

## Changes Made
- Detailed list of changes
- Another change
- Third change

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Performance tests added/updated (if applicable)
- [ ] Manual testing completed

## Performance Impact
- Describe any performance implications
- Include benchmark results if applicable

## Documentation
- [ ] Code comments updated
- [ ] API documentation updated
- [ ] User documentation updated
- [ ] README updated (if applicable)

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] No breaking changes (or breaking changes documented)
```

### 3. Review Process

#### For Contributors
1. **Self-review**: Review your own code before submitting
2. **Clear description**: Provide comprehensive PR description
3. **Respond promptly**: Address reviewer feedback quickly
4. **Test thoroughly**: Ensure all tests pass and functionality works

#### For Reviewers
1. **Timely reviews**: Respond within 2-3 business days
2. **Constructive feedback**: Provide specific, actionable suggestions
3. **Code quality**: Check for style, performance, and maintainability
4. **Test coverage**: Ensure adequate test coverage for changes

#### Review Criteria
- **Functionality**: Does the code work as intended?
- **Code Quality**: Is the code clean, readable, and maintainable?
- **Performance**: Are there any performance implications?
- **Testing**: Is there adequate test coverage?
- **Documentation**: Is the code properly documented?
- **Compatibility**: Does it maintain backward compatibility?

## Community Guidelines

### 1. Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors. Please:

- **Be respectful**: Treat all community members with respect and kindness
- **Be inclusive**: Welcome newcomers and help them get started
- **Be constructive**: Provide helpful feedback and suggestions
- **Be patient**: Remember that everyone has different experience levels
- **Be professional**: Maintain professional communication in all interactions

### 2. Communication Channels

- **GitHub Issues**: Bug reports, feature requests, and discussions
- **Pull Requests**: Code reviews and technical discussions
- **Discussions**: General questions and community discussions
- **Documentation**: Comprehensive guides and API references

### 3. Getting Help

If you need help:

1. **Check documentation**: Review existing documentation and guides
2. **Search issues**: Look for similar issues or questions
3. **Ask questions**: Create a discussion or issue for help
4. **Join community**: Participate in community discussions

### 4. Recognition

We value all contributions and recognize contributors through:

- **Contributors list**: All contributors are listed in the project
- **Release notes**: Significant contributions are highlighted
- **Community recognition**: Outstanding contributions are celebrated
- **Mentorship opportunities**: Experienced contributors can mentor newcomers

## Advanced Contribution Topics

### 1. Architecture Decisions

For significant architectural changes:

1. **Create RFC**: Write a Request for Comments document
2. **Community discussion**: Engage the community in design discussions
3. **Prototype**: Create a proof-of-concept implementation
4. **Review process**: Go through thorough design review
5. **Implementation**: Implement with community feedback

### 2. Performance Contributions

For performance improvements:

1. **Benchmark baseline**: Establish current performance metrics
2. **Profile bottlenecks**: Identify performance bottlenecks
3. **Implement optimization**: Make targeted improvements
4. **Validate improvement**: Demonstrate performance gains
5. **Document changes**: Update performance documentation

### 3. Research Contributions

For research-oriented contributions:

1. **Literature review**: Review relevant research papers
2. **Experimental design**: Design experiments to validate approaches
3. **Implementation**: Implement research ideas
4. **Evaluation**: Compare against baselines and existing methods
5. **Documentation**: Document research findings and implications

## Release Process

### 1. Version Numbering

We follow [Semantic Versioning](https://semver.org/):
- **MAJOR**: Incompatible API changes
- **MINOR**: Backward-compatible functionality additions
- **PATCH**: Backward-compatible bug fixes

### 2. Release Cycle

- **Regular releases**: Monthly minor releases
- **Patch releases**: As needed for critical bug fixes
- **Major releases**: Quarterly or as needed for breaking changes

### 3. Contribution to Releases

Contributors can help with releases by:
- **Testing release candidates**: Help validate pre-release versions
- **Documentation updates**: Update documentation for new features
- **Migration guides**: Help create migration guides for breaking changes
- **Release notes**: Contribute to release note preparation

## Getting Started Checklist

For new contributors:

- [ ] Read this contributing guide completely
- [ ] Set up development environment
- [ ] Run tests to ensure everything works
- [ ] Read project documentation
- [ ] Look for "good first issue" labels
- [ ] Join community discussions
- [ ] Make your first contribution
- [ ] Ask questions when needed

## Resources

### Documentation
- [Project README](../README.md)
- [API Documentation](../api/README.md)
- [Architecture Guide](../architecture/README.md)
- [Training Guide](../training/README.md)

### Development Tools
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [Git Documentation](https://git-scm.com/doc)

### Learning Resources
- [Reinforcement Learning: An Introduction](http://incompleteideas.net/book/the-book.html)
- [Deep Learning](https://www.deeplearningbook.org/)
- [Chess Programming Wiki](https://www.chessprogramming.org/)

Thank you for contributing to the Chess RL Bot project! Your contributions help make this project better for everyone.