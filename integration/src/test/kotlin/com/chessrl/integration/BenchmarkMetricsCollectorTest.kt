package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.LearningSession
import com.chessrl.integration.config.ChessRLConfig
// ReplayType is a String in ChessRLConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BenchmarkMetricsCollectorTest {
    
    @Test
    fun testCollectTrainingMetricsBasic() {
        val collector = BenchmarkMetricsCollector()
        val config = createTestConfig()
        val session = createMockLearningSession()
        
        val gameResults = listOf(
            GameResult(GameOutcome.WHITE_WINS, 45, 5.0),
            GameResult(GameOutcome.DRAW, 60, 0.0),
            GameResult(GameOutcome.BLACK_WINS, 30, -3.0)
        )
        
        val metrics = collector.collectTrainingMetrics(
            session = session,
            backendType = BackendType.MANUAL,
            config = config,
            cycleNumber = 5,
            gameResults = gameResults
        )
        
        // Verify basic metrics
        assertEquals(5, metrics.cycle)
        assertEquals("MANUAL", metrics.backendType)
        assertTrue(metrics.timestamp > 0)
        
        // Verify game statistics
        assertEquals(1.0/3.0, metrics.winRate, 0.001)
        assertEquals(1.0/3.0, metrics.drawRate, 0.001)
        assertEquals(1.0/3.0, metrics.lossRate, 0.001)
        assertEquals(3, metrics.totalGames)
        
        // Verify configuration snapshot
        assertEquals(0.001, metrics.learningRate)
        assertEquals(32, metrics.batchSize)
        assertEquals(0.99, metrics.gamma)
        assertEquals("512x256", metrics.hiddenLayers)
        assertEquals("ADAM", metrics.optimizer)
        assertEquals("UNIFORM", metrics.replayType)
    }
    
    @Test
    fun testCollectTrainingMetricsRL4J() {
        val collector = BenchmarkMetricsCollector()
        val config = createTestConfig()
        val session = createMockLearningSession()
        
        val metrics = collector.collectTrainingMetrics(
            session = session,
            backendType = BackendType.RL4J,
            config = config,
            cycleNumber = 10,
            gameResults = emptyList()
        )
        
        // Verify RL4J backend is recorded
        assertEquals("RL4J", metrics.backendType)
        assertEquals(10, metrics.cycle)
        
        // With no game results, rates should be 0
        assertEquals(0.0, metrics.winRate)
        assertEquals(0.0, metrics.drawRate)
        assertEquals(0.0, metrics.lossRate)
        assertEquals(0, metrics.totalGames)
    }
    
    @Test
    fun testCollectEvaluationMetrics() {
        val collector = BenchmarkMetricsCollector()
        
        val evaluationResults = listOf(
            GameResult(GameOutcome.WHITE_WINS, 40, 8.0),
            GameResult(GameOutcome.WHITE_WINS, 35, 12.0),
            GameResult(GameOutcome.DRAW, 80, 0.0),
            GameResult(GameOutcome.BLACK_WINS, 25, -5.0)
        )
        
        val metrics = collector.collectEvaluationMetrics(
            evaluationResults = evaluationResults,
            backendType = BackendType.DL4J,
            modelPath = "/path/to/model.zip"
        )
        
        // Verify evaluation metrics
        assertEquals("DL4J", metrics.backendType)
        assertEquals("/path/to/model.zip", metrics.modelPath)
        assertTrue(metrics.timestamp > 0)
        
        // Verify game statistics
        assertEquals(0.5, metrics.winRate) // 2/4
        assertEquals(0.25, metrics.drawRate) // 1/4
        assertEquals(0.25, metrics.lossRate) // 1/4
        assertEquals(4, metrics.totalGames)
        
        // Verify game quality metrics
        assertEquals(45.0, metrics.averageGameLength) // (40+35+80+25)/4
        assertEquals(3.75, metrics.averageMaterialAdvantage) // (8+12+0-5)/4
        assertEquals(0.75, metrics.decisiveGameRate) // 3/4 non-draws
    }
    
    @Test
    fun testMetricsCsvSerialization() {
        val metrics = BenchmarkTrainingMetrics(
            cycle = 1,
            backendType = "MANUAL",
            timestamp = 1234567890L,
            winRate = 0.6,
            drawRate = 0.2,
            lossRate = 0.2,
            totalGames = 10,
            averageLoss = 0.123456,
            gradientNorm = 1.234567,
            policyEntropy = 0.987654,
            qValueMean = 0.456789,
            illegalMoveCount = 5,
            trainingTimePerCycle = 30000L,
            peakMemoryUsage = 512L,
            currentMemoryUsage = 256L,
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.99,
            explorationRate = 0.1,
            hiddenLayers = "512x256",
            optimizer = "ADAM",
            replayType = "UNIFORM"
        )
        
        val csvRow = metrics.toCsvRow()
        val header = BenchmarkTrainingMetrics.csvHeader()
        
        // Verify CSV format
        assertNotNull(csvRow)
        assertNotNull(header)
        
        val headerFields = header.split(",")
        val dataFields = csvRow.split(",")
        
        // Should have same number of fields
        assertEquals(headerFields.size, dataFields.size)
        
        // Verify some key fields
        assertTrue(csvRow.contains("MANUAL"))
        assertTrue(csvRow.contains("0.6"))
        assertTrue(csvRow.contains("\"512x256\""))
    }
    
    @Test
    fun testEvaluationMetricsCsvSerialization() {
        val metrics = BenchmarkEvaluationMetrics(
            backendType = "RL4J",
            modelPath = "/test/model.zip",
            timestamp = 1234567890L,
            winRate = 0.7,
            drawRate = 0.1,
            lossRate = 0.2,
            totalGames = 100,
            averageGameLength = 45.5,
            averageMaterialAdvantage = 2.3,
            decisiveGameRate = 0.9
        )
        
        val csvRow = metrics.toCsvRow()
        val header = BenchmarkEvaluationMetrics.csvHeader()
        
        // Verify CSV format
        assertNotNull(csvRow)
        assertNotNull(header)
        
        val headerFields = header.split(",")
        val dataFields = csvRow.split(",")
        
        // Should have same number of fields
        assertEquals(headerFields.size, dataFields.size)
        
        // Verify some key fields
        assertTrue(csvRow.contains("RL4J"))
        assertTrue(csvRow.contains("\"/test/model.zip\""))
        assertTrue(csvRow.contains("0.7"))
    }
    
    @Test
    fun testEmptyGameResults() {
        val collector = BenchmarkMetricsCollector()
        val config = createTestConfig()
        val session = createMockLearningSession()
        
        val metrics = collector.collectTrainingMetrics(
            session = session,
            backendType = BackendType.MANUAL,
            config = config,
            cycleNumber = 1,
            gameResults = emptyList()
        )
        
        // With empty game results, all rates should be 0
        assertEquals(0.0, metrics.winRate)
        assertEquals(0.0, metrics.drawRate)
        assertEquals(0.0, metrics.lossRate)
        assertEquals(0, metrics.totalGames)
    }
    
    private fun createTestConfig(): ChessRLConfig {
        return ChessRLConfig(
            learningRate = 0.001,
            batchSize = 32,
            gamma = 0.99,
            explorationRate = 0.1,
            hiddenLayers = listOf(512, 256),
            optimizer = "ADAM",
            replayType = "UNIFORM",
            maxExperienceBuffer = 10000,
            targetUpdateFrequency = 100,
            doubleDqn = true
        )
    }
    
    private fun createMockLearningSession(): LearningSession {
        return object : LearningSession {
            override val config: ChessRLConfig = createTestConfig()
            override val mainAgent: ChessAgent = createMockAgent()
            override val opponentAgent: ChessAgent = createMockAgent()
            
            override fun trainOnBatch(experiences: List<com.chessrl.rl.Experience<DoubleArray, Int>>): com.chessrl.rl.PolicyUpdateResult {
                return com.chessrl.rl.PolicyUpdateResult(0.1, 1.0, 0.5, 0.3)
            }
            
            override fun updateOpponent() {}
            override fun saveCheckpoint(path: String) {}
            override fun saveBest(path: String) {}
            override fun close() {}
        }
    }
    
    private fun createMockAgent(): ChessAgent {
        return object : ChessAgent {
            override fun selectAction(state: DoubleArray, validActions: List<Int>): Int = validActions.firstOrNull() ?: 0
            override fun trainBatch(experiences: List<com.chessrl.rl.Experience<DoubleArray, Int>>): com.chessrl.rl.PolicyUpdateResult {
                return com.chessrl.rl.PolicyUpdateResult(0.1, 1.0, 0.5, 0.3)
            }
            override fun save(path: String) {}
            override fun load(path: String) {}
            override fun getConfig(): ChessAgentConfig = ChessAgentConfig(
                batchSize = 32,
                maxBufferSize = 10000,
                learningRate = 0.001,
                explorationRate = 0.1,
                targetUpdateFrequency = 100,
                gamma = 0.99
            )
            override fun learn(experience: com.chessrl.rl.Experience<DoubleArray, Int>) {}
            override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> = actions.associateWith { 0.5 }
            override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> = actions.associateWith { 1.0 / actions.size }
            override fun getTrainingMetrics(): ChessAgentMetrics = ChessAgentMetrics(0.0, 0.1, 1000)
            override fun forceUpdate() {}
            override fun reset() {}
            override fun setExplorationRate(rate: Double) {}
        }
    }
}