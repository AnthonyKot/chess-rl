package com.chessrl.integration

import com.chessrl.chess.*
import kotlin.test.*

/**
 * Tests for neural network analyzer reliability and accuracy
 */
class NeuralNetworkAnalyzerTest {
    
    private lateinit var mockAgent: MockChessAgent
    private lateinit var analyzer: NeuralNetworkAnalyzer
    
    @BeforeTest
    fun setup() {
        mockAgent = MockChessAgent()
        analyzer = NeuralNetworkAnalyzer(mockAgent)
    }
    
    @Test
    fun testNetworkOutputAnalysis() {
        val board = ChessBoard()
        val output = analyzer.analyzeNetworkOutput(board)
        
        assertNotNull(output)
        assertEquals(board.toFEN(), output.position)
        assertTrue(output.policyOutput.isNotEmpty())
        assertTrue(output.qValues.isNotEmpty())
        assertNotNull(output.policyAnalysis)
        assertNotNull(output.valueAnalysis)
        assertNotNull(output.qValueAnalysis)
        assertTrue(output.timestamp > 0)
    }
    
    @Test
    fun testPolicyDistributionAnalysis() {
        val board = ChessBoard()
        val output = analyzer.analyzeNetworkOutput(board)
        val policyAnalysis = output.policyAnalysis
        
        assertTrue(policyAnalysis.totalValidProbability >= 0.0)
        assertTrue(policyAnalysis.totalValidProbability <= 1.0)
        assertTrue(policyAnalysis.entropy >= 0.0)
        assertTrue(policyAnalysis.maxProbability >= 0.0)
        assertTrue(policyAnalysis.maxProbability <= 1.0)
        assertTrue(policyAnalysis.concentration >= 0.0)
        assertTrue(policyAnalysis.concentration <= 1.0)
        assertNotNull(policyAnalysis.distributionShape)
    }
    
    @Test
    fun testValueEstimationAnalysis() {
        val board = ChessBoard()
        val output = analyzer.analyzeNetworkOutput(board)
        val valueAnalysis = output.valueAnalysis
        
        assertTrue(valueAnalysis.estimatedValue >= -10.0) // Reasonable bounds
        assertTrue(valueAnalysis.estimatedValue <= 10.0)
        assertNotNull(valueAnalysis.expectedRange)
        assertNotNull(valueAnalysis.gamePhase)
        assertTrue(valueAnalysis.confidence >= 0.0)
        assertTrue(valueAnalysis.confidence <= 1.0)
    }
    
    @Test
    fun testQValueDistributionAnalysis() {
        val board = ChessBoard()
        val output = analyzer.analyzeNetworkOutput(board)
        val qValueAnalysis = output.qValueAnalysis
        
        assertTrue(qValueAnalysis.qValueSpread >= 0.0)
        assertTrue(qValueAnalysis.maxQValue >= qValueAnalysis.minQValue)
        assertNotNull(qValueAnalysis.distributionShape)
    }
    
    @Test
    fun testActivationAnalysis() {
        val board = ChessBoard()
        val analysis = analyzer.analyzeActivations(
            board = board,
            includeLayerAnalysis = true,
            includeAttentionMaps = true
        )
        
        assertNotNull(analysis)
        assertEquals(board.toFEN(), analysis.position)
        assertTrue(analysis.layerActivations.isNotEmpty())
        assertTrue(analysis.attentionMaps.isNotEmpty())
        assertNotNull(analysis.activationPatterns)
        assertTrue(analysis.analysisTimestamp > 0)
    }
    
    @Test
    fun testLayerActivationAnalysis() {
        val board = ChessBoard()
        val analysis = analyzer.analyzeActivations(board, includeLayerAnalysis = true)
        
        analysis.layerActivations.forEach { (layerName, layerAnalysis) ->
            assertNotNull(layerName)
            assertNotNull(layerAnalysis.layerName)
            assertNotNull(layerAnalysis.activationStats)
            assertTrue(layerAnalysis.sparsity >= 0.0)
            assertTrue(layerAnalysis.sparsity <= 1.0)
            assertTrue(layerAnalysis.saturation >= 0.0)
            assertTrue(layerAnalysis.saturation <= 1.0)
        }
    }
    
