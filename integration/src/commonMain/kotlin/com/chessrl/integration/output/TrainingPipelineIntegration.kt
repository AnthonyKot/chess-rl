package com.chessrl.integration.output

import com.chessrl.integration.*
import com.chessrl.integration.config.ChessRLConfig
import kotlin.time.Duration.Companion.milliseconds

/**
 * Integration utilities for connecting OutputManager with TrainingPipeline.
 * Provides conversion functions and integration helpers.
 */
object TrainingPipelineIntegration {
    
    /**
     * Create OutputConfig from ChessRLConfig
     */
    fun createOutputConfig(
        config: ChessRLConfig,
        logLevel: LogLevel = LogLevel.INFO,
        outputMode: OutputMode = OutputMode.STANDARD,
        logInterval: Int = 1
    ): OutputConfig {
        return OutputConfig(
            logLevel = logLevel,
            outputMode = outputMode,
            logInterval = logInterval,
            summaryOnly = false,
            metricsExport = null, // Can be configured later via CLI flags
            trendWindowSize = 10,
            decimalPlaces = 2
        )
    }
    
    /**
     * Convert TrainingCycleResult to CycleProgressData
     */
    fun toCycleProgressData(
        result: TrainingCycleResult,
        totalCycles: Int,
        bufferSize: Int,
        maxBufferSize: Int,
        checkpointEvent: CheckpointEvent? = null
    ): CycleProgressData {
        // Calculate win/draw/loss from game results
        val outcomes = result.gameResults.groupingBy { it.gameOutcome }.eachCount()
        val wins = outcomes[GameOutcome.WHITE_WINS] ?: 0
        val draws = outcomes[GameOutcome.DRAW] ?: 0
        val losses = outcomes[GameOutcome.BLACK_WINS] ?: 0
        
        return CycleProgressData(
            cycleNumber = result.cycle,
            totalCycles = totalCycles,
            gamesPlayed = result.gamesPlayed,
            winDrawLoss = Triple(wins, draws, losses),
            averageGameLength = result.averageGameLength,
            averageReward = result.averageReward,
            batchesProcessed = result.trainingMetrics.batchCount,
            averageLoss = result.trainingMetrics.averageLoss,
            gradientNorm = result.trainingMetrics.averageGradientNorm,
            bufferUtilization = BufferStats.create(bufferSize, maxBufferSize),
            cycleDuration = result.cycleDuration.milliseconds,
            checkpointEvent = checkpointEvent
        )
    }
    
    /**
     * Convert TrainingResults to FinalSummaryData
     */
    fun toFinalSummaryData(results: TrainingResults): FinalSummaryData {
        val finalMetrics = results.finalMetrics
        
        return FinalSummaryData(
            totalCycles = results.totalCycles,
            totalDuration = results.totalDuration.milliseconds,
            bestPerformance = results.bestPerformance,
            totalGamesPlayed = results.totalGamesPlayed,
            totalExperiencesCollected = results.totalExperiencesCollected,
            finalAverageReward = finalMetrics.averageReward,
            finalWinRate = finalMetrics.averageWinRate,
            finalDrawRate = 1.0 - finalMetrics.averageWinRate // Simplified calculation
        )
    }
    
    /**
     * Create configuration summary data from ChessRLConfig
     */
    fun createConfigSummaryData(
        config: ChessRLConfig,
        profiles: List<String> = emptyList(),
        overrides: Map<String, Any> = emptyMap()
    ): Triple<List<String>, Map<String, Any>, Map<String, Any>> {
        val keyParameters = mapOf(
            "maxCycles" to config.maxCycles,
            "gamesPerCycle" to config.gamesPerCycle,
            "batchSize" to config.batchSize,
            "maxExperienceBuffer" to config.maxExperienceBuffer,
            "maxStepsPerGame" to config.maxStepsPerGame,
            "evaluationGames" to config.evaluationGames,
            "checkpointInterval" to config.checkpointInterval,
            "seed" to (config.seed?.toString() ?: "random")
        )
        
        return Triple(profiles, overrides, keyParameters)
    }
    
    /**
     * Create CheckpointEvent from checkpoint save operation
     */
    fun createCheckpointEvent(
        type: CheckpointType,
        path: String,
        currentPerformance: Double,
        previousBest: Double? = null,
        isBest: Boolean = false
    ): CheckpointEvent {
        val performanceDelta = if (previousBest != null && previousBest != Double.NEGATIVE_INFINITY) {
            currentPerformance - previousBest
        } else {
            null
        }
        
        return CheckpointEvent(
            type = type,
            path = path,
            performanceDelta = performanceDelta,
            isBest = isBest
        )
    }
}

/**
 * Extension functions for easier integration
 */

/**
 * Extension function to convert TrainingCycleResult to CycleProgressData
 */
fun TrainingCycleResult.toCycleProgressData(
    totalCycles: Int,
    bufferSize: Int,
    maxBufferSize: Int,
    checkpointEvent: CheckpointEvent? = null
): CycleProgressData {
    return TrainingPipelineIntegration.toCycleProgressData(
        this, totalCycles, bufferSize, maxBufferSize, checkpointEvent
    )
}

/**
 * Extension function to convert TrainingResults to FinalSummaryData
 */
fun TrainingResults.toFinalSummaryData(): FinalSummaryData {
    return TrainingPipelineIntegration.toFinalSummaryData(this)
}