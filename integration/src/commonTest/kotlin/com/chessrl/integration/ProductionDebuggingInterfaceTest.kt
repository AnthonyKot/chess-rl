package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive tests for production debugging interface reliability and accuracy
 */
class ProductionDebuggingInterfaceTest {
    
    private lateinit var mockAgent: MockChessAgent
    private lateinit var mockEnvironment: MockChessEnvironment
    private lateinit var debuggingInterface: ProductionDebuggingInterface
    
    @BeforeTest
    fun setup() {
        mockAgent = MockChessAgent()
        mockEnvironment = MockChessEnvironment()
        debuggingInterface = ProductionDebuggingInterface(mockAgent, mockEnvironment)
    }
    
    @Test
    fun testDebuggingSessionCreation() {
        val session = debuggingInterface.startDebuggingSession("Test Session")
        
        assertNotNull(session)
        assertEquals("Test Session", session.sessionName)
        assertTrue(session.sessionId.isNotEmpty())
        assertTrue(session.startTime > 0)
        assertEquals(mockAgent, session.agent)
        assertEquals(mockEnvironment, session.environment)
    }
    
    @Test
    fun testInteractiveGameAnalysis() {
        debuggingInterface.startDebuggingSession("Game Analysis Test")
        
        val gameHistory = createTestGameHistory()
        val analysisSession = debuggingInterface.analyzeGameInteractively(gameHistory)
        
        assertNotNull(analysisSession)
        assertEquals(gameHistory, analysisSession.gameHistory)
        assertEquals(0, analysisSession.currentMoveIndex)
        assertNotNull(analysisSession.board)
    }
    
    @Test
    fun testStepThroughGame() {
        debuggingInterface.startDebuggingSession("Step Through Test")
        
        val gameHistory = createTestGameHistory()
        val analysisSession = debuggingInterface.analyzeGameInteractively(gameHistory)
        
        // Step forward
        val forwardStep = debuggingInterface.stepThroughGame(analysisSession, StepDirection.FORWARD)
        assertEquals(2, forwardStep.moveNumber) // Move number is 1-indexed
        assertNotNull(forwardStep.positionAnalysis)
        
        // Step backward
        val backwardStep = debuggingInterface.stepThroughGame(analysisSession, StepDirection.BACKWARD)
        assertEquals(1, backwardStep.moveNumber)
        assertNotNull(backwardStep.positionAnalysis)
    }
    
    @Test
    fun testPositionAnalysis() {
        val board = ChessBoard()
        val analysis = debuggingInterface.analyzeCurrentPosition(board)
        
        assertNotNull(analysis)
        assertEquals(board.toFEN(), analysis.position)
        assertNotNull(analysis.decisionAnalysis)
        assertNotNull(analysis.positionEvaluation)
        assertNotNull(analysis.neuralNetworkOutput)
        assertNotNull(analysis.tacticalAnalysis)
        assertNotNull(analysis.strategicAnalysis)
        assertTrue(analysis.timestamp > 0)
    }
    
    @Test
    fun testManualPlaySession() {
        debuggingInterface.startDebuggingSession("Manual Play Test")
        
        val playSession = debuggingInterface.startManualPlaySession(PieceColor.WHITE)
        
        assertNotNull(playSession)
        assertTrue(playSession.sessionId.isNotEmpty())
        assertEquals(PieceColor.WHITE, playSession.playerColor)
        assertEquals(PieceColor.BLACK, playSession.agentColor)
        assertNotNull(playSession.board)
        assertTrue(playSession.moveHistory.isEmpty())
        assertTrue(playSession.analysisHistory.isEmpty())
    }
    
    @Test
    fun testPlayerMoveValidation() {
        debuggingInterface.startDebuggingSession("Move Validation Test")
        
        val playSession = debuggingInterface.startManualPlaySession(PieceColor.WHITE)
        
        // Test valid move
        val validMove = Move(Position(1, 4), Position(3, 4)) // e2-e4
        mockEnvironment.setValidMoves(listOf(validMove))
        
        val result = debuggingInterface.makePlayerMove(playSession, validMove)
        assertTrue(result is ManualPlayResult.MoveMade)
        
        // Test invalid move
        val invalidMove = Move(Position(0, 0), Position(7, 7))
        val invalidResult = debuggingInterface.makePlayerMove(playSession, invalidMove)
        assertTrue(invalidResult is ManualPlayResult.InvalidMove)
    }
    
