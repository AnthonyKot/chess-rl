package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.test.*

/**
 * End-to-end tests that demonstrate the complete debugging system working together
 * from start to finish, proving all components integrate properly.
 */
class ProductionDebuggingE2ETest {
    
    private lateinit var agent: TestChessAgent
    private lateinit var environment: TestChessEnvironment
    private lateinit var debuggingInterface: ProductionDebuggingInterface
    
    @BeforeTest
    fun setup() {
        agent = TestChessAgent()
        environment = TestChessEnvironment()
        debuggingInterface = ProductionDebuggingInterface(agent, environment)
    }
    
    @Test
    fun testCompleteDebuggingWorkflow() {
        println("🔍 Starting Complete E2E Debugging Workflow Test")
        
        // 1. Start debugging session
        val session = debuggingInterface.startDebuggingSession("E2E Test Session")
        assertNotNull(session)
        println("✅ Session started: ${session.sessionId}")
        
        // 2. Create test training data
        val experiences = createRealisticTrainingData(1000)
        val gameHistory = createRealisticGameHistory()
        val trainingMetrics = createRealisticTrainingMetrics(50)
        
        // 3. Perform comprehensive buffer analysis
        println("\n📊 Step 1: Experience Buffer Analysis")
        val bufferInspection = debuggingInterface.inspectExperienceBuffer(
            experiences, AnalysisDepth.COMPREHENSIVE
        )
        
        assertNotNull(bufferInspection)
        assertEquals(1000, bufferInspection.basicAnalysis.bufferSize)
        assertTrue(bufferInspection.basicAnalysis.qualityScore >= 0.0)
        assertNotNull(bufferInspection.detailedAnalysis)
        assertNotNull(bufferInspection.episodeAnalysis)
        assertNotNull(bufferInspection.correlationAnalysis)
        
        println("   Buffer quality: ${(bufferInspection.basicAnalysis.qualityScore * 100).toInt()}%")
        println("   Issues found: ${bufferInspection.basicAnalysis.qualityIssues.size}")
        println("   Recommendations: ${bufferInspection.basicAnalysis.recommendations.size}")
        
        // 4. Perform interactive game analysis
        println("\n🎮 Step 2: Interactive Game Analysis")
        val gameAnalysisSession = debuggingInterface.analyzeGameInteractively(
            gameHistory = gameHistory,
            annotations = mapOf(
                0 to "Opening move",
                5 to "Development phase",
                15 to "Middlegame begins"
            )
        )
        
        assertNotNull(gameAnalysisSession)
        assertEquals(gameHistory, gameAnalysisSession.gameHistory)
        
        // Step through several moves
        val analysisSteps = mutableListOf<GameAnalysisStep>()
        repeat(5) {
            val step = debuggingInterface.stepThroughGame(gameAnalysisSession, StepDirection.FORWARD)
            analysisSteps.add(step)
            assertNotNull(step.positionAnalysis)
            assertTrue(step.moveNumber > 0)
        }
        
        println("   Analyzed ${analysisSteps.size} positions")
        println("   Average confidence: ${analysisSteps.map { it.positionAnalysis.decisionAnalysis.decisionConfidence }.average()}")
        
        // 5. Perform neural network analysis
        println("\n🧠 Step 3: Neural Network Analysis")
        val board = ChessBoard()
        val networkAnalysis = debuggingInterface.analyzeNeuralNetworkActivations(
            board = board,
            includeLayerAnalysis = true,
            includeAttentionMaps = true
        )
        
        assertNotNull(networkAnalysis)
        assertTrue(networkAnalysis.layerActivations.isNotEmpty())
        assertTrue(networkAnalysis.attentionMaps.isNotEmpty())
        assertNotNull(networkAnalysis.activationPatterns)
        
        println("   Layers analyzed: ${networkAnalysis.layerActivations.size}")
        println("   Network issues: ${networkAnalysis.networkIssues.size}")
        println("   Activation health: ${networkAnalysis.activationPatterns.activationHealth}")
        
        // 6. Perform training pipeline debugging
        println("\n🔧 Step 4: Training Pipeline Debugging")
        val pipelineDebug = debuggingInterface.debugTrainingPipeline(
            experiences = experiences.take(100), // Use subset for pipeline analysis
            trainingConfig = "test_config"
        )
        
        assertNotNull(pipelineDebug)
        assertNotNull(pipelineDebug.bufferAnalysis)
        assertNotNull(pipelineDebug.batchAnalysis)
        assertNotNull(pipelineDebug.forwardPassAnalysis)
        assertNotNull(pipelineDebug.lossAnalysis)
        assertNotNull(pipelineDebug.backwardPassAnalysis)
        assertTrue(pipelineDebug.recommendations.isNotEmpty())
        
        println("   Pipeline components analyzed: 5")
        println("   Recommendations generated: ${pipelineDebug.recommendations.size}")
        
        // 7. Perform performance profiling
        println("\n⚡ Step 5: Performance Profiling")
        val systemMetrics = SystemMetrics(
            cpuUtilization = 0.75,
            memoryUsage = 0.60,
            gpuUtilization = 0.85,
            throughput = 150.0
        )
        
        val performanceResult = debuggingInterface.profilePerformance(
            trainingMetrics = trainingMetrics,
            systemMetrics = systemMetrics
        )
        
        assertNotNull(performanceResult)
        assertNotNull(performanceResult.trainingPerformance)
        assertNotNull(performanceResult.systemPerformance)
        assertTrue(performanceResult.optimizationRecommendations.isNotEmpty())
        
        println("   Training stability: ${(performanceResult.trainingPerformance.trainingStability * 100).toInt()}%")
        println("   System bottlenecks: ${performanceResult.systemPerformance!!.bottlenecks.size}")
        println("   Optimization recommendations: ${performanceResult.optimizationRecommendations.size}")
        
        // 8. Test manual play functionality
        println("\n🎯 Step 6: Manual Play Testing")
        val playSession = debuggingInterface.startManualPlaySession(PieceColor.WHITE)
        assertNotNull(playSession)
        
        // Make a test move
        val testMove = Move(Position(1, 4), Position(3, 4)) // e2-e4
        environment.addValidMove(testMove)
        
        val playResult = debuggingInterface.makePlayerMove(playSession, testMove)
        assertTrue(playResult is ManualPlayResult.MoveMade)
        
        val moveResult = playResult as ManualPlayResult.MoveMade
        assertEquals(testMove, moveResult.playerMove)
        assertNotNull(moveResult.agentMove)
        assertNotNull(moveResult.positionAnalysis)
        
        println("   Manual play session created successfully")
        println("   Player move: ${moveResult.playerMove.toAlgebraic()}")
        println("   Agent response: ${moveResult.agentMove.toAlgebraic()}")
        
        // 9. Generate comprehensive debugging report
        println("\n📋 Step 7: Report Generation")
        val report = debuggingInterface.generateDebuggingReport(session)
        
        assertNotNull(report)
        assertEquals(session.sessionId, report.sessionId)
        assertTrue(report.sessionDuration > 0)
        assertTrue(report.analysisResults.size >= 6) // Should have results from all analyses
        
        println("   Report generated successfully")
        println("   Session duration: ${report.sessionDuration}ms")
        println("   Total analyses: ${report.analysisResults.size}")
        println("   Total recommendations: ${report.recommendations.size}")
        
        // 10. Test data export functionality
        println("\n💾 Step 8: Data Export")
        val exportResult = debuggingInterface.exportDebuggingData(
            session = session,
            format = ExportFormat.JSON,
            includeSensitiveData = false
        )
        
        assertTrue(exportResult is ExportResult.Success)
        val successResult = exportResult as ExportResult.Success
        assertTrue(successResult.filename.endsWith(".json"))
        
        println("   Data exported successfully: ${successResult.filename}")
        
        // 11. Verify all components worked together
        println("\n✅ Step 9: Integration Verification")
        
        // Verify session tracked all activities
        assertTrue(session.analysisResults.size >= 6, "Session should track all analyses")
        assertTrue(session.recommendations.isNotEmpty(), "Session should have recommendations")
        
        // Verify cross-component data consistency
        val bufferQuality = bufferInspection.basicAnalysis.qualityScore
        val pipelineBufferQuality = pipelineDebug.bufferAnalysis.qualityScore
        assertEquals(bufferQuality, pipelineBufferQuality, 0.01, "Buffer quality should be consistent across components")
        
        println("   ✅ All components integrated successfully")
        println("   ✅ Data consistency verified")
        println("   ✅ Session tracking verified")
        
        println("\n🎉 Complete E2E Debugging Workflow Test PASSED!")
    }
    
