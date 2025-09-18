# Practical Ideas (from docs/ only)

This is a curated, actionable list distilled from the documentation under `docs/` (and subfolders). It omits fluff and focuses on items you can implement or verify directly in this codebase.

## Training Fundamentals
- Batch size: Start at 64; stay within 32–128 depending on memory and stability.
- Optimizer/loss: Adam + HuberLoss for RL stability; ReLU in hidden layers.
- Episode length: Keep max steps modest initially (e.g., 80–100) to avoid long random games and increase reward variance.
- Reward shaping: Use positional rewards early to reduce sparsity; dial down later once the policy starts improving.
- Determinism: Use a single master seed threaded through NN init, exploration, and replay; store/log seeds with checkpoints.

Source: See README.md (Profile-based Training, Diagnostics profile, Evaluation tips, Status Reality Check).

## DQN‑specific
- Target network sync: Use 50–200 updates per sync (100 default). For debug visibility, 20 is fine.
- Valid‑action masking: Ensure next‑state targets are masked to legal actions only; verify via one‑time logs.
- Experience replay: Use a circular buffer; sample MIXED batches (recent + random) to balance stability and recency.

Source: See README.md (Integration Layer examples, Status Reality Check).

## Self‑Play & Evaluation
- Concurrency: Run N concurrent self‑play games ≈ number of CPU cores; synchronize agent calls where needed.
- Baseline evaluation: Periodically measure win/draw/loss vs heuristic baseline independent of self‑play stats.
- Opponent updates: Copy main to opponent on a cadence (e.g., every 3 cycles) or use historical snapshots to avoid overfitting.

Source: See README.md (Integration Layer examples, Non‑NN Evaluation).

## Checkpointing & Retention
- Frequency: Save regular checkpoints every N cycles (e.g., 5). Keep a trailing window to reduce clutter.
- Retention: Keep best, last K, and optionally every N‑th checkpoint; clean up at end of run.
- Metadata: Persist seed and key training parameters with each checkpoint for reproducibility.

Source: See README.md (Best checkpoint resolution, Profile-based Training).

## Performance Optimization (JVM‑first)
- Matmul: Route DenseLayer matmul to a BLAS backend (e.g., netlib‑java with native BLAS) for large speedups.
- Activations: Vectorize activation loops (Java Vector API) or fuse bias+activation passes to reduce memory traffic.
- Memory: Use circular buffers; preallocate batch arrays; avoid per‑sample object churn; monitor GC pressure.
- Throughput: Prefer batched GEMM over many GEMV calls; profile hotspots and adjust batch sizes accordingly.

Source: See README.md (Status Reality Check, Package Overview).

## Monitoring & Troubleshooting
- Metrics to track: loss, entropy, gradient norm; win/draw/loss; avg game length; buffer sizes; system metrics (CPU/mem).
- Timings: Record real timings around NN forward/backward/train, replay sampling, and move gen (min/avg/p95); export to CSV/JSON for comparisons.
- Debugging buffers: Inspect reward sparsity, action diversity, terminal ratios regularly; ensure these aren’t flat.

Source: See README.md (Status Reality Check, Integration Layer).

## Configuration & Profiles
- Profiles: Encode common setups (DQN default, debug, PG default) in YAML; override with CLI flags when needed.
- Knobs to keep handy: maxStepsPerGame, maxConcurrentGames, explorationRate, targetUpdateFrequency, checkpointInterval, retention policy (keepBest, keepLastN, keepEveryN).

Source: See README.md (Profile-based Training and Warm Start).

## Robustness & Safety
- Sanity checks: Validate dimensions, reward finiteness, mask lengths; fail fast on inconsistencies.
- Convergence checks: Watch for loss oscillation, entropy collapse, exploding/vanishing gradients; react by adjusting LR, clipping, batch size.

Source: See README.md (Status Reality Check).

## Suggested Next Steps
- Wire real performance timings (NN forward/backward/train, replay sampling, move gen) into the monitoring path.
- Add a BLAS adapter for DenseLayer matmul on JVM; consider cinterop to OpenBLAS for Kotlin/Native.
- Strengthen per‑state action masking for replay (decode state→board or store per‑experience masks).
- Add opponent variety (historical snapshots, heuristic variations) in evaluation and self‑play.

Source: See README.md (Status Reality Check).
