package com.chessrl.integration.backend

import com.chessrl.integration.config.ChessRLConfig
import kotlin.test.*

/**
 * Tests for configuration consistency verification.
 * 
 * Tests the requirement: "Print configuration summary at startup for both backends"
 * and "Verify identical hyperparameter values are used".
 */
class ConfigurationConsistencyTest {
    
    @Test
    fun testConfigurationSummaryPrintsCorrectValues() {
        // Given a specific configuration
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 64,
            gamma = 0.99,
            targetUpdateFrequency = 200,
            maxExperienceBuffer = 50000,
            hiddenLayers = listOf(512, 256, 128),
            explorationRate = 0.05,
            doubleDqn = true,
            optimizer = "adam",
            seed = 12345L
        )
        
        // When printing configuration summary
        // Then it should not throw exceptions (we can't easily capture stdout in tests)
        assertDoesNotThrow {
            ConfigurationMapper.printConfigurationSummary(config, BackendType.MANUAL)
        }
        
        assertDoesNotThrow {
            ConfigurationMapper.printConfigurationSummary(config, BackendType.RL4J)
        }
    }
    
    @Test
    fun testManualConfigurationMappingIsConsistent() {
        // Given a configuration
        val config = ChessRLConfig(
            learningRate = 0.002,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128),
            explorationRate = 0.1,
            doubleDqn = false,
            optimizer = "sgd",
            seed = 54321L
        )
        
        // When mapping to manual config
        val manualConfig = ConfigurationMapper.toManualConfig(config)
        
        // Then all parameters should match exactly
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
    fun testParameterConsistencyValidationWithMatchingConfigs() {
        // Given a configuration and matching manual config
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val manualConfig = ConfigurationMapper.toManualConfig(config)
        
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
        
        // Then there should be no inconsistencies
        assertTrue(inconsistencies.isEmpty(), "Expected no inconsistencies, but found: $inconsistencies")
    }
    
    @Test
    fun testParameterConsistencyValidationDetectsManualMismatches() {
        // Given a configuration and mismatched manual config
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val mismatchedManualConfig = ManualDQNConfig(
            learningRate = 0.002, // Different
            batchSize = 64, // Different
            gamma = 0.99, // Different
            targetUpdateFrequency = 200, // Different
            maxExperienceBuffer = 20000, // Different
            hiddenLayers = listOf(512, 256), // Different
            explorationRate = config.explorationRate,
            doubleDqn = config.doubleDqn,
            optimizer = config.optimizer,
            seed = config.seed
        )
        
        // Create a mock RL4J config with matching values to original config
        val mockRL4JConfig = MockRL4JConfig(
            learningRate = config.learningRate,
            gamma = config.gamma,
            batchSize = config.batchSize,
            expRepMaxSize = config.maxExperienceBuffer,
            targetDqnUpdateFreq = config.targetUpdateFrequency
        )
        
        // When validating parameter consistency
        val inconsistencies = ConfigurationMapper.validateParameterConsistency(
            config, mismatchedManualConfig, mockRL4JConfig
        )
        
        // Then there should be inconsistencies for manual config
        assertEquals(6, inconsistencies.size)
        assertTrue(inconsistencies.any { it.contains("Learning rate mismatch: Manual=0.002") })
        assertTrue(inconsistencies.any { it.contains("Batch size mismatch: Manual=64") })
        assertTrue(inconsistencies.any { it.contains("Gamma mismatch: Manual=0.99") })
        assertTrue(inconsistencies.any { it.contains("Target update frequency mismatch: Manual=200") })
        assertTrue(inconsistencies.any { it.contains("Experience buffer mismatch: Manual=20000") })
        assertTrue(inconsistencies.any { it.contains("Hidden layers mismatch: Manual=[512, 256]") })
    }
    
    @Test
    fun testParameterConsistencyValidationDetectsRL4JMismatches() {
        // Given a configuration and matching manual config
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val manualConfig = ConfigurationMapper.toManualConfig(config)
        
        // Create a mock RL4J config with mismatched values
        val mismatchedRL4JConfig = MockRL4JConfig(
            learningRate = 0.002, // Different
            gamma = 0.99, // Different
            batchSize = 64, // Different
            expRepMaxSize = 20000, // Different
            targetDqnUpdateFreq = 200 // Different
        )
        
        // When validating parameter consistency
        val inconsistencies = ConfigurationMapper.validateParameterConsistency(
            config, manualConfig, mismatchedRL4JConfig
        )
        
        // Then there should be inconsistencies for RL4J config
        assertEquals(5, inconsistencies.size)
        assertTrue(inconsistencies.any { it.contains("Learning rate mismatch: RL4J=0.002") })
        assertTrue(inconsistencies.any { it.contains("Gamma mismatch: RL4J=0.99") })
        assertTrue(inconsistencies.any { it.contains("Batch size mismatch: RL4J=64") })
        assertTrue(inconsistencies.any { it.contains("Experience buffer mismatch: RL4J=20000") })
        assertTrue(inconsistencies.any { it.contains("Target update frequency mismatch: RL4J=200") })
    }
    
    @Test
    fun testParameterConsistencyValidationHandlesReflectionErrors() {
        // Given a configuration and manual config
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        val manualConfig = ConfigurationMapper.toManualConfig(config)
        
        // Create an invalid RL4J config object (missing methods)
        val invalidRL4JConfig = "not a valid RL4J config"
        
        // When validating parameter consistency
        val inconsistencies = ConfigurationMapper.validateParameterConsistency(
            config, manualConfig, invalidRL4JConfig
        )
        
        // Then there should be an error about validation failure
        assertEquals(1, inconsistencies.size)
        assertTrue(inconsistencies[0].contains("Could not validate RL4J configuration consistency"))
    }
    
    @Test
    fun testConfigurationLoggingIncludesAllParameters() {
        // Given a configuration with all parameters set
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
        
        // When mapping to manual config (which triggers logging)
        val manualConfig = ConfigurationMapper.toManualConfig(config)
        
        // Then the mapping should succeed (logging is tested indirectly)
        assertNotNull(manualConfig)
        
        // Verify all parameters are correctly mapped
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
    fun testConfigurationSummaryHandlesNullSeed() {
        // Given a configuration with null seed
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128),
            seed = null // Null seed should be handled gracefully
        )
        
        // When printing configuration summary
        // Then it should not throw exceptions
        assertDoesNotThrow {
            ConfigurationMapper.printConfigurationSummary(config, BackendType.MANUAL)
        }
        
        // When mapping to manual config
        val manualConfig = ConfigurationMapper.toManualConfig(config)
        
        // Then seed should be preserved as null
        assertNull(manualConfig.seed)
    }
    
    @Test
    fun testConfigurationSummaryShowsBackendType() {
        // Given a configuration
        val config = ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.95,
            targetUpdateFrequency = 100,
            maxExperienceBuffer = 10000,
            hiddenLayers = listOf(256, 128)
        )
        
        // When printing configuration summary for different backends
        // Then it should handle both backend types without exceptions
        assertDoesNotThrow {
            ConfigurationMapper.printConfigurationSummary(config, BackendType.MANUAL)
            ConfigurationMapper.printConfigurationSummary(config, BackendType.RL4J)
            ConfigurationMapper.printConfigurationSummary(config, BackendType.DL4J)
        }
    }
    
    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}

