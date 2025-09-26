# Implementation Plan

- [x] 1. Backend Flag and Factory Routing Infrastructure
  - Add CLI flag parsing for --nn manual|dl4j|kotlindl with manual as default
  - Extend ChessAgentFactory to route based on backend type while preserving existing manual workflow
  - Implement BackendType enum and BackendSelector with graceful fallback to manual backend
  - _Requirements: 1.1, 1.4, 1.5_

- [x] 2. Core Network Adapter Interface and Manual Implementation
  - Define NetworkAdapter interface that implements NeuralNetwork, TrainableNeuralNetwork, and SynchronizableNetwork
  - Create ManualNetworkAdapter that wraps existing FeedforwardNetwork without modification
  - Implement BackendConfig data class for network architecture configuration
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. DL4J Dependencies and Skeleton Implementation
  - Add DL4J dependencies (deeplearning4j-core, nd4j-native-platform) to build.gradle.kts
  - Create Dl4jNetworkAdapter skeleton with MultiLayerNetwork initialization
  - Implement forward pass with 839→[hidden]→4096 architecture using ReLU activation and identity output
  - _Requirements: 9.1, 9.2, 2.1, 2.2_

- [x] 4. DL4J Training and Optimization Implementation
  - Document how you map DoubleArray/FloatArray into INDArray, making sure masking, value/policy heads, and batching stay consistent. Call out precision (DL4J defaults to float32)
  - Implement trainBatch method with INDArray conversion and DataSet training
  - Add Adam optimizer configuration with learning rate and L2 regularization
  - Implement Huber loss function for DQN compatibility with gradient clipping
  - _Requirements: 2.3, 7.2, 7.4_

- [x] 5. DL4J Weight Synchronization and Persistence
  - Check Checkpoint compatibility: current JSON checkpoints must still load when --nn manual is used. Spell out conversion rules (e.g. manual can’t load .zip, DL4J can’t load vanilla JSON) and how you surface a helpful error
  - Implement copyWeightsTo method for target network synchronization using params() and updater state
  - Add save/load methods using ModelSerializer with updater state preservation (ZIP format)
  - Implement parameter count and memory usage estimation methods
  - _Requirements: 2.4, 2.5, 6.1, 6.2_

- [x] 6. Backend-Aware Agent Factory Integration
  - Create BackendAwareChessAgentFactory with createDQNAgent method that routes based on BackendType
  - Implement separate agent creation paths for manual and DL4J backends
  - Ensure identical DQN algorithm configuration across all backends
  - _Requirements: 3.1, 3.2, 3.3_

- [x] 7. Save/Load Compatibility and Checkpoint Resolution
  - Implement checkpoint file format detection (.json for manual, .zip for DL4J)
  - Update checkpoint loading logic to detect and route to appropriate backend based on file extension
  - Add save/load round-trip validation tests for both backends
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 8. Backend Validation and Error Handling
  - Implement comprehensive adapter validation with forward pass dimension checks and finite value validation
  - Add SafeNetworkAdapter wrapper with fallback error handling for production use
  - Create gradient validation utilities for debugging training issues
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 9. Synthetic Task Comparison Framework
  - Implement SyntheticTaskComparison class with XOR learning, sine regression, and chess pattern recognition tasks
  - Create performance benchmarking for inference and training speed comparison
  - Add comprehensive comparison report generation with accuracy, speed, and convergence metrics
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 10. Backend Parity Testing Suite
  - Create BackendParityTester for cross-backend validation with forward pass and training parity checks
  - Implement numerical tolerance testing for identical behavior verification
  - Add integration tests ensuring DQN training produces identical results across backends
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 11. Profile Configuration Integration
  - Add nnBackend configuration support to profile YAML files
  - Implement BackendConfigLoader for parsing network architecture from profiles
  - Add backend-specific configuration sections (dl4jConfig, kotlindlConfig)
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 12. CLI Integration and Help Documentation
  - Update CLI argument parsing to include --nn flag with help text
  - Integrate backend selection with existing profile loading and command-line argument precedence
  - Add backend information to training logs and output for debugging
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 13. Unit Tests for Core Adapter Functionality
  - Write unit tests for ManualNetworkAdapter forward pass, training, and weight synchronization
  - Create unit tests for Dl4jNetworkAdapter with synthetic data and loss validation
  - Add tests for BackendSelector factory methods and validation logic
  - _Requirements: 10.1, 10.2_

- [ ] 14. Integration Tests for DQN Training Pipeline
  - Create integration test that runs small DQN training loop with --nn dl4j flag
  - Verify action masking logs appear and no invalid moves are generated
  - Test target network synchronization logs and training metrics consistency
  - _Requirements: 10.4, 3.4, 3.5_

- [ ] 15. Performance Benchmarking and Optimization
  - Implement BackendBenchmark class for inference and training speed measurement
  - Add memory usage profiling and parameter count comparison between backends
  - Create performance regression tests to ensure no significant slowdown in manual backend
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 16. Documentation and Migration Guide
  - Update README.md and DQN.md with backend selection instructions and troubleshooting
  - Create migration guide for switching between backends and interpreting performance results
  - Document backend-specific configuration options and optimization recommendations
  - _Requirements: 1.1, 7.5, 4.5_

- [ ] 17. KotlinDL Backend Implementation (Optional)
  - Add KotlinDL dependencies and create KotlinDlNetworkAdapter skeleton
  - Implement Sequential model with Dense layers and Adam optimizer
  - Add KotlinDL-specific save/load using H5 or SavedModel format
  - _Requirements: 1.3, 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ] 18. End-to-End Validation and Rollout Preparation
  - Run comprehensive evaluation tests (--eval-baseline, --eval-h2h) with --nn dl4j
  - Verify zero invalid moves and consistent draw reason reporting across backends
  - Validate that existing profiles and training workflows work unchanged with manual backend
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_