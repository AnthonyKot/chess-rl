# Requirements Document

## Introduction

This feature migrates the RL4J backend from a retrofitted shim-based integration to a first-class backend implementation. The goal is to eliminate reflection-based workarounds and allow RL4J to manage its own training lifecycle, experience replay, and policy management. This creates a cleaner separation between the manual experience pipeline (legacy) and the native RL4J framework pipeline, while maintaining compatibility and providing users with a choice between approaches.

## Requirements

### Requirement 1

**User Story:** As a chess RL researcher, I want RL4J to operate as a first-class backend without reflection-based workarounds, so that I can leverage the full power of the RL4J framework with proper lifecycle management.

#### Acceptance Criteria

1. WHEN using RL4J backend THEN the system SHALL use real RL4J/DL4J configuration builders instead of reflection
2. WHEN training with RL4J THEN the system SHALL call public training APIs (learn() or trainer loop) instead of protected trainStep() via reflection
3. WHEN using RL4J backend THEN the system SHALL honor ChessAgentConfig and map all hyperparameters to native RL4J configuration
4. WHEN RL4J backend initializes THEN the system SHALL remove all mock policy fallbacks and ensure policy always exposes nextAction
5. WHEN extracting metrics THEN the system SHALL use actual RL4J info objects instead of manual tracking

### Requirement 2

**User Story:** As a developer, I want RL4J to manage its own experience replay buffer instead of being forced into the manual experience pipeline, so that the framework can optimize memory usage and sampling strategies.

#### Acceptance Criteria

1. WHEN using RL4J backend THEN the system SHALL route experiences directly to RL4J's ReplayMemory using storeTransition
2. WHEN training occurs THEN the system SHALL allow RL4J to handle its own experience sampling and batch creation
3. WHEN replay buffer is full THEN the system SHALL let RL4J manage buffer overflow and replacement strategies
4. WHEN experiences are stored THEN the system SHALL ensure proper format conversion from chess observations to RL4J expected format
5. WHEN replay buffer plumbing is complete THEN the system SHALL verify experiences land correctly in RL4J's internal structures

### Requirement 3

**User Story:** As a chess RL practitioner, I want robust legal move enforcement that works natively with RL4J's policy selection, so that illegal moves are prevented at the policy level rather than through post-hoc fallbacks.

#### Acceptance Criteria

1. WHEN using RL4J backend THEN the system SHALL implement policy-level action masking that sets illegal action Q-values to very negative values
2. WHEN action masking is applied THEN the system SHALL ensure RL4J's epsilon-greedy selection only considers legal moves
3. WHEN fallback masking is needed THEN the system SHALL maintain it in ChessMDP.step() as a safety net
4. WHEN masking statistics are collected THEN the system SHALL instrument and log masking effectiveness to confirm no illegal actions reach engines
5. WHEN policy wrapper is implemented THEN the system SHALL ensure it integrates seamlessly with RL4J's training loop

### Requirement 4

**User Story:** As a developer, I want proper configuration mapping between ChessAgentConfig and RL4J/DL4J native configurations, so that hyperparameters are correctly translated without manual intervention.

#### Acceptance Criteria

1. WHEN BackendAwareChessAgentFactory is used THEN the system SHALL ensure RL4J picks up the real config mapper
2. WHEN configuration values diverge THEN the system SHALL surface validation errors with clear explanations
3. WHEN hyperparameters are mapped THEN the system SHALL translate learning rate, batch size, epsilon values, and network architecture to RL4J format
4. WHEN configuration is resolved THEN the system SHALL log the final RL4J configuration for verification
5. WHEN invalid mappings are detected THEN the system SHALL provide specific guidance on correct parameter ranges

### Requirement 5

**User Story:** As a user, I want optional CLI stack presets that simplify backend selection, so that I can easily choose between legacy and framework approaches without specifying individual components.

#### Acceptance Criteria

1. WHEN I use `--stack legacy` flag THEN the system SHALL preset --engine builtin --nn manual --rl manual
2. WHEN I use `--stack framework` flag THEN the system SHALL preset --engine chesslib --nn dl4j --rl rl4j  
3. WHEN stack presets are used THEN the system SHALL allow individual component overrides to take precedence
4. WHEN stack preset is applied THEN the system SHALL log resolved components for transparency
5. WHEN profile parsing occurs THEN the system SHALL update to handle stack presets if they are introduced

### Requirement 6

**User Story:** As a developer, I want comprehensive RL4J-specific testing that validates the first-class backend implementation, so that I can ensure the migration from reflection-based integration is successful.

#### Acceptance Criteria

1. WHEN RL4J is available THEN the system SHALL enable RL4J-only tests that cover initialization, training, and save/load round trips
2. WHEN running tests locally THEN the system SHALL provide clear documentation on JDK requirements and enableRL4J=true flag
3. WHEN CI is configured THEN the system SHALL include a job that builds with RL4J enabled and runs the gated test suite
4. WHEN RL4J backend is tested THEN the system SHALL verify proper integration without reflection-based workarounds
5. WHEN tests are gated THEN the system SHALL ensure they only run when RL4J dependencies are available

### Requirement 7

**User Story:** As a user, I want enhanced observability that clearly shows when RL4J is operating as a first-class backend, so that I can verify the migration was successful and debug any issues.

#### Acceptance Criteria

1. WHEN RL4J backend initializes THEN the system SHALL log backend creation details including component versions
2. WHEN replay buffer is used THEN the system SHALL log replay buffer usage statistics and RL4J integration status
3. WHEN checkpoints are created THEN the system SHALL log checkpoint format and include stack/component information
4. WHEN masking occurs THEN the system SHALL expand logging around action masking effectiveness and illegal action attempts
5. WHEN training progresses THEN the system SHALL include stack/component info in all relevant log messages

### Requirement 8

**User Story:** As a developer, I want updated documentation that reflects the first-class RL4J backend approach, so that users understand the migration from reflection-based integration and how to use the new capabilities.

#### Acceptance Criteria

1. WHEN documentation is updated THEN the system SHALL describe stack presets and their component mappings
2. WHEN RL4J enablement is documented THEN the system SHALL provide clear instructions for local development setup
3. WHEN masking behavior is explained THEN the system SHALL describe both policy-level and fallback masking approaches
4. WHEN migration is documented THEN the system SHALL explain the benefits of first-class RL4J backend over reflection-based approach
5. WHEN README is refreshed THEN the system SHALL include examples of using stack presets and component overrides