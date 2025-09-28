# Design Document

  ## Overview

  We are upgrading the RL4J backend from a reflection-heavy shim to a first-class integration. The manual (“legacy”) pipeline keeps its bespoke replay buffer and DQN updates; the RL4J pipeline will instead rely on RL4J’s native builders, lifecycle, and
  metrics. No reflection, no mock policies, no hand-built experience plumbing.

  Key focus areas:

  1. Instantiate RL4J/DL4J with their public builders.
  2. Let RL4J control experience replay and training loops.
  3. Keep legality guarantees via the environment (optional policy masking later).
  4. Harmonize configuration via a dedicated mapper.
  5. Surface metrics/logging straight from RL4J handlers.

  ———

  ## Architecture

  ### RL4J First-Class Backend

  graph TB
      CLI[Chess RL CLI] --> ConfigMapper[Config Resolver]
      ConfigMapper --> BackendFactory[Backend Factory]

      subgraph LegacyPipeline
          LegacyTrainer[Manual DQN Trainer]
          LegacyReplay[Manual Replay Buffer]
          LegacyPolicy[Manual Policy]
          LegacyTrainer --> LegacyReplay --> LegacyPolicy
      end

      subgraph RL4JPipeline
          RL4JConfig[RL4J + DL4J Builders]
          RL4JTrainer[QLearningDiscreteDense]
          RL4JReplay[RL4J ReplayMemory]
          RL4JPolicy[DQNPolicy]
          RL4JConfig --> RL4JTrainer --> RL4JReplay --> RL4JPolicy
      end

      BackendFactory -->|--nn manual| LegacyTrainer
      BackendFactory -->|--nn rl4j| RL4JConfig

      subgraph Improvements
          NativeBuilders[Native builders only]
          NativeLifecycle[Native training loop]
          NoReflection[No reflection/mocks]
      end

  ### Native Initialization Flow

  flowchart TD
      start([--nn rl4j]) --> resolve[Resolve ChessAgentConfig]
      resolve --> mapConfig[Map to RL4J + DL4J builders]
      mapConfig --> buildDQN[Build IDQN via DQNFactoryStdDense]
      buildDQN --> createMDP[Instantiate ChessMDP]
      createMDP --> createTrainer[Create QLearningDiscreteDense]
      createTrainer --> attachListeners[Attach RL4J TrainingListeners]
      attachListeners --> runTraining[Call qLearning.learn()]

  ### Training Lifecycle & Replay

  graph LR
      ChessMDP -.step().-> QLearning
      QLearning -->|collect| RL4JReplay[ReplayMemory]
      RL4JReplay -->|sample| QLearning
      subgraph ManagedByRL4J
          RL4JReplay
          QLearning
      end
      ManualReplay -.bypassed.-> X

  The environment remains responsible for legal-move fallbacks: if RL4J selects an illegal action, ChessMDP.step() picks a legal substitute, logs it, and proceeds.

  ———

  ## Components

  ### 1. RL4JConfigurationMapper

  Maps ChessAgentConfig fields to RL4J and DL4J configuration builders.

  class RL4JConfigurationMapper {
      fun qLearningConfig(config: ChessAgentConfig): QLearningConfiguration =
          QLearningConfiguration.builder()
              .seed(config.seed?.toInt() ?: System.currentTimeMillis().toInt())
              .maxEpochStep(config.maxStepsPerEpisode ?: DEFAULT_MAX_EPOCH)
              .maxStep(config.totalTrainingSteps ?: DEFAULT_MAX_STEP)
              .expRepMaxSize(config.maxBufferSize)
              .batchSize(config.batchSize)
              .targetDqnUpdateFreq(config.targetUpdateFrequency)
              .updateStart(config.warmupSteps ?: DEFAULT_WARMUP)
              .rewardFactor(1.0)
              .gamma(config.gamma)
              .errorClamp(config.errorClamp ?: DEFAULT_ERROR_CLAMP)
              .minEpsilon(config.explorationRate.toFloat())
              .epsilonNbStep(config.epsilonDecaySteps ?: DEFAULT_EPSILON_DECAY)
              .doubleDQN(config.doubleDqn)
              .build()

      fun dqnFactoryConfig(config: ChessAgentConfig): DQNFactoryStdDense.Configuration =
          DQNFactoryStdDense.Configuration.builder()
              .l2(config.l2Regularization ?: DEFAULT_L2)
              .learningRate(config.learningRate)
              .numHiddenNodes(config.hiddenLayers.firstOrNull() ?: DEFAULT_HIDDEN_NODES)
              .numLayers(config.hiddenLayers.size)
              .build()
  }

  Note: If ChessAgentConfig lacks a field (e.g., warmupSteps), we either add it or fall back to a documented default.

  ### 2. First-Class RL4J Backend Factory

  class RL4JBackendFactory(
      private val mapper: RL4JConfigurationMapper,
      private val observationEncoder: ObservationEncoder,
      private val actionAdapter: ActionAdapter,
      private val engine: ChessEngineAdapter
  ) : BackendFactory {

      override fun createTrainer(config: IntegrationConfig): Trainer {
          val agentCfg = config.profileConfig
          RL4JAvailability.validate()

          val qlConfig = mapper.qLearningConfig(agentCfg)
          val dqnConfig = mapper.dqnFactoryConfig(agentCfg)

          val dqnFactory = DQNFactoryStdDense(dqnConfig)
          val mdp = ChessMDP(engine, actionAdapter, observationEncoder)

          val qLearning = QLearningDiscreteDense(mdp, dqnFactory.buildDQN(mdp.observationSpace.shape(), mdp.actionSpace.shape()[0]), qlConfig)

          return RL4JTrainer(qLearning)
      }
  }

  No reflection, no Any. The trainer exposes save/load via qLearning.policy.

  ### 3. ChessMDP Enhancements

  ChessMDP already implements MDP<ChessObservation, Int, DiscreteSpace>. We reinforce:

  - Illegal action fallback inside step().
  - Rich StepReply metadata for observability.
  - Optionally surface legal-move masks as part of the observation if future policy masking is desired.

  ### 4. Observability

  Use RL4J’s TrainingListener to capture metrics:

  class RL4JMetricsListener(
      private val observer: MetricsSink
  ) : TrainingListener {
      override fun onNewEpoch(training: Training, episodeNum: Int) = ListenerResponse.CONTINUE

      override fun onTrainingResult(training: Training, result: StepResult): ListenerResponse {
          observer.record(
              episode = result.epoch counter,
              steps = result.stepCounter,
              epsilon = result.epislon,
              reward = result.reward
          )
          return ListenerResponse.CONTINUE
      }
  }

  We wire this listener into QLearningDiscreteDense so metrics flow directly from RL4J.

  ### 5. Optional Policy Masking

  Phase 1 relies on ChessMDP fallbacks. If we need policy-level masking later, we can wrap RL4J’s Policy:

  - Duplicate Q-values (INDArray) and mask illegal indices with NEGATIVE_INFINITY.
  - Delegate to the original policy’s nextAction(Observation).

  This will be treated as an enhancement after the first-class integration lands.

  ———

  ## Data Models

  - ChessObservation: implements Encodable, holds the 839-feature vector (and optionally a legal-mask array for future use).
  - ComponentConfiguration: (engine, nn, rl) structure used by CLI stack presets.
  - RL4JBackendMetadata: recorded in checkpoints; includes RL4J/DL4J versions, config hash, flag that reflection is disabled.

  ———

  ## Error Handling

  1. Availability: if RL4J classes aren’t on the classpath, BackendFactory refuses to build (instructions: set enableRL4J=true).
  2. Configuration mismatches: mapper validates numeric ranges and across-field consistency (e.g., buffer size ≥ batch size). Fail fast with descriptive messages.
  3. Illegal actions: ChessMDP.step() catches them, chooses a legal fallback, logs metrics, and keeps training stable.
  4. Metrics source: ensure we only read RL4J’s info objects; no leftover manual counters.

  ———

  ## Testing Strategy

  ### Unit Tests

  - Configuration mapper creates the correct RL4J builder objects with expected values.
  - Backend factory returns a QLearningDiscreteDense wired to ChessMDP, preserving type safety.
  - ChessMDP.step() handles legal/illegal moves, returns StepReply<ChessObservation>.

  ### Integration Tests (Gated by RL4J Availability)

  - Train for a few epochs with QLearningDiscreteDense.learn(); assert we get non-empty metrics, ReplayMemory grows, and checkpoints save/load.
  - Compare policy action selection before/after training to ensure we’re using real RL4J output.
  - CLI --nn rl4j path constructs RL4JLearningBackend and runs without reflection.

  ### Manual/Performance Tests

  - Run short training sessions (e.g., 100 episodes) to validate throughput and confirm the training loop is native RL4J.
  - Inspect logs for “mock policy” or “reflection” to ensure they no longer appear.

  ———

  ## Implementation Phases

  1. Native Configuration & Trainer
      - Replace reflection builders with native RL4J/DL4J configuration.
      - Instantiate QLearningDiscreteDense directly.
      - Remove mock policy fallback; rely on real DQNPolicy.
  2. Experience Handling & Training Loop
      - Ensure the manual replay buffer code path is disabled for RL4J.
      - Call qLearning.learn() (or trainEpoch) to let RL4J drive sampling.
  3. Legal Action Handling
      - Verify ChessMDP fallback handles illegal actions
      - Instrument counts for debugging.
      - Optionally, add policy-level masking after the core integration is stable.
  4. CLI & Config Harmonization
      - Use BackendFactory everywhere (already done—verify).

  - Introduce --stack presets if helpful, but not required for core backend work.

  5. Testing & Observability
      - Add RL4J-gated integration tests.
      - Document the build flag (enableRL4J=true, JDK requirement).
      - Hook up training listeners to capture RL4J metrics directly.
  6. Docs & Cleanup
      - Update README/DQN docs to describe the RL4J backend and how to run it.
      - Remove remaining reflection code and deprecated helper classes.
      - Add release notes summarizing the first-class RL4J support.