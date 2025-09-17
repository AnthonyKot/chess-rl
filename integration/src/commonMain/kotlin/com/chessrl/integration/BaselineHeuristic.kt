package com.chessrl.integration

import com.chessrl.chess.*
import kotlin.math.abs

/**
 * Simple baseline heuristic opponent for evaluation: material + mobility.
 * - Material: standard piece values
 * - Mobility: legal moves count (small weight)
 */
object BaselineHeuristicOpponent {
    private val actionEncoder = ChessActionEncoder()
    private val legalMoveValidator = LegalMoveValidator()

    fun selectAction(environment: ChessEnvironment, validActions: List<Int>): Int {
        if (validActions.isEmpty()) return -1
        val board = environment.getCurrentBoard()
        val sideToMove = board.getActiveColor()
        val legalMoves = legalMoveValidator.getAllLegalMoves(board, sideToMove)
        if (legalMoves.isEmpty()) return validActions.first()

        var bestAction = validActions.first()
        var bestScore = Double.NEGATIVE_INFINITY

        for (a in validActions) {
            val move = decodeToLegalMove(a, legalMoves) ?: continue
            val score = evaluateMove(board, move, sideToMove)
            if (score > bestScore) {
                bestScore = score
                bestAction = a
            }
        }
        return bestAction
    }

    private fun decodeToLegalMove(actionIndex: Int, legalMoves: List<Move>): Move? {
        val base = actionEncoder.decodeAction(actionIndex)
        // Try exact match without promotion
        legalMoves.find { it.from == base.from && it.to == base.to && it.promotion == null }?.let { return it }
        // Prefer queen promotion if any
        legalMoves.find { it.from == base.from && it.to == base.to && it.promotion == PieceType.QUEEN }?.let { return it }
        // Any promotion match
        return legalMoves.find { it.from == base.from && it.to == base.to }
    }

    private fun evaluateMove(board: ChessBoard, move: Move, side: PieceColor): Double {
        val copy = board.copy()
        val result = copy.makeLegalMove(move)
        if (result != MoveResult.SUCCESS) return Double.NEGATIVE_INFINITY
        // After move, switch active color as environment does
        copy.switchActiveColor()
        return evaluateBoard(copy, side)
    }

