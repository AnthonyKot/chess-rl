# Requirements Document

  ## Introduction

  This feature delivers a production-quality RL4J backend that can be compared head-to-head with our manual DQN implementation. The objective is to run RL4J end-to-end on the existing chess environment, collect authentic metrics, and decide whether we’re
  compute- or algorithm-bound.

  ## Core Scope: Deliver an RL4J Backend Prototype

  Goal: Run RL4J training end-to-end on the chess environment and produce comparable metrics to the manual backend.

  ## Requirements

  ### Requirement 1: Real RL4J API Integration

  User Story: As a chess RL researcher, I need the RL4J backend to call the actual RL4J APIs so the results reflect genuine RL4J performance.

  #### Acceptance Criteria

  1. WHEN RL4JChessAgent selects actions THEN it SHALL invoke real RL4J policy methods (no reflection stubs when RL4J is present).
  2. WHEN RL4JChessAgent saves or loads checkpoints THEN it SHALL call RL4J’s persistence APIs directly.
  3. WHEN RL4JLearningBackend trains THEN it SHALL use RL4J’s trainer entrypoints instead of a simulated loop.
  4. WHEN RL4J classes are available THEN ALL core agent methods SHALL bypass reflection fallback paths.

  Acceptance Test: Code inspection confirms no TODOs or reflection branches in the hot path once RL4J is on the classpath.

  ### Requirement 2: Proper MDP Bridge Implementation

  User Story: As a developer, I want ChessMDP and its helpers to match RL4J’s expected types so the framework can consume our environment without adapters.

  #### Acceptance Criteria

  1. WHEN ChessObservationSpace is instantiated THEN it SHALL implement ObservationSpace<ChessObservation>.
  2. WHEN ChessActionSpace is instantiated THEN it SHALL extend RL4J’s DiscreteSpace.
  3. WHEN ChessMDP.step() is executed THEN it SHALL return org.deeplearning4j.gym.StepReply<ChessObservation> populated with real observations.
  4. WHEN ChessMDP is registered with RL4J THEN it SHALL implement MDP<ChessObservation, Int, DiscreteSpace> directly—no wrapper conversion required.

  Acceptance Test: QLearningDiscreteDense<ChessObservation> can consume ChessMDP without any type adapters or reflection.

  ### Requirement 3: Real RL4J Training Loop Integration

  User Story: As a developer, I want the RL4J backend to run RL4J’s own training loop so we get authentic replay, target updates, and metrics.

  #### Acceptance Criteria

  1. WHEN RL4JLearningBackend performs training THEN it SHALL trigger RL4J’s training cycle (e.g., learn() / epoch loop), not a manual experience pump.
  2. WHEN replay memory is used THEN it SHALL rely on RL4J’s experience replay implementation (no manual store(obs, action, …) hacks).
  3. WHEN training completes THEN reported loss, epsilon, and score SHALL originate from RL4J’s training info objects.
  4. WHEN configuration objects are built THEN they SHALL use RL4J’s builders (QLearning.QLConfiguration, DQNFactoryStdDense.Configuration) with fields populated from ChessAgentConfig.

  Acceptance Test: Training-loop inspection shows RL4J trainer calls and the resulting metrics trace back to RL4J info objects.

  ### Requirement 4: RL4J Runtime Verification

  User Story: As a developer, I want automated tests that run with RL4J enabled so we can prove the integration works on real RL4J runtimes.

  #### Acceptance Criteria

  1. WHEN RL4J-specific tests run THEN they SHALL execute with RL4JAvailability.isAvailable() returning true.
  2. WHEN integration tests execute THEN they SHALL instantiate real RL4J trainers/policies (no skipping or short-circuiting).
  3. WHEN tests finish THEN they SHALL confirm training, saving, loading, and inference all succeed with RL4J artifacts.
  4. WHEN CI runs THEN RL4J tests SHALL be conditionally enabled only when RL4J dependencies are present (e.g., Gradle property).

  Acceptance Test: At least one integration test exercises a full RL4J training + checkpoint + reload cycle using real RL4J classes.

  ### Requirement 5: CLI Backend Factory Integration

  User Story: As a user, I want the CLI to honor --nn rl4j so that selecting RL4J actually runs the RL4J backend.

  #### Acceptance Criteria

  1. WHEN the CLI receives --nn rl4j THEN it SHALL route through BackendFactory.createBackend() instead of constructing DqnLearningBackend manually.
  2. WHEN BackendFactory handles BackendType.RL4J THEN it SHALL return an RL4JLearningBackend instance.
  3. WHEN configuration is resolved THEN the chosen backend type SHALL propagate to all training/evaluation commands unchanged.
  4. WHEN RL4J backend is active THEN the entire training pipeline SHALL use the RL4J implementation (no silent fallback to manual DQN).

  Acceptance Test: Running the CLI with --nn rl4j builds RL4JLearningBackend, runs training with real RL4J components, and emits RL4J checkpoints.