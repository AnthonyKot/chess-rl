package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.random.Random

/**
 * Experience replay buffer interface for RL algorithms
 */
interface ExperienceReplay<S, A> {
    fun add(experience: Experience<S, A>)
    fun sample(batchSize: Int): List<Experience<S, A>>
    fun size(): Int
    fun clear()
}

/**
 * Circular buffer implementation for experience replay
 */
class CircularExperienceBuffer<S, A>(
    private val maxSize: Int
) : ExperienceReplay<S, A> {
    
    private val buffer = mutableListOf<Experience<S, A>>()
    private var currentIndex = 0
    private var isFull = false
    
    override fun add(experience: Experience<S, A>) {
        if (buffer.size < maxSize) {
            buffer.add(experience)
        } else {
            buffer[currentIndex] = experience
            currentIndex = (currentIndex + 1) % maxSize
            isFull = true
        }
    }
    
    override fun sample(batchSize: Int): List<Experience<S, A>> {
        require(batchSize > 0) { "Batch size must be positive" }
        require(size() >= batchSize) { "Not enough experiences in buffer. Have ${size()}, need $batchSize" }
        
        val availableExperiences = if (isFull) maxSize else buffer.size
        val indices = (0 until availableExperiences).shuffled(Random).take(batchSize)
        
        return indices.map { buffer[it] }
    }
    
    override fun size(): Int = if (isFull) maxSize else buffer.size
    
    override fun clear() {
        buffer.clear()
        currentIndex = 0
        isFull = false
    }
}

/**
 * Prioritized experience replay buffer (simplified implementation)
 */
class PrioritizedExperienceBuffer<S, A>(
    private val maxSize: Int,
    private val alpha: Double = 0.6,
    private val beta: Double = 0.4
) : ExperienceReplay<S, A> {
    
    private val buffer = mutableListOf<Experience<S, A>>()
    private val priorities = mutableListOf<Double>()
    private var currentIndex = 0
    private var isFull = false
    private var maxPriority = 1.0
    
    override fun add(experience: Experience<S, A>) {
        val priority = maxPriority
        
        if (buffer.size < maxSize) {
            buffer.add(experience)
            priorities.add(priority)
        } else {
            buffer[currentIndex] = experience
            priorities[currentIndex] = priority
            currentIndex = (currentIndex + 1) % maxSize
            isFull = true
        }
    }
    
    override fun sample(batchSize: Int): List<Experience<S, A>> {
        require(batchSize > 0) { "Batch size must be positive" }
        require(size() >= batchSize) { "Not enough experiences in buffer. Have ${size()}, need $batchSize" }
        
        val availableSize = size()
        val totalPriority = priorities.take(availableSize).sum()
        
        val indices = mutableListOf<Int>()
        repeat(batchSize) {
            val randomValue = Random.nextDouble() * totalPriority
            var cumulativePriority = 0.0
            
            for (i in 0 until availableSize) {
                cumulativePriority += priorities[i]
                if (randomValue <= cumulativePriority) {
                    indices.add(i)
                    break
                }
            }
        }
        
        return indices.map { buffer[it] }
    }
    
    override fun size(): Int = if (isFull) maxSize else buffer.size
    
    override fun clear() {
        buffer.clear()
        priorities.clear()
        currentIndex = 0
        isFull = false
        maxPriority = 1.0
    }
    
    /**
     * Update priorities for sampled experiences
     */
    fun updatePriorities(indices: List<Int>, newPriorities: List<Double>) {
        require(indices.size == newPriorities.size) { "Indices and priorities must have same size" }
        
        for (i in indices.indices) {
            val index = indices[i]
            val priority = newPriorities[i].coerceAtLeast(1e-6) // Minimum priority
            priorities[index] = priority
            maxPriority = maxOf(maxPriority, priority)
        }
    }
}

/**
 * Exploration strategy interface for action selection
 */
interface ExplorationStrategy<A> {
    fun selectAction(validActions: List<A>, actionValues: Map<A, Double>): A
    fun updateExploration(episode: Int)
    fun getExplorationRate(): Double
}

/**
 * Epsilon-greedy exploration strategy
 */
