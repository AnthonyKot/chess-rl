# Deep Q-Network (DQN) for Chess — Theory and Implementation Map

This note explains how canonical DQN works in theory, how we map it to the chess setting in this repo, and where the current implementation deviates from the ideal. It’s written to help you make informed decisions and to revisit details over time.

## 1) Conceptual Overview

Goal: Learn an action-value function Q(s, a) that estimates the expected discounted return when taking action a in state s and following the current policy thereafter.

Key components in “vanilla” DQN:
- Neural network Qθ(s, a) approximates Q-values for all actions given a state.
- Target network Qθ¯(s, a) is a periodically updated copy of Qθ to stabilize targets.
- Experience replay buffer stores (s, a, r, s′, done) tuples to decorrelate updates.
- Bellman target: y = r if done else r + γ max_{a′} Qθ¯(s′, a′)
- Loss: mean-squared-error between Qθ(s, a) and y (or Huber loss for robustness)
- Exploration: typically ε-gredy on Q-values.

## 2) Chess-specific mapping

- State space: 839 features per state (12 piece planes + side to move + castling rights + en passant one‑hot 64 + clocks). Aligned with chess‑engine FeatureEncoding.
- Action space: 4096 indices (fromSquare × toSquare), promotions handled via matching to legal moves
- Sparse/terminal rewards: +1/-1/0 on win/loss/draw; optional position-based shaping; optional step-limit penalty
- Valid action masking is crucial: Most of the 4096 actions are illegal in a given chess position; max over illegal actions corrupts targets.

## 3) Implementation Map (this repo)

Files to know (main pathnames abbreviated):
- rl-framework/RLAlgorithms.kt: DQNAlgorithm (Q-learning, target updates, replay), exploration strategies, metrics types
- integration/RealChessAgentFactory.kt: Builds real DQN agent (FeedforwardNetwork + Adam + Huber + replay), wires target frequency
- integration/ChessTrainingPipeline.kt: Wires next-state masking provider into the agent (agent.setNextActionProvider)
- integration/AdvancedSelfPlayTrainingPipeline.kt: Orchestrates self-play → experience processing → validated batch training → evaluation, checkpoints, rollback
- integration/SelfPlaySystem.kt: (Now) concurrent self-play with per-game threads; produces EnhancedExperience
- integration/ChessEnvironment.kt: Encodes state, validates and executes moves, computes rewards, exposes valid actions
- nn-package/NeuralNetwork.kt: FeedforwardNetwork (layers, training loop, Adam, Huber), model save/load (JVM)

### 3.1 Q-network and target network
- Construction: RealChessAgentFactory.createRealDQNAgent builds two identical FeedforwardNetwork instances for Q and target.
- Optimizer/loss: Adam + HuberLoss(δ=1.0) by default
- Synchronization cadence: targetUpdateFrequency updates (default 100, profile/CLI overridable)
- Copy mechanism: DQNAlgorithm.updateTargetNetwork() via SynchronizableNetwork.copyWeightsTo

### 3.2 Experience replay
- Experience buffers:
  - CircularExperienceBuffer (unseeded) and SeededCircularExperienceBuffer
  - DQNAlgorithm.updatePolicy(experiences) appends new tuples and samples a batch when size ≥ batchSize
- Bellman targets:
  - computeQTargets(batch): uses target network and (importantly) valid-action masking when provided (more below)

### 3.3 Action selection and exploration
- Exploration strategy: EpsilonGreedyStrategy (or seeded variant)
- selectAction: choose argmax Q(s, a) over valid actions with ε-greedy exploration

### 3.4 Valid-action masking (critical for chess)
- Where it’s set: ChessTrainingPipeline.init calls agent.setNextActionProvider(environment::getValidActions)
- DQNAlgorithm.computeQTargets:
  - If provider is set, it queries valid next actions for each s′ and computes max only over those actions.
  - One-time logs confirm masking is active:
    - “🧩 DQN next-state action masking enabled (provider set)”
    - “🧩 DQN masking applied: valid next actions for sample=N”
- Why it matters: prevents illegal-move Q-values from inflating targets and stalling learning.
- Caveat: environment.getValidActions(state) currently ignores the passed DoubleArray and uses the internal board. This is fine for on-policy calls in the same env, but for replay we want true per-state masking. See §6 for improvements.

