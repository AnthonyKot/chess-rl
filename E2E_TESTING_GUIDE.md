# End-to-End Testing Guide for Chess RL Bot

## Overview

This guide provides comprehensive manual testing procedures for validating the Chess RL Bot system after refactoring or changes. It covers training, evaluation, and interactive play scenarios with expected outputs and troubleshooting guidance.

## Prerequisites

- Java 11+ installed
- Gradle build system
- At least 4GB RAM available
- 2+ CPU cores recommended for concurrent training

## Testing Environment Setup

```bash
# Clone and build the project
git clone <repository-url>
cd chess-rl-bot
./gradlew build

# Verify all modules compile successfully
./gradlew :chess-engine:build
./gradlew :nn-package:build  
./gradlew :rl-framework:build
./gradlew :integration:build
```

## 1. Training System Validation

### 1.1 Fast Debug Training Test

**Purpose**: Verify basic training pipeline functionality with minimal resource usage.

**Command**:
```bash
./gradlew :integration:runCli --args="--train --profile fast-debug --cycles 5 --seed 12345 --checkpoint-dir checkpoints/test-run"
```

**Expected Behavior**:
- Training starts within 5 seconds
- Configuration validation passes with no errors
- Deterministic mode enabled with seed 12345
- Self-play games complete (5 games per cycle)
- Neural network training occurs after each cycle
- Checkpoints saved every 2 cycles
- Training completes in under 2 minutes

**Expected Output Patterns**:
```
[INFO] Starting training command
[INFO] Training configuration loaded: profile=fast-debug
[INFO] Training Pipeline initialized successfully
[INFO] Starting Training Pipeline
[INFO] Training Cycle 1/5
[INFO] Self-play completed: X wins, Y losses, Z draws
[INFO] Training cycle 1/5 completed
```

**Success Criteria**:
- ✅ No compilation errors
- ✅ Training completes all 5 cycles
- ✅ Checkpoints created in specified directory
- ✅ Final training summary displayed
- ✅ Process exits with code 0

### 1.2 Long Training Test

**Purpose**: Validate production training configuration and performance.

**Command**:
```bash
./gradlew :integration:runCli --args="--train --profile long-train --cycles 20 --seed 42 --checkpoint-dir checkpoints/long-test"
```

**Expected Behavior**:
- Larger network architecture (768, 512, 256 layers)
- 50 games per cycle with up to 8 concurrent games
- More thorough training with 50,000 experience buffer
- Checkpoints saved every 5 cycles
- Training takes 10-30 minutes depending on hardware

**Performance Expectations**:
- Games/second: 0.3-1.0 (depending on hardware)
- Memory usage: 1-3GB
- CPU utilization: 50-100% during self-play
- Average episode length: 40-80 moves

### 1.3 Deterministic Training Test

**Purpose**: Verify reproducible results with fixed seeds.

**Commands**:
```bash
# Run 1
./gradlew :integration:runCli --args="--train --profile fast-debug --cycles 3 --seed 999 --checkpoint-dir checkpoints/deterministic-1"

# Run 2  
./gradlew :integration:runCli --args="--train --profile fast-debug --cycles 3 --seed 999 --checkpoint-dir checkpoints/deterministic-2"
```

**Expected Behavior**:
- Both runs should produce identical results
- Same game outcomes and lengths
- Same training metrics (loss, rewards)
- Same final model performance

**Validation**:
Compare final training summaries - key metrics should match exactly.

## 2. Evaluation System Validation

### 2.1 Baseline Evaluation Test

**Purpose**: Verify agent evaluation against heuristic and minimax opponents.

**Commands**:
```bash
# Against heuristic baseline
./gradlew :integration:runCli --args="--evaluate --baseline --model checkpoints/test-run/best_model.json --games 50 --opponent heuristic --seed 123"

# Against minimax depth-1
./gradlew :integration:runCli --args="--evaluate --baseline --model checkpoints/test-run/best_model.json --games 50 --opponent minimax --depth 1 --seed 123"

# Against minimax depth-2
./gradlew :integration:runCli --args="--evaluate --baseline --model checkpoints/test-run/best_model.json --games 50 --opponent minimax --depth 2 --seed 123"
```

**Expected Output**:
```
Evaluation Results:
==================
Games Played: 50
Win Rate: X.X% (Y wins)
Draw Rate: X.X% (Y draws)  
Loss Rate: X.X% (Y losses)
Average Game Length: XX.X moves
Evaluation Time: X.Xs
```

**Success Criteria**:
- ✅ Evaluation completes without errors
- ✅ Win rate > 0% against heuristic
- ✅ Reasonable performance against minimax depth-1 (>20% win rate)
- ✅ Some wins against minimax depth-2 (target >40% but acceptable if lower for short training)

