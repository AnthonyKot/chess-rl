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
        if (!RL4JAvailability.isAvailable()) {
            println("RL4J not available, skipping test")
            return
        }
        
        println("Testing real QLearningDiscreteDense creation")
        
        val config = ChessAgentConfig()
        val agent = RL4JChessAgent(config)
        
        try {
            agent.initialize()
            println("RL4J agent initialized successfully")
            
            // Test save functionality
            val checkpointPath = "test_qlearning_checkpoint.zip"
            agent.save(checkpointPath)
            
            // Verify file was created
            val checkpointFile = File(checkpointPath)
            assertTrue(checkpointFile.exists(), "Checkpoint file should be created")
            assertTrue(checkpointFile.length() > 0, "Checkpoint file should not be empty")
            
            println("Checkpoint file created: ${checkpointFile.absolutePath} (${checkpointFile.length()} bytes)")
            
            // Test load functionality
            val loadedAgent = RL4JChessAgent(config)
            loadedAgent.load(checkpointPath)
            println("Checkpoint loaded successfully")
            
            // Clean up
            checkpointFile.delete()
            assertFalse(checkpointFile.exists(), "Checkpoint file should be cleaned up")
            
        } catch (e: Exception) {
            println("RL4J QLearning test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}