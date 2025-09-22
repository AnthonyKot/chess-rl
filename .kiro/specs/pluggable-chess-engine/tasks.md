# Implementation Plan - Pluggable Chess Engine Adapter

## Overview

This implementation plan provides a systematic approach to implementing the pluggable chess engine adapter system. The plan follows the milestone-based approach outlined in the original refactoring plan, with specific coding tasks that build incrementally toward a production-ready dual-engine system.

The implementation maintains the current builtin engine as the permanent default while adding chesslib as an optional alternative, ensuring backward compatibility and providing comprehensive testing and validation capabilities.

## Implementation Tasks

- [ ] 1. Core Interface and Data Types Implementation
  - **Deliverable**: Core adapter interface and data types with comprehensive documentation
  - Implement ChessEngineAdapter interface with immutable state management
  - Create ChessState, ChessMove, and TerminalInfo data classes
  - Add GameOutcome enumeration and conversion utilities
  - Implement action space mapping for 4096 from-to combinations
  - **Output**: Foundation types that both adapters will implement
  - _Requirements: 1, 4_

- [ ] 1.1 Define ChessEngineAdapter Interface
  - **Deliverable**: Complete ChessEngineAdapter interface in integration package
  - Create `integration/src/commonMain/kotlin/com/chessrl/integration/adapter/ChessEngineAdapter.kt`
  - Define interface methods: initialState, getLegalMoves, applyMove, isTerminal, getOutcome, toFen, fromFen, perft
  - Add engine identification method getEngineName() for logging and debugging
  - Include comprehensive KDoc documentation with usage examples
  - _Requirements: 1_

- [ ] 1.2 Implement Core Data Types
  - **Deliverable**: Immutable data classes for chess state and move representation
  - Create `integration/src/commonMain/kotlin/com/chessrl/integration/adapter/ChessAdapterTypes.kt`
  - Implement ChessState data class with FEN-based immutable state
  - Create ChessMove data class with algebraic notation support
  - Add TerminalInfo and GameOutcome types for standardized game endings
  - Include conversion utilities and validation methods
  - _Requirements: 1, 4_

- [ ] 1.3 Implement Action Space Mapping
  - **Deliverable**: Action encoding/decoding system for 4096 from-to action space
  - Create `integration/src/commonMain/kotlin/com/chessrl/integration/adapter/ActionSpaceMapping.kt`
  - Implement encodeMove and decodeAction methods for 64x64 action space
  - Add action masking utilities for legal move validation
  - Include promotion handling strategy with queen default
  - Add comprehensive unit tests for action space mapping
  - _Requirements: 4_

- [ ] 2. BuiltinAdapter Implementation
  - **Prerequisites**: Complete Task 1 (core interfaces and types)
  - Implement adapter that wraps existing chess engine without modification
  - Ensure 100% parity with current ChessEnvironment behavior
  - Add comprehensive conversion between engine types and adapter types
  - Validate that all existing tests pass with BuiltinAdapter
  - _Requirements: 2_

- [ ] 2.1 Create BuiltinAdapter Class
  - **Prerequisites**: ChessEngineAdapter interface and data types from Task 1
  - Create `integration/src/commonMain/kotlin/com/chessrl/integration/adapter/BuiltinAdapter.kt`
  - Implement all ChessEngineAdapter methods delegating to existing ChessBoard
  - Add conversion methods between Move/ChessMove and Position types
  - Ensure FEN-based state reconstruction maintains game state accuracy
  - Handle castling rights, en passant, and move counters correctly
  - _Requirements: 2_

- [ ] 2.2 Implement State Conversion Methods
  - **Deliverable**: Robust conversion between ChessBoard and ChessState
  - Create createBoardFromState method using FEN reconstruction
  - Implement convertToChessMove and convertToEngineMove methods
  - Add error handling for invalid FEN strings and malformed states
  - Ensure thread safety through immutable state management
  - _Requirements: 2_

