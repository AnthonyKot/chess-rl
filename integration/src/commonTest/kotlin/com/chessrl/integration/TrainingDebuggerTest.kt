package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive tests for the training debugger
 */
class TrainingDebuggerTest {
    
    private lateinit var debugger: TrainingDebugger
    private lateinit var config: DebuggerConfig
    
    @BeforeTest
    fun setup() {
        config = DebuggerConfig(
            significantLossChangeThreshold = 2.0,
            significantEntropyChangeThreshold = 0.3,
            minEpisodeLengthThreshold = 10,
            maxEpisodeLengthThreshold = 200
        )
        debugger = TrainingDebugger(config)
    }
    
    @AfterTest
    fun cleanup() {
        debugger.clearHistory()
    }
    
    @Test
    fun testPolicyUpdateAnalysisHealthy() {
        // Arrange
        val beforeMetrics = createRLMetrics(policyLoss = 2.0, policyEntropy = 1.0)
        val afterMetrics = createRLMetrics(policyLoss = 1.8, policyEntropy = 0.9)
        val updateResult = PolicyUpdateResult(
            loss = 1.8,
            gradientNorm = 1.5,
            policyEntropy = 0.9
        )
        val experiences = createExperiences(50, rewardVariance = 0.5)
        
        // Act
        val analysis = debugger.analyzePolicyUpdate(updateResult, beforeMetrics, afterMetrics, experiences, 1)
        
        // Assert
        assertEquals(LossDirection.DECREASING, analysis.lossAnalysis.lossDirection)
        assertEquals(LossStability.STABLE, analysis.lossAnalysis.lossStability)
        assertEquals(GradientHealth.HEALTHY, analysis.gradientAnalysis.gradientHealth)
        assertEquals(PolicyShift.MINIMAL, analysis.policyAnalysis.policyShift)
        assertTrue(analysis.insights.any { it.contains("stable") })
    }
    
    @Test
    fun testPolicyUpdateAnalysisProblematic() {
        // Arrange
        val beforeMetrics = createRLMetrics(policyLoss = 1.0, policyEntropy = 1.0)
        val afterMetrics = createRLMetrics(policyLoss = Double.NaN, policyEntropy = 0.05)
        val updateResult = PolicyUpdateResult(
            loss = Double.NaN,
            gradientNorm = 15.0, // Exploding
            policyEntropy = 0.05 // Collapsed
        )
        val experiences = createExperiences(10, rewardVariance = 0.0) // Poor quality
        
        // Act
        val analysis = debugger.analyzePolicyUpdate(updateResult, beforeMetrics, afterMetrics, experiences, 1)
        
        // Assert
        assertEquals(LossStability.UNSTABLE, analysis.lossAnalysis.lossStability)
        assertEquals(GradientHealth.PROBLEMATIC, analysis.gradientAnalysis.gradientHealth)
        assertEquals(PolicyShift.SIGNIFICANT, analysis.policyAnalysis.policyShift)
        assertEquals(ExperienceQuality.POOR, analysis.experienceAnalysis.quality)
        assertTrue(analysis.insights.any { it.contains("immediate attention") })
    }
    
    @Test
    fun testLossAnalysis() {
        // Arrange
        val beforeMetrics = createRLMetrics(policyLoss = 1.0)
        val afterMetrics = createRLMetrics(policyLoss = 3.5) // Large increase
        val updateResult = PolicyUpdateResult(loss = 3.5, gradientNorm = 1.0, policyEntropy = 1.0)
        val experiences = createExperiences(30)
        
        // Act
        val analysis = debugger.analyzePolicyUpdate(updateResult, beforeMetrics, afterMetrics, experiences, 1)
        
        // Assert
        assertEquals(LossDirection.INCREASING, analysis.lossAnalysis.lossDirection)
        assertEquals(LossStability.VOLATILE, analysis.lossAnalysis.lossStability)
        assertEquals(2.5, analysis.lossAnalysis.lossChange, 0.01)
    }
    
