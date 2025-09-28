# Implementation Plan

- [x] 1. Set up RL4J dependencies and basic infrastructure
  - Add RL4J/ND4J dependencies to integration/build.gradle.kts with Gradle property guard
  - Extend NeuralNetworkBackend enum to include RL4J option
  - Add compile-time availability check for RL4J classes
  - _Requirements: 1.3_

- [x] 2. Implement Chess MDP wrapper for RL4J integration
  - [x] 2.1 Create ChessMDP class implementing RL4J MDP interface
    - Implement reset() method returning ChessObservation with 839 features
    - Implement step() method using existing chess environment logic
    - Add illegal action fallback mechanism with logging
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 2.2 Create RL4J-compatible observation and action spaces
    - Implement ChessObservation wrapper for 839-dimensional state vector
    - Create DiscreteSpace(4096) for chess action space
    - Add validation for correct dimensions
    - _Requirements: 1.1, 1.2_

  - [x] 2.3 Add comprehensive unit tests for MDP wrapper
    - Test reset() returns standard starting FEN position
    - Test step(e2e4) updates board and reward correctly
    - Test illegal action triggers fallback path with logging
    - Verify observation and action space dimensions
    - _Requirements: 3.4, Acceptance Test for Requirement 3_

- [x] 3. Implement configuration mapping and validation
  
  - [x] 3.0 Clean up the same from previous task (RL4J-specific)

  - [x] 3.1 Create ConfigurationMapper class
    - Map ChessRLConfig to QLearning.QLConfiguration
    - Map learningRate, batchSize, gamma, targetUpdateFrequency, maxExperienceBuffer, hiddenLayers
    - Ensure identical parameter values for both backends
    - _Requirements: 2.1, 2.2_

  - [x] 3.2 Turn these wrappers into actual RL4J implementations:

    class ChessMDP(
        private val env: ChessEnvironment
    ) : MDP<ChessObservation, Int, DiscreteSpace> {
        override fun reset(): ChessObservation { … }
        override fun step(action: Int): StepReply<ChessObservation> { … }
        override fun getObservationSpace(): ObservationSpace<ChessObservation> {
            return ChessObservationSpace()
        }
        override fun getActionSpace(): DiscreteSpace = ChessActionSpace()
        override fun close() {}
        override fun isDone(): Boolean = doneFlag
        override fun newInstance(): MDP<ChessObservation, Int, DiscreteSpace> =
            ChessMDP(env.clone()) // or rebuild env
    }

    And likewise convert ChessObservationSpace to extend ObservationSpace<ChessObservation> and ChessActionSpace to extend DiscreteSpace. Once those exist, we can hook them into an RL4J QLearning config and prove the integration.

  - [x] 3.3 Add parameter validation with clear error messages
    - Validate learning rate is positive (reject -0.1 with clear error)
    - Validate batch size, gamma range, buffer size constraints
    - Provide actionable error messages for invalid values
    - _Requirements: 2.3, Acceptance Test for Requirement 2_

  - [x] 3.4 Implement configuration consistency verification
    - Print configuration summary at startup for both backends
    - Verify identical hyperparameter values are used
    - Add logging to confirm parameter mapping correctness
    - _Requirements: 2.4_

- [x] 4. Extend CLI to support RL4J backend selection
  - [x] 4.1 Modify ChessRLCLI to handle --nn rl4j flag
    - Extend existing --nn flag parsing to include rl4j option
    - Add NeuralNetworkBackend.RL4J enum value
    - Implement backend selection routing logic
    - _Requirements: 1.3_

  - [x] 4.2 Create BackendFactory for backend instantiation
    - Implement factory pattern for creating Manual vs RL4J backends
    - Handle backend-specific configuration and initialization
    - Add error handling for missing RL4J dependencies
    - _Requirements: 1.3_

  - [x] 4.3 Test CLI integration end-to-end
    - Verify `./gradlew :integration:run --args="--train --nn rl4j ..."` completes one cycle
    - Test backend switching without changing other configuration
    - Validate error handling for invalid backend selection
    - _Requirements: Acceptance Test for Requirement 1_

- [x] 5. Implement RL4J training backend
  - [x] 5.1 Create RL4JBackend class
    - Implement RL4J QLearningDiscreteDense trainer setup
    - Configure DQNFactoryStdDense with mapped parameters
    - Integrate ChessMDP with RL4J training loop
    - _Requirements: 1.1, 1.2_

  - [x] 5.2 Add training session management
    - Implement training cycle execution matching manual backend
    - Add progress tracking and metrics collection
    - Handle training interruption and resumption
    - _Requirements: 4.1, 4.2_

  - [x] 5.3 Test RL4J training pipeline
    - Run complete training cycle without errors
    - Verify training metrics are collected correctly
    - Test training can be stopped and resumed
    - _Requirements: Acceptance Test for Requirement 1_

