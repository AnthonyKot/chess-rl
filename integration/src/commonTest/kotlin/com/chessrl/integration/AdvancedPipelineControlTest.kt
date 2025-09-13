package com.chessrl.integration

import kotlin.test.*

/**
 * Test for advanced pipeline control methods (pause/resume/stop)
 * to verify Task 9.3 readiness
 */
class AdvancedPipelineControlTest {
    
    @Test
    fun testPipelineControlMethods() {
        println("🧪 Testing advanced pipeline control methods")
        
        val config = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 3,
            maxStepsPerGame = 20,
            evaluationGamesPerCycle = 2,
            batchSize = 8,
            maxBatchesPerCycle = 2,
            enableEarlyStopping = false
        )
        
        val pipeline = AdvancedSelfPlayTrainingPipeline(config)
        assertTrue(pipeline.initialize(), "Pipeline should initialize successfully")
        
        // Test initial state
        val initialStatus = pipeline.getTrainingStatus()
        assertFalse(initialStatus.isTraining, "Should not be training initially")
        assertFalse(pipeline.isTrainingPaused(), "Should not be paused initially")
        
        // Test pause/resume methods when not training (should be safe)
        pipeline.pauseTraining()  // Should be safe when not training
        pipeline.resumeTraining() // Should be safe when not training
        pipeline.stopTraining()   // Should be safe when not training
        
        println("   ✅ Control methods work safely when not training")
        
        // Note: We don't test actual pause during training as it would require
        // multi-threading and would make the test complex and potentially flaky.
        // The integration will be tested in Task 9.3 implementation.
        
        println("   ✅ Pipeline control interface ready for Task 9.3")
    }
    
    @Test
    fun testTrainingStatusInterface() {
        println("🧪 Testing training status interface for monitoring")
        
        val config = AdvancedSelfPlayConfig(
            initialGamesPerCycle = 2,
            maxStepsPerGame = 15,
            evaluationGamesPerCycle = 1,
            batchSize = 4
        )
        
        val pipeline = AdvancedSelfPlayTrainingPipeline(config)
        assertTrue(pipeline.initialize(), "Pipeline should initialize successfully")
        
        // Test status interface
        val status = pipeline.getTrainingStatus()
        assertNotNull(status, "Status should not be null")
        assertFalse(status.isTraining, "Should not be training initially")
        assertEquals(0, status.currentCycle, "Current cycle should be 0")
        assertEquals(0, status.totalCycles, "Total cycles should be 0")
        
        println("   ✅ Training status interface ready for Task 9.3 monitoring")
    }
}