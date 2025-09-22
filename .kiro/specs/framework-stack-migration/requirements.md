# Requirements Document

## Introduction

This feature implements a comprehensive framework stack migration that allows users to choose between a legacy custom implementation and a mature framework-based implementation using industry-standard tools (RL4J, DL4J, chesslib). The system provides a single `--stack` flag to switch the entire pipeline while maintaining full backward compatibility and allowing granular component overrides for advanced users.

## Requirements

### Requirement 1

**User Story:** As a chess RL researcher, I want to choose between legacy and framework stacks via a single command-line flag, so that I can leverage mature frameworks while preserving my existing custom implementation.

#### Acceptance Criteria

1. WHEN I run the CLI with `--stack legacy` THEN the system SHALL use the current custom engine + manual NN + manual DQN implementation
2. WHEN I run the CLI with `--stack framework` THEN the system SHALL use chesslib engine + DL4J NN + RL4J DQN implementation
3. WHEN I run the CLI without the `--stack` flag THEN the system SHALL default to legacy stack
4. WHEN I run the CLI with `--help` THEN the system SHALL display the `--stack` flag options and descriptions
5. WHEN I specify an invalid stack option THEN the system SHALL show an error message and exit

### Requirement 2

**User Story:** As a power user, I want granular component overrides for engine, neural network, and RL backends, so that I can experiment with mixed configurations.

#### Acceptance Criteria

1. WHEN I specify `--engine builtin|chesslib` THEN the system SHALL override the stack default for chess engine
2. WHEN I specify `--nn manual|dl4j` THEN the system SHALL override the stack default for neural network backend
3. WHEN I specify `--rl manual|rl4j` THEN the system SHALL override the stack default for RL backend
4. WHEN I use `--stack framework --engine builtin` THEN the system SHALL use builtin engine with DL4J NN and RL4J RL
5. WHEN component overrides conflict THEN the system SHALL use the explicit override values

### Requirement 3

**User Story:** As a developer, I want unified configuration management that maps existing profiles to both legacy and framework stacks, so that I maintain consistent hyperparameters across implementations.

#### Acceptance Criteria

1. WHEN I use existing profile files THEN the system SHALL automatically translate parameters to the selected stack format
2. WHEN using framework stack THEN the system SHALL map learning rate, batch size, and RL parameters to RL4J/DL4J configurations
3. WHEN using legacy stack THEN the system SHALL maintain existing parameter interpretation
4. WHEN stack selection occurs THEN the system SHALL log the resolved configuration for reproducibility
5. WHEN invalid parameters are detected THEN the system SHALL provide clear error messages with suggested corrections

### Requirement 4

**User Story:** As a researcher, I want chess engine abstraction that provides consistent interfaces across builtin and chesslib implementations, so that game logic remains unchanged regardless of engine choice.

#### Acceptance Criteria

1. WHEN using ChessEngineAdapter interface THEN the system SHALL provide immutable FEN state representation
2. WHEN querying legal moves THEN the system SHALL return consistent move representations across engines
3. WHEN applying moves THEN the system SHALL update game state identically for both engine implementations
4. WHEN detecting game outcomes THEN the system SHALL use consistent terminal state detection (checkmate, stalemate, 50-move rule)
5. WHEN switching engines THEN the system SHALL maintain identical observation and action space encodings

### Requirement 5

**User Story:** As a chess RL practitioner, I want strict legal move enforcement across all stack configurations, so that agents never attempt illegal moves during training or evaluation.

#### Acceptance Criteria

1. WHEN any agent selects an illegal action THEN the system SHALL either mask the action or provide a legal fallback
2. WHEN action masking is enabled THEN the system SHALL set illegal action Q-values to very negative values before selection
3. WHEN fallback mode is used THEN the system SHALL log the fallback action selection for debugging
4. WHEN training completes THEN the system SHALL report zero illegal move attempts in the metrics
5. WHEN using framework stack THEN the system SHALL implement custom policy wrapper for RL4J epsilon-greedy masking

### Requirement 6

**User Story:** As a user, I want unified checkpoint management that supports models from both stacks, so that I can save, load, and compare models regardless of which stack was used for training.

#### Acceptance Criteria

1. WHEN I save a model with legacy stack THEN the system SHALL create .json format files with metadata
2. WHEN I save a model with framework stack THEN the system SHALL create .zip format files with metadata
3. WHEN I load a model with `--load` or `--load-best` THEN the system SHALL automatically detect and support both formats
4. WHEN determining best model THEN the system SHALL use consistent promotion logic across stack types
5. WHEN checkpointing occurs THEN the system SHALL include stack type, component versions, and performance metrics in metadata

### Requirement 7

**User Story:** As a researcher, I want identical CLI user experience and JSON outputs across stacks, so that I can compare performance and integrate with existing tooling.

#### Acceptance Criteria

1. WHEN running training with any stack THEN the system SHALL produce identical high-level metrics format
2. WHEN running evaluation with any stack THEN the system SHALL produce identical JSON output structure
3. WHEN running head-to-head matches THEN the system SHALL support models from different stacks
4. WHEN using interactive play mode THEN the system SHALL work identically regardless of stack choice
5. WHEN generating diagnostics THEN the system SHALL maintain consistent measurement methodology across stacks

### Requirement 8

**User Story:** As a developer, I want comprehensive observability that clearly indicates which components are active, so that I can debug issues and verify correct stack selection.

#### Acceptance Criteria

1. WHEN training starts THEN the system SHALL log the selected stack and resolved component choices
2. WHEN components initialize THEN the system SHALL log backend names and versions for engine, NN, and RL
3. WHEN action masking occurs THEN the system SHALL log masking statistics and illegal action attempts
4. WHEN checkpointing occurs THEN the system SHALL log checkpoint format and metadata details
5. WHEN errors occur THEN the system SHALL provide stack-aware error messages with component context

### Requirement 9

**User Story:** As a researcher, I want framework stack to leverage mature implementations while maintaining performance parity, so that I can benefit from established libraries without sacrificing training efficiency.

#### Acceptance Criteria

1. WHEN using chesslib engine THEN the system SHALL provide legal move generation with performance comparable to builtin engine
2. WHEN using DL4J neural networks THEN the system SHALL maintain training throughput within 20% of manual implementation
3. WHEN using RL4J algorithms THEN the system SHALL achieve convergence rates comparable to manual DQN implementation
4. WHEN running long training sessions THEN the system SHALL maintain memory usage and stability across both stacks
5. WHEN switching stacks THEN the system SHALL complete identical training scenarios with comparable wall-clock time

### Requirement 10

**User Story:** As a developer, I want comprehensive testing that validates correctness and compatibility across all stack configurations, so that I can ensure reliability of the migration.

#### Acceptance Criteria

1. WHEN running unit tests THEN the system SHALL verify engine adapter correctness for legal moves, outcomes, and FEN handling
2. WHEN running integration tests THEN the system SHALL verify end-to-end training produces valid metrics for all stack combinations
3. WHEN running compatibility tests THEN the system SHALL verify checkpoint save/load round-trip for both formats
4. WHEN running evaluation tests THEN the system SHALL verify JSON output consistency across stacks
5. WHEN running soak tests THEN the system SHALL verify long-term stability and performance for framework stack components