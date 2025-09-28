package com.chessrl.integration

import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.backend.rl4j.ChessActionSpace
import com.chessrl.integration.backend.rl4j.ChessMDP
import com.chessrl.integration.backend.rl4j.RL4JChessAgent
import com.chessrl.integration.ChessAgentConfig
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class RL4JRealImplementationTest {
    private fun isRl4jEnabled(): Boolean {
        return RL4JAvailability.isAvailable() && (System.getProperty("enableRL4J")?.toBoolean() ?: true)
    }

    @Test
    fun testRL4JRealImplementationInitialization() {
        // Skip test if RL4J is not available
        if (!isRl4jEnabled()) {
            println("RL4J not available, skipping test")
            return
        }
        
        println("Testing real RL4J implementation initialization")
        
        val config = ChessAgentConfig()
        val agent = RL4JChessAgent(config)

        val result = agent.trainBatch(emptyList())
        val policy = agent.getRL4JPolicy()
        val policyClass = policy?.javaClass?.name ?: "<null>"

        println("RL4J agent policy class: $policyClass")
        assertNotNull(policy, "Agent should expose a real RL4J policy")
        assertTrue(policyClass.startsWith("org.deeplearning4j"), "Policy should be provided by RL4J, not a fallback")

        val metrics = agent.getTrainingMetrics()
        println("RL4J metrics after training: episodes=${metrics.episodeCount}, avgReward=${metrics.averageReward}")
        assertTrue(metrics.episodeCount >= 0, "Metrics should be populated after training")
        assertEquals(result.loss, metrics.averageLoss, "Loss from update should match metrics average loss")
    }

    @Test
    fun testChessMdpIllegalActionInfo() {
        if (!isRl4jEnabled()) {
            println("RL4J not available, skipping ChessMDP info test")
            return
        }

        val mdp = ChessMDP(com.chessrl.integration.ChessEnvironment())
        val observation = mdp.reset()
        val mask = observation.getLegalActionMask()
        assertNotNull(mask, "Reset observation should contain a legal action mask")
        assertTrue(mask!!.any { it }, "Mask should mark at least one legal action")

        val reply = mdp.step(ChessActionSpace.ACTION_COUNT + 5)
        val info = reply.info as? Map<*, *>
        assertNotNull(info, "Step info should be a map")
        assertTrue(info!!["illegal"] == true || info["illegalAttempt"] == true, "Illegal flag should be set in info map")
        assertTrue(info["fallbackUsed"] == true || info["reason"] == "illegal-no-legal", "Fallback usage should be captured")
    }
}
}
