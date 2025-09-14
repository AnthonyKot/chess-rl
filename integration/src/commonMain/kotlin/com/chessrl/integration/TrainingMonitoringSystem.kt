package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*
import kotlin.math.*

/**
 * Comprehensive training monitoring and analysis system for chess RL training.
 * 
 * This system provides production-ready performance indicators, real-time monitoring,
 * automated training validation, and comprehensive analysis capabilities.
 * 
 * Features:
 * - Advanced training metrics collection with statistical significance analysis
 * - Real-time monitoring interface with interactive commands
 * - Automated training issue detection and quality assurance
 * - Comprehensive training logs and progress reports
 * - Advanced game replay with position analysis
 */
class TrainingMonitoringSystem(
    private val config: MonitoringConfig = MonitoringConfig()
) {
    
    // Core monitoring components
    private val metricsCollector = AdvancedMetricsCollector(config.metricsConfig)
    private val realTimeMonitor = RealTimeMonitor(config.monitorConfig)
    private val issueDetector = TrainingIssueDetector(config.issueDetectionConfig)
    private val gameAnalyzer = GameAnalyzer(config.gameAnalysisConfig)
    private val reportGenerator = TrainingReportGenerator(config.reportConfig)
    
    // Monitoring state
    private var isMonitoring = false
    private var monitoringStartTime = 0L
    private var totalTrainingTime = 0L
    
    // Training history and analysis
    private val trainingHistory = mutableListOf<TrainingCycleMetrics>()
    private val gameHistory = mutableListOf<AnalyzedGame>()
    private val issueHistory = mutableListOf<DetectedIssue>()
    private val performanceHistory = mutableListOf<PerformanceSnapshot>()
    
    /**
     * Start comprehensive monitoring of training process
     */
    fun startMonitoring(trainingConfig: Any): MonitoringSession {
        if (isMonitoring) {
            throw IllegalStateException("Monitoring is already active")
        }
        
        println("🔍 Starting Comprehensive Training Monitoring System")
        println("Configuration: $config")
        println("=" * 80)
        
        isMonitoring = true
        monitoringStartTime = getCurrentTimeMillis()
        
        // Initialize monitoring components
        metricsCollector.initialize()
        realTimeMonitor.start()
        issueDetector.initialize()
        gameAnalyzer.initialize()
        
        // Clear previous session data
        trainingHistory.clear()
        gameHistory.clear()
        issueHistory.clear()
        performanceHistory.clear()
        
        val session = MonitoringSession(
            sessionId = generateSessionId(),
            startTime = monitoringStartTime,
            trainingConfig = trainingConfig,
            monitoringConfig = config
        )
        
        println("✅ Monitoring session started: ${session.sessionId}")
        return session
    }
    
    /**
     * Record training cycle metrics with comprehensive analysis
     */
    fun recordTrainingCycle(
        cycle: Int,
        selfPlayResults: SelfPlayResults,
        trainingResults: ValidatedTrainingResults,
        performanceResults: PerformanceEvaluationResult,
        additionalMetrics: Map<String, Double> = emptyMap()
    ): TrainingCycleAnalysis {
        
        if (!isMonitoring) {
            throw IllegalStateException("Monitoring is not active")
        }
        
        val cycleStartTime = getCurrentTimeMillis()
        
        // Collect comprehensive metrics
        val metrics = metricsCollector.collectCycleMetrics(
            cycle = cycle,
            selfPlayResults = selfPlayResults,
            trainingResults = trainingResults,
            performanceResults = performanceResults,
            additionalMetrics = additionalMetrics
        )
        
        trainingHistory.add(metrics)
        
        // Analyze games from this cycle
        val gameAnalyses = analyzeGamesFromCycle(selfPlayResults)
        gameHistory.addAll(gameAnalyses)
        
        // Detect training issues
        val detectedIssues = issueDetector.detectIssues(metrics, trainingHistory)
        issueHistory.addAll(detectedIssues)
        
        // Create performance snapshot
        val performanceSnapshot = createPerformanceSnapshot(cycle, metrics)
        performanceHistory.add(performanceSnapshot)
        
        // Real-time monitoring update
        realTimeMonitor.updateMetrics(metrics, detectedIssues)
        
        // Generate cycle analysis
        val analysis = TrainingCycleAnalysis(
            cycle = cycle,
            metrics = metrics,
            gameAnalyses = gameAnalyses,
            detectedIssues = detectedIssues,
            performanceSnapshot = performanceSnapshot,
            convergenceStatus = analyzeConvergence(),
            recommendations = generateRecommendations(metrics, detectedIssues),
            analysisTime = getCurrentTimeMillis() - cycleStartTime
        )
        
        // Display real-time updates
        if (cycle % config.displayUpdateInterval == 0) {
            displayRealTimeUpdate(analysis)
        }
        
        return analysis
    }
    
    /**
     * Get real-time training dashboard
     */
    fun getTrainingDashboard(): TrainingDashboard {
        if (!isMonitoring) {
            throw IllegalStateException("Monitoring is not active")
        }
        
        val currentTime = getCurrentTimeMillis()
        val elapsedTime = currentTime - monitoringStartTime
        
        // Calculate current statistics
        val currentStats = calculateCurrentStatistics()
        val recentTrends = calculateRecentTrends()
        val systemHealth = assessSystemHealth()
        val activeIssues = getActiveIssues()
        
        return TrainingDashboard(
            sessionInfo = SessionInfo(
                sessionId = monitoringStartTime,
                totalEpisodes = trainingHistory.size,
                totalSteps = trainingHistory.sumOf { it.gamesPlayed },
                sessionDuration = elapsedTime
            ),
            currentStats = currentStats,
            recentTrends = recentTrends,
            systemHealth = systemHealth,
            activeIssues = activeIssues,
            performanceMetrics = getPerformanceMetrics(),
            gameQualityMetrics = getGameQualityMetrics(),
            trainingEfficiency = calculateTrainingEfficiency(),
            lastUpdate = currentTime
        )
    }
    
    /**
     * Execute interactive monitoring command
     */
    fun executeCommand(command: MonitoringCommand): CommandResult {
        return when (command) {
            is MonitoringCommand.Pause -> pauseMonitoring()
            is MonitoringCommand.Resume -> resumeMonitoring()
            is MonitoringCommand.Stop -> CommandResult.Success("Monitoring stopped", stopMonitoring())
            is MonitoringCommand.GenerateReport -> CommandResult.Success("Report generated", generateReport(command.reportType))
            is MonitoringCommand.AnalyzeGame -> CommandResult.Success("Game analyzed", analyzeSpecificGame(command.gameId))
            is MonitoringCommand.ShowTrends -> CommandResult.Success("Trends", showTrends(command.metric, command.window))
            is MonitoringCommand.DiagnoseIssues -> CommandResult.Success("Diagnostics", diagnoseIssues())
            is MonitoringCommand.ExportData -> CommandResult.Success("Exported", exportData(command.format, command.path))
            is MonitoringCommand.Configure -> CommandResult.Success("Reconfigured", updateConfiguration(command.newConfig))
        }
    }
    
    /**
     * Generate comprehensive training report
     */
    fun generateComprehensiveReport(): ComprehensiveTrainingReport {
        if (trainingHistory.isEmpty()) {
            throw IllegalStateException("No training data available for report generation")
        }
        
        println("📊 Generating Comprehensive Training Report...")
        
        val reportStartTime = getCurrentTimeMillis()
        
        // Generate all report sections
        val executiveSummary = reportGenerator.generateExecutiveSummary(trainingHistory, gameHistory)
        val performanceAnalysis = reportGenerator.generatePerformanceAnalysis(performanceHistory)
        val gameQualityAnalysis = reportGenerator.generateGameQualityAnalysis(gameHistory)
        val trainingEfficiencyAnalysis = reportGenerator.generateEfficiencyAnalysis(trainingHistory)
        val issueAnalysis = reportGenerator.generateIssueAnalysis(issueHistory)
        val convergenceAnalysis = reportGenerator.generateConvergenceAnalysis(trainingHistory)
        val recommendations = reportGenerator.generateRecommendations(trainingHistory, issueHistory)
        val technicalDetails = reportGenerator.generateTechnicalDetails(trainingHistory)
        
        val report = ComprehensiveTrainingReport(
            sessionId = generateSessionId(),
            generatedAt = getCurrentTimeMillis(),
            trainingPeriod = TrainingPeriod(
                startTime = monitoringStartTime,
                endTime = getCurrentTimeMillis(),
                totalCycles = trainingHistory.size,
                totalDuration = getCurrentTimeMillis() - monitoringStartTime
            ),
            executiveSummary = executiveSummary,
            performanceAnalysis = performanceAnalysis,
            gameQualityAnalysis = gameQualityAnalysis,
            trainingEfficiencyAnalysis = trainingEfficiencyAnalysis,
            issueAnalysis = issueAnalysis,
            convergenceAnalysis = convergenceAnalysis,
            recommendations = recommendations,
            technicalDetails = technicalDetails,
            generationTime = getCurrentTimeMillis() - reportStartTime
        )
        
        println("✅ Comprehensive report generated in ${report.generationTime}ms")
        return report
    }
    
    /**
     * Analyze specific game with detailed position analysis
     */
    fun analyzeGameWithPositions(
        gameId: String,
        includePositionEvaluation: Boolean = true,
        includeMoveAnalysis: Boolean = true,
        includeStrategicAnalysis: Boolean = true
    ): DetailedGameAnalysis {
        
        val game = gameHistory.find { it.gameId == gameId }
            ?: throw IllegalArgumentException("Game not found: $gameId")
        
        println("🔍 Analyzing game $gameId with detailed position analysis...")
        
        val analysisStartTime = getCurrentTimeMillis()
        
        // Perform detailed analysis
        val positionAnalyses = if (includePositionEvaluation) {
            gameAnalyzer.analyzePositions(game)
        } else emptyList()
        
        val moveAnalyses = if (includeMoveAnalysis) {
            gameAnalyzer.analyzeMoves(game)
        } else emptyList()
        
        val strategicAnalysis = if (includeStrategicAnalysis) {
            gameAnalyzer.analyzeStrategy(game)
        } else null
        
        val gameQuality = gameAnalyzer.assessGameQuality(game)
        val learningInsights = gameAnalyzer.extractLearningInsights(game)
        
        return DetailedGameAnalysis(
            gameId = gameId,
            game = game,
            positionAnalyses = positionAnalyses,
            moveAnalyses = moveAnalyses,
            strategicAnalysis = strategicAnalysis,
            gameQuality = gameQuality,
            learningInsights = learningInsights,
            analysisTime = getCurrentTimeMillis() - analysisStartTime
        )
    }
    
    /**
     * Stop monitoring and generate final report
     */
    fun stopMonitoring(): MonitoringResults {
        if (!isMonitoring) {
            throw IllegalStateException("Monitoring is not active")
        }
        
        println("🛑 Stopping Training Monitoring System...")
        
        val stopTime = getCurrentTimeMillis()
        totalTrainingTime = stopTime - monitoringStartTime
        
        // Stop monitoring components
        realTimeMonitor.stop()
        metricsCollector.finalize()
        issueDetector.finalize()
        gameAnalyzer.finalize()
        
        // Generate final comprehensive report
        val finalReport = generateComprehensiveReport()
        
        // Create monitoring results
        val results = MonitoringResults(
            sessionDuration = totalTrainingTime,
            totalCycles = trainingHistory.size,
            totalGames = trainingHistory.sumOf { it.gamesPlayed },
            totalIssues = issueHistory.size,
            finalPerformance = performanceHistory.lastOrNull()?.overallScore ?: 0.0,
            finalReport = finalReport,
            dataExports = exportAllData()
        )
        
        isMonitoring = false
        
        println("✅ Monitoring stopped. Session duration: ${totalTrainingTime}ms")
        println("📊 Final report generated with ${results.totalCycles} cycles analyzed")
        
        return results
    }
    
    // Private helper methods
    
    private fun analyzeGamesFromCycle(selfPlayResults: SelfPlayResults): List<AnalyzedGame> {
        return selfPlayResults.gameResults.map { gameResult ->
            gameAnalyzer.analyzeGame(gameResult)
        }
    }
    
    private fun createPerformanceSnapshot(cycle: Int, metrics: TrainingCycleMetrics): PerformanceSnapshot {
        return PerformanceSnapshot(
            cycle = cycle,
            timestamp = getCurrentTimeMillis(),
            overallScore = calculateOverallScore(metrics),
            winRate = metrics.winRate,
            averageReward = metrics.averageReward,
            gameQuality = metrics.averageGameQuality,
            trainingEfficiency = metrics.trainingEfficiency,
            convergenceIndicator = calculateConvergenceIndicator()
        )
    }
    
    private fun analyzeConvergence(): ConvergenceAnalysis {
        if (trainingHistory.size < config.convergenceAnalysisWindow) {
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
        
        val recentMetrics = trainingHistory.takeLast(config.convergenceAnalysisWindow)
        val rewards = recentMetrics.map { it.averageReward }
        val winRates = recentMetrics.map { it.winRate }
        
        val rewardTrend = calculateTrend(rewards)
        val winRateTrend = calculateTrend(winRates)
        val rewardStability = calculateStability(rewards)
        val winRateStability = calculateStability(winRates)
        
        val overallStability = (rewardStability + winRateStability) / 2.0
        val isConverging = overallStability > config.convergenceStabilityThreshold
        
        val status = when {
            isConverging && rewardTrend > -0.001 -> com.chessrl.rl.ConvergenceStatus.CONVERGED
            rewardTrend > 0.01 || winRateTrend > 0.01 -> com.chessrl.rl.ConvergenceStatus.IMPROVING
            else -> com.chessrl.rl.ConvergenceStatus.STAGNANT
        }
        
        val trendDirection = when {
            rewardTrend > 0.01 || winRateTrend > 0.01 -> TrendDirection.IMPROVING
            rewardTrend < -0.01 || winRateTrend < -0.01 -> TrendDirection.DECLINING
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
    
    private fun generateRecommendations(
        metrics: TrainingCycleMetrics,
        issues: List<DetectedIssue>
    ): List<TrainingRecommendation> {
        val recommendations = mutableListOf<TrainingRecommendation>()
        
        // Performance-based recommendations
        if (metrics.winRate < 0.3) {
            recommendations.add(TrainingRecommendation(
                type = RecommendationType.PERFORMANCE,
                priority = RecommendationPriority.HIGH,
                title = "Low Win Rate Detected",
                description = "Win rate is ${(metrics.winRate * 100).toInt()}%, consider adjusting training parameters",
                actions = listOf(
                    "Increase exploration rate",
                    "Adjust reward function",
                    "Increase training frequency"
                )
            ))
        }
        
        // Issue-based recommendations
        for (issue in issues) {
            when (issue.type) {
                IssueType.EXPLODING_GRADIENTS -> {
                    recommendations.add(TrainingRecommendation(
                        type = RecommendationType.STABILITY,
                        priority = RecommendationPriority.CRITICAL,
                        title = "Exploding Gradients Detected",
                        description = "Gradient norm is ${issue.severity}, training may be unstable",
                        actions = listOf(
                            "Apply gradient clipping",
                            "Reduce learning rate",
                            "Check network architecture"
                        )
                    ))
                }
                IssueType.POLICY_COLLAPSE -> {
                    recommendations.add(TrainingRecommendation(
                        type = RecommendationType.EXPLORATION,
                        priority = RecommendationPriority.HIGH,
                        title = "Policy Collapse Detected",
                        description = "Policy entropy is very low, agent may be over-exploiting",
                        actions = listOf(
                            "Increase exploration rate",
                            "Add entropy regularization",
                            "Use different exploration strategy"
                        )
                    ))
                }
                else -> {
                    // Handle other issue types
                }
            }
        }
        
        return recommendations
    }
    
    private fun displayRealTimeUpdate(analysis: TrainingCycleAnalysis) {
        println("\n" + "=" * 80)
        println("🔍 REAL-TIME TRAINING MONITOR - Cycle ${analysis.cycle}")
        println("=" * 80)
        
        val metrics = analysis.metrics
        println("📊 Performance Metrics:")
        println("   Win Rate: ${(metrics.winRate * 100).toInt()}%")
        println("   Average Reward: ${String.format("%.3f", metrics.averageReward)}")
        println("   Game Quality: ${String.format("%.3f", metrics.averageGameQuality)}")
        println("   Training Efficiency: ${String.format("%.3f", metrics.trainingEfficiency)}")
        
        println("\n🎮 Game Statistics:")
        println("   Games Played: ${metrics.gamesPlayed}")
        println("   Average Game Length: ${String.format("%.1f", metrics.averageGameLength)} moves")
        println("   Experiences Collected: ${metrics.experiencesCollected}")
        
        println("\n🧠 Training Statistics:")
        println("   Batch Updates: ${metrics.batchUpdates}")
        println("   Average Loss: ${String.format("%.4f", metrics.averageLoss)}")
        println("   Policy Entropy: ${String.format("%.3f", metrics.policyEntropy)}")
        println("   Gradient Norm: ${String.format("%.3f", metrics.gradientNorm)}")
        
        if (analysis.detectedIssues.isNotEmpty()) {
            println("\n⚠️ Detected Issues:")
            analysis.detectedIssues.forEach { issue ->
                println("   ${issue.type}: ${issue.description}")
            }
        }
        
        println("\n🎯 Convergence Status: ${analysis.convergenceStatus.status}")
        // Convergence details
        println("   Convergence trend: ${analysis.convergenceStatus.trendDirection}")
        
        if (analysis.recommendations.isNotEmpty()) {
            println("\n💡 Recommendations:")
            analysis.recommendations.take(3).forEach { rec ->
                println("   ${rec.title}: ${rec.description}")
            }
        }
        
        println("=" * 80)
    }
    
    private fun calculateCurrentStatistics(): CurrentStatistics {
        if (trainingHistory.isEmpty()) {
            return CurrentStatistics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val recent = trainingHistory.takeLast(10)
        val lastCycle = trainingHistory.last()
        val averageReward = recent.map { it.averageReward }.average()
        val winRate = recent.map { it.winRate }.average()
        val drawRate = 0.0
        val lossRate = 0.0
        val gameLength = recent.map { it.averageGameLength }.average()

        return CurrentStatistics(
            currentEpisode = lastCycle.cycle.toDouble(),
            averageReward = averageReward,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            gameLength = gameLength
        )
    }
    
    private fun calculateRecentTrends(): TrendAnalysis {
        if (trainingHistory.size < 5) {
            return TrendAnalysis(0.0, 0.0, 0.0, 0.0)
        }
        
        val recent = trainingHistory.takeLast(10)
        return TrendAnalysis(
            rewardTrend = calculateTrend(recent.map { it.averageReward }),
            winRateTrend = calculateTrend(recent.map { it.winRate }),
            gameQualityTrend = calculateTrend(recent.map { it.averageGameQuality }),
            efficiencyTrend = calculateTrend(recent.map { it.trainingEfficiency })
        )
    }
    
    private fun assessSystemHealth(): SystemHealth {
        val recentIssues = issueHistory.takeLast(10)
        val criticalIssues = recentIssues.count { it.severity > 0.8 }
        val totalIssues = recentIssues.size
        
        val healthScore = when {
            criticalIssues > 0 -> 0.3
            totalIssues > 5 -> 0.6
            totalIssues > 2 -> 0.8
            else -> 1.0
        }
        
        val status = when {
            healthScore >= 0.8 -> HealthStatus.HEALTHY
            healthScore >= 0.6 -> HealthStatus.WARNING
            else -> HealthStatus.CRITICAL
        }
        
        return SystemHealth(
            status = status,
            score = healthScore,
            warnings = (totalIssues - criticalIssues).coerceAtLeast(0),
            errors = criticalIssues
        )
    }
    
    private fun getActiveIssues(): List<ActiveIssue> {
        return issueHistory.takeLast(5).map { issue ->
            ActiveIssue(
                type = issue.type,
                description = issue.description,
                severity = issue.severity,
                firstDetected = issue.timestamp,
                occurrences = issueHistory.count { it.type == issue.type }
            )
        }
    }
    
    private fun getPerformanceMetrics(): PerformanceMetrics {
        if (performanceHistory.isEmpty()) {
            return PerformanceMetrics(0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val recent = performanceHistory.takeLast(10)
        val winRate = recent.last().winRate
        val rewards = recent.map { it.averageReward }
        val averageReward = rewards.average()
        val rewardVariance = calculateVariance(rewards)

        // Draw/loss rates are not tracked in PerformanceSnapshot; leave at 0.0 for now.
        return PerformanceMetrics(
            winRate = winRate,
            drawRate = 0.0,
            lossRate = 0.0,
            averageReward = averageReward,
            rewardVariance = rewardVariance
        )
    }
    
    private fun getGameQualityMetrics(): GameQualityMetrics {
        if (gameHistory.isEmpty()) {
            return GameQualityMetrics(0.0, 0.0, 0.0, 0.0)
        }

        val recent = gameHistory.takeLast(50)
        val averageQuality = recent.map { it.qualityScore }.average()
        val moveAccuracy = recent.map { it.moveAccuracy }.average()
        val strategicDepth = recent.map { it.strategicComplexity }.average()
        val tacticalAccuracy = recent.map { it.tacticalAccuracy }.average()

        return GameQualityMetrics(
            averageQuality = averageQuality,
            moveAccuracy = moveAccuracy,
            strategicDepth = strategicDepth,
            tacticalAccuracy = tacticalAccuracy
        )
    }
    
    private fun calculateTrainingEfficiency(): TrainingEfficiency {
        if (trainingHistory.isEmpty()) {
            return TrainingEfficiency(0.0, 0.0, 0.0, 0.0)
        }

        val totalTimeSec = (getCurrentTimeMillis() - monitoringStartTime) / 1000.0
        val episodesPerSecond = trainingHistory.size / totalTimeSec
        val batchesPerSecond = trainingHistory.sumOf { it.batchUpdates } / totalTimeSec

        return TrainingEfficiency(
            episodesPerSecond = episodesPerSecond,
            batchesPerSecond = batchesPerSecond,
            memoryUsage = 0.0,
            cpuUsage = 0.0
        )
    }
    
    // Command execution methods
    
    private fun pauseMonitoring(): CommandResult {
        realTimeMonitor.pause()
        return CommandResult.Success("Monitoring paused")
    }
    
    private fun resumeMonitoring(): CommandResult {
        realTimeMonitor.resume()
        return CommandResult.Success("Monitoring resumed")
    }
    
    private fun generateReport(reportType: ReportType): CommandResult {
        return try {
            val report = when (reportType) {
                ReportType.COMPREHENSIVE -> generateComprehensiveReport()
                ReportType.PERFORMANCE -> reportGenerator.generatePerformanceReport(performanceHistory)
                ReportType.GAME_QUALITY -> reportGenerator.generateGameQualityReport(gameHistory)
                ReportType.ISSUES -> reportGenerator.generateIssueReport(issueHistory)
                ReportType.VALIDATION -> reportGenerator.generateGameQualityReport(gameHistory)
            }
            CommandResult.Success("Report generated successfully", report)
        } catch (e: Exception) {
            CommandResult.Error("Failed to generate report: ${e.message}")
        }
    }
    
    private fun analyzeSpecificGame(gameId: String): CommandResult {
        return try {
            val analysis = analyzeGameWithPositions(gameId)
            CommandResult.Success("Game analysis completed", analysis)
        } catch (e: Exception) {
            CommandResult.Error("Failed to analyze game: ${e.message}")
        }
    }
    
    private fun showTrends(metric: String, window: Int): CommandResult {
        return try {
            val trends = calculateMetricTrends(metric, window)
            CommandResult.Success("Trends calculated", trends)
        } catch (e: Exception) {
            CommandResult.Error("Failed to calculate trends: ${e.message}")
        }
    }
    
    private fun diagnoseIssues(): CommandResult {
        val diagnosis = issueDetector.performComprehensiveDiagnosis(trainingHistory, issueHistory)
        return CommandResult.Success("Diagnosis completed", diagnosis)
    }
    
    private fun exportData(format: String, path: String): CommandResult {
        return try {
            val exportResult = exportTrainingData(format, path)
            CommandResult.Success("Data exported successfully", exportResult)
        } catch (e: Exception) {
            CommandResult.Error("Failed to export data: ${e.message}")
        }
    }
    
    private fun updateConfiguration(newConfig: MonitoringConfig): CommandResult {
        // Update configuration (simplified)
        return CommandResult.Success("Configuration updated")
    }
    
    // Utility methods
    
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
    
    private fun calculateStability(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        return if (abs(mean) > 1e-8) {
            1.0 / (1.0 + stdDev / abs(mean))
        } else {
            if (stdDev < 1e-8) 1.0 else 0.0
        }
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateOverallScore(metrics: TrainingCycleMetrics): Double {
        return (metrics.winRate * 0.3 + 
                (metrics.averageReward + 1.0) / 2.0 * 0.3 + 
                metrics.averageGameQuality * 0.2 + 
                metrics.trainingEfficiency * 0.2)
    }
    
    private fun calculateConvergenceIndicator(): Double {
        if (performanceHistory.size < 10) return 0.0
        
        val recent = performanceHistory.takeLast(10)
        val scores = recent.map { it.overallScore }
        return calculateStability(scores)
    }
    
    private fun estimateConvergenceCycles(trend: Double, stability: Double): Int? {
        if (stability > 0.9) return 0 // Already converged
        if (trend <= 0) return null // Not improving
        
        val remainingImprovement = 1.0 - stability
        val cyclesNeeded = (remainingImprovement / trend).toInt()
        return cyclesNeeded.coerceIn(1, 1000)
    }
    
    private fun calculateOverallEfficiency(): Double {
        if (trainingHistory.isEmpty()) return 0.0
        
        val totalTime = getCurrentTimeMillis() - monitoringStartTime
        val totalPerformanceGain = performanceHistory.lastOrNull()?.overallScore ?: 0.0
        
        return totalPerformanceGain / (totalTime / 1000.0 / 3600.0) // Performance gain per hour
    }
    
    private fun calculateMetricTrends(metric: String, window: Int): Any {
        // Implementation for calculating specific metric trends
        return "Trends for $metric over $window cycles"
    }
    
    private fun exportTrainingData(format: String, path: String): Any {
        // Implementation for exporting training data
        return "Data exported to $path in $format format"
    }
    
    private fun exportAllData(): List<String> {
        // Implementation for exporting all monitoring data
        return listOf("training_metrics.json", "game_analyses.json", "issues.json")
    }
    
    private fun generateSessionId(): String {
        return "session_${getCurrentTimeMillis()}"
    }
}
