package com.chessrl.integration

import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.backend.rl4j.RL4JChessAgent
import com.chessrl.integration.ChessAgentConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class RL4JImplementationStatusTest {
    
    @Test
    fun testRL4JImplementationStatus() {
        // Skip test if RL4J is not available
        if (!RL4JAvailability.isAvailable() || System.getProperty("enableRL4J") == "false") {
            println("RL4J not available, skipping test")
            return
        }
        
        println("=== Testing RL4J Implementation Status ===")

        val config = ChessAgentConfig()
        val agent = RL4JChessAgent(config)

        val update = agent.trainBatch(emptyList())
        println("✅ RL4J agent completed native training call: loss=${update.loss}")

        val policy = agent.getRL4JPolicy()
        assertTrue(policy != null, "RL4J agent should expose a real DQN policy")

        val metrics = agent.getTrainingMetrics()
        assertTrue(metrics.episodeCount > 0, "Training metrics should include at least one episode")
        println("📊 RL4J metrics: episodes=${metrics.episodeCount}, avgReward=${metrics.averageReward}, epsilon=${metrics.explorationRate}")
    }
}
