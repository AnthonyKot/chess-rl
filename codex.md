# Repository Context (Chess RL Project)

## High-Level Goals
- Reinforcement-learning chess system with plug-and-play engines (builtin and chesslib) and multiple NN backends.
- Recent focus: integrating RL4J as an alternative training backend alongside manual/DL4J/KotlinDL implementations.

## Current State of RL4J Integration
- CLI now routes `--nn rl4j` through `BackendFactory`, creating `RL4JLearningBackend`.
- Observation (`ChessObservation`), action (`ChessActionSpace`), and MDP (`ChessMDP`) wrappers directly implement RL4J interfaces.
- `RL4JChessAgent` still relies extensively on reflection: configuration builders, replay buffer writes, and training loops all fall back to reflective access.
- Reflection path creates mock policy artifacts and uses protected methods (e.g., `trainStep`) because the real RL4J APIs are not wired end-to-end.
- Experience replay integration is fragile: code attempts `replay.store(Encodable, Int, Double, Encodable, Boolean)` but RL4J 1.0.0-beta7 expects `storeTransition(Transition)`; the current call throws and is silently ignored.
- Configuration mapping currently ignores many values from `ChessAgentConfig` (hard-coded learning rate, gamma, etc.), so parity with other backends is not yet guaranteed.
- Tests referencing real RL4J behavior exist, but builds fail locally without a JDK and the reflection fallbacks mean “success” does not imply true RL4J usage.

## Known Issues / Warnings
- Compiler warnings in `RL4JChessAgent.kt` about unused locals (`stepCounter`, `currentEpsilon`) and generic type inference.
- Recent attempt to swap in direct builder APIs caused compilation errors (`seed` expects Int, `learningRate` method undefined, constructor wants `IDQN`).
- Need to ensure RL4J dependencies are gated by `enableRL4J` flag in `gradle.properties` and resolve missing JDK for tests.

## Next Steps (High Priority)
1. Replace reflection scaffolding with real RL4J builder usage, honoring `ChessAgentConfig` values.
2. Fix replay buffer interaction using the correct RL4J `Transition` API; ensure experiences actually feed into RL4J training.
3. Invoke RL4J training through public entry points (e.g., `learn()`), avoiding protected-method reflection.
4. Remove or rework mock policy fallback so the agent always returns a valid RL4J policy.
5. Once real integration is solid, add gated tests/CI jobs that run with RL4J on the classpath and verify checksum vs. manual backend.

## Miscellaneous Notes
- Builtin vs chesslib engine parity still under investigation—builtin engine reports many invalid moves during heuristic evaluations.
- `SEARCH.md` created to capture future search-based enhancements (e.g., MCTS assistance with limited compute).
- Documentation cleanup ongoing (`README.md`, `DQN.md` updated; other docs removed per earlier request).