### 2.2 Model Comparison Test

**Purpose**: Verify head-to-head model comparison functionality.

**Command**:
```bash
./gradlew :integration:runCli --args="--evaluate --compare --modelA checkpoints/test-run/checkpoint_cycle_2.json --modelB checkpoints/test-run/checkpoint_cycle_4.json --games 30"
```

**Expected Behavior**:
- Both models load successfully
- Head-to-head games played
- Comparison statistics displayed
- Later checkpoint should generally perform better

## 3. Interactive Play Validation

### 3.1 Human vs Agent Test

**Purpose**: Verify interactive play interface works correctly.

**Command**:
```bash
./gradlew :integration:runCli --args="--play --model checkpoints/test-run/best_model.json --as white"
```

**Expected Behavior**:
- Game board displays correctly
- Human can input moves in standard notation (e.g., "e2-e4")
- Agent responds with legal moves
- Game state updates properly
- Game ends correctly (checkmate, stalemate, or draw)

**Manual Test Steps**:
1. Start the interactive session
2. Play opening moves (e2-e4, d2-d4, etc.)
3. Verify agent makes legal responses
4. Test invalid move handling
5. Play until game conclusion or resign

## 4. Configuration System Validation

### 4.1 Profile Loading Test

**Purpose**: Verify all configuration profiles load correctly.

**Commands**:
```bash
# Test each profile
./gradlew :integration:runCli --args="--train --profile fast-debug --cycles 1"
./gradlew :integration:runCli --args="--train --profile long-train --cycles 1"  
./gradlew :integration:runCli --args="--train --profile eval-only --cycles 1"
```

**Expected Behavior**:
- Each profile loads without validation errors
- Different parameters applied correctly
- Training adapts to profile settings

### 4.2 Parameter Override Test

**Purpose**: Verify CLI parameter overrides work correctly.

**Command**:
```bash
./gradlew :integration:runCli --args="--train --profile fast-debug --cycles 2 --games 3 --learning-rate 0.002 --seed 777"
```

**Expected Behavior**:
- Base profile loaded first
- CLI overrides applied correctly
- Final configuration shows overridden values

## 5. Error Handling Validation

### 5.1 Invalid Configuration Test

**Commands**:
```bash
# Invalid learning rate
./gradlew :integration:runCli --args="--train --learning-rate -0.1"

# Invalid games per cycle
./gradlew :integration:runCli --args="--train --games 0"

# Non-existent profile
./gradlew :integration:runCli --args="--train --profile non-existent"
```

**Expected Behavior**:
- Clear error messages displayed
- Process exits with non-zero code
- No training attempted with invalid configuration

### 5.2 Missing Model Test

**Command**:
```bash
./gradlew :integration:runCli --args="--evaluate --baseline --model non-existent-model.json"
```

**Expected Behavior**:
- Clear error message about missing model file
- Graceful failure without crash

## 6. Performance Validation

### 6.1 Resource Usage Test

**Purpose**: Verify system operates within reasonable resource constraints.

**Monitoring During Training**:
- Memory usage should not exceed 4GB
- CPU usage should be reasonable (not 100% continuously)
- No memory leaks over extended runs
- Disk usage grows predictably with checkpoints

**Tools**:
```bash
# Monitor during training
top -p $(pgrep java)
# or
htop
```

### 6.2 Concurrent Training Test

**Purpose**: Verify multi-threaded self-play works correctly.

**Command**:
```bash
./gradlew :integration:runCli --args="--train --profile long-train --max-concurrent-games 4 --cycles 3"
```

**Expected Behavior**:
- Multiple games run simultaneously
- No race conditions or crashes
- Performance improvement over sequential execution
- Consistent results compared to sequential training

## 7. Regression Testing

### 7.1 Chess Rules Validation

**Purpose**: Verify chess engine maintains correct rule implementation.

**Test Cases**:
- Castling rights preserved/lost correctly
- En passant captures work
- Pawn promotion handled properly
- Check/checkmate detection accurate
- Stalemate detection correct
- Draw by repetition works

**Validation Method**:
Run existing chess engine unit tests:
```bash
./gradlew :chess-engine:test
```

### 7.2 Neural Network Training

**Purpose**: Verify NN can learn simple patterns.

**Test**: Train on XOR problem:
```bash
./gradlew :nn-package:test --tests "*XOR*"
```

**Expected**: Network should achieve >95% accuracy on XOR.

### 7.3 RL Algorithm Validation

**Purpose**: Verify DQN implementation works correctly.

**Test**: Run DQN on simple test environment:
```bash
./gradlew :rl-framework:test --tests "*DQN*"
```

