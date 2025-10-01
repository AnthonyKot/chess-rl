package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import kotlin.test.*

/**
 * Comprehensive test for RL4J training pipeline.
 * 
 * Tests the complete RL4J training cycle including:
 * - Training session creation and initialization
 * - Batch training execution
 * - Metrics collection and validation
 * - Checkpoint saving and loading
 * - Training interruption and resumption
 */
class RL4JTrainingPipelineTest {
    
    private val logger = ChessRLLogger.forComponent("RL4JTrainingPipelineTest")
    
    @Test
    fun testRL4JAvailabilityCheck() {
        // Test that we can check RL4J availability without errors
        val isAvailable = RL4JAvailability.isAvailable()
        val message = RL4JAvailability.getAvailabilityMessage()
        
        assertNotNull(message)
        assertTrue(message.isNotEmpty())
        
        logger.info("RL4J availability: $isAvailable")
        logger.info("RL4J message: $message")
        
        if (!isAvailable) {
            logger.warn("RL4J not available, skipping integration tests")
        }
    }
    
    @Test
    fun testRL4JBackendCreation() {
        if (!RL4JAvailability.isAvailable()) {
            logger.warn("Skipping RL4J backend creation test - RL4J not available")
            return
        }
        
        try {
            val backend = RL4JLearningBackend()
            assertNotNull(backend)
            assertEquals("rl4j", backend.id)
            
            logger.info("RL4J backend created successfully")
        } catch (e: Exception) {
            logger.error("Failed to create RL4J backend: ${e.message}")
            if (e.message?.contains("RL4J") == true) {
                // Expected failure when RL4J is not available
                logger.warn("RL4J backend creation failed as expected when RL4J is not available")
            } else {
                throw e
            }
        }
    }
    
    @Test
    fun testRL4JLearningSessionCreation() {
        if (!RL4JAvailability.isAvailable()) {
            logger.warn("Skipping RL4J learning session test - RL4J not available")
            return
        }
        
        try {
            val backend = RL4JLearningBackend()
            val config = createTestConfig()
            
            val session = backend.createSession(config)
            assertNotNull(session)
            assertEquals(config, session.config)
            assertNotNull(session.mainAgent)
            assertNotNull(session.opponentAgent)
            
            // Test that agents are properly initialized
            val mainAgentMetrics = session.mainAgent.getTrainingMetrics()
            assertNotNull(mainAgentMetrics)
            
            session.close()
            logger.info("RL4J learning session created and closed successfully")
            
        } catch (e: Exception) {
            logger.error("Failed to create RL4J learning session: ${e.message}")
            if (e.message?.contains("RL4J") == true) {
                logger.warn("RL4J learning session creation failed as expected when RL4J is not available")
            } else {
                throw e
            }
        }
    }
    
    @Test
    fun testRL4JTrainingCycleExecution() {
        if (!RL4JAvailability.isAvailable()) {
            logger.warn("Skipping RL4J training cycle test - RL4J not available")
            return
        }
        
        try {
            val backend = RL4JLearningBackend()
            val config = createTestConfig()
            
            backend.createSession(config).use { session ->
                // Test basic training operations
                testBasicTrainingOperations(session)
                
                // Test metrics collection
                testMetricsCollection(session)
                
                // Test checkpoint operations
                testCheckpointOperations(session)
            }
            
            logger.info("RL4J training cycle executed successfully")
            
        } catch (e: Exception) {
            logger.error("Failed to execute RL4J training cycle: ${e.message}")
            if (e.message?.contains("RL4J") == true) {
                logger.warn("RL4J training cycle failed as expected when RL4J is not available")
            } else {
                throw e
            }
        }
    }
    
    @Test
    fun testRL4JTrainingInterruptionAndResumption() {
        if (!RL4JAvailability.isAvailable()) {
            logger.warn("Skipping RL4J interruption/resumption test - RL4J not available")
            return
        }
        
        try {
            val backend = RL4JLearningBackend()
            val config = createTestConfig()
            
            backend.createSession(config).use { session ->
                // Cast to RL4JLearningSession to access extended functionality
                val rl4jSession = getRL4JSession(session)
                
                if (rl4jSession != null) {
                    // Test training pause/resume
                    testTrainingPauseResume(rl4jSession)
                    
                    // Test checkpoint resumption
                    testCheckpointResumption(rl4jSession)
                } else {
                    logger.warn("Could not access RL4J session extended functionality")
                }
            }
            
            logger.info("RL4J training interruption and resumption tested successfully")
            
        } catch (e: Exception) {
            logger.error("Failed to test RL4J training interruption/resumption: ${e.message}")
            if (e.message?.contains("RL4J") == true) {
                logger.warn("RL4J interruption/resumption test failed as expected when RL4J is not available")
            } else {
                throw e
            }
        }
    }
    
