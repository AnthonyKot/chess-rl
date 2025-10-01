package com.chessrl.integration.backend

import com.chessrl.integration.config.ChessRLConfig
import kotlin.test.*

/**
 * Tests for ConfigurationMapper functionality.
 */
class ConfigurationMapperTest {
    
    @Test
    fun testToManualConfigMapsAllParameters() {
        // Given a ChessRLConfig with specific values
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128),
            explorationRate = 0.1,
            doubleDqn = true,
            optimizer = "adam",
            seed = 12345L
        )
        
        // When mapping to manual config
        val manualConfig = ConfigurationMapper.toManualConfig(config)
        
        // Then all parameters should be mapped correctly
        assertEquals(config.learningRate, manualConfig.learningRate)
        assertEquals(config.batchSize, manualConfig.batchSize)
        assertEquals(config.gamma, manualConfig.gamma)
        assertEquals(config.targetUpdateFrequency, manualConfig.targetUpdateFrequency)
        assertEquals(config.maxExperienceBuffer, manualConfig.maxExperienceBuffer)
        assertEquals(config.hiddenLayers, manualConfig.hiddenLayers)
        assertEquals(config.explorationRate, manualConfig.explorationRate)
        assertEquals(config.doubleDqn, manualConfig.doubleDqn)
        assertEquals(config.optimizer, manualConfig.optimizer)
        assertEquals(config.seed, manualConfig.seed)
    }
    
    @Test
    fun testToRL4JConfigFailsWhenRL4JNotAvailable() {
        // Given a valid ChessRLConfig
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config (RL4J not available by default)
        // Then it should throw IllegalStateException
        assertFailsWith<IllegalStateException> {
            ConfigurationMapper.toRL4JConfig(config)
        }
    }
    
    @Test
    fun testValidateRL4JParametersWithValidConfig() {
        // Given a valid ChessRLConfig
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128),
            explorationRate = 0.1
        )
        
        // When validating parameters (using reflection to access private method)
        // Then it should not throw any exceptions
        // Note: We can't directly test the private method, but we can test through toRL4JConfig
        // which will fail on availability, not validation
        val exception = assertFailsWith<IllegalStateException> {
            ConfigurationMapper.toRL4JConfig(config)
        }
        
        // The exception should be about RL4J availability, not parameter validation
        assertTrue(exception.message?.contains("RL4J") == true)
        assertFalse(exception.message?.contains("Learning rate") == true)
    }
    
    @Test
    fun testValidateRL4JParametersWithInvalidLearningRate() {
        // Given a ChessRLConfig with invalid learning rate
        val config = ChessRLConfig(
            learningRate = -0.1, // Invalid: negative
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        // Then it should throw IllegalArgumentException about learning rate
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(config)
        }
        
        assertTrue(exception.message?.contains("Learning rate must be positive") == true)
    }
    
    @Test
    fun testValidateRL4JParametersWithInvalidGamma() {
        // Given a ChessRLConfig with invalid gamma
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 1.5, // Invalid: > 1.0
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        // Then it should throw IllegalArgumentException about gamma
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(config)
        }
        
        assertTrue(exception.message?.contains("Gamma must be in range (0,1)") == true)
    }
    
    @Test
    fun testValidateRL4JParametersWithInvalidBufferSize() {
        // Given a ChessRLConfig with buffer smaller than batch size
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 100,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 50, // Invalid: smaller than batch size
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        // Then it should throw IllegalArgumentException about buffer size
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(config)
        }
        
        assertTrue(exception.message?.contains("Experience buffer size") == true)
        assertTrue(exception.message?.contains("must be larger than batch size") == true)
    }
    
    @Test
    fun testValidateParameterConsistencyWithMatchingConfigs() {
        // Given a ChessRLConfig and matching manual config
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val manualConfig = ConfigurationMapper.toManualConfig(config)
        
        // Create a mock RL4J config (since real RL4J is not available)
        val mockRL4JConfig = MockRL4JConfig(
            learningRate = config.learningRate,
            gamma = config.gamma,
            batchSize = config.batchSize,
            expRepMaxSize = config.maxExperienceBuffer,
            targetDqnUpdateFreq = config.targetUpdateFrequency
        )
        
        // When validating parameter consistency
        val inconsistencies = ConfigurationMapper.validateParameterConsistency(
            config, manualConfig, mockRL4JConfig
        )
        
        // Then there should be no inconsistencies
        assertTrue(inconsistencies.isEmpty(), "Expected no inconsistencies, but found: $inconsistencies")
    }
    
    @Test
    fun testValidateParameterConsistencyWithMismatchedConfigs() {
        // Given a ChessRLConfig and mismatched manual config
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val manualConfig = ManualDQNConfig(
            learningRate = 0.002, // Different from config
            batchSize = 64, // Different from config
            gamma = config.gamma,
            targetUpdateFrequency = config.targetUpdateFrequency,
            maxExperienceBuffer = config.maxExperienceBuffer,
            hiddenLayers = config.hiddenLayers,
            explorationRate = config.explorationRate,
            doubleDqn = config.doubleDqn,
            optimizer = config.optimizer,
            seed = config.seed
        )
        
        // Create a mock RL4J config with matching values
        val mockRL4JConfig = MockRL4JConfig(
            learningRate = config.learningRate,
            gamma = config.gamma,
            batchSize = config.batchSize,
            expRepMaxSize = config.maxExperienceBuffer,
            targetDqnUpdateFreq = config.targetUpdateFrequency
        )
        
        // When validating parameter consistency
        val inconsistencies = ConfigurationMapper.validateParameterConsistency(
            config, manualConfig, mockRL4JConfig
        )
        
        // Then there should be inconsistencies for manual config
        assertEquals(2, inconsistencies.size)
        assertTrue(inconsistencies.any { it.contains("Learning rate mismatch: Manual=0.002") })
        assertTrue(inconsistencies.any { it.contains("Batch size mismatch: Manual=64") })
    }
    
    @Test
    fun testPrintConfigurationSummaryDoesNotThrow() {
        // Given a valid ChessRLConfig
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When printing configuration summary
        // Then it should not throw any exceptions
        try {
            ConfigurationMapper.printConfigurationSummary(config, BackendType.MANUAL)
            ConfigurationMapper.printConfigurationSummary(config, BackendType.RL4J)
            // If we reach here, no exception was thrown
            assertTrue(true)
        } catch (e: Exception) {
            fail("printConfigurationSummary should not throw exceptions, but got: ${e.message}")
        }
    }
}

/**
 * Mock RL4J configuration class for testing parameter consistency validation.
 */
internal class MockRL4JConfig(
    private val learningRate: Double,
    private val gamma: Double,
    private val batchSize: Int,
    private val expRepMaxSize: Int,
    private val targetDqnUpdateFreq: Int
) {
    fun getLearningRate(): Double = learningRate
    fun getGamma(): Double = gamma
    fun getBatchSize(): Int = batchSize
    fun getExpRepMaxSize(): Int = expRepMaxSize
    fun getTargetDqnUpdateFreq(): Int = targetDqnUpdateFreq
}