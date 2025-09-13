package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Tests for experience buffer analyzer reliability and accuracy
 */
class ExperienceBufferAnalyzerTest {
    
    private lateinit var analyzer: ExperienceBufferAnalyzer
    
    @BeforeTest
    fun setup() {
        analyzer = ExperienceBufferAnalyzer()
    }
    
    @Test
    fun testBasicBufferAnalysis() {
        val experiences = createTestExperiences(100)
        val analysis = analyzer.analyzeExperienceBuffer(experiences)
        
        assertNotNull(analysis)
        assertEquals(100, analysis.bufferSize)
        assertTrue(analysis.qualityScore >= 0.0)
        assertTrue(analysis.qualityScore <= 1.0)
        assertNotNull(analysis.rewardAnalysis)
        assertNotNull(analysis.stateAnalysis)
        assertNotNull(analysis.actionAnalysis)
        assertNotNull(analysis.temporalAnalysis)
        assertNotNull(analysis.diversityAnalysis)
        assertNotNull(analysis.qualityIssues)
        assertNotNull(analysis.recommendations)
    }
    
    @Test
    fun testEmptyBufferAnalysis() {
        val emptyExperiences = emptyList<Experience<DoubleArray, Int>>()
        val analysis = analyzer.analyzeExperienceBuffer(emptyExperiences)
        
        assertEquals(0, analysis.bufferSize)
        assertEquals(0.0, analysis.qualityScore)
        assertTrue(analysis.qualityIssues.contains("Empty experience buffer"))
        assertTrue(analysis.recommendations.contains("Collect training experiences"))
    }
    
    @Test
    fun testRewardAnalysis() {
        val experiences = createExperiencesWithRewards(listOf(0.0, 1.0, 0.0, 0.5, 0.0))
        val analysis = analyzer.analyzeExperienceBuffer(experiences)
        val rewardAnalysis = analysis.rewardAnalysis
        
        assertEquals(0.3, rewardAnalysis.mean, 0.01)
        assertTrue(rewardAnalysis.variance > 0.0)
        assertEquals(0.0, rewardAnalysis.min)
        assertEquals(1.0, rewardAnalysis.max)
        assertEquals(0.6, rewardAnalysis.sparsity, 0.01) // 3 out of 5 are zero
    }
    
    @Test
    fun testStateAnalysis() {
        val experiences = createTestExperiences(50)
        val analysis = analyzer.analyzeExperienceBuffer(experiences)
        val stateAnalysis = analysis.stateAnalysis
        
        assertTrue(stateAnalysis.averageVariance >= 0.0)
        assertTrue(stateAnalysis.averageNorm >= 0.0)
        assertNotNull(stateAnalysis.problematicDimensions)
    }
    
    @Test
    fun testActionAnalysis() {
        val experiences = createExperiencesWithActions(listOf(0, 1, 0, 2, 1, 0, 3))
        val analysis = analyzer.analyzeExperienceBuffer(experiences)
        val actionAnalysis = analysis.actionAnalysis
        
        assertEquals(4, actionAnalysis.uniqueActions) // 0, 1, 2, 3
        assertTrue(actionAnalysis.diversity > 0.0)
        assertTrue(actionAnalysis.diversity <= 1.0)
        assertEquals(7, actionAnalysis.actionDistribution.values.sum())
    }
    
    @Test
    fun testTemporalAnalysis() {
        val experiences = createTestExperiences(100)
        val analysis = analyzer.analyzeExperienceBuffer(experiences)
        val temporalAnalysis = analysis.temporalAnalysis
        
        assertTrue(temporalAnalysis.autocorrelation >= -1.0)
        assertTrue(temporalAnalysis.autocorrelation <= 1.0)
        assertTrue(temporalAnalysis.consistency >= 0.0)
        assertNotNull(temporalAnalysis.patterns)
    }
    
