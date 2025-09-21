# Chess RL Bot – Architecture & Improvement Notes

Repo Sync Update — 2025-09-14

This section reflects the current repository state and supersedes outdated parts of this document. It highlights what is already done, what needs immediate fixes to compile/align, and reorders the near-term priorities.

Completed (present in repo)
- Unified models: SelfPlayGameResult adopted; EpisodeTerminationReason used consistently (AdvancedMetricsCollector, SelfPlaySystem, RealSelfPlayController).
- Game analysis alignment: GameAnalyzer uses consolidated fields (moveAccuracy, strategicComplexity, tacticalAccuracy) and SelfPlayGameResult throughout.
- Duplicates removed: Canonical ChessAgent/ChessAgentFactory/ChessAgentConfig; MonitoringConfig and ConfigurationValidationResult settled; interface files reference canonical types.
- PerformanceMonitor shapes: Uses shared PerformanceSnapshot and string-based OptimizationRecommendation; MonitoringSnapshot variant not used.
- Random Gaussian: Single extension fun Random.nextGaussian(); call sites updated to Random.Default.nextGaussian() or instance.nextGaussian().
- Seeded components: Seeded exploration strategies and replay buffers match rl-framework interfaces (selectAction signature with random param; sample(batchSize, random); capacity/isFull exposed).
- Deterministic/dashboard drift: Advanced dashboard/controller demos moved under integration/_excluded pending re-enable.
- Production debugging: Demo modules not present/compiled.

Immediate Fixes (to make everything consistent)
- TrainingMonitoringSystem.getPerformanceMetrics(): Build shared PerformanceMetrics(winRate, drawRate, lossRate, averageReward, rewardVariance) from history; remove custom fields like currentScore/bestScore.
- TrainingMonitoringSystem.getGameQualityMetrics(): Return shared GameQualityMetrics(averageQuality, moveAccuracy, strategicDepth, tacticalAccuracy); drop qualityTrend/totalGamesAnalyzed here.
- TrainingControlInterface.createEmptyDashboard(): Construct GameQualityMetrics with 4 args (remove the extra totalGames parameter).
- SystemOptimizationCoordinator.printOptimizationSummary(): Do not access non-existent PerformanceSnapshot.metrics/resources; print via available fields (overallScore, trainingEfficiency, convergenceIndicator). If richer detail is needed, extend PerformanceReport explicitly and thread it through.
- ExperienceBufferAnalyzer: Replace invalid string templates like "${value:.2f}" with "%.2f".format(value) (or String.format).

Reordered Near-Term Priorities
1) Batch training API in RL/NN [CRUCIAL]
   - Add trainBatch(inputs, targets) path via ChessNeuralNetwork or extend rl.NeuralNetwork; wire into algorithms to ensure actual optimizer steps.
2) Illegal-action masking in Q-targets [CRUCIAL]
   - Use next-state legal action masks in DQN target computation; carry mask in Experience or expose an env callback.
3) Target network synchronization [IMPORTANT]
   - Implement deep copy from online to target network in DQNAlgorithm.updateTargetNetwork().
4) Checkpointing (JVM-first) [IMPORTANT]
   - Real save/load for network config + weights [+ optimizer state], and resume training.
5) Self-play concurrency and backpressure [IMPORTANT]
   - Coroutines with bounded Channel; honor maxConcurrentGames; graceful shutdown.
6) Replay efficiency/memory layout [IMPORTANT]
   - Replace .shuffled() with indexed sampling; consider ring buffer with primitive arrays for hot path.
7) Performance/optimization wiring [IMPORTANT]
   - Call PerformanceMonitor.collectPerformanceSnapshot() at episode/batch/self-play boundaries; replace simulated benchmarks with timers around real NN forward/backward/train, replay sample, move generation.

Notes on Plan Changes
- Items about unifying result models, termination reasons, seeded components alignment, Random.nextGaussian ambiguity, and PerformanceMonitor shape drift are DONE in the repo and can be de-emphasized in ongoing work.
- Advanced UI/controllers remain excluded until core metrics stabilize; re-enable after items 1–4 are complete and TrainingMonitoringSystem metrics are corrected.

Status Reality Check (details)
- RL algorithms:
  - DQN: no optimizer step; target network sync is a placeholder; illegal‑action masking not applied when computing next‑state max.
  - Policy Gradient: computes returns/advantages but does not perform a real optimizer/batch update.
