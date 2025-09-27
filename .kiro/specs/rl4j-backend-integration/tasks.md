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

- [ ] 7. Implement benchmark metrics collection system
  - [ ] 7.1 Create BenchmarkMetricsCollector
    - Collect win/draw/loss rates, loss curves, illegal move counts
    - Track training time per cycle and peak memory usage
    - Ensure identical metrics collection for both backends
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ] 7.2 Add controlled experiment framework
    - Support running identical seeds and profiles for both backends
    - Implement parallel training runs for comparison
    - Add statistical analysis tools for performance differences
    - _Requirements: 4.1_

  - [ ] 7.3 Create data export and visualization tools
    - Export benchmark data to CSV/JSON format
    - Generate side-by-side comparison plots and tables
    - Create reproducible analysis scripts
    - _Requirements: 4.4, Acceptance Test for Requirement 4_

- [ ] 8. Add comprehensive testing and validation
  - [ ] 8.1 Complete unit test suite
    - Test all configuration mapping edge cases
    - Validate MDP wrapper behavior thoroughly
    - Test checkpoint compatibility across formats
    - _Requirements: All requirements validation_

  - [ ] 8.2 Add integration tests for full pipeline
    - Test end-to-end training with both backends
    - Verify benchmark data collection and export
    - Test evaluation pipeline with mixed checkpoint types
    - _Requirements: All acceptance tests_

  - [ ] 8.3 Create benchmark validation script
    - Implement `scripts/benchmark_rl4j_vs_manual.md` or equivalent
    - Run controlled 5 cycles × 20 games comparison
    - Generate side-by-side performance analysis
    - _Requirements: Acceptance Test for Requirement 4_

- [ ] 9. Optional: Extended resource analysis
  - [ ] 9.1 Implement resource scaling experiments
    - Test performance with increased cycles and memory
    - Document scaling behavior for both backends
    - Analyze hardware utilization patterns
    - _Requirements: 5.1, 5.2, 5.3_

  - [ ] 9.2 Generate hardware investment case documentation
    - Create research summary with benchmark data
    - Document compute-bound vs algorithm-bound analysis
    - Provide clear hardware investment recommendations
    - _Requirements: 6.1, 6.2, 6.3, 6.4_