### 3.5 Training loop in advanced pipeline
- Self-play (concurrent): SelfPlaySystem generates games between main and opponent agents; produces EnhancedExperience (with metadata and chess metrics)
- Experience processing: AdvancedExperienceManager filters/assesses quality; produces training batches
- Validated batch training: AdvancedSelfPlayTrainingPipeline.performValidatedBatchTraining
  - Batches are converted to basic experiences and passed to agent.trainBatch (which calls DQNAlgorithm.updatePolicy)
  - Metrics reported: loss, gradient norm (proxy), entropy; optional validation checks
- Target sync visibility: “🔁 DQN target network synchronized at update=… (freq=…)” shows up after the configured number of policy updates

### 3.6 Checkpointing and retention
- CheckpointManager creates checkpoints regularly and at end; model saved via FeedforwardNetwork.save
- Retention policy: keep best, last N, and optionally every N-th; cleanup metadata-level by default (JVM file deletion could be added)

## 4) Theoretical “ideal” vs current implementation

| Aspect | Ideal DQN | Current status |
| --- | --- | --- |
| Q-network | MLP with stable optimizer & loss | FeedforwardNetwork + Adam + Huber (OK) |
| Target network | Periodically synced | Implemented, freq configurable (OK) |
| Replay | IID batches from large buffer | Circular buffers (OK) |
| Masking | True per-state valid-action masking for s′ | Provider wired; env ignores passed state (needs improvement) |
| Exploration | ε-greedy | Implemented; warmup supported (OK) |
| Rewards | Terminal + optional shaping | Configurable; step-limit penalty supported (OK) |
| Concurrency | Parallel self-play | Thread pool per batch (JVM) (OK) |
| Metrics | True loss/entropy/grad stats | Reported from PolicyUpdateResult (OK) |
| Serialization | Save/load model | Implemented on JVM (OK) |

## 5) Practical guidance (DQN for chess)

- Keep masking enabled (default) and verify logs once per run.
- Use shorter games (e.g., 80–100 max steps) and a small negative step-limit penalty to avoid flat random games.
- Consider reward shaping early (enablePositionRewards=true) to provide signal when decisive results are rare.
- Target update frequency 50–200 is typical; use 20 for quick debug.
- Check loss, entropy, gradient-norm and self-play win/draw rates: expect modest trends, not dramatic changes in a few cycles.
- Concurrency: set maxConcurrentGames near your core count; we synchronize agent calls internally to ensure thread safety.

## 5.1) “Advanced” Training (what the CLI actually does)

Command example
```
./gradlew :integration:runCli -Dargs="--train-advanced --cycles 3 --profile dqn_debug_masking --seed 12345"
```

What happens (one end‑to‑end cycle)
- Phase 1 — Self‑play generation
  - Runs N concurrent self‑play games (thread pool) until the requested total is reached.
  - Produces EnhancedExperience per move with chess metadata and a base quality score.
  - Progress lines show draw/win rates, average game length, and experiences collected.
- Phase 2 — Experience processing
  - Quality assessment and filtering; buffers updated (main/high‑quality/recent) with simple retention rules.
- Phase 3 — Batch training with validation
  - Samples experiences into batches and calls `agent.trainBatch` (DQNAlgorithm.updatePolicy) per batch.
  - Prints per‑batch metrics from the algorithm result (loss, policy entropy, gradient norm; optional EMA and extras).
  - Periodically prints target sync: `🔁 DQN target network synchronized at update=… (freq=…)`.
- Phase 4 — Evaluation (internal)
  - Plays a small number of evaluation games to estimate reward/win/draw/loss and compute a composite performance score.
- Checkpointing
  - Creates an initial checkpoint (v0), regular checkpoints every `checkpointInterval` cycles, and a final checkpoint.
  - Optional retention cleanup at the end (keep best + last K + every Nth) per config/profile.

Why you sometimes see “Cycle 4/3”
- The advanced pipeline treats `--cycles K` as K + 1 iterations with an initial warmup cycle in some configurations. That’s why a 3‑cycle run can display “Advanced Training Cycle 4/3”.

Notes on flags/profiles
- Profiles (profiles.yaml) set defaults for: algorithm, max steps, concurrency, reward shaping, warmups, checkpoint cadence, retention, target sync frequency, and draw reporting for step‑limit.
- CLI flags override profile keys: `--max-steps`, `--concurrency`, `--target-update`, `--checkpoint-interval`, retention flags, etc.
- Masking verification: once per run you should see
  - `🧩 DQN next-state action masking enabled (provider set)`
  - `🧩 DQN masking applied: valid next actions for sample=N`

About `--double-dqn`
- Double‑DQN changes the target to use the online net to select the next action and the target net to evaluate it. Supported here; enable via profile (e.g., `dqn_debug_masking`, `dqn_imitation_bootstrap`) or `--double-dqn`.