    @Test
    fun testRealWorldDebuggingScenario() {
        println("🌍 Testing Real-World Debugging Scenario")
        
        // Simulate a real debugging scenario: agent performance has degraded
        val session = debuggingInterface.startDebuggingSession("Performance Degradation Investigation")
        
        // 1. Create problematic training data (low quality)
        val problematicExperiences = createProblematicTrainingData()
        val degradedMetrics = createDegradedTrainingMetrics()
        
        // 2. Investigate buffer quality issues
        println("\n🔍 Investigating buffer quality...")
        val bufferAnalysis = debuggingInterface.inspectExperienceBuffer(
            problematicExperiences, AnalysisDepth.DETAILED
        )
        
        // Should detect quality issues
        assertTrue(bufferAnalysis.basicAnalysis.qualityScore < 0.6, "Should detect low quality")
        assertTrue(bufferAnalysis.basicAnalysis.qualityIssues.isNotEmpty(), "Should identify specific issues")
        
        println("   Quality score: ${(bufferAnalysis.basicAnalysis.qualityScore * 100).toInt()}%")
        println("   Issues detected: ${bufferAnalysis.basicAnalysis.qualityIssues.size}")
        
        // 3. Investigate network behavior
        println("\n🧠 Investigating network behavior...")
        val board = ChessBoard()
        val confidenceAnalysis = debuggingInterface.analyzeNeuralNetworkActivations(board)
        
        // Check for network issues
        val hasNetworkIssues = confidenceAnalysis.networkIssues.isNotEmpty()
        println("   Network issues found: $hasNetworkIssues")
        if (hasNetworkIssues) {
            confidenceAnalysis.networkIssues.forEach { issue ->
                println("   - ${issue.type}: ${issue.description}")
            }
        }
        
        // 4. Investigate training stability
        println("\n📈 Investigating training stability...")
        val performanceAnalysis = debuggingInterface.profilePerformance(degradedMetrics)
        
        val stability = performanceAnalysis.trainingPerformance.trainingStability
        println("   Training stability: ${(stability * 100).toInt()}%")
        
        // Should detect instability
        assertTrue(stability < 0.8, "Should detect training instability")
        
        // 5. Generate actionable recommendations
        println("\n💡 Generating recommendations...")
        val allRecommendations = mutableListOf<String>()
        allRecommendations.addAll(bufferAnalysis.basicAnalysis.recommendations)
        allRecommendations.addAll(performanceAnalysis.optimizationRecommendations.map { it.description })
        
        assertTrue(allRecommendations.isNotEmpty(), "Should generate recommendations")
        println("   Total recommendations: ${allRecommendations.size}")
        
        // 6. Verify debugging identified root causes
        val qualityIssues = bufferAnalysis.basicAnalysis.qualityIssues
        val hasRewardSparsityIssue = qualityIssues.any { it.contains("sparse") }
        val hasActionDiversityIssue = qualityIssues.any { it.contains("diversity") }
        
        assertTrue(hasRewardSparsityIssue || hasActionDiversityIssue, 
                  "Should identify specific data quality issues")
        
        println("   ✅ Root cause analysis completed")
        println("   ✅ Actionable recommendations generated")
        
        // 7. Generate final report
        val report = debuggingInterface.generateDebuggingReport(session)
        assertTrue(report.issuesFound.isNotEmpty(), "Report should document issues found")
        assertTrue(report.recommendations.isNotEmpty(), "Report should include recommendations")
        
        println("\n🎉 Real-World Debugging Scenario Test PASSED!")
    }
    
