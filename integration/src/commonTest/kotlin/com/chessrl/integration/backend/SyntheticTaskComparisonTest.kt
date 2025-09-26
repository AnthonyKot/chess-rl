package com.chessrl.integration.backend

import kotlin.test.*

class SyntheticTaskComparisonTest {
    
    private val comparison = SyntheticTaskComparison()
    
    @Test
    fun testXORLearningComparison() {
        val backends = listOf(BackendType.MANUAL)
        val result = comparison.compareXORLearning(backends, maxEpochs = 100)
        
        assertNotNull(result)
        assertEquals("XOR Learning", result.taskName)
        assertTrue(result.results.containsKey(BackendType.MANUAL))
        
        val manualResult = result.results[BackendType.MANUAL]!!
        assertEquals("manual", manualResult.backend)
        assertEquals("XOR", manualResult.taskName)
        assertTrue(manualResult.finalAccuracy >= 0.0)
        assertTrue(manualResult.finalAccuracy <= 1.0)
        assertTrue(manualResult.trainingTimeMs >= 0)
        assertTrue(manualResult.inferenceTimeMs >= 0)
        assertTrue(manualResult.lossHistory.isNotEmpty())
        assertTrue(manualResult.accuracyHistory.isNotEmpty())
        
        // XOR should be learnable, so we expect reasonable performance
        if (manualResult.converged) {
            assertTrue(manualResult.finalAccuracy > 0.8, "XOR task should achieve high accuracy when converged")
        }
        
        println("XOR Learning Result:")
        println("  Converged: ${manualResult.converged}")
        println("  Final Accuracy: ${manualResult.finalAccuracy}")
        println("  Training Time: ${manualResult.trainingTimeMs}ms")
        println("  Final Loss: ${manualResult.finalLoss}")
    }
    
    @Test
    fun testSineRegressionComparison() {
        val backends = listOf(BackendType.MANUAL)
        val result = comparison.compareSineRegression(backends, numSamples = 50, maxEpochs = 100)
        
        assertNotNull(result)
        assertEquals("Sine Regression", result.taskName)
        assertTrue(result.results.containsKey(BackendType.MANUAL))
        
        val manualResult = result.results[BackendType.MANUAL]!!
        assertEquals("manual", manualResult.backend)
        assertEquals("Sine Regression", manualResult.taskName)
        assertTrue(manualResult.finalAccuracy >= -1.0) // R² can be negative
        assertTrue(manualResult.trainingTimeMs >= 0)
        assertTrue(manualResult.inferenceTimeMs >= 0)
        assertTrue(manualResult.lossHistory.isNotEmpty())
        assertTrue(manualResult.accuracyHistory.isNotEmpty())
        
        println("Sine Regression Result:")
        println("  Converged: ${manualResult.converged}")
        println("  Final Accuracy (R²): ${manualResult.finalAccuracy}")
        println("  Training Time: ${manualResult.trainingTimeMs}ms")
        println("  Final Loss: ${manualResult.finalLoss}")
    }
    
    @Test
    fun testChessPatternRecognitionComparison() {
        val backends = listOf(BackendType.MANUAL)
        val result = comparison.compareChessPatternRecognition(backends, numPatterns = 20, maxEpochs = 50)
        
        assertNotNull(result)
        assertEquals("Chess Pattern Recognition", result.taskName)
        assertTrue(result.results.containsKey(BackendType.MANUAL))
        
        val manualResult = result.results[BackendType.MANUAL]!!
        assertEquals("manual", manualResult.backend)
        assertEquals("Chess Pattern Recognition", manualResult.taskName)
        assertTrue(manualResult.finalAccuracy >= 0.0)
        assertTrue(manualResult.finalAccuracy <= 1.0)
        assertTrue(manualResult.trainingTimeMs >= 0)
        assertTrue(manualResult.inferenceTimeMs >= 0)
        assertTrue(manualResult.lossHistory.isNotEmpty())
        assertTrue(manualResult.accuracyHistory.isNotEmpty())
        
        println("Chess Pattern Recognition Result:")
        println("  Converged: ${manualResult.converged}")
        println("  Final Accuracy: ${manualResult.finalAccuracy}")
        println("  Training Time: ${manualResult.trainingTimeMs}ms")
        println("  Final Loss: ${manualResult.finalLoss}")
    }
    