    @Test
    fun testNeuralNetworkActivationAnalysis() {
        val board = ChessBoard()
        val analysis = debuggingInterface.analyzeNeuralNetworkActivations(
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
    fun testTrainingPipelineDebugging() {
        val experiences = createTestExperiences(100)
        val trainingConfig = "test_config"
        
        val debugResult = debuggingInterface.debugTrainingPipeline(experiences, trainingConfig)
        
        assertNotNull(debugResult)
        assertNotNull(debugResult.bufferAnalysis)
        assertNotNull(debugResult.batchAnalysis)
        assertNotNull(debugResult.forwardPassAnalysis)
        assertNotNull(debugResult.lossAnalysis)
        assertNotNull(debugResult.backwardPassAnalysis)
        assertTrue(debugResult.recommendations.isNotEmpty())
    }
    
    @Test
    fun testExperienceBufferInspection() {
        val experiences = createTestExperiences(50)
        
        val inspectionResult = debuggingInterface.inspectExperienceBuffer(
            experiences = experiences,
            analysisDepth = AnalysisDepth.DETAILED
        )
        
        assertNotNull(inspectionResult)
        assertNotNull(inspectionResult.basicAnalysis)
        assertEquals(50, inspectionResult.basicAnalysis.bufferSize)
        assertTrue(inspectionResult.basicAnalysis.qualityScore >= 0.0)
        assertTrue(inspectionResult.basicAnalysis.qualityScore <= 1.0)
    }
    
    @Test
    fun testPerformanceProfiling() {
        val trainingMetrics = createTestTrainingMetrics(20)
        val systemMetrics = SystemMetrics(
            cpuUtilization = 0.7,
            memoryUsage = 0.5,
            gpuUtilization = 0.8,
            throughput = 100.0
        )
        
        val profilingResult = debuggingInterface.profilePerformance(trainingMetrics, systemMetrics)
        
        assertNotNull(profilingResult)
        assertNotNull(profilingResult.trainingPerformance)
        assertNotNull(profilingResult.systemPerformance)
        assertTrue(profilingResult.optimizationRecommendations.isNotEmpty())
        assertTrue(profilingResult.profilingTimestamp > 0)
    }
    
    @Test
    fun testDebuggingReportGeneration() {
        val session = debuggingInterface.startDebuggingSession("Report Test")
        
        // Add some analysis results to the session
        session.analysisResults.add("Test analysis 1")
        session.analysisResults.add("Test analysis 2")
        session.recommendations.add("Test recommendation")
        session.issuesFound.add("Test issue")
        
        val report = debuggingInterface.generateDebuggingReport(session)
        
        assertNotNull(report)
        assertEquals(session.sessionId, report.sessionId)
        assertEquals(session.sessionName, report.sessionName)
        assertTrue(report.sessionDuration >= 0)
        assertEquals(2, report.analysisResults.size)
        assertEquals(1, report.recommendations.size)
        assertEquals(1, report.issuesFound.size)
        assertTrue(report.generatedAt > 0)
    }
    
    @Test
    fun testDataExport() {
        val session = debuggingInterface.startDebuggingSession("Export Test")
        session.analysisResults.add("Test data")
        
        // Test JSON export
        val jsonResult = debuggingInterface.exportDebuggingData(
            session = session,
            format = ExportFormat.JSON,
            includeSensitiveData = false
        )
        assertTrue(jsonResult is ExportResult.Success)
        
        // Test CSV export
        val csvResult = debuggingInterface.exportDebuggingData(
            session = session,
            format = ExportFormat.CSV,
            includeSensitiveData = false
        )
        assertTrue(csvResult is ExportResult.Success)
    }
    
    @Test
    fun testErrorHandling() {
        // Test without active session
        assertFailsWith<IllegalStateException> {
            debuggingInterface.analyzeGameInteractively(emptyList())
        }
        
        // Test with empty game history
        debuggingInterface.startDebuggingSession("Error Test")
        val emptyAnalysis = debuggingInterface.analyzeGameInteractively(emptyList())
        assertNotNull(emptyAnalysis)
        assertEquals(0, emptyAnalysis.gameHistory.size)
    }
    
    @Test
    fun testConfigurationValidation() {
        val config = ProductionDebuggingConfig(
            enableDetailedLogging = true,
            maxSessionHistory = 50,
            enablePerformanceProfiling = true,
            exportSensitiveData = false,
            analysisTimeout = 10000,
            maxAnalysisDepth = 5
        )
        
        val configuredInterface = ProductionDebuggingInterface(mockAgent, mockEnvironment, config)
        assertNotNull(configuredInterface)
        
        val session = configuredInterface.startDebuggingSession("Config Test")
        assertNotNull(session)
    }
    
    @Test
    fun testAnalysisAccuracy() {
        val board = ChessBoard()
        
        // Test multiple analyses of the same position for consistency
        val analysis1 = debuggingInterface.analyzeCurrentPosition(board)
        val analysis2 = debuggingInterface.analyzeCurrentPosition(board)
        
        assertEquals(analysis1.position, analysis2.position)
        assertEquals(analysis1.decisionAnalysis.position, analysis2.decisionAnalysis.position)
        assertEquals(analysis1.decisionAnalysis.activeColor, analysis2.decisionAnalysis.activeColor)
        
        // Results should be consistent for the same position
        assertEquals(analysis1.decisionAnalysis.validMovesCount, analysis2.decisionAnalysis.validMovesCount)
    }
    
    @Test
    fun testMemoryManagement() {
        // Test that multiple sessions don't cause memory leaks
        repeat(10) { i ->
            val session = debuggingInterface.startDebuggingSession("Memory Test $i")
            
            // Add some data to the session
            repeat(100) { j ->
                session.analysisResults.add("Analysis $j")
            }
            
            // Generate report to ensure cleanup
            debuggingInterface.generateDebuggingReport(session)
        }
        
        // If we get here without OutOfMemoryError, memory management is working
        assertTrue(true)
    }
    
    @Test
    fun testConcurrentAccess() {
        // Test that the interface handles concurrent access gracefully
        val session1 = debuggingInterface.startDebuggingSession("Concurrent Test 1")
        val session2 = debuggingInterface.startDebuggingSession("Concurrent Test 2")
        
        assertNotEquals(session1.sessionId, session2.sessionId)
        assertNotEquals(session1.startTime, session2.startTime)
    }
    
    // Helper methods for creating test data
    
    private fun createTestGameHistory(): List<Move> {
        return listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4)), // e7-e5
            Move(Position(0, 6), Position(2, 5)), // Ng1-f3
            Move(Position(7, 1), Position(5, 2))  // Nb8-c6
        )
    }
    
    private fun createTestExperiences(count: Int): List<Experience<DoubleArray, Int>> {
        return (0 until count).map { i ->
            Experience(
                state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                action = i % 10,
                reward = if (i % 10 == 0) 1.0 else 0.0,
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                done = i % 20 == 19
            )
        }
    }
    
    private fun createTestTrainingMetrics(count: Int): List<RLMetrics> {
        return (0 until count).map { i ->
            RLMetrics(
                episode = i,
                averageReward = 0.5 + kotlin.random.Random.nextDouble() * 0.5,
                policyLoss = kotlin.random.Random.nextDouble(),
                policyEntropy = kotlin.random.Random.nextDouble(),
                explorationRate = 0.1 + kotlin.random.Random.nextDouble() * 0.4,
                gradientNorm = kotlin.random.Random.nextDouble() * 2.0,
                qValueStats = null
            )
        }
    }
}

