package com.chessrl.integration.backend

import com.chessrl.integration.config.ChessRLConfig
import kotlin.test.*

/**
 * Comprehensive tests for parameter validation with clear error messages.
 * 
 * Tests the requirement: "Validate learning rate is positive (reject -0.1 with clear error)"
 * and other parameter validation scenarios.
 */
class ParameterValidationTest {
    
    @Test
    fun testValidConfigurationPasses() {
        // Given a valid configuration
        val validConfig = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128),
            explorationRate = 0.1
        )
        
        // When mapping to manual config
        val manualConfig = ConfigurationMapper.toManualConfig(validConfig)
        
        // Then it should succeed without exceptions
        assertNotNull(manualConfig)
        assertEquals(validConfig.learningRate, manualConfig.learningRate)
    }
    
    @Test
    fun testNegativeLearningRateRejectedWithClearError() {
        // Given a configuration with negative learning rate
        val invalidConfig = ChessRLConfig(
            learningRate = -0.1, // Invalid: negative
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should provide a clear error message
        assertTrue(exception.message?.contains("Learning rate must be positive") == true)
        assertTrue(exception.message?.contains("-0.1") == true)
        assertTrue(exception.message?.contains("RL4J backend") == true)
    }
    
    @Test
    fun testZeroLearningRateRejectedWithClearError() {
        // Given a configuration with zero learning rate
        val invalidConfig = ChessRLConfig(
            learningRate = 0.0, // Invalid: zero
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should provide a clear error message
        assertTrue(exception.message?.contains("Learning rate must be positive") == true)
        assertTrue(exception.message?.contains("0.0") == true)
    }
    
    @Test
    fun testTooHighLearningRateRejectedWithClearError() {
        // Given a configuration with learning rate > 1.0
        val invalidConfig = ChessRLConfig(
            learningRate = 1.5, // Invalid: > 1.0
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should provide a clear error message
        assertTrue(exception.message?.contains("Learning rate must be <= 1.0") == true)
        assertTrue(exception.message?.contains("1.5") == true)
    }
    
    @Test
    fun testNegativeBatchSizeRejectedWithClearError() {
        // Given a configuration with negative batch size
        val invalidConfig = ChessRLConfig(
            learningRate = 0.001,
            batchSize = -10, // Invalid: negative
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should provide a clear error message
        assertTrue(exception.message?.contains("Batch size must be positive") == true)
        assertTrue(exception.message?.contains("-10") == true)
    }
    
    @Test
    fun testInvalidGammaRangeRejectedWithClearError() {
        // Test gamma = 0 (invalid)
        val invalidConfig1 = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.0, // Invalid: not in (0,1)
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val exception1 = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig1)
        }
        
        assertTrue(exception1.message?.contains("Gamma must be in range (0,1)") == true)
        assertTrue(exception1.message?.contains("0.0") == true)
        
        // Test gamma = 1 (invalid)
        val invalidConfig2 = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 1.0, // Invalid: not in (0,1)
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val exception2 = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig2)
        }
        
        assertTrue(exception2.message?.contains("Gamma must be in range (0,1)") == true)
        assertTrue(exception2.message?.contains("1.0") == true)
        
        // Test gamma > 1 (invalid)
        val invalidConfig3 = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 1.5, // Invalid: > 1
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val exception3 = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig3)
        }
        
        assertTrue(exception3.message?.contains("Gamma must be in range (0,1)") == true)
        assertTrue(exception3.message?.contains("1.5") == true)
    }
    
    @Test
    fun testInvalidTargetUpdateFrequencyRejectedWithClearError() {
        // Given a configuration with invalid target update frequency
        val invalidConfig = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = -5, // Invalid: negative
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should provide a clear error message
        assertTrue(exception.message?.contains("Target update frequency must be positive") == true)
        assertTrue(exception.message?.contains("-5") == true)
    }
    
    @Test
    fun testBufferSmallerThanBatchSizeRejectedWithClearError() {
        // Given a configuration where buffer size <= batch size
        val invalidConfig = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 100,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 50, // Invalid: smaller than batch size
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should provide a clear error message
        assertTrue(exception.message?.contains("Experience buffer size (50) must be larger than batch size (100)") == true)
    }
    
    @Test
    fun testInvalidExplorationRateRejectedWithClearError() {
        // Test negative exploration rate
        val invalidConfig1 = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128),
            explorationRate = -0.1 // Invalid: negative
        )
        
        val exception1 = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig1)
        }
        
        assertTrue(exception1.message?.contains("Exploration rate must be in range [0,1]") == true)
        assertTrue(exception1.message?.contains("-0.1") == true)
        
        // Test exploration rate > 1
        val invalidConfig2 = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128),
            explorationRate = 1.5 // Invalid: > 1
        )
        
        val exception2 = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig2)
        }
        
        assertTrue(exception2.message?.contains("Exploration rate must be in range [0,1]") == true)
        assertTrue(exception2.message?.contains("1.5") == true)
    }
    
    @Test
    fun testEmptyHiddenLayersRejectedWithClearError() {
        // Given a configuration with empty hidden layers
        val invalidConfig = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = emptyList() // Invalid: empty
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should provide a clear error message
        assertTrue(exception.message?.contains("Hidden layers cannot be empty") == true)
    }
    
    @Test
    fun testNegativeHiddenLayerSizeRejectedWithClearError() {
        // Given a configuration with negative hidden layer size
        val invalidConfig = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, -128) // Invalid: negative size
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should provide a clear error message
        assertTrue(exception.message?.contains("All hidden layer sizes must be positive") == true)
        assertTrue(exception.message?.contains("[256, -128]") == true)
    }
    
    @Test
    fun testMultipleErrorsReportedTogether() {
        // Given a configuration with multiple invalid parameters
        val invalidConfig = ChessRLConfig(
            learningRate = -0.1, // Invalid: negative
            batchSize = -10, // Invalid: negative
            gamma = 1.5, // Invalid: > 1
            targetUpdateFrequency = -5, // Invalid: negative
            maxExperienceBuffer = 10, // Invalid: smaller than batch size (but batch size is also invalid)
            hiddenLayers = emptyList() // Invalid: empty
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(invalidConfig)
        }
        
        // Then it should report multiple errors
        val message = exception.message ?: ""
        assertTrue(message.contains("Learning rate must be positive"))
        assertTrue(message.contains("Batch size must be positive"))
        assertTrue(message.contains("Gamma must be in range (0,1)"))
        assertTrue(message.contains("Target update frequency must be positive"))
        assertTrue(message.contains("Hidden layers cannot be empty"))
        
        // Should use semicolon to separate multiple errors
        assertTrue(message.contains(";"))
    }
    
    @Test
    fun testWarningForLargeBatchSizeRelativeToBuffer() {
        // Given a configuration with batch size close to buffer size
        val configWithLargeBatch = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 800, // Large relative to buffer
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 1000, // Only 1.25x batch size
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(configWithLargeBatch)
        }
        
        // Then it should warn about the ratio
        assertTrue(exception.message?.contains("Batch size (800) should be significantly smaller than experience buffer (1000)") == true)
    }
    
    @Test
    fun testWarningForVeryHighTargetUpdateFrequency() {
        // Given a configuration with very high target update frequency
        val configWithHighFreq = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 50000, // Very high
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(configWithHighFreq)
        }
        
        // Then it should warn about the high frequency
        assertTrue(exception.message?.contains("Target update frequency (50000) is very high and may slow learning") == true)
    }
    
    @Test
    fun testWarningForVeryLargeHiddenLayers() {
        // Given a configuration with very large hidden layers
        val configWithLargeLayers = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(4096, 2048) // Very large
        )
        
        // When trying to map to RL4J config
        val exception = assertFailsWith<IllegalArgumentException> {
            ConfigurationMapper.toRL4JConfig(configWithLargeLayers)
        }
        
        // Then it should warn about memory usage
        assertTrue(exception.message?.contains("Hidden layer sizes are very large") == true)
        assertTrue(exception.message?.contains("may cause memory issues") == true)
    }
}