# Implementation Plan

- [ ] 1. Add stack selection CLI flags and configuration resolution
  - Add `--stack legacy|framework` flag parsing to ChessRLCLI argument handling
  - Add component override flags `--engine builtin|chesslib`, `--nn manual|dl4j`, `--rl manual|rl4j`
  - Create StackType, EngineType, NeuralNetworkType, and RLType enums
  - Implement IntegrationConfig.resolve() method with stack defaults and override logic
  - Update CLI help text to document new stack selection and component override flags
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 2. Create chess engine adapter interface and builtin implementation
  - Define ChessEngineAdapter interface with getLegalMoves, applyMove, getGameOutcome, isValidMove methods
  - Create ChessMove data class with from, to, and optional promotion fields
  - Create GameOutcome sealed class with WhiteWins, BlackWins, and Draw variants
  - Implement BuiltinEngineAdapter that wraps existing chess engine implementation
  - Write unit tests for BuiltinEngineAdapter covering legal moves, move application, and outcome detection
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 3. Add framework dependencies and create chesslib engine adapter
  - Add chesslib dependency to integration module build.gradle.kts
  - Implement ChesslibEngineAdapter using chesslib for move generation and validation
  - Add FEN parsing and game state management using chesslib APIs
  - Implement game outcome detection (checkmate, stalemate, 50-move rule) with chesslib
  - Write unit tests comparing BuiltinEngineAdapter and ChesslibEngineAdapter for consistency
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 4. Create observation encoder and action adapter interfaces
  - Define ObservationEncoder interface with encode(fen: String): DoubleArray method
  - Implement existing 839-dimensional encoding logic in ObservationEncoderImpl
  - Define ActionAdapter interface with moveToActionId, actionIdToMove, getLegalActionIds methods
  - Implement ActionAdapterImpl with 4096-dimensional action space mapping
  - Write unit tests verifying observation size (839) and action space size (4096) consistency
  - _Requirements: 4.5, 5.4_

- [ ] 5. Implement backend factory system with legacy factory
  - Define BackendFactory interface with createTrainer and createEvaluator methods
  - Define Trainer and Evaluator interfaces with train, evaluateBaseline, evaluateHeadToHead methods
  - Create TrainingReport, EvaluationReport, and H2HReport data classes
  - Implement LegacyBackendFactory that creates trainers/evaluators using existing DQN implementation
  - Wire LegacyBackendFactory to wrap current agent and environment in new interfaces
  - _Requirements: 3.1, 3.2, 3.3, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 6. Add DL4J dependencies and create neural network wrapper
  - Add DL4J dependencies (deeplearning4j-core, nd4j-native-platform) to build.gradle.kts
  - Create DL4JNeuralNetwork class that wraps MultiLayerNetwork for chess RL
  - Implement network architecture: 839 input → hidden layers → 4096 output
  - Create configuration mapping from existing NN parameters to DL4J MultiLayerConfiguration
  - Write unit tests for DL4J network creation, forward pass, and parameter mapping
  - _Requirements: 3.1, 3.2, 3.3, 9.2_

- [ ] 7. Add RL4J dependencies and create MDP wrapper
  - Add RL4J dependencies (rl4j-core, rl4j-api) to build.gradle.kts
  - Create ChessMDP class implementing MDP<ChessObservation, Int, DiscreteSpace>
  - Implement reset() method to initialize chess position and return encoded observation
  - Implement step(action) method with move application, reward computation, and terminal detection
  - Implement isDone() method using chess engine adapter for game outcome detection
  - _Requirements: 3.1, 3.2, 3.3, 9.3_

