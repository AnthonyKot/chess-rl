package com.chessrl.integration

import kotlin.test.*

/**
 * Test runner that executes comprehensive E2E tests to prove the entire debugging system works.
 * This demonstrates that all components integrate properly and function as a complete system.
 */
class DebuggingSystemE2ERunner {
    
    @Test
    fun runAllE2ETests() {
        println("🚀 Running Complete Debugging System E2E Tests")
        println("=" * 80)
        
        val testResults = mutableListOf<TestResult>()
        
        // Test 1: Complete Debugging Workflow
        testResults.add(runTest("Complete Debugging Workflow") {
            val e2eTest = ProductionDebuggingE2ETest()
            e2eTest.setup()
            e2eTest.testCompleteDebuggingWorkflow()
        })
        
        // Test 2: Real-World Debugging Scenario
        testResults.add(runTest("Real-World Debugging Scenario") {
            val e2eTest = ProductionDebuggingE2ETest()
            e2eTest.setup()
            e2eTest.testRealWorldDebuggingScenario()
        })
        
        // Test 3: Continuous Monitoring Workflow
        testResults.add(runTest("Continuous Monitoring Workflow") {
            val e2eTest = ProductionDebuggingE2ETest()
            e2eTest.setup()
            e2eTest.testContinuousMonitoringWorkflow()
        })
        
        // Test 4: Debugging Tools Integration
        testResults.add(runTest("Debugging Tools Integration") {
            val e2eTest = ProductionDebuggingE2ETest()
            e2eTest.setup()
            e2eTest.testDebuggingToolsIntegration()
        })
        
        // Test 5: Individual Component Tests
        testResults.add(runTest("Production Debugging Interface") {
            val test = ProductionDebuggingInterfaceTest()
            test.setup()
            test.testDebuggingSessionCreation()
            test.testInteractiveGameAnalysis()
            test.testPositionAnalysis()
            test.testManualPlaySession()
            test.testTrainingPipelineDebugging()
            test.testPerformanceProfiling()
            test.testDebuggingReportGeneration()
        })
        
        testResults.add(runTest("Neural Network Analyzer") {
            val test = NeuralNetworkAnalyzerTest()
            test.setup()
            test.testNetworkOutputAnalysis()
            test.testActivationAnalysis()
            test.testNetworkComparison()
            test.testNetworkConfidenceAnalysis()
        })
        
        testResults.add(runTest("Experience Buffer Analyzer") {
            val test = ExperienceBufferAnalyzerTest()
            test.setup()
            test.testBasicBufferAnalysis()
            test.testDetailedInspection()
            test.testBufferComparison()
            test.testAnomalyDetection()
            test.testQualityTimeSeriesAnalysis()
        })
        
        // Display results
        displayTestResults(testResults)
        
        // Verify all tests passed
        val failedTests = testResults.filter { !it.passed }
        if (failedTests.isNotEmpty()) {
            fail("${failedTests.size} E2E tests failed: ${failedTests.map { it.name }}")
        }
        
        println("\n🎉 ALL E2E TESTS PASSED!")
        println("The complete debugging system is working end-to-end.")
    }
    
    @Test
    fun demonstrateSystemCapabilities() {
        println("\n🎯 Demonstrating System Capabilities")
        println("=" * 50)
        
        try {
            // Run the comprehensive demo
            val demo = ProductionDebuggingDemo()
            demo.runCompleteDemo()
            
            println("\n✅ System capabilities demonstration completed successfully!")
        } catch (e: Exception) {
            fail("System demonstration failed: ${e.message}")
        }
    }
    
