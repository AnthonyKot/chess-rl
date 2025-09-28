package com.chessrl.integration.backend

import com.chessrl.integration.backend.rl4j.RL4JLearningBackend
import com.chessrl.integration.config.ChessRLConfig
import kotlin.test.*

/**
 * Test CLI backend integration to ensure --nn rl4j creates RL4J backend.
 */
class CLIBackendIntegrationTest {
    
    @Test
    fun testBackendFactoryCreatesRL4JBackend() {
        // Given RL4J is available and we request RL4J backend
        if (!RL4JAvailability.isAvailable()) {
            println("⚠️ RL4J not available, skipping CLI integration test")
            return
        }
        
        val config = ChessRLConfig()
        
        // When we create an RL4J backend through the factory
        val backend = BackendFactory.createBackend(BackendType.RL4J, config)
        
        // Then we should get an RL4JLearningBackend instance
        assertTrue(backend is RL4JLearningBackend, "Expected RL4JLearningBackend, got ${backend::class.simpleName}")
        
        println("✅ BackendFactory correctly creates RL4JLearningBackend for RL4J backend type")
    }
    
    @Test
    fun testBackendFactoryFailsForUnavailableBackend() {
        // Given we have a config
        val config = ChessRLConfig()
        
        // When RL4J is not available and we request RL4J backend
        if (RL4JAvailability.isAvailable()) {
            println("⚠️ RL4J is available, skipping unavailable backend test")
            return
        }
        
        // Then we should get an exception
        assertFailsWith<IllegalStateException> {
            BackendFactory.createBackend(BackendType.RL4J, config)
        }
        
        println("✅ BackendFactory correctly fails when RL4J backend is not available")
    }
    
    @Test
    fun testBackendSelectorParsesRL4JFromArgs() {
        // Given CLI args with --nn rl4j
        val args = arrayOf("--train", "--nn", "rl4j", "--profile", "test")
        
        // When we parse the backend from args
        val backendType = BackendSelector.parseBackendFromArgs(args)
        
        // Then we should get RL4J backend type
        assertEquals(BackendType.RL4J, backendType)
        
        println("✅ BackendSelector correctly parses RL4J from CLI args")
    }
    
    @Test
    fun testCLIIntegrationFlow() {
        // Given RL4J is available
        if (!RL4JAvailability.isAvailable()) {
            println("⚠️ RL4J not available, skipping CLI integration flow test")
            return
        }
        
        // Simulate the CLI flow: parse args -> create backend
        val args = arrayOf("--train", "--nn", "rl4j", "--profile", "test")
        val config = ChessRLConfig()
        
        // When we follow the CLI flow
        val backendType = BackendSelector.parseBackendFromArgs(args)
        val backend = BackendFactory.createBackend(backendType, config)
        
        // Then we should get the correct backend
        assertEquals(BackendType.RL4J, backendType)
        assertTrue(backend is RL4JLearningBackend)
        
        println("✅ CLI integration flow correctly creates RL4J backend from --nn rl4j flag")
    }
}