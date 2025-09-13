# Stabilisation Plan — Integration Module

Purpose: Restore a compiling, coherent integration module by aligning types, removing conflicting definitions, and fixing platform/utility issues blocking tests (e.g., SystemOptimizationBasicTest).

## Goals
- Unified self‑play result model used across analytics/monitoring.
- Single source of truth for shared data classes (no redeclarations/visibility conflicts).
- Remove inheritance from final classes; prefer composition.
- Clean, unambiguous `expect/actual` time API.
- Eliminate unsupported `Random.nextGaussian()` usage.
- Fix missing imports, numeric type mismatches, and invalid named arguments.

## Workstream 1 — Standardize Self‑Play Result Model [CRUCIAL]
- Choose one model across integration:
  - Preferred: `SelfPlaySystem`’s `SelfPlayGameResult` + `GameOutcome { WHITE_WINS, BLACK_WINS, DRAW, ONGOING }`.
- Align `RealSelfPlayController` to the chosen model:
  - Rename enum values: `WHITE_WIN/BLACK_WIN` → `WHITE_WINS/BLACK_WINS`.
  - Add fields to `GameResult` (or switch to `SelfPlayGameResult`): `terminationReason`, `gameLength`, `finalPosition`.
  - Provide mapping helpers if definitions cannot be changed immediately.
- Acceptance:
  - All usages in `AdvancedMetricsCollector`, `GameAnalyzer`, `TrainingMonitoringSystem`, `TrainingReportGenerator`, dashboards compile with a single `GameOutcome` and result type.

## Workstream 2 — Consolidate Shared Data Classes [IMPORTANT]
- Single source of truth (public) for:
  - `PerformanceMetrics`, `PerformanceSnapshot`, `GameQualityMetrics`, `DashboardCommand`, `VisualizationType`, `ReportType`, `TrendDirection`, `CommandResult`, `GamePhaseAnalysis`, `StrategicAnalysis`, `OptimizationRecommendation`, `RecommendationPriority`.
- Actions:
  - Move canonical definitions into one package (e.g., `com.chessrl.integration.model` or `monitoring`).
  - Make them `public` if used across files; remove `private` file‑scope variants.
  - Delete duplicates and fix imports.
- Acceptance:
  - No “Redeclaration” or “Cannot access … private in file” compile errors remain.

## Workstream 3 — Stop Extending Final ChessAgent [IMPORTANT]
- Replace subclassing with composition:
  - Introduce `RealChessAgentAdapter` wrapping a `ChessAgent` instance.
  - Remove all `override` of final methods.
- Alternative (only if absolutely necessary): Mark `ChessAgent` (and required methods) `open`, noting ripple effects.
- Acceptance:
  - No “final cannot be inherited/overridden” errors. Adapter compiles and passes through required calls.

## Workstream 4 — Fix Time Function Declarations [IMPORTANT]
- Keep only:
  - `commonMain`: `expect fun getCurrentTimeMillis(): Long`
  - `jvmMain`: `actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()`
  - `nativeMain`: one `actual` implementation.
- Remove any extra non‑`expect` definition in `commonMain` or duplicate `actual` in platform sources.
- Acceptance:
  - No “Conflicting overloads / several compatible actual declarations” errors.

## Workstream 5 — Replace Random.nextGaussian() [IMPORTANT]
- Kotlin’s `kotlin.random.Random` has no `nextGaussian()`.
- Add a shared extension (Box–Muller) in a utils file:
  - `fun Random.nextGaussian(): Double { val u1 = nextDouble(); val u2 = nextDouble(); return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * kotlin.math.PI * u2) }`
- Replace all usages and add `import kotlin.random.Random` where missing.
- Acceptance:
  - No unresolved `nextGaussian` or missing imports.

## Workstream 6 — Imports & Type Mismatches [FAST]
- Add missing imports (e.g., `kotlin.random.Random`).
- NativeOptimizer numeric types: ensure `Long` fields (e.g., `moveGenerationTime`) use `.toLong()` and consistent types.
- Remove/fix named arguments that don’t exist (e.g., `metrics=`, `resources=`) in constructor calls.
- Acceptance:
  - No remaining “Cannot find parameter …”, “Type mismatch … Int vs Long” compile errors.

## Sequencing
1) Workstream 1: Self‑play model unification (unblocks many analytics errors).
2) Workstream 2: Data‑class consolidation (removes redeclarations/visibility issues).
3) Workstream 3: Agent inheritance removal (final overrides).
4) Workstream 4: Time `expect/actual` cleanup (single source).
5) Workstream 5: Gaussian replacement + imports.
6) Workstream 6: Remaining type/argument fixes.

## Risks / Mitigations
- Risk: Broad refactor breaks tests that assumed old types.
  - Mitigation: Provide mapping helpers and incremental PRs; update tests alongside.
- Risk: Downstream modules rely on duplicate classes.
  - Mitigation: Search and replace imports; keep temporary typealiases if needed.

## Acceptance Criteria (Module‑level)
- `:integration:compileKotlinJvm` succeeds.
- `:integration:jvmTest --tests "*SystemOptimizationBasicTest*"` runs and passes.
- No redeclaration/visibility/final‑override/time API errors in the logs.

## Out of Scope (for this stabilisation)
- Algorithmic fixes (PG log‑prob loss, DQN target sync, masking) — tracked in CHATGPT_NOTES.md.
- Wiring optimizers/monitors into real training — tracked separately in notes.

