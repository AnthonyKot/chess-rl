package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.LearningBackend
import com.chessrl.integration.backend.LearningSession
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.integration.output.TrainingMetrics

/**
 * Collects benchmark metrics from training sessions for performance comparison.
 * 
 * This collector ensures identical metrics collection for both manual and RL4J backends,
 * enabling fair performance comparisons between implementations.
 */
class BenchmarkMetricsCollector {
    
    companion object {
        private val logger = ChessRLLogger.forComponent("BenchmarkMetricsCollector")
    }
    
    /**
     * Collect comprehensive training metrics from a learning session.
     * 
     * @param session The learning session to collect metrics from
     * @param backendType The backend type being used
     * @param config The configuration used for training
     * @param cycleNumber The current training cycle number
     * @param gameResults Recent game results for win/draw/loss calculation
     * @return Collected benchmark metrics
     */
    fun collectTrainingMetrics(
        session: LearningSession,
        backendType: BackendType,
        config: ChessRLConfig,
        cycleNumber: Int,
        gameResults: List<GameResult> = emptyList()
    ): BenchmarkTrainingMetrics {
        logger.debug("Collecting training metrics for ${backendType.name} backend, cycle $cycleNumber")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Collect basic training metrics
            val trainingMetrics = session.mainAgent.getTrainingMetrics()
            
            // Calculate win/draw/loss rates from recent games
            val gameStats = calculateGameStatistics(gameResults)
            
            // Get memory usage
            val memoryUsage = getMemoryUsage()
            
            // Collect illegal move count if available
            val illegalMoveCount = getIllegalMoveCount(session)
            
            // Calculate training time per cycle
            val trainingTimePerCycle = calculateTrainingTimePerCycle(session)
            
            val metrics = BenchmarkTrainingMetrics(
                cycle = cycleNumber,
                backendType = backendType.name,
                timestamp = System.currentTimeMillis(),
                
                // Game performance metrics
                winRate = gameStats.winRate,
                drawRate = gameStats.drawRate,
                lossRate = gameStats.lossRate,
                totalGames = gameStats.totalGames,
                
                // Training performance metrics
                averageLoss = trainingMetrics.averageLoss,
                gradientNorm = 0.0, // Not available in ChessAgentMetrics
                policyEntropy = trainingMetrics.policyEntropy,
                qValueMean = calculateQValueMean(session),
                
                // Operational metrics
                illegalMoveCount = illegalMoveCount,
                trainingTimePerCycle = trainingTimePerCycle,
                peakMemoryUsage = memoryUsage.peakMemoryMB,
                currentMemoryUsage = memoryUsage.currentMemoryMB,
                
                // Configuration snapshot
                learningRate = config.learningRate,
                batchSize = config.batchSize,
                gamma = config.gamma,
                explorationRate = config.explorationRate,
                
                // Additional metadata
                hiddenLayers = config.hiddenLayers.joinToString("x"),
                optimizer = config.optimizer,
                replayType = config.replayType
            )
            
            val collectionTime = System.currentTimeMillis() - startTime
            logger.debug("Metrics collection completed in ${collectionTime}ms for ${backendType.name}")
            
            return metrics
            
        } catch (e: Exception) {
            logger.error("Error collecting training metrics for ${backendType.name}: ${e.message}")
            
            // Return minimal metrics on error
            return BenchmarkTrainingMetrics(
                cycle = cycleNumber,
                backendType = backendType.name,
                timestamp = System.currentTimeMillis(),
                winRate = 0.0,
                drawRate = 0.0,
                lossRate = 0.0,
                totalGames = 0,
                averageLoss = 0.0,
                gradientNorm = 0.0,
                policyEntropy = 0.0,
                qValueMean = 0.0,
                illegalMoveCount = 0,
                trainingTimePerCycle = 0L,
                peakMemoryUsage = 0L,
                currentMemoryUsage = 0L,
                learningRate = config.learningRate,
                batchSize = config.batchSize,
                gamma = config.gamma,
                explorationRate = config.explorationRate,
                hiddenLayers = config.hiddenLayers.joinToString("x"),
                optimizer = config.optimizer,
                replayType = config.replayType,
                error = e.message
            )
        }
    }
    
    /**
     * Collect evaluation metrics from baseline evaluation.
     * 
     * @param evaluationResults Results from baseline evaluation
     * @param backendType The backend type that was evaluated
     * @param modelPath Path to the model that was evaluated
     * @return Collected evaluation metrics
     */
    fun collectEvaluationMetrics(
        evaluationResults: List<GameResult>,
        backendType: BackendType,
        modelPath: String
    ): BenchmarkEvaluationMetrics {
        logger.debug("Collecting evaluation metrics for ${backendType.name} backend")
        
        val gameStats = calculateGameStatistics(evaluationResults)
        
        return BenchmarkEvaluationMetrics(
            backendType = backendType.name,
            modelPath = modelPath,
            timestamp = System.currentTimeMillis(),
            
            // Game performance metrics
            winRate = gameStats.winRate,
            drawRate = gameStats.drawRate,
            lossRate = gameStats.lossRate,
            totalGames = gameStats.totalGames,
            
            // Game quality metrics
            averageGameLength = evaluationResults.map { it.moveCount }.average(),
            averageMaterialAdvantage = calculateAverageMaterialAdvantage(evaluationResults),
            decisiveGameRate = calculateDecisiveGameRate(evaluationResults)
        )
    }
    
    /**
     * Calculate game statistics from a list of game results.
     * 
     * Note: This assumes we're collecting metrics from the perspective of the main agent.
     * WHITE_WINS and BLACK_WINS are treated as wins/losses based on which side the agent played.
     * For simplicity, we'll treat WHITE_WINS as wins and BLACK_WINS as losses.
     */
    private fun calculateGameStatistics(gameResults: List<GameResult>): GameStatistics {
        if (gameResults.isEmpty()) {
            return GameStatistics(0.0, 0.0, 0.0, 0)
        }
        
        val wins = gameResults.count { it.outcome == GameOutcome.WHITE_WINS }
        val draws = gameResults.count { it.outcome == GameOutcome.DRAW }
        val losses = gameResults.count { it.outcome == GameOutcome.BLACK_WINS }
        val total = gameResults.size
        
        return GameStatistics(
            winRate = wins.toDouble() / total,
            drawRate = draws.toDouble() / total,
            lossRate = losses.toDouble() / total,
            totalGames = total
        )
    }
    
    /**
     * Get current memory usage statistics.
     */
    private fun getMemoryUsage(): MemoryUsage {
        return try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val maxMemory = runtime.maxMemory()
            
            MemoryUsage(
                currentMemoryMB = usedMemory / (1024 * 1024),
                peakMemoryMB = (totalMemory - freeMemory) / (1024 * 1024),
                maxMemoryMB = maxMemory / (1024 * 1024)
            )
        } catch (e: Exception) {
            logger.warn("Could not get memory usage: ${e.message}")
            MemoryUsage(0L, 0L, 0L)
        }
    }
    
    /**
     * Get illegal move count from the learning session.
     */
    private fun getIllegalMoveCount(session: LearningSession): Int {
        return try {
            // For now, return 0 as we don't have a direct way to track this
            // This would need to be implemented in the training loop
            0
        } catch (e: Exception) {
            logger.debug("Could not get illegal move count: ${e.message}")
            0
        }
    }
    
    /**
     * Calculate training time per cycle.
     */
    private fun calculateTrainingTimePerCycle(session: LearningSession): Long {
        // This would need to be tracked by the training session
        // For now, return 0 as a placeholder
        return 0L
    }
    
    /**
     * Calculate mean Q-value from the learning session.
     */
    private fun calculateQValueMean(session: LearningSession): Double {
        return try {
            // Get Q-values from a sample state
            val sampleState = DoubleArray(839) { 0.0 } // Standard starting position
            val sampleActions = listOf(0, 1, 2, 3, 4) // Sample actions
            
            val qValues = session.mainAgent.getQValues(sampleState, sampleActions)
            qValues.values.average()
        } catch (e: Exception) {
            logger.debug("Could not calculate Q-value mean: ${e.message}")
            0.0
        }
    }
    
    /**
     * Calculate average material advantage from game results.
     */
    private fun calculateAverageMaterialAdvantage(gameResults: List<GameResult>): Double {
        return try {
            gameResults.mapNotNull { it.finalMaterialAdvantage }.average()
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Calculate the rate of decisive games (non-draws).
     */
    private fun calculateDecisiveGameRate(gameResults: List<GameResult>): Double {
        if (gameResults.isEmpty()) return 0.0
        
        val decisiveGames = gameResults.count { it.outcome != GameOutcome.DRAW && it.outcome != GameOutcome.ONGOING }
        return decisiveGames.toDouble() / gameResults.size
    }
}

/**
 * Comprehensive training metrics for benchmarking.
 */
data class BenchmarkTrainingMetrics(
    val cycle: Int,
    val backendType: String,
    val timestamp: Long,
    
    // Game performance metrics
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val totalGames: Int,
    
    // Training performance metrics
    val averageLoss: Double,
    val gradientNorm: Double,
    val policyEntropy: Double,
    val qValueMean: Double,
    
    // Operational metrics
    val illegalMoveCount: Int,
    val trainingTimePerCycle: Long, // milliseconds
    val peakMemoryUsage: Long, // MB
    val currentMemoryUsage: Long, // MB
    
    // Configuration snapshot
    val learningRate: Double,
    val batchSize: Int,
    val gamma: Double,
    val explorationRate: Double,
    val hiddenLayers: String,
    val optimizer: String,
    val replayType: String,
    
    // Error information
    val error: String? = null
) {
    /**
     * Convert to CSV row format.
     */
    fun toCsvRow(): String {
        return listOf(
            cycle.toString(),
            backendType,
            timestamp.toString(),
            winRate.toString(),
            drawRate.toString(),
            lossRate.toString(),
            totalGames.toString(),
            averageLoss.toString(),
            gradientNorm.toString(),
            policyEntropy.toString(),
            qValueMean.toString(),
            illegalMoveCount.toString(),
            trainingTimePerCycle.toString(),
            peakMemoryUsage.toString(),
            currentMemoryUsage.toString(),
            learningRate.toString(),
            batchSize.toString(),
            gamma.toString(),
            explorationRate.toString(),
            "\"$hiddenLayers\"",
            optimizer,
            replayType,
            error?.let { "\"$it\"" } ?: ""
        ).joinToString(",")
    }
    
    companion object {
        /**
         * Get CSV header for training metrics.
         */
        fun csvHeader(): String {
            return listOf(
                "cycle",
                "backendType",
                "timestamp",
                "winRate",
                "drawRate",
                "lossRate",
                "totalGames",
                "averageLoss",
                "gradientNorm",
                "policyEntropy",
                "qValueMean",
                "illegalMoveCount",
                "trainingTimePerCycle",
                "peakMemoryUsage",
                "currentMemoryUsage",
                "learningRate",
                "batchSize",
                "gamma",
                "explorationRate",
                "hiddenLayers",
                "optimizer",
                "replayType",
                "error"
            ).joinToString(",")
        }
    }
}

/**
 * Evaluation metrics for benchmarking.
 */
data class BenchmarkEvaluationMetrics(
    val backendType: String,
    val modelPath: String,
    val timestamp: Long,
    
    // Game performance metrics
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val totalGames: Int,
    
    // Game quality metrics
    val averageGameLength: Double,
    val averageMaterialAdvantage: Double,
    val decisiveGameRate: Double
) {
    /**
     * Convert to CSV row format.
     */
    fun toCsvRow(): String {
        return listOf(
            backendType,
            "\"$modelPath\"",
            timestamp.toString(),
            winRate.toString(),
            drawRate.toString(),
            lossRate.toString(),
            totalGames.toString(),
            averageGameLength.toString(),
            averageMaterialAdvantage.toString(),
            decisiveGameRate.toString()
        ).joinToString(",")
    }
    
    companion object {
        /**
         * Get CSV header for evaluation metrics.
         */
        fun csvHeader(): String {
            return listOf(
                "backendType",
                "modelPath",
                "timestamp",
                "winRate",
                "drawRate",
                "lossRate",
                "totalGames",
                "averageGameLength",
                "averageMaterialAdvantage",
                "decisiveGameRate"
            ).joinToString(",")
        }
    }
}

/**
 * Game statistics summary.
 */
private data class GameStatistics(
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val totalGames: Int
)

/**
 * Memory usage information.
 */
private data class MemoryUsage(
    val currentMemoryMB: Long,
    val peakMemoryMB: Long,
    val maxMemoryMB: Long
)

/**
 * Game result for metrics collection.
 */
data class GameResult(
    val outcome: GameOutcome,
    val moveCount: Int,
    val finalMaterialAdvantage: Double? = null
)