- Serialization: FeedforwardNetwork.save/load is stubbed; load throws via loadFromFile.
- Self‑play concurrency: SelfPlaySystem simulates concurrency; batch loop runs games sequentially.
- Optimizers/monitors: PerformanceMonitor/JVMTrainingOptimizer produce simulated metrics and are not wired to real NN/replay operations.
- Monitoring API mismatches to fix next: TrainingMonitoringSystem.getPerformanceMetrics/getGameQualityMetrics, TrainingControlInterface.createEmptyDashboard, SystemOptimizationCoordinator printing.
- Minor syntax: ExperienceBufferAnalyzer string formatting uses Python‑style specifiers.

What’s needed to make it work (concrete next steps)
- Wire optimizer updates:
  - Add a minimal trainBatch(inputs, targets) path via the existing NN adapter or extend rl.NeuralNetwork; use it from PG first, then DQN.
  - Implement DQN target network weight copy and illegal‑action masking for next‑state Q.
- Replace simulated metrics with real timings around NN forward/backward/train, replay sample, and move generation; invoke PerformanceMonitor at episode/batch boundaries.
- Introduce coroutine‑based self‑play (bounded Channel from producers to trainer); remove thread simulations.
- Implement real model save/load on JVM (weights + config [+ optimizer state]); persist seed/config with checkpoints.
11.5–11.13 Priority Slice (curated, ordered)

1) 11.8 Replace simulated metrics [IMPORTANT][medium]
- State: TrainableNeuralNetwork + DQN batching exist; pipeline still computes synthetic metrics.
- Action: Return real loss/entropy/grad‑norm from algorithms; plumb to pipeline reports; remove proxies.
- DoD: Reports show real batch metrics; values are finite and trend with training.

2) 11.5 Determinism & seeding [IMPORTANT][fast]
- State: SeedManager + SeededComponents exist; not centralized; seed not checkpointed.
- Action: Single run seed; thread Random into NN init, replay sampling, exploration; include seed in checkpoints + logs.
- DoD: Deterministic test mode, documented seed flow.

3) 11.9 Serialization / checkpointing (JVM minimal) [IMPORTANT][medium]
- State: Serialize to string; loadFromFile throws; checkpoint flow present.
- Action: JVM file I/O for FeedforwardNetwork.save/load (config + weights); integrate with CheckpointManager; resume.
- DoD: Save→load roundtrip test; resume training from checkpoint.

4) 11.11 API cleanup around buffers [IMPROVEMENT][fast]
- State: Agent keeps its own buffer while algorithm manages replay; risk of duplication.
- Action: Agent forwards to algorithm only; keep per‑episode buffer for on‑policy PG; remove off‑policy duplication.
- DoD: Single source of truth for experiences.

5) 11.6 Logging & metrics standardization [IMPROVEMENT][fast]
- State: printlns; metric keys/units vary; exports ad hoc.
- Action: Lightweight logger; standard keys (loss, grad_norm, entropy, q_mean, reward, win_rate, etc.); CSV/JSON snapshots.
- DoD: Consistent logs and exportable metrics.

6) 11.7 Benchmark standardization [IMPROVEMENT][fast]
- State: Scripts exist; lack warmup and metadata; JVM flags vary.
- Action: Add warmup, pin flags, record HW/OS/JDK metadata; stable filenames.
- DoD: Reproducible outputs with metadata.

7) 11.12 Safety checks & assertions [IMPROVEMENT][fast]
- State: Some requires present; dims/masks/rewards not consistently enforced.
- Action: Enforce input/output dims, mask sizes, reward ranges; fail fast in pipeline/algorithms.
- DoD: Clear early errors on invalid inputs.

8) 11.13 Baseline heuristic opponent [ADDITION][medium]
- State: Self‑play only; no baseline.
- Action: Add simple material+mobility heuristic as opponent for evaluation cycles; compute baseline metrics.
- DoD: Baseline win/draw/loss tracked separately from self‑play.

Runtime/test updates (just done)
- Added DQNLearningSmokeTest and ChessEnvironmentInvalidActionTest.
- Scaled down AdvancedSelfPlay* tests (fewer games/steps/cycles) to keep CI under minutes.
- Removed/rewrote drifted integration/UI tests; kept meaningful core coverage.