    @Test
    fun testContinuousMonitoringWorkflow() {
        println("📊 Testing Continuous Monitoring Workflow")
        
        val session = debuggingInterface.startDebuggingSession("Continuous Monitoring")
        
        // Simulate continuous training monitoring
        val monitoringResults = mutableListOf<MonitoringSnapshot>()
        
        repeat(10) { episode ->
            // Generate training data for this episode
            val episodeExperiences = createRealisticTrainingData(100)
            val episodeMetrics = createSingleTrainingMetric(episode)
            
            // Perform monitoring analysis
            val bufferAnalysis = debuggingInterface.inspectExperienceBuffer(
                episodeExperiences, AnalysisDepth.BASIC
            )
            
            val snapshot = MonitoringSnapshot(
                episode = episode,
                qualityScore = bufferAnalysis.basicAnalysis.qualityScore,
                hasIssues = bufferAnalysis.basicAnalysis.qualityIssues.isNotEmpty(),
                timestamp = getCurrentTimeMillis()
            )
            
            monitoringResults.add(snapshot)
            
            // Simulate alerting on quality degradation
            if (snapshot.qualityScore < 0.5) {
                println("   ⚠️ Episode $episode: Quality alert (${(snapshot.qualityScore * 100).toInt()}%)")
            }
        }
        
        // Analyze monitoring trends
        val qualityTrend = calculateQualityTrend(monitoringResults)
        val issueRate = monitoringResults.count { it.hasIssues }.toDouble() / monitoringResults.size
        
        println("   Episodes monitored: ${monitoringResults.size}")
        println("   Quality trend: ${if (qualityTrend > 0) "Improving" else if (qualityTrend < 0) "Declining" else "Stable"}")
        println("   Issue rate: ${(issueRate * 100).toInt()}%")
        
        // Verify monitoring captured quality changes
        assertTrue(monitoringResults.isNotEmpty(), "Should capture monitoring data")
        assertTrue(monitoringResults.all { it.qualityScore >= 0.0 }, "All quality scores should be valid")
        
        println("   ✅ Continuous monitoring workflow verified")
        
        println("\n🎉 Continuous Monitoring Workflow Test PASSED!")
    }
    
