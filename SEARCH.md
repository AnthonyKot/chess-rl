# Adding Light Search to ChessRL

## Why Add Search?

- **Catch tactics early**: A shallow search stops blunders (mates-in-1, hanging queens) so the learner isn’t forced to rediscover them from scratch.
- **Better training targets**: Use search results as “teacher” move distributions/value estimates to guide the policy.
- **Cleaner self-play**: Even with a weak policy, each move is vetted, so games are more reasonable and data less noisy.

## Big Picture

We bolt a micro-searcher (depth-1/2 minimax with a fast eval) onto the existing self-play loop. Look-ahead runs in milliseconds, keeping training fast while giving the agent tactical awareness.

## Components

### 1. Search Assistant
- Wrap the existing chess engine or heuristic opponent.
- Provide `SearchAssistant.proposeMove(position, depth=2, timeMs=5–20, policyHint)`.
- Return best move(s) + evaluation (e.g., centipawns).

### 2. Policy Conditioning
- For each move the policy suggests, run search.
- If policy move score < best score – margin → replace with search best (keeps play sane).
- Optionally blend policy and search (ε scheduling).

### 3. Training Targets
- Use search visit counts/value to create a target distribution (e.g., best=0.7, next=0.2, 0.1).
- Train the policy head toward these targets (behaviour cloning + RL loss).
- Store search info in replay buffer transitions for future updates.

### 4. Curriculum Schedule (optional)
- Early: always use search (agent imitates “teacher”).
- Later: decay search usage or threshold so policy takes over more decisions.

## Interaction with Current Pipeline

1. Generate state via self-play.
2. Run policy network.
3. Use `SearchAssistant` to vet/adjust move, optionally mix policy + search.
4. Play move, collect outcomes.
5. Backpropagate using RL loss + search-derived targets.

## Configuration Knobs
- `searchDepth` (default: 2 ply)
- `searchTimeMs` (per move)
- `searchAlpha` (weight for search vs policy logits)
- `searchUsageStrategy` (always / decay / only in critical positions)
- Override threshold (score margin to replace policy move)

## Metrics to Track
- Policy override rate (% moves replaced by search)
- Baseline win rate over time (search on vs off)
- Training time per cycle (with search overhead)
- Loss/entropy curves (does search accelerate convergence?)

## Variations
- Full MCTS (AlphaZero style) if resources allow
- Depth 1 + SEE (static exchange evaluation) for cheap lookahead
- Hybrid: heuristics in early game, deeper search in tactics/endgames
- Probabilistic blend of policy vs search move per turn

## Implementation Steps

1. Implement `SearchAssistant` (engine or heuristic-based).
2. Integrate into self-play loop (override policy moves).
3. Add search-derived targets into training batches.
4. Introduce config flags and logging (override rate, search depth).
5. Benchmark short runs (same seeds) to validate improvement.

## Expected Outcome

Within a single notebook day (~10⁵ positions) the agent should reach ~500 ELO because search prevents catastrophic blunders and provides better training targets.

