# Requirements Document

## Introduction

This feature adds RL4J (Reinforcement Learning for Java) as an alternative backend to the existing manual RL implementation. The goal is to provide users with a choice between the current custom DQN implementation and a mature, industry-standard RL framework while maintaining full compatibility with existing chess engine integration and neural network components.

## Requirements

### Requirement 1

**User Story:** As a chess RL researcher, I want to choose between manual and RL4J backends via a command-line flag, so that I can leverage mature RL frameworks while preserving my existing custom implementation.

#### Acceptance Criteria

1. WHEN I run the CLI with `--rl manual` THEN the system SHALL use the existing custom DQN implementation
2. WHEN I run the CLI with `--rl rl4j` THEN the system SHALL use the RL4J framework backend
3. WHEN I run the CLI without the `--rl` flag THEN the system SHALL default to manual backend
4. WHEN I run the CLI with `--help` THEN the system SHALL display the `--rl` flag options and descriptions

### Requirement 2

**User Story:** As a developer, I want the RL4J backend to reuse existing state encoding and action spaces, so that I maintain consistency across different RL implementations.

#### Acceptance Criteria

1. WHEN using RL4J backend THEN the system SHALL use the existing 839-dimensional state encoding
2. WHEN using RL4J backend THEN the system SHALL use the existing 4096-dimensional action space
3. WHEN switching between backends THEN the system SHALL maintain identical observation and action representations
4. WHEN using RL4J backend THEN the system SHALL wrap existing encodings in RL4J-compatible interfaces

### Requirement 3

**User Story:** As a chess RL practitioner, I want strict legal move enforcement in RL4J backend, so that the agent never attempts illegal moves during training or evaluation.

#### Acceptance Criteria

1. WHEN the RL4J agent selects an illegal action THEN the system SHALL either mask the action or provide a legal fallback
2. WHEN action masking is enabled THEN the system SHALL set illegal action Q-values to very negative values before selection
3. WHEN fallback mode is used THEN the system SHALL log the fallback action selection for debugging
4. WHEN training completes THEN the system SHALL report zero illegal move attempts in the metrics

### Requirement 4

**User Story:** As a researcher, I want RL4J configuration to map from existing profile settings, so that I can maintain consistent hyperparameters across backends.

#### Acceptance Criteria

1. WHEN I specify learning rate in profiles THEN the system SHALL map it to RL4J's QLearningConfiguration
2. WHEN I specify batch size, buffer size, and other RL parameters THEN the system SHALL configure RL4J accordingly
3. WHEN I use existing profile files THEN the system SHALL automatically translate parameters to RL4J format
4. WHEN RL4J training starts THEN the system SHALL log the resolved configuration for reproducibility

### Requirement 5

**User Story:** As a user, I want checkpoint compatibility between backends, so that I can save and load models regardless of which RL backend was used for training.

#### Acceptance Criteria

1. WHEN I save a model with RL4J backend THEN the system SHALL create both .zip and metadata files
2. WHEN I load a model with `--load` or `--load-best` THEN the system SHALL support both .json and .zip formats
3. WHEN I switch backends THEN the system SHALL maintain model evaluation and promotion logic
4. WHEN checkpointing occurs THEN the system SHALL include backend type and version in metadata

### Requirement 6

**User Story:** As a developer, I want the RL4J MDP wrapper to integrate seamlessly with existing chess engine adapters, so that game logic remains unchanged.

#### Acceptance Criteria

1. WHEN the MDP resets THEN the system SHALL initialize the chess position using existing engine adapters
2. WHEN the MDP steps THEN the system SHALL apply moves through existing chess engine interfaces
3. WHEN computing rewards THEN the system SHALL use existing reward shaping logic
4. WHEN detecting terminal states THEN the system SHALL use existing game outcome detection

### Requirement 7

**User Story:** As a researcher, I want evaluation and testing to work identically across backends, so that I can compare performance fairly.

#### Acceptance Criteria

1. WHEN running evaluation with RL4J backend THEN the system SHALL produce identical JSON output format
2. WHEN running head-to-head matches THEN the system SHALL support models from both backends
3. WHEN generating metrics THEN the system SHALL maintain consistent measurement methodology
4. WHEN running baseline evaluations THEN the system SHALL produce comparable results across backends

### Requirement 8

**User Story:** As a developer, I want comprehensive testing for RL4J integration, so that I can ensure reliability and correctness of the new backend.

#### Acceptance Criteria

1. WHEN running unit tests THEN the system SHALL verify MDP reset/step/isDone correctness
2. WHEN testing action masking THEN the system SHALL ensure only legal moves are selected
3. WHEN running integration tests THEN the system SHALL verify end-to-end training produces valid metrics
4. WHEN running soak tests THEN the system SHALL verify checkpointing, resuming, and long-term stability