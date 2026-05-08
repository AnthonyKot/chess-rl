package com.chessrl.integration.mcts

import com.chessrl.chess.ChessBoard
import com.chessrl.chess.PieceColor
import com.chessrl.integration.ChessStateEncoder
import com.chessrl.integration.adapter.ActionSpaceMapping
import com.chessrl.integration.adapter.ChessEngineAdapter
import com.chessrl.integration.adapter.ChessState
import com.chessrl.integration.adapter.GameOutcome
import com.chessrl.integration.backend.DualHeadNetwork
import kotlin.math.*
import kotlin.random.Random

/**
 * Monte Carlo Tree Search with a dual-head neural network (AlphaZero-style).
 *
 * Selection uses UCB-PUCT. Expansion calls the network for prior probabilities and
 * a position value, replacing random rollouts entirely. Backpropagation negates the
 * value at each level to handle the two-player zero-sum structure.
 *
 * Dirichlet noise is added to root priors during training to ensure exploration
 * even when the policy becomes confident.
 */
class MCTSTree(
    private val network: DualHeadNetwork,
    private val adapter: ChessEngineAdapter,
    private val simulations: Int,
    private val cPuct: Double,
    private val dirichletAlpha: Double = 0.3,
    private val dirichletEpsilon: Double = 0.25,
    private val random: Random = Random.Default
) {
    private val encoder = ChessStateEncoder()

    /**
     * Run MCTS from [rootState] and return a visit-count array of size 4096.
     * [addNoise] should be true during self-play training, false during evaluation.
     */
    fun search(rootState: ChessState, addNoise: Boolean): IntArray {
        val root = MCTSNode(rootState, prior = 1.0)
        expandNode(root)

        if (addNoise && root.children.isNotEmpty()) {
            addDirichletNoise(root)
        }

        repeat(simulations) {
            var node = root
            val path = mutableListOf<MCTSNode>()

            // SELECT: walk down the tree choosing children by UCB-PUCT
            while (node.isExpanded && node.children.isNotEmpty()) {
                path.add(node)
                node = selectChild(node)
            }

            // EXPAND + EVALUATE
            val value = if (adapter.isTerminal(node.state)) {
                terminalValue(node.state)
            } else {
                expandNode(node)
                networkValue(node.state)
            }

            // BACKPROPAGATE: alternate sign per level (opponent's gain = our loss)
            var v = value
            node.visitCount++
            node.valueSum += v
            for (parent in path.reversed()) {
                v = -v
                parent.visitCount++
                parent.valueSum += v
            }
        }

        return visitCountArray(root)
    }

    /**
     * Convert visit counts to a policy distribution (temperature controls sharpness).
     * temperature=1.0 → proportional to visits; temperature=0.0 → argmax (greedy).
     */
    fun visitCountsToPolicy(visitCounts: IntArray, temperature: Double): DoubleArray {
        if (temperature == 0.0) {
            val best = visitCounts.indices.maxByOrNull { visitCounts[it] } ?: 0
            return DoubleArray(visitCounts.size).also { it[best] = 1.0 }
        }
        val raised = DoubleArray(visitCounts.size) { visitCounts[it].toDouble().pow(1.0 / temperature) }
        val sum = raised.sum().coerceAtLeast(1e-10)
        return DoubleArray(raised.size) { raised[it] / sum }
    }

    /**
     * Sample an action index from a policy distribution.
     */
    fun sampleAction(policy: DoubleArray): Int {
        var r = random.nextDouble()
        for (i in policy.indices) {
            r -= policy[i]
            if (r <= 0.0) return i
        }
        return policy.indices.maxByOrNull { policy[it] } ?: 0
    }

    // ---- private helpers ----

    private fun selectChild(node: MCTSNode): MCTSNode =
        node.children.values.maxByOrNull { it.ucbScore(node.visitCount, cPuct) }!!

    private fun expandNode(node: MCTSNode) {
        if (node.isExpanded) return
        node.isExpanded = true

        val legalMoves = adapter.getLegalMoves(node.state)
        if (legalMoves.isEmpty()) return

        val encoded = encodeState(node.state)
        val (policyLogits, _) = network.forwardDual(encoded)

        // Softmax over all 4096 logits, then redistribute mass to legal moves only
        val probs = softmax(policyLogits)
        val legalActions = legalMoves.map { ActionSpaceMapping.encodeMove(it) }
        val legalMass = legalActions.sumOf { probs[it] }.coerceAtLeast(1e-10)

        for (move in legalMoves) {
            val action = ActionSpaceMapping.encodeMove(move)
            val prior = probs[action] / legalMass
            val childState = adapter.applyMove(node.state, move)
            node.children[action] = MCTSNode(childState, prior, parent = node, actionTaken = action)
        }
    }

    private fun networkValue(state: ChessState): Double {
        val encoded = encodeState(state)
        val (_, value) = network.forwardDual(encoded)
        return value  // tanh output ∈ [-1, 1], from current player's POV
    }

    private fun terminalValue(state: ChessState): Double {
        val outcome = adapter.getOutcome(state).outcome
        return when (outcome) {
            GameOutcome.WHITE_WINS -> if (state.activeColor == PieceColor.WHITE) 1.0 else -1.0
            GameOutcome.BLACK_WINS -> if (state.activeColor == PieceColor.BLACK) 1.0 else -1.0
            else -> 0.0
        }
    }

    private fun encodeState(state: ChessState): DoubleArray {
        val board = ChessBoard()
        board.fromFEN(state.fen)
        return encoder.encode(board)
    }

    private fun addDirichletNoise(root: MCTSNode) {
        val noise = sampleDirichlet(root.children.size, dirichletAlpha)
        root.children.values.forEachIndexed { i, child ->
            child.prior = (1.0 - dirichletEpsilon) * child.prior + dirichletEpsilon * noise[i]
        }
    }

    private fun visitCountArray(root: MCTSNode): IntArray {
        val counts = IntArray(ActionSpaceMapping.ACTION_SPACE_SIZE)
        for ((action, child) in root.children) {
            counts[action] = child.visitCount
        }
        return counts
    }

    private fun softmax(logits: DoubleArray): DoubleArray {
        val max = logits.maxOrNull() ?: 0.0
        val exp = DoubleArray(logits.size) { exp(logits[it] - max) }
        val sum = exp.sum().coerceAtLeast(1e-10)
        return DoubleArray(logits.size) { exp[it] / sum }
    }

    private fun sampleDirichlet(n: Int, alpha: Double): DoubleArray {
        val samples = DoubleArray(n) { sampleGamma(alpha) }
        val sum = samples.sum().coerceAtLeast(1e-10)
        return DoubleArray(n) { samples[it] / sum }
    }

    // Marsaglia-Tsang method for Gamma(alpha, 1) with alpha < 1 reduction
    private fun sampleGamma(alpha: Double): Double {
        if (alpha < 1.0) {
            return sampleGamma(alpha + 1.0) * random.nextDouble().pow(1.0 / alpha)
        }
        val d = alpha - 1.0 / 3.0
        val c = 1.0 / sqrt(9.0 * d)
        while (true) {
            var x: Double
            var v: Double
            do {
                x = nextGaussian()
                v = 1.0 + c * x
            } while (v <= 0.0)
            v = v * v * v
            val u = random.nextDouble()
            if (u < 1.0 - 0.0331 * x * x * x * x) return d * v
            if (ln(u) < 0.5 * x * x + d * (1.0 - v + ln(v))) return d * v
        }
    }

    private fun nextGaussian(): Double {
        val u1 = random.nextDouble().coerceAtLeast(1e-10)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
    }
}