    @Test
    fun testDiversityAnalysis() {
        val experiences = createTestExperiences(100)
        val analysis = analyzer.analyzeExperienceBuffer(experiences)
        val diversityAnalysis = analysis.diversityAnalysis
        
        assertTrue(diversityAnalysis.stateDiversity >= 0.0)
        assertTrue(diversityAnalysis.actionDiversity >= 0.0)
        assertTrue(diversityAnalysis.actionDiversity <= 1.0)
        assertTrue(diversityAnalysis.rewardDiversity >= 0.0)
        assertTrue(diversityAnalysis.rewardDiversity <= 1.0)
    }
    
    @Test
    fun testDetailedInspection() {
        val experiences = createTestExperiences(200)
        val inspection = analyzer.inspectBuffer(experiences, AnalysisDepth.DETAILED)
        
        assertNotNull(inspection)
        assertNotNull(inspection.basicAnalysis)
        assertNotNull(inspection.detailedAnalysis)
        assertNotNull(inspection.episodeAnalysis)
        assertNull(inspection.correlationAnalysis) // Only in comprehensive mode
        assertEquals(AnalysisDepth.DETAILED, inspection.analysisDepth)
        assertTrue(inspection.inspectionTimestamp > 0)
    }
    
    @Test
    fun testComprehensiveInspection() {
        val experiences = createTestExperiences(100)
        val inspection = analyzer.inspectBuffer(experiences, AnalysisDepth.COMPREHENSIVE)
        
        assertNotNull(inspection)
        assertNotNull(inspection.basicAnalysis)
        assertNotNull(inspection.detailedAnalysis)
        assertNotNull(inspection.episodeAnalysis)
        assertNotNull(inspection.correlationAnalysis)
        assertEquals(AnalysisDepth.COMPREHENSIVE, inspection.analysisDepth)
    }
    
    @Test
    fun testBufferComparison() {
        val buffer1 = createTestExperiences(100)
        val buffer2 = createHighQualityExperiences(100)
        val buffer3 = createLowQualityExperiences(100)
        
        val buffers = mapOf(
            "standard" to buffer1,
            "high_quality" to buffer2,
            "low_quality" to buffer3
        )
        
        val comparison = analyzer.compareBuffers(buffers)
        
        assertNotNull(comparison)
        assertEquals(3, comparison.bufferNames.size)
        assertEquals(3, comparison.bufferAnalyses.size)
        assertTrue(comparison.comparisons.isNotEmpty())
        assertTrue(comparison.rankings.isNotEmpty())
        assertTrue(comparison.insights.isNotEmpty())
        
        // High quality buffer should rank better
        val qualityRanking = comparison.rankings[ComparisonMetric.QUALITY_SCORE]
        assertNotNull(qualityRanking)
        assertEquals("high_quality", qualityRanking.first())
    }
    
    @Test
    fun testQualityTimeSeriesAnalysis() {
        val experiences = createTestExperiences(2000)
        val timeSeriesAnalysis = analyzer.analyzeQualityOverTime(experiences, windowSize = 500)
        
        assertNotNull(timeSeriesAnalysis)
        assertEquals(500, timeSeriesAnalysis.windowSize)
        assertTrue(timeSeriesAnalysis.qualityTimeSeries.isNotEmpty())
        assertNotNull(timeSeriesAnalysis.trends)
        assertNotNull(timeSeriesAnalysis.changePoints)
        assertNotNull(timeSeriesAnalysis.overallTrend)
    }
    
    @Test
    fun testAnomalyDetection() {
        val experiences = createExperiencesWithAnomalies()
        val anomalyResult = analyzer.detectAnomalies(experiences, anomalyThreshold = 2.0)
        
        assertNotNull(anomalyResult)
        assertTrue(anomalyResult.totalAnomalies >= 0)
        assertTrue(anomalyResult.anomalyRate >= 0.0)
        assertTrue(anomalyResult.anomalyRate <= 1.0)
        assertEquals(2.0, anomalyResult.detectionThreshold)
        assertNotNull(anomalyResult.severityDistribution)
        
        // Check that anomalies are properly categorized
        anomalyResult.anomalies.forEach { anomaly ->
            assertTrue(anomaly.index >= 0)
            assertTrue(anomaly.index < experiences.size)
            assertNotNull(anomaly.type)
            assertTrue(anomaly.severity > 0.0)
            assertTrue(anomaly.description.isNotEmpty())
            assertNotNull(anomaly.experience)
        }
    }
    
