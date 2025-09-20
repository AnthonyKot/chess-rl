package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*
import kotlin.test.*

/**
 * Comprehensive test suite for the Robust Training Validator
 * 
 * Tests all aspects of robust training validation including:
 * - Learning vs. stagnation detection
 * - Baseline evaluation against heuristic opponents
 * - Chess progress tracking
 * - Early stopping detection
 * - Integration with existing validation components
 */
class RobustTrainingValidatorTest {
    
    private lateinit var validator: RobustTrainingValidator
    private lateinit var mockAgent: MockChessAgent
    private lateinit var mockEnvironment: MockChessEnvironment
    
    @BeforeTest
    fun setup() {
        validator = RobustTrainingValidator()
        mockAgent = MockChessAgent()
        mockEnvironment = MockChessEnvironment()
        validator.initialize()
    }
    
    @Test
    fun testValidatorInitialization() {
        // Test that validator initializes correctly
        val summary = validator.getValidationSummary()
        
        assertEquals(0, summary.totalValidations)
        assertEquals(0, summary.validValidations)
        assertEquals(0, summary.earlyStoppingSuggestions)
        assertEquals(0.0, summary.bestPerformanceScore)
        assertEquals(LearningStatusType.INSUFFICIENT_DATA, summary.currentLearningStatus)
    }
    
    @Test
    fun testLearningDetection() {
        // Test learning vs. stagnation detection
        
        // Simulate improving performance
        val improvingMetrics = listOf(
            createMockMetrics(0.3, 0.1, 0.5),
            createMockMetrics(0.4, 0.08, 0.4),
            createMockMetrics(0.5, 0.06, 0.3),
            createMockMetrics(0.6, 0.05, 0.2),
            createMockMetrics(0.7, 0.04, 0.1)
        )
        
        val gameResults = createMockGameResults(5, 0.7, 30)
        
        // Perform validations
        improvingMetrics.forEachIndexed { index, metrics ->
            val result = validator.validateTrainingCycle(
                cycle = index,
                agent = mockAgent,
                environment = mockEnvironment,
                trainingMetrics = metrics,
                updateResult = createMockUpdateResult(0.05, 0.5, 1.0),
                gameResults = gameResults
            )
            
            assertTrue(result.isValid, "Validation should be valid for improving performance")
            assertFalse(result.shouldStop, "Should not suggest stopping during improvement")
        }
        
        // Check that learning is detected
        val summary = validator.getValidationSummary()
        assertTrue(summary.validValidations > 0, "Should have valid validations")
        assertTrue(summary.bestPerformanceScore > 0.5, "Best performance should improve")
    }
    
    @Test
    fun testStagnationDetection() {
        // Test stagnation detection
        
        // Simulate stagnant performance
        val stagnantMetrics = (0..15).map {
            createMockMetrics(0.5, 0.1, 0.2) // Same performance
        }
        
        val gameResults = createMockGameResults(3, 0.5, 25)
        
        // Perform validations
        stagnantMetrics.forEachIndexed { index, metrics ->
            val result = validator.validateTrainingCycle(
                cycle = index,
                agent = mockAgent,
                environment = mockEnvironment,
                trainingMetrics = metrics,
                updateResult = createMockUpdateResult(0.1, 0.2, 1.0),
                gameResults = gameResults
            )
            
            if (index >= 10) {
                // Should detect stagnation after sufficient cycles
                assertEquals(LearningStatusType.STAGNANT, result.learningStatus.status)
            }
        }
    }
    
    @Test
    fun testBaselineEvaluation() {
        // Test baseline evaluation functionality
        
        val metrics = createMockMetrics(0.6, 0.05, 0.1)
        val gameResults = createMockGameResults(5, 0.7, 35)
        
        // Perform validation that should trigger baseline evaluation
        val result = validator.validateTrainingCycle(
            cycle = 0, // First cycle should trigger baseline
            agent = mockAgent,
            environment = mockEnvironment,
            trainingMetrics = metrics,
            updateResult = createMockUpdateResult(0.05, 0.6, 1.0),
            gameResults = gameResults
        )
        
        assertNotNull(result.baselineEvaluation, "Baseline evaluation should be performed")
        
        val baseline = result.baselineEvaluation!!
        assertTrue(baseline.overallScore >= 0.0, "Baseline score should be non-negative")
        assertTrue(baseline.randomOpponentResult.gamesPlayed > 0, "Should play games against random opponent")
        assertTrue(baseline.heuristicOpponentResult.gamesPlayed > 0, "Should play games against heuristic opponent")
        assertTrue(baseline.materialOpponentResult.gamesPlayed > 0, "Should play games against material opponent")
    }
    