- [ ] 2.3 Add BuiltinAdapter Validation Tests
  - **Deliverable**: Comprehensive test suite validating BuiltinAdapter parity
  - Create `integration/src/commonTest/kotlin/com/chessrl/integration/adapter/BuiltinAdapterTest.kt`
  - Test starting position has exactly 20 legal moves
  - Validate e2e4 move produces correct FEN: "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
  - Test checkmate and stalemate detection in known positions
  - Verify castling, en passant, and promotion handling
  - _Requirements: 2, 8_

- [ ] 3. ChesslibAdapter Implementation
  - **Prerequisites**: Complete Task 2 (BuiltinAdapter working and tested)
  - Add chesslib dependency to integration module build configuration
  - Implement ChesslibAdapter using bhlangonijr/chesslib library
  - Use FEN-based immutable state with Board rehydration pattern
  - Add comprehensive error handling and validation
  - _Requirements: 3, 7_

- [ ] 3.1 Add Chesslib Dependency
  - **Deliverable**: Gradle configuration with chesslib dependency
  - Add `implementation("com.github.bhlangonijr:chesslib:1.3.3")` to integration/build.gradle.kts
  - Verify Apache-2.0 license compatibility and document in README
  - Test that dependency resolves and builds successfully
  - Add dependency documentation with version requirements
  - _Requirements: 7_

- [ ] 3.2 Implement ChesslibAdapter Class
  - **Prerequisites**: Chesslib dependency added and BuiltinAdapter completed
  - Create `integration/src/commonMain/kotlin/com/chessrl/integration/adapter/ChesslibAdapter.kt`
  - Implement all ChessEngineAdapter methods using chesslib Board and MoveGenerator
  - Use Board.loadFromFen() for state reconstruction and Board.doMove() for move application
  - Handle terminal detection: isMated, isStaleMate, isDraw, isRepetition, halfMoveCounter >= 100
  - Add conversion methods between chesslib types and adapter types
  - _Requirements: 3_

- [ ] 3.3 Implement Chesslib Type Conversions
  - **Deliverable**: Robust conversion between chesslib types and adapter types
  - Create convertToChessMove method handling chesslib Move to ChessMove conversion
  - Implement convertToChesslibMove with proper Square and Piece mapping
  - Add promotion handling for all piece types (Queen, Rook, Bishop, Knight)
  - Handle Side.WHITE/BLACK to PieceColor.WHITE/BLACK conversion
  - _Requirements: 3_

- [ ] 3.4 Add ChesslibAdapter Validation Tests
  - **Deliverable**: Test suite validating ChesslibAdapter correctness
  - Create `integration/src/commonTest/kotlin/com/chessrl/integration/adapter/ChesslibAdapterTest.kt`
  - Test same basic positions as BuiltinAdapter (starting position, e2e4, etc.)
  - Validate terminal detection for checkmate, stalemate, draws
  - Test complex positions with castling, en passant, promotions
  - Verify perft functionality for performance testing
  - _Requirements: 3, 8_

- [ ] 4. Engine Selection Mechanism
  - **Prerequisites**: Both BuiltinAdapter and ChesslibAdapter implemented and tested
  - Implement EngineSelector for creating adapters based on configuration
  - Add CLI flag parsing for --engine builtin|chesslib
  - Support engine selection in profile configuration files
  - Ensure builtin remains permanent default for backward compatibility
  - _Requirements: 6_

- [ ] 4.1 Implement EngineSelector Class
  - **Prerequisites**: Both adapters implemented and tested
  - Create `integration/src/commonMain/kotlin/com/chessrl/integration/adapter/EngineSelector.kt`
  - Implement EngineType enumeration with BUILTIN and CHESSLIB values
  - Add createAdapter method that instantiates appropriate adapter
  - Include parseEngineFromArgs and parseEngineFromProfile methods
  - Add validateAdapter method for basic engine functionality testing
  - _Requirements: 6_