    @Test
    fun testDebuggingToolsIntegration() {
        println("🔗 Testing Integration Between All Debugging Tools")
        
        val session = debuggingInterface.startDebuggingSession("Integration Test")
        
        // 1. Test that ManualValidationTools integration works
        val board = ChessBoard()
        val positionAnalysis = debuggingInterface.analyzeCurrentPosition(board)
        
        // Should use ManualValidationTools internally
        assertNotNull(positionAnalysis.decisionAnalysis)
        assertNotNull(positionAnalysis.positionEvaluation)
        assertEquals(board.toFEN(), positionAnalysis.position)
        
        // 2. Test that ValidationConsole integration works
        // (This would be tested through the display methods in a real scenario)
        
        // 3. Test that TrainingDebugger integration works
        val experiences = createRealisticTrainingData(50)
        val pipelineDebug = debuggingInterface.debugTrainingPipeline(experiences, "test")
        
        // Should use TrainingDebugger capabilities
        assertNotNull(pipelineDebug.bufferAnalysis)
        assertTrue(pipelineDebug.recommendations.isNotEmpty())
        
        // 4. Test that enhanced episode tracking works
        val gameHistory = createRealisticGameHistory()
        val gameAnalysis = debuggingInterface.analyzeGameInteractively(gameHistory)
        
        // Should track episodes properly
        assertNotNull(gameAnalysis)
        assertEquals(gameHistory.size, gameAnalysis.gameHistory.size)
        
        // 5. Test cross-component data consistency
        val bufferAnalysis1 = debuggingInterface.inspectExperienceBuffer(experiences, AnalysisDepth.BASIC)
        val bufferAnalysis2 = debuggingInterface.inspectExperienceBuffer(experiences, AnalysisDepth.BASIC)
        
        // Should produce consistent results
        assertEquals(bufferAnalysis1.basicAnalysis.qualityScore, 
                    bufferAnalysis2.basicAnalysis.qualityScore, 0.001)
        
        println("   ✅ ManualValidationTools integration verified")
        println("   ✅ TrainingDebugger integration verified")
        println("   ✅ Episode tracking integration verified")
        println("   ✅ Data consistency across components verified")
        
        println("\n🎉 Debugging Tools Integration Test PASSED!")
    }
    
    // Helper methods for creating realistic test data
    
