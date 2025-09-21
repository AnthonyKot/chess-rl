package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.rl.*
import kotlin.math.*
import com.chessrl.integration.TrendAnalysis

/**
 * Consolidated metrics collector that combines functionality from:
 * - AdvancedMetricsCollector.kt (comprehensive metrics collection)
 * - RealTimeMonitor.kt (real-time monitoring and display)
 * 
 * Focuses on essential metrics: episode length, buffer utilization, Q-value stats
 * while providing clear progress reporting and trend analysis.
 */
class MetricsCollector(
    private val config: ChessRLConfig
) {
    
    // Metrics history for trend analysis
    private val metricsHistory = mutableListOf<CycleMetrics>()
    private var startTime = 0L
    private var totalGamesPlayed = 0
    private var totalExperiencesCollected = 0
    
    /**
     * Initialize the metrics collector.
     */
    fun initialize() {
        startTime = System.currentTimeMillis()
        metricsHistory.clear()
        totalGamesPlayed = 0
        totalExperiencesCollected = 0
        println("📊 Metrics Collector initialized")
    }
    
    /**
     * Collect metrics for a training cycle.
     */
    fun collectCycleMetrics(
        cycle: Int,
        gameResults: List<SelfPlayGameResult>,
        trainingMetrics: TrainingMetrics,
        experienceManager: ExperienceManager
    ): CycleMetrics {
        
        val timestamp = System.currentTimeMillis()
        
        // Episode length metrics
        val episodeLengths = gameResults.map { it.gameLength }
        val avgEpisodeLength = if (episodeLengths.isNotEmpty()) episodeLengths.average() else 0.0
        val minEpisodeLength = episodeLengths.minOrNull() ?: 0
        val maxEpisodeLength = episodeLengths.maxOrNull() ?: 0
        
        // Buffer utilization - use actual buffer statistics for accuracy
        val bufferStats = experienceManager.getStatistics()
        val experienceBufferSize = bufferStats.bufferSize
        val bufferUtilization = bufferStats.utilization
        
        // Game outcome analysis
        val outcomes = gameResults.groupingBy { it.gameOutcome }.eachCount()
        val total = gameResults.size
        val winRate = if (total > 0) (outcomes[GameOutcome.WHITE_WINS] ?: 0).toDouble() / total else 0.0
        val drawRate = if (total > 0) (outcomes[GameOutcome.DRAW] ?: 0).toDouble() / total else 0.0
        val lossRate = if (total > 0) (outcomes[GameOutcome.BLACK_WINS] ?: 0).toDouble() / total else 0.0
        
        // Training efficiency metrics
        val elapsedTime = timestamp - startTime
        val gamesPerSecond = if (elapsedTime > 0) totalGamesPlayed.toDouble() / (elapsedTime / 1000.0) else 0.0
        
        // Q-value statistics - calculate from actual experience rewards
        val allRewards = gameResults.flatMap { game -> game.experiences.map { it.reward } }
        val qValueStats = if (allRewards.isNotEmpty()) {
            val meanReward = allRewards.average()
            val minReward = allRewards.minOrNull() ?: 0.0
            val maxReward = allRewards.maxOrNull() ?: 0.0
            val variance = if (allRewards.size > 1) {
                val mean = meanReward
                allRewards.map { (it - mean) * (it - mean) }.average()
            } else 0.0
            
            QValueStats(
                meanQValue = meanReward,
                minQValue = minReward,
                maxQValue = maxReward,
                qValueVariance = variance
            )
        } else {
            QValueStats(
                meanQValue = 0.0,
                minQValue = 0.0,
                maxQValue = 0.0,
                qValueVariance = 0.0
            )
        }
        
        // Termination analysis
        val terminationAnalysis = analyzeTerminations(gameResults)
        
        // Update totals
        totalGamesPlayed += gameResults.size
        totalExperiencesCollected += gameResults.sumOf { it.experiences.size }
        
        val metrics = CycleMetrics(
            cycle = cycle,
            timestamp = timestamp,
            
            // Episode metrics
            gamesPlayed = gameResults.size,
            avgEpisodeLength = avgEpisodeLength,
            minEpisodeLength = minEpisodeLength,
            maxEpisodeLength = maxEpisodeLength,
            
            // Buffer metrics
            experienceBufferSize = experienceBufferSize,
            bufferUtilization = bufferUtilization,
            
            // Performance metrics
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageReward = trainingMetrics.averageReward,
            
            // Training metrics
            averageLoss = trainingMetrics.averageLoss,
            averageGradientNorm = trainingMetrics.averageGradientNorm,
            averagePolicyEntropy = trainingMetrics.averagePolicyEntropy,
            batchCount = trainingMetrics.batchCount,
            
            // Q-value stats
            qValueStats = qValueStats,
            
            // Efficiency metrics
            gamesPerSecond = gamesPerSecond,
            totalGamesPlayed = totalGamesPlayed,
            totalExperiencesCollected = totalExperiencesCollected,
            
            // Termination analysis
            terminationAnalysis = terminationAnalysis
        )
        
        // Store metrics
        metricsHistory.add(metrics)
        
        // Limit history size
        if (metricsHistory.size > 200) {
            metricsHistory.removeAt(0)
        }
        
        return metrics
    }
    
    /**
     * Analyze game termination reasons.
     */
    private fun analyzeTerminations(gameResults: List<SelfPlayGameResult>): TerminationAnalysis {
        if (gameResults.isEmpty()) {
            return TerminationAnalysis(0.0, 0.0, 0.0)
        }
        
        val total = gameResults.size
        val naturalTerminations = gameResults.count { it.terminationReason == EpisodeTerminationReason.GAME_ENDED }
        val stepLimitTerminations = gameResults.count { it.terminationReason == EpisodeTerminationReason.STEP_LIMIT }
        val manualTerminations = gameResults.count { it.terminationReason == EpisodeTerminationReason.MANUAL }
        
        return TerminationAnalysis(
            naturalTerminationRate = naturalTerminations.toDouble() / total,
            stepLimitTerminationRate = stepLimitTerminations.toDouble() / total,
            manualTerminationRate = manualTerminations.toDouble() / total
        )
    }
    
    /**
     * Report progress with essential metrics.
     */
    fun reportProgress(cycle: Int) {
        if (metricsHistory.isEmpty()) return
        
        val currentMetrics = metricsHistory.last()
        val trends = calculateTrends()
        val recentHistory = metricsHistory.takeLast(10)
        val episodeTrend = calculateTrendSafe(recentHistory) { it.avgEpisodeLength }
        val bufferTrend = calculateTrendSafe(recentHistory) { it.bufferUtilization }
        val lossTrend = calculateTrendSafe(recentHistory) { it.averageLoss }

        println("\n📊 Training Progress - Cycle $cycle")
        println("-".repeat(50))
        
        // Episode metrics
        println("🎮 Episode Metrics:")
        println("   Games Played: ${currentMetrics.gamesPlayed}")
        println("   Avg Episode Length: ${"%.1f".format(currentMetrics.avgEpisodeLength)} moves ${formatTrend(episodeTrend)}")
        println("   Episode Range: ${currentMetrics.minEpisodeLength}-${currentMetrics.maxEpisodeLength} moves")
        
        // Buffer utilization
        println("\n🧠 Buffer Utilization:")
        println("   Buffer Size: ${currentMetrics.experienceBufferSize}/${config.maxExperienceBuffer}")
        println("   Utilization: ${"%.1f".format(currentMetrics.bufferUtilization * 100)}% ${formatTrend(bufferTrend)}")
        
        // Performance metrics
        println("\n🎯 Performance:")
        println("   Win Rate: ${"%.1f".format(currentMetrics.winRate * 100)}% ${formatTrend(trends.winRateTrend)}")
        println("   Draw Rate: ${"%.1f".format(currentMetrics.drawRate * 100)}%")
        println("   Average Reward: ${"%.3f".format(currentMetrics.averageReward)} ${formatTrend(trends.rewardTrend)}")
        
        // Q-value statistics
        println("\n📈 Q-Value Stats:")
        println("   Mean Q-Value: ${"%.2f".format(currentMetrics.qValueStats.meanQValue)}")
        println("   Q-Value Range: ${"%.2f".format(currentMetrics.qValueStats.minQValue)} to ${"%.2f".format(currentMetrics.qValueStats.maxQValue)}")
        println("   Q-Value Variance: ${"%.3f".format(currentMetrics.qValueStats.qValueVariance)}")
        
        // Training metrics
        println("\n🔬 Training:")
        println("   Batch Updates: ${currentMetrics.batchCount}")
        println("   Average Loss: ${"%.4f".format(currentMetrics.averageLoss)} ${formatTrend(lossTrend)}")
        println("   Gradient Norm: ${"%.4f".format(currentMetrics.averageGradientNorm)}")
        println("   Policy Entropy: ${"%.3f".format(currentMetrics.averagePolicyEntropy)}")
        
        // Efficiency metrics
        println("\n⚡ Efficiency:")
        println("   Games/Second: ${"%.2f".format(currentMetrics.gamesPerSecond)}")
        println("   Total Games: ${currentMetrics.totalGamesPlayed}")
        println("   Total Experiences: ${currentMetrics.totalExperiencesCollected}")
        
        // Termination analysis
        println("\n🏁 Termination Analysis:")
        println("   Natural: ${"%.1f".format(currentMetrics.terminationAnalysis.naturalTerminationRate * 100)}%")
        println("   Step Limit: ${"%.1f".format(currentMetrics.terminationAnalysis.stepLimitTerminationRate * 100)}%")
        if (currentMetrics.terminationAnalysis.manualTerminationRate > 0) {
            println("   Manual: ${"%.1f".format(currentMetrics.terminationAnalysis.manualTerminationRate * 100)}%")
        }
        
        println("-".repeat(50))
    }
    
    /**
     * Calculate trends for key metrics.
     */
    private fun calculateTrends(): TrendAnalysis {
        if (metricsHistory.size < 5) {
            return TrendAnalysis(0.0, 0.0, 0.0, 0.0)
        }

        val recent = metricsHistory.takeLast(10)

        val rewardTrend = calculateTrend(recent.map { it.averageReward })
        val winRateTrend = calculateTrend(recent.map { it.winRate })
        val qualityTrend = calculateTrend(recent.map { it.averagePolicyEntropy })
        val efficiencyTrend = calculateTrend(recent.map { it.gamesPerSecond })

        return TrendAnalysis(
            rewardTrend = rewardTrend,
            winRateTrend = winRateTrend,
            gameQualityTrend = qualityTrend,
            efficiencyTrend = efficiencyTrend
        )
    }

    private fun calculateTrendSafe(
        recent: List<CycleMetrics>,
        selector: (CycleMetrics) -> Double
    ): Double {
        if (recent.size < 2) return 0.0
        return calculateTrend(recent.map(selector))
    }
    
    /**
     * Calculate trend using linear regression.
     */
    private fun calculateTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val x = (0 until n).map { it.toDouble() }
        val y = values
        
        val xMean = x.average()
        val yMean = y.average()
        
        val numerator = x.zip(y) { xi, yi -> (xi - xMean) * (yi - yMean) }.sum()
        val denominator = x.map { (it - xMean).pow(2) }.sum()
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    /**
     * Format trend indicator.
     */
    private fun formatTrend(trend: Double): String {
        return when {
            trend > 0.01 -> "📈"
            trend < -0.01 -> "📉"
            else -> "➡️"
        }
    }
    
    /**
     * Get training summary.
     */
    fun getTrainingSummary(): TrainingSummary {
        if (metricsHistory.isEmpty()) {
            return TrainingSummary(
                totalCycles = 0,
                totalGamesPlayed = 0,
                totalExperiencesCollected = 0,
                averageEpisodeLength = 0.0,
                averageBufferUtilization = 0.0,
                averageWinRate = 0.0,
                bestWinRate = 0.0,
                averageReward = 0.0,
                bestReward = 0.0,
                trainingEfficiency = 0.0
            )
        }
        
        val totalCycles = metricsHistory.size
        val avgEpisodeLength = metricsHistory.map { it.avgEpisodeLength }.average()
        val avgBufferUtilization = metricsHistory.map { it.bufferUtilization }.average()
        val avgWinRate = metricsHistory.map { it.winRate }.average()
        val bestWinRate = metricsHistory.map { it.winRate }.maxOrNull() ?: 0.0
        val avgReward = metricsHistory.map { it.averageReward }.average()
        val bestReward = metricsHistory.map { it.averageReward }.maxOrNull() ?: 0.0
        
        // Calculate training efficiency (improvement per cycle)
        val firstMetrics = metricsHistory.first()
        val lastMetrics = metricsHistory.last()
        val performanceImprovement = lastMetrics.winRate - firstMetrics.winRate
        val trainingEfficiency = if (totalCycles > 1) performanceImprovement / totalCycles else 0.0
        
        return TrainingSummary(
            totalCycles = totalCycles,
            totalGamesPlayed = totalGamesPlayed,
            totalExperiencesCollected = totalExperiencesCollected,
            averageEpisodeLength = avgEpisodeLength,
            averageBufferUtilization = avgBufferUtilization,
            averageWinRate = avgWinRate,
            bestWinRate = bestWinRate,
            averageReward = avgReward,
            bestReward = bestReward,
            trainingEfficiency = trainingEfficiency
        )
    }
    
    /**
     * Get metrics history for analysis.
     */
    fun getMetricsHistory(): List<CycleMetrics> = metricsHistory.toList()
    
    /**
     * Clear metrics history.
     */
    fun clearHistory() {
        metricsHistory.clear()
        totalGamesPlayed = 0
        totalExperiencesCollected = 0
    }
    
    /**
     * Finalize metrics collection.
     */
    fun finalize() {
        val totalTime = System.currentTimeMillis() - startTime
        println("📊 Metrics collection completed. Total time: ${formatDuration(totalTime)}")
        
        val summary = getTrainingSummary()
        println("\n📈 Final Training Summary:")
        println("   Total Cycles: ${summary.totalCycles}")
        println("   Total Games: ${summary.totalGamesPlayed}")
        println("   Total Experiences: ${summary.totalExperiencesCollected}")
        println("   Average Episode Length: ${"%.1f".format(summary.averageEpisodeLength)} moves")
        println("   Average Buffer Utilization: ${"%.1f".format(summary.averageBufferUtilization * 100)}%")
        println("   Average Win Rate: ${"%.1f".format(summary.averageWinRate * 100)}%")
        println("   Best Win Rate: ${"%.1f".format(summary.bestWinRate * 100)}%")
        println("   Training Efficiency: ${"%.4f".format(summary.trainingEfficiency)} win rate improvement per cycle")
    }
    
    /**
     * Validate that metrics contain actual data, not placeholder values.
     */
    fun validateMetrics(metrics: CycleMetrics): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for suspicious placeholder values
        if (metrics.avgEpisodeLength == 0.0 && metrics.gamesPlayed > 0) {
            issues.add("Average episode length is 0.0 despite games being played")
        }
        
        if (metrics.bufferUtilization < 0.0 || metrics.bufferUtilization > 1.0) {
            issues.add("Buffer utilization is out of valid range [0.0, 1.0]: ${metrics.bufferUtilization}")
        }
        
        if (metrics.winRate + metrics.drawRate + metrics.lossRate > 1.01) {
            issues.add("Win/draw/loss rates sum to more than 1.0: ${metrics.winRate + metrics.drawRate + metrics.lossRate}")
        }
        
        if (metrics.gamesPerSecond < 0.0) {
            issues.add("Games per second is negative: ${metrics.gamesPerSecond}")
        }
        
        return issues
    }
    
    /**
     * Format duration in human-readable format.
     */
    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}

