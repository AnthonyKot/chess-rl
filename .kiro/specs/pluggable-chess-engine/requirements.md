# Requirements Document - Pluggable Chess Engine Adapter

## Introduction

This feature introduces a pluggable ChessEngineAdapter system that allows the chess RL bot to use different chess engine implementations while maintaining the same RL API. The primary goal is to enable side-by-side operation of the current built-in engine with a new chesslib-based implementation, selectable via CLI/config flags, until parity and stability are achieved.

The system will maintain the current engine as default while providing the option to use bhlangonijr/chesslib via `--engine chesslib`, ensuring no changes to RL API and only touching engine internals and adapter-backed environment.

## Requirements

### Requirement 1 - Chess Engine Abstraction Interface

**User Story:** As a chess RL developer, I want a clean abstraction interface for chess engines, so that I can swap between different chess rule implementations without changing RL training code.

#### Acceptance Criteria

1. WHEN defining the interface THEN it SHALL provide ChessEngineAdapter with immutable ChessState and ChessMove types
2. WHEN implementing core methods THEN it SHALL include initialState, getLegalMoves, applyMove, isTerminal, getOutcome functions
3. WHEN supporting standard formats THEN it SHALL provide toFen/fromFen conversion methods
4. WHEN enabling debugging THEN it SHALL optionally support perft (performance test) functionality
5. WHEN ensuring immutability THEN it SHALL guarantee ChessState objects are immutable and thread-safe
6. IF an adapter implementation violates immutability THEN it SHALL be rejected during validation

### Requirement 2 - Built-in Engine Wrapper with Permanent Fallback

**User Story:** As a chess RL developer, I want the current handmade engine permanently available as a fallback option, so that I can always revert to the proven implementation regardless of new engine adoption.

#### Acceptance Criteria

1. WHEN wrapping the current engine THEN it SHALL implement BuiltinAdapter that delegates to existing chess logic WITHOUT modifying the original engine
2. WHEN maintaining parity THEN it SHALL produce identical results to the current direct engine usage
3. WHEN preserving fallback THEN it SHALL keep the original engine code intact and accessible via `--engine builtin` flag
4. WHEN handling state conversion THEN it SHALL convert between current board representation and ChessState
5. WHEN processing moves THEN it SHALL convert between current move format and ChessMove
6. WHEN detecting outcomes THEN it SHALL map current game status to standardized TerminalInfo
7. WHEN ensuring permanence THEN it SHALL guarantee builtin engine remains available indefinitely as fallback option
8. IF BuiltinAdapter produces different results than current engine THEN it SHALL be considered a regression

### Requirement 3 - Chesslib Engine Implementation

**User Story:** As a chess RL developer, I want a ChessEngineAdapter implementation using bhlangonijr/chesslib, so that I can leverage a well-tested external chess library for rule validation.

#### Acceptance Criteria

1. WHEN implementing chesslib adapter THEN it SHALL use FEN-based immutable state management
2. WHEN generating legal moves THEN it SHALL use MoveGenerator.generateLegalMoves for move generation
3. WHEN detecting terminal states THEN it SHALL recognize checkmate, stalemate, 50-move rule, and insufficient material
4. WHEN handling move application THEN it SHALL rehydrate Board objects per call for thread safety
5. WHEN supporting action space THEN it SHALL implement 4096 from-to action IDs with default queen promotion
6. IF chesslib adapter fails basic chess rule validation THEN it SHALL not be considered production-ready

### Requirement 4 - Action Space Mapping

**User Story:** As a chess RL developer, I want a consistent action space mapping system, so that neural networks can work with both engine implementations using the same action encoding.

#### Acceptance Criteria

1. WHEN defining action space THEN it SHALL use 4096 from-to action IDs for consistent encoding
2. WHEN handling promotions THEN it SHALL default to queen promotion for initial implementation
3. WHEN mapping actions THEN it SHALL convert between action IDs and full ChessMove objects
4. WHEN debugging moves THEN it SHALL provide clear action ID to move string conversion
5. WHEN expanding functionality THEN it SHALL support future promotion piece selection
6. IF action mapping produces invalid moves THEN it SHALL fall back to random legal move selection

### Requirement 5 - Engine-Backed Environment

**User Story:** As a chess RL developer, I want a thin environment wrapper that delegates rule logic to the adapter, so that RL training code remains unchanged while benefiting from pluggable engines.

#### Acceptance Criteria

