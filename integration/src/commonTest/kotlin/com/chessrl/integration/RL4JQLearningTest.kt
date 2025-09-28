package com.chessrl.integration

import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.backend.rl4j.RL4JChessAgent
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.io.File

class RL4JQLearningTest {
    
    @Test
    fun testRL4JQLearningDiscreteDenseCreation() {
        // Skip test if RL4J is not available
        if (!RL4JAvailability.isAvailable() || System.getProperty("enableRL4J") == "false") {
            println("RL4J not available, skipping test")
            return
        }
        
        println("Testing real QLearningDiscreteDense creation")
        
        val config = ChessAgentConfig()
        val agent = RL4JChessAgent(config)

        try {
            // Trigger the RL4J trainer once to ensure the underlying policy is materialized
            agent.trainBatch(emptyList())

            val checkpointPath = "test_qlearning_checkpoint.zip"
            agent.save(checkpointPath)

            val checkpointFile = File(checkpointPath)
            assertTrue(checkpointFile.exists(), "Checkpoint file should be created")
            assertTrue(checkpointFile.length() > 0, "Checkpoint file should not be empty")

            println("Checkpoint file created: ${checkpointFile.absolutePath} (${checkpointFile.length()} bytes)")

            val loadedAgent = RL4JChessAgent(config)
            loadedAgent.load(checkpointPath)
            println("Checkpoint loaded successfully")

            checkpointFile.delete()
            assertFalse(checkpointFile.exists(), "Checkpoint file should be cleaned up")

        } catch (e: Exception) {
            println("RL4J QLearning test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