    @Test
    fun testChessProgressTracking() {
        // Test chess-specific progress tracking
        
        val metrics = createMockMetrics(0.6, 0.05, 0.1)
        
        // Create game results with varying quality
        val goodGameResults = createMockGameResults(3, 0.8, 40, "WIN")
        val poorGameResults = createMockGameResults(2, 0.3, 15, "LOSS")
        
        val result1 = validator.validateTrainingCycle(
            cycle = 0,
            agent = mockAgent,
            environment = mockEnvironment,
            trainingMetrics = metrics,
            updateResult = createMockUpdateResult(0.05, 0.6, 1.0),
            gameResults = goodGameResults
        )
        
        val result2 = validator.validateTrainingCycle(
            cycle = 1,
            agent = mockAgent,
            environment = mockEnvironment,
            trainingMetrics = metrics,
            updateResult = createMockUpdateResult(0.05, 0.6, 1.0),
            gameResults = poorGameResults
        )
        
        assertNotNull(result1.progressUpdate, "Progress update should be available")
        assertNotNull(result2.progressUpdate, "Progress update should be available")
        
        val progress1 = result1.progressUpdate!!
        val progress2 = result2.progressUpdate!!
        
        assertTrue(progress1.chessImprovementScore > progress2.chessImprovementScore, 
                  "Good games should result in higher chess improvement score")
    }
    
    @Test
    fun testEarlyStoppingDetection() {
        // Test early stopping detection
        
        // Simulate declining performance
        val decliningMetrics = listOf(
            createMockMetrics(0.7, 0.05, 0.1),
            createMockMetrics(0.6, 0.06, 0.2),
            createMockMetrics(0.5, 0.08, 0.3),
            createMockMetrics(0.4, 0.12, 0.4),
            createMockMetrics(0.3, 0.15, 0.5)
        )
        
        val gameResults = createMockGameResults(3, 0.3, 20)
        
        // Perform validations
        decliningMetrics.forEachIndexed { index, metrics ->
            val result = validator.validateTrainingCycle(
                cycle = index,
                agent = mockAgent,
                environment = mockEnvironment,
                trainingMetrics = metrics,
                updateResult = createMockUpdateResult(0.15, 0.1, 2.0),
                gameResults = gameResults
            )
            
            if (index >= 3) {
                // Should suggest early stopping for declining performance
                assertEquals(LearningStatusType.DECLINING, result.learningStatus.status)
                assertTrue(result.earlyStoppingRecommendation.recommendations.isNotEmpty(),
                          "Should provide early stopping recommendations")
            }
        }
    }
    
    @Test
    fun testValidationIssueIntegration() {
        // Test integration with existing validation components
        
        val metrics = createMockMetrics(0.5, 0.1, 0.2)
        val gameResults = createMockGameResults(3, 0.5, 25)
        
        // Create update result with issues (high gradient norm)
        val problematicUpdate = createMockUpdateResult(15.0, 0.05, 1.0) // High gradient norm
        
        val result = validator.validateTrainingCycle(
            cycle = 0,
            agent = mockAgent,
            environment = mockEnvironment,
            trainingMetrics = metrics,
            updateResult = problematicUpdate,
            gameResults = gameResults
        )
        
        assertFalse(result.isValid, "Validation should be invalid with high gradient norm")
        assertTrue(result.policyValidation.issues.isNotEmpty(), "Should detect policy validation issues")
        assertTrue(result.recommendations.isNotEmpty(), "Should provide recommendations for issues")
    }
    
    @Test
    fun testConvergenceDetection() {
        // Test convergence detection
        
        // Simulate converged performance (stable high performance)
        val convergedMetrics = (0..25).map {
            createMockMetrics(0.85 + (kotlin.random.Random.nextDouble() - 0.5) * 0.02, 0.02, 0.05)
        }
        
        val gameResults = createMockGameResults(5, 0.85, 45)
        
        // Perform validations
        convergedMetrics.forEachIndexed { index, metrics ->
            val result = validator.validateTrainingCycle(
                cycle = index,
                agent = mockAgent,
                environment = mockEnvironment,
                trainingMetrics = metrics,
                updateResult = createMockUpdateResult(0.02, 0.8, 0.5),
                gameResults = gameResults
            )
            
            if (index >= 20) {
                // Should detect convergence after sufficient stable performance
                assertTrue(result.convergenceStatus.hasConverged || result.convergenceStatus.confidence > 0.7,
                          "Should detect convergence or high confidence")
            }
        }
    }
    