/**
 * Data classes for metrics
 */

data class CycleMetrics(
    val cycle: Int,
    val timestamp: Long,
    
    // Episode metrics
    val gamesPlayed: Int,
    val avgEpisodeLength: Double,
    val minEpisodeLength: Int,
    val maxEpisodeLength: Int,
    
    // Buffer metrics
    val experienceBufferSize: Int,
    val bufferUtilization: Double,
    
    // Performance metrics
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val averageReward: Double,
    
    // Training metrics
    val averageLoss: Double,
    val averageGradientNorm: Double,
    val averagePolicyEntropy: Double,
    val batchCount: Int,
    
    // Q-value stats
    val qValueStats: QValueStats,
    
    // Efficiency metrics
    val gamesPerSecond: Double,
    val totalGamesPlayed: Int,
    val totalExperiencesCollected: Int,
    
    // Termination analysis
    val terminationAnalysis: TerminationAnalysis
)

data class QValueStats(
    val meanQValue: Double,
    val minQValue: Double,
    val maxQValue: Double,
    val qValueVariance: Double
)

data class TerminationAnalysis(
    val naturalTerminationRate: Double,
    val stepLimitTerminationRate: Double,
    val manualTerminationRate: Double
)

data class TrainingSummary(
    val totalCycles: Int,
    val totalGamesPlayed: Int,
    val totalExperiencesCollected: Int,
    val averageEpisodeLength: Double,
    val averageBufferUtilization: Double,
    val averageWinRate: Double,
    val bestWinRate: Double,
    val averageReward: Double,
    val bestReward: Double,
    val trainingEfficiency: Double
)
