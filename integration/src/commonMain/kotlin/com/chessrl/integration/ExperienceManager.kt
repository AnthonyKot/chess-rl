package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.random.Random

/**
 * Simple experience manager with circular buffer and fixed cleanup strategy.
 * Consolidates AdvancedExperienceManager and ExperienceBufferAnalyzer into essential functionality only.
 */
class ExperienceManager(
    private val maxBufferSize: Int = 50000,
    private val cleanupRatio: Double = 0.1,
    private val random: Random = Random.Default
) {
    
    // Simple circular buffer for experience storage
    private val experienceBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    private var nextIndex = 0
    
    // Basic statistics
    private var totalExperiencesAdded = 0
    private var totalExperiencesRemoved = 0
    
    /**
     * Add new experiences to the buffer
     */
    fun addExperiences(newExperiences: List<Experience<DoubleArray, Int>>) {
        for (experience in newExperiences) {
            addExperience(experience)
        }
    }
    
    /**
     * Add a single experience to the buffer
     */
    fun addExperience(experience: Experience<DoubleArray, Int>) {
        if (experienceBuffer.size < maxBufferSize) {
            // Buffer not full, just add
            experienceBuffer.add(experience)
        } else {
            // Buffer full, replace oldest (circular buffer behavior)
            experienceBuffer[nextIndex] = experience
            nextIndex = (nextIndex + 1) % maxBufferSize
        }
        
        totalExperiencesAdded++
    }
    
    /**
     * Sample a batch of experiences uniformly at random
     */
    fun sampleBatch(batchSize: Int): List<Experience<DoubleArray, Int>> {
        if (experienceBuffer.isEmpty()) {
            return emptyList()
        }
        
        val actualBatchSize = minOf(batchSize, experienceBuffer.size)
        return experienceBuffer.shuffled(random).take(actualBatchSize)
    }
    
    /**
     * Get current buffer size
     */
    fun getBufferSize(): Int = experienceBuffer.size
    
    /**
     * Get buffer utilization (0.0 to 1.0)
     */
    fun getBufferUtilization(): Double {
        return experienceBuffer.size.toDouble() / maxBufferSize
    }
    
    /**
     * Get basic buffer statistics
     */
    fun getStatistics(): ExperienceBufferStats {
        val rewards = experienceBuffer.map { it.reward }
        val averageReward = if (rewards.isNotEmpty()) rewards.average() else 0.0
        
        return ExperienceBufferStats(
            bufferSize = experienceBuffer.size,
            maxBufferSize = maxBufferSize,
            utilization = getBufferUtilization(),
            totalAdded = totalExperiencesAdded,
            totalRemoved = totalExperiencesRemoved,
            averageReward = averageReward
        )
    }
    
    /**
     * Perform cleanup if buffer is getting full
     */
    fun performCleanup(): CleanupStats {
        val beforeSize = experienceBuffer.size
        
        if (experienceBuffer.size >= maxBufferSize * 0.9) {
            val cleanupCount = (experienceBuffer.size * cleanupRatio).toInt()

            // Remove oldest experiences efficiently
            if (cleanupCount > 0 && cleanupCount <= experienceBuffer.size) {
                experienceBuffer.subList(0, cleanupCount).clear()
                totalExperiencesRemoved += cleanupCount
            }
            
            // Reset circular buffer index if needed
            nextIndex = minOf(nextIndex, experienceBuffer.size)
        }
        
        val afterSize = experienceBuffer.size
        val experiencesRemoved = beforeSize - afterSize
        
        return CleanupStats(
            experiencesRemoved = experiencesRemoved,
            bufferSizeBefore = beforeSize,
            bufferSizeAfter = afterSize
        )
    }
    
    /**
     * Clear all experiences from buffer
     */
    fun clear() {
        experienceBuffer.clear()
        nextIndex = 0
        totalExperiencesAdded = 0
        totalExperiencesRemoved = 0
    }
    
    /**
     * Check if buffer is empty
     */
    fun isEmpty(): Boolean = experienceBuffer.isEmpty()
    
    /**
     * Check if buffer is full
     */
    fun isFull(): Boolean = experienceBuffer.size >= maxBufferSize
}

/**
 * Basic statistics for experience buffer
 */
data class ExperienceBufferStats(
    val bufferSize: Int,
    val maxBufferSize: Int,
    val utilization: Double,
    val totalAdded: Int,
    val totalRemoved: Int,
    val averageReward: Double
)

/**
 * Cleanup operation statistics
 */
data class CleanupStats(
    val experiencesRemoved: Int,
    val bufferSizeBefore: Int,
    val bufferSizeAfter: Int
)