class EpsilonGreedyStrategy<A>(
    private var epsilon: Double,
    private val epsilonDecay: Double = 0.995,
    private val minEpsilon: Double = 0.01
) : ExplorationStrategy<A> {
    
    override fun selectAction(validActions: List<A>, actionValues: Map<A, Double>): A {
        require(validActions.isNotEmpty()) { "Valid actions cannot be empty" }
        
        return if (Random.nextDouble() < epsilon) {
            // Explore: random action
            validActions.random()
        } else {
            // Exploit: best action
            validActions.maxByOrNull { actionValues[it] ?: Double.NEGATIVE_INFINITY }
                ?: validActions.first()
        }
    }
    
    override fun updateExploration(episode: Int) {
        epsilon = (epsilon * epsilonDecay).coerceAtLeast(minEpsilon)
    }
    
    override fun getExplorationRate(): Double = epsilon
    
    fun setEpsilon(newEpsilon: Double) {
        epsilon = newEpsilon.coerceIn(0.0, 1.0)
    }
}

/**
 * Boltzmann (softmax) exploration strategy
 */
class BoltzmannStrategy<A>(
    private var temperature: Double,
    private val temperatureDecay: Double = 0.995,
    private val minTemperature: Double = 0.1
) : ExplorationStrategy<A> {
    
    override fun selectAction(validActions: List<A>, actionValues: Map<A, Double>): A {
        require(validActions.isNotEmpty()) { "Valid actions cannot be empty" }
        
        if (validActions.size == 1) {
            return validActions.first()
        }
        
        // Calculate softmax probabilities
        val values = validActions.map { actionValues[it] ?: 0.0 }
        val maxValue = values.maxOrNull() ?: 0.0
        val expValues = values.map { kotlin.math.exp((it - maxValue) / temperature) }
        val sumExp = expValues.sum()
        
        if (sumExp == 0.0) {
            return validActions.random()
        }
        
        val probabilities = expValues.map { it / sumExp }
        
        // Sample from probability distribution
        val randomValue = Random.nextDouble()
        var cumulativeProbability = 0.0
        
        for (i in validActions.indices) {
            cumulativeProbability += probabilities[i]
            if (randomValue <= cumulativeProbability) {
                return validActions[i]
            }
        }
        
        return validActions.last()
    }
    
    override fun updateExploration(episode: Int) {
        temperature = (temperature * temperatureDecay).coerceAtLeast(minTemperature)
    }
    
    override fun getExplorationRate(): Double = temperature
    
    fun setTemperature(newTemperature: Double) {
        temperature = newTemperature.coerceAtLeast(0.01)
    }
}

/**
 * Upper Confidence Bound (UCB) exploration strategy
 */
class UCBStrategy<A>(
    private val c: Double = 2.0
) : ExplorationStrategy<A> {
    
    private val actionCounts = mutableMapOf<A, Int>()
    private val actionValues = mutableMapOf<A, Double>()
    private var totalCount = 0
    
    override fun selectAction(validActions: List<A>, actionValues: Map<A, Double>): A {
        require(validActions.isNotEmpty()) { "Valid actions cannot be empty" }
        
        totalCount++
        
        // Initialize unseen actions
        for (action in validActions) {
            if (action !in actionCounts) {
                actionCounts[action] = 0
                this.actionValues[action] = 0.0
            }
        }
        
        // Select action with highest UCB value
        return validActions.maxByOrNull { action ->
            val count = actionCounts[action] ?: 0
            val value = this.actionValues[action] ?: 0.0
            
            if (count == 0) {
                Double.POSITIVE_INFINITY // Explore unseen actions first
            } else {
                value + c * kotlin.math.sqrt(kotlin.math.ln(totalCount.toDouble()) / count)
            }
        } ?: validActions.first()
    }
    
    override fun updateExploration(episode: Int) {
        // UCB doesn't need episode-based updates
    }
    
    override fun getExplorationRate(): Double {
        // Return average exploration based on action distribution
        val totalActions = actionCounts.values.sum()
        return if (totalActions > 0) {
            1.0 - (actionCounts.values.maxOrNull() ?: 0).toDouble() / totalActions
        } else {
            1.0
        }
    }
    
    /**
     * Update action value estimates
     */
    fun updateActionValue(action: A, reward: Double) {
        val count = actionCounts[action] ?: 0
        val oldValue = actionValues[action] ?: 0.0
        
        actionCounts[action] = count + 1
        actionValues[action] = oldValue + (reward - oldValue) / (count + 1)
    }
}