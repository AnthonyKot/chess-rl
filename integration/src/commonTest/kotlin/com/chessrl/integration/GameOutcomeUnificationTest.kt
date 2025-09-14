package com.chessrl.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test to verify GameOutcome enum unification is working correctly
 */
class GameOutcomeUnificationTest {
    
    @Test
    fun testUnifiedGameOutcomeEnum() {
        // Test that all expected values exist
        val outcomes = GameOutcome.values()
        assertEquals(4, outcomes.size)
        
        assertTrue(GameOutcome.WHITE_WINS in outcomes)
        assertTrue(GameOutcome.BLACK_WINS in outcomes)
        assertTrue(GameOutcome.DRAW in outcomes)
        assertTrue(GameOutcome.ONGOING in outcomes)
    }
    
    @Test
    fun testSelfPlayGameResultUsesUnifiedEnum() {
        // Test that SelfPlayGameResult can be created with unified enum
        val metrics = ChessMetrics(
            gameLength = 50,
            totalMaterialValue = 0,
            piecesInCenter = 0,
            developedPieces = 0,
            kingSafetyScore = 0.0,
            moveCount = 50,
            captureCount = 0,
            checkCount = 0
        )
        val result = SelfPlayGameResult(
            gameId = 1,
            gameLength = 50,
            gameOutcome = GameOutcome.WHITE_WINS,
            terminationReason = EpisodeTerminationReason.GAME_ENDED,
            gameDuration = 1000L,
            experiences = emptyList(),
            chessMetrics = metrics,
            finalPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )
        assertEquals(GameOutcome.WHITE_WINS, result.gameOutcome)
        assertEquals(1, result.gameId)
        assertEquals(50, result.gameLength)
    }
}
