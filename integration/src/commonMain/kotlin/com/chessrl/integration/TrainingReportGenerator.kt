package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Training report generator for comprehensive training logs and detailed progress reports.
 * 
 * Generates:
 * - Executive summaries with key achievements and recommendations
 * - Performance analysis with trend analysis and statistical insights
 * - Game quality analysis with strategic and tactical breakdowns
 * - Training efficiency analysis with bottleneck identification
 * - Issue analysis with resolution tracking and prevention recommendations
 * - Technical details with configuration and system metrics
 */
class TrainingReportGenerator(
    private val config: ReportGeneratorConfig
) {
    
    // Report generation state
    private val reportHistory = mutableListOf<ComprehensiveTrainingReport>()
    private val templateEngine = ReportTemplateEngine()
    
    /**
     * Generate executive summary
     */
    fun generateExecutiveSummary(
        trainingHistory: List<TrainingCycleMetrics>
    ): ExecutiveSummary {
        
        if (trainingHistory.isEmpty()) {
            return ExecutiveSummary(
                overallPerformance = "No training data available",
                keyAchievements = emptyList(),
                majorIssues = emptyList(),
                finalMetrics = emptyMap(),
                recommendationSummary = "Continue training to generate meaningful insights"
            )
        }
        
        val firstMetrics = trainingHistory.first()
        val lastMetrics = trainingHistory.last()
        
        // Overall performance assessment
        val performanceImprovement = lastMetrics.winRate - firstMetrics.winRate
        val overallPerformance = when {
            performanceImprovement > 0.3 -> "Excellent - Significant improvement achieved"
            performanceImprovement > 0.1 -> "Good - Steady improvement observed"
            performanceImprovement > 0.0 -> "Fair - Modest improvement shown"
            else -> "Poor - Performance declined or stagnated"
        }
        
        // Key achievements
        val keyAchievements = mutableListOf<String>()
        
        val bestWinRate = trainingHistory.maxByOrNull { it.winRate }?.winRate ?: 0.0
        if (bestWinRate > 0.8) {
            keyAchievements.add("Achieved ${formatPercentage(bestWinRate)} peak win rate")
        }
        
        val bestGameQuality = trainingHistory.maxByOrNull { it.averageGameQuality }?.averageGameQuality ?: 0.0
        if (bestGameQuality > 0.7) {
            keyAchievements.add("Reached ${formatDecimal(bestGameQuality, 2)} game quality score")
        }
        
        if (trainingHistory.size >= 50) {
            keyAchievements.add("Completed ${trainingHistory.size} training cycles")
        }
        
        val totalGames = trainingHistory.sumOf { it.gamesPlayed }
        if (totalGames >= 1000) {
            keyAchievements.add("Generated $totalGames training games")
        }
        
        // Major issues
        val majorIssues = mutableListOf<String>()
        
        val recentMetrics = trainingHistory.takeLast(10)
        val avgRecentWinRate = recentMetrics.map { it.winRate }.average()
        if (avgRecentWinRate < 0.3) {
            majorIssues.add("Low win rate (${formatPercentage(avgRecentWinRate)}) indicates training difficulties")
        }
        
        val avgRecentEntropy = recentMetrics.map { it.policyEntropy }.average()
        if (avgRecentEntropy < 0.2) {
            majorIssues.add("Low policy entropy (${formatDecimal(avgRecentEntropy, 3)}) suggests policy collapse")
        }
        
        val avgRecentLoss = recentMetrics.map { it.averageLoss }.average()
        if (avgRecentLoss > 2.0) {
            majorIssues.add("High training loss (${formatDecimal(avgRecentLoss, 3)}) indicates instability")
        }
        
        // Final metrics
        val finalMetrics = mapOf(
            "Final Win Rate" to lastMetrics.winRate,
            "Final Game Quality" to lastMetrics.averageGameQuality,
            "Final Training Efficiency" to lastMetrics.trainingEfficiency,
            "Total Cycles" to trainingHistory.size.toDouble(),
            "Total Games" to totalGames.toDouble(),
            "Performance Improvement" to performanceImprovement
        )
        
        // Recommendation summary
        val recommendationSummary = generateRecommendationSummary(
            performanceImprovement, avgRecentWinRate, majorIssues.size
        )
        
        return ExecutiveSummary(
            overallPerformance = overallPerformance,
            keyAchievements = keyAchievements,
            majorIssues = majorIssues,
            finalMetrics = finalMetrics,
            recommendationSummary = recommendationSummary
        )
    }
    
    /**
     * Generate performance analysis
     */
    fun generatePerformanceAnalysis(performanceHistory: List<PerformanceSnapshot>): PerformanceAnalysis {
        if (performanceHistory.isEmpty()) {
            return PerformanceAnalysis(
                performanceTrend = "No performance data available",
                bestPerformance = 0.0,
                averagePerformance = 0.0,
                performanceVariability = 0.0,
                convergenceAssessment = "Insufficient data",
                performanceBreakdown = emptyMap()
            )
        }
        
        val overallScores = performanceHistory.map { it.overallScore }
        val winRates = performanceHistory.map { it.winRate }
        val gameQualities = performanceHistory.map { it.gameQuality }
        
        // Performance trend analysis
        val performanceTrend = calculateTrend(overallScores)
        val trendDescription = when {
            performanceTrend > 0.01 -> "Strong upward trend (${formatDecimal(performanceTrend, 4)} per cycle)"
            performanceTrend > 0.001 -> "Gradual improvement (${formatDecimal(performanceTrend, 4)} per cycle)"
            performanceTrend > -0.001 -> "Stable performance with minimal change"
            performanceTrend > -0.01 -> "Slight decline (${formatDecimal(performanceTrend, 4)} per cycle)"
            else -> "Significant decline (${formatDecimal(performanceTrend, 4)} per cycle)"
        }
        
        // Performance statistics
        val bestPerformance = overallScores.maxOrNull() ?: 0.0
        val averagePerformance = overallScores.average()
        val performanceVariability = calculateVariance(overallScores)
        
        // Convergence assessment
        val recentStability = if (performanceHistory.size >= 10) {
            val recentScores = overallScores.takeLast(10)
            calculateStability(recentScores)
        } else 0.0
        
        val convergenceAssessment = when {
            recentStability > 0.9 -> "Converged - Performance is stable"
            recentStability > 0.7 -> "Near convergence - Performance stabilizing"
            recentStability > 0.5 -> "Moderate stability - Still improving"
            else -> "High variability - Training in progress"
        }
        
        // Performance breakdown
        val performanceBreakdown = mapOf(
            "Win Rate Contribution" to winRates.average() * 0.4,
            "Game Quality Contribution" to gameQualities.average() * 0.3,
            "Efficiency Contribution" to performanceHistory.map { it.trainingEfficiency }.average() * 0.3,
            "Trend Strength" to abs(performanceTrend) * 100,
            "Stability Score" to recentStability
        )
        
        return PerformanceAnalysis(
            performanceTrend = trendDescription,
            bestPerformance = bestPerformance,
            averagePerformance = averagePerformance,
            performanceVariability = performanceVariability,
            convergenceAssessment = convergenceAssessment,
            performanceBreakdown = performanceBreakdown
        )
    }
    
    /**
     * Generate game quality analysis
     */
    fun generateGameQualityAnalysis(gameHistory: List<AnalyzedGame>): GameQualityAnalysis {
        if (gameHistory.isEmpty()) {
            return GameQualityAnalysis(
                overallQuality = 0.0,
                qualityTrend = "No game data available",
                strategicImprovement = 0.0,
                tacticalImprovement = 0.0,
                gamePhaseAnalysis = emptyMap(),
                qualityDistribution = emptyMap()
            )
        }
        
        val qualityScores = gameHistory.map { it.qualityScore }
        val strategicComplexities = gameHistory.map { it.strategicComplexity }
        val tacticalAccuracies = gameHistory.map { it.tacticalAccuracy }
        
        // Overall quality metrics
        val overallQuality = qualityScores.average()
        
        // Quality trend analysis
        val qualityTrend = calculateTrend(qualityScores)
        val trendDescription = when {
            qualityTrend > 0.01 -> "Improving - Quality increasing by ${formatDecimal(qualityTrend, 4)} per game"
            qualityTrend > -0.01 -> "Stable - Quality remains consistent"
            else -> "Declining - Quality decreasing by ${formatDecimal(abs(qualityTrend), 4)} per game"
        }
        
        // Strategic and tactical improvement
        val strategicImprovement = if (gameHistory.size >= 10) {
            val firstHalf = strategicComplexities.take(gameHistory.size / 2).average()
            val secondHalf = strategicComplexities.drop(gameHistory.size / 2).average()
            secondHalf - firstHalf
        } else 0.0
        
        val tacticalImprovement = if (gameHistory.size >= 10) {
            val firstHalf = tacticalAccuracies.take(gameHistory.size / 2).average()
            val secondHalf = tacticalAccuracies.drop(gameHistory.size / 2).average()
            secondHalf - firstHalf
        } else 0.0
        
        // Game phase analysis
        val gamePhaseAnalysis = mapOf(
            "Opening Quality" to gameHistory.map { it.openingQuality }.average(),
            "Middlegame Quality" to gameHistory.map { it.middlegameQuality }.average(),
            "Endgame Quality" to gameHistory.map { it.endgameQuality }.average(),
            "Phase Consistency" to calculatePhaseConsistency(gameHistory)
        )
        
        // Quality distribution
        val qualityDistribution = mapOf(
            "Excellent (0.8+)" to gameHistory.count { it.qualityScore >= 0.8 },
            "Good (0.6-0.8)" to gameHistory.count { it.qualityScore >= 0.6 && it.qualityScore < 0.8 },
            "Fair (0.4-0.6)" to gameHistory.count { it.qualityScore >= 0.4 && it.qualityScore < 0.6 },
            "Poor (<0.4)" to gameHistory.count { it.qualityScore < 0.4 }
        )
        
        return GameQualityAnalysis(
            overallQuality = overallQuality,
            qualityTrend = trendDescription,
            strategicImprovement = strategicImprovement,
            tacticalImprovement = tacticalImprovement,
            gamePhaseAnalysis = gamePhaseAnalysis,
            qualityDistribution = qualityDistribution
        )
    }
    
    /**
     * Generate training efficiency analysis
     */
    fun generateEfficiencyAnalysis(trainingHistory: List<TrainingCycleMetrics>): TrainingEfficiencyAnalysis {
        if (trainingHistory.isEmpty()) {
            return TrainingEfficiencyAnalysis(
                overallEfficiency = 0.0,
                throughputMetrics = emptyMap(),
                resourceUtilization = emptyMap(),
                bottleneckAnalysis = emptyList(),
                optimizationOpportunities = emptyList()
            )
        }
        
        // Overall efficiency
        val efficiencyScores = trainingHistory.map { it.trainingEfficiency }
        val overallEfficiency = efficiencyScores.average()
        
        // Throughput metrics
        val totalTime = trainingHistory.size.toDouble() // Simplified time calculation
        val throughputMetrics = mapOf(
            "Games per Cycle" to trainingHistory.map { it.gamesPlayed }.average(),
            "Experiences per Cycle" to trainingHistory.map { it.experiencesCollected }.average(),
            "Batch Updates per Cycle" to trainingHistory.map { it.batchUpdates }.average(),
            "Average Cycle Time" to totalTime / trainingHistory.size,
            "Throughput Score" to trainingHistory.map { it.throughput }.average()
        )
        
        // Resource utilization
        val resourceUtilization = mapOf(
            "Average CPU Utilization" to trainingHistory.map { it.resourceUtilization }.average(),
            "Memory Efficiency" to 0.8, // Placeholder
            "Training Efficiency" to overallEfficiency,
            "Resource Optimization Score" to calculateResourceOptimization(trainingHistory)
        )
        
        // Bottleneck analysis
        val bottleneckAnalysis = identifyBottlenecks(trainingHistory)
        
        // Optimization opportunities
        val optimizationOpportunities = identifyOptimizationOpportunities(trainingHistory, bottleneckAnalysis)
        
        return TrainingEfficiencyAnalysis(
            overallEfficiency = overallEfficiency,
            throughputMetrics = throughputMetrics,
            resourceUtilization = resourceUtilization,
            bottleneckAnalysis = bottleneckAnalysis,
            optimizationOpportunities = optimizationOpportunities
        )
    }
    
    /**
     * Generate issue analysis
     */
    fun generateIssueAnalysis(issueHistory: List<DetectedIssue>): IssueAnalysis {
        if (issueHistory.isEmpty()) {
            return IssueAnalysis(
                totalIssues = 0,
                issuesByType = emptyMap(),
                criticalIssues = emptyList(),
                issueResolution = emptyMap(),
                preventionRecommendations = emptyList()
            )
        }
        
        val totalIssues = issueHistory.size
        
        // Issues by type
        val issuesByType = issueHistory.groupBy { it.type }.mapValues { it.value.size }
        
        // Critical issues
        val criticalIssues = issueHistory.filter { it.severity > 0.8 }.takeLast(10)
        
        // Issue resolution analysis
        val issueResolution = analyzeIssueResolution(issueHistory)
        
        // Prevention recommendations
        val preventionRecommendations = generatePreventionRecommendations(issuesByType)
        
        return IssueAnalysis(
            totalIssues = totalIssues,
            issuesByType = issuesByType,
            criticalIssues = criticalIssues,
            issueResolution = issueResolution,
            preventionRecommendations = preventionRecommendations
        )
    }
    
    /**
     * Generate convergence analysis
     */
    fun generateConvergenceAnalysis(trainingHistory: List<TrainingCycleMetrics>): ConvergenceAnalysis {
        if (trainingHistory.size < 10) {
            return ConvergenceAnalysis(
                status = com.chessrl.rl.ConvergenceStatus.INSUFFICIENT_DATA,
                confidence = 0.0,
                trendDirection = TrendDirection.UNKNOWN,
                stabilityScore = 0.0,
                rewardTrend = 0.0,
                lossTrend = 0.0,
                rewardStability = 0.0,
                lossStability = 0.0,
                recommendations = emptyList()
            )
        }

        val recentMetrics = trainingHistory.takeLast(20)
        val winRates = recentMetrics.map { it.winRate }
        val rewards = recentMetrics.map { it.averageReward }

        // Calculate trends and stability
        val winRateTrend = calculateTrend(winRates)
        val rewardTrend = calculateTrend(rewards)
        val winRateStability = calculateStability(winRates)
        val rewardStability = calculateStability(rewards)

        val overallStability = (winRateStability + rewardStability) / 2.0

        // Determine convergence status
        val status = when {
            overallStability > 0.9 && abs(winRateTrend) < 0.01 -> com.chessrl.rl.ConvergenceStatus.CONVERGED
            winRateTrend > 0.01 || rewardTrend > 0.01 -> com.chessrl.rl.ConvergenceStatus.IMPROVING
            else -> com.chessrl.rl.ConvergenceStatus.STAGNANT
        }

        val trendDirection = when {
            winRateTrend > 0.01 || rewardTrend > 0.01 -> TrendDirection.IMPROVING
            winRateTrend < -0.01 || rewardTrend < -0.01 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        return ConvergenceAnalysis(
            status = status,
            confidence = overallStability,
            trendDirection = trendDirection,
            stabilityScore = overallStability,
            rewardTrend = rewardTrend,
            lossTrend = 0.0,
            rewardStability = rewardStability,
            lossStability = 0.0,
            recommendations = emptyList()
        )
    }
    
    /**
     * Generate recommendations
     */
    fun generateRecommendations(
        trainingHistory: List<TrainingCycleMetrics>,
        issueHistory: List<DetectedIssue>
    ): List<TrainingRecommendation> {
        val recommendations = mutableListOf<TrainingRecommendation>()
        
        if (trainingHistory.isEmpty()) {
            return recommendations
        }
        
        val recentMetrics = trainingHistory.takeLast(10)
        val avgWinRate = recentMetrics.map { it.winRate }.average()
        val avgGameQuality = recentMetrics.map { it.averageGameQuality }.average()
        val avgEntropy = recentMetrics.map { it.policyEntropy }.average()
        
        // Performance recommendations
        if (avgWinRate < 0.4) {
            recommendations.add(TrainingRecommendation(
                type = RecommendationType.PERFORMANCE,
                priority = RecommendationPriority.HIGH,
                title = "Improve Win Rate",
                description = "Current win rate (${formatPercentage(avgWinRate)}) is below target",
                actions = listOf(
                    "Increase exploration rate",
                    "Adjust reward function",
                    "Review opponent strength"
                ),
                expectedImpact = "Should improve win rate by 10-20%",
                confidence = 0.8
            ))
        }
        
        // Quality recommendations
        if (avgGameQuality < 0.5) {
            recommendations.add(TrainingRecommendation(
                type = RecommendationType.QUALITY,
                priority = RecommendationPriority.MEDIUM,
                title = "Enhance Game Quality",
                description = "Game quality (${formatDecimal(avgGameQuality, 2)}) needs improvement",
                actions = listOf(
                    "Improve state representation",
                    "Add position-based rewards",
                    "Increase network capacity"
                ),
                expectedImpact = "Should improve game quality by 15-25%",
                confidence = 0.7
            ))
        }
        
        // Exploration recommendations
        if (avgEntropy < 0.3) {
            recommendations.add(TrainingRecommendation(
                type = RecommendationType.EXPLORATION,
                priority = RecommendationPriority.HIGH,
                title = "Increase Exploration",
                description = "Policy entropy (${formatDecimal(avgEntropy, 3)}) indicates insufficient exploration",
                actions = listOf(
                    "Increase exploration rate to 0.2",
                    "Add entropy regularization",
                    "Use different exploration strategy"
                ),
                expectedImpact = "Should prevent policy collapse",
                confidence = 0.9
            ))
        }
        
        // Issue-based recommendations
        val recentIssues = issueHistory.takeLast(10)
        val criticalIssues = recentIssues.filter { it.severity > 0.8 }
        
        if (criticalIssues.isNotEmpty()) {
            recommendations.add(TrainingRecommendation(
                type = RecommendationType.STABILITY,
                priority = RecommendationPriority.CRITICAL,
                title = "Address Critical Issues",
                description = "${criticalIssues.size} critical issues detected",
                actions = criticalIssues.flatMap { it.suggestedActions }.distinct(),
                expectedImpact = "Should stabilize training",
                confidence = 0.85
            ))
        }
        
        return recommendations.sortedByDescending { it.priority.ordinal }
    }
    
    /**
     * Generate technical details
     */
    fun generateTechnicalDetails(trainingHistory: List<TrainingCycleMetrics>): TechnicalDetails {
        if (trainingHistory.isEmpty()) {
            return TechnicalDetails(
                configurationUsed = emptyMap(),
                systemMetrics = emptyMap(),
                dataQuality = emptyMap(),
                algorithmPerformance = emptyMap(),
                resourceConsumption = emptyMap()
            )
        }
        
        // Configuration used (simplified)
        val configurationUsed = mapOf<String, Any>(
            "Training Cycles" to trainingHistory.size,
            "Batch Size" to "64", // Placeholder
            "Learning Rate" to "0.001", // Placeholder
            "Exploration Strategy" to "Epsilon-Greedy" // Placeholder
        )
        
        // System metrics
        val systemMetrics = mapOf(
            "Average Cycle Time" to trainingHistory.size.toDouble(),
            "Total Training Time" to trainingHistory.size * 60.0, // Placeholder
            "Memory Usage" to 512.0, // MB, placeholder
            "CPU Utilization" to trainingHistory.map { it.resourceUtilization }.average()
        )
        
        // Data quality
        val dataQuality = mapOf(
            "Total Games Generated" to trainingHistory.sumOf { it.gamesPlayed }.toDouble(),
            "Total Experiences" to trainingHistory.sumOf { it.experiencesCollected }.toDouble(),
            "Average Game Length" to trainingHistory.map { it.averageGameLength }.average(),
            "Data Completeness" to 0.95 // Placeholder
        )
        
        // Algorithm performance
        val algorithmPerformance = mutableMapOf<String, Double>().apply {
            this["Final Win Rate"] = trainingHistory.last().winRate
            this["Best Win Rate"] = trainingHistory.maxByOrNull { it.winRate }?.winRate ?: 0.0
            this["Average Loss"] = trainingHistory.map { it.averageLoss }.average()
            this["Policy Entropy"] = trainingHistory.map { it.policyEntropy }.average()
            this["Gradient Norm"] = trainingHistory.map { it.gradientNorm }.average()
        }.toMap()
        
        // Resource consumption
        val resourceConsumption = mapOf(
            "Total Compute Hours" to trainingHistory.size / 60.0, // Placeholder
            "Average Memory Usage" to 400.0, // MB, placeholder
            "Peak Memory Usage" to 600.0, // MB, placeholder
            "Efficiency Score" to trainingHistory.map { it.trainingEfficiency }.average()
        )
        
        return TechnicalDetails(
            configurationUsed = configurationUsed,
            systemMetrics = systemMetrics,
            dataQuality = dataQuality,
            algorithmPerformance = algorithmPerformance,
            resourceConsumption = resourceConsumption
        )
    }
    
    /**
     * Generate performance report
     */
    fun generatePerformanceReport(performanceHistory: List<PerformanceSnapshot>): Any {
        return generatePerformanceAnalysis(performanceHistory)
    }
    
    /**
     * Generate game quality report
     */
    fun generateGameQualityReport(gameHistory: List<AnalyzedGame>): Any {
        return generateGameQualityAnalysis(gameHistory)
    }
    
    /**
     * Generate issue report
     */
    fun generateIssueReport(issueHistory: List<DetectedIssue>): Any {
        return generateIssueAnalysis(issueHistory)
    }
    
    // Private helper methods
    
    private fun generateRecommendationSummary(
        performanceImprovement: Double,
        avgRecentWinRate: Double,
        majorIssuesCount: Int
    ): String {
        return when {
            performanceImprovement > 0.2 && majorIssuesCount == 0 -> 
                "Excellent progress. Continue current approach with minor optimizations."
            performanceImprovement > 0.1 && majorIssuesCount <= 1 -> 
                "Good progress. Address minor issues and maintain training consistency."
            avgRecentWinRate < 0.3 || majorIssuesCount > 2 -> 
                "Significant issues detected. Immediate intervention required to stabilize training."
            else -> 
                "Moderate progress. Consider adjusting hyperparameters and monitoring closely."
        }
    }
    
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
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateStability(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = calculateVariance(values)
        val stdDev = sqrt(variance)
        
        return if (abs(mean) > 1e-8) {
            1.0 / (1.0 + stdDev / abs(mean))
        } else {
            if (stdDev < 1e-8) 1.0 else 0.0
        }
    }
    
    private fun calculatePhaseConsistency(gameHistory: List<AnalyzedGame>): Double {
        if (gameHistory.isEmpty()) return 0.0
        
        val phaseVariances = listOf(
            calculateVariance(gameHistory.map { it.openingQuality }),
            calculateVariance(gameHistory.map { it.middlegameQuality }),
            calculateVariance(gameHistory.map { it.endgameQuality })
        )
        
        val avgVariance = phaseVariances.average()
        return 1.0 - avgVariance // Lower variance = higher consistency
    }
    
    private fun calculateResourceOptimization(trainingHistory: List<TrainingCycleMetrics>): Double {
        val efficiencyScores = trainingHistory.map { it.trainingEfficiency }
        val resourceScores = trainingHistory.map { it.resourceUtilization }
        
        return (efficiencyScores.average() + resourceScores.average()) / 2.0
    }
    
    private fun identifyBottlenecks(trainingHistory: List<TrainingCycleMetrics>): List<String> {
        val bottlenecks = mutableListOf<String>()
        
        val avgThroughput = trainingHistory.map { it.throughput }.average()
        if (avgThroughput < 1.0) {
            bottlenecks.add("Low throughput (${formatDecimal(avgThroughput, 2)} games/sec) - consider parallel processing")
        }
        
        val avgResourceUtil = trainingHistory.map { it.resourceUtilization }.average()
        if (avgResourceUtil < 0.5) {
            bottlenecks.add("Low resource utilization (${formatPercentage(avgResourceUtil)}) - optimize resource usage")
        }
        
        val avgBatchUpdates = trainingHistory.map { it.batchUpdates }.average()
        if (avgBatchUpdates < 5) {
            bottlenecks.add("Few batch updates (${formatDecimal(avgBatchUpdates, 1)}) - increase training frequency")
        }
        
        return bottlenecks
    }
    
    private fun identifyOptimizationOpportunities(
        trainingHistory: List<TrainingCycleMetrics>,
        bottlenecks: List<String>
    ): List<String> {
        val opportunities = mutableListOf<String>()
        
        if (bottlenecks.any { it.contains("throughput") }) {
            opportunities.add("Implement parallel game generation")
            opportunities.add("Optimize batch processing pipeline")
        }
        
        if (bottlenecks.any { it.contains("resource") }) {
            opportunities.add("Increase batch sizes for better GPU utilization")
            opportunities.add("Implement memory pooling for efficiency")
        }
        
        val avgGameLength = trainingHistory.map { it.averageGameLength }.average()
        if (avgGameLength > 100) {
            opportunities.add("Implement early game termination for efficiency")
        }
        
        if (opportunities.isEmpty()) {
            opportunities.add("Current configuration appears well-optimized")
        }
        
        return opportunities
    }
    
    private fun analyzeIssueResolution(issueHistory: List<DetectedIssue>): Map<String, String> {
        val issueGroups = issueHistory.groupBy { it.type }
        
        val evaluated: Map<IssueType, String> = issueGroups.mapValues { (_, issues) ->
            val recentIssues = issues.takeLast(5)
            val avgSeverity = recentIssues.map { it.severity }.average()
            
            when {
                avgSeverity < 0.3 -> "Resolved - Issue severity decreased significantly"
                avgSeverity < 0.6 -> "Improving - Issue severity is decreasing"
                else -> "Persistent - Issue requires immediate attention"
            }
        }

        return evaluated.entries.associate { it.key.name to it.value }
    }
    
    private fun generatePreventionRecommendations(issuesByType: Map<IssueType, Int>): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (issuesByType.getOrDefault(IssueType.EXPLODING_GRADIENTS, 0) > 0) {
            recommendations.add("Implement gradient clipping by default")
            recommendations.add("Use learning rate scheduling to prevent gradient explosion")
        }
        
        if (issuesByType.getOrDefault(IssueType.POLICY_COLLAPSE, 0) > 0) {
            recommendations.add("Maintain minimum exploration rate throughout training")
            recommendations.add("Add entropy regularization to prevent policy collapse")
        }
        
        if (issuesByType.getOrDefault(IssueType.EXPLORATION_INSUFFICIENT, 0) > 0) {
            recommendations.add("Use adaptive exploration strategies")
            recommendations.add("Implement curiosity-driven exploration")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Continue monitoring for potential issues")
        }
        
        return recommendations
    }
    
    private fun formatPercentage(value: Double): String {
        return "${(value * 100).toInt()}%"
    }
    
    private fun formatDecimal(value: Double, places: Int): String {
        return "%.${places}f".format(value)
    }
}

/**
 * Report template engine for formatting reports
 */
class ReportTemplateEngine {
    // Implementation for report formatting and templating
}
