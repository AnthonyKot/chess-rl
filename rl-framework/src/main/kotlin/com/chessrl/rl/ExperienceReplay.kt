package com.chessrl.rl

import kotlin.random.Random

/**
 * Interface for experience replay buffers
 */
interface ExperienceReplay<S, A> {
    fun add(experience: Experience<S, A>)
    fun sample(batchSize: Int, random: Random = Random.Default): List<Experience<S, A>>
    fun size(): Int
    fun capacity(): Int
    fun clear()
    fun isFull(): Boolean
}

/**
 * Simple circular buffer for experience replay
 * Stores experiences in a fixed-size buffer, overwriting oldest experiences when full
 */
class CircularExperienceBuffer<S, A>(
    private val maxSize: Int
) : ExperienceReplay<S, A> {
    
    private val buffer = mutableListOf<Experience<S, A>>()
    private var nextIndex = 0
    
    init {
        require(maxSize > 0) { "Buffer size must be positive" }
    }
    
    override fun add(experience: Experience<S, A>) {
        if (buffer.size < maxSize) {
            buffer.add(experience)
        } else {
            buffer[nextIndex] = experience
        }
        nextIndex = (nextIndex + 1) % maxSize
    }
    
    override fun sample(batchSize: Int, random: Random): List<Experience<S, A>> {
        require(batchSize > 0) { "Batch size must be positive" }
        require(buffer.isNotEmpty()) { "Cannot sample from empty buffer" }
        
        val actualBatchSize = minOf(batchSize, buffer.size)
        return buffer.shuffled(random).take(actualBatchSize)
    }
    
    override fun size(): Int = buffer.size
    
    override fun capacity(): Int = maxSize
    
    override fun clear() {
        buffer.clear()
        nextIndex = 0
    }
    
    override fun isFull(): Boolean = buffer.size >= maxSize
    
    /**
     * Get all experiences in the buffer (for testing/debugging)
     */
    fun getAllExperiences(): List<Experience<S, A>> = buffer.toList()
}

/**
 * Prioritized experience replay buffer
 * Samples experiences based on their temporal difference (TD) error priority
 * Simplified version without complex priority calculations for Kotlin/Native compatibility
 */
class PrioritizedExperienceBuffer<S, A>(
    private val maxSize: Int,
    private val alpha: Double = 0.6,  // Priority exponent
    private val beta: Double = 0.4,   // Importance sampling exponent
    private val betaIncrement: Double = 0.001,
    private val epsilon: Double = 1e-6  // Small constant to avoid zero priorities
) : ExperienceReplay<S, A> {
    
    private val buffer = mutableListOf<Experience<S, A>>()
    private val priorities = mutableListOf<Double>()
    private var nextIndex = 0
    private var currentBeta = beta
    
    init {
        require(maxSize > 0) { "Buffer size must be positive" }
        require(alpha >= 0.0) { "Alpha must be non-negative" }
        require(beta >= 0.0) { "Beta must be non-negative" }
    }
    
    override fun add(experience: Experience<S, A>) {
        val maxPriority = if (priorities.isEmpty()) 1.0 else priorities.maxOrNull() ?: 1.0
        
        if (buffer.size < maxSize) {
            buffer.add(experience)
            priorities.add(maxPriority)
        } else {
            buffer[nextIndex] = experience
            priorities[nextIndex] = maxPriority
        }
        nextIndex = (nextIndex + 1) % maxSize
    }
    
    override fun sample(batchSize: Int, random: Random): List<Experience<S, A>> {
        require(batchSize > 0) { "Batch size must be positive" }
        require(buffer.isNotEmpty()) { "Cannot sample from empty buffer" }
        
        val actualBatchSize = minOf(batchSize, buffer.size)
        
        // Simplified sampling - weight by priorities but without complex power calculations
        val totalPriority = priorities.sum()
        if (totalPriority == 0.0) {
            // Fallback to uniform sampling
            return buffer.shuffled(random).take(actualBatchSize)
        }
        
        val probabilities = priorities.map { (it + epsilon) / (totalPriority + epsilon * priorities.size) }
        
        val sampledExperiences = mutableListOf<Experience<S, A>>()
        
        repeat(actualBatchSize) {
            val randomValue = random.nextDouble()
            var cumulativeProbability = 0.0
            
            for (i in probabilities.indices) {
                cumulativeProbability += probabilities[i]
                if (randomValue <= cumulativeProbability) {
                    sampledExperiences.add(buffer[i])
                    break
                }
            }
        }
        
        // Update beta for importance sampling
        currentBeta = minOf(1.0, currentBeta + betaIncrement)
        
        return sampledExperiences
    }
    
    override fun size(): Int = buffer.size
    
    override fun capacity(): Int = maxSize
    
    override fun clear() {
        buffer.clear()
        priorities.clear()
        nextIndex = 0
    }
    
    override fun isFull(): Boolean = buffer.size >= maxSize
    
    /**
     * Update priorities for experiences (typically called after learning)
     */
    fun updatePriorities(indices: List<Int>, tdErrors: List<Double>) {
        require(indices.size == tdErrors.size) { "Indices and TD errors must have same size" }
        
        for (i in indices.indices) {
            val index = indices[i]
            if (index in 0 until priorities.size) {
                priorities[index] = kotlin.math.abs(tdErrors[i]) + epsilon
            }
        }
    }
    
    /**
     * Get importance sampling weights for the last sampled batch
     * Simplified version without complex power calculations
     */
    fun getImportanceSamplingWeights(sampledIndices: List<Int>): List<Double> {
        // Simplified importance sampling weights
        val maxPriority = priorities.maxOrNull() ?: 1.0
        return sampledIndices.map { index ->
            val priority = priorities.getOrNull(index) ?: 1.0
            val weight = maxPriority / (priority + epsilon)
            weight
        }
    }
}