package com.chessrl.integration.mcts

import com.chessrl.integration.adapter.ChessState

/**
 * A single node in the MCTS search tree.
 *
 * Each node represents a board position reached by [actionTaken] from [parent].
 * [prior] is the policy network's prior probability P(s,a) for the action that
 * led here, used in the UCB-PUCT selection formula.
 *
 * Values are stored from the perspective of the player to move at this node.
 * Backpropagation negates values at each level to handle the two-player zero-sum structure.
 */
class MCTSNode(
    val state: ChessState,
    var prior: Double,           // var so Dirichlet noise can be applied at root
    val parent: MCTSNode? = null,
    val actionTaken: Int = -1
) {
    var visitCount: Int = 0
    var valueSum: Double = 0.0
    val children: MutableMap<Int, MCTSNode> = mutableMapOf()
    var isExpanded: Boolean = false

    /** Mean value (Q) from this node's perspective; 0 if unvisited. */
    val q: Double get() = if (visitCount == 0) 0.0 else valueSum / visitCount

    /**
     * UCB-PUCT score used by the parent to select this child:
     *   Q(s,a) + c_puct * P(s,a) * sqrt(N_parent) / (1 + N(s,a))
     */
    fun ucbScore(parentVisits: Int, cPuct: Double): Double =
        q + cPuct * prior * sqrt(parentVisits.toDouble()) / (1.0 + visitCount)

    private fun sqrt(x: Double) = kotlin.math.sqrt(x)
}