- [ ] 4.2 Add CLI Engine Selection Support
  - **Deliverable**: CLI argument parsing for engine selection
  - Modify existing CLI classes to support --engine flag
  - Add engine validation and error messages for invalid engine names
  - Ensure --engine builtin always works as fallback option
  - Update help text to document engine selection options
  - _Requirements: 6_

- [ ] 4.3 Add Profile Engine Configuration
  - **Deliverable**: Profile configuration support for engine selection
  - Update profile YAML parsing to support engine: builtin|chesslib field
  - Ensure profiles default to builtin engine when engine field is omitted
  - Add validation for engine names in profile configuration
  - Update example profiles to demonstrate engine selection
  - _Requirements: 6_

- [ ] 5. EngineBackedEnvironment Implementation
  - **Prerequisites**: Engine selection mechanism completed
  - Create new environment class that delegates to ChessEngineAdapter
  - Maintain same RL interface as existing ChessEnvironment
  - Add adapter-based state encoding and action decoding
  - Ensure reward calculation and game metrics remain consistent
  - _Requirements: 5_

- [ ] 5.1 Implement EngineBackedEnvironment Class
  - **Prerequisites**: Engine selection and both adapters working
  - Create `integration/src/commonMain/kotlin/com/chessrl/integration/EngineBackedEnvironment.kt`
  - Implement Environment<DoubleArray, Int> interface delegating to ChessEngineAdapter
  - Add reset() method using adapter.initialState()
  - Implement step() method with adapter.getLegalMoves() and adapter.applyMove()
  - Maintain same 839-dimensional state encoding as ChessEnvironment
  - _Requirements: 5_

- [ ] 5.2 Add State Encoding Compatibility
  - **Deliverable**: State encoding that works with both engines
  - Create encodeStateForRL method that converts ChessState to DoubleArray
  - Use existing ChessStateEncoder by converting ChessState back to ChessBoard
  - Ensure encoded states are identical between engines for same positions
  - Add validation tests comparing encoded states across engines
  - _Requirements: 5_

- [ ] 5.3 Implement Action Decoding and Move Matching
  - **Deliverable**: Action decoding with legal move validation and promotion handling
  - Add findMatchingMove method that handles promotion disambiguation
  - Implement fallback to queen promotion for pawn promotions
  - Add invalid move handling with appropriate penalty rewards
  - Include comprehensive logging for debugging invalid moves
  - _Requirements: 5, 4_

- [ ] 6. Parity and Correctness Testing
  - **Prerequisites**: EngineBackedEnvironment implemented
  - Create comprehensive test suite comparing both engines
  - Implement cross-engine validation for identical positions
  - Add performance benchmarking and comparison tools
  - Validate that both engines produce equivalent training results
  - _Requirements: 8, 13_

- [ ] 6.1 Implement Engine Parity Testing Framework
  - **Prerequisites**: Both engines working in EngineBackedEnvironment
  - Create `integration/src/commonTest/kotlin/com/chessrl/integration/adapter/EngineParityTester.kt`
  - Implement testBasicParity method comparing starting positions and basic moves
  - Add testComplexPositions method with Italian Game, endgames, and tactical positions
  - Include comprehensive FEN validation and move count comparison
  - Test terminal detection consistency across engines
  - _Requirements: 8, 13_

- [ ] 6.2 Add Performance Benchmarking Suite
  - **Deliverable**: Performance comparison tools for both engines
  - Create `integration/src/commonTest/kotlin/com/chessrl/integration/adapter/EnginePerformanceTester.kt`
  - Implement benchmarkLegalMoveGeneration and benchmarkMoveApplication methods
  - Add memory usage tracking and garbage collection analysis
  - Create performance regression tests to ensure optimization doesn't break functionality
  - _Requirements: 10_