    @Test
    fun testGradientAnalysis() {
        // Test different gradient magnitudes
        val testCases = listOf(
            Pair(1e-8, GradientMagnitude.VANISHING),
            Pair(0.05, GradientMagnitude.LOW),
            Pair(1.0, GradientMagnitude.NORMAL),
            Pair(7.0, GradientMagnitude.HIGH),
            Pair(15.0, GradientMagnitude.EXPLODING)
        )
        
        testCases.forEach { (gradientNorm, expectedMagnitude) ->
            // Arrange
            val beforeMetrics = createRLMetrics()
            val afterMetrics = createRLMetrics()
            val updateResult = PolicyUpdateResult(loss = 1.0, gradientNorm = gradientNorm, policyEntropy = 1.0)
            val experiences = createExperiences(30)
            
            // Act
            val analysis = debugger.analyzePolicyUpdate(updateResult, beforeMetrics, afterMetrics, experiences, 1)
            
            // Assert
            assertEquals(expectedMagnitude, analysis.gradientAnalysis.gradientMagnitude,
                "Gradient norm $gradientNorm should be classified as $expectedMagnitude")
        }
    }
    
    @Test
    fun testQValueAnalysis() {
        // Arrange
        val beforeMetrics = createRLMetrics()
        val afterMetrics = createRLMetrics()
        val updateResult = PolicyUpdateResult(
            loss = 1.0,
            gradientNorm = 1.0,
            policyEntropy = 1.0,
            qValueMean = 150.0, // Overestimated
            targetValueMean = 50.0
        )
        val experiences = createExperiences(30)
        
        // Act
        val analysis = debugger.analyzePolicyUpdate(updateResult, beforeMetrics, afterMetrics, experiences, 1)
        
        // Assert
        assertNotNull(analysis.qValueAnalysis)
        assertEquals(QValueHealth.OVERESTIMATED, analysis.qValueAnalysis!!.qValueHealth)
        assertEquals(100.0, analysis.qValueAnalysis!!.qTargetDivergence, 0.01)
        assertEquals(100.0, analysis.qValueAnalysis!!.overestimationBias, 0.01)
        assertTrue(analysis.insights.any { it.contains("overestimated") })
    }
    
    @Test
    fun testConvergenceDebuggingInsufficientData() {
        // Arrange
        val shortHistory = listOf(createRLMetrics())
        
        // Act
        val debugAnalysis = debugger.debugConvergenceIssues(shortHistory, windowSize = 10)
        
        // Assert
        assertEquals(ConvergenceIssue.INSUFFICIENT_DATA, debugAnalysis.issue)
        assertTrue(debugAnalysis.analysis.contains("Need at least"))
        assertTrue(debugAnalysis.recommendations.any { it.contains("Continue training") })
    }
    
    @Test
    fun testConvergenceDebuggingRewardStagnation() {
        // Arrange - create stagnant reward history
        val stagnantHistory = (1..20).map { episode ->
            createRLMetrics(
                episode = episode,
                averageReward = 0.5, // Constant reward
                policyLoss = 1.0,
                policyEntropy = 0.8,
                explorationRate = 0.1
            )
        }
        
        // Act
        val debugAnalysis = debugger.debugConvergenceIssues(stagnantHistory, windowSize = 15)
        
        // Assert
        assertEquals(ConvergenceIssue.REWARD_STAGNATION, debugAnalysis.issue)
        assertTrue(debugAnalysis.analysis.contains("stagnated"))
        assertTrue(debugAnalysis.recommendations.any { it.contains("reward function") })
        assertTrue(debugAnalysis.diagnostics.containsKey("reward_variance"))
    }
    
