package com.chessrl.integration

import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.backend.rl4j.RL4JChessAgent
import kotlin.test.Test
import kotlin.test.assertTrue

class RL4JRealImplementationTest {
    
    @Test
    fun testRL4JRealImplementationInitialization() {
        // Skip test if RL4J is not available
        if (!RL4JAvailability.isAvailable()) {
            println("RL4J not available, skipping test")
            return
        }
        
        println("Testing real RL4J implementation initialization")
        
        val config = ChessAgentConfig()
        val agent = RL4JChessAgent(config)
        
        try {
            agent.initialize()
            println("RL4J agent initialized successfully")
            assertTrue(true, "Agent should initialize without throwing exceptions")
        } catch (e: Exception) {
            println("RL4J agent initialization failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}