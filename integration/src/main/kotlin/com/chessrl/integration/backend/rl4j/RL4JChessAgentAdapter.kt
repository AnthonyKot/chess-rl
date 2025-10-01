package com.chessrl.integration.backend.rl4j

import com.chessrl.integration.ChessAgent
import com.chessrl.integration.ChessAgentConfig
import com.chessrl.integration.ChessAgentMetrics
import com.chessrl.rl.Experience
import com.chessrl.rl.PolicyUpdateResult

/**
 * Adapter that wraps RL4JChessAgent to provide consistent interface
 * with other backend implementations.
 */
class RL4JChessAgentAdapter(
    private val rl4jAgent: RL4JChessAgent,
    private val config: ChessAgentConfig
) : ChessAgent {
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        return rl4jAgent.selectAction(state, validActions)
    }
    
    override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        return rl4jAgent.trainBatch(experiences)
    }
    
    override fun save(path: String) {
        rl4jAgent.save(path)
    }
    
    override fun load(path: String) {
        rl4jAgent.load(path)
    }
    
    override fun getConfig(): ChessAgentConfig = config
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        rl4jAgent.learn(experience)
    }
    
    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return rl4jAgent.getQValues(state, actions)
    }
    
    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return rl4jAgent.getActionProbabilities(state, actions)
    }
    
    override fun getTrainingMetrics(): ChessAgentMetrics {
        return rl4jAgent.getTrainingMetrics()
    }
    
    override fun forceUpdate() {
        rl4jAgent.forceUpdate()
    }
    
    override fun reset() {
        rl4jAgent.reset()
    }
    
    override fun setExplorationRate(rate: Double) {
        rl4jAgent.setExplorationRate(rate)
    }
}