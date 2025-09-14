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
        // Mobility: difference in number of legal moves
        val sideMoves = legalMoveValidator.getAllLegalMoves(board, side).size
        val oppMoves = legalMoveValidator.getAllLegalMoves(board, side.opposite()).size
        val mobilityScore = (sideMoves - oppMoves) * 0.05
        return materialScore + mobilityScore
    }
}

