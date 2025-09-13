package com.chessrl.integration

import kotlin.test.*

class SystemOptimizationBasicTest {
    
    @Test
    fun testJVMTrainingOptimizerBasics() {
        val optimizer = JVMTrainingOptimizer()
        val config = JVMTrainingOptimizer.JVMOptimizationConfig(
            batchSize = 64,
            memoryPoolSize = 10000,
            concurrentThreads = 2
        )
        
        val result = optimizer.optimizeForJVMTraining(config)
        
        // Verify basic structure
        assertNotNull(result)
        assertNotNull(result.recommendedConfig)
        assertTrue(result.totalOptimizationTime > 0)
        
        println("JVM optimization test passed - time: ${result.totalOptimizationTime}ms")
    }
    
    @Test
    fun testNativeDeploymentOptimizerBasics() {
        val optimizer = NativeDeploymentOptimizer()
        val config = NativeDeploymentOptimizer.NativeOptimizationConfig(
            optimizeForInference = true,
            minimizeMemoryFootprint = true
        )
        
        val result = optimizer.optimizeForNativeDeployment(config)
        
        // Verify basic structure
        assertNotNull(result)
        assertTrue(result.totalOptimizationTime > 0)
        
        println("Native optimization test passed - time: ${result.totalOptimizationTime}ms")
    }
    
    @Test
    fun testHyperparameterOptimizerBasics() {
        val optimizer = HyperparameterOptimizer()
        val baseConfig = HyperparameterOptimizer.HyperparameterConfig(
            learningRate = 0.001,
            batchSize = 64
        )
        
        val result = optimizer.optimizeHyperparameters(baseConfig, optimizationBudget = 5)
        
        // Verify basic structure
        assertNotNull(result)
        assertNotNull(result.optimalConfig)
        assertTrue(result.totalOptimizationTime > 0)
        assertTrue(result.improvementFactor >= 1.0)
        
        println("Hyperparameter optimization test passed - improvement: ${result.improvementFactor}x")
    }
    
    @Test
    fun testPerformanceBenchmarkSuiteBasics() {
        val benchmarkSuite = PerformanceBenchmarkSuite()
        val results = benchmarkSuite.runBenchmarks()
        
        // Verify all benchmarks ran
        assertNotNull(results.neuralNetwork)
        assertNotNull(results.chessEngine)
        assertNotNull(results.rlAlgorithm)
        assertNotNull(results.integration)
        
        // Check basic metrics
        assertTrue(results.neuralNetwork.duration > 0)
        assertTrue(results.chessEngine.duration > 0)
        assertTrue(results.rlAlgorithm.duration > 0)
        assertTrue(results.integration.duration > 0)
        
        assertTrue(results.overallScore >= 0.0)
        
        println("Benchmark suite test passed - overall score: ${results.overallScore}")
    }
    
    @Test
    fun testSystemOptimizationCoordinatorBasics() {
        val coordinator = SystemOptimizationCoordinator()
        val config = SystemOptimizationCoordinator.SystemOptimizationConfig(
            optimizeJVMTraining = true,
            optimizeNativeDeployment = false, // Skip to reduce test time
            optimizeHyperparameters = true,
            enablePerformanceMonitoring = false, // Skip to avoid conflicts
            hyperparameterBudget = 3 // Reduced for testing
        )
        
        val result = coordinator.performSystemOptimization(config)
        
        // Verify coordination worked
        assertNotNull(result)
        assertNotNull(result.jvmOptimization)
        assertNull(result.nativeOptimization) // Should be null since disabled
        assertNotNull(result.hyperparameterOptimization)
        assertNotNull(result.optimizationSummary)
        
        assertTrue(result.totalOptimizationTime > 0)
        assertTrue(result.optimizationSummary.totalSpeedupFactor >= 1.0)
        
        println("System coordination test passed - speedup: ${result.optimizationSummary.totalSpeedupFactor}x")
    }
    
    @Test
    fun testOptimizationGuidelinesGeneration() {
        val coordinator = SystemOptimizationCoordinator()
        val guidelines = coordinator.generateOptimizationGuidelines()
        
        // Verify all guideline categories exist
        assertTrue(guidelines.jvmTrainingGuidelines.isNotEmpty())
        assertTrue(guidelines.nativeDeploymentGuidelines.isNotEmpty())
        assertTrue(guidelines.hyperparameterTuningGuidelines.isNotEmpty())
        assertTrue(guidelines.performanceMonitoringGuidelines.isNotEmpty())
        assertTrue(guidelines.bestPractices.isNotEmpty())
        
        println("Guidelines generation test passed")
        println("- JVM guidelines: ${guidelines.jvmTrainingGuidelines.size}")
        println("- Native guidelines: ${guidelines.nativeDeploymentGuidelines.size}")
        println("- Hyperparameter guidelines: ${guidelines.hyperparameterTuningGuidelines.size}")
        println("- Monitoring guidelines: ${guidelines.performanceMonitoringGuidelines.size}")
        println("- Best practices: ${guidelines.bestPractices.size}")
    }
}