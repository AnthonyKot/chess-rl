package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.backend.RL4JAvailability
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PolicyActionSelectionTest {

    @Test
    fun selectsValidActionWhenRl4jAvailable() {
        if (!RL4JAvailability.isAvailable()) {
            println("⚠️ RL4J not available, skipping real policy selection test")
            return
        }

        val config = ChessAgentConfig()
        val agent = RL4JChessAgent(config)

        val state = DoubleArray(ChessObservation.EXPECTED_DIMENSIONS) { 0.5 }
        val validActions = listOf(0, 1, 2, 3, 4)

        val action = agent.selectAction(state, validActions)
        assertTrue(action in validActions, "Selected action should belong to provided valid actions")
}

    @Test
    fun legalActionMaskTracksMoves() {
        val observation = ChessObservation(DoubleArray(ChessObservation.EXPECTED_DIMENSIONS) { 0.0 })
            .withLegalActions(listOf(0, 5, 123))

        val mask = observation.getLegalActionMask()
        assertNotNull(mask, "Mask should be populated after withLegalActions call")
        assertTrue(mask!!.count { it } == 3, "Mask should mark exactly the provided legal actions")
        assertTrue(mask[0] && mask[5] && mask[123], "Provided actions should be set in the mask")
    }
}
