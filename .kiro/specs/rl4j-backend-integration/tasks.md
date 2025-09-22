# Implementation Plan

- [ ] 1. Add CLI flag support and backend routing
  - Add `--rl manual|rl4j` flag parsing to ChessRLCLI argument handling
  - Update help text to document the new RL backend selection flag
  - Pass backend selection parameter to ChessAgentFactory methods
  - Add validation to ensure only valid backend types are accepted
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 2. Add RL4J dependencies and create stub interfaces
  - Add RL4J dependency `org.deeplearning4j:rl4j-core:1.0.0-M2.1` to build.gradle.kts
  - Create ChessObservation class that wraps DoubleArray in RL4J observation format
  - Create ChessActionSpace class extending DiscreteSpace with 4096 actions
  - Create ChessObservationSpace class for 839-dimensional state encoding
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ] 3. Implement ChessMDP wrapper for RL4J integration
  - Create ChessMDP class implementing MDP<ChessObservation, Int, DiscreteSpace>
  - Implement reset() method to initialize chess position and return encoded observation
  - Implement step(action) method with move application and reward computation
  - Implement isDone() method using existing game outcome detection logic
  - Add getObservationSpace() and getActionSpace() methods with correct dimensions
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 4. Create RL4J configuration mapping system
  - Create RL4JConfigMapper class to translate ChessRLConfig to RL4J configurations
  - Implement mapping from learningRate, batchSize, gamma to QLearningConfiguration
  - Implement mapping from maxExperienceBuffer, targetUpdateFrequency to RL4J parameters
  - Create DQNFactoryStdDense configuration with appropriate network architecture
  - Add configuration validation and error handling for invalid parameter ranges
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 5. Implement RL4J backend in learning system
  - Create Rl4jLearningBackend class implementing LearningBackend interface
  - Create Rl4jLearningSession class implementing LearningSession interface
  - Implement QLearningDiscreteDense trainer creation with chess MDP
  - Add trainOnBatch() method that delegates to RL4J training mechanisms
  - Implement updateOpponent() method for opponent policy synchronization
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [ ] 6. Add backend selection to ChessAgentFactory
  - Modify createDQNAgent() method to accept rlBackend parameter
  - Add routing logic to select between manual and RL4J agent creation
  - Implement createRL4JAgent() method that uses RL4J backend
  - Ensure backward compatibility by defaulting to manual backend
  - Add error handling for unknown backend types
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 7. Implement action masking system - Phase 1 (Fallback)
  - Add illegal action detection in ChessMDP.step() method
  - Implement legal action fallback selection using Q-value ranking
  - Add logging for illegal action attempts and fallback selections
  - Ensure zero illegal moves reach the chess engine during training
  - Add configuration flag to enable/disable fallback mode for debugging
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 8. Implement action masking system - Phase 2 (Policy Masking)
  - Create custom policy wrapper that masks illegal actions before selection
  - Implement Q-value masking by setting illegal actions to negative infinity
  - Integrate masking with RL4J's epsilon-greedy action selection
  - Add performance optimization to minimize masking overhead
  - Validate that only legal actions are selected during training and evaluation
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 9. Implement RL4J checkpoint management
  - Create RL4JCheckpointManager class for model persistence
  - Implement saveCheckpoint() method that saves RL4J models as .zip files
  - Add metadata generation including backend type, performance metrics, and model info
  - Implement model loading that supports both .json (manual) and .zip (RL4J) formats
  - Integrate with existing best model selection and promotion logic
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 10. Update CLI integration and profile mapping
  - Integrate --rl flag with existing profile and configuration system
  - Ensure RL4J backend works with all existing profile configurations
  - Add RL4J-specific parameter validation in configuration parsing
  - Update configuration summary logging to include backend selection
  - Test CLI flag interaction with --profile and --config options
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 4.1, 4.2, 4.3_

- [ ] 11. Ensure evaluation system compatibility
  - Verify RL4J-trained models work with existing BaselineEvaluator
  - Test head-to-head evaluation between manual and RL4J trained models
  - Ensure identical JSON output format for evaluation results
  - Validate that evaluation metrics are consistent across backends
  - Test model loading in evaluation mode for both backend types
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 12. Create comprehensive unit tests for MDP wrapper
  - Write tests for ChessMDP.reset() returning valid initial observations
  - Write tests for ChessMDP.step() handling both legal and illegal actions correctly
  - Write tests for ChessMDP.isDone() detecting all terminal game states
  - Write tests verifying observation space dimensions (839) and action space (4096)
  - Write tests for reward computation consistency with existing reward logic
  - _Requirements: 8.1_

- [ ] 13. Create unit tests for action masking system
  - Write tests verifying illegal actions are properly masked or handled via fallback
  - Write tests ensuring only legal actions are selected during policy execution
  - Write tests for masking performance impact and optimization effectiveness
  - Write tests for edge cases like positions with very few legal moves
  - Write tests validating masking works correctly with different exploration rates
  - _Requirements: 8.2_

- [ ] 14. Create integration tests for RL4J backend
  - Write tests for complete RL4J training cycles producing valid metrics
  - Write tests for checkpoint saving and loading round-trip functionality
  - Write tests for configuration mapping accuracy between ChessRLConfig and RL4J
  - Write tests for backend switching via CLI flag with different configurations
  - Write tests for error handling when RL4J dependencies are unavailable
  - _Requirements: 8.3_

- [ ] 15. Create end-to-end validation tests
  - Write tests for short RL4J training runs producing non-NaN metrics and convergence
  - Write tests for evaluation JSON output consistency between backends
  - Write tests for head-to-head matches between manual and RL4J trained models
  - Write tests for deterministic training behavior with identical seeds
  - Write tests for long-running training stability and checkpoint integrity
  - _Requirements: 8.4_