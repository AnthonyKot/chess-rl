# RL4J Backend – Legacy vs First-Class Integration

This document summarises how the project’s RL4J backend has evolved from the
reflection-based shim to the new native integration. It captures the main
behavioural differences, configuration changes, pros & cons, and practical tips
for running and troubleshooting RL4J training.

---

## At a Glance

| Topic | Legacy RL4J (reflection shim) | New RL4J (first-class) |
|-------|--------------------------------|------------------------|
| Instantiation | Built DQN/MDP via reflection; fall back to manual policy when classes missing | Uses `RL4JConfigurationMapper`, `DQNFactoryStdDense`, `QLearningDiscreteDense` directly |
| Experience handling | Manual replay buffer, `learn(experience)` plumbing | RL4J owns replay and training loop via `train()` |
| Policy implementation | Optional mock policy when RL4J unavailable | Always a real `DQNPolicy<ChessObservation>` (no mock fallback) |
| Illegal move handling | Environment fallback with minimal logging | Environment fallback + per-episode metrics/masking hook |
| Metrics/logging | Custom counters, limited insight | RL4J `TrainingListener` exposes reward/epsilon/steps, backend logs version/hyperparameters |
| Tests | Sparse, reflection heavy | Gated RL4J integration tests (init/train/save-load, legality checks) |
| Dependencies | Mixed versions, reflection resilience | All DL4J/ND4J/RL4J on matching release (`1.0.0-beta7`) |
| Multi-process self-play | Could use existing harness | Disabled by default (stability), sequential fallback |

---

## Behavioural Differences

### 1. Native Configuration & Training
- **Before**: Training went through manual replay buffers; RL4J calls were wrapped
  in reflection and could silently fall back to manual DQN.
- **Now**: `RL4JChessAgent` instantiates `QLearningDiscreteDense` with
  `ChessMDP` and runs RL4J’s `train()` directly. Manual replay buffers are no
  longer touched for the RL4J backend.

### 2. Experience & Illegal Move Handling
- **Before**: Illegal actions were corrected by `ChessEnvironment`, but the
  policy had no notion of legal masks; logging was minimal.
- **Now**: `ChessMDP` tracks per-episode counts (`illegalAttempts`, `fallbackActions`), logs a summary per reset, and exposes legal-action masks through `ChessObservation` and `ChessActionSpace`. Illegal actions are masked in `selectAction` before hitting the environment.

### 3. Metrics & Observability
- **Before**: Metrics were stitched together manually (loss=0 placeholders, etc.).
- **Now**: An RL4J `TrainingListener` captures reward totals, step counts, and
  epsilon. The backend logs library versions, hyperparameters, and listener
  metrics after each training run.

### 4. Reflection & Mock Policies
- **Before**: Extensive use of reflection plus a mock policy to keep the CLI
  usable when RL4J classes were absent.
- **Now**: No reflection path and no mock policy. `RL4JAvailability` gates setup
  and caches availability.

### 5. Testing & Docs
- **Before**: Limited RL4J coverage; instructions missing.
- **Now**: RL4J tests run when `-PenableRL4J=true`. README documents JDK
  requirement, Gradle flag, and how to run the suite. RL4J.md (this doc) captures
  the integration details.

---

## Pros & Cons of the New Integration

**Pros**
- *Native Lifecycle*: Training loop, replay, and metrics come from RL4J, so
  behaviour matches official examples.
- *Type Safety*: Builders and agents use concrete types—no `Any` casts or mock
  policies.
- *Better Observability*: Hyperparameters, episodes, epsilon, and reward stats
  are logged automatically.
- *Legal Move Masking Hook*: Observations now optionally expose legal action
  masks for future policy-level masking.
- *Test Coverage*: RL4J-gated tests verify initialization, training, and
  checkpoint round trips.

**Cons / Known Trade-offs**
- *Performance*: Sequential self-play is the default (multi-process disabled to
  avoid worker launch issues). RL4J training can be slower than the manual path.
- *Illegal Actions Still Possible*: While masked earlier, RL4J may still propose
  illegal moves at high epsilon; the environment falls back but logs them.
- *Increased Setup Requirements*: Must enable RL4J dependencies (`-PenableRL4J=true`) and ensure JDK 11+ is available locally.
- *Version Sensitivity*: All DL4J/RL4J bundles must stay on the same release to
  avoid `NoSuchMethodError`s.

---

## Running RL4J Training

```bash
# Enable RL4J dependencies and run the fast-debug profile
./gradlew :integration:run -PenableRL4J=true --args="--train --nn rl4j --profile fast-debug"

# Run RL4J integration tests (skipped if the flag is absent)
./gradlew :integration:test -PenableRL4J=true
```

Tips:
1. Ensure a JDK (11+) is installed and `JAVA_HOME` is set.
2. Consider lowering `maxConcurrentGames` (or keep the default sequential mode) to
   avoid worker launcher errors.
3. Watch for the `RL4J training completed: ...` log each cycle—it includes reward,
   epsilon, step counts, and loss.

---

## Tuning & Troubleshooting

- **Illegal action spam**: Early training uses high epsilon; you can lower
  `explorationRate` in your config or adjust epsilon decay. The environment still
  guards legality and logs a single warning per episode.
- **Slow cycles**: Reduce `maxStepsPerGame` or map `maxEpochStep`/`maxStep` to
  smaller values via `ChessRLConfig` → `RL4JConfigurationMapper`. Multi-process
  support can be re-enabled once RL4J worker boot is stable.
- **Version conflicts**: Keep `deeplearning4j-core`, `nd4j-native-platform`, and
  `rl4j-*` on the same release (`1.0.0-beta7` currently).
- **Missing RL4J classes**: If the availability warning appears, double-check the
  Gradle flag or add `enableRL4J=true` to `gradle.properties`.

---

## Summary

The first-class RL4J backend removes the reflection shim, aligns configuration
and metrics with RL4J’s public APIs, and adds observability hooks (legal-action
masking, per-episode summaries, training listeners). It trades some of the legacy
pipeline’s speed and simplicity for correctness and maintainability. Use the
Gradle flag to opt in, rely on the new logs/tests for visibility, and adjust your
config (episode length, epsilon schedule) to fit the RL4J lifecycle.

