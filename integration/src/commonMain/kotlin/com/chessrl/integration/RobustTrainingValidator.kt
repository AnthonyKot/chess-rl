package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*
import kotlin.math.*

/**
 * Robust Training Validation System for Chess RL
 * 
 * Provides comprehensive training validation including:
 * - Learning vs. stagnation detection
 * - Baseline evaluation against heuristic opponents
 * - Real chess improvement tracking
 * - Early stopping and training issue detection
 * 
 * This system integrates existing validation components and adds new capabilities
 * for production-ready training validation.
 */
class RobustTrainingValidator(
    private val config: RobustValidationConfig = RobustValidationConfig()
) {
    
    // Core validation components
    private val trainingValidator = TrainingValidator(config.trainingValidatorConfig)
    private val chessValidator = ChessTrainingValidator(config.chessValidatorConfig)
    private val convergenceDetector = ConvergenceDetector(config.convergenceConfig)
    private val baselineEvaluator = BaselineEvaluator(config.baselineConfig)
    private val progressTracker = ChessProgressTracker(config.progressConfig)
    private val earlyStoppingDetector = EarlyStoppingDetector(config.earlyStoppingConfig)
    
    // Validation state
    private val validationHistory = mutableListOf<RobustValidationResult>()
    private val baselineResults = mutableListOf<BaselineEvaluationResult>()
    private val progressSnapshots = mutableListOf<ProgressSnapshot>()
    private var lastValidationTime = 0L
    private var consecutiveStagnationCycles = 0
    private var bestPerformanceScore = 0.0
    
    /**
     * Initialize the robust training validator
     */
    fun initialize() {
        println("🔍 Initializing Robust Training Validation System")
        println("Configuration: $config")
        
        // Initialize components
        convergenceDetector.reset()
        baselineEvaluator.initialize()
        progressTracker.initialize()
        earlyStoppingDetector.initialize()
        
        // Clear state
        validationHistory.clear()
        baselineResults.clear()
        progressSnapshots.clear()
        lastValidationTime = getCurrentTimeMillis()
        consecutiveStagnationCycles = 0
        bestPerformanceScore = 0.0
        
        println("✅ Robust Training Validator initialized")
    }
    
    /**
     * Perform comprehensive training validation
     */
    fun validateTrainingCycle(
        cycle: Int,
        agent: ChessAgent,
        environment: ChessEnvironment,
        trainingMetrics: RLMetrics,
        updateResult: PolicyUpdateResult,
        gameResults: List<ChessGameResult>
    ): RobustValidationResult {
        
        val validationStartTime = getCurrentTimeMillis()
        
        println("🔍 Performing robust validation for cycle $cycle")
        
        // 1. Core training validation
        val policyValidation = trainingValidator.validatePolicyUpdate(
            beforeMetrics = trainingMetrics,
            afterMetrics = trainingMetrics, // Same for single cycle
            updateResult = updateResult,
            episodeNumber = cycle
        )
        
        // 2. Chess-specific validation
        val chessValidation = chessValidator.validateChessTraining(
            gameResults = gameResults,
            episodeNumber = cycle
        )
        
        // 3. Learning vs. stagnation detection
        val learningStatus = detectLearningStatus(cycle, trainingMetrics, gameResults)
        
        // 4. Baseline evaluation (periodic)
        val baselineEvaluation = if (shouldPerformBaselineEvaluation(cycle)) {
            performBaselineEvaluation(agent, environment, cycle)
        } else null
        
        // 5. Progress tracking
        val progressUpdate = progressTracker.updateProgress(
            cycle = cycle,
            gameResults = gameResults,
            trainingMetrics = trainingMetrics,
            baselineResult = baselineEvaluation
        )
        
        // 6. Convergence analysis
        val performanceHistory = validationHistory.map { it.overallPerformanceScore }
        val convergenceStatus = convergenceDetector.checkConvergence(performanceHistory + calculatePerformanceScore(trainingMetrics))
        
        // 7. Early stopping detection
        val earlyStoppingRecommendation = earlyStoppingDetector.checkEarlyStopping(
            cycle = cycle,
            performanceScore = calculatePerformanceScore(trainingMetrics),
            convergenceStatus = convergenceStatus,
            learningStatus = learningStatus,
            validationIssues = policyValidation.issues + chessValidation.issues.map { 
                ValidationIssue(
                    type = mapChessIssueToValidationIssue(it.type),
                    severity = it.severity,
                    message = it.message,
                    value = it.value
                )
            }
        )
        
        // 8. Generate comprehensive recommendations
        val recommendations = generateComprehensiveRecommendations(
            policyValidation = policyValidation,
            chessValidation = chessValidation,
            learningStatus = learningStatus,
            convergenceStatus = convergenceStatus,
            earlyStoppingRecommendation = earlyStoppingRecommendation
        )
        
        // 9. Calculate overall validation result
        val overallPerformanceScore = calculatePerformanceScore(trainingMetrics)
        val isValid = policyValidation.isValid && chessValidation.isValid
        val shouldStop = earlyStoppingRecommendation.shouldStop
        
        // Update best performance tracking
        if (overallPerformanceScore > bestPerformanceScore) {
            bestPerformanceScore = overallPerformanceScore
            consecutiveStagnationCycles = 0
        } else {
            consecutiveStagnationCycles++
        }
        
        // Create validation result
        val validationResult = RobustValidationResult(
            cycle = cycle,
            timestamp = getCurrentTimeMillis(),
            isValid = isValid,
            shouldStop = shouldStop,
            overallPerformanceScore = overallPerformanceScore,
            learningStatus = learningStatus,
            policyValidation = policyValidation,
            chessValidation = chessValidation,
            baselineEvaluation = baselineEvaluation,
            progressUpdate = progressUpdate,
            convergenceStatus = convergenceStatus,
            earlyStoppingRecommendation = earlyStoppingRecommendation,
            recommendations = recommendations,
            validationTime = getCurrentTimeMillis() - validationStartTime
        )
        
        // Record validation result
        validationHistory.add(validationResult)
        baselineEvaluation?.let { baselineResults.add(it) }
        progressSnapshots.add(ProgressSnapshot(
            cycle = cycle,
            timestamp = getCurrentTimeMillis(),
            performanceScore = overallPerformanceScore,
            chessImprovement = progressUpdate.chessImprovementScore,
            learningRate = learningStatus.learningRate
        ))
        
        // Display validation summary
        displayValidationSummary(validationResult)
        
        return validationResult
    }
    
    /**
     * Detect learning vs. stagnation status
     */
    private fun detectLearningStatus(
        cycle: Int,
        trainingMetrics: RLMetrics,
        gameResults: List<ChessGameResult>
    ): LearningStatus {
        
        if (validationHistory.size < config.learningDetectionWindow) {
            return LearningStatus(
                status = LearningStatusType.INSUFFICIENT_DATA,
                learningRate = 0.0,
                stagnationCycles = 0,
                improvementTrend = 0.0,
                confidence = 0.0,
                evidence = listOf("Need more cycles for learning detection")
            )
        }
        
        val recentHistory = validationHistory.takeLast(config.learningDetectionWindow)
        val performanceScores = recentHistory.map { it.overallPerformanceScore }
        val chessScores = recentHistory.mapNotNull { it.progressUpdate?.chessImprovementScore }
        
        // Calculate learning indicators
        val performanceTrend = calculateTrend(performanceScores)
        val chessTrend = if (chessScores.isNotEmpty()) calculateTrend(chessScores) else 0.0
        val performanceStability = calculateStability(performanceScores)
        val recentImprovement = performanceScores.last() - performanceScores.first()
        
        // Determine learning status
        val status = when {
            performanceTrend > config.learningThreshold && recentImprovement > 0.01 -> LearningStatusType.LEARNING
            performanceTrend > config.improvementThreshold -> LearningStatusType.SLOW_IMPROVEMENT
            abs(performanceTrend) <= config.stagnationThreshold && performanceStability > 0.8 -> LearningStatusType.STAGNANT
            performanceTrend < -config.learningThreshold -> LearningStatusType.DECLINING
            else -> LearningStatusType.UNSTABLE
        }
        
        // Calculate confidence based on data quality
        val confidence = calculateLearningConfidence(performanceStability, recentHistory.size)
        
        // Generate evidence
        val evidence = generateLearningEvidence(status, performanceTrend, chessTrend, recentImprovement)
        
        return LearningStatus(
            status = status,
            learningRate = performanceTrend,
            stagnationCycles = consecutiveStagnationCycles,
            improvementTrend = recentImprovement,
            confidence = confidence,
            evidence = evidence
        )
    }
    
    /**
     * Perform baseline evaluation against heuristic opponents
     */
    private fun performBaselineEvaluation(
        agent: ChessAgent,
        environment: ChessEnvironment,
        cycle: Int
    ): BaselineEvaluationResult {
        
        println("🎯 Performing baseline evaluation against heuristic opponents...")
        
        val evaluationStartTime = getCurrentTimeMillis()
        
        // Evaluate against different baseline opponents
        val randomOpponentResult = baselineEvaluator.evaluateAgainstRandomOpponent(agent, environment)
        val heuristicOpponentResult = baselineEvaluator.evaluateAgainstHeuristicOpponent(agent, environment)
        val materialOpponentResult = baselineEvaluator.evaluateAgainstMaterialOpponent(agent, environment)
        
        // Calculate overall baseline performance
        val overallScore = (randomOpponentResult.winRate * 0.2 + 
                           heuristicOpponentResult.winRate * 0.5 + 
                           materialOpponentResult.winRate * 0.3)
        
        // Determine improvement since last baseline
        val improvement = if (baselineResults.isNotEmpty()) {
            overallScore - baselineResults.last().overallScore
        } else 0.0
        
        // Generate baseline insights
        val insights = generateBaselineInsights(
            randomOpponentResult,
            heuristicOpponentResult,
            materialOpponentResult,
            improvement
        )
        
        val result = BaselineEvaluationResult(
            cycle = cycle,
            timestamp = getCurrentTimeMillis(),
            randomOpponentResult = randomOpponentResult,
            heuristicOpponentResult = heuristicOpponentResult,
            materialOpponentResult = materialOpponentResult,
            overallScore = overallScore,
            improvementSinceLastEvaluation = improvement,
            insights = insights,
            evaluationTime = getCurrentTimeMillis() - evaluationStartTime
        )
        
        println("✅ Baseline evaluation completed. Overall score: ${String.format("%.3f", overallScore)}")
        
        return result
    }
    
    /**
     * Generate comprehensive recommendations
     */
    private fun generateComprehensiveRecommendations(
        policyValidation: PolicyValidationResult,
        chessValidation: ChessValidationResult,
        learningStatus: LearningStatus,
        convergenceStatus: ConvergenceStatus,
        earlyStoppingRecommendation: EarlyStoppingRecommendation
    ): List<TrainingRecommendation> {
        
        val recommendations = mutableListOf<TrainingRecommendation>()
        
        // Early stopping recommendations (highest priority)
        if (earlyStoppingRecommendation.shouldStop) {
            recommendations.add(TrainingRecommendation(
                type = RecommendationType.STABILITY,
                priority = RecommendationPriority.CRITICAL,
                title = "Early Stopping Recommended",
                description = earlyStoppingRecommendation.reason,
                actions = earlyStoppingRecommendation.recommendations
            ))
        }
        
        // Learning status recommendations
        when (learningStatus.status) {
            LearningStatusType.STAGNANT -> {
                recommendations.add(TrainingRecommendation(
                    type = RecommendationType.EXPLORATION,
                    priority = RecommendationPriority.HIGH,
                    title = "Training Stagnation Detected",
                    description = "Agent has been stagnant for ${learningStatus.stagnationCycles} cycles",
                    actions = listOf(
                        "Increase exploration rate by 50%",
                        "Add curriculum learning",
                        "Adjust reward function",
                        "Consider architecture changes"
                    )
                ))
            }
            LearningStatusType.DECLINING -> {
                recommendations.add(TrainingRecommendation(
                    type = RecommendationType.STABILITY,
                    priority = RecommendationPriority.CRITICAL,
                    title = "Performance Declining",
                    description = "Agent performance is declining (trend: ${String.format("%.4f", learningStatus.learningRate)})",
                    actions = listOf(
                        "Reduce learning rate by 50%",
                        "Check for training instabilities",
                        "Consider model rollback",
                        "Increase regularization"
                    )
                ))
            }
            LearningStatusType.LEARNING -> {
                recommendations.add(TrainingRecommendation(
                    type = RecommendationType.PERFORMANCE,
                    priority = RecommendationPriority.LOW,
                    title = "Good Learning Progress",
                    description = "Agent is learning well, continue current approach",
                    actions = listOf("Continue current training parameters")
                ))
            }
            else -> {
                // Handle other learning statuses
            }
        }
        
        // Policy validation recommendations
        if (!policyValidation.isValid) {
            policyValidation.recommendations.forEach { rec ->
                recommendations.add(TrainingRecommendation(
                    type = RecommendationType.STABILITY,
                    priority = RecommendationPriority.HIGH,
                    title = "Policy Validation Issue",
                    description = rec,
                    actions = listOf(rec)
                ))
            }
        }
        
        // Chess validation recommendations
        if (!chessValidation.isValid) {
            chessValidation.recommendations.forEach { rec ->
                recommendations.add(TrainingRecommendation(
                    type = RecommendationType.PERFORMANCE,
                    priority = RecommendationPriority.MEDIUM,
                    title = "Chess Training Issue",
                    description = rec,
                    actions = listOf(rec)
                ))
            }
        }
        
        // Convergence recommendations
        recommendations.addAll(convergenceStatus.recommendations.map { rec ->
            TrainingRecommendation(
                type = RecommendationType.PERFORMANCE,
                priority = RecommendationPriority.MEDIUM,
                title = "Convergence Analysis",
                description = rec,
                actions = listOf(rec)
            )
        })
        
        return recommendations.sortedByDescending { it.priority.ordinal }
    }
    
    /**
     * Get comprehensive validation summary
     */
    fun getValidationSummary(): RobustValidationSummary {
        return RobustValidationSummary(
            totalValidations = validationHistory.size,
            validValidations = validationHistory.count { it.isValid },
            earlyStoppingSuggestions = validationHistory.count { it.shouldStop },
            bestPerformanceScore = bestPerformanceScore,
            currentLearningStatus = validationHistory.lastOrNull()?.learningStatus?.status ?: LearningStatusType.INSUFFICIENT_DATA,
            baselineEvaluations = baselineResults.size,
            averageBaselineScore = if (baselineResults.isNotEmpty()) baselineResults.map { it.overallScore }.average() else 0.0,
            convergenceStatus = validationHistory.lastOrNull()?.convergenceStatus?.hasConverged ?: false,
            totalValidationTime = validationHistory.sumOf { it.validationTime },
            recommendations = validationHistory.lastOrNull()?.recommendations ?: emptyList()
        )
    }
    
    /**
     * Export validation data for analysis
     */
    fun exportValidationData(format: String = "json"): ValidationDataExport {
        return ValidationDataExport(
            format = format,
            timestamp = getCurrentTimeMillis(),
            validationHistory = validationHistory,
            baselineResults = baselineResults,
            progressSnapshots = progressSnapshots,
            summary = getValidationSummary()
        )
    }
    
    // Private helper methods
    
    private fun shouldPerformBaselineEvaluation(cycle: Int): Boolean {
        return cycle % config.baselineEvaluationInterval == 0 || cycle <= 5
    }
    
    private fun calculatePerformanceScore(metrics: RLMetrics): Double {
        // Weighted combination of key performance indicators
        val rewardScore = (metrics.averageReward + 1.0) / 2.0 // Normalize to [0,1]
        val explorationScore = 1.0 - metrics.explorationRate // Higher when exploration is lower (more confident)
        val lossScore = 1.0 / (1.0 + metrics.policyLoss) // Lower loss is better
        
        return (rewardScore * 0.5 + explorationScore * 0.2 + lossScore * 0.3).coerceIn(0.0, 1.0)
    }
    
    private fun mapChessIssueToValidationIssue(chessIssue: ChessIssueType): IssueType {
        return when (chessIssue) {
            ChessIssueType.LOW_MOVE_DIVERSITY -> IssueType.EXPLORATION_INSUFFICIENT
            ChessIssueType.HIGH_BLUNDER_RATE -> IssueType.Q_VALUE_OVERESTIMATION
            ChessIssueType.GAMES_TOO_SHORT -> IssueType.POLICY_COLLAPSE
            else -> IssueType.NUMERICAL_INSTABILITY
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
    
    private fun calculateLearningConfidence(stability: Double, sampleSize: Int): Double {
        val stabilityScore = stability.coerceIn(0.0, 1.0)
        val sampleScore = (sampleSize.toDouble() / config.learningDetectionWindow).coerceIn(0.0, 1.0)
        return (stabilityScore + sampleScore) / 2.0
    }
    
    private fun generateLearningEvidence(
        status: LearningStatusType,
        performanceTrend: Double,
        chessTrend: Double,
        recentImprovement: Double
    ): List<String> {
        val evidence = mutableListOf<String>()
        
        evidence.add("Performance trend: ${String.format("%.4f", performanceTrend)}")
        evidence.add("Chess improvement trend: ${String.format("%.4f", chessTrend)}")
        evidence.add("Recent improvement: ${String.format("%.4f", recentImprovement)}")
        
        when (status) {
            LearningStatusType.LEARNING -> {
                evidence.add("Strong positive trend in multiple metrics")
                evidence.add("Consistent improvement over recent cycles")
            }
            LearningStatusType.STAGNANT -> {
                evidence.add("Minimal change in performance metrics")
                evidence.add("High stability but low improvement")
            }
            LearningStatusType.DECLINING -> {
                evidence.add("Negative trend in performance metrics")
                evidence.add("Potential training instability")
            }
            else -> {
                evidence.add("Mixed or unclear learning signals")
            }
        }
        
        return evidence
    }
    
    private fun generateBaselineInsights(
        randomResult: OpponentEvaluationResult,
        heuristicResult: OpponentEvaluationResult,
        materialResult: OpponentEvaluationResult,
        improvement: Double
    ): List<String> {
        val insights = mutableListOf<String>()
        
        // Performance against different opponents
        insights.add("Random opponent win rate: ${String.format("%.1f%%", randomResult.winRate * 100)}")
        insights.add("Heuristic opponent win rate: ${String.format("%.1f%%", heuristicResult.winRate * 100)}")
        insights.add("Material opponent win rate: ${String.format("%.1f%%", materialResult.winRate * 100)}")
        
        // Improvement analysis
        if (improvement > 0.05) {
            insights.add("Significant improvement since last evaluation (+${String.format("%.3f", improvement)})")
        } else if (improvement < -0.05) {
            insights.add("Performance decline since last evaluation (${String.format("%.3f", improvement)})")
        } else {
            insights.add("Stable performance since last evaluation")
        }
        
        // Relative performance analysis
        if (heuristicResult.winRate > 0.7) {
            insights.add("Strong performance against heuristic opponent")
        } else if (heuristicResult.winRate < 0.3) {
            insights.add("Struggling against heuristic opponent - needs improvement")
        }
        
        return insights
    }
    
    private fun displayValidationSummary(result: RobustValidationResult) {
        println("\n" + "=" * 60)
        println("🔍 ROBUST VALIDATION SUMMARY - Cycle ${result.cycle}")
        println("=" * 60)
        
        println("📊 Overall Status:")
        println("   Valid: ${if (result.isValid) "✅" else "❌"}")
        println("   Should Stop: ${if (result.shouldStop) "🛑 YES" else "✅ NO"}")
        println("   Performance Score: ${String.format("%.3f", result.overallPerformanceScore)}")
        println("   Learning Status: ${result.learningStatus.status}")
        
        if (result.baselineEvaluation != null) {
            println("\n🎯 Baseline Evaluation:")
            println("   Overall Score: ${String.format("%.3f", result.baselineEvaluation.overallScore)}")
            println("   Improvement: ${String.format("%.3f", result.baselineEvaluation.improvementSinceLastEvaluation)}")
        }
        
        println("\n📈 Progress:")
        println("   Chess Improvement: ${String.format("%.3f", result.progressUpdate?.chessImprovementScore ?: 0.0)}")
        println("   Convergence: ${if (result.convergenceStatus.hasConverged) "✅ Converged" else "⏳ In Progress"}")
        
        if (result.recommendations.isNotEmpty()) {
            println("\n💡 Top Recommendations:")
            result.recommendations.take(3).forEach { rec ->
                println("   ${rec.priority.name}: ${rec.title}")
            }
        }
        
        println("=" * 60)
    }
}

// Configuration and data classes

/**
 * Configuration for robust training validation
 */
data class RobustValidationConfig(
    val trainingValidatorConfig: ValidationConfig = ValidationConfig(),
    val chessValidatorConfig: ChessValidationConfig = ChessValidationConfig(),
    val convergenceConfig: ConvergenceConfig = ConvergenceConfig(),
    val baselineConfig: BaselineEvaluationConfig = BaselineEvaluationConfig(),
    val progressConfig: ProgressTrackingConfig = ProgressTrackingConfig(),
    val earlyStoppingConfig: EarlyStoppingConfig = EarlyStoppingConfig(),
    
    // Learning detection parameters
    val learningDetectionWindow: Int = 10,
    val learningThreshold: Double = 0.01,
    val improvementThreshold: Double = 0.005,
    val stagnationThreshold: Double = 0.001,
    
    // Baseline evaluation parameters
    val baselineEvaluationInterval: Int = 10,
    val baselineGamesPerOpponent: Int = 20
)

/**
 * Comprehensive validation result
 */
data class RobustValidationResult(
    val cycle: Int,
    val timestamp: Long,
    val isValid: Boolean,
    val shouldStop: Boolean,
    val overallPerformanceScore: Double,
    val learningStatus: LearningStatus,
    val policyValidation: PolicyValidationResult,
    val chessValidation: ChessValidationResult,
    val baselineEvaluation: BaselineEvaluationResult?,
    val progressUpdate: ProgressUpdate?,
    val convergenceStatus: ConvergenceStatus,
    val earlyStoppingRecommendation: EarlyStoppingRecommendation,
    val recommendations: List<TrainingRecommendation>,
    val validationTime: Long
)

/**
 * Learning status detection result
 */
data class LearningStatus(
    val status: LearningStatusType,
    val learningRate: Double,
    val stagnationCycles: Int,
    val improvementTrend: Double,
    val confidence: Double,
    val evidence: List<String>
)

/**
 * Learning status types
 */
enum class LearningStatusType {
    LEARNING,
    SLOW_IMPROVEMENT,
    STAGNANT,
    DECLINING,
    UNSTABLE,
    INSUFFICIENT_DATA
}

/**
 * Baseline evaluation result
 */
data class BaselineEvaluationResult(
    val cycle: Int,
    val timestamp: Long,
    val randomOpponentResult: OpponentEvaluationResult,
    val heuristicOpponentResult: OpponentEvaluationResult,
    val materialOpponentResult: OpponentEvaluationResult,
    val overallScore: Double,
    val improvementSinceLastEvaluation: Double,
    val insights: List<String>,
    val evaluationTime: Long
)

/**
 * Progress snapshot for tracking
 */
data class ProgressSnapshot(
    val cycle: Int,
    val timestamp: Long,
    val performanceScore: Double,
    val chessImprovement: Double,
    val learningRate: Double
)

/**
 * Validation summary
 */
data class RobustValidationSummary(
    val totalValidations: Int,
    val validValidations: Int,
    val earlyStoppingSuggestions: Int,
    val bestPerformanceScore: Double,
    val currentLearningStatus: LearningStatusType,
    val baselineEvaluations: Int,
    val averageBaselineScore: Double,
    val convergenceStatus: Boolean,
    val totalValidationTime: Long,
    val recommendations: List<TrainingRecommendation>
)

/**
 * Validation data export
 */
data class ValidationDataExport(
    val format: String,
    val timestamp: Long,
    val validationHistory: List<RobustValidationResult>,
    val baselineResults: List<BaselineEvaluationResult>,
    val progressSnapshots: List<ProgressSnapshot>,
    val summary: RobustValidationSummary
)