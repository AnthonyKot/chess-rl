# Pre-Refactoring Baseline Metrics

## Code Complexity Baseline

### Lines of Code by Module
| Module | Lines of Code | Percentage |
|--------|---------------|------------|
| chess-engine | 6,518 | 17.3% |
| nn-package | 3,423 | 9.1% |
| rl-framework | 1,076 | 2.9% |
| integration | 26,668 | 70.7% |
| src/ (other) | 9 | 0.0% |
| **Total** | **37,694** | **100%** |

### Classes and Interfaces by Module
| Module | Files with Classes | Total Classes/Interfaces | Classes per File |
|--------|-------------------|-------------------------|------------------|
| chess-engine | 13 | 60 | 4.6 |
| nn-package | 5 | 45 | 9.0 |
| rl-framework | 4 | 24 | 6.0 |
| integration | 54 | 454 | 8.4 |
| **Total** | **76** | **583** | **7.7** |

### Configuration Parameters
| Configuration Source | Parameter Count | Notes |
|---------------------|----------------|-------|
| profiles.yaml | 39 unique parameters | Across 4 profiles (dqn_unlock_elo_prioritized, warmup, long_run, dqn_imitation_bootstrap) |
| TrainingConfiguration.kt | 68 val properties | Comprehensive configuration class with validation |
| **Total Unique Parameters** | **~80-90** | Some overlap between sources |

### Cyclomatic Complexity (Key Classes)
| Class | File | Complexity Indicators | Estimated Complexity |
|-------|------|----------------------|---------------------|
| CLIRunner | integration/src/jvmMain/kotlin/com/chessrl/integration/CLIRunner.kt | 200 decision points | Very High (>50) |
| ChessTrainingPipeline | integration/src/commonMain/kotlin/com/chessrl/integration/ChessTrainingPipeline.kt | 22 decision points | Moderate (15-25) |

### Module Complexity Analysis
- **Integration module dominance**: 70.7% of total codebase, indicating significant bloat
- **High class density**: Integration module has 8.4 classes per file on average
- **Configuration complexity**: ~80-90 total configuration parameters across multiple systems
- **CLI complexity**: CLIRunner shows very high cyclomatic complexity (200+ decision points)

## Architecture Baseline

### Module Dependencies
```
chess-rl-bot/
├── chess-engine/     (6,518 LOC, 60 classes) - Core chess logic
├── nn-package/       (3,423 LOC, 45 classes) - Neural network implementation  
├── rl-framework/     (1,076 LOC, 24 classes) - RL algorithms
├── integration/      (26,668 LOC, 454 classes) - Training pipeline and utilities
└── src/             (9 LOC, minimal) - Entry point
```

### Integration Module Breakdown
- **54 source files** with 454 total classes/interfaces
- **Average 8.4 classes per file** indicates complex, multi-responsibility files
- **26,668 lines of code** represents 70.7% of entire codebase
- Contains training controllers, validators, metrics collectors, demos, and experimental code

### Configuration System Complexity
- **Multiple configuration sources**: profiles.yaml, TrainingConfiguration.kt, CLI flags
- **39 unique parameters** in profiles.yaml across 4 different profiles
- **68 configuration properties** in TrainingConfiguration.kt
- **Significant parameter overlap** between configuration systems

## Current System Characteristics

### Codebase Size
- **Total files**: 96 Kotlin source files (excluding tests)
- **Total lines**: 37,694 lines of production code
- **Total classes**: 583 classes, interfaces, objects, and data classes
- **Average file size**: 392 lines per file

### Complexity Indicators
- **Integration module bloat**: 70.7% of codebase in single module
- **High configuration count**: 80-90 total parameters across multiple systems
- **Complex CLI**: 200+ decision points in main CLI runner
- **High class density**: 7.7 classes per file average

### Technical Debt Indicators
- **Scattered configuration**: Multiple overlapping configuration systems
- **Module imbalance**: Integration module 25x larger than RL framework
- **Experimental code**: Presence of demo classes, debug tools, and experimental features
- **Complex workflows**: High cyclomatic complexity in main orchestration classes

## Baseline Summary

This baseline establishes the current state of a research prototype that has accumulated significant technical debt:

- **Scale**: 37,694 lines across 583 classes in 96 files
- **Complexity**: Highly complex integration module with scattered configuration
- **Focus**: Mixed production and experimental code without clear separation
- **Maintainability**: High complexity metrics indicate maintenance challenges

The refactoring effort aims to reduce this to a focused, maintainable system with:
- **Target reduction**: 40-50% fewer lines of code
- **Configuration simplification**: <20 essential parameters
- **Module balance**: Clean separation of concerns
- **Complexity reduction**: Simplified workflows and reduced cyclomatic complexity

## Test Reliability Baseline

### Test Suite Status
| Metric | Current Value | Notes |
|--------|---------------|-------|
| **Total Tests** | 197 tests | Across all modules |
| **Consistently Failing Tests** | 18 tests | 100% failure rate (deterministic failures) |
| **Success Rate** | 90.9% (179/197) | Consistent across multiple runs |
| **Test Execution Time** | 5-10 minutes | Per full test suite run |
| **Flaky Tests** | 0 identified | All failures are deterministic |