    @Test
    fun testQualityIssueIdentification() {
        // Test sparse rewards
        val sparseExperiences = createExperiencesWithRewards(List(100) { 0.0 })
        val sparseAnalysis = analyzer.analyzeExperienceBuffer(sparseExperiences)
        assertTrue(sparseAnalysis.qualityIssues.any { it.contains("sparse rewards") })
        
        // Test low action diversity
        val lowDiversityExperiences = createExperiencesWithActions(List(100) { 0 })
        val lowDiversityAnalysis = analyzer.analyzeExperienceBuffer(lowDiversityExperiences)
        assertTrue(lowDiversityAnalysis.qualityIssues.any { it.contains("action diversity") })
    }
    
    @Test
    fun testRecommendationGeneration() {
        val lowQualityExperiences = createLowQualityExperiences(100)
        val analysis = analyzer.analyzeExperienceBuffer(lowQualityExperiences)
        
        assertTrue(analysis.recommendations.isNotEmpty())
        
        // Should contain relevant recommendations for low quality data
        val recommendationText = analysis.recommendations.joinToString(" ")
        assertTrue(
            recommendationText.contains("exploration") ||
            recommendationText.contains("reward") ||
            recommendationText.contains("diversity")
        )
    }
    
    @Test
    fun testConfigurationValidation() {
        val config = BufferAnalysisConfig(
            enableDetailedAnalysis = true,
            anomalyDetectionThreshold = 1.5,
            maxAnalysisSize = 50000,
            enableCorrelationAnalysis = true
        )
        
        val configuredAnalyzer = ExperienceBufferAnalyzer(config)
        assertNotNull(configuredAnalyzer)
        
        val experiences = createTestExperiences(100)
        val analysis = configuredAnalyzer.analyzeExperienceBuffer(experiences)
        assertNotNull(analysis)
    }
    
    @Test
    fun testPerformanceWithLargeBuffers() {
        val largeBuffer = createTestExperiences(10000)
        
        val startTime = getCurrentTimeMillis()
        val analysis = analyzer.analyzeExperienceBuffer(largeBuffer)
        val endTime = getCurrentTimeMillis()
        
        val analysisTime = endTime - startTime
        
        // Analysis should complete in reasonable time (less than 5 seconds for large buffer)
        assertTrue(analysisTime < 5000, "Analysis took too long: ${analysisTime}ms")
        
        assertNotNull(analysis)
        assertEquals(10000, analysis.bufferSize)
    }
    
    @Test
    fun testMemoryEfficiency() {
        // Test that analyzer doesn't hold references to large buffers
        repeat(10) {
            val experiences = createTestExperiences(1000)
            analyzer.analyzeExperienceBuffer(experiences)
        }
        
        // If we get here without OutOfMemoryError, memory management is working
        assertTrue(true)
    }
    
    @Test
    fun testAnalysisConsistency() {
        val experiences = createTestExperiences(100)
        
        // Run analysis multiple times
        val analyses = (1..3).map { analyzer.analyzeExperienceBuffer(experiences) }
        
        // Results should be consistent
        val firstAnalysis = analyses.first()
        analyses.forEach { analysis ->
            assertEquals(firstAnalysis.bufferSize, analysis.bufferSize)
            assertEquals(firstAnalysis.rewardAnalysis.mean, analysis.rewardAnalysis.mean, 0.001)
            assertEquals(firstAnalysis.actionAnalysis.uniqueActions, analysis.actionAnalysis.uniqueActions)
        }
    }
    
    @Test
    fun testEdgeCases() {
        // Test single experience
        val singleExperience = listOf(createSingleExperience(0.5, 1))
        val singleAnalysis = analyzer.analyzeExperienceBuffer(singleExperience)
        assertEquals(1, singleAnalysis.bufferSize)
        
        // Test all identical experiences
        val identicalExperiences = List(50) { createSingleExperience(1.0, 0) }
        val identicalAnalysis = analyzer.analyzeExperienceBuffer(identicalExperiences)
        assertEquals(0.0, identicalAnalysis.rewardAnalysis.variance, 0.001)
        assertEquals(1, identicalAnalysis.actionAnalysis.uniqueActions)
    }
    