    private fun createRealisticTrainingData(size: Int): List<Experience<DoubleArray, Int>> {
        return (0 until size).map { i ->
            Experience(
                state = DoubleArray(64) { 
                    // Create more realistic state values
                    when (kotlin.random.Random.nextInt(3)) {
                        0 -> 0.0 // Empty square
                        1 -> kotlin.random.Random.nextDouble(-1.0, 1.0) // Piece value
                        else -> kotlin.random.Random.nextDouble(0.1, 0.9) // Position value
                    }
                },
                action = kotlin.random.Random.nextInt(4096),
                reward = when {
                    i % 20 == 19 -> 1.0 // Win reward
                    i % 20 == 0 -> -1.0 // Loss reward
                    i % 10 == 5 -> 0.1 // Small positive reward
                    else -> 0.0 // No reward
                },
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble(-1.0, 1.0) },
                done = i % 20 == 19 // Episode ends every 20 steps
            )
        }
    }
    
    private fun createRealisticGameHistory(): List<Move> {
        return listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4)), // e7-e5
            Move(Position(0, 6), Position(2, 5)), // Ng1-f3
            Move(Position(7, 1), Position(5, 2)), // Nb8-c6
            Move(Position(0, 5), Position(3, 2)), // Bf1-c4
            Move(Position(7, 5), Position(4, 2)), // Bf8-c5
            Move(Position(1, 3), Position(3, 3)), // d2-d3
            Move(Position(6, 3), Position(4, 3)), // d7-d6
            Move(Position(0, 2), Position(2, 4)), // Bc1-e3
            Move(Position(7, 2), Position(5, 4)), // Bc8-e6
            Move(Position(1, 7), Position(2, 7)), // h2-h3
            Move(Position(6, 7), Position(5, 7)), // h7-h6
            Move(Position(0, 3), Position(1, 3)), // Qd1-d2
            Move(Position(7, 3), Position(6, 3)), // Qd8-d7
            Move(Position(0, 0), Position(0, 3)), // Ra1-d1 (castling queenside)
        )
    }
    
    private fun createRealisticTrainingMetrics(count: Int): List<RLMetrics> {
        return (0 until count).map { i ->
            val progress = i.toDouble() / count
            RLMetrics(
                episode = i,
                averageReward = -0.5 + progress * 1.5 + kotlin.random.Random.nextGaussian() * 0.1,
                policyLoss = 2.0 * (1.0 - progress) + kotlin.random.Random.nextGaussian() * 0.2,
                policyEntropy = 3.0 * (1.0 - progress * 0.8) + kotlin.random.Random.nextGaussian() * 0.1,
                explorationRate = 1.0 - progress * 0.9,
                gradientNorm = 1.0 + kotlin.random.Random.nextGaussian() * 0.3,
                qValueStats = QValueStats(
                    mean = kotlin.random.Random.nextGaussian() * 0.5,
                    variance = kotlin.random.Random.nextDouble(0.1, 1.0),
                    min = kotlin.random.Random.nextDouble(-2.0, 0.0),
                    max = kotlin.random.Random.nextDouble(0.0, 2.0)
                )
            )
        }
    }
    
    private fun createProblematicTrainingData(): List<Experience<DoubleArray, Int>> {
        return (0 until 500).map { i ->
            Experience(
                state = DoubleArray(64) { 0.1 }, // Low variance states
                action = 0, // No action diversity
                reward = 0.0, // Sparse rewards
                nextState = DoubleArray(64) { 0.1 },
                done = i % 100 == 99 // Very long episodes
            )
        }
    }
    
    private fun createDegradedTrainingMetrics(): List<RLMetrics> {
        return (0 until 20).map { i ->
            RLMetrics(
                episode = i,
                averageReward = kotlin.random.Random.nextGaussian() * 0.1, // No improvement
                policyLoss = 2.0 + kotlin.random.Random.nextGaussian() * 1.0, // High, unstable loss
                policyEntropy = 0.1 + kotlin.random.Random.nextGaussian() * 0.05, // Low entropy
                explorationRate = 0.01, // Very low exploration
                gradientNorm = 5.0 + kotlin.random.Random.nextGaussian() * 2.0, // High gradient norm
                qValueStats = null
            )
        }
    }
    
    private fun createSingleTrainingMetric(episode: Int): RLMetrics {
        return RLMetrics(
            episode = episode,
            averageReward = kotlin.random.Random.nextDouble(-0.5, 1.0),
            policyLoss = kotlin.random.Random.nextDouble(0.1, 2.0),
            policyEntropy = kotlin.random.Random.nextDouble(0.5, 3.0),
            explorationRate = kotlin.random.Random.nextDouble(0.1, 0.9),
            gradientNorm = kotlin.random.Random.nextDouble(0.5, 3.0),
            qValueStats = null
        )
    }
    
    private fun calculateQualityTrend(snapshots: List<MonitoringSnapshot>): Double {
        if (snapshots.size < 2) return 0.0
        
        val firstHalf = snapshots.take(snapshots.size / 2).map { it.qualityScore }.average()
        val secondHalf = snapshots.drop(snapshots.size / 2).map { it.qualityScore }.average()
        
        return secondHalf - firstHalf
    }
}

