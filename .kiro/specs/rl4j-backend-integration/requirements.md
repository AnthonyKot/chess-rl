# Requirements Document

## Introduction

This feature delivers an RL4J (Reinforcement Learning for Java) backend prototype that enables apples-to-apples performance comparison with our current custom DQN implementation. The core goal is to get RL4J training running end-to-end with the same chess environment so we can produce comparable metrics and determine whether we're compute-bound or algorithm-bound.

## Core Scope: Deliver an RL4J Backend Prototype

Goal: Get RL4J training running end-to-end with the same chess environment so we can produce apples-to-apples metrics.

## Requirements

### Requirement 1: Functional Parity

**User Story:** As a chess RL researcher, I want an RL4J backend that uses exactly the same problem formulation as my custom implementation, so that I can make fair performance comparisons.

#### Acceptance Criteria

1. WHEN I run training with RL4J backend THEN the system SHALL use exactly our existing 839-feature observation encoder
2. WHEN I run training with RL4J backend THEN the system SHALL use exactly our existing 4096-move action encoder
3. WHEN I use CLI switch `--nn rl4j` THEN training SHALL run with RL4J backend without changing other configs
4. WHEN I save checkpoints from RL4J backend THEN the existing BaselineEvaluator SHALL be able to load and evaluate them

**Acceptance Test:** `./gradlew :integration:run --args="--train --nn rl4j ..."` completes at least one cycle without error; resulting checkpoint loads and plays 20 evaluation games.

### Requirement 2: Configuration Mapping

**User Story:** As a developer, I want consistent hyperparameter handling across backends, so that configuration changes affect both implementations identically.

#### Acceptance Criteria

1. WHEN I specify learningRate, batchSize, gamma, targetUpdateFrequency, maxExperienceBuffer, or hidden layers THEN both backends SHALL use identical values
2. WHEN I use profile overrides or CLI flags THEN both backends SHALL be affected the same way
3. WHEN I provide invalid configuration values THEN the system SHALL refuse them with actionable error messages
4. WHEN training starts THEN the system SHALL print configuration summary showing identical hyperparameter values for both backends

**Acceptance Test:** Configuration summary printed at startup shows identical hyperparameter values for `--nn manual` and `--nn rl4j`; invalid learning rate (e.g., -0.1) aborts with a clear error.

### Requirement 3: Chess MDP Wrapper

**User Story:** As a developer, I want RL4J to integrate seamlessly with our existing chess environment, so that game logic and reward computation remain unchanged.

#### Acceptance Criteria

1. WHEN the MDP resets THEN it SHALL return the standard chess starting position
2. WHEN the MDP processes a legal move THEN it SHALL update the board and reward correctly using existing logic
3. WHEN the MDP receives an illegal action THEN it SHALL log the attempt and auto-fix by selecting the best legal move
4. WHEN the MDP detects terminal states THEN it SHALL use existing reward and terminal detection logic

**Acceptance Test:** Automated unit tests cover: reset() returns standard FEN; step(e2e4) updates board and reward correctly; illegal action triggers fallback path.

### Requirement 4: Baseline Metrics Collection

**User Story:** As a researcher, I want controlled benchmark data from both backends, so that I can make quantitative performance comparisons.

#### Acceptance Criteria

1. WHEN I run controlled training with identical seeds and profiles THEN the system SHALL capture win/draw/loss rates across cycles for both backends
2. WHEN training progresses THEN the system SHALL record loss curves, illegal move counts, and training time per cycle
3. WHEN training completes THEN the system SHALL capture peak memory usage from the JVM
4. WHEN benchmark data is collected THEN it SHALL be stored in reproducible format (CSV/JSON) for comparison

**Acceptance Test:** A `scripts/benchmark_rl4j_vs_manual.md` (or similar) showing side-by-side plots/tables produced from the same dataset.

### Requirement 5: Extended Resource Analysis (Optional)

**User Story:** As a performance analyst, I want to understand how both backends respond to increased computational resources, so that I can identify hardware scaling potential.

#### Acceptance Criteria

1. WHEN I increase training cycles THEN both backends SHALL demonstrate their scaling behavior
2. WHEN I increase worker heap size THEN the system SHALL document memory utilization changes
3. WHEN I run extended experiments THEN the system SHALL show whether both backends plateau at similar performance levels
4. WHEN resource analysis completes THEN results SHALL be clearly labeled as experimental/hardware sensitivity data

**Acceptance Test:** Report summarizing long-run results, labeled clearly as "experimental / hardware sensitivity".

### Requirement 6: Hardware Investment Case Documentation

**User Story:** As a research director, I want data-driven analysis of our performance constraints, so that I can make informed decisions about hardware investment.

#### Acceptance Criteria

1. WHEN benchmark data is analyzed THEN the system SHALL provide evidence of whether we're compute-bound
2. WHEN comparing implementations THEN any remaining algorithmic differences SHALL be highlighted for future focus
3. WHEN analysis is complete THEN findings SHALL be documented in a research summary with clear recommendations
4. WHEN presenting results THEN documentation SHALL reference specific benchmark outputs as supporting evidence

**Acceptance Test:** Research summary document (slides/markdown) referencing the benchmark outputs with clear hardware investment recommendations.