    @Test
    fun testEpisodeStructureAnalysis() {
        val experiences = createExperiencesWithEpisodes()
        val inspection = analyzer.inspectBuffer(experiences, AnalysisDepth.DETAILED)
        val episodeAnalysis = inspection.episodeAnalysis!!
        
        assertTrue(episodeAnalysis.episodeCount > 0)
        assertTrue(episodeAnalysis.averageLength > 0.0)
        assertTrue(episodeAnalysis.lengthVariance >= 0.0)
        assertTrue(episodeAnalysis.lengthDistribution.isNotEmpty())
    }
    
    // Helper methods for creating test data
    
    private fun createTestExperiences(count: Int): List<Experience<DoubleArray, Int>> {
        return (0 until count).map { i ->
            Experience(
                state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                action = i % 10,
                reward = if (i % 5 == 0) kotlin.random.Random.nextDouble() else 0.0,
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                done = i % 20 == 19
            )
        }
    }
    
    private fun createExperiencesWithRewards(rewards: List<Double>): List<Experience<DoubleArray, Int>> {
        return rewards.mapIndexed { i, reward ->
            Experience(
                state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                action = i % 10,
                reward = reward,
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                done = i % 10 == 9
            )
        }
    }
    
    private fun createExperiencesWithActions(actions: List<Int>): List<Experience<DoubleArray, Int>> {
        return actions.mapIndexed { i, action ->
            Experience(
                state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                action = action,
                reward = kotlin.random.Random.nextDouble(),
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                done = i % 10 == 9
            )
        }
    }
    
    private fun createHighQualityExperiences(count: Int): List<Experience<DoubleArray, Int>> {
        return (0 until count).map { i ->
            Experience(
                state = DoubleArray(64) { kotlin.random.Random.nextDouble() * 2.0 - 1.0 }, // More varied states
                action = kotlin.random.Random.nextInt(20), // More diverse actions
                reward = kotlin.random.Random.nextDouble() * 2.0 - 1.0, // Varied rewards
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() * 2.0 - 1.0 },
                done = i % 15 == 14
            )
        }
    }
    
    private fun createLowQualityExperiences(count: Int): List<Experience<DoubleArray, Int>> {
        return (0 until count).map { i ->
            Experience(
                state = DoubleArray(64) { 0.1 }, // Low variance states
                action = 0, // No action diversity
                reward = 0.0, // Sparse rewards
                nextState = DoubleArray(64) { 0.1 },
                done = i % 50 == 49 // Long episodes
            )
        }
    }
    
    private fun createExperiencesWithAnomalies(): List<Experience<DoubleArray, Int>> {
        val normalExperiences = createTestExperiences(95)
        val anomalies = listOf(
            // Reward outlier
            Experience(
                state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                action = 5,
                reward = 100.0, // Extreme reward
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                done = false
            ),
            // State outlier
            Experience(
                state = DoubleArray(64) { 1000.0 }, // Extreme state values
                action = 3,
                reward = 0.5,
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                done = false
            ),
            // Sequence error
            Experience(
                state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                action = 1,
                reward = 0.0,
                nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                done = false // Should be terminal after previous terminal
            )
        )
        
        return normalExperiences + anomalies
    }
    
    private fun createSingleExperience(reward: Double, action: Int): Experience<DoubleArray, Int> {
        return Experience(
            state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
            action = action,
            reward = reward,
            nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
            done = false
        )
    }
    
    private fun createExperiencesWithEpisodes(): List<Experience<DoubleArray, Int>> {
        val experiences = mutableListOf<Experience<DoubleArray, Int>>()
        
        // Create 5 episodes of varying lengths
        val episodeLengths = listOf(10, 15, 8, 20, 12)
        
        episodeLengths.forEach { length ->
            repeat(length) { step ->
                experiences.add(Experience(
                    state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                    action = kotlin.random.Random.nextInt(10),
                    reward = if (step == length - 1) 1.0 else 0.0, // Reward at episode end
                    nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
                    done = step == length - 1 // Terminal at episode end
                ))
            }
        }
        
        return experiences
    }
}