Notes for implementers
- DQN masking requires next‑state valid action info: extend EnhancedExperience to carry mask or list of valid actions; do not change base Experience generics.
- Target sync path is available via SynchronizableNetwork and the real wrapper; still add cadence verification/logging.
- For save/load: use simple line‑based format already produced by SimpleModelSerializer; add JVM file I/O helpers and weight import safety checks.
Risks and mitigations
- Concurrency defects (races/backpressure): use structured concurrency and ownership of buffers; add cancellation/timeout tests.
- Memory pressure (large buffers): prefer primitive arrays, batch allocations, and sampling by index (avoid shuffled()) to reduce GC churn.
- Action space instability: ensure illegal actions are masked for both selection and loss/targets; add unit tests.
- Reproducibility: centralize seeds through SeedManager; log/run deterministic mode in CI smoke tests.

How to run (updated realistic set)
- Tests that reflect current wiring:
  - integration: ChessTrainingPipelineTest, SelfPlayIntegrationTest, TrainingMonitoringSystemTest, ExperienceBufferAnalyzerTest
  - chess-engine/nn-package/rl-framework: module tests
- Demos:
  - :integration:runTrainingDemo and :integration:runIntegratedDemo
- Note: ProductionDebugging* demos/interfaces mentioned in some docs are not included; E2E test runners that depend on them are out of scope until re‑enabled.


Decisions (current milestone)
- Focus: Prioritize Policy Gradient / Actor-Critic work before DQN.
- API: Extend `rl.NeuralNetwork` with a batch `trainBatch(inputs, targets)` method and implement it by delegating to `nn`’s `FeedforwardNetwork.trainBatch` via `ChessNeuralNetwork`.
- Experience: Extend `rl.Experience` with optional `nextActionMask: DoubleArray?` and `fen: String?` metadata populated during self-play/step.
- Platform: JVM-only training path is acceptable short-term (serialization, concurrency on JVM first).
- Promotions: Less important for now; always promote to queen (defer richer encoding).
- Evaluation: Add a simple heuristic opponent for baseline evaluation.

High-Priority PG/AC Tasks (before DQN)
- Batch train API on `rl.NeuralNetwork` and wiring through `ChessNeuralNetwork` and PG algorithm.
- Proper PG loss with masked log-prob objective using advantages; remove simulated metrics and report real loss/entropy/grad-norm.
- Checkpointing (save/load) on JVM to resume PG training; include seed + config snapshot.
- Extend `Experience` with `nextActionMask` and `fen` to support masked loss and better debugging.

Ordered proposals by importance, with package tags (nn, rl, chess, integration), optional class refs, and complexity.

1) Weight updates not applied / Batch train API to RL NN [CRUCIAL] [medium]
- Packages: nn, rl, integration
- Refs: `rl-framework/RLAlgorithms.kt::DQNAlgorithm.updatePolicy`, `nn-package/NeuralNetwork.kt::FeedforwardNetwork`, `integration/ChessAgent.kt::ChessNeuralNetwork`
- Issue: DQN calls `forward/backward` but never performs optimizer updates. `FeedforwardNetwork` updates weights only via `trainBatch`/`optimizer.updateWeights`, not via `backward` alone. Net effect: no learning.
- Fix: Provide a batched train step from `nn-package` and call it from RL:
  - Option A (recommended): Extend `ChessNeuralNetwork` with `trainBatch(inputs, targets, epoch?)` delegating to `FeedforwardNetwork.trainBatch`, and expose via `rl.NeuralNetwork` or an internal cast in algorithms.
  - Option B: Refactor `rl.NeuralNetwork` interface to include a `trainBatch`/`optimizerStep` API; update algorithms accordingly.
- Tests: Unit test that loss decreases on a simple supervised batch; integration test where Q-network values change after updates.

2) Illegal-action masking in Q-targets [CRUCIAL] [medium]
- Packages: rl, integration, chess
- Refs: `rl-framework/RLAlgorithms.kt::DQNAlgorithm.computeQTargets`, `integration/ChessEnvironment.getValidActions`, `integration/ChessEnvironment.getActionMask`
- Issue: Targets use `max` over all 4096 actions, including illegal ones, biasing overestimation.
- Fix: Mask invalid actions in next-state Q before `max`:
  - Store `nextActionMask` (DoubleArray or list of valid next actions) per experience during generation; or
  - Add a callback/Environment adapter in algorithms to obtain legal actions for a given state representation (e.g., FEN or decoded state).
