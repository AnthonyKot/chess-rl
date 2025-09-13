# Chess RL Bot – Architecture & Improvement Notes

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
