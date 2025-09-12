package com.chessrl.nn

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test for the advanced optimizers demonstration
 */
class AdvancedOptimizersDemoTest {
    
    @Test
    fun testDemoRuns() {
        // Test that all demonstrations run without throwing exceptions
        try {
            AdvancedOptimizersDemo.demonstrateOptimizers()
            AdvancedOptimizersDemo.demonstrateLossFunctions()
            AdvancedOptimizersDemo.demonstrateRegularization()
            AdvancedOptimizersDemo.demonstrateLearningRateScheduling()
            AdvancedOptimizersDemo.demonstrateComplexScenario()
            
            assertTrue(true, "All demonstrations completed successfully")
        } catch (e: Exception) {
            assertTrue(false, "Demo failed with exception: ${e.message}")
        }
    }
}