- Tests: Verify masked-`max` never selects illegal indices; regression test on positions with few legal moves.

3) Target network update is a no-op [ADDITION] [hard]
- Packages: rl, nn
- Refs: `rl-framework/RLAlgorithms.kt::DQNAlgorithm.updateTargetNetwork`, `nn-package/NeuralNetwork.kt::DenseLayer` getters/setters
- Issue: Target network never syncs, harming stability.
- Fix: Implement deep copy of weights/biases from online to target; use `DenseLayer.getWeights()/getBiases()` and `setWeights/setBiases`.
- Tests: After N updates, target differs; after sync, parameters match exactly.

4) Real serialization/checkpointing [IMPORTANT] [medium]
- Packages: nn, integration
- Refs: `nn-package/NeuralNetwork.kt::FeedforwardNetwork.save/load`, `SimpleModelSerializer`
- Issue: Save prints only; load throws. Cannot resume training.
- Fix: Implement JVM serializer (JSON/CBOR/binary) for network config + weights; add `expect/actual` or limit to JVM initially for training. Include optimizer state (Adam moments) if feasible.
- Tests: Save→load roundtrip preserves outputs; integration checkpoint resume during training.

5) Self-play real concurrency with backpressure [IMPORTANT] [hard]
- Packages: integration
- Refs: `integration/SelfPlaySystem.kt` (sequential batches with comments), `SelfPlayController`
- Issue: “Concurrent” games currently run sequentially.
- Fix: Use Kotlin coroutines:
  - Launch up to `maxConcurrentGames` game coroutines.
  - Stream `EnhancedExperience` batches via a bounded `Channel` to a trainer coroutine.
  - Handle cancellation, graceful shutdown, and backpressure.
- Tests: Concurrency smoke tests (start/stop), backpressure (slow trainer), and correctness (counts match).

6) Experience replay efficiency and memory layout [IMPORTANT] [medium]
- Packages: rl
- Refs: `rl-framework/ExperienceReplay.kt::CircularExperienceBuffer.sample`
- Issue: `.shuffled()` is O(n); `MutableList<Experience>` holds many objects and arrays → GC churn.
- Fix: Index-based random sampling and/or ring buffer with primitive arrays (`DoubleArray`/`FloatArray`) for state/action/reward fields; avoid boxing.
- Tests: Sampling correctness; throughput benchmark improvements.

7) Prioritized replay integration [ADDITION] [medium]
- Packages: rl
- Refs: `rl-framework/ExperienceReplay.kt::PrioritizedExperienceBuffer`
- Issue: Class exists but not used; no TD-error updates.
- Fix: Add option in `DQNAlgorithm` to use PER; compute TD errors, update priorities post-update, and apply importance sampling weights.
- Tests: Priority distribution sanity, learning speed vs. uniform.

8) Replace simulated training metrics with real ones [IMPORTANT] [medium]
- Packages: integration, rl
- Refs: `integration/ChessTrainingPipeline.performPolicyUpdate`, `SelfPlayController.trainOnSelfPlayExperiences`
- Issue: Metrics (loss, entropy, gradient norm) are synthetic.
- Fix: Return actual metrics from algorithms/networks after real updates; standardize a metrics struct.
- Tests: Assert metrics are finite, trend over time on a toy task.

9) Unify NN interfaces / adapter boundary [IMPORTANT] [medium]
- Packages: nn, rl, integration
- Refs: `rl-framework/NeuralNetwork` vs `nn-package/NeuralNetwork`, `ChessNeuralNetwork`
- Issue: Two interfaces cause impedance mismatch (see item #1).
- Fix: Choose one as canonical or add a minimal train API to the RL-facing interface; keep `ChessNeuralNetwork` as adapter.
- Tests: Compile-time alignment; algorithm-only unit tests using a mock NN.

10) Double DQN and N-step returns [ADDITION] [medium]
- Packages: rl
- Refs: `DQNAlgorithm.computeQTargets`
- Issue: Plain DQN more prone to overestimation and slow credit assignment.
- Fix: Implement Double DQN target selection; optionally support N-step returns for better propagation.
- Tests: Unit tests on toy MDPs; compare overestimation metrics.

