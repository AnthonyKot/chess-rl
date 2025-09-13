package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Experience buffer inspection and quality analysis component for debugging
 * training data quality and identifying potential issues in the experience collection process.
 */
class ExperienceBufferAnalyzer(
    private val config: BufferAnalysisConfig = BufferAnalysisConfig()
) {
    
    /**
     * Comprehensive analysis of experience buffer
     */
    fun analyzeExperienceBuffer(
        experiences: List<Experience<DoubleArray, Int>>
    ): ExperienceBufferAnalysis {
        
        if (experiences.isEmpty()) {
            return ExperienceBufferAnalysis(
                bufferSize = 0,
                qualityScore = 0.0,
                rewardAnalysis = RewardAnalysis(0.0, 0.0, 0.0, 0.0, 0.0),
                stateAnalysis = StateAnalysis(0.0, 0.0, emptyList()),
                actionAnalysis = ActionAnalysis(0, 0.0, emptyMap()),
                temporalAnalysis = TemporalAnalysis(0.0, 0.0, emptyList()),
                diversityAnalysis = DiversityAnalysis(0.0, 0.0, 0.0),
                qualityIssues = listOf("Empty experience buffer"),
                recommendations = listOf("Collect training experiences")
            )
        }
        
        println("🔍 Analyzing Experience Buffer")
        println("Buffer size: ${experiences.size} experiences")
        println("=" * 50)
        
        // Analyze different aspects of the experience buffer
        val rewardAnalysis = analyzeRewards(experiences)
        val stateAnalysis = analyzeStates(experiences)
        val actionAnalysis = analyzeActions(experiences)
        val temporalAnalysis = analyzeTemporalPatterns(experiences)
        val diversityAnalysis = analyzeDiversity(experiences)
        
        // Calculate overall quality score
        val qualityScore = calculateOverallQuality(
            rewardAnalysis, stateAnalysis, actionAnalysis, temporalAnalysis, diversityAnalysis
        )
        
        // Identify quality issues
        val qualityIssues = identifyQualityIssues(
            rewardAnalysis, stateAnalysis, actionAnalysis, temporalAnalysis, diversityAnalysis
        )
        
        // Generate recommendations
        val recommendations = generateRecommendations(qualityIssues, qualityScore)
        
        return ExperienceBufferAnalysis(
            bufferSize = experiences.size,
            qualityScore = qualityScore,
            rewardAnalysis = rewardAnalysis,
            stateAnalysis = stateAnalysis,
            actionAnalysis = actionAnalysis,
            temporalAnalysis = temporalAnalysis,
            diversityAnalysis = diversityAnalysis,
            qualityIssues = qualityIssues,
            recommendations = recommendations
        )
    }
    
    /**
     * Detailed inspection of experience buffer with configurable depth
     */
    fun inspectBuffer(
        experiences: List<Experience<DoubleArray, Int>>,
        analysisDepth: AnalysisDepth = AnalysisDepth.STANDARD
    ): ExperienceBufferInspectionResult {
        
        val basicAnalysis = analyzeExperienceBuffer(experiences)
        
        val detailedAnalysis = when (analysisDepth) {
            AnalysisDepth.BASIC -> null
            AnalysisDepth.STANDARD -> performStandardInspection(experiences)
            AnalysisDepth.DETAILED -> performDetailedInspection(experiences)
            AnalysisDepth.COMPREHENSIVE -> performComprehensiveInspection(experiences)
        }
        
        val episodeAnalysis = if (analysisDepth >= AnalysisDepth.DETAILED) {
            analyzeEpisodeStructure(experiences)
        } else null
        
        val correlationAnalysis = if (analysisDepth == AnalysisDepth.COMPREHENSIVE) {
            analyzeCorrelations(experiences)
        } else null
        
        return ExperienceBufferInspectionResult(
            basicAnalysis = basicAnalysis,
            detailedAnalysis = detailedAnalysis,
            episodeAnalysis = episodeAnalysis,
            correlationAnalysis = correlationAnalysis,
            analysisDepth = analysisDepth,
            inspectionTimestamp = getCurrentTimeMillis()
        )
    }
    
    /**
     * Compare multiple experience buffers
     */
    fun compareBuffers(
        buffers: Map<String, List<Experience<DoubleArray, Int>>>,
        comparisonMetrics: List<ComparisonMetric> = ComparisonMetric.values().toList()
    ): BufferComparisonResult {
        
        val bufferAnalyses = buffers.mapValues { (name, experiences) ->
            println("Analyzing buffer: $name")
            analyzeExperienceBuffer(experiences)
        }
        
        val comparisons = mutableMapOf<ComparisonMetric, Map<String, Double>>()
        
        comparisonMetrics.forEach { metric ->
            comparisons[metric] = when (metric) {
                ComparisonMetric.QUALITY_SCORE -> bufferAnalyses.mapValues { it.value.qualityScore }
                ComparisonMetric.REWARD_VARIANCE -> bufferAnalyses.mapValues { it.value.rewardAnalysis.variance }
                ComparisonMetric.ACTION_DIVERSITY -> bufferAnalyses.mapValues { it.value.actionAnalysis.diversity }
                ComparisonMetric.STATE_DIVERSITY -> bufferAnalyses.mapValues { it.value.diversityAnalysis.stateDiversity }
                ComparisonMetric.TEMPORAL_CONSISTENCY -> bufferAnalyses.mapValues { it.value.temporalAnalysis.consistency }
            }
        }
        
        val rankings = generateRankings(comparisons)
        val insights = generateComparisonInsights(bufferAnalyses, comparisons)
        
        return BufferComparisonResult(
            bufferNames = buffers.keys.toList(),
            bufferAnalyses = bufferAnalyses,
            comparisons = comparisons,
            rankings = rankings,
            insights = insights
        )
    }
    
    /**
     * Analyze experience quality over time
     */
    fun analyzeQualityOverTime(
        experiences: List<Experience<DoubleArray, Int>>,
        windowSize: Int = 1000
    ): QualityTimeSeriesAnalysis {
        
        if (experiences.size < windowSize) {
            return QualityTimeSeriesAnalysis(
                windowSize = windowSize,
                qualityTimeSeries = emptyList(),
                trends = QualityTrends(0.0, 0.0, 0.0, 0.0),
                changePoints = emptyList(),
                overallTrend = TrendDirection.STABLE
            )
        }
        
        val qualityTimeSeries = mutableListOf<QualityDataPoint>()
        
        // Analyze quality in sliding windows
        for (i in 0 until experiences.size - windowSize + 1 step windowSize / 2) {
            val window = experiences.subList(i, minOf(i + windowSize, experiences.size))
            val windowAnalysis = analyzeExperienceBuffer(window)
            
            qualityTimeSeries.add(QualityDataPoint(
                timestamp = i,
                qualityScore = windowAnalysis.qualityScore,
                rewardVariance = windowAnalysis.rewardAnalysis.variance,
                actionDiversity = windowAnalysis.actionAnalysis.diversity,
                stateDiversity = windowAnalysis.diversityAnalysis.stateDiversity
            ))
        }
        
        // Analyze trends
        val trends = analyzeQualityTrends(qualityTimeSeries)
        
        // Detect change points
        val changePoints = detectQualityChangePoints(qualityTimeSeries)
        
        // Determine overall trend
        val overallTrend = determineOverallTrend(trends)
        
        return QualityTimeSeriesAnalysis(
            windowSize = windowSize,
            qualityTimeSeries = qualityTimeSeries,
            trends = trends,
            changePoints = changePoints,
            overallTrend = overallTrend
        )
    }
    
    /**
     * Detect anomalies in experience buffer
     */
    fun detectAnomalies(
        experiences: List<Experience<DoubleArray, Int>>,
        anomalyThreshold: Double = 2.0
    ): AnomalyDetectionResult {
        
        val anomalies = mutableListOf<ExperienceAnomaly>()
        
        // Analyze rewards for anomalies
        val rewards = experiences.map { it.reward }
        val rewardMean = rewards.average()
        val rewardStd = sqrt(calculateVariance(rewards))
        
        experiences.forEachIndexed { index, experience ->
            val rewardZScore = abs(experience.reward - rewardMean) / (rewardStd + 1e-8)
            if (rewardZScore > anomalyThreshold) {
                anomalies.add(ExperienceAnomaly(
                    index = index,
                    type = AnomalyType.REWARD_OUTLIER,
                    severity = rewardZScore / anomalyThreshold,
                    description = "Reward ${experience.reward} is ${rewardZScore:.2f} standard deviations from mean",
                    experience = experience
                ))
            }
        }
        
        // Analyze state anomalies (simplified)
        val stateNorms = experiences.map { experience ->
            sqrt(experience.state.map { it * it }.sum())
        }
        val stateNormMean = stateNorms.average()
        val stateNormStd = sqrt(calculateVariance(stateNorms))
        
        experiences.forEachIndexed { index, experience ->
            val stateNorm = stateNorms[index]
            val stateZScore = abs(stateNorm - stateNormMean) / (stateNormStd + 1e-8)
            if (stateZScore > anomalyThreshold) {
                anomalies.add(ExperienceAnomaly(
                    index = index,
                    type = AnomalyType.STATE_OUTLIER,
                    severity = stateZScore / anomalyThreshold,
                    description = "State norm ${stateNorm:.2f} is ${stateZScore:.2f} standard deviations from mean",
                    experience = experience
                ))
            }
        }
        
        // Detect sequence anomalies
        val sequenceAnomalies = detectSequenceAnomalies(experiences, anomalyThreshold)
        anomalies.addAll(sequenceAnomalies)
        
        // Categorize anomalies by severity
        val severityDistribution = anomalies.groupBy { 
            when {
                it.severity > 3.0 -> AnomalySeverity.CRITICAL
                it.severity > 2.0 -> AnomalySeverity.HIGH
                it.severity > 1.5 -> AnomalySeverity.MEDIUM
                else -> AnomalySeverity.LOW
            }
        }
        
        return AnomalyDetectionResult(
            totalAnomalies = anomalies.size,
            anomalies = anomalies,
            severityDistribution = severityDistribution,
            anomalyRate = anomalies.size.toDouble() / experiences.size,
            detectionThreshold = anomalyThreshold
        )
    }
    
    // Private analysis methods
    
    private fun analyzeRewards(experiences: List<Experience<DoubleArray, Int>>): RewardAnalysis {
        val rewards = experiences.map { it.reward }
        
        val mean = rewards.average()
        val variance = calculateVariance(rewards)
        val min = rewards.minOrNull() ?: 0.0
        val max = rewards.maxOrNull() ?: 0.0
        val sparsity = rewards.count { it == 0.0 }.toDouble() / rewards.size
        
        return RewardAnalysis(
            mean = mean,
            variance = variance,
            min = min,
            max = max,
            sparsity = sparsity
        )
    }
    
    private fun analyzeStates(experiences: List<Experience<DoubleArray, Int>>): StateAnalysis {
        val states = experiences.map { it.state }
        
        if (states.isEmpty()) {
            return StateAnalysis(0.0, 0.0, emptyList())
        }
        
        val stateSize = states.first().size
        val stateMeans = DoubleArray(stateSize) { 0.0 }
        val stateVariances = DoubleArray(stateSize) { 0.0 }
        
        // Calculate per-dimension statistics
        for (dim in 0 until stateSize) {
            val dimValues = states.map { it[dim] }
            stateMeans[dim] = dimValues.average()
            stateVariances[dim] = calculateVariance(dimValues)
        }
        
        val averageVariance = stateVariances.average()
        val averageNorm = states.map { state ->
            sqrt(state.map { it * it }.sum())
        }.average()
        
        // Identify problematic dimensions
        val problematicDimensions = stateVariances.mapIndexedNotNull { index, variance ->
            if (variance < 1e-8) "Dimension $index has zero variance" else null
        }
        
        return StateAnalysis(
            averageVariance = averageVariance,
            averageNorm = averageNorm,
            problematicDimensions = problematicDimensions
        )
    }
    
    private fun analyzeActions(experiences: List<Experience<DoubleArray, Int>>): ActionAnalysis {
        val actions = experiences.map { it.action }
        
        val uniqueActions = actions.toSet().size
        val actionCounts = actions.groupBy { it }.mapValues { it.value.size }
        val diversity = uniqueActions.toDouble() / actions.size
        
        return ActionAnalysis(
            uniqueActions = uniqueActions,
            diversity = diversity,
            actionDistribution = actionCounts
        )
    }
    
    private fun analyzeTemporalPatterns(experiences: List<Experience<DoubleArray, Int>>): TemporalAnalysis {
        val rewards = experiences.map { it.reward }
        
        // Calculate autocorrelation at lag 1
        val autocorrelation = if (rewards.size > 1) {
            calculateAutocorrelation(rewards, 1)
        } else 0.0
        
        // Calculate consistency (inverse of variance in reward differences)
        val rewardDifferences = rewards.zipWithNext { a, b -> abs(b - a) }
        val consistency = if (rewardDifferences.isNotEmpty()) {
            1.0 / (1.0 + calculateVariance(rewardDifferences))
        } else 0.0
        
        // Identify patterns (simplified)
        val patterns = identifyTemporalPatterns(experiences)
        
        return TemporalAnalysis(
            autocorrelation = autocorrelation,
            consistency = consistency,
            patterns = patterns
        )
    }
    
    private fun analyzeDiversity(experiences: List<Experience<DoubleArray, Int>>): DiversityAnalysis {
        // State diversity (simplified using state norms)
        val stateNorms = experiences.map { experience ->
            sqrt(experience.state.map { it * it }.sum())
        }
        val stateDiversity = calculateVariance(stateNorms) / (stateNorms.average() + 1e-8)
        
        // Action diversity
        val actions = experiences.map { it.action }
        val uniqueActions = actions.toSet().size
        val actionDiversity = uniqueActions.toDouble() / actions.size
        
        // Reward diversity
        val rewards = experiences.map { it.reward }
        val uniqueRewards = rewards.toSet().size
        val rewardDiversity = uniqueRewards.toDouble() / rewards.size
        
        return DiversityAnalysis(
            stateDiversity = stateDiversity,
            actionDiversity = actionDiversity,
            rewardDiversity = rewardDiversity
        )
    }
    
    private fun calculateOverallQuality(
        rewardAnalysis: RewardAnalysis,
        stateAnalysis: StateAnalysis,
        actionAnalysis: ActionAnalysis,
        temporalAnalysis: TemporalAnalysis,
        diversityAnalysis: DiversityAnalysis
    ): Double {
        
        // Quality components (0-1 scale)
        val rewardQuality = 1.0 - rewardAnalysis.sparsity // Lower sparsity is better
        val stateQuality = minOf(1.0, stateAnalysis.averageVariance) // Some variance is good
        val actionQuality = diversityAnalysis.actionDiversity // Higher diversity is better
        val temporalQuality = temporalAnalysis.consistency // Higher consistency is better
        val diversityQuality = (diversityAnalysis.stateDiversity + diversityAnalysis.actionDiversity) / 2.0
        
        // Weighted average
        return (rewardQuality * 0.3 + 
                stateQuality * 0.2 + 
                actionQuality * 0.2 + 
                temporalQuality * 0.15 + 
                diversityQuality * 0.15).coerceIn(0.0, 1.0)
    }
    
    private fun identifyQualityIssues(
        rewardAnalysis: RewardAnalysis,
        stateAnalysis: StateAnalysis,
        actionAnalysis: ActionAnalysis,
        temporalAnalysis: TemporalAnalysis,
        diversityAnalysis: DiversityAnalysis
    ): List<String> {
        
        val issues = mutableListOf<String>()
        
        if (rewardAnalysis.sparsity > 0.9) {
            issues.add("Very sparse rewards (${(rewardAnalysis.sparsity * 100).toInt()}% zero rewards)")
        }
        
        if (rewardAnalysis.variance < 1e-6) {
            issues.add("No reward variance - all rewards are identical")
        }
        
        if (stateAnalysis.averageVariance < 1e-6) {
            issues.add("Very low state variance - limited state diversity")
        }
        
        if (actionAnalysis.diversity < 0.1) {
            issues.add("Low action diversity (${(actionAnalysis.diversity * 100).toInt()}% unique actions)")
        }
        
        if (temporalAnalysis.consistency < 0.3) {
            issues.add("Low temporal consistency - highly variable reward sequences")
        }
        
        if (diversityAnalysis.stateDiversity < 0.1) {
            issues.add("Low state diversity - experiences may be too similar")
        }
        
        if (stateAnalysis.problematicDimensions.isNotEmpty()) {
            issues.add("${stateAnalysis.problematicDimensions.size} state dimensions have issues")
        }
        
        return issues
    }
    
    private fun generateRecommendations(issues: List<String>, qualityScore: Double): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (qualityScore < 0.5) {
            recommendations.add("Overall buffer quality is low - consider improving data collection")
        }
        
        issues.forEach { issue ->
            when {
                issue.contains("sparse rewards") -> {
                    recommendations.add("Implement reward shaping to reduce sparsity")
                    recommendations.add("Consider intrinsic motivation or curiosity-driven exploration")
                }
                issue.contains("reward variance") -> {
                    recommendations.add("Check reward function implementation")
                    recommendations.add("Ensure diverse training scenarios")
                }
                issue.contains("state variance") -> {
                    recommendations.add("Increase exploration to visit more diverse states")
                    recommendations.add("Check state encoding for potential issues")
                }
                issue.contains("action diversity") -> {
                    recommendations.add("Increase exploration rate or use different exploration strategy")
                    recommendations.add("Check action space coverage")
                }
                issue.contains("temporal consistency") -> {
                    recommendations.add("Review episode termination conditions")
                    recommendations.add("Consider smoothing reward signals")
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Buffer quality is acceptable - continue current approach")
        }
        
        return recommendations.distinct()
    }
    
    private fun performStandardInspection(
        experiences: List<Experience<DoubleArray, Int>>
    ): StandardInspectionResult {
        
        // Episode length analysis
        val episodeLengths = extractEpisodeLengths(experiences)
        val avgEpisodeLength = episodeLengths.average()
        val episodeLengthVariance = calculateVariance(episodeLengths.map { it.toDouble() })
        
        // Terminal state analysis
        val terminalStates = experiences.filter { it.done }
        val terminalRatio = terminalStates.size.toDouble() / experiences.size
        
        return StandardInspectionResult(
            averageEpisodeLength = avgEpisodeLength,
            episodeLengthVariance = episodeLengthVariance,
            terminalRatio = terminalRatio,
            episodeCount = episodeLengths.size
        )
    }
    
    private fun performDetailedInspection(
        experiences: List<Experience<DoubleArray, Int>>
    ): DetailedInspectionResult {
        
        val standardResult = performStandardInspection(experiences)
        
        // State transition analysis
        val stateTransitions = analyzeStateTransitions(experiences)
        
        // Reward distribution analysis
        val rewardDistribution = analyzeRewardDistribution(experiences)
        
        // Action sequence analysis
        val actionSequences = analyzeActionSequences(experiences)
        
        return DetailedInspectionResult(
            standardInspection = standardResult,
            stateTransitions = stateTransitions,
            rewardDistribution = rewardDistribution,
            actionSequences = actionSequences
        )
    }
    
    private fun performComprehensiveInspection(
        experiences: List<Experience<DoubleArray, Int>>
    ): ComprehensiveInspectionResult {
        
        val detailedResult = performDetailedInspection(experiences)
        
        // Advanced statistical analysis
        val statisticalAnalysis = performStatisticalAnalysis(experiences)
        
        // Clustering analysis
        val clusteringAnalysis = performClusteringAnalysis(experiences)
        
        // Information theory analysis
        val informationAnalysis = performInformationAnalysis(experiences)
        
        return ComprehensiveInspectionResult(
            detailedInspection = detailedResult,
            statisticalAnalysis = statisticalAnalysis,
            clusteringAnalysis = clusteringAnalysis,
            informationAnalysis = informationAnalysis
        )
    }
    
    // Utility methods
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateAutocorrelation(values: List<Double>, lag: Int): Double {
        if (values.size <= lag) return 0.0
        
        val mean = values.average()
        val variance = calculateVariance(values)
        
        if (variance < 1e-8) return 0.0
        
        val covariance = (0 until values.size - lag).map { i ->
            (values[i] - mean) * (values[i + lag] - mean)
        }.average()
        
        return covariance / variance
    }
    
    private fun identifyTemporalPatterns(experiences: List<Experience<DoubleArray, Int>>): List<String> {
        val patterns = mutableListOf<String>()
        
        // Check for reward trends
        val rewards = experiences.map { it.reward }
        if (rewards.size > 10) {
            val firstHalf = rewards.take(rewards.size / 2).average()
            val secondHalf = rewards.drop(rewards.size / 2).average()
            
            if (secondHalf > firstHalf * 1.1) {
                patterns.add("Increasing reward trend")
            } else if (secondHalf < firstHalf * 0.9) {
                patterns.add("Decreasing reward trend")
            } else {
                patterns.add("Stable reward pattern")
            }
        }
        
        // Check for action patterns
        val actions = experiences.map { it.action }
        val actionRuns = findActionRuns(actions)
        if (actionRuns.any { it > 5 }) {
            patterns.add("Long action sequences detected")
        }
        
        return patterns
    }
    
    private fun findActionRuns(actions: List<Int>): List<Int> {
        if (actions.isEmpty()) return emptyList()
        
        val runs = mutableListOf<Int>()
        var currentRun = 1
        
        for (i in 1 until actions.size) {
            if (actions[i] == actions[i - 1]) {
                currentRun++
            } else {
                runs.add(currentRun)
                currentRun = 1
            }
        }
        runs.add(currentRun)
        
        return runs
    }
    
    private fun extractEpisodeLengths(experiences: List<Experience<DoubleArray, Int>>): List<Int> {
        val episodeLengths = mutableListOf<Int>()
        var currentLength = 0
        
        experiences.forEach { experience ->
            currentLength++
            if (experience.done) {
                episodeLengths.add(currentLength)
                currentLength = 0
            }
        }
        
        // Add incomplete episode if exists
        if (currentLength > 0) {
            episodeLengths.add(currentLength)
        }
        
        return episodeLengths
    }
    
    private fun detectSequenceAnomalies(
        experiences: List<Experience<DoubleArray, Int>>,
        threshold: Double
    ): List<ExperienceAnomaly> {
        
        val anomalies = mutableListOf<ExperienceAnomaly>()
        
        // Detect impossible transitions (simplified)
        experiences.zipWithNext { current, next ->
            if (current.done && !next.done) {
                // This might indicate a data collection error
                anomalies.add(ExperienceAnomaly(
                    index = experiences.indexOf(next),
                    type = AnomalyType.SEQUENCE_ERROR,
                    severity = 2.0,
                    description = "Non-terminal experience follows terminal experience",
                    experience = next
                ))
            }
        }
        
        return anomalies
    }
    
    // Placeholder implementations for advanced analysis
    
    private fun analyzeEpisodeStructure(experiences: List<Experience<DoubleArray, Int>>): EpisodeStructureAnalysis {
        val episodeLengths = extractEpisodeLengths(experiences)
        return EpisodeStructureAnalysis(
            episodeCount = episodeLengths.size,
            averageLength = episodeLengths.average(),
            lengthVariance = calculateVariance(episodeLengths.map { it.toDouble() }),
            lengthDistribution = episodeLengths.groupBy { it }.mapValues { it.value.size }
        )
    }
    
    private fun analyzeCorrelations(experiences: List<Experience<DoubleArray, Int>>): CorrelationAnalysis {
        // Simplified correlation analysis
        return CorrelationAnalysis(
            stateActionCorrelation = 0.0,
            rewardStateCorrelation = 0.0,
            temporalCorrelations = emptyMap()
        )
    }
    
    private fun generateRankings(comparisons: Map<ComparisonMetric, Map<String, Double>>): Map<ComparisonMetric, List<String>> {
        return comparisons.mapValues { (_, values) ->
            values.toList().sortedByDescending { it.second }.map { it.first }
        }
    }
    
    private fun generateComparisonInsights(
        analyses: Map<String, ExperienceBufferAnalysis>,
        comparisons: Map<ComparisonMetric, Map<String, Double>>
    ): List<String> {
        
        val insights = mutableListOf<String>()
        
        // Find best and worst performing buffers
        val qualityScores = comparisons[ComparisonMetric.QUALITY_SCORE] ?: emptyMap()
        val bestBuffer = qualityScores.maxByOrNull { it.value }?.key
        val worstBuffer = qualityScores.minByOrNull { it.value }?.key
        
        if (bestBuffer != null && worstBuffer != null) {
            insights.add("Best performing buffer: $bestBuffer")
            insights.add("Worst performing buffer: $worstBuffer")
        }
        
        // Analyze variance in quality
        val qualityVariance = calculateVariance(qualityScores.values.toList())
        if (qualityVariance > 0.1) {
            insights.add("High variance in buffer quality detected")
        }
        
        return insights
    }
    
    private fun analyzeQualityTrends(timeSeries: List<QualityDataPoint>): QualityTrends {
        val qualityScores = timeSeries.map { it.qualityScore }
        val rewardVariances = timeSeries.map { it.rewardVariance }
        val actionDiversities = timeSeries.map { it.actionDiversity }
        val stateDiversities = timeSeries.map { it.stateDiversity }
        
        return QualityTrends(
            qualityTrend = calculateTrend(qualityScores),
            rewardVarianceTrend = calculateTrend(rewardVariances),
            actionDiversityTrend = calculateTrend(actionDiversities),
            stateDiversityTrend = calculateTrend(stateDiversities)
        )
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
    
    private fun detectQualityChangePoints(timeSeries: List<QualityDataPoint>): List<ChangePoint> {
        // Simplified change point detection
        val changePoints = mutableListOf<ChangePoint>()
        
        if (timeSeries.size < 10) return changePoints
        
        val qualityScores = timeSeries.map { it.qualityScore }
        val windowSize = 5
        
        for (i in windowSize until qualityScores.size - windowSize) {
            val beforeMean = qualityScores.subList(i - windowSize, i).average()
            val afterMean = qualityScores.subList(i, i + windowSize).average()
            
            if (abs(afterMean - beforeMean) > 0.2) {
                changePoints.add(ChangePoint(
                    timestamp = timeSeries[i].timestamp,
                    changeType = if (afterMean > beforeMean) ChangeType.IMPROVEMENT else ChangeType.DEGRADATION,
                    magnitude = abs(afterMean - beforeMean)
                ))
            }
        }
        
        return changePoints
    }
    
    private fun determineOverallTrend(trends: QualityTrends): TrendDirection {
        val avgTrend = (trends.qualityTrend + trends.actionDiversityTrend + trends.stateDiversityTrend) / 3.0
        
        return when {
            avgTrend > 0.01 -> TrendDirection.IMPROVING
            avgTrend < -0.01 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }
    }
    
    // Placeholder implementations for detailed analysis methods
    
    private fun analyzeStateTransitions(experiences: List<Experience<DoubleArray, Int>>): StateTransitionAnalysis {
        return StateTransitionAnalysis(
            transitionCount = experiences.size - 1,
            averageTransitionMagnitude = 0.0,
            transitionPatterns = emptyList()
        )
    }
    
    private fun analyzeRewardDistribution(experiences: List<Experience<DoubleArray, Int>>): RewardDistributionAnalysis {
        val rewards = experiences.map { it.reward }
        return RewardDistributionAnalysis(
            distribution = rewards.groupBy { it }.mapValues { it.value.size },
            skewness = 0.0,
            kurtosis = 0.0
        )
    }
    
    private fun analyzeActionSequences(experiences: List<Experience<DoubleArray, Int>>): ActionSequenceAnalysis {
        val actions = experiences.map { it.action }
        return ActionSequenceAnalysis(
            sequenceLength = actions.size,
            uniqueSequences = 0,
            commonPatterns = emptyList()
        )
    }
    
    private fun performStatisticalAnalysis(experiences: List<Experience<DoubleArray, Int>>): StatisticalAnalysis {
        return StatisticalAnalysis(
            normalityTests = emptyMap(),
            correlationMatrix = emptyMap(),
            significanceTests = emptyMap()
        )
    }
    
    private fun performClusteringAnalysis(experiences: List<Experience<DoubleArray, Int>>): ClusteringAnalysis {
        return ClusteringAnalysis(
            clusterCount = 0,
            clusterSizes = emptyList(),
            clusterCharacteristics = emptyMap()
        )
    }
    
    private fun performInformationAnalysis(experiences: List<Experience<DoubleArray, Int>>): InformationAnalysis {
        return InformationAnalysis(
            mutualInformation = 0.0,
            entropy = 0.0,
            informationGain = 0.0
        )
    }
}

/**
 * Configuration for experience buffer analysis
 */
data class BufferAnalysisConfig(
    val enableDetailedAnalysis: Boolean = true,
    val anomalyDetectionThreshold: Double = 2.0,
    val maxAnalysisSize: Int = 100000,
    val enableCorrelationAnalysis: Boolean = false
)

// Enums and supporting classes

enum class ComparisonMetric {
    QUALITY_SCORE, REWARD_VARIANCE, ACTION_DIVERSITY, STATE_DIVERSITY, TEMPORAL_CONSISTENCY
}

enum class AnomalyType {
    REWARD_OUTLIER, STATE_OUTLIER, ACTION_OUTLIER, SEQUENCE_ERROR, TEMPORAL_ANOMALY
}

enum class AnomalySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class TrendDirection {
    IMPROVING, DECLINING, STABLE
}

enum class ChangeType {
    IMPROVEMENT, DEGRADATION, SHIFT
}

// Data classes for analysis results

data class ExperienceBufferAnalysis(
    val bufferSize: Int,
    val qualityScore: Double,
    val rewardAnalysis: RewardAnalysis,
    val stateAnalysis: StateAnalysis,
    val actionAnalysis: ActionAnalysis,
    val temporalAnalysis: TemporalAnalysis,
    val diversityAnalysis: DiversityAnalysis,
    val qualityIssues: List<String>,
    val recommendations: List<String>
)

data class RewardAnalysis(
    val mean: Double,
    val variance: Double,
    val min: Double,
    val max: Double,
    val sparsity: Double
)

data class StateAnalysis(
    val averageVariance: Double,
    val averageNorm: Double,
    val problematicDimensions: List<String>
)

data class ActionAnalysis(
    val uniqueActions: Int,
    val diversity: Double,
    val actionDistribution: Map<Int, Int>
)

data class TemporalAnalysis(
    val autocorrelation: Double,
    val consistency: Double,
    val patterns: List<String>
)

data class DiversityAnalysis(
    val stateDiversity: Double,
    val actionDiversity: Double,
    val rewardDiversity: Double
)

data class ExperienceBufferInspectionResult(
    val basicAnalysis: ExperienceBufferAnalysis,
    val detailedAnalysis: DetailedInspectionResult?,
    val episodeAnalysis: EpisodeStructureAnalysis?,
    val correlationAnalysis: CorrelationAnalysis?,
    val analysisDepth: AnalysisDepth,
    val inspectionTimestamp: Long
)

data class BufferComparisonResult(
    val bufferNames: List<String>,
    val bufferAnalyses: Map<String, ExperienceBufferAnalysis>,
    val comparisons: Map<ComparisonMetric, Map<String, Double>>,
    val rankings: Map<ComparisonMetric, List<String>>,
    val insights: List<String>
)

data class QualityTimeSeriesAnalysis(
    val windowSize: Int,
    val qualityTimeSeries: List<QualityDataPoint>,
    val trends: QualityTrends,
    val changePoints: List<ChangePoint>,
    val overallTrend: TrendDirection
)

data class QualityDataPoint(
    val timestamp: Int,
    val qualityScore: Double,
    val rewardVariance: Double,
    val actionDiversity: Double,
    val stateDiversity: Double
)

data class QualityTrends(
    val qualityTrend: Double,
    val rewardVarianceTrend: Double,
    val actionDiversityTrend: Double,
    val stateDiversityTrend: Double
)

data class ChangePoint(
    val timestamp: Int,
    val changeType: ChangeType,
    val magnitude: Double
)

data class AnomalyDetectionResult(
    val totalAnomalies: Int,
    val anomalies: List<ExperienceAnomaly>,
    val severityDistribution: Map<AnomalySeverity, List<ExperienceAnomaly>>,
    val anomalyRate: Double,
    val detectionThreshold: Double
)

data class ExperienceAnomaly(
    val index: Int,
    val type: AnomalyType,
    val severity: Double,
    val description: String,
    val experience: Experience<DoubleArray, Int>
)

// Additional analysis result classes

data class StandardInspectionResult(
    val averageEpisodeLength: Double,
    val episodeLengthVariance: Double,
    val terminalRatio: Double,
    val episodeCount: Int
)

data class DetailedInspectionResult(
    val standardInspection: StandardInspectionResult,
    val stateTransitions: StateTransitionAnalysis,
    val rewardDistribution: RewardDistributionAnalysis,
    val actionSequences: ActionSequenceAnalysis
)

data class ComprehensiveInspectionResult(
    val detailedInspection: DetailedInspectionResult,
    val statisticalAnalysis: StatisticalAnalysis,
    val clusteringAnalysis: ClusteringAnalysis,
    val informationAnalysis: InformationAnalysis
)

data class EpisodeStructureAnalysis(
    val episodeCount: Int,
    val averageLength: Double,
    val lengthVariance: Double,
    val lengthDistribution: Map<Int, Int>
)

data class CorrelationAnalysis(
    val stateActionCorrelation: Double,
    val rewardStateCorrelation: Double,
    val temporalCorrelations: Map<String, Double>
)

data class StateTransitionAnalysis(
    val transitionCount: Int,
    val averageTransitionMagnitude: Double,
    val transitionPatterns: List<String>
)

data class RewardDistributionAnalysis(
    val distribution: Map<Double, Int>,
    val skewness: Double,
    val kurtosis: Double
)

data class ActionSequenceAnalysis(
    val sequenceLength: Int,
    val uniqueSequences: Int,
    val commonPatterns: List<String>
)

data class StatisticalAnalysis(
    val normalityTests: Map<String, Double>,
    val correlationMatrix: Map<String, Map<String, Double>>,
    val significanceTests: Map<String, Double>
)

data class ClusteringAnalysis(
    val clusterCount: Int,
    val clusterSizes: List<Int>,
    val clusterCharacteristics: Map<Int, String>
)

data class InformationAnalysis(
    val mutualInformation: Double,
    val entropy: Double,
    val informationGain: Double
)