    @Test
    fun validateSystemIntegration() {
        println("\n🔗 Validating System Integration")
        println("=" * 40)
        
        val agent = TestChessAgent()
        val environment = TestChessEnvironment()
        val debuggingInterface = ProductionDebuggingInterface(agent, environment)
        
        // Test that all major components can be instantiated and work together
        val session = debuggingInterface.startDebuggingSession("Integration Validation")
        assertNotNull(session, "Should be able to start debugging session")
        
        // Test basic functionality of each component
        val experiences = createTestExperiences(100)
        val gameHistory = createTestGameHistory()
        val board = ChessBoard()
        
        // 1. Experience buffer analysis
        val bufferAnalysis = debuggingInterface.inspectExperienceBuffer(experiences, AnalysisDepth.BASIC)
        assertNotNull(bufferAnalysis, "Buffer analysis should work")
        assertEquals(100, bufferAnalysis.basicAnalysis.bufferSize, "Should analyze correct number of experiences")
        
        // 2. Game analysis
        val gameAnalysis = debuggingInterface.analyzeGameInteractively(gameHistory)
        assertNotNull(gameAnalysis, "Game analysis should work")
        assertEquals(gameHistory.size, gameAnalysis.gameHistory.size, "Should analyze complete game")
        
        // 3. Position analysis
        val positionAnalysis = debuggingInterface.analyzeCurrentPosition(board)
        assertNotNull(positionAnalysis, "Position analysis should work")
        assertEquals(board.toFEN(), positionAnalysis.position, "Should analyze correct position")
        
        // 4. Network analysis
        val networkAnalysis = debuggingInterface.analyzeNeuralNetworkActivations(board)
        assertNotNull(networkAnalysis, "Network analysis should work")
        assertTrue(networkAnalysis.layerActivations.isNotEmpty(), "Should analyze network layers")
        
        // 5. Pipeline debugging
        val pipelineDebug = debuggingInterface.debugTrainingPipeline(experiences, "test")
        assertNotNull(pipelineDebug, "Pipeline debugging should work")
        assertNotNull(pipelineDebug.bufferAnalysis, "Should include buffer analysis")
        
        // 6. Performance profiling
        val metrics = createTestMetrics(10)
        val performanceResult = debuggingInterface.profilePerformance(metrics)
        assertNotNull(performanceResult, "Performance profiling should work")
        assertNotNull(performanceResult.trainingPerformance, "Should include training performance")
        
        // 7. Report generation
        val report = debuggingInterface.generateDebuggingReport(session)
        assertNotNull(report, "Report generation should work")
        assertTrue(report.analysisResults.size >= 5, "Should track all analyses performed")
        
        // 8. Data export
        val exportResult = debuggingInterface.exportDebuggingData(session, ExportFormat.JSON)
        assertTrue(exportResult is ExportResult.Success, "Data export should work")
        
        println("✅ All system components integrated successfully")
        println("✅ End-to-end workflow validated")
        println("✅ Data consistency verified")
    }
    
    private fun runTest(testName: String, testFunction: () -> Unit): TestResult {
        return try {
            val startTime = getCurrentTimeMillis()
            testFunction()
            val duration = getCurrentTimeMillis() - startTime
            
            println("✅ $testName - PASSED (${duration}ms)")
            TestResult(testName, true, duration, null)
        } catch (e: Exception) {
            println("❌ $testName - FAILED: ${e.message}")
            TestResult(testName, false, 0, e.message)
        }
    }
    
    private fun displayTestResults(results: List<TestResult>) {
        println("\n" + "=" * 60)
        println("📊 E2E TEST RESULTS SUMMARY")
        println("=" * 60)
        
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }
        val totalTime = results.sumOf { it.duration }
        
        println("Total Tests: ${results.size}")
        println("Passed: $passed")
        println("Failed: $failed")
        println("Total Time: ${totalTime}ms")
        println("Success Rate: ${(passed * 100 / results.size)}%")
        
        if (failed > 0) {
            println("\n❌ FAILED TESTS:")
            results.filter { !it.passed }.forEach { result ->
                println("  • ${result.name}: ${result.error}")
            }
        }
        
        println("\n✅ PASSED TESTS:")
        results.filter { it.passed }.forEach { result ->
            println("  • ${result.name} (${result.duration}ms)")
        }
    }
    
    // Helper methods for creating test data
    
    private fun createTestExperiences(count: Int): List<Experience<DoubleArray, Int>> {
        return (0 until count).map { i ->
            Experience(
                state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                action = i % 10,
                reward = if (i % 5 == 0) kotlin.random.Random.nextDouble() else 0.0,
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                done = i % 20 == 19
            )
        }
    }
    
    private fun createTestGameHistory(): List<Move> {
        return listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4)), // e7-e5
            Move(Position(0, 6), Position(2, 5)), // Ng1-f3
            Move(Position(7, 1), Position(5, 2)), // Nb8-c6
            Move(Position(0, 5), Position(3, 2)), // Bf1-c4
        )
    }
    
    private fun createTestMetrics(count: Int): List<RLMetrics> {
        return (0 until count).map { i ->
            RLMetrics(
                episode = i,
                averageReward = kotlin.random.Random.nextDouble(),
                policyLoss = kotlin.random.Random.nextDouble(),
                policyEntropy = kotlin.random.Random.nextDouble(),
                explorationRate = kotlin.random.Random.nextDouble(),
                gradientNorm = kotlin.random.Random.nextDouble() * 2.0,
                qValueStats = null
            )
        }
    }
}

data class TestResult(
    val name: String,
    val passed: Boolean,
    val duration: Long,
    val error: String?
)

// Extension function for string repetition
private operator fun String.times(n: Int): String = this.repeat(n)