## 5.2) How to verify that something was learned

From logs (first few cycles)
- Target syncs: You should see `🔁 … synchronized …` at the configured frequency (e.g., every 50 updates). This confirms updates are flowing.
- Entropy < 8.29: Policy entropy near ln(4096) ≈ 8.29 means uniform. Seeing values lower than that (e.g., ~3.0) indicates non‑uniform preferences emerging. Beware: too low too fast can signal collapse; modest reduction is good.
- Loss variability: Exact scale depends on network/loss; look for non‑flat behavior and mild downward drift over cycles.
- Self‑play stats: With short episodes and shaping, expect many step‑limit draws early. As the policy improves, evaluate vs the baseline heuristic to see improvements independent of self‑play reporting.

Cross‑cycle checks (recommended)
- Baseline evaluation: Run `--eval-baseline --games 20 --load-best` after a few cycles. Compare win/draw/loss to earlier runs.
- Experience buffer quality: Use the analyzer to check reward variance > 0, terminal ratio > 0, and reasonable action diversity.
- Checkpoint diffs: Ensure new checkpoints are being created and the “best model” version eventually advances beyond 0.

If you see 100% draws
- With `maxStepsPerGame=80..100` and `treatStepLimitAsDraw=true`, many early games will report as draws by step limit. That’s OK — verify that
  - Masking logs appear,
  - Batch metrics vary,
  - Baseline evaluation slowly improves.

## 5.3) Metric glossary (as printed)
- loss: The batch loss (Huber/MSE proxy for TD error) aggregated over samples.
- gradient norm: A proxy norm at the network output (or true norm when available) for stability checks.
- policy entropy: Entropy computed from softmax over Q-values or policy logits; lower implies more certainty.
- td (if printed): An estimate of TD error magnitude (implementation‑dependent; not always printed).

## 6) Known gaps and next steps

1) True per-state masking for replay
- Issue: environment.getValidActions(state: DoubleArray) uses internal board; for replayed (s′) it should compute from the vector.
- Options:
  - Decode function: state → board (inverse of encoder) and compute legal moves on the synthetic board.
  - Per-experience mask: store valid-action masks alongside experiences and extend DQNAlgorithm to consume masks in targets.

2) Metrics & monitoring
- Replace simulated performance monitoring with real timings around: NN forward/backward/train, replay sampling, move generation.

3) Compression
- Checkpoints use .json.gz extension but content is JSON. Add actual gzip for JVM saves/loads if desired.

4) Opponent strategy
- Profiles currently copy main weights or use simple strategies. More varied opponents can speed training robustness.

5) Double‑DQN
- Optionally add a Double‑DQN mode (profile/flag) that selects the argmax action with the online Q net and evaluates it with the target net in the target computation.

## 7) Pseudocode (reference)

Canonical DQN (with masking and target net):
```
Initialize Qθ and target Qθ¯; replay buffer D
for each step do
  Observe s, select a=ε-greedy(Qθ(s,·)), execute, get r, s′, done
  D.add(s,a,r,s′,done)
  if |D| ≥ batchSize:
    B ← sample minibatch from D
    for (s,a,r,s′,done) in B:
      if done: y ← r
      else:
        A_valid ← valid_actions(s′)
        y ← r + γ max_{a′∈A_valid} Qθ¯(s′,a′)
      L ← Huber(Qθ(s,a), y)  // or MSE
    θ ← θ - α ∇θ L
  Every N updates: Qθ¯ ← Qθ  // target sync
```

In our pipeline:
- SelfPlaySystem generates Batches of EnhancedExperience via parallel games
- AdvancedExperienceManager processes/filters into minibatches
- performValidatedBatchTraining calls agent.trainBatch (→ DQNAlgorithm.updatePolicy)
- Masking provider gives valid actions for s′ (with the caveat described above)
- Target sync prints a log when triggered

## 8) Quick glossary
- Q-network: Approximator for action-values Q(s,a)
- Target network: Fixed-lag copy of Q-network for stable targets
- Replay buffer: Storage for experience tuples, sampled IID for updates
- Masking: Restrict backups to valid actions, crucial in large, constrained action spaces
- ε-greedy: Choose best action w.p. 1-ε else random valid action
- Huber loss: Robust loss less sensitive to outliers than MSE

---
If you want, I can prototype “per-state” masking via masks embedded in experiences or build the inverse encoder to decode state arrays to ChessBoard for exact legality checks in replay.
## Functional Summary (step-by-step)