- [ ] 8. Create RL4J configuration mapping and trainer implementation
  - Create RL4JConfigMapper to translate ChessRLConfig to QLearningConfiguration
  - Map learningRate, batchSize, gamma, maxExperienceBuffer, targetUpdateFrequency to RL4J parameters
  - Create RL4JTrainer class implementing Trainer interface using QLearningDiscreteDense
  - Implement train() method that runs RL4J training cycles and returns TrainingReport
  - Write unit tests for configuration mapping accuracy and trainer initialization
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 9. Implement action masking system - Phase 1 (Fallback)
  - Add illegal action detection in ChessMDP.step() method
  - Implement legal action fallback selection using highest Q-value among legal moves
  - Add logging for illegal action attempts and fallback selections with statistics
  - Ensure zero illegal moves reach the chess engine during training
  - Write unit tests for fallback behavior with various illegal action scenarios
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 10. Implement action masking system - Phase 2 (Policy Masking)
  - Create MaskedPolicy wrapper class that masks illegal actions before selection
  - Implement Q-value masking by setting illegal actions to Double.NEGATIVE_INFINITY
  - Integrate masking with RL4J's epsilon-greedy action selection mechanism
  - Add performance optimization to minimize masking overhead during training
  - Write unit tests validating that only legal actions are selected during policy execution
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 11. Create framework backend factory and trainer integration
  - Implement FrameworkBackendFactory that creates RL4J-based trainers and evaluators
  - Create FrameworkTrainer that uses ChessMDP, DL4J networks, and RL4J algorithms
  - Implement FrameworkEvaluator that loads RL4J models for baseline and head-to-head evaluation
  - Wire framework components together with proper action masking and configuration
  - Write integration tests for complete framework stack training cycles
  - _Requirements: 3.1, 3.2, 3.3, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 12. Implement unified checkpoint management system
  - Create CheckpointManagerAdapter that handles both .json and .zip model formats
  - Implement saveCheckpoint() method that saves appropriate format based on neural network type
  - Implement loadCheckpoint() method with automatic format detection and validation
  - Create CheckpointMetadata data class with stack type, component versions, and performance metrics
  - Integrate with existing best model selection and promotion logic across both formats
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 13. Update CLI integration with backend factory routing
  - Modify ChessRLCLI to use IntegrationConfig for component selection
  - Implement backend factory selection based on resolved stack configuration
  - Route training, evaluation, and interactive play commands through appropriate factories
  - Ensure CLI maintains identical user experience regardless of stack selection
  - Add configuration summary logging that shows resolved stack and component choices
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 8.1, 8.2_

- [ ] 14. Implement profile integration and parameter mapping
  - Update profile parsing to support stack selection and component override keys
  - Create parameter mapping logic that translates profile values to framework configurations
  - Ensure existing profiles work with both legacy and framework stacks
  - Add validation for framework-specific parameter ranges and compatibility
  - Write tests for profile parameter mapping accuracy across different stack configurations
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 15. Add comprehensive observability and logging
  - Implement startup logging that shows selected stack and resolved component choices
  - Add component initialization logging with backend names and versions
  - Implement action masking statistics logging during training
  - Add checkpoint format and metadata logging during save/load operations
  - Create stack-aware error messages that provide component context for debugging
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 16. Create unit tests for engine adapters and configuration
  - Write tests for ChessEngineAdapter interface compliance across both implementations
  - Write tests for legal move generation consistency between builtin and chesslib engines
  - Write tests for FEN parsing, move application, and game outcome detection accuracy
  - Write tests for IntegrationConfig resolution with various flag combinations
  - Write tests for configuration validation and error handling with invalid parameters
  - _Requirements: 10.1_

- [ ] 17. Create unit tests for action masking and MDP wrapper
  - Write tests for ChessMDP reset(), step(), and isDone() method correctness
  - Write tests for action masking ensuring only legal actions are selected
  - Write tests for illegal action fallback behavior and logging
  - Write tests for observation encoding consistency (839 dimensions) across stacks
  - Write tests for action space mapping consistency (4096 dimensions) across stacks
  - _Requirements: 10.1_

- [ ] 18. Create integration tests for framework stack components
  - Write tests for complete RL4J training cycles producing valid metrics and convergence
  - Write tests for DL4J neural network training and inference with chess observations
  - Write tests for RL4J MDP integration with chess engine adapters
  - Write tests for configuration mapping between ChessRLConfig and framework configurations
  - Write tests for framework component initialization and error handling
  - _Requirements: 10.2_

- [ ] 19. Create end-to-end validation tests for both stacks
  - Write tests for training pipeline execution with both legacy and framework stacks
  - Write tests for evaluation JSON output consistency across different stack configurations
  - Write tests for head-to-head matches between models trained with different stacks
  - Write tests for checkpoint save/load round-trip functionality for both formats
  - Write tests for CLI flag combinations and stack selection routing
  - _Requirements: 10.2, 10.3, 10.4_

- [ ] 20. Create performance and soak tests
  - Write tests measuring training throughput (games/second) comparison between stacks
  - Write tests for memory usage analysis during long training sessions
  - Write tests for action selection latency with and without masking
  - Write tests for checkpoint integrity and model loading performance
  - Write tests for long-running training stability and error recovery
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 10.5_