1. WHEN implementing environment THEN it SHALL create EngineBackedEnvironment that delegates to ChessEngineAdapter
2. WHEN resetting games THEN it SHALL call adapter.initialState for game initialization
3. WHEN getting valid actions THEN it SHALL call adapter.getLegalMoves and encode to action IDs
4. WHEN processing steps THEN it SHALL decode action IDs, call adapter.applyMove, and apply reward shaping
5. WHEN checking termination THEN it SHALL use adapter outcome while preserving early adjudication toggles
6. IF environment behavior changes between engines THEN it SHALL be documented and validated

### Requirement 6 - Engine Selection Mechanism with Permanent Dual Support

**User Story:** As a chess RL developer, I want a simple engine selection mechanism via CLI flags that permanently supports both engines, so that I can choose between implementations without code changes and always have both options available.

#### Acceptance Criteria

1. WHEN selecting engines THEN it SHALL support `--engine builtin|chesslib` CLI flag with builtin as permanent default
2. WHEN using default behavior THEN it SHALL use builtin engine unless explicitly specified to ensure backward compatibility
3. WHEN wiring components THEN it SHALL use EngineSelector to instantiate appropriate adapter for either engine
4. WHEN injecting dependencies THEN it SHALL provide selected adapter to EngineBackedEnvironment regardless of engine choice
5. WHEN loading profiles THEN it SHALL support engine selection in profile configuration while maintaining builtin default
6. WHEN ensuring availability THEN it SHALL guarantee both engines remain permanently supported and selectable
7. WHEN validating selection THEN it SHALL ensure both engines work through identical ChessEngineAdapter interface
8. IF invalid engine name is specified THEN it SHALL provide clear error message listing both available options (builtin, chesslib)

### Requirement 7 - Dependency Management

**User Story:** As a chess RL developer, I want proper dependency management for external chess libraries, so that the system can build and run with chesslib integration.

#### Acceptance Criteria

1. WHEN adding dependencies THEN it SHALL include `com.github.bhlangonijr:chesslib` in Gradle configuration
2. WHEN managing versions THEN it SHALL use latest stable chesslib version with documented compatibility
3. WHEN handling licensing THEN it SHALL confirm Apache-2.0 license compatibility
4. WHEN packaging THEN it SHALL properly shade dependencies or document version requirements
5. WHEN building THEN it SHALL ensure clean builds with both engine implementations
6. IF dependency conflicts occur THEN it SHALL provide resolution strategy and documentation

### Requirement 8 - Parity and Correctness Validation

**User Story:** As a chess RL developer, I want comprehensive parity testing between engine implementations, so that I can confidently switch engines without introducing chess rule bugs.

#### Acceptance Criteria

1. WHEN testing basic functionality THEN it SHALL verify starting position has 20 legal moves for both engines
2. WHEN testing move application THEN it SHALL confirm e2e4 produces correct FEN for both engines
3. WHEN testing terminal detection THEN it SHALL recognize checkmate and stalemate consistently
4. WHEN testing special rules THEN it SHALL handle 50-move rule and insufficient material correctly
5. WHEN running integration tests THEN it SHALL complete DQN training with zero invalid moves for both engines
6. IF engines produce different results for identical positions THEN it SHALL be investigated and resolved

### Requirement 9 - Repetition Detection

**User Story:** As a chess RL developer, I want optional repetition detection for draw by repetition, so that games can properly terminate when positions repeat three times.

#### Acceptance Criteria

1. WHEN tracking repetitions THEN it SHALL maintain minimal history signature using FEN sans clocks
2. WHEN detecting repetitions THEN it SHALL flag threefold repetition as draw with reason "repetition"
3. WHEN managing history THEN it SHALL use efficient hash set or wrapper-side tracking
4. WHEN mapping outcomes THEN it SHALL convert repetition detection to TerminalInfo format
5. WHEN testing repetitions THEN it SHALL correctly identify known threefold positions
6. IF repetition detection impacts performance significantly THEN it SHALL be made optional

### Requirement 10 - Performance and Safety

**User Story:** As a chess RL developer, I want the new engine system to maintain or improve performance while ensuring thread safety, so that training efficiency is not compromised.

#### Acceptance Criteria

1. WHEN implementing initially THEN it SHALL use simple FEN rehydrate/apply pattern for correctness
2. WHEN optimizing iteratively THEN it SHALL improve performance while maintaining immutable ChessState API
3. WHEN handling concurrency THEN it SHALL ensure no lock contention during parallel self-play
4. WHEN benchmarking THEN it SHALL achieve equal or better throughput vs builtin engine in evaluation
5. WHEN training THEN it SHALL maintain acceptable training speed range for RL workflows
6. IF performance degrades significantly THEN it SHALL be optimized or implementation approach reconsidered