/**
 * Test implementations that provide realistic behavior for E2E testing
 */
class TestChessAgent : ChessAgent {
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        // Return a realistic action selection
        return validActions.randomOrNull() ?: 0
    }
    
    override fun getActionProbabilities(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        // Generate realistic probability distribution
        val probabilities = validActions.associateWith { kotlin.random.Random.nextDouble() }
        val sum = probabilities.values.sum()
        return probabilities.mapValues { it.value / sum }
    }
    
    override fun getQValues(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        // Generate realistic Q-values
        return validActions.associateWith { 
            kotlin.random.Random.nextGaussian() * 0.5 
        }
    }
    
    override fun updatePolicy(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        return PolicyUpdateResult(
            loss = kotlin.random.Random.nextDouble(0.1, 2.0),
            gradientNorm = kotlin.random.Random.nextDouble(0.5, 3.0),
            policyEntropy = kotlin.random.Random.nextDouble(0.5, 3.0),
            qValueMean = kotlin.random.Random.nextGaussian() * 0.5,
            targetValueMean = kotlin.random.Random.nextGaussian() * 0.5
        )
    }
    
    fun getPolicyOutput(state: DoubleArray): DoubleArray {
        // Generate realistic policy output
        return DoubleArray(4096) { 
            if (kotlin.random.Random.nextDouble() < 0.1) {
                kotlin.random.Random.nextDouble()
            } else {
                kotlin.random.Random.nextDouble() * 0.01
            }
        }
    }
    
    fun getValueOutput(state: DoubleArray): Double {
        return kotlin.random.Random.nextGaussian() * 0.5
    }
}

class TestChessEnvironment : ChessEnvironment {
    private val validMoves = mutableListOf<Move>()
    
    fun addValidMove(move: Move) {
        validMoves.add(move)
    }
    
    override fun reset(): DoubleArray {
        return DoubleArray(64) { 0.0 }
    }
    
    override fun step(action: Int): StepResult {
        return StepResult(
            nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
            reward = kotlin.random.Random.nextDouble(-1.0, 1.0),
            done = kotlin.random.Random.nextBoolean(),
            info = emptyMap()
        )
    }
    
    override fun getValidActions(state: DoubleArray): List<Int> {
        return (0..100).toList() // Return reasonable number of valid actions
    }
    
    override fun getCurrentBoard(): ChessBoard {
        return ChessBoard()
    }
    
    override fun getGameStatus(): GameStatus {
        return GameStatus.IN_PROGRESS
    }
    
    override fun getPositionEvaluation(color: PieceColor): Double {
        return kotlin.random.Random.nextGaussian() * 0.3
    }
    
    override fun loadFromFEN(fen: String) {
        // Mock implementation
    }
    
    fun getValidMoves(board: ChessBoard): List<Move> {
        return validMoves.ifEmpty {
            listOf(
                Move(Position(1, 4), Position(3, 4)), // e2-e4
                Move(Position(1, 3), Position(3, 3)), // d2-d4
                Move(Position(0, 6), Position(2, 5))  // Ng1-f3
            )
        }
    }
}

data class MonitoringSnapshot(
    val episode: Int,
    val qualityScore: Double,
    val hasIssues: Boolean,
    val timestamp: Long
)