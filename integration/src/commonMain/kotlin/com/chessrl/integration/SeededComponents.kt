package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.random.Random

/**
 * Seeded exploration strategies that use deterministic random generators
 */

/**
 * Seeded epsilon-greedy exploration strategy
 */
class SeededEpsilonGreedyStrategy<A>(
    private var epsilon: Double = 0.1,
    private val epsilonDecay: Double = 0.995,
    private val minEpsilon: Double = 0.01,
    private val random: Random
) : ExplorationStrategy<A> {
    
    override fun selectAction(
        validActions: List<A>,
        actionValues: Map<A, Double>,
        random: Random
    ): A {
        require(validActions.isNotEmpty()) { "Valid actions list cannot be empty" }
        
        return if (this.random.nextDouble() < epsilon) {
            // Explore: select random action using seeded random
            validActions[this.random.nextInt(validActions.size)]
        } else {
            // Exploit: select action with highest value
            validActions.maxByOrNull { actionValues[it] ?: Double.NEGATIVE_INFINITY }
                ?: validActions.first()
        }
    }
    
    override fun updateExploration(episode: Int) {
        epsilon = maxOf(minEpsilon, epsilon * epsilonDecay)
    }
    
    override fun getExplorationRate(): Double = epsilon
    
    fun setEpsilon(newEpsilon: Double) {
        epsilon = newEpsilon.coerceIn(0.0, 1.0)
    }
}

/**
 * Seeded Boltzmann (softmax) exploration strategy
 */
class SeededBoltzmannStrategy<A>(
    private var temperature: Double = 1.0,
    private val temperatureDecay: Double = 0.995,
    private val minTemperature: Double = 0.1,
    private val random: Random
) : ExplorationStrategy<A> {
    
    override fun selectAction(
        validActions: List<A>,
        actionValues: Map<A, Double>,
        random: Random
    ): A {
        require(validActions.isNotEmpty()) { "Valid actions list cannot be empty" }
        
        if (validActions.size == 1) return validActions.first()
        
        // Calculate softmax probabilities
        val values = validActions.map { actionValues[it] ?: 0.0 }
        val maxValue = values.maxOrNull() ?: 0.0
        
        // Subtract max for numerical stability
        val expValues = values.map { kotlin.math.exp((it - maxValue) / temperature) }
        val sumExp = expValues.sum()
        
        if (sumExp == 0.0) {
            // Fallback to random selection if all probabilities are zero
            return validActions[this.random.nextInt(validActions.size)]
        }
        
        val probabilities = expValues.map { it / sumExp }
        
        // Sample from probability distribution using seeded random
        val randomValue = this.random.nextDouble()
        var cumulativeProbability = 0.0
        
        for (i in validActions.indices) {
            cumulativeProbability += probabilities[i]
            if (randomValue <= cumulativeProbability) {
                return validActions[i]
            }
        }
        
        // Fallback (should not reach here)
        return validActions.last()
    }
    
    override fun updateExploration(episode: Int) {
        temperature = maxOf(minTemperature, temperature * temperatureDecay)
    }
    
    override fun getExplorationRate(): Double = temperature
    
    fun setTemperature(newTemperature: Double) {
        temperature = maxOf(0.01, newTemperature)
    }
}

/**
 * Seeded circular experience buffer for deterministic replay sampling
 */
class SeededCircularExperienceBuffer<S, A>(
    private val maxSize: Int,
    private val random: Random
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
        
        // Use seeded random for deterministic sampling
        return buffer.shuffled(this.random).take(actualBatchSize)
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
    
    /**
     * Sample with specific sampling strategy using seeded random
     */
    fun sampleWithStrategy(
        batchSize: Int, 
        strategy: SamplingStrategy = SamplingStrategy.UNIFORM
    ): List<Experience<S, A>> {
        require(batchSize > 0) { "Batch size must be positive" }
        require(buffer.isNotEmpty()) { "Cannot sample from empty buffer" }
        
        val actualBatchSize = minOf(batchSize, buffer.size)
        
        return when (strategy) {
            SamplingStrategy.UNIFORM -> {
                buffer.shuffled(random).take(actualBatchSize)
            }
            SamplingStrategy.RECENT -> {
                // Sample more recent experiences with higher probability
                val recentCount = minOf(actualBatchSize, buffer.size / 2)
                val recentExperiences = buffer.takeLast(recentCount)
                val remainingCount = actualBatchSize - recentCount
                
                if (remainingCount > 0) {
                    val olderExperiences = buffer.dropLast(recentCount).shuffled(random).take(remainingCount)
                    recentExperiences + olderExperiences
                } else {
                    recentExperiences.shuffled(random)
                }
            }
            SamplingStrategy.MIXED -> {
                // Mix of recent and uniform sampling
                val recentCount = actualBatchSize / 3
                val uniformCount = actualBatchSize - recentCount
                
                val recentExperiences = buffer.takeLast(recentCount)
                val uniformExperiences = buffer.shuffled(random).take(uniformCount)
                
                (recentExperiences + uniformExperiences).shuffled(random)
            }
        }
    }
}

/**
 * Sampling strategies for experience replay
 */
enum class SamplingStrategy {
    UNIFORM,
    RECENT,
    MIXED
}

/**
 * Seeded prioritized experience replay buffer
 */
class SeededPrioritizedExperienceBuffer<S, A>(
    private val maxSize: Int,
    private val alpha: Double = 0.6,
    private val beta: Double = 0.4,
    private val betaIncrement: Double = 0.001,
    private val epsilon: Double = 1e-6,
    private val random: Random
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
        
        // Simplified sampling using seeded random - weight by priorities
        val totalPriority = priorities.sum()
        if (totalPriority == 0.0) {
            // Fallback to uniform sampling
            return buffer.shuffled(this.random).take(actualBatchSize)
        }
        
        val probabilities = priorities.map { (it + epsilon) / (totalPriority + epsilon * priorities.size) }
        
        val sampledExperiences = mutableListOf<Experience<S, A>>()
        
        repeat(actualBatchSize) {
            val randomValue = this.random.nextDouble()
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
     */
    fun getImportanceSamplingWeights(sampledIndices: List<Int>): List<Double> {
        val maxPriority = priorities.maxOrNull() ?: 1.0
        return sampledIndices.map { index ->
            val priority = priorities.getOrNull(index) ?: 1.0
            val weight = maxPriority / (priority + epsilon)
            weight
        }
    }
}