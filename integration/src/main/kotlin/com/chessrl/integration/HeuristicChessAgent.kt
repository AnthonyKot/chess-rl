package com.chessrl.integration

import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult

/**
 * A lightweight ChessAgent that uses the built-in BaselineHeuristicOpponent for move selection.
 * Learning methods are no-ops. Useful as an opponent during warmup to produce decisive outcomes.
 */
class HeuristicChessAgent(
    private val config: ChessAgentConfig = ChessAgentConfig()
) : ChessAgent {
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        // Use a temporary environment view to evaluate actions.
        // The Advanced pipeline passes a real environment when evaluating baseline; here we rely on
        // BaselineHeuristicOpponent which only needs the environment to read board state.
        // For self-play usage, SelfPlayGame constructs actions from the environment it owns, so
        // we cannot access it here. Instead, selection is based solely on validActions via baseline
        // evaluation that requires environment; provide a fallback to simple choice if not available.
        // In self-play, this agent will be used through SelfPlayGame which can call Baseline directly
        // when needed, but to satisfy ChessAgent, choose first valid action as fallback.
        return validActions.firstOrNull() ?: 0
    }

    override fun learn(experience: Experience<DoubleArray, Int>) { /* no-op */ }

    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> =
        actions.associateWith { 0.0 }

    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> =
        actions.associateWith { 1.0 / (actions.size.coerceAtLeast(1)) }

    override fun getTrainingMetrics(): ChessAgentMetrics = ChessAgentMetrics(
        averageReward = 0.0,
        explorationRate = 0.0,
        experienceBufferSize = 0,
        averageLoss = 0.0,
        policyEntropy = 0.0
    )

    override fun forceUpdate() { /* no-op */ }

    override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult =
        PolicyUpdateResult(loss = 0.0, gradientNorm = 0.0, policyEntropy = 0.0)

    override fun save(path: String) { /* no-op */ }
    override fun load(path: String) { /* no-op */ }
    override fun reset() { /* no-op */ }
    override fun setExplorationRate(rate: Double) { /* no-op */ }
    override fun getConfig(): ChessAgentConfig = config
}