- Command: `./gradlew :integration:runCli -Dargs="--train-advanced --cycles 3 --profile dqn_debug_masking --seed 12345"`
- Effects by phase:
  - Phase 1: Self-play generation
    - Runs 20 games with 8 concurrent workers (thread pool).
    - Collects EnhancedExperience tuples; reports progress (win/draw, avg length).
  - Phase 2: Experience processing
    - Computes quality, filters, fills buffers.
  - Phase 3: Batch training with validation
    - Samples experiences into batches; calls `DQNAlgorithm.updatePolicy` (via `agent.trainBatch`) per batch.
    - Prints per-batch metrics: loss, gradient norm (and EMA), policy entropy (and EMA).
    - Prints target sync when frequency is hit: `🔁 DQN target network synchronized at update=40 (freq=20)`.
    - Masking logs (printed once early) confirm valid-action masking for next-state targets:
      - `🧩 DQN next-state action masking enabled (provider set)`
      - `🧩 DQN masking applied: valid next actions for sample=N`
  - Phase 4: Evaluation
    - Runs quick evaluation games; computes a composite performance score.
  - Checkpointing
    - Initial checkpoint v0; regular checkpoints by interval; final checkpoint; optional retention cleanup.
# Heuristic‑Guided Bootstrapping for Chess DQN

Self‑play from scratch in chess is slow. To bootstrap the Q‑network efficiently, we use a simple teacher policy (2–3‑ply minimax with a light evaluation) to generate diverse, high‑quality data and pretrain by imitation before switching to DQN fine‑tuning.

Teacher policy
- Search: 2–3‑ply minimax
- Evaluation: material values, piece‑square tables, mobility, king safety (normalized to a consistent scale)
- Outputs per state s:
  - a*: teacher’s best legal move (action index)
  - top‑k(s): next best moves (e.g., k=3–5)
  - Vθ(s): scalar evaluation for side‑to‑move, normalized to [-1, 1]

Temperature and diversity
- Softmax over teacher move scores with temperature τ: pT(a|s) ∝ exp(score(a)/τ)
- Restrict sampling to top‑k for quality + diversity
- τ in [0.5, 1.5] (anneal down during collection)

Phase A — Data collection (teacher self‑play)
- Teacher vs teacher with temperature sampling; alternate colors, limit episode length
- Sample positions across the game; exclude trivial/no‑progress loops
- Prefer decisive games initially (filter or down‑weight draws)
- Deduplicate by (FEN + side); cap occurrences per position; target 50k–200k unique positions
- Store per sample: state (or FEN), valid‑action mask, pT over valid actions, a*, Vθ(s), meta

Phase B — Supervised imitation pretraining
- Primary loss (policy): cross‑entropy between softmax(Q(s,·)) masked to valid actions and pT(·|s)
- Optional loss (value): MSE between value head (or Q(s,a*)) and Vθ(s) with small weight μ
- Train with masked losses only over valid actions; standard split for train/val
- Metrics: Match@1/Match@k, KL(pT || p̂), (optional) value MSE

Phase C — DQN fine‑tuning (optional shaping)
- Initialize Q‑network from the pretrained weights (keep legal‑action masking and Double DQN)
- Optional potential‑based shaping using teacher eval (safe shaping): r′ = r + λ[γΦ(s′) − Φ(s)], Φ(s)=Vθ(s)
- Optional small imitation bonus δ for picking a* or top‑k(s)
- Anneal λ and δ → 0 over a few cycles to remove teacher bias

Evaluation
- Before/after imitation: Match@k on held‑out set; greedy eval vs baseline
- During DQN: TD error trend, masked entropy on valid actions, win/draw/loss and average game length
- Vs teacher: agent vs greedy teacher for 50–100 games; target ~50%+ after bootstrap

Minimal implementation plan (works with current code)
1) Teacher (minimax+eval): implement 2–3‑ply move scoring and return a*, top‑k, Vθ(s); add τ sampling over top‑k.
2) Data collection mode: teacher self‑play to NDJSON/CSV; deduplicate by (FEN, side); filter draws; cap episodes and per‑position repeats.
3) Imitation pretraining: load dataset; compute valid‑action mask; train masked cross‑entropy over valid actions; save checkpoint.
4) DQN warm‑start: add CLI to load pretrained model and run advanced training with legal‑action masking (and Double DQN enabled).
5) Verification: greedy eval (ε=0) vs baseline and vs teacher; confirm Match@k gain and reduced average game length.