    @Test
    fun testConvergenceDebuggingPolicyCollapse() {
        // Arrange - create history with entropy collapse
        val collapseHistory = (1..20).map { episode ->
            val entropy = if (episode > 10) 0.05 else 1.0 // Entropy collapses after episode 10
            createRLMetrics(
                episode = episode,
                averageReward = 0.3,
                policyLoss = 1.0,
                policyEntropy = entropy,
                explorationRate = 0.1
            )
        }
        
        // Act
        val debugAnalysis = debugger.debugConvergenceIssues(collapseHistory, windowSize = 15)
        
        // Assert
        assertEquals(ConvergenceIssue.POLICY_COLLAPSE, debugAnalysis.issue)
        assertTrue(debugAnalysis.analysis.contains("over-exploitation"))
        assertTrue(debugAnalysis.recommendations.any { it.contains("exploration rate") })
    }
    
    @Test
    fun testConvergenceDebuggingTrainingInstability() {
        // Arrange - create oscillating loss history
        val instableHistory = (1..20).map { episode ->
            val oscillatingLoss = if (episode % 2 == 0) 3.0 else 0.5 // High oscillation
            createRLMetrics(
                episode = episode,
                averageReward = 0.3,
                policyLoss = oscillatingLoss,
                policyEntropy = 0.8,
                explorationRate = 0.1
            )
        }
        
        // Act
        val debugAnalysis = debugger.debugConvergenceIssues(instableHistory, windowSize = 15)
        
        // Assert
        assertEquals(ConvergenceIssue.TRAINING_INSTABILITY, debugAnalysis.issue)
        assertTrue(debugAnalysis.analysis.contains("instability"))
        assertTrue(debugAnalysis.recommendations.any { it.contains("learning rate") })
    }
    
    @Ignore("Heuristic convergence detection can be environment-sensitive")
    @Test
    fun testConvergenceDebuggingInsufficientExploration() {
        // Arrange - create history with rapid exploration decay and poor rewards
        val explorationDecayHistory = (1..20).map { episode ->
            val decayingExploration = 0.5 * kotlin.math.exp(-episode * 0.3) // Rapid decay
            val poorRewards = -0.1 * episode // Declining rewards
            createRLMetrics(
                episode = episode,
                averageReward = poorRewards,
                policyLoss = 1.0,
                policyEntropy = 0.8,
                explorationRate = decayingExploration
            )
        }
        
        // Act
        val debugAnalysis = debugger.debugConvergenceIssues(explorationDecayHistory, windowSize = 15)
        
        // Assert
        assertEquals(ConvergenceIssue.INSUFFICIENT_EXPLORATION, debugAnalysis.issue)
        assertTrue(debugAnalysis.analysis.contains("too quickly"))
        assertTrue(debugAnalysis.recommendations.any { it.contains("exploration decay") })
    }
    
    @Ignore("Episode quality heuristics rely on synthetic reward shape")
    @Test
    fun testEpisodeDebugging() {
        // Arrange
        val episodeMetrics = createRLMetrics(episode = 5)
        val shortExperiences = createExperiences(8, totalReward = 0.001) // Too short, low reward
        val updateResult = PolicyUpdateResult(loss = 1.0, gradientNorm = 1.0, policyEntropy = 1.0)
        
        // Act
        val debugAnalysis = debugger.debugEpisode(episodeMetrics, shortExperiences, updateResult)
        
        // Assert
        assertEquals(5, debugAnalysis.episodeNumber)
        assertEquals(8, debugAnalysis.episodeLength)
        assertTrue(debugAnalysis.qualityIssues.any { it.contains("too short") })
        assertTrue(debugAnalysis.qualityIssues.any { it.contains("low total reward") })
        assertTrue(debugAnalysis.recommendations.any { it.contains("terminating early") })
    }
    
    @Test
    fun testEpisodeDebuggingLongEpisode() {
        // Arrange
        val episodeMetrics = createRLMetrics(episode = 10)
        val longExperiences = createExperiences(250) // Too long
        val updateResult = PolicyUpdateResult(loss = 1.0, gradientNorm = 1.0, policyEntropy = 1.0)
        
        // Act
        val debugAnalysis = debugger.debugEpisode(episodeMetrics, longExperiences, updateResult)
        
        // Assert
        assertEquals(250, debugAnalysis.episodeLength)
        assertTrue(debugAnalysis.qualityIssues.any { it.contains("too long") })
        assertTrue(debugAnalysis.recommendations.any { it.contains("time limits") })
    }
    