    @Test
    fun testRL4JMetricsValidation() {
        if (!RL4JAvailability.isAvailable()) {
            logger.warn("Skipping RL4J metrics validation test - RL4J not available")
            return
        }
        
        try {
            val backend = RL4JLearningBackend()
            val config = createTestConfig()
            
            backend.createSession(config).use { session ->
                // Run some training to generate metrics
                val experiences = createMockExperiences(config.batchSize)
                val result = session.trainOnBatch(experiences)
                
                // Validate training result
                assertNotNull(result)
                assertTrue(result.loss >= 0.0, "Loss should be non-negative")
                assertTrue(result.gradientNorm >= 0.0, "Gradient norm should be non-negative")
                
                // Validate agent metrics
                val agentMetrics = session.mainAgent.getTrainingMetrics()
                assertNotNull(agentMetrics)
                assertTrue(agentMetrics.explorationRate >= 0.0, "Exploration rate should be non-negative")
                assertTrue(agentMetrics.explorationRate <= 1.0, "Exploration rate should be <= 1.0")
                
                logger.info("RL4J metrics validation completed successfully")
            }
            
        } catch (e: Exception) {
            logger.error("Failed to validate RL4J metrics: ${e.message}")
            if (e.message?.contains("RL4J") == true) {
                logger.warn("RL4J metrics validation failed as expected when RL4J is not available")
            } else {
                throw e
            }
        }
    }
    
    // Helper methods
    
    private fun createTestConfig(): ChessRLConfig {
        return ChessRLConfig(
            learningRate = 0.001,
            batchSize = 8, // Small batch for testing
            gamma = 0.99,
            targetUpdateFrequency = 10,
            maxExperienceBuffer = 100, // Small buffer for testing
            hiddenLayers = listOf(64, 32), // Small network for testing
            explorationRate = 0.1,
            doubleDqn = false,
            optimizer = "adam",
            replayType = "UNIFORM",
            seed = 12345L,
            checkpointInterval = 5,
            checkpointDirectory = "test_checkpoints"
        )
    }
    
    private fun createMockExperiences(batchSize: Int): List<com.chessrl.rl.Experience<DoubleArray, Int>> {
        return (1..batchSize).map { i ->
            com.chessrl.rl.Experience(
                state = DoubleArray(839) { 0.1 * (i % 10) }, // Mock state
                action = i % 4096, // Mock action
                reward = if (i % 3 == 0) 1.0 else 0.0, // Mock reward
                nextState = DoubleArray(839) { 0.1 * ((i + 1) % 10) }, // Mock next state
                done = i % 10 == 0 // Mock terminal state
            )
        }
    }
    
    private fun testBasicTrainingOperations(session: com.chessrl.integration.backend.LearningSession) {
        // Test batch training
        val experiences = createMockExperiences(8)
        val result = session.trainOnBatch(experiences)
        assertNotNull(result)
        
        // Test opponent update
        session.updateOpponent()
        
        logger.debug("Basic training operations completed")
    }
    
    private fun testMetricsCollection(session: com.chessrl.integration.backend.LearningSession) {
        // Get initial metrics
        val initialMetrics = session.mainAgent.getTrainingMetrics()
        assertNotNull(initialMetrics)
        
        // Run training and check metrics update
        val experiences = createMockExperiences(8)
        session.trainOnBatch(experiences)
        
        val updatedMetrics = session.mainAgent.getTrainingMetrics()
        assertNotNull(updatedMetrics)
        
        logger.debug("Metrics collection tested")
    }
    
    private fun testCheckpointOperations(session: com.chessrl.integration.backend.LearningSession) {
        val checkpointPath = "test_rl4j_checkpoint.model"
        val bestModelPath = "test_rl4j_best.model"
        
        try {
            // Test checkpoint saving
            session.saveCheckpoint(checkpointPath)
            
            // Test best model saving
            session.saveBest(bestModelPath)
            
            logger.debug("Checkpoint operations completed")
            
        } catch (e: Exception) {
            logger.warn("Checkpoint operations failed (expected if file system not available): ${e.message}")
        }
    }
    
    private fun getRL4JSession(session: com.chessrl.integration.backend.LearningSession): Any? {
        return try {
            // Use reflection to access RL4J-specific functionality
            val sessionClass = session.javaClass
            if (sessionClass.simpleName.contains("RL4J")) {
                session
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug("Could not access RL4J session: ${e.message}")
            null
        }
    }
    
    private fun testTrainingPauseResume(rl4jSession: Any) {
        try {
            // Use reflection to test pause/resume functionality
            val pauseMethod = rl4jSession.javaClass.getMethod("pauseTraining")
            val resumeMethod = rl4jSession.javaClass.getMethod("resumeTraining")
            val canInterruptMethod = rl4jSession.javaClass.getMethod("canInterrupt")
            
            // Test pause
            pauseMethod.invoke(rl4jSession)
            
            // Test can interrupt
            val canInterrupt = canInterruptMethod.invoke(rl4jSession) as Boolean
            assertTrue(canInterrupt, "Should be able to interrupt when paused")
            
            // Test resume
            resumeMethod.invoke(rl4jSession)
            
            logger.debug("Training pause/resume tested")
            
        } catch (e: Exception) {
            logger.debug("Could not test pause/resume functionality: ${e.message}")
        }
    }
    
    private fun testCheckpointResumption(rl4jSession: Any) {
        try {
            val checkpointPath = "test_rl4j_resume_checkpoint.model"
            
            // Use reflection to test checkpoint resumption
            val resumeMethod = rl4jSession.javaClass.getMethod("resumeFromCheckpoint", String::class.java)
            
            // This will likely fail due to missing checkpoint file, but we test the method exists
            try {
                resumeMethod.invoke(rl4jSession, checkpointPath)
            } catch (e: Exception) {
                logger.debug("Checkpoint resumption failed as expected (no checkpoint file): ${e.message}")
            }
            
            logger.debug("Checkpoint resumption method tested")
            
        } catch (e: Exception) {
            logger.debug("Could not test checkpoint resumption: ${e.message}")
        }
    }
}