# Implementation Plan

  - [ ] 1. Replace reflection builders with native RL4J/DL4J configuration
      - Create RL4JConfigurationMapper that maps ChessAgentConfig into QLearning.QLConfiguration and DQNFactoryStdDense.Configuration
      - Populate all supported hyperparameters (learning rate, batch size, epsilon schedule, hidden layers, etc.) with defaults for fields we don’t track yet
      - Remove reflection-based configuration instantiation from RL4JChessAgent / BackendAwareChessAgentFactory
      - Add validation/error messages when mapped values fall outside RL4J expectations
      - Requirements: 1.1, 1.2, 1.3, 4.3
  - [ ] 2. Instantiate RL4J trainer via public APIs and let it manage replay
      - Construct QLearningDiscreteDense directly with ChessMDP, IDQN, and the mapped configs
      - Remove manual experience routing; rely on RL4J’s internal ReplayMemory and sampling (learn() / epoch loop)
      - Delete all reflection calls to trainStep() or buffer store(...) helpers
      - Ensure RL4JLearningBackend delegates training to qLearning.learn() (or equivalent public loop)
      - Requirements: 1.2, 1.3
  - [ ] 3. Remove mock policy fallback and expose real policy
      - Delete mock/random policy implementation and associated reflection fallbacks
      - Ensure RL4JChessAgent always holds a DQNPolicy<ChessObservation> for nextAction, save, and load
      - Validate RL4J availability up front (RL4JAvailability.validate()); fail fast with a clear message if dependencies are missing
      - Requirements: 1.4, 5.4
  - [ ] 4. Surface RL4J metrics through TrainingListener
      - Attach a TrainingListener (or TrainingListenerList) to QLearningDiscreteDense to capture rewards, epsilon, and step counts
      - Replace manual metric bookkeeping with values drawn from RL4J’s TrainingResult / QLearningStatEntry
      - Update logging/PolicyUpdateResult to use those native metrics; drop any unused fields like lastTrainingLoss
      - Requirements: 1.5, 7.3
  - [ ] 5. Maintain ChessMDP legality safeguards with logging
      - Keep illegal-action fallback logic in ChessMDP.step() and enrich the StepReply info map
      - Add per-episode counters/logging so we can confirm illegal actions never reach the engine
      - (Optional) Expose a legal-action mask in ChessObservation for future policy-level masking experiments
      - Requirements: 3.1, 3.2, 3.3, 3.4
  - [ ] 6. Add RL4J-gated tests and usage documentation
      - Restore/author integration tests that run only when enableRL4J=true, covering initialization, a short learn() run, and checkpoint save/load
      - Ensure tests assert the absence of reflection fallbacks (e.g., by checking logs or configuration flags)
      - Document local setup: JDK requirement, Gradle flag, and commands for running the RL4J suite
      - Requirements: 4.1, 4.2, 4.3, 4.4, 6.2
  - [ ] 7. Expand logging around backend creation and training
      - Log backend type, RL4J/DL4J versions, and resolved hyperparameters when the RL4J backend is instantiated
      - Emit replay/episode metrics sourced from the TrainingListener so it’s obvious RL4J is driving the loop
      - Verify there are no remaining “reflection fallback” log lines in normal operation
      - Requirements: 5.4, 7.1, 7.2, 7.4

  ———

  ### Future Backlog (track separately)

  - Policy-level masking wrapper for RL4J policies (once core path is stable)
  - --stack legacy/framework presets and profile integration
  - CI job that builds with enableRL4J=true and runs the gated test suite
  - README/guide refresh describing presets, masking options, and troubleshooting