**Expected**: Q-values should converge for simple test cases.

## 8. Integration Testing Checklist

### Pre-Testing Setup
- [ ] Clean build completed successfully
- [ ] All unit tests pass
- [ ] Sufficient disk space (>1GB) available
- [ ] No other resource-intensive processes running

### Core Functionality
- [ ] Fast debug training completes successfully
- [ ] Long training runs without crashes
- [ ] Deterministic training produces identical results
- [ ] Checkpoints save and load correctly
- [ ] Configuration validation works properly

### Evaluation System
- [ ] Baseline evaluation runs successfully
- [ ] Model comparison works correctly
- [ ] Different opponent types supported
- [ ] Evaluation metrics calculated properly

### Interactive Features
- [ ] Human vs agent play works
- [ ] Move input/output correct
- [ ] Game state visualization accurate
- [ ] Game termination handled properly

### Error Handling
- [ ] Invalid configurations rejected gracefully
- [ ] Missing files handled with clear errors
- [ ] Resource exhaustion handled appropriately
- [ ] Network errors don't crash system

### Performance
- [ ] Training speed within expected range
- [ ] Memory usage reasonable
- [ ] CPU utilization appropriate
- [ ] No obvious memory leaks

## 9. Troubleshooting Guide

### Common Issues and Solutions

#### Training Fails to Start
**Symptoms**: Error during initialization, configuration validation fails
**Solutions**:
- Check Java version (requires 11+)
- Verify all dependencies built successfully
- Check available memory (need 2GB+)
- Validate configuration parameters

#### Model Loading Errors
**Symptoms**: "Failed to load model", architecture mismatch errors
**Solutions**:
- Ensure evaluation profile matches training profile
- Check model file exists and is not corrupted
- Verify network architecture compatibility
- Try using checkpoint manager models (*.qnet.json)

#### Poor Training Performance
**Symptoms**: Very slow games/second, high memory usage
**Solutions**:
- Reduce concurrent games count
- Use smaller network architecture
- Reduce experience buffer size
- Check for background processes consuming resources

#### Evaluation Hangs or Crashes
**Symptoms**: Evaluation never completes, out of memory errors
**Solutions**:
- Reduce number of evaluation games
- Use simpler opponent (heuristic vs minimax)
- Increase JVM heap size if needed
- Check model compatibility

#### Inconsistent Results
**Symptoms**: Different outcomes with same seed
**Solutions**:
- Verify seed is actually being used
- Check for non-deterministic operations
- Ensure single-threaded execution for reproducibility
- Validate configuration consistency

### Performance Expectations by Hardware

#### Minimum System (2 cores, 4GB RAM):
- Fast debug: 1-2 minutes for 5 cycles
- Long train: 2-5 minutes per cycle
- Evaluation: 30-60 seconds for 50 games

#### Recommended System (4+ cores, 8GB+ RAM):
- Fast debug: 30-60 seconds for 5 cycles  
- Long train: 1-2 minutes per cycle
- Evaluation: 15-30 seconds for 50 games

### Log Analysis

#### Key Log Patterns to Monitor:
```
[INFO] Training Pipeline initialized successfully
[INFO] Self-play completed: X wins, Y losses, Z draws
[INFO] Training cycle N/M completed
[WARN] Validation issues detected
[ERROR] Failed to...
```

#### Warning Signs:
- Consistent 0% win rates across many cycles
- Very high draw rates (>80%)
- Exploding gradients (gradient norm >10)
- Memory usage continuously increasing
- Very long or very short average game lengths

## 10. Success Criteria Summary

### Training System
- ✅ All profiles load and execute successfully
- ✅ Deterministic results with fixed seeds
- ✅ Reasonable training speed and resource usage
- ✅ Checkpoints save and load correctly
- ✅ Training metrics show learning progress

### Evaluation System  
- ✅ Baseline evaluation completes successfully
- ✅ Model comparison works correctly
- ✅ Results are consistent and reasonable
- ✅ Multiple opponent types supported

### Interactive Features
- ✅ Human vs agent play functions correctly
- ✅ Move validation and game state accurate
- ✅ User interface responsive and clear

### System Reliability
- ✅ Graceful error handling
- ✅ No crashes during normal operation
- ✅ Resource usage within acceptable bounds
- ✅ Consistent behavior across runs

### Performance Targets
- ✅ Training: >0.3 games/second on recommended hardware
- ✅ Evaluation: Complete 100 games in <60 seconds
- ✅ Memory: <4GB peak usage during training
- ✅ Agent competitiveness: >20% win rate vs minimax depth-1

This comprehensive testing guide ensures the Chess RL Bot system functions correctly after refactoring and provides a reliable framework for validating future changes.