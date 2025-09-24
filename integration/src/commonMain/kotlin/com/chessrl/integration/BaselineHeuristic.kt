package com.chessrl.integration

import com.chessrl.chess.ChessBoard
import com.chessrl.chess.Piece
import com.chessrl.chess.PieceColor
import com.chessrl.chess.PieceType
import com.chessrl.chess.Position

/**
 * Heuristic opponent that relies on the adapter-provided legal actions.
 * Scores moves based on material, piece-square tables, pawn structure, king safety and mobility.
 */
object BaselineHeuristicOpponent {
    fun selectAction(environment: ChessEnvironment, validActions: List<Int>): Int {
        if (validActions.isEmpty()) return -1

        val sideToMove = environment.getCurrentBoard().getActiveColor()

        var bestAction: Int? = null
        var firstFeasible: Int? = null
        var bestScore = Double.NEGATIVE_INFINITY

        for (action in validActions) {
            val previewBoard = environment.previewBoardAfterAction(action) ?: continue
            if (firstFeasible == null) firstFeasible = action
            val score = evaluateBoard(previewBoard, sideToMove)
            if (score > bestScore) {
                bestScore = score
                bestAction = action
            }
        }

        return bestAction ?: firstFeasible ?: validActions.first()
    }

    private fun evaluateBoard(board: ChessBoard, side: PieceColor): Double {
        val material = evaluateMaterial(board, side) - evaluateMaterial(board, side.opposite())
        val pieceSquare = pieceSquareScore(board, side) - pieceSquareScore(board, side.opposite())
        val pawnStruct = pawnStructureScore(board, side) - pawnStructureScore(board, side.opposite())
        val kingSafety = kingSafetyScore(board, side) - kingSafetyScore(board, side.opposite())
        val mobility = mobilityScore(board, side) - mobilityScore(board, side.opposite())

        return material + pieceSquare + pawnStruct + kingSafety + mobility
    }

