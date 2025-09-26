package com.chessrl.integration.backend

import com.chessrl.integration.*
import com.chessrl.rl.*

/**
 * Adapter that wraps Dl4jChessAgent to implement the ChessAgent interface
 */
class Dl4jChessAgentAdapter(
    private val dl4jAgent: Dl4jChessAgent,
    private val config: ChessAgentConfig
) : ChessAgent {
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val proposed = dl4jAgent.selectAction(state, validActions)
        if (proposed in validActions) return proposed
        // Strict masking: fall back to a legal action only
        return try {
            val probs = dl4jAgent.getActionProbabilities(state, validActions)
            if (probs.isNotEmpty()) {
                // Pick highest-prob legal action
                probs.maxByOrNull { it.value }?.key ?: validActions.first()
            } else {
                val q = dl4jAgent.getQValues(state, validActions)
                if (q.isNotEmpty()) q.maxByOrNull { it.value }?.key ?: validActions.first() else validActions.first()
            }
        } catch (_: Throwable) {
            validActions.first()
        }
    }
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        dl4jAgent.learn(experience)
    }
    
    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return dl4jAgent.getQValues(state, actions)
    }
    
    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return dl4jAgent.getActionProbabilities(state, actions)
    }
    
    override fun getTrainingMetrics(): ChessAgentMetrics {
        val simpleMetrics = dl4jAgent.getTrainingMetrics()
        val last = dl4jAgent.getLastUpdate()
        return ChessAgentMetrics(
            averageReward = simpleMetrics.averageReward,
            explorationRate = simpleMetrics.explorationRate,
            experienceBufferSize = simpleMetrics.experienceBufferSize,
            averageLoss = last?.loss ?: 0.0,
            policyEntropy = last?.policyEntropy ?: 0.0
        )
    }
    
    override fun forceUpdate() {
        dl4jAgent.forceUpdate()
    }

    override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        return dl4jAgent.trainBatch(experiences)
    }
    
    override fun save(path: String) {
        dl4jAgent.save(path)
    }
    
    override fun load(path: String) {
        dl4jAgent.load(path)
    }
    
    override fun reset() {
        dl4jAgent.reset()
    }
    
    override fun setExplorationRate(rate: Double) {
        dl4jAgent.setExplorationRate(rate)
    }
    
    override fun getConfig(): ChessAgentConfig {
        return config
    }

    override fun setNextActionProvider(provider: (DoubleArray) -> List<Int>) {
        dl4jAgent.setNextActionProvider(provider)
    }
}

/**
 * Adapter that wraps SeededDl4jChessAgent to implement the ChessAgent interface
 */
class SeededDl4jChessAgentAdapter(
    private val dl4jAgent: SeededDl4jChessAgent,
    private val config: ChessAgentConfig
) : ChessAgent {
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val proposed = dl4jAgent.selectAction(state, validActions)
        if (proposed in validActions) return proposed
        // Strict masking: fall back to a legal action only
        return try {
            val probs = dl4jAgent.getActionProbabilities(state, validActions)
            if (probs.isNotEmpty()) {
                // Pick highest-prob legal action
                probs.maxByOrNull { it.value }?.key ?: validActions.first()
            } else {
                val q = dl4jAgent.getQValues(state, validActions)
                if (q.isNotEmpty()) q.maxByOrNull { it.value }?.key ?: validActions.first() else validActions.first()
            }
        } catch (_: Throwable) {
            validActions.first()
        }
    }
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        dl4jAgent.learn(experience)
    }
    
    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return dl4jAgent.getQValues(state, actions)
    }
    
    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return dl4jAgent.getActionProbabilities(state, actions)
    }
    
    override fun getTrainingMetrics(): ChessAgentMetrics {
        val simpleMetrics = dl4jAgent.getTrainingMetrics()
        val last = dl4jAgent.getLastUpdate()
        return ChessAgentMetrics(
            averageReward = simpleMetrics.averageReward,
            explorationRate = simpleMetrics.explorationRate,
            experienceBufferSize = simpleMetrics.experienceBufferSize,
            averageLoss = last?.loss ?: 0.0,
            policyEntropy = last?.policyEntropy ?: 0.0
        )
    }
    
    override fun forceUpdate() {
        dl4jAgent.forceUpdate()
    }

    override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        return dl4jAgent.trainBatch(experiences)
    }
    
    override fun save(path: String) {
        dl4jAgent.save(path)
    }
    
    override fun load(path: String) {
        dl4jAgent.load(path)
    }
    
    override fun reset() {
        dl4jAgent.reset()
    }
    
    override fun setExplorationRate(rate: Double) {
        dl4jAgent.setExplorationRate(rate)
    }
    
    override fun getConfig(): ChessAgentConfig {
        return config
    }

    override fun setNextActionProvider(provider: (DoubleArray) -> List<Int>) {
        dl4jAgent.setNextActionProvider(provider)
    }
}