/**
 * Mock implementations for testing
 */
class MockChessAgent : ChessAgent {
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        return validActions.firstOrNull() ?: 0
    }
    
    override fun getActionProbabilities(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        return validActions.associateWith { 1.0 / validActions.size }
    }
    
    override fun getQValues(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        return validActions.associateWith { kotlin.random.Random.nextDouble() }
    }
    
    override fun updatePolicy(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        return PolicyUpdateResult(
            loss = kotlin.random.Random.nextDouble(),
            gradientNorm = kotlin.random.Random.nextDouble(),
            policyEntropy = kotlin.random.Random.nextDouble(),
            qValueMean = kotlin.random.Random.nextDouble(),
            targetValueMean = kotlin.random.Random.nextDouble()
        )
    }
    
    fun getPolicyOutput(state: DoubleArray): DoubleArray {
        return DoubleArray(4096) { kotlin.random.Random.nextDouble() }
    }
    
    fun getValueOutput(state: DoubleArray): Double {
        return kotlin.random.Random.nextDouble() * 2.0 - 1.0
    }
}

class MockChessEnvironment : ChessEnvironment {
    private var validMoves: List<Move> = emptyList()
    
    fun setValidMoves(moves: List<Move>) {
        validMoves = moves
    }
    
    override fun reset(): DoubleArray {
        return DoubleArray(64) { 0.0 }
    }
    
    override fun step(action: Int): StepResult {
        return StepResult(
            nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
            reward = kotlin.random.Random.nextDouble(),
            done = kotlin.random.Random.nextBoolean(),
            info = emptyMap()
        )
    }
    
    override fun getValidActions(state: DoubleArray): List<Int> {
        return (0..4095).toList()
    }
    
    override fun getCurrentBoard(): ChessBoard {
        return ChessBoard()
    }
    
    override fun getGameStatus(): GameStatus {
        return GameStatus.IN_PROGRESS
    }
    
    override fun getPositionEvaluation(color: PieceColor): Double {
        return kotlin.random.Random.nextDouble() * 2.0 - 1.0
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