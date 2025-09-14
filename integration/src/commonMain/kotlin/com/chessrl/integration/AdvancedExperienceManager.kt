package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced experience buffer management with sophisticated sampling strategies,
 * quality assessment, and memory optimization for large-scale training.
 */
class AdvancedExperienceManager(
    private val config: ExperienceManagerConfig = ExperienceManagerConfig()
) {
    
    // Experience storage with multiple buffers
    private val experienceBuffer = mutableListOf<EnhancedExperience>()
    private val highQualityBuffer = mutableListOf<EnhancedExperience>()
    private val recentBuffer = mutableListOf<EnhancedExperience>()
    
    // Quality and statistics tracking
    private var totalExperiencesProcessed = 0
    private var totalExperiencesDiscarded = 0
    private val qualityHistogram = mutableMapOf<Double, Int>()
    private val samplingStatistics = mutableMapOf<SamplingStrategy, SamplingStats>()
    private val random: Random = try { SeedManager.getReplayBufferRandom() } catch (_: Throwable) { Random.Default }
    
    // Memory management
    private var lastCleanupSize = 0
    private var cleanupCount = 0
    
    /**
     * Process new experiences with quality assessment and buffer management
     */
    fun processExperiences(
        newExperiences: List<EnhancedExperience>,
        gameResults: List<SelfPlayGameResult>
    ): ExperienceProcessingResult {
        
        val processingStartTime = getCurrentTimeMillis()
        
        println("   🔍 Processing ${newExperiences.size} new experiences")
        
        // Quality assessment and filtering
        val qualityAssessment = assessExperienceQuality(newExperiences, gameResults)
        val filteredExperiences = filterExperiencesByQuality(newExperiences, qualityAssessment)
        
        // Add to appropriate buffers
        val bufferUpdates = addToBuffers(filteredExperiences)
        
        // Update statistics
        updateStatistics(newExperiences, filteredExperiences, qualityAssessment)
        
        // Memory management
        val memoryManagement = performMemoryManagement()
        
        val processingEndTime = getCurrentTimeMillis()
        val processingDuration = processingEndTime - processingStartTime
        
        return ExperienceProcessingResult(
            totalExperiences = newExperiences.size,
            acceptedExperiences = filteredExperiences.size,
            discardedExperiences = newExperiences.size - filteredExperiences.size,
            averageQuality = qualityAssessment.averageQuality,
            qualityDistribution = qualityAssessment.qualityDistribution,
            bufferUpdates = bufferUpdates,
            memoryManagement = memoryManagement,
            processingDuration = processingDuration
        )
    }
    
    /**
     * Sample a batch of experiences using advanced strategies
     */
    fun sampleBatch(batchSize: Int): List<EnhancedExperience> {
        val availableExperiences = experienceBuffer.size
        if (availableExperiences == 0) {
            return emptyList()
        }
        
        val actualBatchSize = minOf(batchSize, availableExperiences)
        val strategy = selectSamplingStrategy()
        
        val sampledExperiences = when (strategy) {
            SamplingStrategy.RANDOM -> sampleUniform(actualBatchSize)
            SamplingStrategy.RECENT -> sampleRecent(actualBatchSize)
            SamplingStrategy.MIXED -> sampleMixed(actualBatchSize)
        }
        
        // Update sampling statistics
        updateSamplingStatistics(strategy, sampledExperiences)
        
        return sampledExperiences
    }
    
    /**
     * Calculate quality score for a batch of experiences
     */
    fun calculateBatchQuality(experiences: List<EnhancedExperience>): Double {
        if (experiences.isEmpty()) return 0.0
        return experiences.map { it.qualityScore }.average()
    }
    
    /**
     * Get current buffer statistics
     */
    fun getStatistics(): ExperienceStatistics {
        return ExperienceStatistics(
            totalBufferSize = experienceBuffer.size,
            highQualityBufferSize = highQualityBuffer.size,
            recentBufferSize = recentBuffer.size,
            totalExperiencesProcessed = totalExperiencesProcessed,
            totalExperiencesDiscarded = totalExperiencesDiscarded,
            averageQuality = if (experienceBuffer.isNotEmpty()) {
                experienceBuffer.map { it.qualityScore }.average()
            } else 0.0,
            qualityHistogram = qualityHistogram.toMap(),
            samplingStatistics = samplingStatistics.toMap(),
            memoryUtilization = calculateMemoryUtilization()
        )
    }
    
    /**
     * Perform cleanup and memory optimization
     */
    fun performCleanup(): CleanupResult {
        val beforeSize = experienceBuffer.size
        val beforeMemory = calculateMemoryUtilization()
        
        // Remove low-quality experiences if buffer is full
        if (experienceBuffer.size >= config.maxBufferSize) {
            val cleanupCount = (experienceBuffer.size * config.cleanupRatio).toInt()
            performQualityBasedCleanup(cleanupCount)
        }
        
        // Optimize buffer organization
        optimizeBufferOrganization()
        
        // Update cleanup statistics
        val afterSize = experienceBuffer.size
        val afterMemory = calculateMemoryUtilization()
        cleanupCount++
        
        return CleanupResult(
            experiencesRemoved = beforeSize - afterSize,
            memoryFreed = beforeMemory - afterMemory,
            cleanupStrategy = config.cleanupStrategy,
            optimizationPerformed = true
        )
    }
    
    /**
     * Get buffer size
     */
    fun getBufferSize(): Int = experienceBuffer.size
    
    // Private helper methods
    
    /**
     * Assess quality of new experiences
     */
    private fun assessExperienceQuality(
        experiences: List<EnhancedExperience>,
        gameResults: List<SelfPlayGameResult>
    ): QualityAssessment {
        
        val qualityScores = experiences.map { experience ->
            calculateEnhancedQualityScore(experience, gameResults)
        }
        
        val averageQuality = qualityScores.average()
        val qualityDistribution = createQualityDistribution(qualityScores)
        
        return QualityAssessment(
            averageQuality = averageQuality,
            qualityDistribution = qualityDistribution,
            highQualityCount = qualityScores.count { it >= config.highQualityThreshold },
            mediumQualityCount = qualityScores.count { it >= config.mediumQualityThreshold && it < config.highQualityThreshold },
            lowQualityCount = qualityScores.count { it < config.mediumQualityThreshold }
        )
    }
    
    /**
     * Calculate enhanced quality score for an experience
     */
    @Suppress("UNUSED_PARAMETER")
    private fun calculateEnhancedQualityScore(
        experience: EnhancedExperience,
        gameResults: List<SelfPlayGameResult>
    ): Double {
        var qualityScore = experience.qualityScore // Base quality score
        
        // Game outcome bonus
        when (experience.gameOutcome) {
            GameOutcome.WHITE_WINS, GameOutcome.BLACK_WINS -> qualityScore += 0.2
            GameOutcome.DRAW -> qualityScore += 0.1
            GameOutcome.ONGOING -> qualityScore += 0.0
        }
        
        // Termination reason bonus
        when (experience.terminationReason) {
            EpisodeTerminationReason.GAME_ENDED -> qualityScore += 0.3
            EpisodeTerminationReason.STEP_LIMIT -> qualityScore += 0.1
            EpisodeTerminationReason.MANUAL -> qualityScore += 0.0
        }
        
        // Game phase bonus (endgame experiences are more valuable)
        if (experience.isEndGame) qualityScore += 0.15
        else if (experience.isMidGame) qualityScore += 0.1
        else if (experience.isEarlyGame) qualityScore += 0.05
        
        // Reward magnitude bonus
        val absReward = abs(experience.reward)
        if (absReward > 0.8) qualityScore += 0.1
        else if (absReward > 0.5) qualityScore += 0.05
        
        // Chess-specific quality factors (simplified for now)
        // Chess-specific quality assessment can use experience.chessMetrics in future
        
        return qualityScore.coerceIn(0.0, 1.0)
    }
    
    /**
     * Create quality distribution histogram
     */
    private fun createQualityDistribution(qualityScores: List<Double>): Map<String, Int> {
        val distribution = mutableMapOf<String, Int>()
        
        for (score in qualityScores) {
            val bucket = when {
                score >= 0.8 -> "excellent"
                score >= 0.6 -> "good"
                score >= 0.4 -> "fair"
                score >= 0.2 -> "poor"
                else -> "very_poor"
            }
            distribution[bucket] = distribution.getOrDefault(bucket, 0) + 1
        }
        
        return distribution
    }
    
    /**
     * Filter experiences by quality threshold
     */
    @Suppress("UNUSED_PARAMETER")
    private fun filterExperiencesByQuality(
        experiences: List<EnhancedExperience>,
        qualityAssessment: QualityAssessment
    ): List<EnhancedExperience> {
        
        return experiences.filter { experience ->
            val enhancedQuality = calculateEnhancedQualityScore(experience, emptyList())
            enhancedQuality >= config.qualityThreshold
        }
    }
    
    /**
     * Add experiences to appropriate buffers
     */
    private fun addToBuffers(experiences: List<EnhancedExperience>): BufferUpdateResult {
        var addedToMain = 0
        var addedToHighQuality = 0
        var addedToRecent = 0
        
        for (experience in experiences) {
            // Add to main buffer
            experienceBuffer.add(experience)
            addedToMain++
            
            // Add to high-quality buffer if quality is high enough
            if (experience.qualityScore >= config.highQualityThreshold) {
                highQualityBuffer.add(experience)
                addedToHighQuality++
            }
            
            // Add to recent buffer
            recentBuffer.add(experience)
            addedToRecent++
            
            // Maintain recent buffer size
            if (recentBuffer.size > config.recentBufferSize) {
                recentBuffer.removeAt(0)
                addedToRecent--
            }
        }
        
        return BufferUpdateResult(
            addedToMain = addedToMain,
            addedToHighQuality = addedToHighQuality,
            addedToRecent = addedToRecent,
            mainBufferSize = experienceBuffer.size,
            highQualityBufferSize = highQualityBuffer.size,
            recentBufferSize = recentBuffer.size
        )
    }
    
    /**
     * Update processing statistics
     */
    @Suppress("UNUSED_PARAMETER")
    private fun updateStatistics(
        newExperiences: List<EnhancedExperience>,
        filteredExperiences: List<EnhancedExperience>,
        qualityAssessment: QualityAssessment
    ) {
        totalExperiencesProcessed += newExperiences.size
        totalExperiencesDiscarded += (newExperiences.size - filteredExperiences.size)
        
        // Update quality histogram
        for (experience in filteredExperiences) {
            val qualityBucket = (experience.qualityScore * 10).toInt() / 10.0
            qualityHistogram[qualityBucket] = qualityHistogram.getOrDefault(qualityBucket, 0) + 1
        }
    }
    
    /**
     * Perform memory management
     */
    private fun performMemoryManagement(): MemoryManagementResult {
        val beforeSize = experienceBuffer.size
        var experiencesRemoved = 0
        var memoryOptimized = false
        
        // Remove excess experiences if buffer is over capacity
        if (experienceBuffer.size > config.maxBufferSize) {
            val excessCount = experienceBuffer.size - config.maxBufferSize
            
            when (config.cleanupStrategy) {
                ExperienceCleanupStrategy.OLDEST_FIRST -> {
                    repeat(excessCount) {
                        if (experienceBuffer.isNotEmpty()) {
                            experienceBuffer.removeAt(0)
                            experiencesRemoved++
                        }
                    }
                }
                ExperienceCleanupStrategy.LOWEST_QUALITY -> {
                    // Sort by quality and remove lowest quality experiences
                    experienceBuffer.sortBy { it.qualityScore }
                    repeat(excessCount) {
                        if (experienceBuffer.isNotEmpty()) {
                            experienceBuffer.removeAt(0)
                            experiencesRemoved++
                        }
                    }
                }
                ExperienceCleanupStrategy.RANDOM -> {
                    repeat(excessCount) {
                        if (experienceBuffer.isNotEmpty()) {
                            val randomIndex = Random.nextInt(experienceBuffer.size)
                            experienceBuffer.removeAt(randomIndex)
                            experiencesRemoved++
                        }
                    }
                }
            }
            
            memoryOptimized = true
        }
        
        return MemoryManagementResult(
            experiencesRemoved = experiencesRemoved,
            memoryOptimized = memoryOptimized,
            bufferSizeBefore = beforeSize,
            bufferSizeAfter = experienceBuffer.size
        )
    }
    
    /**
     * Select sampling strategy based on configuration and performance
     */
    private fun selectSamplingStrategy(): SamplingStrategy {
        if (config.samplingStrategies.size == 1) {
            return config.samplingStrategies.first()
        }
        
        // Use round-robin or performance-based selection
        val strategies = config.samplingStrategies
        val index = (totalExperiencesProcessed / 100) % strategies.size
        return strategies[index]
    }
    
    /**
     * Sample experiences uniformly from the buffer
     */
    private fun sampleUniform(batchSize: Int): List<EnhancedExperience> {
        return experienceBuffer.shuffled(random).take(batchSize)
    }
    
    /**
     * Sample recent experiences
     */
    private fun sampleRecent(batchSize: Int): List<EnhancedExperience> {
        return recentBuffer.takeLast(batchSize)
    }
    
    /**
     * Sample with mixed strategy (recent + uniform)
     */
    private fun sampleMixed(batchSize: Int): List<EnhancedExperience> {
        val recentCount = (batchSize * config.mixedSamplingRecentRatio).toInt()
        val uniformCount = batchSize - recentCount
        
        val recentSamples = recentBuffer.takeLast(recentCount)
        val uniformSamples = experienceBuffer.filter { it !in recentSamples }
            .shuffled(random).take(uniformCount)
        
        return recentSamples + uniformSamples
    }
    
    /**
     * Update sampling statistics
     */
    private fun updateSamplingStatistics(strategy: SamplingStrategy, experiences: List<EnhancedExperience>) {
        val stats = samplingStatistics.getOrDefault(strategy, SamplingStats())
        val avgQuality = experiences.map { it.qualityScore }.average()
        
        samplingStatistics[strategy] = SamplingStats(
            timesUsed = stats.timesUsed + 1,
            totalExperiencesSampled = stats.totalExperiencesSampled + experiences.size,
            averageQuality = (stats.averageQuality * stats.timesUsed + avgQuality) / (stats.timesUsed + 1)
        )
    }
    
    /**
     * Perform quality-based cleanup
     */
    private fun performQualityBasedCleanup(cleanupCount: Int) {
        // Sort by quality (ascending) and remove lowest quality experiences
        experienceBuffer.sortBy { it.qualityScore }
        repeat(cleanupCount) {
            if (experienceBuffer.isNotEmpty()) {
                experienceBuffer.removeAt(0)
            }
        }
    }
    
    /**
     * Optimize buffer organization for better performance
     */
    private fun optimizeBufferOrganization() {
        // Sort main buffer by quality (descending) for better sampling performance
        experienceBuffer.sortByDescending { it.qualityScore }
        
        // Rebuild high-quality buffer
        highQualityBuffer.clear()
        highQualityBuffer.addAll(
            experienceBuffer.filter { it.qualityScore >= config.highQualityThreshold }
        )
    }
    
    /**
     * Calculate memory utilization estimate
     */
    private fun calculateMemoryUtilization(): Double {
        // Rough estimate based on buffer sizes
        val mainBufferMemory = experienceBuffer.size * 0.001 // MB per experience (rough estimate)
        val highQualityBufferMemory = highQualityBuffer.size * 0.001
        val recentBufferMemory = recentBuffer.size * 0.001
        
        return mainBufferMemory + highQualityBufferMemory + recentBufferMemory
    }
}

