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
        val result = SelfPlayGameResult(
            gameId = 1,
            gameLength = 50,
            gameOutcome = GameOutcome.WHITE_WINS,
            terminationReason = EpisodeTerminationReason.GAME_ENDED,
            gameDuration = 1000L,
            experiences = emptyList(),
            chessMetrics = ChessMetrics(),
            finalPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )
        
        assertEquals(GameOutcome.WHITE_WINS, result.gameOutcome)
        assertEquals(1, result.gameId)
        assertEquals(50, result.gameLength)
    }
    
    @Test
    fun testGameResultToSelfPlayGameResultConversion() {
        // Test that GameResult can be converted to SelfPlayGameResult
        val gameResult = GameResult(
            gameId = 2,
            moves = emptyList(),
            outcome = GameOutcome.BLACK_WINS,
            moveCount = 30,
            duration = 2000L,
            terminationReason = EpisodeTerminationReason.GAME_ENDED,
            finalPosition = "test_position"
        )
        
        val selfPlayResult = gameResult.toSelfPlayGameResult()
        assertEquals(GameOutcome.BLACK_WINS, selfPlayResult.gameOutcome)
        assertEquals(2, selfPlayResult.gameId)
        assertEquals(30, selfPlayResult.gameLength)
        assertEquals("test_position", selfPlayResult.finalPosition)
    }
}