11) Record next-state masks during experience generation [IMPORTANT] [medium]
- Packages: integration, chess
- Refs: `integration/SelfPlayGame.playGame`, `ChessEnvironment.getActionMask`
- Issue: For target masking, need `nextValidActions`/mask per experience.
- Fix: Augment `EnhancedExperience` with `nextActionMask: DoubleArray` (or `List<Int>`), set at step time; optionally extend base `Experience` with optional metadata without breaking APIs.
- Tests: Mask shape correctness, legal indices only.

12) Policy Gradient correctness (log-prob gradients) [IMPORTANT] [hard]
- Packages: rl, nn
- Refs: `PolicyGradientAlgorithm.updatePolicyNetwork`
- Issue: Uses `backward` with probability targets, not proper ∇ log π(a|s) * advantage.
- Fix: Implement explicit gradient for selected action log-prob; if staying within current NN API, compute logits, softmax, and backprop analytically or extend NN to accept per-sample gradients.
- Tests: REINFORCE learns simple bandit; variance reduces with baseline.

13) Exploration and entropy control [IMPROVEMENT] [fast]
- Packages: rl, integration
- Refs: `EpsilonGreedyStrategy`, `BoltzmannStrategy`, usage in `ChessAgent`
- Fix: Schedule epsilon/temperature per run phase; add entropy regularization knob in algorithms; expose via config.
- Tests: Entropy trends under schedule; no collapse warnings.

14) Determinism and seeding [IMPORTANT] [fast]
- Packages: nn, rl, integration
- Refs: various `Random.Default`
- Fix: Centralize seed in config; thread through NN init, replay sampling, exploration; log seed in checkpoints.
- Tests: Deterministic runs under fixed seed.

15) Action encoding for promotions [ADDITION] [hard]
- Packages: integration, chess
- Refs: `ChessActionEncoder`, `ChessEnvironment.findMatchingMove`
- Issue: 4096 mapping drops promotion piece choice (defaults to queen), limiting policy learning.
- Fix: Encode promotion type (e.g., 4096 × {none,Q,R,B,N} or add offsets); adjust mask generation and decoding.
- Tests: Promotion legality/masks; endgame tasks with underpromotion options.

16) Data type and batching performance [PERF] [medium]
- Packages: nn
- Refs: `DenseLayer`, `FeedforwardNetwork.trainBatch`
- Fixes:
  - Consider `FloatArray` for activations/weights on JVM to halve memory traffic.
  - Reduce allocations: reuse scratch buffers; avoid per-sample `copyOf` in hot paths.
- Tests: Microbenchmarks of forward/backward; memory allocations per batch.

17) Vectorized batch forward/backward [PERF] [hard]
- Packages: nn
- Refs: `FeedforwardNetwork.trainBatch`
- Issue: Loops per sample; no SIMD/batch matmul.
- Fix: Introduce simple matrix ops or BLAS binding for JVM; or hand-rolled vectorized loops that process small blocks.
- Tests: Throughput increase on large heads (4096 output).

18) Pipeline → algorithm plumbing [IMPORTANT] [medium]
- Packages: integration, rl
- Refs: `ChessTrainingPipeline.performBatchTraining/performPolicyUpdate`
- Issue: Pipeline simulates updates and calls `agent.learn` per experience.
- Fix: Expose algorithm via agent (getter) or pass algorithm into pipeline; call a real `updatePolicy(experiences)` batched method and return true metrics.
- Tests: Pipeline batch step changes weights and metrics.

19) Evaluation vs baseline and fixed suites [IMPORTANT] [medium]
- Packages: integration, chess
- Refs: `SelfPlayController.evaluateAgentPerformance`
- Fix: Add evaluation games vs. a simple heuristic engine or scripted opponent; maintain a suite of fixed positions; compute ELO-like deltas per iteration.
- Tests: Deterministic eval games; regression alert when performance drops.

20) Reward shaping scales & toggles [IMPORTANT] [fast]
- Packages: integration, chess
- Refs: `ChessPositionEvaluator`, `ChessRewardConfig`
- Fix: Ensure positional rewards are small vs terminal; add tests validating relative magnitudes; enable/disable shaping per phase.
- Tests: Reward sums bounded; no dominance over terminal signals.