/**
 * Configuration for experience manager
 */
data class ExperienceManagerConfig(
    val maxBufferSize: Int = 50000,
    val recentBufferSize: Int = 5000,
    val qualityThreshold: Double = 0.3,
    val highQualityThreshold: Double = 0.7,
    val mediumQualityThreshold: Double = 0.5,
    val samplingStrategies: List<SamplingStrategy> = listOf(
        SamplingStrategy.RANDOM, SamplingStrategy.RECENT, SamplingStrategy.MIXED
    ),
    val mixedSamplingRecentRatio: Double = 0.3,
    val cleanupStrategy: ExperienceCleanupStrategy = ExperienceCleanupStrategy.LOWEST_QUALITY,
    val cleanupRatio: Double = 0.1,
    val memoryOptimization: Boolean = true
)

/**
 * Result of experience processing
 */
data class ExperienceProcessingResult(
    val totalExperiences: Int,
    val acceptedExperiences: Int,
    val discardedExperiences: Int,
    val averageQuality: Double,
    val qualityDistribution: Map<String, Int>,
    val bufferUpdates: BufferUpdateResult,
    val memoryManagement: MemoryManagementResult,
    val processingDuration: Long
)

/**
 * Quality assessment result
 */
data class QualityAssessment(
    val averageQuality: Double,
    val qualityDistribution: Map<String, Int>,
    val highQualityCount: Int,
    val mediumQualityCount: Int,
    val lowQualityCount: Int
)