    private fun evaluateMaterial(board: ChessBoard, color: PieceColor): Double {
        var score = 0.0
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board.getPieceAt(position(rank, file)) ?: continue
                if (piece.color == color) {
                    score += ChessPositionEvaluator.PIECE_VALUES[piece.type] ?: 0
                }
            }
        }
        return score
    }

    private fun pieceSquareScore(board: ChessBoard, color: PieceColor): Double {
        var total = 0.0
        for (rank in 0..7) {
            for (file in 0..7) {
                val pos = position(rank, file)
                val piece = board.getPieceAt(pos) ?: continue
                if (piece.color != color) continue
                total += pieceSquareValue(piece, pos)
            }
        }
        return total
    }

    private fun pawnStructureScore(board: ChessBoard, color: PieceColor): Double {
        var score = 0.0
        val pawnsByFile = IntArray(8)
        val pawnPositions = mutableListOf<Pair<Int, Int>>()

        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board.getPieceAt(position(rank, file))
                if (piece?.color == color && piece.type == PieceType.PAWN) {
                    pawnsByFile[file]++
                    pawnPositions += rank to file
                }
            }
        }

        for (file in 0..7) {
            val count = pawnsByFile[file]
            if (count > 1) score -= 0.15 * (count - 1)
        }

        for ((rank, file) in pawnPositions) {
            val isolated = (file - 1 !in 0..7 || pawnsByFile[file - 1] == 0) &&
                           (file + 1 !in 0..7 || pawnsByFile[file + 1] == 0)
            if (isolated) score -= 0.12
            if (isPassedPawn(board, rank, file, color)) score += 0.25
        }

        return score
    }

    private fun isPassedPawn(board: ChessBoard, rank: Int, file: Int, color: PieceColor): Boolean {
        val dir = if (color == PieceColor.WHITE) 1 else -1
        var r = rank + dir
        while (r in 0..7) {
            for (df in -1..1) {
                val f = file + df
                if (f !in 0..7) continue
                val piece = board.getPieceAt(position(r, f))
                if (piece?.type == PieceType.PAWN && piece.color != color) return false
            }
            r += dir
        }
        return true
    }

    private fun kingSafetyScore(board: ChessBoard, color: PieceColor): Double {
        val kingPos = findKing(board, color) ?: return -0.5
        val centerDist = (kingPos.rank - 3.5) * (kingPos.rank - 3.5) + (kingPos.file - 3.5) * (kingPos.file - 3.5)
        var score = 0.04 * centerDist

        val dir = if (color == PieceColor.WHITE) 1 else -1
        for (dr in 1..2) {
            val rr = kingPos.rank + dir * dr
            if (rr !in 0..7) continue
            for (df in -1..1) {
                val ff = kingPos.file + df
                if (ff !in 0..7) continue
                val piece = board.getPieceAt(position(rr, ff))
                if (piece?.color == color && piece.type == PieceType.PAWN) score += 0.06
            }
        }
        return score
    }

    private fun mobilityScore(board: ChessBoard, color: PieceColor): Double {
        val moves = board.getAllValidMoves(color).size
        return moves * 0.05
    }

    private fun pieceSquareValue(piece: Piece, pos: Position): Double {
        val r = if (piece.color == PieceColor.WHITE) pos.rank else 7 - pos.rank
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

    private fun position(rank: Int, file: Int) = Position(rank, file)

    private fun findKing(board: ChessBoard, color: PieceColor): Position? {
        for (rank in 0..7) {
            for (file in 0..7) {
                val pos = Position(rank, file)
                val piece = board.getPieceAt(pos)
                if (piece?.type == PieceType.KING && piece.color == color) return pos
            }
        }
        return null
    }

    // reuse PST tables from previous implementation
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
        doubleArrayOf(-0.03, -0.01, 0.0, 0.0, 0.0, 0.0, -0.01, -0.03),
        doubleArrayOf(-0.02, 0.0, 0.01, 0.02, 0.02, 0.01, 0.0, -0.02),
        doubleArrayOf(-0.02, 0.01, 0.02, 0.03, 0.03, 0.02, 0.01, -0.02),
        doubleArrayOf(-0.02, 0.0, 0.02, 0.03, 0.03, 0.02, 0.0, -0.02),
        doubleArrayOf(-0.02, 0.01, 0.01, 0.02, 0.02, 0.01, 0.01, -0.02),
        doubleArrayOf(-0.03, -0.01, 0.0, 0.01, 0.01, 0.0, -0.01, -0.03),
        doubleArrayOf(-0.05, -0.03, -0.02, -0.02, -0.02, -0.02, -0.03, -0.05)
    )
    private val BISHOP_PST = arrayOf(
        doubleArrayOf(-0.02, -0.01, -0.01, -0.01, -0.01, -0.01, -0.01, -0.02),
        doubleArrayOf(-0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.01, 0.01, 0.01, 0.01, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.01, 0.01, 0.01, 0.01, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.01, 0.0, 0.01, 0.01, 0.0, 0.01, -0.01),
        doubleArrayOf(-0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, -0.01),
        doubleArrayOf(-0.02, -0.01, -0.01, -0.01, -0.01, -0.01, -0.01, -0.02)
    )
    private val ROOK_PST = arrayOf(
        doubleArrayOf(0.0, 0.0, 0.0, 0.01, 0.01, 0.0, 0.0, 0.0),
        doubleArrayOf(-0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.01),
        doubleArrayOf(0.01, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.01),
        doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )
    private val QUEEN_PST = arrayOf(
        doubleArrayOf(-0.02, -0.01, -0.01, -0.01, -0.01, -0.01, -0.01, -0.02),
        doubleArrayOf(-0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.01, 0.01, 0.01, 0.01, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.01, 0.01, 0.01, 0.01, 0.0, -0.01),
        doubleArrayOf(-0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, -0.01),
        doubleArrayOf(-0.01, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -0.01),
        doubleArrayOf(-0.02, -0.01, -0.01, -0.01, -0.01, -0.01, -0.01, -0.02)
    )
    private val KING_PST = arrayOf(
        doubleArrayOf(-0.03, -0.04, -0.04, -0.05, -0.05, -0.04, -0.04, -0.03),
        doubleArrayOf(-0.03, -0.04, -0.04, -0.05, -0.05, -0.04, -0.04, -0.03),
        doubleArrayOf(-0.03, -0.04, -0.04, -0.05, -0.05, -0.04, -0.04, -0.03),
        doubleArrayOf(-0.03, -0.04, -0.04, -0.05, -0.05, -0.04, -0.04, -0.03),
        doubleArrayOf(-0.02, -0.03, -0.03, -0.04, -0.04, -0.03, -0.03, -0.02),
        doubleArrayOf(-0.01, -0.02, -0.02, -0.02, -0.02, -0.02, -0.02, -0.01),
        doubleArrayOf(0.02, 0.02, 0.0, 0.0, 0.0, 0.0, 0.02, 0.02),
        doubleArrayOf(0.02, 0.03, 0.01, 0.0, 0.0, 0.01, 0.03, 0.02)
    )
}
