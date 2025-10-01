package com.chessrl.rl

import kotlin.random.Random

/**
 * Interface for exploration strategies in reinforcement learning
 */
interface ExplorationStrategy<A> {
    fun selectAction(
        validActions: List<A>,
        actionValues: Map<A, Double>,
        random: Random = Random.Default
    ): A
    
    fun updateExploration(episode: Int)
    fun getExplorationRate(): Double
}

/**
 * Epsilon-greedy exploration strategy
 * Selects random action with probability epsilon, otherwise selects greedy action
 */
class EpsilonGreedyStrategy<A>(
    private var epsilon: Double = 0.1,
    private val epsilonDecay: Double = 0.995,
    private val minEpsilon: Double = 0.01
) : ExplorationStrategy<A> {
    
    override fun selectAction(
        validActions: List<A>,
        actionValues: Map<A, Double>,
        random: Random
    ): A {
        require(validActions.isNotEmpty()) { "Valid actions list cannot be empty" }
        
        return if (random.nextDouble() < epsilon) {
            // Explore: select random action
            validActions[random.nextInt(validActions.size)]
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
 * Boltzmann (softmax) exploration strategy
 * Selects actions based on their values using softmax probability distribution
 */
class BoltzmannStrategy<A>(
    private var temperature: Double = 1.0,
    private val temperatureDecay: Double = 0.995,
    private val minTemperature: Double = 0.1
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
            return validActions[random.nextInt(validActions.size)]
        }
        
        val probabilities = expValues.map { it / sumExp }
        
        // Sample from probability distribution
        val randomValue = random.nextDouble()
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