package com.chessrl.integration.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite for backend infrastructure components
 */
class BackendInfrastructureTest {
    
    @Test
    fun testBackendTypeFromString() {
        assertEquals(BackendType.MANUAL, BackendType.fromString("manual"))
        assertEquals(BackendType.MANUAL, BackendType.fromString("MANUAL"))
        assertEquals(BackendType.DL4J, BackendType.fromString("dl4j"))
        assertEquals(BackendType.DL4J, BackendType.fromString("DL4J"))
        assertEquals(BackendType.KOTLINDL, BackendType.fromString("kotlindl"))
        assertEquals(BackendType.KOTLINDL, BackendType.fromString("KOTLINDL"))
        assertEquals(BackendType.RL4J, BackendType.fromString("rl4j"))
        assertEquals(BackendType.RL4J, BackendType.fromString("RL4J"))
        assertEquals(null, BackendType.fromString("unknown"))
    }
    
    @Test
    fun testBackendTypeDefault() {
        assertEquals(BackendType.DL4J, BackendType.getDefault())
    }
    
    @Test
    fun testBackendSelectorParseFromArgs() {
        // Test with --nn flag
        val args1 = arrayOf("--train", "--nn", "dl4j", "--cycles", "10")
        assertEquals(BackendType.DL4J, BackendSelector.parseBackendFromArgs(args1))
        
        // Test without --nn flag
        val args2 = arrayOf("--train", "--cycles", "10")
        assertEquals(BackendType.DL4J, BackendSelector.parseBackendFromArgs(args2))
        
        // Test with invalid backend
        val args3 = arrayOf("--train", "--nn", "invalid", "--cycles", "10")
        assertEquals(BackendType.DL4J, BackendSelector.parseBackendFromArgs(args3))
    }
    
    @Test
    fun testBackendSelectorParseFromProfile() {
        // Test with nnBackend in profile
        val profile1 = mapOf("nnBackend" to "dl4j")
        assertEquals(BackendType.DL4J, BackendSelector.parseBackendFromProfile(profile1))
        
        // Test without nnBackend in profile
        val profile2 = mapOf("learningRate" to 0.001)
        assertEquals(BackendType.DL4J, BackendSelector.parseBackendFromProfile(profile2))
        
        // Test with invalid backend in profile
        val profile3 = mapOf("nnBackend" to "invalid")
        assertEquals(BackendType.DL4J, BackendSelector.parseBackendFromProfile(profile3))
    }
    
    @Test
    fun testBackendSelectorSelectBackend() {
        // CLI args should take precedence
        val args1 = arrayOf("--nn", "dl4j")
        val profile1 = mapOf("nnBackend" to "kotlindl")
        assertEquals(BackendType.DL4J, BackendSelector.selectBackend(args1, profile1))
        
        // Profile should be used if no CLI args
        val args2 = arrayOf("--train")
        val profile2 = mapOf("nnBackend" to "kotlindl")
        assertEquals(BackendType.KOTLINDL, BackendSelector.selectBackend(args2, profile2))
        
        // Default should be used if neither
        val args3 = arrayOf("--train")
        val profile3 = mapOf("learningRate" to 0.001)
        assertEquals(BackendType.DL4J, BackendSelector.selectBackend(args3, profile3))
    }
    
    @Test
    fun testBackendValidation() {
        // Manual backend should always be valid
        val manualValidation = BackendSelector.validateBackendAvailability(BackendType.MANUAL)
        assertTrue(manualValidation.isValid)
        assertEquals("MANUAL", manualValidation.backend)
        assertTrue(manualValidation.issues.isEmpty())
    }
    
    @Test
    fun testBackendConfig() {
        val config = BackendConfig(
            inputSize = 839,
            outputSize = 4096,
            hiddenLayers = listOf(512, 256),
            learningRate = 0.001
        )
        
        assertEquals(839, config.inputSize)
        assertEquals(4096, config.outputSize)
        assertEquals(listOf(512, 256), config.hiddenLayers)
        assertEquals(0.001, config.learningRate)
    }
    
    @Test
    fun testBackendAwareFactoryCreateBackendConfig() {
        val config = BackendAwareChessAgentFactory.createBackendConfig(
            hiddenLayers = listOf(256, 128),
            learningRate = 0.002,
            batchSize = 64
        )
        
        assertEquals(listOf(256, 128), config.hiddenLayers)
        assertEquals(0.002, config.learningRate)
        assertEquals(64, config.batchSize)
    }
}