    @Test
    fun testNetworkIssueDetection() {
        val board = ChessBoard()
        val analysis = analyzer.analyzeActivations(board, includeLayerAnalysis = true)
        
        // Test that issues are properly categorized
        analysis.networkIssues.forEach { issue ->
            assertNotNull(issue.type)
            assertNotNull(issue.layer)
            assertTrue(issue.severity >= 0.0)
            assertTrue(issue.severity <= 1.0)
            assertTrue(issue.description.isNotEmpty())
        }
    }
    
    @Test
    fun testNetworkComparison() {
        val positions = listOf(
            ChessBoard(),
            ChessBoard().apply { 
                // Make a move to create different position
                makeLegalMove(Move(Position(1, 4), Position(3, 4)))
            }
        )
        
        val comparison = analyzer.compareNetworkOutputs(
            positions = positions,
            comparisonType = ComparisonType.POLICY_SIMILARITY
        )
        
        assertNotNull(comparison)
        assertEquals(positions.size, comparison.positions.size)
        assertEquals(positions.size, comparison.outputs.size)
        assertTrue(comparison.similarities.isNotEmpty())
        assertNotNull(comparison.commonPatterns)
        assertEquals(ComparisonType.POLICY_SIMILARITY, comparison.comparisonType)
    }
    
