package com.chessrl.integration.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Parity tests that ensure both engine adapters expose consistent behaviour
 * for a representative set of positions.
 */
class EngineParityTester {

    private val builtin = BuiltinAdapter()
    private val chesslib = ChesslibAdapter()

    @Test
    fun testStartingPositionParity() {
        val builtinState = builtin.initialState()
        val chesslibState = chesslib.initialState()

        assertEquals(builtinState.fen, chesslibState.fen, "Initial FEN should match")
        assertLegalMoveParity(builtinState)
        assertLegalMoveParity(chesslibState)
        assertEquals(legalMoves(builtin, builtinState), legalMoves(chesslib, chesslibState))
    }

    @Test
    fun testSimpleMoveParity() {
        val move = requireNotNull(ChessMove.fromAlgebraic("e2e4")) { "e2e4 should parse" }

        val builtinAfter = builtin.applyMove(builtin.initialState(), move)
        val chesslibAfter = chesslib.applyMove(chesslib.initialState(), move)

        assertEquals(builtinAfter.fen, chesslibAfter.fen, "FEN after e2e4 should match")
        assertEquals(legalMoves(builtin, builtinAfter), legalMoves(chesslib, chesslibAfter))
    }

    @Test
    fun testItalianGamePositionParity() {
        val fen = "r1bqkbnr/pppppppp/2n5/2b5/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 3"
        assertLegalMoveParity(fen)
    }

    @Test
    fun testEndgamePositionParity() {
        val fen = "8/5k2/8/6K1/5P2/8/8/8 w - - 0 60"
        assertLegalMoveParity(fen)
    }

    @Test
    fun testCheckmateDetectionParity() {
        val fen = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
        assertOutcomeParity(fen, isTerminalExpected = true, expectedReason = "checkmate")
    }

    @Test
    fun testStalemateDetectionParity() {
        val fen = "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"
        assertOutcomeParity(fen, isTerminalExpected = true, expectedReason = "stalemate")
    }

    private fun assertLegalMoveParity(fen: String) {
        val builtinState = requireNotNull(builtin.fromFen(fen)) { "Builtin adapter should parse FEN $fen" }
        val chesslibState = requireNotNull(chesslib.fromFen(fen)) { "Chesslib adapter should parse FEN $fen" }

        val builtinMoves = legalMoves(builtin, builtinState)
        val chesslibMoves = legalMoves(chesslib, chesslibState)
        if (builtinMoves != chesslibMoves) {
            val builtinOnly = (builtinMoves - chesslibMoves.toSet()).sorted()
            val chesslibOnly = (chesslibMoves - builtinMoves.toSet()).sorted()
            fail(
                "Legal move sets should match for FEN $fen. " +
                        "Only in builtin: $builtinOnly; only in chesslib: $chesslibOnly"
            )
        }
    }

    private fun assertOutcomeParity(fen: String, isTerminalExpected: Boolean, expectedReason: String) {
        val builtinState = requireNotNull(builtin.fromFen(fen)) { "Builtin adapter should parse FEN $fen" }
        val chesslibState = requireNotNull(chesslib.fromFen(fen)) { "Chesslib adapter should parse FEN $fen" }

        val builtinOutcome = builtin.getOutcome(builtinState)
        val chesslibOutcome = chesslib.getOutcome(chesslibState)

        assertEquals(isTerminalExpected, builtinOutcome.isTerminal, "Builtin terminal flag mismatch for $fen")
        assertEquals(isTerminalExpected, chesslibOutcome.isTerminal, "Chesslib terminal flag mismatch for $fen")
        assertEquals(builtinOutcome.outcome, chesslibOutcome.outcome, "Outcomes should match for $fen")
        if (isTerminalExpected) {
            assertEquals(expectedReason, builtinOutcome.reason.lowercase(), "Builtin termination reason mismatch for $fen")
            assertEquals(expectedReason, chesslibOutcome.reason.lowercase(), "Chesslib termination reason mismatch for $fen")
        }
    }

    private fun legalMoves(adapter: ChessEngineAdapter, state: ChessState): List<String> {
        return adapter.getLegalMoves(state).map { it.algebraic }.sorted()
    }

    private fun assertLegalMoveParity(state: ChessState) {
        val moves = legalMoves(builtin, state)
        val reference = legalMoves(chesslib, state)
        if (reference != moves) {
            val builtinOnly = (moves - reference.toSet()).sorted()
            val chesslibOnly = (reference - moves.toSet()).sorted()
            fail("Legal move sets differ. Only in builtin: $builtinOnly; only in chesslib: $chesslibOnly")
        }
    }
}