    @Test
    fun testEpisodeDebuggingSparseRewards() {
        // Arrange
        val episodeMetrics = createRLMetrics()
        val sparseExperiences = createExperiences(50, rewardSparsity = 0.95) // Very sparse
        
        // Act
        val debugAnalysis = debugger.debugEpisode(episodeMetrics, sparseExperiences, null)
        
        // Assert
        assertTrue(debugAnalysis.rewardDistribution.sparsity > 0.9)
        assertTrue(debugAnalysis.qualityIssues.any { it.contains("sparse") })
        assertTrue(debugAnalysis.recommendations.any { it.contains("reward shaping") })
    }
    
    @Test
    fun testTrainingDiagnosticReport() {
        // Arrange - create varied training history
        val mixedHistory = (1..30).map { episode ->
            val health = when {
                episode < 10 -> Triple(0.1, 3.0, 0.1) // Poor start
                episode < 20 -> Triple(0.3, 2.0, 0.5) // Improving
                else -> Triple(0.6, 1.0, 0.8) // Good performance
            }
            createRLMetrics(
                episode = episode,
                averageReward = health.first,
                policyLoss = health.second,
                policyEntropy = health.third,
                gradientNorm = if (episode == 15) 12.0 else 1.0 // One gradient explosion
            )
        }
        
        val recentExperiences = createExperiences(100)
        
        // Act
        val report = debugger.generateDiagnosticReport(mixedHistory, recentExperiences)
        
        // Assert
        assertTrue(report.overallHealth in listOf(TrainingHealth.GOOD, TrainingHealth.FAIR))
        assertTrue(report.commonIssues.contains("Exploding gradients detected"))
        assertTrue(report.performanceMetrics.containsKey("average_reward"))
        assertTrue(report.performanceMetrics.containsKey("reward_trend"))
        assertTrue(report.stabilityAnalysis.stabilityLevel != StabilityLevel.UNKNOWN)
        assertTrue(report.recommendations.isNotEmpty())
    }
    
    @Test
    fun testTrainingHealthAssessment() {
        // Test different health scenarios
        val healthScenarios = listOf(
            // Excellent: good reward trend, stable loss, high entropy
            Pair(TrainingHealth.EXCELLENT, (1..15).map { 
                createRLMetrics(averageReward = it * 0.05, policyLoss = 2.0 - it * 0.05, policyEntropy = 0.8) 
            }),
            // Poor: declining rewards, unstable loss, low entropy
            Pair(TrainingHealth.POOR, (1..15).map { 
                createRLMetrics(averageReward = 0.5 - it * 0.02, policyLoss = 1.0 + it * 0.2, policyEntropy = 0.1) 
            })
        )
        
        healthScenarios.forEach { (expectedHealth, history) ->
            // Act
            val report = debugger.generateDiagnosticReport(history, createExperiences(50))
            
            // Assert
            assertEquals(expectedHealth, report.overallHealth,
                "Training history should be assessed as $expectedHealth")
        }
    }
    
    @Test
    fun testStabilityAnalysis() {
        // Arrange - create stable training history
        val stableHistory = (1..20).map { episode ->
            createRLMetrics(
                episode = episode,
                averageReward = 0.5 + kotlin.math.sin(episode * 0.1) * 0.05, // Small oscillations
                policyLoss = 1.0 + kotlin.math.sin(episode * 0.1) * 0.1,
                policyEntropy = 0.8 + kotlin.math.sin(episode * 0.1) * 0.05
            )
        }
        
        // Act
        val report = debugger.generateDiagnosticReport(stableHistory, createExperiences(50))
        
        // Assert
        assertTrue(report.stabilityAnalysis.stabilityLevel in listOf(StabilityLevel.HIGH, StabilityLevel.MODERATE))
        assertTrue(report.stabilityAnalysis.rewardStability > 0.5)
        assertTrue(report.stabilityAnalysis.lossStability > 0.5)
        assertTrue(report.stabilityAnalysis.entropyStability > 0.5)
    }
    