    private fun evaluateBoard(board: ChessBoard, side: PieceColor): Double {
        // Material
        var materialSide = 0
        var materialOpp = 0
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board.getPieceAt(Position(rank, file)) ?: continue
                val value = ChessPositionEvaluator.PIECE_VALUES[piece.type] ?: 0
                if (piece.color == side) materialSide += value else materialOpp += value
            }
        }
        val materialScore = (materialSide - materialOpp).toDouble()

        // Piece-square tables (middlegame-ish, small weights)
        var pstSide = 0.0
        var pstOpp = 0.0
        for (rank in 0..7) {
            for (file in 0..7) {
                val pos = Position(rank, file)
                val piece = board.getPieceAt(pos) ?: continue
                val v = pieceSquareValue(piece, pos)
                if (piece.color == side) pstSide += v else pstOpp += v
            }
        }
        val pstScore = pstSide - pstOpp

        // Pawn structure heuristics
        val pawnStructScore = pawnStructureScore(board, side) - pawnStructureScore(board, side.opposite())

        // King safety
        val kingSafetyScore = kingSafety(board, side) - kingSafety(board, side.opposite())

        // Mobility (small weight)
        val sideMoves = legalMoveValidator.getAllLegalMoves(board, side).size
        val oppMoves = legalMoveValidator.getAllLegalMoves(board, side.opposite()).size
        val mobilityScore = (sideMoves - oppMoves) * 0.05

        return materialScore + pstScore + pawnStructScore + kingSafetyScore + mobilityScore
    }

    // --- Heuristics helpers ---

    private fun mirrorRankForBlack(rank: Int): Int = 7 - rank

    private fun pieceSquareValue(piece: Piece, pos: Position): Double {
        val r = if (piece.color == PieceColor.WHITE) pos.rank else mirrorRankForBlack(pos.rank)
        val f = pos.file
        return when (piece.type) {
            PieceType.PAWN -> PAWN_PST[r][f]
            PieceType.KNIGHT -> KNIGHT_PST[r][f]
            PieceType.BISHOP -> BISHOP_PST[r][f]
            PieceType.ROOK -> ROOK_PST[r][f]
            PieceType.QUEEN -> QUEEN_PST[r][f]
            PieceType.KING -> KING_PST[r][f]
        }
    }

    private fun pawnStructureScore(board: ChessBoard, color: PieceColor): Double {
        // Simple: penalties for doubled/isolated pawns, bonus for passed pawns
        var score = 0.0
        // Count pawns per file
        val pawnsByFile = IntArray(8)
        val pawnPositions = mutableListOf<Position>()
        for (rank in 0..7) {
            for (file in 0..7) {
                val p = board.getPieceAt(Position(rank, file))
                if (p?.color == color && p.type == PieceType.PAWN) {
                    pawnsByFile[file]++
                    pawnPositions += Position(rank, file)
                }
            }
        }
        // Doubled pawns
        for (file in 0..7) {
            val count = pawnsByFile[file]
            if (count > 1) score -= 0.15 * (count - 1)
        }
        // Isolated & passed pawns
        for (pos in pawnPositions) {
            val f = pos.file
            val isolated = (f - 1 < 0 || pawnsByFile[f - 1] == 0) && (f + 1 > 7 || pawnsByFile[f + 1] == 0)
            if (isolated) score -= 0.12
            if (isPassedPawn(board, pos, color)) score += 0.25
        }
        return score
    }

    private fun isPassedPawn(board: ChessBoard, pos: Position, color: PieceColor): Boolean {
        val dir = if (color == PieceColor.WHITE) 1 else -1
        val endRank = if (color == PieceColor.WHITE) 7 else 0
        var r = pos.rank + dir
        while (if (dir > 0) r <= endRank else r >= endRank) {
            for (df in -1..1) {
                val f = pos.file + df
                if (f !in 0..7) continue
                val p = board.getPieceAt(Position(r, f))
                if (p?.type == PieceType.PAWN && p.color != color) return false
            }
            r += dir
        }
        return true
    }

    private fun kingSafety(board: ChessBoard, color: PieceColor): Double {
        // Penalize king near center, reward pawn shield
        val kingPos = findKing(board, color) ?: return -0.5
        val centerDistSq = (kingPos.rank - 3.5) * (kingPos.rank - 3.5) + (kingPos.file - 3.5) * (kingPos.file - 3.5)
        var score = 0.04 * centerDistSq // further from center is slightly better in middlegame

        // Pawn shield: pawns in front of king (one or two ranks ahead for white; behind for black)
        val dir = if (color == PieceColor.WHITE) 1 else -1
        for (dr in 1..2) {
            val rr = kingPos.rank + dir * dr
            if (rr !in 0..7) continue
            for (df in -1..1) {
                val ff = kingPos.file + df
                if (ff !in 0..7) continue
                val p = board.getPieceAt(Position(rr, ff))
                if (p?.color == color && p.type == PieceType.PAWN) score += 0.06
            }
        }
        return score
    }

    private fun findKing(board: ChessBoard, color: PieceColor): Position? {
        for (rank in 0..7) for (file in 0..7) {
            val pos = Position(rank, file)
            val p = board.getPieceAt(pos)
            if (p?.color == color && p.type == PieceType.KING) return pos
        }
        return null
    }

    // --- Simple piece-square tables (values in ~[-0.3, +0.3]) ---
    private val PAWN_PST = arrayOf(
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.05, 0.06, 0.06, 0.08, 0.08, 0.06, 0.06, 0.05),
        doubleArrayOf(0.02, 0.03, 0.04, 0.06, 0.06, 0.04, 0.03, 0.02),
        doubleArrayOf(0.01, 0.02, 0.03, 0.05, 0.05, 0.03, 0.02, 0.01),
        doubleArrayOf(0.0, 0.01, 0.02, 0.04, 0.04, 0.02, 0.01, 0.0),
        doubleArrayOf(0.0, 0.0, 0.01, 0.02, 0.02, 0.01, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.01, 0.01, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, -0.01, -0.01, 0.0, 0.0, 0.0)
    )
    private val KNIGHT_PST = arrayOf(
        doubleArrayOf(-0.05, -0.03, -0.02, -0.02, -0.02, -0.02, -0.03, -0.05),
        doubleArrayOf(-0.03, 0.0, 0.01, 0.02, 0.02, 0.01, 0.0, -0.03),
        doubleArrayOf(-0.02, 0.01, 0.03, 0.04, 0.04, 0.03, 0.01, -0.02),
        doubleArrayOf(-0.02, 0.02, 0.04, 0.05, 0.05, 0.04, 0.02, -0.02),
        doubleArrayOf(-0.02, 0.01, 0.04, 0.05, 0.05, 0.04, 0.01, -0.02),
        doubleArrayOf(-0.02, 0.01, 0.03, 0.04, 0.04, 0.03, 0.01, -0.02),
        doubleArrayOf(-0.03, 0.0, 0.01, 0.02, 0.02, 0.01, 0.0, -0.03),
        doubleArrayOf(-0.05, -0.03, -0.02, -0.02, -0.02, -0.02, -0.03, -0.05)
    )
    private val BISHOP_PST = arrayOf(
        doubleArrayOf(-0.02, -0.01, -0.01, -0.01, -0.01, -0.01, -0.01, -0.02),
        doubleArrayOf(-0.01, 0.01, 0.02, 0.03, 0.03, 0.02, 0.01, -0.01),
        doubleArrayOf(-0.01, 0.02, 0.03, 0.04, 0.04, 0.03, 0.02, -0.01),
        doubleArrayOf(-0.01, 0.02, 0.03, 0.04, 0.04, 0.03, 0.02, -0.01),
        doubleArrayOf(-0.01, 0.02, 0.03, 0.04, 0.04, 0.03, 0.02, -0.01),
        doubleArrayOf(-0.01, 0.02, 0.03, 0.04, 0.04, 0.03, 0.02, -0.01),
        doubleArrayOf(-0.01, 0.01, 0.02, 0.03, 0.03, 0.02, 0.01, -0.01),
        doubleArrayOf(-0.02, -0.01, -0.01, -0.01, -0.01, -0.01, -0.01, -0.02)
    )
    private val ROOK_PST = arrayOf(
        doubleArrayOf(0.0, 0.0, 0.01, 0.02, 0.02, 0.01, 0.0, 0.0),
        doubleArrayOf(0.02, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.02),
        doubleArrayOf(0.01, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.01),
        doubleArrayOf(0.01, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.01),
        doubleArrayOf(0.0, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.0),
        doubleArrayOf(0.0, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )
    private val QUEEN_PST = arrayOf(
        doubleArrayOf(0.0, 0.01, 0.01, 0.02, 0.02, 0.01, 0.01, 0.0),
        doubleArrayOf(0.01, 0.02, 0.02, 0.03, 0.03, 0.02, 0.02, 0.01),
        doubleArrayOf(0.01, 0.02, 0.03, 0.03, 0.03, 0.03, 0.02, 0.01),
        doubleArrayOf(0.02, 0.03, 0.03, 0.04, 0.04, 0.03, 0.03, 0.02),
        doubleArrayOf(0.01, 0.02, 0.03, 0.03, 0.03, 0.03, 0.02, 0.01),
        doubleArrayOf(0.01, 0.02, 0.02, 0.03, 0.03, 0.02, 0.02, 0.01),
        doubleArrayOf(0.0, 0.01, 0.02, 0.02, 0.02, 0.02, 0.01, 0.0),
        doubleArrayOf(0.0, 0.0, 0.01, 0.02, 0.02, 0.01, 0.0, 0.0)
    )
    private val KING_PST = arrayOf(
        doubleArrayOf(0.2, 0.22, 0.22, 0.22, 0.22, 0.22, 0.22, 0.2),
        doubleArrayOf(0.18, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.18),
        doubleArrayOf(0.1, 0.12, 0.12, 0.12, 0.12, 0.12, 0.12, 0.1),
        doubleArrayOf(0.06, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.06),
        doubleArrayOf(0.03, 0.04, 0.05, 0.06, 0.06, 0.05, 0.04, 0.03),
        doubleArrayOf(0.02, 0.03, 0.04, 0.05, 0.05, 0.04, 0.03, 0.02),
        doubleArrayOf(0.01, 0.02, 0.03, 0.04, 0.04, 0.03, 0.02, 0.01),
        doubleArrayOf(0.0, 0.01, 0.02, 0.03, 0.03, 0.02, 0.01, 0.0)
    )

}