    @Test
    fun testInferenceSpeedBenchmark() {
        val backends = listOf(BackendType.MANUAL)
        val result = comparison.benchmarkInferenceSpeed(
            backends = backends,
            inputSize = 100,
            outputSize = 50,
            batchSize = 8,
            numIterations = 20
        )
        
        assertNotNull(result)
        assertEquals("Inference Speed Benchmark", result.taskName)
        assertEquals(100, result.inputSize)
        assertEquals(50, result.outputSize)
        assertEquals(8, result.batchSize)
        assertEquals(20, result.numIterations)
        assertTrue(result.results.containsKey(BackendType.MANUAL))
        
        val manualMetrics = result.results[BackendType.MANUAL]!!
        assertEquals("manual", manualMetrics.backend)
        assertTrue(manualMetrics.avgInferenceTimeMs >= 0.0)
        assertTrue(manualMetrics.memoryUsageMB >= 0)
        assertTrue(manualMetrics.parameterCount > 0)
        
        println("Inference Speed Benchmark:")
        println("  Average Inference Time: ${manualMetrics.avgInferenceTimeMs}ms")
        println("  Memory Usage: ${manualMetrics.memoryUsageMB}MB")
        println("  Parameter Count: ${manualMetrics.parameterCount}")
    }
    
    @Test
    fun testTrainingSpeedBenchmark() {
        val backends = listOf(BackendType.MANUAL)
        val result = comparison.benchmarkTrainingSpeed(
            backends = backends,
            inputSize = 100,
            outputSize = 50,
            batchSize = 8,
            numIterations = 10
        )
        
        assertNotNull(result)
        assertEquals("Training Speed Benchmark", result.taskName)
        assertTrue(result.results.containsKey(BackendType.MANUAL))
        
        val manualMetrics = result.results[BackendType.MANUAL]!!
        assertEquals("manual", manualMetrics.backend)
        assertTrue(manualMetrics.avgTrainingTimeMs >= 0.0)
        assertTrue(manualMetrics.memoryUsageMB >= 0)
        assertTrue(manualMetrics.parameterCount > 0)
        
        println("Training Speed Benchmark:")
        println("  Average Training Time: ${manualMetrics.avgTrainingTimeMs}ms")
        println("  Memory Usage: ${manualMetrics.memoryUsageMB}MB")
        println("  Parameter Count: ${manualMetrics.parameterCount}")
    }
    
    @Test
    fun testComprehensiveReport() {
        val backends = listOf(BackendType.MANUAL)
        
        // Run all comparisons with reduced parameters for faster testing
        val xorResult = comparison.compareXORLearning(backends, maxEpochs = 50)
        val sineResult = comparison.compareSineRegression(backends, numSamples = 30, maxEpochs = 50)
        val chessResult = comparison.compareChessPatternRecognition(backends, numPatterns = 10, maxEpochs = 30)
        val inferenceResult = comparison.benchmarkInferenceSpeed(backends, inputSize = 50, outputSize = 25, numIterations = 10)
        val trainingResult = comparison.benchmarkTrainingSpeed(backends, inputSize = 50, outputSize = 25, numIterations = 5)
        
        // Generate comprehensive report
        val report = comparison.generateComprehensiveReport(
            xorResult, sineResult, chessResult, inferenceResult, trainingResult
        )
        
        assertNotNull(report)
        assertTrue(report.backendSummaries.containsKey(BackendType.MANUAL))
        assertTrue(report.recommendations.isNotEmpty())
        
        val manualSummary = report.backendSummaries[BackendType.MANUAL]!!
        assertEquals("manual", manualSummary.backend)
        assertTrue(manualSummary.overallScore >= 0.0)
        
        // Test text report generation
        val textReport = report.generateTextReport()
        assertNotNull(textReport)
        assertTrue(textReport.contains("NEURAL NETWORK BACKEND COMPARISON REPORT"))
        assertTrue(textReport.contains("EXECUTIVE SUMMARY"))
        assertTrue(textReport.contains("BACKEND PERFORMANCE SUMMARY"))
        assertTrue(textReport.contains("DETAILED TASK RESULTS"))
        assertTrue(textReport.contains("PERFORMANCE BENCHMARKS"))
        
        // Test CSV report generation
        val csvReport = report.generateCSVReport()
        assertNotNull(csvReport)
        assertTrue(csvReport.contains("Backend,XOR_Accuracy"))
        assertTrue(csvReport.contains("manual,"))
        
        println("Comprehensive Report Generated:")
        println("Text Report Length: ${textReport.length} characters")
        println("CSV Report Length: ${csvReport.length} characters")
        println("Recommendations: ${report.recommendations.size}")
        
        // Print a sample of the text report
        println("\nSample Text Report:")
        println(textReport.take(500) + "...")
    }
    