Optional next (after basic bootstrap works)
- Potential‑based shaping (λ annealed) and small match bonus δ (annealed)
- Prioritized replay and/or dueling architecture
- Deeper or alternative teachers (opening books, 3‑ply+ with pruning)

## Teacher Tools (implemented)

- Minimal 2–3 ply minimax teacher and NDJSON dataset generator are available under the chess engine module.
- Command examples:
  - Generate a small dataset (20 games, depth 2, top‑k 5, τ=1.0):
    - `./gradlew :chess-engine:runTeacherCollector -Dargs="--collect --games 20 --depth 2 --topk 5 --tau 1.0 --out data/teacher.ndjson"`
  - Larger run with dedup and limited repeats per FEN:
    - `./gradlew :chess-engine:runTeacherCollector -Dargs="--collect --games 200 --max-plies 160 --depth 2 --topk 5 --tau 0.9 --max-repeats 2 --out data/teacher.ndjson"`

The NDJSON schema per line:
- `fen`: FEN string including side to move
- `side`: `"w"|"b"`
- `best_action`: action index in [0, 4095] via from×64+to mapping
- `top_k`: list of top‑k action indices
- `teacher_policy`: map actionIndex→probability over the top‑k moves (softmax with τ)
- `value`: scalar evaluation in [-1,1] for side to move
- `valid_actions`: list of all legal action indices for the position
- `move`: algebraic long move (e.g., e2e4)
- `game_id`, `ply`, `ts`

You can use this dataset to run imitation pretraining (Phase B) by computing a masked cross‑entropy loss between your model’s softmax over valid actions and `teacher_policy` for those same indices.

### Quick Start: Teacher Bootstrapping

- Collect a small teacher dataset (depth‑2):
  - `./gradlew :chess-engine:runTeacherCollector -Dargs="--collect --games 50 --depth 2 --topk 5 --tau 1.0 --out data/teacher.ndjson"`

- Run imitation pretraining over the dataset and save a checkpoint (with optional label smoothing and train/val split). The default imitation architecture now uses three hidden layers `[512,256,128]` to match DQN defaults:
  - `./gradlew :chess-engine:runImitationTrainer -Dargs="--train-imitation --data data/teacher.ndjson --epochs 3 --batch 64 --lr 0.001 --smooth 0.05 --val-split 0.10 --out data/imitation_qnet.json"`

The imitation trainer prints per-epoch training loss and validation metrics, and finishes with a quick evaluation on a random subset. Metrics include:
- `match@1`: fraction where the model’s best valid action equals the teacher’s best action.
- `KL`: KL(pT || p̂) between teacher policy and the model’s policy over valid actions.

### Expected Results (Teacher Learning)

When the pipeline is wired correctly, you should observe clear learning signals on teacher examples:
- `match@1` significantly above random: random top‑1 is roughly 1/|valid| (typically <5%); after 1–3 epochs on a few thousand samples, expect 40–70% depending on depth, dataset size, and architecture.
- `KL` consistently decreasing across epochs (lower is better), indicating the model’s masked distribution aligns with the teacher.
- Qualitative: lower policy entropy on valid actions compared to uniform and more decisive move preferences in opening and tactical positions.

After imitation, warm‑start DQN fine‑tuning. Two options:

- Use the profile that loads the imitation checkpoint automatically and tunes training for visibility/debugging:
  - `./gradlew :integration:runCli -Dargs="--train-advanced --cycles 3 --profile dqn_imitation_bootstrap"`
- Or pass the checkpoint path explicitly (overrides profile):
  - `./gradlew :integration:runCli -Dargs="--train-advanced --cycles 3 --profile dqn_imitation_bootstrap --load data/imitation_qnet.json"`

In evaluation matches (greedy), the warm‑started agent should perform noticeably better than a reference untrained/baseline model. With a 2‑ply teacher dataset, expect stronger early‑game play and shorter average game lengths versus the reference model; with 3‑ply data these effects should be more pronounced. As DQN fine‑tuning proceeds, the model can further improve beyond the teacher in frequently encountered positions.

### Profiles Overview

- `dqn_imitation_bootstrap`: loads `data/imitation_qnet.json`, sets `hiddenLayers: 512,256,128`, enables position rewards, `targetUpdateFrequency: 20`, and Double‑DQN. Use for faster visible progress when bootstrapping.
- `dqn_debug_masking`: enables Double‑DQN and a shorter target sync to surface masking/sync logs quickly.

Notes:
- Integration and chess‑engine encoders are aligned at 839 inputs. Ensure the imitation model architecture (hidden layers) matches the DQN hidden layers to avoid shape mismatches when loading.