- [ ] 6.3 Create Cross-Engine Training Validation
  - **Deliverable**: Tests validating identical RL training behavior between engines
  - Create test that runs identical DQN training sessions with both engines using same seeds
  - Compare final agent performance against same baseline opponents
  - Validate that game length distributions and outcome ratios are equivalent
  - Test that invalid move counts remain zero for both engines during training
  - _Requirements: 13_

- [ ] 7. Integration with Training Pipeline
  - **Prerequisites**: Parity testing completed successfully
  - Modify existing training pipeline to support engine selection
  - Update TrainingPipeline to use EngineBackedEnvironment when engine is specified
  - Ensure all training modes work with both engines
  - Add engine information to training logs and metrics
  - _Requirements: 12_

- [ ] 7.1 Update TrainingPipeline for Engine Selection
  - **Prerequisites**: EngineBackedEnvironment tested and validated
  - Modify `integration/src/commonMain/kotlin/com/chessrl/integration/TrainingPipeline.kt`
  - Add engine parameter to TrainingPipeline constructor
  - Update environment creation to use EngineBackedEnvironment when engine is specified
  - Maintain backward compatibility with existing ChessEnvironment usage
  - _Requirements: 12_

- [ ] 7.2 Add Engine Information to Logging
  - **Deliverable**: Enhanced logging with engine identification
  - Update training logs to include engine name in all relevant messages
  - Add engine information to training metrics and result JSON files
  - Include engine-specific outcome reasons in evaluation reports
  - Update progress logging to show which engine is being used
  - _Requirements: 12_

- [ ] 7.3 Update All Training Modes
  - **Deliverable**: Engine selection support across all training and evaluation modes
  - Update train-advanced, eval-baseline, eval-h2h, and play-human modes
  - Ensure --engine flag works consistently across all CLI commands
  - Add engine validation at startup with clear error messages
  - Test all modes with both engines to ensure functionality
  - _Requirements: 13_

- [ ] 8. Repetition Detection Implementation
  - **Prerequisites**: Basic engine functionality working
  - Add optional repetition detection for threefold repetition draws
  - Implement efficient position history tracking
  - Map repetition detection to standardized TerminalInfo format
  - Make repetition detection configurable for performance tuning
  - _Requirements: 9_

- [ ] 8.1 Implement Position History Tracking
  - **Prerequisites**: Core adapter functionality working
  - Add history tracking to EngineBackedEnvironment using FEN signatures
  - Implement efficient hash-based repetition detection
  - Use position-only FEN (excluding clocks) for repetition comparison
  - Add configurable repetition threshold (default 3 occurrences)
  - _Requirements: 9_

- [ ] 8.2 Add Repetition Detection to Adapters
  - **Deliverable**: Repetition detection integrated into both adapters
  - Update BuiltinAdapter to use existing game history for repetition detection
  - Enhance ChesslibAdapter to use chesslib's built-in repetition detection
  - Ensure both adapters report "repetition" as termination reason consistently
  - Add tests with known threefold repetition positions
  - _Requirements: 9_

- [ ] 9. Performance Optimization
  - **Prerequisites**: Basic functionality working and tested
  - Optimize FEN parsing and Board reconstruction patterns
  - Add caching for legal moves and frequently accessed states
  - Implement object pooling for high-throughput scenarios
  - Benchmark and optimize critical path performance
  - _Requirements: 10_

- [ ] 9.1 Implement State Caching
  - **Deliverable**: Performance optimization through intelligent caching
  - Add legal move caching to ChessState for repeated access
  - Implement FEN-based state memoization for expensive operations
  - Add cache invalidation and memory management
  - Benchmark performance improvements and validate correctness
  - _Requirements: 10_

- [ ] 9.2 Optimize Board Reconstruction
  - **Deliverable**: Optimized FEN parsing and Board object reuse
  - Investigate Board object pooling for ChesslibAdapter
  - Optimize BuiltinAdapter FEN parsing with caching
  - Add thread-local optimization for concurrent training
  - Ensure thread safety while improving performance
  - _Requirements: 10_