### Requirement 11 - Permanent Dual-Engine Support Strategy

**User Story:** As a chess RL developer, I want permanent support for both engines with easy switching capability, so that I can choose the most appropriate engine for each use case and always have a reliable fallback.

#### Acceptance Criteria

1. WHEN implementing THEN it SHALL maintain builtin as permanent default engine for maximum compatibility
2. WHEN providing options THEN it SHALL always support both `--engine builtin` and `--engine chesslib` flags
3. WHEN testing THEN it SHALL validate both engines in CI to ensure neither implementation regresses
4. WHEN adopting THEN it SHALL allow users to choose chesslib for specific profiles while keeping builtin as system default
5. WHEN ensuring reliability THEN it SHALL never remove or deprecate the builtin engine option
6. WHEN comparing THEN it SHALL provide tools to validate identical behavior between engines for the same positions
7. IF either engine fails THEN it SHALL allow immediate switching to the other engine without system changes

### Requirement 12 - Observability and Debugging

**User Story:** As a chess RL developer, I want comprehensive observability of engine behavior, so that I can debug issues and validate correctness across different engine implementations.

#### Acceptance Criteria

1. WHEN logging THEN it SHALL include engine name in all training and evaluation logs
2. WHEN reporting outcomes THEN it SHALL include engine-reported reason string for draws and wins
3. WHEN diagnosing THEN it SHALL provide detailed termination reason classification without "other" category
4. WHEN comparing THEN it SHALL optionally support side-by-side evaluation with both engines
5. WHEN debugging THEN it SHALL log action ID to move conversions for invalid move investigation
6. IF discrepancies are found between engines THEN it SHALL provide detailed comparison reports

### Requirement 13 - Comprehensive Engine Comparison Testing

**User Story:** As a chess RL developer, I want comprehensive testing that directly compares both engines for RL DQN training effectiveness, so that I can validate that both engines produce equivalent training results.

#### Acceptance Criteria

1. WHEN testing engines THEN it SHALL validate both builtin and chesslib implementations with identical test suites
2. WHEN comparing RL training THEN it SHALL run identical DQN training sessions with both engines using same seeds
3. WHEN measuring training effectiveness THEN it SHALL compare final agent performance between engines against same baselines
4. WHEN testing modes THEN it SHALL cover train-advanced, eval-baseline, eval-h2h, and play-human modes for both engines
5. WHEN ensuring reproducibility THEN it SHALL use deterministic seeds for consistent cross-engine comparison
6. WHEN validating quality THEN it SHALL verify invalid_moves=0 and identical termination reason classification for both engines
7. WHEN comparing performance THEN it SHALL ensure average game lengths and training convergence are comparable between engines
8. WHEN providing comparison tools THEN it SHALL include utilities to run side-by-side engine validation on same positions
9. IF engines produce different RL training outcomes THEN it SHALL be investigated and documented before deployment

### Requirement 14 - Feature Gap Management

**User Story:** As a chess RL developer, I want a clear plan for handling feature gaps and future enhancements, so that the system can evolve while maintaining compatibility.

#### Acceptance Criteria

1. WHEN handling promotions THEN it SHALL initially default to queen with plan for full promotion support
2. WHEN retiring legacy code THEN it SHALL migrate gradually while keeping legacy files until parity tests pass
3. WHEN simplifying adjudication THEN it SHALL keep early adjudication as evaluation-layer toggle, not engine logic
4. WHEN expanding action space THEN it SHALL support future promotion piece selection or post-policy heuristics
5. WHEN cleaning up THEN it SHALL ensure all rule-dependent code references adapter exclusively
6. IF feature gaps impact training effectiveness THEN it SHALL be prioritized for implementation

### Requirement 15 - Documentation and Adoption

**User Story:** As a chess RL developer, I want clear documentation of the engine selection system and migration status, so that I can effectively use and contribute to the pluggable engine system.

#### Acceptance Criteria

1. WHEN documenting usage THEN it SHALL provide clear examples of engine selection via CLI and profiles
2. WHEN explaining architecture THEN it SHALL document adapter interface and implementation patterns
3. WHEN guiding migration THEN it SHALL provide step-by-step adoption path from builtin to chesslib
4. WHEN troubleshooting THEN it SHALL document common issues and resolution strategies
5. WHEN contributing THEN it SHALL provide guidelines for adding new engine implementations
6. IF documentation is unclear or incomplete THEN it SHALL be updated based on user feedback