    @Test
    fun testMultipleBackendsWithFallback() {
        // Test with multiple backends including ones that might not be available
        val backends = listOf(BackendType.MANUAL, BackendType.DL4J, BackendType.KOTLINDL)
        val result = comparison.compareXORLearning(backends, maxEpochs = 30)
        
        assertNotNull(result)
        assertTrue(result.results.containsKey(BackendType.MANUAL))
        
        // Other backends should fall back to manual if not available
        for (backend in backends) {
            assertTrue(result.results.containsKey(backend))
            val backendResult = result.results[backend]!!
            // All should use manual backend due to fallback
            assertEquals("manual", backendResult.backend)
        }
        
        println("Multi-backend test completed with ${result.results.size} results")
    }
    
    @Test
    fun testErrorHandling() {
        // Test with invalid configuration that should trigger error handling
        val comparison = SyntheticTaskComparison()
        
        // This should still work due to error handling in the implementation
        val backends = listOf(BackendType.MANUAL)
        val result = comparison.compareXORLearning(backends, maxEpochs = 1) // Very few epochs
        
        assertNotNull(result)
        assertTrue(result.results.containsKey(BackendType.MANUAL))
        
        val manualResult = result.results[BackendType.MANUAL]!!
        // Should complete without throwing exceptions
        assertTrue(manualResult.trainingTimeMs >= 0)
        assertTrue(manualResult.lossHistory.isNotEmpty())
    }
    
    @Test
    fun testResultDataStructures() {
        // Test the data structures directly
        val taskResult = SyntheticTaskResult(
            backend = "test",
            taskName = "test_task",
            converged = true,
            finalAccuracy = 0.95,
            convergenceEpoch = 50,
            trainingTimeMs = 1000L,
            inferenceTimeMs = 10L,
            finalLoss = 0.05,
            lossHistory = listOf(1.0, 0.5, 0.1, 0.05),
            accuracyHistory = listOf(0.5, 0.7, 0.9, 0.95)
        )
        
        assertEquals("test", taskResult.backend)
        assertEquals("test_task", taskResult.taskName)
        assertTrue(taskResult.converged)
        assertEquals(0.95, taskResult.finalAccuracy)
        assertEquals(50, taskResult.convergenceEpoch)
        
        val perfMetrics = PerformanceMetrics(
            backend = "test",
            avgInferenceTimeMs = 5.0,
            avgTrainingTimeMs = 100.0,
            memoryUsageMB = 256L,
            parameterCount = 10000L
        )
        
        assertEquals("test", perfMetrics.backend)
        assertEquals(5.0, perfMetrics.avgInferenceTimeMs)
        assertEquals(100.0, perfMetrics.avgTrainingTimeMs)
        assertEquals(256L, perfMetrics.memoryUsageMB)
        assertEquals(10000L, perfMetrics.parameterCount)
    }
}