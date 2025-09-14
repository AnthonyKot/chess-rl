package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Training debugger that provides detailed analysis and debugging tools
 * for chess RL training issues and policy updates
 */
class TrainingDebugger(
    private val config: DebuggerConfig = DebuggerConfig()
) {
    
    private val debugHistory = mutableListOf<DebugSnapshot>()
    private val policyAnalysisHistory = mutableListOf<PolicyAnalysisSnapshot>()
    
    /**
     * Analyze policy update in detail for debugging
     */
    fun analyzePolicyUpdate(
        updateResult: PolicyUpdateResult,
        beforeMetrics: RLMetrics,
        afterMetrics: RLMetrics,
        experiences: List<Experience<DoubleArray, Int>>,
        episodeNumber: Int
    ): PolicyUpdateAnalysis {
        
        // Analyze loss behavior
        val lossAnalysis = analyzeLossBehavior(updateResult, beforeMetrics, afterMetrics)
        
        // Analyze gradient behavior
        val gradientAnalysis = analyzeGradientBehavior(updateResult)
        
        // Analyze policy changes
        val policyAnalysis = analyzePolicyChanges(updateResult, beforeMetrics, afterMetrics)
        
        // Analyze experience quality
        val experienceAnalysis = analyzeExperienceQuality(experiences)
        
        // Analyze Q-value behavior (for DQN)
        val qValueAnalysis = updateResult.qValueMean?.let { qMean ->
            updateResult.targetValueMean?.let { targetMean ->
                analyzeQValueBehavior(qMean, targetMean, beforeMetrics.qValueStats)
            }
        }
        
        // Generate debugging insights
        val insights = generateDebuggingInsights(
            lossAnalysis, gradientAnalysis, policyAnalysis, experienceAnalysis, qValueAnalysis
        )
        
        // Create analysis result
        val analysis = PolicyUpdateAnalysis(
            episodeNumber = episodeNumber,
            lossAnalysis = lossAnalysis,
            gradientAnalysis = gradientAnalysis,
            policyAnalysis = policyAnalysis,
            experienceAnalysis = experienceAnalysis,
            qValueAnalysis = qValueAnalysis,
            insights = insights,
            timestamp = getCurrentTimeMillis()
        )
        
        // Record for history
        recordPolicyAnalysis(analysis)
        
        return analysis
    }
    
    /**
     * Debug training convergence issues
     */
    fun debugConvergenceIssues(
        trainingHistory: List<RLMetrics>,
        windowSize: Int = config.convergenceWindowSize
    ): ConvergenceDebugAnalysis {
        
        if (trainingHistory.size < windowSize) {
            return ConvergenceDebugAnalysis(
                issue = ConvergenceIssue.INSUFFICIENT_DATA,
                analysis = "Need at least $windowSize episodes for convergence debugging",
                recommendations = listOf("Continue training to gather more data"),
                diagnostics = emptyMap()
            )
        }
        
        val recentHistory = trainingHistory.takeLast(windowSize)
        val rewards = recentHistory.map { it.averageReward }
        val losses = recentHistory.map { it.policyLoss }
        val entropies = recentHistory.map { it.policyEntropy }
        val explorationRates = recentHistory.map { it.explorationRate }
        
        // Analyze different convergence issues
        val rewardStagnation = analyzeRewardStagnation(rewards)
        val lossOscillation = analyzeLossOscillation(losses)
        val entropyCollapse = analyzeEntropyCollapse(entropies)
        val explorationDecay = analyzeExplorationDecay(explorationRates, rewards)
        
        // Determine primary issue
        val primaryIssue = determinePrimaryConvergenceIssue(
            rewardStagnation, lossOscillation, entropyCollapse, explorationDecay
        )
        
        // Generate detailed analysis
        val analysis = generateConvergenceAnalysis(primaryIssue, recentHistory)
        
        // Generate recommendations
        val recommendations = generateConvergenceRecommendations(primaryIssue, recentHistory)
        
        // Collect diagnostics
        val diagnostics = mapOf(
            "reward_variance" to calculateVariance(rewards),
            "loss_variance" to calculateVariance(losses),
            "entropy_trend" to calculateTrend(entropies),
            "exploration_trend" to calculateTrend(explorationRates),
            "reward_trend" to calculateTrend(rewards),
            "loss_trend" to calculateTrend(losses)
        )
        
        return ConvergenceDebugAnalysis(
            issue = primaryIssue,
            analysis = analysis,
            recommendations = recommendations,
            diagnostics = diagnostics
        )
    }
    
    /**
     * Debug specific training episode
     */
    fun debugEpisode(
        episodeMetrics: RLMetrics,
        experiences: List<Experience<DoubleArray, Int>>,
        policyUpdate: PolicyUpdateResult?
    ): EpisodeDebugAnalysis {
        
        // Analyze episode characteristics
        val episodeLength = experiences.size
        val totalReward = experiences.sumOf { it.reward }
        val rewardDistribution = analyzeRewardDistribution(experiences)
        val stateActionAnalysis = analyzeStateActionPatterns(experiences)
        
        // Analyze episode quality
        val qualityIssues = mutableListOf<String>()
        
        if (episodeLength < config.minEpisodeLengthThreshold) {
            qualityIssues.add("Episode too short ($episodeLength steps)")
        }
        
        if (episodeLength > config.maxEpisodeLengthThreshold) {
            qualityIssues.add("Episode too long ($episodeLength steps)")
        }
        
        if (abs(totalReward) < config.minRewardThreshold) {
            qualityIssues.add("Very low total reward ($totalReward)")
        }
        
        if (rewardDistribution.sparsity > config.maxRewardSparsityThreshold) {
            qualityIssues.add("Reward signal too sparse")
        }
        
        // Analyze policy update (if available)
        val policyUpdateAnalysis = policyUpdate?.let { update ->
            mapOf(
                "loss_magnitude" to update.loss,
                "gradient_norm" to update.gradientNorm,
                "policy_entropy" to update.policyEntropy,
                "update_quality" to assessUpdateQuality(update)
            )
        } ?: emptyMap()
        
        return EpisodeDebugAnalysis(
            episodeNumber = episodeMetrics.episode,
            episodeLength = episodeLength,
            totalReward = totalReward,
            rewardDistribution = rewardDistribution,
            stateActionAnalysis = stateActionAnalysis,
            qualityIssues = qualityIssues,
            policyUpdateAnalysis = policyUpdateAnalysis,
            recommendations = generateEpisodeRecommendations(qualityIssues, rewardDistribution)
        )
    }
    
    /**
     * Generate training diagnostic report
     */
    @Suppress("UNUSED_PARAMETER")
    fun generateDiagnosticReport(
        trainingHistory: List<RLMetrics>,
        recentEpisodes: List<Experience<DoubleArray, Int>>
    ): TrainingDiagnosticReport {
        
        val overallHealth = assessTrainingHealth(trainingHistory)
        val commonIssues = identifyCommonIssues(trainingHistory)
        val performanceMetrics = calculatePerformanceMetrics(trainingHistory)
        val stabilityAnalysis = analyzeTrainingStability(trainingHistory)
        val recommendations = generateOverallRecommendations(overallHealth, commonIssues, stabilityAnalysis)
        
        return TrainingDiagnosticReport(
            overallHealth = overallHealth,
            commonIssues = commonIssues,
            performanceMetrics = performanceMetrics,
            stabilityAnalysis = stabilityAnalysis,
            recommendations = recommendations,
            generatedAt = getCurrentTimeMillis()
        )
    }
    
    /**
     * Clear debugging history
     */
    fun clearHistory() {
        debugHistory.clear()
        policyAnalysisHistory.clear()
    }
    
    // Private analysis methods
    
    private fun analyzeLossBehavior(
        updateResult: PolicyUpdateResult,
        beforeMetrics: RLMetrics,
        afterMetrics: RLMetrics
    ): LossAnalysis {
        
        val lossChange = afterMetrics.policyLoss - beforeMetrics.policyLoss
        val eps = 1e-9
        val lossDirection = when {
            lossChange > eps -> LossDirection.INCREASING
            lossChange < -eps -> LossDirection.DECREASING
            else -> LossDirection.STABLE
        }
        
        val lossStability = when {
            updateResult.loss.isNaN() || updateResult.loss.isInfinite() -> LossStability.UNSTABLE
            abs(lossChange) > beforeMetrics.policyLoss * 0.5 -> LossStability.VOLATILE
            abs(lossChange) < beforeMetrics.policyLoss * 0.1 -> LossStability.STABLE
            else -> LossStability.MODERATE
        }
        
        return LossAnalysis(
            currentLoss = updateResult.loss,
            lossChange = lossChange,
            lossDirection = lossDirection,
            lossStability = lossStability
        )
    }
    
    private fun analyzeGradientBehavior(updateResult: PolicyUpdateResult): GradientAnalysis {
        val gradientMagnitude = when {
            updateResult.gradientNorm > 10.0 -> GradientMagnitude.EXPLODING
            updateResult.gradientNorm > 5.0 -> GradientMagnitude.HIGH
            updateResult.gradientNorm > 0.1 -> GradientMagnitude.NORMAL
            updateResult.gradientNorm > 1e-6 -> GradientMagnitude.LOW
            else -> GradientMagnitude.VANISHING
        }
        
        val gradientHealth = when (gradientMagnitude) {
            GradientMagnitude.NORMAL -> GradientHealth.HEALTHY
            GradientMagnitude.HIGH, GradientMagnitude.LOW -> GradientHealth.CONCERNING
            GradientMagnitude.EXPLODING, GradientMagnitude.VANISHING -> GradientHealth.PROBLEMATIC
        }
        
        return GradientAnalysis(
            gradientNorm = updateResult.gradientNorm,
            gradientMagnitude = gradientMagnitude,
            gradientHealth = gradientHealth
        )
    }
    
    private fun analyzePolicyChanges(
        updateResult: PolicyUpdateResult,
        beforeMetrics: RLMetrics,
        afterMetrics: RLMetrics
    ): PolicyChangeAnalysis {
        
        val entropyChange = updateResult.policyEntropy - beforeMetrics.policyEntropy
        val explorationChange = afterMetrics.explorationRate - beforeMetrics.explorationRate
        
        val policyShift = when {
            abs(entropyChange) > config.significantEntropyChangeThreshold -> PolicyShift.SIGNIFICANT
            abs(entropyChange) > config.moderateEntropyChangeThreshold -> PolicyShift.MODERATE
            else -> PolicyShift.MINIMAL
        }
        
        return PolicyChangeAnalysis(
            entropyChange = entropyChange,
            explorationChange = explorationChange,
            policyShift = policyShift,
            currentEntropy = updateResult.policyEntropy
        )
    }
    
    private fun analyzeExperienceQuality(experiences: List<Experience<DoubleArray, Int>>): ExperienceQualityAnalysis {
        if (experiences.isEmpty()) {
            return ExperienceQualityAnalysis(0.0, 0.0, 0.0, ExperienceQuality.POOR)
        }
        
        val rewards = experiences.map { it.reward }
        val rewardVariance = calculateVariance(rewards)
        val rewardSparsity = rewards.count { it == 0.0 }.toDouble() / rewards.size
        val terminalRatio = experiences.count { it.done }.toDouble() / experiences.size
        
        val quality = when {
            // Very sparse or nearly no variation in rewards indicates poor-quality signal
            rewardSparsity > 0.9 -> ExperienceQuality.POOR
            rewardVariance < 0.01 && rewardSparsity > 0.2 -> ExperienceQuality.POOR
            rewardSparsity > 0.7 -> ExperienceQuality.LOW
            rewardVariance > 1.0 && terminalRatio > 0.1 -> ExperienceQuality.GOOD
            else -> ExperienceQuality.MODERATE
        }
        
        return ExperienceQualityAnalysis(
            rewardVariance = rewardVariance,
            rewardSparsity = rewardSparsity,
            terminalRatio = terminalRatio,
            quality = quality
        )
    }
    
    private fun analyzeQValueBehavior(
        qMean: Double,
        targetMean: Double,
        qValueStats: QValueStats?
    ): QValueAnalysis {
        
        val qTargetDivergence = abs(qMean - targetMean)
        val overestimationBias = qMean - targetMean
        
        val qValueHealth = when {
            qMean > 100.0 -> QValueHealth.OVERESTIMATED
            qMean < -100.0 -> QValueHealth.UNDERESTIMATED
            qTargetDivergence > 50.0 -> QValueHealth.DIVERGENT
            else -> QValueHealth.NORMAL
        }
        
        return QValueAnalysis(
            meanQValue = qMean,
            targetMean = targetMean,
            qTargetDivergence = qTargetDivergence,
            overestimationBias = overestimationBias,
            qValueHealth = qValueHealth,
            qValueStats = qValueStats
        )
    }
    
    private fun generateDebuggingInsights(
        lossAnalysis: LossAnalysis,
        gradientAnalysis: GradientAnalysis,
        policyAnalysis: PolicyChangeAnalysis,
        experienceAnalysis: ExperienceQualityAnalysis,
        qValueAnalysis: QValueAnalysis?
    ): List<String> {
        
        val insights = mutableListOf<String>()
        
        // Loss insights
        when (lossAnalysis.lossStability) {
            LossStability.UNSTABLE -> insights.add("Loss is numerically unstable - check learning rate")
            LossStability.VOLATILE -> insights.add("Loss is highly volatile - consider gradient clipping")
            LossStability.STABLE -> insights.add("Loss is stable")
            LossStability.MODERATE -> insights.add("Loss shows moderate fluctuations")
        }
        
        // Gradient insights
        when (gradientAnalysis.gradientHealth) {
            GradientHealth.PROBLEMATIC -> insights.add("Gradient issues detected - immediate attention needed")
            GradientHealth.CONCERNING -> insights.add("Gradient behavior is concerning - monitor closely")
            GradientHealth.HEALTHY -> insights.add("Gradient behavior is healthy")
        }
        
        // Policy insights
        when (policyAnalysis.policyShift) {
            PolicyShift.SIGNIFICANT -> insights.add("Policy is changing significantly - may indicate instability")
            PolicyShift.MODERATE -> insights.add("Policy is evolving at moderate pace")
            PolicyShift.MINIMAL -> insights.add("Policy changes are minimal - may indicate convergence or stagnation")
        }
        
        // Experience insights
        when (experienceAnalysis.quality) {
            ExperienceQuality.POOR -> insights.add("Experience quality is poor - check environment and rewards")
            ExperienceQuality.LOW -> insights.add("Experience quality is low - consider improving exploration")
            ExperienceQuality.MODERATE -> insights.add("Experience quality is moderate")
            ExperienceQuality.GOOD -> insights.add("Experience quality is good")
        }
        
        // Q-value insights
        qValueAnalysis?.let { qAnalysis ->
            when (qAnalysis.qValueHealth) {
                QValueHealth.OVERESTIMATED -> insights.add("Q-values are overestimated - consider double DQN")
                QValueHealth.UNDERESTIMATED -> insights.add("Q-values are underestimated - check reward scaling")
                QValueHealth.DIVERGENT -> insights.add("Q-values diverging from targets - update target network more frequently")
                QValueHealth.NORMAL -> insights.add("Q-value behavior is normal")
            }
        }
        
        return insights
    }
    
    private fun analyzeRewardStagnation(rewards: List<Double>): Boolean {
        val recentVariance = calculateVariance(rewards.takeLast(10))
        return recentVariance < config.rewardStagnationThreshold
    }
    
    private fun analyzeLossOscillation(losses: List<Double>): Boolean {
        if (losses.size < 4) return false
        
        val oscillations = (1 until losses.size - 1).count { i ->
            val prev = losses[i - 1]
            val curr = losses[i]
            val next = losses[i + 1]
            (curr > prev && curr > next) || (curr < prev && curr < next)
        }
        
        return oscillations.toDouble() / (losses.size - 2) > config.lossOscillationThreshold
    }
    
    private fun analyzeEntropyCollapse(entropies: List<Double>): Boolean {
        return entropies.lastOrNull()?.let { it < config.entropyCollapseThreshold } ?: false
    }
    
    private fun analyzeExplorationDecay(explorationRates: List<Double>, rewards: List<Double>): Boolean {
        val explorationTrend = calculateTrend(explorationRates)
        val rewardTrend = calculateTrend(rewards)
        return explorationTrend < -config.explorationDecayThreshold && rewardTrend < 0
    }
    
    private fun determinePrimaryConvergenceIssue(
        rewardStagnation: Boolean,
        lossOscillation: Boolean,
        entropyCollapse: Boolean,
        explorationDecay: Boolean
    ): ConvergenceIssue {
        return when {
            entropyCollapse -> ConvergenceIssue.POLICY_COLLAPSE
            lossOscillation -> ConvergenceIssue.TRAINING_INSTABILITY
            explorationDecay -> ConvergenceIssue.INSUFFICIENT_EXPLORATION
            rewardStagnation -> ConvergenceIssue.REWARD_STAGNATION
            else -> ConvergenceIssue.NORMAL_CONVERGENCE
        }
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun generateConvergenceAnalysis(issue: ConvergenceIssue, history: List<RLMetrics>): String {
        return when (issue) {
            ConvergenceIssue.POLICY_COLLAPSE -> "Policy entropy has collapsed, indicating over-exploitation"
            ConvergenceIssue.TRAINING_INSTABILITY -> "Training shows high instability with oscillating loss"
            ConvergenceIssue.INSUFFICIENT_EXPLORATION -> "Exploration has decayed too quickly relative to learning progress"
            ConvergenceIssue.REWARD_STAGNATION -> "Rewards have stagnated without clear improvement"
            ConvergenceIssue.NORMAL_CONVERGENCE -> "Training appears to be converging normally"
            ConvergenceIssue.INSUFFICIENT_DATA -> "Insufficient data for convergence analysis"
        }
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun generateConvergenceRecommendations(issue: ConvergenceIssue, history: List<RLMetrics>): List<String> {
        return when (issue) {
            ConvergenceIssue.POLICY_COLLAPSE -> listOf(
                "Increase exploration rate",
                "Add entropy regularization",
                "Use different exploration strategy"
            )
            ConvergenceIssue.TRAINING_INSTABILITY -> listOf(
                "Reduce learning rate",
                "Apply gradient clipping",
                "Increase batch size"
            )
            ConvergenceIssue.INSUFFICIENT_EXPLORATION -> listOf(
                "Slow down exploration decay",
                "Use curiosity-driven exploration",
                "Increase exploration bonus"
            )
            ConvergenceIssue.REWARD_STAGNATION -> listOf(
                "Check reward function design",
                "Increase exploration",
                "Try curriculum learning"
            )
            ConvergenceIssue.NORMAL_CONVERGENCE -> listOf(
                "Training is converging normally; continue current approach"
            )
            ConvergenceIssue.INSUFFICIENT_DATA -> listOf(
                "Continue training to gather more data"
            )
        }
    }
    
    private fun analyzeRewardDistribution(experiences: List<Experience<DoubleArray, Int>>): RewardDistribution {
        val rewards = experiences.map { it.reward }
        val nonZeroRewards = rewards.filter { it != 0.0 }
        
        return RewardDistribution(
            mean = rewards.average(),
            variance = calculateVariance(rewards),
            sparsity = (rewards.size - nonZeroRewards.size).toDouble() / rewards.size,
            range = (rewards.maxOrNull() ?: 0.0) - (rewards.minOrNull() ?: 0.0)
        )
    }
    
    private fun analyzeStateActionPatterns(experiences: List<Experience<DoubleArray, Int>>): StateActionAnalysis {
        val actions = experiences.map { it.action }
        val uniqueActions = actions.toSet()
        val actionFrequency = actions.groupBy { it }.mapValues { it.value.size }
        
        return StateActionAnalysis(
            uniqueActions = uniqueActions.size,
            actionDiversity = uniqueActions.size.toDouble() / actions.size,
            mostFrequentAction = actionFrequency.maxByOrNull { it.value }?.key ?: -1,
            actionDistribution = actionFrequency
        )
    }
    
    private fun assessUpdateQuality(update: PolicyUpdateResult): Double {
        var quality = 1.0
        
        if (update.loss.isNaN() || update.loss.isInfinite()) quality -= 0.5
        if (update.gradientNorm > 10.0) quality -= 0.3
        if (update.gradientNorm < 1e-6) quality -= 0.2
        if (update.policyEntropy < 0.1) quality -= 0.3
        
        return quality.coerceIn(0.0, 1.0)
    }
    
    private fun generateEpisodeRecommendations(
        qualityIssues: List<String>,
        rewardDistribution: RewardDistribution
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (qualityIssues.any { it.contains("too short") }) {
            recommendations.add("Investigate why episodes are terminating early")
        }
        
        if (qualityIssues.any { it.contains("too long") }) {
            recommendations.add("Consider adding time limits or improving termination conditions")
        }
        
        if (rewardDistribution.sparsity > 0.8) {
            recommendations.add("Reward signal is too sparse - consider reward shaping")
        }
        
        return recommendations
    }
    
    private fun assessTrainingHealth(trainingHistory: List<RLMetrics>): TrainingHealth {
        if (trainingHistory.size < 10) return TrainingHealth.INSUFFICIENT_DATA
        
        val recentMetrics = trainingHistory.takeLast(10)
        val rewardTrend = calculateTrend(recentMetrics.map { it.averageReward })
        val lossStability = 1.0 - calculateVariance(recentMetrics.map { it.policyLoss })
        val entropyLevel = recentMetrics.map { it.policyEntropy }.average()
        
        return when {
            rewardTrend > 0.01 && lossStability > 0.7 && entropyLevel > 0.3 -> TrainingHealth.EXCELLENT
            rewardTrend > 0.005 && lossStability > 0.5 -> TrainingHealth.GOOD
            rewardTrend > -0.005 && lossStability > 0.3 -> TrainingHealth.FAIR
            else -> TrainingHealth.POOR
        }
    }
    
    private fun identifyCommonIssues(trainingHistory: List<RLMetrics>): List<String> {
        val issues = mutableListOf<String>()
        
        val recentMetrics = trainingHistory.takeLast(20)
        if (recentMetrics.any { it.gradientNorm > 10.0 }) {
            issues.add("Exploding gradients detected")
        }
        
        if (recentMetrics.any { it.policyEntropy < 0.1 }) {
            issues.add("Policy collapse detected")
        }
        
        if (calculateVariance(recentMetrics.map { it.policyLoss }) > 10.0) {
            issues.add("High loss variance")
        }
        
        return issues
    }
    
    private fun calculatePerformanceMetrics(trainingHistory: List<RLMetrics>): Map<String, Double> {
        if (trainingHistory.isEmpty()) return emptyMap()
        
        val rewards = trainingHistory.map { it.averageReward }
        val losses = trainingHistory.map { it.policyLoss }
        
        return mapOf(
            "average_reward" to rewards.average(),
            "reward_trend" to calculateTrend(rewards),
            "average_loss" to losses.average(),
            "loss_stability" to (1.0 - calculateVariance(losses)),
            "training_efficiency" to (rewards.lastOrNull() ?: 0.0) / trainingHistory.size
        )
    }
    
    private fun analyzeTrainingStability(trainingHistory: List<RLMetrics>): StabilityAnalysis {
        if (trainingHistory.size < 10) {
            return StabilityAnalysis(0.0, 0.0, 0.0, StabilityLevel.UNKNOWN)
        }
        
        val rewards = trainingHistory.map { it.averageReward }
        val losses = trainingHistory.map { it.policyLoss }
        val entropies = trainingHistory.map { it.policyEntropy }
        
        val rewardStability = 1.0 - calculateVariance(rewards) / (rewards.average().absoluteValue + 1e-8)
        val lossStability = 1.0 - calculateVariance(losses) / (losses.average().absoluteValue + 1e-8)
        val entropyStability = 1.0 - calculateVariance(entropies) / (entropies.average().absoluteValue + 1e-8)
        
        val overallStability = (rewardStability + lossStability + entropyStability) / 3.0
        
        val stabilityLevel = when {
            overallStability > 0.8 -> StabilityLevel.HIGH
            overallStability > 0.6 -> StabilityLevel.MODERATE
            overallStability > 0.4 -> StabilityLevel.LOW
            else -> StabilityLevel.VERY_LOW
        }
        
        return StabilityAnalysis(rewardStability, lossStability, entropyStability, stabilityLevel)
    }
    
    private fun generateOverallRecommendations(
        health: TrainingHealth,
        issues: List<String>,
        stability: StabilityAnalysis
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (health) {
            TrainingHealth.EXCELLENT -> recommendations.add("Training is performing excellently")
            TrainingHealth.GOOD -> recommendations.add("Training is performing well")
            TrainingHealth.FAIR -> recommendations.add("Training performance is fair - monitor closely")
            TrainingHealth.POOR -> recommendations.add("Training performance is poor - intervention needed")
            TrainingHealth.INSUFFICIENT_DATA -> recommendations.add("Continue training to assess health")
        }
        
        if (stability.stabilityLevel == StabilityLevel.LOW || stability.stabilityLevel == StabilityLevel.VERY_LOW) {
            recommendations.add("Training is unstable - reduce learning rate or increase batch size")
        }
        
        issues.forEach { issue ->
            when {
                issue.contains("exploding") -> recommendations.add("Apply gradient clipping")
                issue.contains("collapse") -> recommendations.add("Increase exploration rate")
                issue.contains("variance") -> recommendations.add("Stabilize training with smaller learning rate")
            }
        }
        
        return recommendations
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
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
    
    private fun recordPolicyAnalysis(analysis: PolicyUpdateAnalysis) {
        policyAnalysisHistory.add(PolicyAnalysisSnapshot(
            timestamp = getCurrentTimeMillis(),
            analysis = analysis
        ))
        
        // Limit history size
        if (policyAnalysisHistory.size > config.maxHistorySize) {
            policyAnalysisHistory.removeAt(0)
        }
    }
}

/**
 * Configuration for training debugger
 */
data class DebuggerConfig(
    val convergenceWindowSize: Int = 50,
    val significantLossChangeThreshold: Double = 1.0,
    val significantEntropyChangeThreshold: Double = 0.5,
    val moderateEntropyChangeThreshold: Double = 0.1,
    val minEpisodeLengthThreshold: Int = 5,
    val maxEpisodeLengthThreshold: Int = 500,
    val minRewardThreshold: Double = 0.01,
    val maxRewardSparsityThreshold: Double = 0.9,
    val rewardStagnationThreshold: Double = 0.001,
    val lossOscillationThreshold: Double = 0.3,
    val entropyCollapseThreshold: Double = 0.1,
    val explorationDecayThreshold: Double = 0.01,
    val maxHistorySize: Int = 1000
)

// Analysis result data classes

data class PolicyUpdateAnalysis(
    val episodeNumber: Int,
    val lossAnalysis: LossAnalysis,
    val gradientAnalysis: GradientAnalysis,
    val policyAnalysis: PolicyChangeAnalysis,
    val experienceAnalysis: ExperienceQualityAnalysis,
    val qValueAnalysis: QValueAnalysis?,
    val insights: List<String>,
    val timestamp: Long
)

data class LossAnalysis(
    val currentLoss: Double,
    val lossChange: Double,
    val lossDirection: LossDirection,
    val lossStability: LossStability
)

data class GradientAnalysis(
    val gradientNorm: Double,
    val gradientMagnitude: GradientMagnitude,
    val gradientHealth: GradientHealth
)

data class PolicyChangeAnalysis(
    val entropyChange: Double,
    val explorationChange: Double,
    val policyShift: PolicyShift,
    val currentEntropy: Double
)

data class ExperienceQualityAnalysis(
    val rewardVariance: Double,
    val rewardSparsity: Double,
    val terminalRatio: Double,
    val quality: ExperienceQuality
)

data class QValueAnalysis(
    val meanQValue: Double,
    val targetMean: Double,
    val qTargetDivergence: Double,
    val overestimationBias: Double,
    val qValueHealth: QValueHealth,
    val qValueStats: QValueStats?
)

data class ConvergenceDebugAnalysis(
    val issue: ConvergenceIssue,
    val analysis: String,
    val recommendations: List<String>,
    val diagnostics: Map<String, Double>
)

data class EpisodeDebugAnalysis(
    val episodeNumber: Int,
    val episodeLength: Int,
    val totalReward: Double,
    val rewardDistribution: RewardDistribution,
    val stateActionAnalysis: StateActionAnalysis,
    val qualityIssues: List<String>,
    val policyUpdateAnalysis: Map<String, Double>,
    val recommendations: List<String>
)

data class TrainingDiagnosticReport(
    val overallHealth: TrainingHealth,
    val commonIssues: List<String>,
    val performanceMetrics: Map<String, Double>,
    val stabilityAnalysis: StabilityAnalysis,
    val recommendations: List<String>,
    val generatedAt: Long
)

data class RewardDistribution(
    val mean: Double,
    val variance: Double,
    val sparsity: Double,
    val range: Double
)

data class StateActionAnalysis(
    val uniqueActions: Int,
    val actionDiversity: Double,
    val mostFrequentAction: Int,
    val actionDistribution: Map<Int, Int>
)

data class StabilityAnalysis(
    val rewardStability: Double,
    val lossStability: Double,
    val entropyStability: Double,
    val stabilityLevel: StabilityLevel
)

data class DebugSnapshot(
    val timestamp: Long,
    val episodeNumber: Int,
    val debugData: Map<String, Any>
)

data class PolicyAnalysisSnapshot(
    val timestamp: Long,
    val analysis: PolicyUpdateAnalysis
)

// Enums for classification

enum class LossDirection { INCREASING, DECREASING, STABLE }
enum class LossStability { STABLE, MODERATE, VOLATILE, UNSTABLE }
enum class GradientMagnitude { VANISHING, LOW, NORMAL, HIGH, EXPLODING }
enum class GradientHealth { HEALTHY, CONCERNING, PROBLEMATIC }
enum class PolicyShift { MINIMAL, MODERATE, SIGNIFICANT }
enum class ExperienceQuality { POOR, LOW, MODERATE, GOOD }
enum class QValueHealth { NORMAL, OVERESTIMATED, UNDERESTIMATED, DIVERGENT }
enum class ConvergenceIssue { 
    NORMAL_CONVERGENCE, 
    REWARD_STAGNATION, 
    TRAINING_INSTABILITY, 
    POLICY_COLLAPSE, 
    INSUFFICIENT_EXPLORATION,
    INSUFFICIENT_DATA
}
enum class TrainingHealth { EXCELLENT, GOOD, FAIR, POOR, INSUFFICIENT_DATA }
enum class StabilityLevel { HIGH, MODERATE, LOW, VERY_LOW, UNKNOWN }