/**
 * Buffer update result
 */
data class BufferUpdateResult(
    val addedToMain: Int,
    val addedToHighQuality: Int,
    val addedToRecent: Int,
    val mainBufferSize: Int,
    val highQualityBufferSize: Int,
    val recentBufferSize: Int
)

/**
 * Memory management result
 */
data class MemoryManagementResult(
    val experiencesRemoved: Int,
    val memoryOptimized: Boolean,
    val bufferSizeBefore: Int,
    val bufferSizeAfter: Int
)

/**
 * Cleanup result
 */
data class CleanupResult(
    val experiencesRemoved: Int,
    val memoryFreed: Double,
    val cleanupStrategy: ExperienceCleanupStrategy,
    val optimizationPerformed: Boolean
)

/**
 * Experience statistics
 */
data class ExperienceStatistics(
    val totalBufferSize: Int,
    val highQualityBufferSize: Int,
    val recentBufferSize: Int,
    val totalExperiencesProcessed: Int,
    val totalExperiencesDiscarded: Int,
    val averageQuality: Double,
    val qualityHistogram: Map<Double, Int>,
    val samplingStatistics: Map<SamplingStrategy, SamplingStats>,
    val memoryUtilization: Double
)

/**
 * Sampling statistics
 */
data class SamplingStats(
    val timesUsed: Int = 0,
    val totalExperiencesSampled: Int = 0,
    val averageQuality: Double = 0.0
)