    @Test
    fun testRecommendationGeneration() {
        // Test comprehensive recommendation generation
        
        val metrics = createMockMetrics(0.3, 0.15, 0.4) // Poor performance
        val gameResults = createMockGameResults(2, 0.2, 12) // Poor games
        
        val result = validator.validateTrainingCycle(
            cycle = 0,
            agent = mockAgent,
            environment = mockEnvironment,
            trainingMetrics = metrics,
            updateResult = createMockUpdateResult(0.15, 0.05, 1.5),
            gameResults = gameResults
        )
        
        assertTrue(result.recommendations.isNotEmpty(), "Should provide recommendations for poor performance")
        
        // Check that recommendations are prioritized
        val highPriorityRecs = result.recommendations.filter { it.priority == RecommendationPriority.HIGH }
        val mediumPriorityRecs = result.recommendations.filter { it.priority == RecommendationPriority.MEDIUM }
        
        assertTrue(highPriorityRecs.isNotEmpty() || mediumPriorityRecs.isNotEmpty(),
                  "Should have prioritized recommendations")
    }
    
    @Test
    fun testValidationDataExport() {
        // Test validation data export functionality
        
        val metrics = createMockMetrics(0.6, 0.05, 0.1)
        val gameResults = createMockGameResults(3, 0.6, 30)
        
        // Perform a few validations
        repeat(3) { cycle ->
            validator.validateTrainingCycle(
                cycle = cycle,
                agent = mockAgent,
                environment = mockEnvironment,
                trainingMetrics = metrics,
                updateResult = createMockUpdateResult(0.05, 0.6, 1.0),
                gameResults = gameResults
            )
        }
        
        val exportData = validator.exportValidationData("json")
        
        assertEquals("json", exportData.format)
        assertEquals(3, exportData.validationHistory.size)
        assertTrue(exportData.timestamp > 0)
        assertNotNull(exportData.summary)
    }
    
    // Helper methods for creating mock data
    
    private fun createMockMetrics(
        averageReward: Double,
        policyLoss: Double,
        explorationRate: Double
    ): RLMetrics {
        return RLMetrics(
            averageReward = averageReward,
            policyLoss = policyLoss,
            explorationRate = explorationRate,
            episodeLength = 50.0,
            qValueStats = QValueStats(averageReward * 2, 1.0, averageReward * 3, averageReward)
        )
    }
    
    private fun createMockUpdateResult(
        gradientNorm: Double,
        policyEntropy: Double,
        loss: Double
    ): PolicyUpdateResult {
        return PolicyUpdateResult(
            loss = loss,
            gradientNorm = gradientNorm,
            policyEntropy = policyEntropy,
            qValueMean = 1.0,
            targetValueMean = 0.9
        )
    }
    
    private fun createMockGameResults(
        count: Int,
        winRate: Double,
        averageMoves: Int,
        outcome: String = "WIN"
    ): List<ChessGameResult> {
        return (0 until count).map { index ->
            val actualOutcome = if (index < (count * winRate).toInt()) outcome else "LOSS"
            ChessGameResult(
                outcome = actualOutcome,
                moveCount = averageMoves + (kotlin.random.Random.nextInt(-5, 6)),
                moves = (0 until averageMoves).map { "move$it" },
                legalMoveRate = 0.95 + kotlin.random.Random.nextDouble() * 0.05,
                gameLength = (averageMoves * 1000).toLong()
            )
        }
    }
    
    // Mock classes for testing
    
    private class MockChessAgent : ChessAgent {
        override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
            return validActions.firstOrNull() ?: 0
        }
        
        override fun learn(experience: Experience<DoubleArray, Int>) {}
        
        override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
            return actions.associateWith { 0.5 }
        }
        
        override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
            return actions.associateWith { 1.0 / actions.size }
        }
        
        override fun getTrainingMetrics(): ChessAgentMetrics {
            return ChessAgentMetrics(0.5, 0.1, 100, 0.1, 0.5)
        }
        
        override fun forceUpdate() {}
        override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
            return PolicyUpdateResult(0.1, 1.0, 0.5)
        }
        override fun save(path: String) {}
        override fun load(path: String) {}
        override fun reset() {}
        override fun setExplorationRate(rate: Double) {}
        override fun getConfig(): ChessAgentConfig = ChessAgentConfig()
    }
    
    private class MockChessEnvironment : ChessEnvironment {
        private var terminal = false
        
        override fun reset(): DoubleArray {
            terminal = false
            return DoubleArray(776) { 0.0 }
        }
        
        override fun step(action: Int): StepResult<DoubleArray> {
            terminal = kotlin.random.Random.nextBoolean()
            return StepResult(
                nextState = DoubleArray(776) { kotlin.random.Random.nextDouble() },
                reward = kotlin.random.Random.nextDouble() - 0.5,
                done = terminal
            )
        }
        
        override fun getValidActions(): List<Int> {
            return (0..10).toList()
        }
        
        override fun isTerminal(): Boolean = terminal
        
        override fun getCurrentBoard(): ChessBoard {
            return ChessBoard() // Mock implementation
        }
        
        override fun getStateSize(): Int = 776
        override fun getActionSize(): Int = 4096
    }
}