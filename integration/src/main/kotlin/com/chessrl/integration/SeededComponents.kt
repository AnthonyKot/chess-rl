package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.random.Random
import kotlin.math.exp

/**
 * Seeded wrappers aligned to current RL interfaces to support deterministic runs.
 * Minimal implementations that delegate to shared components while enforcing a provided Random.
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
            validActions[this.random.nextInt(validActions.size)]
        } else {
            validActions.maxByOrNull { actionValues[it] ?: Double.NEGATIVE_INFINITY }
                ?: validActions.first()
        }
    }

    override fun updateExploration(episode: Int) {
        epsilon = maxOf(minEpsilon, epsilon * epsilonDecay)
    }

    override fun getExplorationRate(): Double = epsilon

    fun setEpsilon(newEpsilon: Double) { epsilon = newEpsilon.coerceIn(0.0, 1.0) }
}

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

        val values = validActions.map { actionValues[it] ?: 0.0 }
        val maxValue = values.maxOrNull() ?: 0.0
        val expValues = values.map { exp((it - maxValue) / temperature) }
        val sumExp = expValues.sum()
        if (sumExp == 0.0) return validActions[this.random.nextInt(validActions.size)]

        val probabilities = expValues.map { it / sumExp }
        val r = this.random.nextDouble()
        var c = 0.0
        for (i in validActions.indices) {
            c += probabilities[i]
            if (r <= c) return validActions[i]
        }
        return validActions.last()
    }

    override fun updateExploration(episode: Int) {
        temperature = maxOf(minTemperature, temperature * temperatureDecay)
    }

    override fun getExplorationRate(): Double = temperature

    fun setTemperature(newTemperature: Double) { temperature = maxOf(0.01, newTemperature) }
}

class SeededCircularExperienceBuffer<S, A>(
    private val maxSize: Int,
    private val random: Random
) : ExperienceReplay<S, A> {
    private val delegate = CircularExperienceBuffer<S, A>(maxSize)
    override fun add(experience: Experience<S, A>) = delegate.add(experience)
    override fun sample(batchSize: Int, random: Random): List<Experience<S, A>> = delegate.sample(batchSize, this.random)
    override fun size(): Int = delegate.size()
    override fun capacity(): Int = delegate.capacity()
    override fun clear() = delegate.clear()
    override fun isFull(): Boolean = delegate.isFull()
}

class SeededPrioritizedExperienceBuffer<S, A>(
    private val maxSize: Int,
    private val random: Random,
    private val alpha: Double = 0.6,
    private val beta: Double = 0.4,
    private val betaIncrement: Double = 0.001,
    private val epsilon: Double = 1e-6
) : ExperienceReplay<S, A> {
    private val delegate = PrioritizedExperienceBuffer<S, A>(maxSize, alpha, beta, betaIncrement, epsilon)
    override fun add(experience: Experience<S, A>) = delegate.add(experience)
    override fun sample(batchSize: Int, random: Random): List<Experience<S, A>> = delegate.sample(batchSize, this.random)
    override fun size(): Int = delegate.size()
    override fun capacity(): Int = delegate.capacity()
    override fun clear() = delegate.clear()
    override fun isFull(): Boolean = delegate.isFull()
}

