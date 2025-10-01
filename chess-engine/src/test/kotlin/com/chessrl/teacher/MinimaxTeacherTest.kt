package com.chessrl.teacher

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import com.chessrl.chess.*

class MinimaxTeacherTest {

    @Test
    fun policyNormalizationAndTopK() {
        val teacher = MinimaxTeacher(depth = 2)
        // Create a simple position (start) to get real scored moves
        val board = ChessBoard()
        val color = board.getActiveColor()
        val scored = teacher.scoreMoves(board, color, searchDepth = 2)
        assertTrue(scored.isNotEmpty(), "Expected at least one scored move")

        // Take a small top-k subset and verify probability normalization and keys
        val k = 5
        val (top, probs) = teacher.chooseWithTemperatureTopK(scored, k, tau = 1.0)
        assertTrue(top.isNotEmpty())
        assertTrue(top.size <= k)
        // Probabilities only for top-k moves
        assertEquals(top.toSet(), probs.keys)
        val sum = probs.values.sum()
        assertTrue(kotlin.math.abs(sum - 1.0) < 1e-6, "Probabilities should sum to 1.0, got $sum")
        // All probabilities positive
        assertTrue(probs.values.all { it > 0.0 })
    }

    @Test
    fun bestMoveIsLegalOnStartPosition() {
        val teacher = MinimaxTeacher(depth = 2)
        val board = ChessBoard()
        val active = board.getActiveColor()
        val legal = GameStateDetector().getAllLegalMoves(board, active)

        val out = teacher.act(board, topK = 5, tau = 1.0)
        // Best move must be legal
        assertTrue(legal.contains(out.bestMove), "Best move must be legal")
        // TopK moves must be subset of legal
        assertTrue(out.topK.all { it in legal })
        // Policy keys must match topK
        assertEquals(out.topK.toSet(), out.policy.keys)
        // Value should be in [-1, 1]
        assertTrue(out.value in -1.0..1.0)
    }

    @Test
    fun terminalCheckmateReturnsTerminalValue() {
        // Reproduce the checkmate setup from GameStateDetectionTest
        val board = ChessBoard()
        board.clearBoard()
        board.setPieceAt(Position(0, 4), Piece(PieceType.KING, PieceColor.WHITE)) // e1
        board.setPieceAt(Position(1, 3), Piece(PieceType.PAWN, PieceColor.WHITE)) // d2
        board.setPieceAt(Position(1, 4), Piece(PieceType.PAWN, PieceColor.WHITE)) // e2
        board.setPieceAt(Position(1, 5), Piece(PieceType.PAWN, PieceColor.WHITE)) // f2
        board.setPieceAt(Position(0, 0), Piece(PieceType.ROOK, PieceColor.BLACK)) // a1

        // Active color is white by default and is checkmated here
        assertEquals(GameStatus.BLACK_WINS, board.getGameStatus())

        val teacher = MinimaxTeacher(depth = 2)
        val out = teacher.act(board, topK = 5, tau = 1.0)
        // Terminal outputs: empty topK/policy and value should be -1 for side to move (white)
        assertTrue(out.topK.isEmpty())
        assertTrue(out.policy.isEmpty())
        assertEquals(-1.0, out.value, 1e-9)
    }

    @Test
    fun terminalStalemateReturnsZeroValue() {
        // Reproduce stalemate setup from GameStateDetectionTest
        val board = ChessBoard()
        board.clearBoard()
        board.setPieceAt(Position(0, 0), Piece(PieceType.KING, PieceColor.WHITE)) // a1
        board.setPieceAt(Position(2, 1), Piece(PieceType.KING, PieceColor.BLACK)) // b3
        board.setPieceAt(Position(1, 2), Piece(PieceType.QUEEN, PieceColor.BLACK)) // c2

        assertEquals(GameStatus.DRAW_STALEMATE, board.getGameStatus())

        val teacher = MinimaxTeacher(depth = 2)
        val out = teacher.act(board)
        assertTrue(out.topK.isEmpty())
        assertTrue(out.policy.isEmpty())
        assertEquals(0.0, out.value, 1e-9)
    }

    @Test
    fun selfPlayE2E_LegalAndTerminatesOrLimits() {
        val teacher = MinimaxTeacher(depth = 2)
        val game = ChessGame()
        val maxPlies = 120
        var ply = 0

        while (ply < maxPlies) {
            val board = game.getCurrentBoard()
            val status = board.getGameStatus()
            if (status.isGameOver) break

            val out = teacher.act(board, topK = 5, tau = 1.0)
            val result = game.makeMove(out.bestMove)
            assertEquals(MoveResult.SUCCESS, result, "Engine selected an illegal/invalid move at ply $ply: ${out.bestMove}")
            ply++
        }

        // Either reached a terminal state, or we hit ply cap
        val finalStatus = game.getCurrentBoard().getGameStatus()
        assertTrue(finalStatus.isGameOver || ply == maxPlies)
    }

    @Test
    fun basicPerformanceSanity_Depth2UnderBudget() {
        // This is a loose sanity check to prevent pathological slowdowns.
        val teacher = MinimaxTeacher(depth = 2)
        val board = ChessBoard() // start position
        val start = System.currentTimeMillis()
        val scores = teacher.scoreMoves(board, board.getActiveColor(), searchDepth = 2)
        val elapsed = System.currentTimeMillis() - start
        // Be generous to avoid flakiness across machines/CI
        assertTrue(elapsed < 3_000, "Depth-2 scoring took too long: ${elapsed}ms")
        assertTrue(scores.isNotEmpty(), "Teacher should return scored moves")
    }
}
