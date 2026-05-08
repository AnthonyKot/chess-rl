package com.chessrl.integration

import com.chessrl.integration.adapter.ChessState
import com.chessrl.integration.backend.DualHeadNetwork
import com.chessrl.integration.mcts.MCTSTree
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult
import kotlin.random.Random

/**
 * Chess agent that uses MCTS + DualHeadNetwork for action selection (AlphaZero-style).
 *
 * selectAction() runs MCTS from the current board state and returns the most-visited move.
 * Training is not done per-step — the pipeline calls [trainBatch] with AlphaZero samples
 * collected across full games.
 *
 * Call [setCurrentChessState] before each [selectAction] call so MCTS has access to
 * the full board state (FEN-based), not just the encoded DoubleArray.
 */
class MCTSChessAgent(
    val network: DualHeadNetwork,
    private val mctsTree: MCTSTree,
    private val temperature: Double,    // 1.0 = sample (training), 0.0 = greedy (eval)
    private val config: ChessAgentConfig = ChessAgentConfig()
) : ChessAgent {

    @Volatile private var currentChessState: ChessState? = null

    /** Must be called before selectAction when driving the game loop from AlphaZeroTrainingPipeline. */
    fun setCurrentChessState(state: ChessState) {
        currentChessState = state
    }

    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val chessState = currentChessState
            ?: return validActions.randomOrNull() ?: 0

        val visitCounts = mctsTree.search(chessState, addNoise = temperature > 0.0)
        val policy = mctsTree.visitCountsToPolicy(visitCounts, temperature)
        // Restrict to validActions in case MCTS explored beyond legal set
        val maskedPolicy = DoubleArray(policy.size) { i ->
            if (i in validActions) policy[i] else 0.0
        }
        val sum = maskedPolicy.sum()
        return if (sum > 0.0) {
            val normalized = DoubleArray(maskedPolicy.size) { maskedPolicy[it] / sum }
            mctsTree.sampleAction(normalized)
        } else {
            validActions.randomOrNull() ?: 0
        }
    }

    /**
     * Run MCTS and return both the visit counts and the selected action.
     * Used by AlphaZeroTrainingPipeline to collect training samples.
     */
    fun searchAndSelect(chessState: ChessState, addNoise: Boolean): Pair<IntArray, Int> {
        val visitCounts = mctsTree.search(chessState, addNoise)
        val policy = mctsTree.visitCountsToPolicy(visitCounts, temperature)
        val action = mctsTree.sampleAction(policy)
        return Pair(visitCounts, action)
    }

    // ---- ChessAgent interface (DQN-specific methods are no-ops for MCTS agent) ----

    override fun learn(experience: Experience<DoubleArray, Int>) { /* no-op: batch training only */ }

    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> =
        actions.associateWith { 0.0 }

    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        val chessState = currentChessState ?: return actions.associateWith { 1.0 / actions.size }
        val visitCounts = mctsTree.search(chessState, addNoise = false)
        val policy = mctsTree.visitCountsToPolicy(visitCounts, 0.0)
        return actions.associateWith { policy[it] }
    }

    override fun getTrainingMetrics() = ChessAgentMetrics(
        averageReward = 0.0,
        explorationRate = temperature,
        experienceBufferSize = 0
    )

    override fun forceUpdate() { /* no-op */ }

    override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult =
        PolicyUpdateResult(loss = 0.0, gradientNorm = 0.0, policyEntropy = 0.0)

    override fun save(path: String) = network.save(path)
    override fun load(path: String) = network.load(path)
    override fun reset() { currentChessState = null }
    override fun setExplorationRate(rate: Double) { /* controlled by temperature, not epsilon */ }
    override fun getConfig() = config
}