    @Test
    fun testGamePhaseAnalysis() {
        val gameHistory = listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4)), // e7-e5
            Move(Position(0, 6), Position(2, 5)), // Ng1-f3
            Move(Position(7, 1), Position(5, 2))  // Nb8-c6
        )
        
        val analysis = analyzer.analyzeGamePhaseNetworkBehavior(gameHistory, samplePositions = 3)
        
        assertNotNull(analysis)
        assertEquals(gameHistory.size, analysis.gameLength)
        assertTrue(analysis.phaseAnalyses.isNotEmpty())
        assertNotNull(analysis.phaseTransitions)
        assertNotNull(analysis.phasePatterns)
    }
    
    @Test
    fun testDecisionVisualization() {
        val board = ChessBoard()
        
        VisualizationType.values().forEach { visualizationType ->
            val visualization = analyzer.visualizeDecisionMaking(board, visualizationType)
            
            assertNotNull(visualization)
            assertEquals(board.toFEN(), visualization.position)
            assertEquals(visualizationType, visualization.visualizationType)
            assertTrue(visualization.visualization.isNotEmpty())
            assertNotNull(visualization.networkOutput)
        }
    }
    
    @Test
    fun testNetworkConfidenceAnalysis() {
        val board = ChessBoard()
        val confidenceAnalysis = analyzer.analyzeNetworkConfidence(board)
        
        assertNotNull(confidenceAnalysis)
        assertEquals(board.toFEN(), confidenceAnalysis.position)
        assertTrue(confidenceAnalysis.policyEntropy >= 0.0)
        assertTrue(confidenceAnalysis.valueConfidence >= 0.0)
        assertTrue(confidenceAnalysis.valueConfidence <= 1.0)
        assertTrue(confidenceAnalysis.qValueSpread >= 0.0)
        assertTrue(confidenceAnalysis.overallConfidence >= 0.0)
        assertTrue(confidenceAnalysis.overallConfidence <= 1.0)
        assertNotNull(confidenceAnalysis.confidenceLevel)
        assertNotNull(confidenceAnalysis.uncertaintySources)
    }
    
    @Test
    fun testUncertaintySourceIdentification() {
        val board = ChessBoard()
        val confidenceAnalysis = analyzer.analyzeNetworkConfidence(board)
        
        confidenceAnalysis.uncertaintySources.forEach { source ->
            assertNotNull(source.type)
            assertTrue(source.description.isNotEmpty())
            assertTrue(source.severity >= 0.0)
            assertTrue(source.severity <= 1.0)
        }
    }
    
    @Test
    fun testAnalysisConsistency() {
        val board = ChessBoard()
        
        // Run the same analysis multiple times
        val analyses = (1..5).map { analyzer.analyzeNetworkOutput(board) }
        
        // Check that results are consistent for the same position
        val firstAnalysis = analyses.first()
        analyses.forEach { analysis ->
            assertEquals(firstAnalysis.position, analysis.position)
            // Policy outputs should be identical for the same position
            assertTrue(firstAnalysis.policyOutput.contentEquals(analysis.policyOutput))
            assertEquals(firstAnalysis.valueOutput, analysis.valueOutput)
        }
    }
    
    @Test
    fun testConfigurationValidation() {
        val config = NetworkAnalysisConfig(
            enableLayerAnalysis = true,
            enableAttentionAnalysis = true,
            maxLayersToAnalyze = 5,
            activationThreshold = 1e-6,
            saturationThreshold = 0.95
        )
        
        val configuredAnalyzer = NeuralNetworkAnalyzer(mockAgent, config)
        assertNotNull(configuredAnalyzer)
        
        val board = ChessBoard()
        val analysis = configuredAnalyzer.analyzeNetworkOutput(board)
        assertNotNull(analysis)
    }
    
    @Test
    fun testErrorHandling() {
        // Test with empty board state
        val board = ChessBoard()
        
        // Should not throw exceptions
        assertDoesNotThrow {
            analyzer.analyzeNetworkOutput(board)
        }
        
        assertDoesNotThrow {
            analyzer.analyzeActivations(board)
        }
        
        assertDoesNotThrow {
            analyzer.analyzeNetworkConfidence(board)
        }
    }
    
    @Test
    fun testPerformanceMetrics() {
        val board = ChessBoard()
        
        // Measure analysis time
        val startTime = getCurrentTimeMillis()
        val analysis = analyzer.analyzeNetworkOutput(board)
        val endTime = getCurrentTimeMillis()
        
        val analysisTime = endTime - startTime
        
        // Analysis should complete in reasonable time (less than 1 second for tests)
        assertTrue(analysisTime < 1000, "Analysis took too long: ${analysisTime}ms")
        
        // Timestamp should be within the analysis period
        assertTrue(analysis.timestamp >= startTime)
        assertTrue(analysis.timestamp <= endTime)
    }
    
    @Test
    fun testMemoryUsage() {
        val board = ChessBoard()
        
        // Run multiple analyses to check for memory leaks
        repeat(100) {
            analyzer.analyzeNetworkOutput(board)
            analyzer.analyzeActivations(board)
            analyzer.analyzeNetworkConfidence(board)
        }
        
        // If we get here without OutOfMemoryError, memory usage is acceptable
        assertTrue(true)
    }
    
    @Test
    fun testGamePhaseDetection() {
        val gameHistory = createLongGameHistory()
        val analysis = analyzer.analyzeGamePhaseNetworkBehavior(gameHistory)
        
        // Should detect different game phases
        val phases = analysis.phaseAnalyses.map { it.gamePhase }.toSet()
        assertTrue(phases.contains(GamePhase.OPENING))
        
        if (gameHistory.size > 40) {
            assertTrue(phases.contains(GamePhase.MIDDLEGAME))
        }
        
        if (gameHistory.size > 80) {
            assertTrue(phases.contains(GamePhase.ENDGAME))
        }
    }
    
    @Test
    fun testAttentionMapAnalysis() {
        val board = ChessBoard()
        val analysis = analyzer.analyzeActivations(
            board = board,
            includeAttentionMaps = true
        )
        
        analysis.attentionMaps.forEach { (name, attentionMap) ->
            assertNotNull(name)
            assertTrue(name.isNotEmpty())
            assertNotNull(attentionMap.dimensions)
            assertTrue(attentionMap.dimensions.first > 0)
            assertTrue(attentionMap.dimensions.second > 0)
            assertNotNull(attentionMap.weights)
            assertNotNull(attentionMap.focusAreas)
        }
    }
    
    // Helper methods
    
    private fun createLongGameHistory(): List<Move> {
        // Create a longer game history to test phase detection
        return (1..100).map { i ->
            val fromRank = (i % 8)
            val fromFile = ((i / 8) % 8)
            val toRank = ((i + 1) % 8)
            val toFile = (((i + 1) / 8) % 8)
            
            Move(Position(fromRank, fromFile), Position(toRank, toFile))
        }
    }
}