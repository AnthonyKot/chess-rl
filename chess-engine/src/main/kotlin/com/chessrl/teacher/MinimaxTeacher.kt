package com.chessrl.teacher

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh
import kotlin.random.Random
import com.chessrl.chess.*

/**
 * Minimal 2–3 ply minimax teacher with a lightweight evaluation.
 * - Uses chess-engine move generation and status detection
 * - Returns best action, top-k, soft policy over top-k with temperature, and a scalar value in [-1,1]
 */
class MinimaxTeacher(
    private val depth: Int = 2,
    private val random: Random = Random.Default,
    private val maxCandidates: Int? = null
) {
    data class TeacherOutput(
        val bestMove: Move,
        val topK: List<Move>,
        val policy: Map<Move, Double>,
        val value: Double
    )

    fun evaluatePosition(board: ChessBoard, forColor: PieceColor): Double {
        // Material values
        val materialWeights = mapOf(
            PieceType.PAWN to 1.0,
            PieceType.KNIGHT to 3.2,
            PieceType.BISHOP to 3.25,
            PieceType.ROOK to 5.0,
            PieceType.QUEEN to 9.0,
            PieceType.KING to 0.0
        )

        fun materialScore(color: PieceColor): Double {
            var s = 0.0
            for ((_, p) in board.getPiecesOfColor(color)) {
                s += materialWeights[p.type] ?: 0.0
            }
            return s
        }

        // Simple mobility
        fun mobility(color: PieceColor): Double {
            return board.getAllValidMoves(color).size.toDouble() / 60.0 // rough normalization
        }

        val my = materialScore(forColor) + 0.1 * mobility(forColor)
        val oppColor = if (forColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        val opp = materialScore(oppColor) + 0.1 * mobility(oppColor)

        // Normalize via tanh for bounded value in [-1,1]
        val raw = my - opp
        return tanh(raw / 10.0)
    }

    private fun terminalValue(status: GameStatus, forColor: PieceColor): Double? {
        return when (status) {
            GameStatus.WHITE_WINS -> if (forColor == PieceColor.WHITE) 1.0 else -1.0
            GameStatus.BLACK_WINS -> if (forColor == PieceColor.BLACK) 1.0 else -1.0
            GameStatus.DRAW_STALEMATE, GameStatus.DRAW_INSUFFICIENT_MATERIAL,
            GameStatus.DRAW_FIFTY_MOVE_RULE, GameStatus.DRAW_REPETITION -> 0.0
            else -> null
        }
    }

    private fun minimax(board: ChessBoard, depth: Int, maximizingFor: PieceColor, toMove: PieceColor, alphaIn: Double, betaIn: Double): Double {
        val status = board.getGameStatus()
        val term = terminalValue(status, maximizingFor)
        if (term != null) return term
        if (depth == 0) return evaluatePosition(board, maximizingFor)

        var alpha = alphaIn
        var beta = betaIn
        // Generate and order moves to improve alpha-beta pruning
        val moves = board.getAllValidMoves(toMove).let { list ->
            if (list.isEmpty()) return evaluatePosition(board, maximizingFor)
            // Simple MVV-LVA style ordering with centrality bonus (same heuristic as in scoring)
            val materialWeights = mapOf(
                PieceType.PAWN to 1.0,
                PieceType.KNIGHT to 3.2,
                PieceType.BISHOP to 3.25,
                PieceType.ROOK to 5.0,
                PieceType.QUEEN to 9.0,
                PieceType.KING to 0.0
            )
            fun heuristic(m: Move): Double {
                val mover = board.getPieceAt(m.from)
                val captured = board.getPieceAt(m.to)
                val capScore = (captured?.let { materialWeights[it.type] ?: 0.0 } ?: 0.0) -
                        (mover?.let { (materialWeights[it.type] ?: 0.0) * 0.1 } ?: 0.0)
                val promo = if (m.isPromotion()) 8.0 else 0.0
                val centerDist = (m.to.file - 3.5) * (m.to.file - 3.5) + (m.to.rank - 3.5) * (m.to.rank - 3.5)
                val centerBonus = -0.05 * centerDist
                return capScore + promo + centerBonus
            }
            list.asSequence()
                .map { it to heuristic(it) }
                .sortedByDescending { it.second }
                .map { it.first }
                .toList()
        }
        if (moves.isEmpty()) return evaluatePosition(board, maximizingFor)

        if (toMove == maximizingFor) {
            var best = Double.NEGATIVE_INFINITY
            for (m in moves) {
                // Recurse on a copied board to avoid expensive FEN round-trips
                val child = board.deepCopy()
                child.makeMove(m)
                child.switchActiveColor()
                val score = minimax(child, depth - 1, maximizingFor, if (toMove == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE, alpha, beta)
                best = max(best, score)
                alpha = max(alpha, best)
                if (beta <= alpha) break
            }
            return best
        } else {
            var best = Double.POSITIVE_INFINITY
            for (m in moves) {
                val child = board.deepCopy()
                child.makeMove(m)
                child.switchActiveColor()
                val score = minimax(child, depth - 1, maximizingFor, if (toMove == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE, alpha, beta)
                best = min(best, score)
                beta = min(beta, best)
                if (beta <= alpha) break
            }
            return best
        }
    }

    fun scoreMoves(board: ChessBoard, color: PieceColor, searchDepth: Int = depth): List<Pair<Move, Double>> {
        val allMoves = board.getAllValidMoves(color)
        val moves = if (maxCandidates != null && allMoves.size > maxCandidates) {
            // Simple MVV-LVA style prioritization to trim candidate set
            val materialWeights = mapOf(
                PieceType.PAWN to 1.0,
                PieceType.KNIGHT to 3.2,
                PieceType.BISHOP to 3.25,
                PieceType.ROOK to 5.0,
                PieceType.QUEEN to 9.0,
                PieceType.KING to 0.0
            )
            fun heuristic(m: Move): Double {
                val mover = board.getPieceAt(m.from)
                val captured = board.getPieceAt(m.to)
                val capScore = (captured?.let { materialWeights[it.type] ?: 0.0 } ?: 0.0) -
                        (mover?.let { (materialWeights[it.type] ?: 0.0) * 0.1 } ?: 0.0)
                val promo = if (m.isPromotion()) 8.0 else 0.0
                // Rough centrality bonus for target square
                val centerDist = (m.to.file - 3.5) * (m.to.file - 3.5) + (m.to.rank - 3.5) * (m.to.rank - 3.5)
                val centerBonus = -0.05 * centerDist
                return capScore + promo + centerBonus
            }
            allMoves.asSequence()
                .map { it to heuristic(it) }
                .sortedByDescending { it.second }
                .take(maxCandidates)
                .map { it.first }
                .toList()
        } else allMoves
        if (moves.isEmpty()) return emptyList()
        val scores = ArrayList<Pair<Move, Double>>(moves.size)
        for (m in moves) {
            val child = board.deepCopy()
            child.makeMove(m)
            child.switchActiveColor()
            val score = minimax(child, max(0, searchDepth - 1), color, if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
            scores.add(m to score)
        }
        return scores.sortedByDescending { it.second }
    }

    fun chooseWithTemperatureTopK(scores: List<Pair<Move, Double>>, topK: Int, tau: Double): Pair<List<Move>, Map<Move, Double>> {
        val k = min(topK, scores.size)
        val top = scores.take(k)
        if (top.isEmpty()) return emptyList<Move>() to emptyMap()
        val scaled = top.map { (m, s) -> m to exp(s / tau) }
        val z = scaled.sumOf { it.second }
        val probs = scaled.associate { it.first to (it.second / z) }
        return top.map { it.first } to probs
    }

    fun act(board: ChessBoard, topK: Int = 5, tau: Double = 1.0): TeacherOutput {
        val color = board.getActiveColor()
        val status = board.getGameStatus()
        terminalValue(status, color)?.let { term ->
            // No legal moves or terminal; fabricate output
            val moves = board.getAllValidMoves(color)
            val dummyMove = moves.firstOrNull() ?: Move(Position(0, 0), Position(0, 0))
            return TeacherOutput(dummyMove, emptyList(), emptyMap(), term)
        }
        val scored = scoreMoves(board, color, depth)
        val (tk, probs) = chooseWithTemperatureTopK(scored, topK, tau)
        val best = scored.first().first
        val value = evaluatePosition(board, color)
        return TeacherOutput(best, tk, probs, value)
    }
}

/** Utilities for mapping moves to action indices (4096) */
object ActionIndexMapper {
    fun posIndex(p: Position): Int = p.rank * 8 + p.file
    fun moveToIndex(m: Move): Int = posIndex(m.from) * 64 + posIndex(m.to)
}