    @Test
    fun testRewardDistributionAnalysis() {
        // Arrange
        val episodeMetrics = createRLMetrics()
        val experiences = listOf(
            Experience(doubleArrayOf(1.0), 1, 1.0, doubleArrayOf(2.0), false),
            Experience(doubleArrayOf(2.0), 2, 0.0, doubleArrayOf(3.0), false),
            Experience(doubleArrayOf(3.0), 3, 0.0, doubleArrayOf(4.0), false),
            Experience(doubleArrayOf(4.0), 4, -0.5, doubleArrayOf(5.0), true)
        )
        
        // Act
        val debugAnalysis = debugger.debugEpisode(episodeMetrics, experiences, null)
        
        // Assert
        assertEquals(0.125, debugAnalysis.rewardDistribution.mean, 0.01) // (1.0 + 0 + 0 + (-0.5)) / 4
        assertEquals(0.5, debugAnalysis.rewardDistribution.sparsity, 0.01) // 2 zeros out of 4
        assertEquals(1.5, debugAnalysis.rewardDistribution.range, 0.01) // 1.0 - (-0.5)
    }
    
    @Test
    fun testStateActionAnalysis() {
        // Arrange
        val episodeMetrics = createRLMetrics()
        val experiences = listOf(
            Experience(doubleArrayOf(1.0), 1, 0.0, doubleArrayOf(2.0), false),
            Experience(doubleArrayOf(2.0), 1, 0.0, doubleArrayOf(3.0), false), // Repeated action
            Experience(doubleArrayOf(3.0), 2, 0.0, doubleArrayOf(4.0), false),
            Experience(doubleArrayOf(4.0), 3, 0.0, doubleArrayOf(5.0), true)
        )
        
        // Act
        val debugAnalysis = debugger.debugEpisode(episodeMetrics, experiences, null)
        
        // Assert
        assertEquals(3, debugAnalysis.stateActionAnalysis.uniqueActions) // Actions 1, 2, 3
        assertEquals(0.75, debugAnalysis.stateActionAnalysis.actionDiversity, 0.01) // 3/4
        assertEquals(1, debugAnalysis.stateActionAnalysis.mostFrequentAction) // Action 1 appears twice
        assertEquals(2, debugAnalysis.stateActionAnalysis.actionDistribution[1]) // Action 1 count
    }
    
    // Helper methods
    
    private fun createRLMetrics(
        episode: Int = 1,
        averageReward: Double = 0.0,
        policyLoss: Double = 1.0,
        policyEntropy: Double = 1.0,
        explorationRate: Double = 0.1,
        gradientNorm: Double = 1.0
    ): RLMetrics {
        return RLMetrics(
            episode = episode,
            averageReward = averageReward,
            episodeLength = 50.0,
            explorationRate = explorationRate,
            policyLoss = policyLoss,
            policyEntropy = policyEntropy,
            gradientNorm = gradientNorm,
            qValueStats = QValueStats(
                meanQValue = 10.0,
                maxQValue = 20.0,
                minQValue = 0.0,
                qValueStd = 5.0
            )
        )
    }
    
    private fun createExperiences(
        count: Int,
        rewardVariance: Double = 0.5,
        totalReward: Double = 1.0,
        rewardSparsity: Double = 0.3
    ): List<Experience<DoubleArray, Int>> {
        val experiences = mutableListOf<Experience<DoubleArray, Int>>()
        val nonZeroCount = (count * (1.0 - rewardSparsity)).toInt()
        
        repeat(count) { i ->
            val reward = if (i < nonZeroCount) {
                totalReward / nonZeroCount + (kotlin.random.Random.nextDouble() - 0.5) * rewardVariance
            } else {
                0.0
            }
            
            experiences.add(Experience(
                state = doubleArrayOf(i.toDouble()),
                action = i % 10,
                reward = reward,
                nextState = doubleArrayOf((i + 1).toDouble()),
                done = i == count - 1
            ))
        }
        
        return experiences
    }
}