- [ ] 10. Comprehensive Testing and Validation
  - **Prerequisites**: All core functionality implemented
  - Run comprehensive test suite across both engines
  - Validate training effectiveness with extended training runs
  - Test all CLI commands and configuration options
  - Ensure system meets all success criteria
  - _Requirements: 8, 13, 14_

- [ ] 10.1 Extended Training Validation
  - **Deliverable**: Comprehensive training effectiveness validation
  - Run 100+ cycle training sessions with both engines using identical seeds
  - Compare final agent performance against minimax depth-2 baseline
  - Validate win rates >40% for both engines against baseline opponents
  - Document any performance differences and investigate causes
  - _Requirements: 13_

- [ ] 10.2 CLI and Configuration Testing
  - **Deliverable**: Complete validation of all user-facing functionality
  - Test all CLI commands with both --engine builtin and --engine chesslib
  - Validate profile configuration with engine selection
  - Test error handling for invalid engine names and configurations
  - Ensure help text and documentation accurately reflect engine options
  - _Requirements: 6, 15_

- [ ] 10.3 Regression Testing Suite
  - **Deliverable**: Automated regression tests for ongoing validation
  - Create comprehensive regression test suite covering both engines
  - Add CI integration for dual-engine testing
  - Implement automated parity checking between engine implementations
  - Add performance regression detection with acceptable thresholds
  - _Requirements: 8, 13_

- [ ] 11. Documentation and Examples
  - **Prerequisites**: All functionality implemented and tested
  - Update README with engine selection documentation
  - Create comprehensive API documentation for adapter interface
  - Add usage examples and troubleshooting guide
  - Document migration path and best practices
  - _Requirements: 15_

- [ ] 11.1 Update Main Documentation
  - **Deliverable**: Updated README.md with comprehensive engine selection guide
  - Document --engine flag usage with examples
  - Add profile configuration examples with engine selection
  - Include troubleshooting section for common engine issues
  - Update quick start guide to mention engine options
  - _Requirements: 15_

- [ ] 11.2 Create API Documentation
  - **Deliverable**: Comprehensive API documentation for ChessEngineAdapter
  - Document ChessEngineAdapter interface with usage examples
  - Add implementation guide for creating new engine adapters
  - Include performance considerations and best practices
  - Document thread safety requirements and recommendations
  - _Requirements: 15_

- [ ] 11.3 Add Usage Examples and Migration Guide
  - **Deliverable**: Practical examples and migration documentation
  - Create example code showing how to use both engines
  - Add migration guide for switching from ChessEnvironment to EngineBackedEnvironment
  - Include performance tuning guide for different use cases
  - Document testing strategies for validating new engine implementations
  - _Requirements: 15_

## Success Criteria

**Functionality:**
- Both builtin and chesslib engines pass identical test suites
- Starting position generates exactly 20 legal moves for both engines
- e2e4 move produces identical FEN for both engines
- All terminal conditions (checkmate, stalemate, draws) detected consistently

**Performance:**
- ChesslibAdapter performance within 2x of BuiltinAdapter for legal move generation
- Training speed degradation <10% when using chesslib engine
- Memory usage remains within acceptable bounds for both engines

**Integration:**
- All CLI commands work with both --engine builtin and --engine chesslib
- Profile configuration supports engine selection
- Training pipeline produces equivalent results with both engines
- Invalid move count remains zero during training for both engines

**Reliability:**
- Comprehensive error handling prevents crashes from engine failures
- Graceful fallback mechanisms for invalid moves and states
- Thread safety maintained across concurrent training scenarios

**Compatibility:**
- Builtin engine remains permanent default for backward compatibility
- Existing training configurations work without modification
- All existing tests pass with BuiltinAdapter
- No breaking changes to RL training interface

This implementation plan provides a systematic approach to creating a robust, dual-engine chess system that maintains backward compatibility while enabling comprehensive validation and testing of different chess rule implementations.