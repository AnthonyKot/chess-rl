package com.chessrl.integration

import kotlin.math.*

/**
 * Early Stopping Detector for Chess RL Training
 * 
 * Detects when training should be stopped early based on:
 * - Performance stagnation
 * - Training instabilities
 * - Convergence achievement
 * - Resource constraints
 * - Quality degradation
 */
class EarlyStoppingDetector(
    private val config: EarlyStoppingConfig = EarlyStoppingConfig()
) {
    
    private val performanceHistory = mutableListOf<Double>()
    private val stagnationHistory = mutableListOf<Boolean>()
    private val instabilityHistory = mutableListOf<Boolean>()
    private var bestPerformance = 0.0
    private var cyclesSinceBestPerformance = 0
    private var consecutiveStagnationCycles = 0
    private var consecutiveInstabilityCycles = 0
    
    /**
     * Initialize the early stopping detector
     */
    fun initialize() {
        println("🛑 Initializing Early Stopping Detector")
        println("Configuration: $config")
        
        performanceHistory.clear()
        stagnationHistory.clear()
        instabilityHistory.clear()
        bestPerformance = 0.0
        cyclesSinceBestPerformance = 0
        consecutiveStagnationCycles = 0
        consecutiveInstabilityCycles = 0
        
        println("✅ Early Stopping Detector initialized")
    }
    
    /**
     * Check if training should be stopped early
     */
    fun checkEarlyStopping(
        cycle: Int,
        performanceScore: Double,
        convergenceStatus: ConvergenceStatus,
        learningStatus: LearningStatus,
        validationIssues: List<ValidationIssue>
    ): EarlyStoppingRecommendation {
        
        // Update performance tracking
        updatePerformanceTracking(performanceScore)
        
        // Check various stopping criteria
        val stagnationStop = checkStagnationCriteria(cycle, performanceScore)
        val convergenceStop = checkConvergenceCriteria(convergenceStatus)
        val instabilityStop = checkInstabilityCriteria(validationIssues)
        val resourceStop = checkResourceCriteria(cycle)
        val qualityStop = checkQualityCriteria(learningStatus)
        val patienceStop = checkPatienceCriteria()
        
        // Determine overall stopping recommendation
        val stoppingCriteria = listOfNotNull(
            stagnationStop,
            convergenceStop,
            instabilityStop,
            resourceStop,
            qualityStop,
            patienceStop
        )
        
        val shouldStop = stoppingCriteria.isNotEmpty()
        val primaryReason = stoppingCriteria.firstOrNull()?.reason ?: "Continue training"
        val confidence = calculateStoppingConfidence(stoppingCriteria)
        
        // Generate recommendations
        val recommendations = generateStoppingRecommendations(stoppingCriteria, learningStatus)
        
        return EarlyStoppingRecommendation(
            shouldStop = shouldStop,
            confidence = confidence,
            reason = primaryReason,
            stoppingCriteria = stoppingCriteria,
            recommendations = recommendations,
            cyclesSinceBestPerformance = cyclesSinceBestPerformance,
            bestPerformanceScore = bestPerformance,
            currentPerformanceScore = performanceScore
        )
    }
    
    /**
     * Get early stopping statistics
     */
    fun getEarlyStoppingStats(): EarlyStoppingStats {
        return EarlyStoppingStats(
            totalCyclesMonitored = performanceHistory.size,
            bestPerformance = bestPerformance,
            cyclesSinceBestPerformance = cyclesSinceBestPerformance,
            consecutiveStagnationCycles = consecutiveStagnationCycles,
            consecutiveInstabilityCycles = consecutiveInstabilityCycles,
            averagePerformance = if (performanceHistory.isNotEmpty()) performanceHistory.average() else 0.0,
            performanceVariance = calculatePerformanceVariance()
        )
    }
    
    // Private stopping criteria methods
    
    private fun updatePerformanceTracking(performanceScore: Double) {
        performanceHistory.add(performanceScore)
        
        // Update best performance tracking
        if (performanceScore > bestPerformance) {
            bestPerformance = performanceScore
            cyclesSinceBestPerformance = 0
        } else {
            cyclesSinceBestPerformance++
        }
        
        // Limit history size
        if (performanceHistory.size > config.maxHistorySize) {
            performanceHistory.removeAt(0)
        }
    }
    
    private fun checkStagnationCriteria(cycle: Int, performanceScore: Double): StoppingCriterion? {
        if (performanceHistory.size < config.stagnationWindow) {
            return null
        }
        
        val recentPerformance = performanceHistory.takeLast(config.stagnationWindow)
        val performanceVariance = calculateVariance(recentPerformance)
        val performanceTrend = calculateTrend(recentPerformance)
        
        val isStagnant = performanceVariance < config.stagnationVarianceThreshold &&
                        abs(performanceTrend) < config.stagnationTrendThreshold
        
        stagnationHistory.add(isStagnant)
        
        if (isStagnant) {
            consecutiveStagnationCycles++
        } else {
            consecutiveStagnationCycles = 0
        }
        
        return if (consecutiveStagnationCycles >= config.maxStagnationCycles) {
            StoppingCriterion(
                type = StoppingCriterionType.STAGNATION,
                reason = "Performance stagnant for ${consecutiveStagnationCycles} cycles",
                confidence = 0.8,
                severity = StoppingSeverity.MEDIUM
            )
        } else null
    }
    
    private fun checkConvergenceCriteria(convergenceStatus: ConvergenceStatus): StoppingCriterion? {
        return if (convergenceStatus.hasConverged && convergenceStatus.confidence > config.convergenceConfidenceThreshold) {
            StoppingCriterion(
                type = StoppingCriterionType.CONVERGENCE,
                reason = "Training has converged (confidence: ${String.format("%.3f", convergenceStatus.confidence)})",
                confidence = convergenceStatus.confidence,
                severity = StoppingSeverity.LOW
            )
        } else null
    }
    
    private fun checkInstabilityCriteria(validationIssues: List<ValidationIssue>): StoppingCriterion? {
        val criticalIssues = validationIssues.filter { it.severity == IssueSeverity.HIGH || it.severity == IssueSeverity.CRITICAL }
        val hasInstability = criticalIssues.isNotEmpty()
        
        instabilityHistory.add(hasInstability)
        
        if (hasInstability) {
            consecutiveInstabilityCycles++
        } else {
            consecutiveInstabilityCycles = 0
        }
        
        return if (consecutiveInstabilityCycles >= config.maxInstabilityCycles) {
            StoppingCriterion(
                type = StoppingCriterionType.INSTABILITY,
                reason = "Training instability for ${consecutiveInstabilityCycles} cycles: ${criticalIssues.map { it.type }.distinct()}",
                confidence = 0.9,
                severity = StoppingSeverity.HIGH
            )
        } else null
    }
    
    private fun checkResourceCriteria(cycle: Int): StoppingCriterion? {
        return if (cycle >= config.maxTrainingCycles) {
            StoppingCriterion(
                type = StoppingCriterionType.RESOURCE_LIMIT,
                reason = "Maximum training cycles reached (${config.maxTrainingCycles})",
                confidence = 1.0,
                severity = StoppingSeverity.LOW
            )
        } else null
    }
    
    private fun checkQualityCriteria(learningStatus: LearningStatus): StoppingCriterion? {
        return when (learningStatus.status) {
            LearningStatusType.DECLINING -> {
                if (learningStatus.learningRate < -config.qualityDeclineThreshold) {
                    StoppingCriterion(
                        type = StoppingCriterionType.QUALITY_DEGRADATION,
                        reason = "Performance declining significantly (rate: ${String.format("%.4f", learningStatus.learningRate)})",
                        confidence = learningStatus.confidence,
                        severity = StoppingSeverity.HIGH
                    )
                } else null
            }
            else -> null
        }
    }
    
    private fun checkPatienceCriteria(): StoppingCriterion? {
        return if (cyclesSinceBestPerformance >= config.patienceCycles) {
            StoppingCriterion(
                type = StoppingCriterionType.PATIENCE_EXHAUSTED,
                reason = "No improvement for ${cyclesSinceBestPerformance} cycles (patience: ${config.patienceCycles})",
                confidence = 0.7,
                severity = StoppingSeverity.MEDIUM
            )
        } else null
    }
    
    private fun calculateStoppingConfidence(criteria: List<StoppingCriterion>): Double {
        if (criteria.isEmpty()) return 0.0
        
        // Weight confidence by severity
        val weightedConfidences = criteria.map { criterion ->
            val severityWeight = when (criterion.severity) {
                StoppingSeverity.LOW -> 0.5
                StoppingSeverity.MEDIUM -> 0.7
                StoppingSeverity.HIGH -> 1.0
            }
            criterion.confidence * severityWeight
        }
        
        return weightedConfidences.maxOrNull() ?: 0.0
    }
    
    private fun generateStoppingRecommendations(
        criteria: List<StoppingCriterion>,
        learningStatus: LearningStatus
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (criteria.isEmpty()) {
            recommendations.add("Continue training - no stopping criteria met")
            return recommendations
        }
        
        // Criterion-specific recommendations
        criteria.forEach { criterion ->
            when (criterion.type) {
                StoppingCriterionType.STAGNATION -> {
                    recommendations.add("Training has stagnated - consider stopping or major parameter changes")
                    recommendations.add("Try increasing exploration rate or changing architecture")
                }
                StoppingCriterionType.CONVERGENCE -> {
                    recommendations.add("Training has converged successfully - safe to stop")
                    recommendations.add("Consider fine-tuning or evaluation phase")
                }
                StoppingCriterionType.INSTABILITY -> {
                    recommendations.add("Training is unstable - stop and investigate issues")
                    recommendations.add("Check learning rate, gradient clipping, and network architecture")
                }
                StoppingCriterionType.RESOURCE_LIMIT -> {
                    recommendations.add("Resource limit reached - stop training")
                    recommendations.add("Evaluate current model performance")
                }
                StoppingCriterionType.QUALITY_DEGRADATION -> {
                    recommendations.add("Performance is degrading - stop and rollback to best model")
                    recommendations.add("Investigate training instabilities")
                }
                StoppingCriterionType.PATIENCE_EXHAUSTED -> {
                    recommendations.add("No improvement for extended period - consider stopping")
                    recommendations.add("Try different hyperparameters or architecture changes")
                }
            }
        }
        
        // Learning status specific recommendations
        when (learningStatus.status) {
            LearningStatusType.DECLINING -> {
                recommendations.add("Performance is declining - immediate intervention needed")
            }
            LearningStatusType.STAGNANT -> {
                recommendations.add("Learning has stagnated - major changes needed to continue")
            }
            else -> {
                // No additional recommendations for other statuses
            }
        }
        
        return recommendations.distinct()
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
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculatePerformanceVariance(): Double {
        return if (performanceHistory.isNotEmpty()) {
            calculateVariance(performanceHistory)
        } else 0.0
    }
}

// Configuration and data classes

/**
 * Configuration for early stopping detection
 */
data class EarlyStoppingConfig(
    // Stagnation criteria
    val stagnationWindow: Int = 15,
    val maxStagnationCycles: Int = 20,
    val stagnationVarianceThreshold: Double = 0.001,
    val stagnationTrendThreshold: Double = 0.001,
    
    // Convergence criteria
    val convergenceConfidenceThreshold: Double = 0.8,
    
    // Instability criteria
    val maxInstabilityCycles: Int = 5,
    
    // Resource criteria
    val maxTrainingCycles: Int = 1000,
    
    // Quality criteria
    val qualityDeclineThreshold: Double = 0.01,
    
    // Patience criteria
    val patienceCycles: Int = 50,
    
    // History management
    val maxHistorySize: Int = 200
)

/**
 * Early stopping recommendation
 */
data class EarlyStoppingRecommendation(
    val shouldStop: Boolean,
    val confidence: Double,
    val reason: String,
    val stoppingCriteria: List<StoppingCriterion>,
    val recommendations: List<String>,
    val cyclesSinceBestPerformance: Int,
    val bestPerformanceScore: Double,
    val currentPerformanceScore: Double
)

/**
 * Individual stopping criterion
 */
data class StoppingCriterion(
    val type: StoppingCriterionType,
    val reason: String,
    val confidence: Double,
    val severity: StoppingSeverity
)

/**
 * Types of stopping criteria
 */
enum class StoppingCriterionType {
    STAGNATION,
    CONVERGENCE,
    INSTABILITY,
    RESOURCE_LIMIT,
    QUALITY_DEGRADATION,
    PATIENCE_EXHAUSTED
}

/**
 * Severity levels for stopping criteria
 */
enum class StoppingSeverity {
    LOW,    // Gentle suggestion to stop
    MEDIUM, // Strong recommendation to stop
    HIGH    // Urgent need to stop
}

/**
 * Early stopping statistics
 */
data class EarlyStoppingStats(
    val totalCyclesMonitored: Int,
    val bestPerformance: Double,
    val cyclesSinceBestPerformance: Int,
    val consecutiveStagnationCycles: Int,
    val consecutiveInstabilityCycles: Int,
    val averagePerformance: Double,
    val performanceVariance: Double
)