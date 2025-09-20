package com.chessrl.integration.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for ChessRLConfig data class and validation.
 */
class ChessRLConfigTest {
    
    @Test
    fun testDefaultConfigurationIsValid() {
        val config = ChessRLConfig()
        val validation = config.validate()
        
        assertTrue(validation.isValid, "Default configuration should be valid")
        assertTrue(validation.errors.isEmpty(), "Default configuration should have no errors")
    }
    
    @Test
    fun testConfigurationValidation() {
        // Test invalid learning rate
        val invalidLearningRate = ChessRLConfig(learningRate = -0.1)
        val validation1 = invalidLearningRate.validate()
        assertFalse(validation1.isValid)
        assertTrue(validation1.errors.any { it.contains("Learning rate") })
        
        // Test invalid exploration rate
        val invalidExploration = ChessRLConfig(explorationRate = 1.5)
        val validation2 = invalidExploration.validate()
        assertFalse(validation2.isValid)
        assertTrue(validation2.errors.any { it.contains("Exploration rate") })
        
        // Test empty hidden layers
        val emptyLayers = ChessRLConfig(hiddenLayers = emptyList())
        val validation3 = emptyLayers.validate()
        assertFalse(validation3.isValid)
        assertTrue(validation3.errors.any { it.contains("Hidden layers") })
    }
    
    @Test
    fun testConfigurationWarnings() {
        // Test configuration that should generate warnings
        val config = ChessRLConfig(
            winReward = -1.0,  // Win reward less than loss reward
            lossReward = 1.0,
            maxConcurrentGames = 16  // High concurrency
        )
        
        val validation = config.validate()
        assertTrue(validation.isValid, "Configuration should be valid despite warnings")
        assertTrue(validation.warnings.isNotEmpty(), "Should have warnings")
        assertTrue(validation.warnings.any { it.contains("Win reward") })
        assertTrue(validation.warnings.any { it.contains("concurrent games") })
    }
    
    @Test
    fun testProfileCreation() {
        val fastDebug = ChessRLConfig().forFastDebug()
        assertEquals(5, fastDebug.gamesPerCycle)
        assertEquals(10, fastDebug.maxCycles)
        assertEquals(2, fastDebug.maxConcurrentGames)
        
        val longTrain = ChessRLConfig().forLongTraining()
        assertEquals(50, longTrain.gamesPerCycle)
        assertEquals(200, longTrain.maxCycles)
        assertEquals(8, longTrain.maxConcurrentGames)
        assertEquals(listOf(768, 512, 256), longTrain.hiddenLayers)
        
        val evalOnly = ChessRLConfig().forEvaluationOnly()
        assertEquals(500, evalOnly.evaluationGames)
        assertEquals(1, evalOnly.maxConcurrentGames)
        assertEquals(12345L, evalOnly.seed)
    }
    
    @Test
    fun testWithSeed() {
        val config = ChessRLConfig()
        val seededConfig = config.withSeed(42L)
        
        assertEquals(42L, seededConfig.seed)
        assertEquals(config.learningRate, seededConfig.learningRate) // Other params unchanged
    }
    
    @Test
    fun testGetSummary() {
        val config = ChessRLConfig()
        val summary = config.getSummary()
        
        assertTrue(summary.contains("Chess RL Configuration Summary"))
        assertTrue(summary.contains("Network:"))
        assertTrue(summary.contains("RL:"))
        assertTrue(summary.contains("Self-play:"))
        assertTrue(summary.contains("Rewards:"))
        assertTrue(summary.contains("System:"))
    }
}