21) Logging & metrics standardization [IMPROVEMENT] [fast]
- Packages: integration, rl
- Refs: progress prints across classes
- Fix: Use a lightweight structured logger; standardize metric names/units; add rolling summaries; optionally emit CSV/JSON for dashboards.
- Tests: Logs parse; metrics files produced per run.

22) Benchmark standardization [IMPROVEMENT] [fast]
- Packages: integration (scripts)
- Refs: `benchmark-*.sh`
- Fix: Pin JVM flags, warmup iterations, fixed seeds, record hardware/JDK; compare apples-to-apples across runs.
- Tests: Scripts produce comparable outputs with metadata.

23) CI enhancements and long-run/nightly [IMPORTANT] [medium]
- Packages: root, all
- Fix: CI to run unit tests on push/PR; nightly long-run self-play smoke; optional performance thresholds; cache Gradle.
- Tests: CI green for short runs; nightly artifacts retained.

24) Documentation: end-to-end architecture & data flow [IMPORTANT] [medium]
- Packages: docs (new), integration
- Fix: Add a “How it fits together” guide: components, data flow (Self-Play → Replay → Training → Checkpointing → Eval), sequence diagrams, config cookbook.
- Tests: N/A; review by reading.

25) Experience metadata extension [ADDITION] [medium]
- Packages: rl, integration, chess
- Refs: `Experience`, `EnhancedExperience`
- Fix: Non-breaking: add optional `metadata` or create `ExperienceExt` including `fen`/`actionMask/nextMask`, timestamps, episode ids; pipelines and algorithms read if present.
- Tests: Backward compatibility; mask consumed when available.

26) Concurrency test suite [IMPORTANT] [medium]
- Packages: integration
- Refs: Self-play coroutines (to be added)
- Fix: Add tests for cancellation, backpressure, resource cleanup; ensure no deadlocks across 100s of games.
- Tests: Dedicated stress tests.

27) Memory & GC hygiene [PERF] [medium]
- Packages: nn, rl, integration
- Fix: Pool `DoubleArray/FloatArray` buffers for states/targets; reuse `Experience` objects or use struct-of-arrays; avoid ephemeral maps in hot paths (e.g., `validActions.associateWith`).
- Tests: Allocation profiling before/after.

28) ChessEnvironment → next-legal-actions helper [ADDITION] [medium]
- Packages: integration, chess
- Refs: `ChessEnvironment`
- Fix: Provide `computeValidActionsFromState(state: DoubleArray)` or accept FEN to compute mask without mutating environment; supports masked targets in RL without environment instance state.
- Tests: Round-trip encode/decode consistency; mask equals internal one.

29) API clean-up around buffers [IMPROVEMENT] [fast]
- Packages: rl, integration
- Issue: Agent stores a buffer, algorithm stores a replay buffer too → duplication.
- Fix: Let agent forward experiences directly to algorithm (single source of truth); keep only short-term episode buffer if needed.
- Tests: No redundant growth; memory usage consistent.

30) Config management [IMPROVEMENT] [fast]
- Packages: integration
- Fix: Add simple HOCON/JSON/YAML config loader; log resolved config; support overrides via CLI/env.
- Tests: Parse and apply across modules.

31) Cross-target consistency checks [NICE-TO-HAVE] [medium]
- Packages: nn, integration
- Fix: Validate JVM vs Native numerical parity on small nets; guard via tests to catch regressions.
- Tests: Output deltas within tolerance.

32) Output head compute reduction [IMPROVEMENT] [hard]
- Packages: nn, integration
- Issue: 4096 head costly; many invalid actions each step.
- Fix: Explore masked softmax/training that only computes gradients for valid indices; or factorized head (from,to) with promotion subhead; careful with loss/selection.
- Tests: Correctness of masks; speed improvements.

33) Safety checks and assertions [IMPROVEMENT] [fast]
- Packages: all
- Fix: Strengthen `require`/`check` messages and add invariants around dimensions, masks, and rewards to fail fast.
- Tests: Unit tests for edge cases (no moves, invalid actions).

34) Early stopping criteria tuning [NICE-TO-HAVE] [fast]
- Packages: integration
- Fix: Replace absolute threshold with patience-based improvement; track best rolling average.
- Tests: Behavior on synthetic learning curves.

35) Self-play opponent strategies [ADDITION] [medium]
- Packages: integration
- Refs: `OpponentUpdateStrategy`
- Fix: Implement snapshotting main agent to frozen copies; support historical sampling; schedule updates by performance.
- Tests: Iteration logs reflect opponent switching.