- [x] 6. Implement checkpoint compatibility system

  - [x] 6.0 Verify that we already have:
    - working RL4Jbackend
    - working session management
    - RL4J can be used end‑to‑end with training pipeline

  - [x] 6.1 Create UnifiedCheckpointManager
    - Handle saving RL4J models with metadata
    - Support loading both manual (.json) and RL4J (.zip) formats
    - Add checkpoint format detection and validation
    - _Requirements: 1.4_

  - [x] 6.2 Ensure BaselineEvaluator compatibility
    - Modify BaselineEvaluator to load RL4J checkpoints
    - Test evaluation pipeline with RL4J-trained models
    - Verify identical evaluation methodology for both backends
    - _Requirements: 1.4_

  - [x] 6.3 Test checkpoint round-trip functionality
    - Save RL4J checkpoint and reload for evaluation
    - Verify RL4J checkpoint loads and plays 20 evaluation games
    - Test checkpoint metadata handling and format detection
    - _Requirements: Acceptance Test for Requirement 1_

                - [x] 7.§ x312  q Implement real RL4J API calls in RL4JChessAgent
                  - [x] 7.1 Replace reflection-based stubs with direct RL4J calls
                    - Remove TODO comments from selectActionWithRealRL4J method
                    - Implement direct calls to RL4J policy.play() or equivalent
                    - Replace saveWithRealRL4J placeholder with actual RL4J model saving
                    - Remove createRealQLearningDiscreteDense fallback logic
                    - _Requirements: 1.1, 1.2, 1.3, 1.4_

                  - [x] 7.2 Implement real RL4J training integrationk
                    - Replace trainBatch simulation with actual RL4J trainer calls
                    - Use RL4J's built-in experience replay instead of custom logic
                    - Return genuine RL4J loss values and training statistics
                    - Integrate with RL4J's target network update mechanisms
                    - _Requirements: 3.1, 3.2, 3.3, 3.4_

                  - [x] 7.3 Test RL4J API integration
                    - Verify all RL4J methods are called directly when available
                    - Test training produces real RL4J loss curves and metrics
                    - Validate model saving/loading uses RL4J persistence
                    - Confirm no placeholder logic remains in production paths
    - _Requirements: 1.4, 3.3_

- [x] 8. Fix MDP bridge to use proper RL4J types
  - [x] 8.1 Implement proper RL4J interface inheritance
    - Make ChessObservationSpace implement ObservationSpace<Observation>
    - Make ChessActionSpace extend DiscreteSpace directly
    - Remove wrapper objects and use RL4J types throughout
    - _Requirements: 2.1, 2.2_

  - [x] 8.2 Fix ChessMDP.step() return types
    - Return org.deeplearning4j.gym.StepReply instead of wrapper
    - Ensure ChessMDP implements MDP<Observation,Int,DiscreteSpace>
    - Remove toRL4JMDP() conversion method - use direct implementation
    - _Requirements: 2.3, 2.4_

  - [x] 8.3 Test MDP bridge with real RL4J
    - Verify ChessMDP can be used directly with QLearningDiscreteDense
    - Test observation and action spaces work with RL4J trainers
    - Validate no type conversion errors occur during training
    - _Requirements: 2.4_

- [ ] 9. Enable RL4J runtime testing
  - [ ] 9.1 Create tests that run with RL4J on classpath
    - Add integration test that requires RL4JAvailability.isAvailable() == true
    - Use @EnabledIf or similar to conditionally run RL4J tests
    - Test actual RL4J training/saving/loading cycle end-to-end
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ] 9.2 Add CI configuration for RL4J testing
    - Configure Gradle properties to enable RL4J dependencies in CI
    - Ensure RL4J tests run in appropriate CI environments
    - Add documentation for running RL4J tests locally
    - _Requirements: 4.4_

  - [ ] 9.3 Verify real RL4J behavior in tests
    - Test that RL4J training produces different results than manual backend
    - Verify RL4J model persistence and loading works correctly
    - Validate RL4J-specific configuration options take effect
    - _Requirements: 4.3_

- [ ] 10. Fix CLI backend factory integration
  - [x] 10.1 Update ChessRLCLI.handleTrain() method
    - Replace direct DqnLearningBackend instantiation with BackendFactory call
    - Pass selectedBackend parameter to BackendFactory.createBackend()
    - Remove hardcoded backend selection logic
    - _Requirements: 5.1, 5.2_

  - [x] 10.2 Ensure BackendFactory routes correctly
    - Verify BackendFactory.createBackend() respects backend parameter
    - Remove manual fallback when RL4J backend is requested
    - Test that --nn rl4j creates RL4JLearningBackend instance
    - _Requirements: 5.3, 5.4_

  - [x] 10.3 Test CLI integration end-to-end
    - Verify --nn rl4j flag actually uses RL4J implementation
    - Test that training runs through RL4J backend when selected
    - Validate CLI error handling for unavailable backends
    - _Requirements: 5.4_