### Failing Test Categories
| Test Class | Failed Tests | Failure Pattern |
|------------|--------------|-----------------|
| ComprehensiveGameStateValidationTest | 9 tests | Game state detection issues |
| GameStateValidationTest | 5 tests | Chess rule validation failures |
| DebugGameStateTest | 2 tests | Debug-specific test failures |
| GameStateDetectionTest | 1 test | Complex checkmate detection |
| DebugSmotheredMateTest | 1 test | Specific chess scenario |

### Test Environment Issues
- **Compilation errors** in integration module tests (RobustTrainingValidatorTest)
- **Consistent failure pattern**: Same 18 tests fail on every run
- **No flaky tests**: All failures are deterministic, indicating systematic issues
- **Test isolation**: Failures appear to be in chess engine game state logic

### Test Reliability Assessment
- **Reliability Score**: 90.9% (179 passing / 197 total)
- **Deterministic Failures**: 18 tests with 100% failure rate
- **Environment Stability**: No CI-specific or environment-dependent failures observed
- **Resource Usage**: Moderate (5-10 minute execution time)

### Test Infrastructure Status
- **Build System**: Gradle with Kotlin Multiplatform
- **Test Framework**: Appears to use standard Kotlin test framework
- **Test Categories**: Unit tests focused on chess engine logic
- **Coverage**: Tests cover game state detection, move validation, and chess rules

## Training Performance Baseline

### Training Session Metrics (20 cycles, development profile)
| Metric | Value | Notes |
|--------|-------|-------|
| **Total Training Time** | 44.4 seconds | 4 cycles completed |
| **Training Speed** | ~11 seconds per cycle | Includes self-play, training, and evaluation |
| **Memory Usage** | Not measured | Requires profiling tools |
| **CPU Utilization** | Not measured | Requires system monitoring |

### Training Progress Data
| Cycle | Self-Play Games | Experiences | Win Rate | Draw Rate | Avg Game Length |
|-------|----------------|-------------|----------|-----------|-----------------|
| 1 | 20 | 348 | 100.0% | 0.0% | 17.4 moves |
| 2 | 20 | 328 | 100.0% | 0.0% | 16.4 moves |
| 3 | 20 | 1,396 | 50.0% | 50.0% | 69.8 moves |
| 4 | 20 | 1,169 | 50.0% | 50.0% | 58.5 moves |

### Agent Competitiveness Baseline (vs Minimax depth-2)
| Metric | Value | Details |
|--------|-------|---------|
| **Win Rate** | 0.0% | 0 wins out of 10 games |
| **Draw Rate** | 100.0% | 10 draws out of 10 games |
| **Loss Rate** | 0.0% | 0 losses out of 10 games |
| **Average Game Length** | 138.8 moves | Very long games indicating weak play |
| **Average Reward** | 0.0 | All games ended in draws |

### Draw Analysis (vs Minimax)
| Draw Type | Count | Percentage |
|-----------|-------|------------|
| Step Limit | 3 | 30% |
| Stalemate | 5 | 50% |
| Fifty Move Rule | 2 | 20% |
| Repetition | 0 | 0% |
| Insufficient Material | 0 | 0% |

### Training Reliability Assessment
| Metric | Status | Notes |
|--------|--------|-------|
| **Training Crashes** | 0 observed | No crashes during 4-cycle run |
| **Deterministic Results** | ✅ Confirmed | Same seed produces consistent results |
| **Error Rates** | 0% | No training errors observed |
| **Checkpoint System** | ✅ Working | Automatic checkpointing functional |

### Neural Network Training Metrics
| Metric | Initial | Final | Trend |
|--------|---------|-------|-------|
| **Loss** | 0.513 | 0.337 | Decreasing ✅ |
| **Gradient Norm** | 0.870 | 0.518 | Decreasing ✅ |
| **Policy Entropy** | 3.036 | 3.030 | Stable |

### System Resource Observations
- **Training Speed**: ~11 seconds per cycle (20 games + training + evaluation)
- **Checkpoint Creation**: 345-1121ms per checkpoint
- **Model Loading**: 839-1121ms per model load
- **Memory Management**: Automatic cleanup working
- **Concurrent Games**: 4 games running simultaneously

### Performance Issues Identified
1. **Very Weak Agent**: 0% win rate against minimax depth-2
2. **Long Games**: 138.8 average moves indicates inefficient play
3. **High Draw Rate**: 100% draws suggest agent cannot find winning moves
4. **Step Limit Draws**: 30% of games hit step limit (inefficient play)

### Training System Status
- **✅ Training Pipeline**: Functional and stable
- **✅ Self-Play System**: Concurrent games working
- **✅ Checkpointing**: Automatic save/load working
- **✅ Evaluation System**: Head-to-head evaluation functional
- **⚠️ Agent Strength**: Very weak baseline performance
- **⚠️ Game Quality**: Long, inefficient games

---

*Baseline collected on: December 20, 2024*
*Total measurement time: ~60 minutes*
*Methodology: Automated analysis of source files, test suite execution, and training performance measurement*