36) Baseline heuristic opponent [ADDITION] [medium]
- Packages: chess, integration
- Fix: Implement a simple material+mobility based opponent to serve as baseline for eval without another trained net.
- Tests: Deterministic games; sanity win/draw/loss distribution.

37) Benchmark harness improvements [IMPROVEMENT] [fast]
- Packages: scripts
- Fix: Add JMH or Kotlin microbench alternatives for core ops (forward, backward, sampling); make results machine-readable.
- Tests: N/A; verify reproducible outputs.

38) Documentation: Developer runbook & troubleshooting [IMPORTANT] [fast]
- Packages: docs
- Fix: Add TSG for common issues: exploding gradients, policy collapse, memory pressure; include how to enable deterministic runs, where to find logs, and how to resume from checkpoints.

39) Structured training artifacts [IMPROVEMENT] [fast]
- Packages: integration
- Fix: Save run summaries (JSON) with metrics, seeds, git commit, config snapshot in a `runs/` directory.

40) JVM flags and GC tuning presets [NICE-TO-HAVE] [fast]
- Packages: scripts
- Fix: Provide presets for training vs eval (e.g., G1 vs ZGC, heap size, `-XX:+AlwaysPreTouch`).

41) Promotion handling defaulting [LESS IMPORTANT][DEFERRED] [fast]
- Packages: integration, chess
- Refs: `ChessEnvironment.findMatchingMove`
- Decision: Always promote to queen (already implemented). Defer richer action encoding for promotions until core PG/AC + metrics + checkpoints are complete.

42) Move selection hot path avoidance of maps [PERF] [fast]
- Packages: integration
- Refs: `ChessAgent.selectAction`
- Fix: Avoid `associateWith`; pre-allocate arrays and compute max or softmax over valid indices.

43) Checkpoint retention policy [NICE-TO-HAVE] [fast]
- Packages: integration
- Fix: Keep last N and best K checkpoints; clean others; include metadata.

44) Code comments/docs in critical classes [IMPROVEMENT] [fast]
- Packages: nn, rl, integration
- Fix: Add short module headers explaining responsibilities and interactions, especially in algorithms and pipeline.

45) Native target parity plan [NICE-TO-HAVE] [hard]
- Packages: nn
- Fix: Plan for implementing file I/O and performance-critical paths for Kotlin/Native, or constrain training to JVM explicitly in docs/config.


Open Questions
- DQN as primary? Should we prioritize DQN fixes vs REINFORCE/actor-critic? Do you want Double DQN and N-step now?
- API change tolerance: Can we extend `rl.NeuralNetwork` to add a batched train API, or should we keep changes inside `ChessNeuralNetwork` adapter?
- Experience schema: Are you okay extending `Experience` or adding a parallel `ExperienceExt` to carry masks/FEN?
- JVM-only training acceptable short-term? If so, we can ship serialization and concurrency sooner for JVM, and plan Native later.
- Promotion encoding: Do you want full promotion action space now, or keep queen-only default and revisit later?
- Baseline opponent: Okay to add a simple heuristic engine in `chess-engine` for evaluation?

## Status Reality Check — Optimization Modules

Scope: integration performance/optimization stack (PerformanceOptimizer.kt, NativeOptimizer.kt, HyperparameterOptimizer.kt, PerformanceMonitor.kt, SystemOptimizationCoordinator.kt, docs/tests).

What exists
- Files/classes present: JVMTrainingOptimizer, TrainingMemoryManager, OptimizedBatchProcessor, ConcurrentTrainingManager; NativeDeploymentOptimizer, NativeInferenceOptimizer, NativeMemoryOptimizer, NativeGameplayOptimizer; HyperparameterOptimizer + SearchSpace/Evaluator/ABTester; PerformanceMonitor + MetricsCollector/SystemProfiler/ResourceMonitor/PerformanceBenchmarkSuite; SystemOptimizationCoordinator; docs and tests.
- Tests exercise these APIs with simulated data; docs outline intended usage.

Gaps and risks
- Not wired to real training: None of these modules are invoked from `ChessTrainingPipeline`, `SelfPlayController`/`SelfPlaySystem`, or RL algorithms.
- Simulated metrics/benchmarks: Most measurements use random inputs and fixed formulas; do not reflect actual NN forward/backward, replay sampling, or chess-engine move-gen.
- Concurrency is simulated with threads; real self-play remains sequential; no backpressure/structured concurrency.
- Core learning is incomplete (see items #1/#2/#7/#8/#9): without real optimizer steps/PG loss/target sync/masking, optimizations can’t show true impact.

Actions to make it real
- Wiring [medium]:
  - Call `PerformanceMonitor.collectPerformanceSnapshot()` at key points in `ChessTrainingPipeline` (episode loop, batch training), in self-play iteration boundaries, and before/after policy updates.
  - Integrate `SystemOptimizationCoordinator` to produce periodic reports during long runs.
- Instrumentation [medium]:
  - Replace simulated benchmarks with timers around real code paths: `FeedforwardNetwork.forward/backward/trainBatch`, replay `sample`, `ChessBoard.getAllValidMoves`, environment `step`.
  - Record GC and allocation stats where available (JVM), and batch/episode throughput.
- Memory/pooling [medium]:
  - Use `ArrayPool` or a lighter pool in replay buffers and batch construction to reduce allocations; measure deltas with the monitor.
- Concurrency [hard]:
  - Implement coroutine-based self-play with bounded channels and a trainer coroutine; capture throughput/latency in the monitor; remove thread simulations.
- Validation [medium]:
  - Turn `PerformanceBenchmarkSuite` into reproducible micro/macro benchmarks against the real pipeline; store results per run for regression checks.

Acceptance criteria
- Monitor collects real throughput/latency/memory snapshots during training; reports show trends and regressions.
- Benchmarks run against actual NN/replay/engine paths; repeated runs show consistent deltas with changes.
- Memory allocation/GC reduced after pooling; measured by monitor and confirmed by allocation counters.
- Self-play concurrency increases experience generation rate with bounded resource usage (no deadlocks/leaks).
Assessment Snapshot — Current

What Works (Real)
- Neural network core: FeedforwardNetwork with layers/optimizers; adapter RealNeuralNetworkWrapper adds TrainableNeuralNetwork + SynchronizableNetwork.
- DQN path: Uses batch training via trainBatch when available; target sync implemented through SynchronizableNetwork copy; end‑to‑end smoke test (DQNLearningSmokeTest) demonstrates Q‑value change.
- Self‑play result model: SelfPlayGameResult + EpisodeTerminationReason used consistently.
- Environment + encoding: 839‑feature state, 4096 action encoding; invalid action yields negative reward (tested).
- Seed/determinism: SeedManager + seeded strategies/replay buffers aligned to rl‑framework interfaces.
- Training pipeline scaffold: ChessTrainingPipeline runs episodes, buffers experiences, and triggers agent.forceUpdate().

What’s Broken/Missing
- DQN: Illegal‑action masking in next‑state targets is not applied; metrics remain simplistic; test coverage is smoke‑level.
- Policy Gradient: Computes returns/advantages but lacks proper batched optimizer updates using log‑prob gradients.
- Serialization: FeedforwardNetwork.save/load lacks JVM file I/O; load still throws; checkpoints cannot restore weights.
- Concurrency: Self‑play concurrency simulated (sequential batches); no coroutines/backpressure channel.
- Performance/monitoring: PerformanceMonitor and optimizers still produce/consume simulated metrics; not wired to pipeline hot paths.
- API cleanliness: Agent keeps an internal buffer even when algorithm maintains replay (off‑policy duplication risk).

How To Run (updated)
- Fast smoke tests:
  - DQNLearningSmokeTest — verifies buffer growth and Q‑value change.
  - ChessEnvironmentInvalidActionTest — penalizes illegal action.
  - ChessAgentTest — creation, selection, learning basics.
  - AdvancedSelfPlayTrainingPipelineTest — scaled configs; validates cycle structure/performance metrics.
  - AdvancedSelfPlayPerformanceTestSimple — small/large configs with tight limits for CI.
- Commands:
  - `./gradlew :integration:jvmTest` (entire integration tests)
  - Subset: `--tests "*DQNLearningSmokeTest*"`, `--tests "*AdvancedSelfPlayTrainingPipelineTest*"`

Outdated/Excluded
- Production Debugging UI/Interface and older control dashboards are excluded or removed; tests referencing them were pruned.
- Scripts that call those components will fail until re‑enabled against stable interfaces.
