package com.chessrl.integration

import com.chessrl.rl.*

/**
 * Unified interface for chess agents that bridges the gap between
 * the RL framework and the chess-specific implementations
 */
interface ChessAgent {
    
    /**
     * Select an action given the current state and valid actions
     */
    fun selectAction(state: DoubleArray, validActions: List<Int>): Int
    
    /**
     * Learn from an experience
     */
    fun learn(experience: Experience<DoubleArray, Int>)
    
    /**
     * Get Q-values for state-action pairs
     */
    fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double>
    
    /**
     * Get action probabilities for policy-based agents
     */
    fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double>
    
    /**
     * Get current training metrics
     */
    fun getTrainingMetrics(): ChessAgentMetrics
    
    /**
     * Force a policy update (useful for batch training)
     */
    fun forceUpdate()
    
    /**
     * Save agent state to file
     */
    fun save(path: String)
    
    /**
     * Load agent state from file
     */
    fun load(path: String)
    
    /**
     * Reset agent state
     */
    fun reset()
    
    /**
     * Set exploration rate (for epsilon-greedy strategies)
     */
    fun setExplorationRate(rate: Double)
    
    /**
     * Get agent configuration
     */
    fun getConfig(): ChessAgentConfig
}

/**
 * Configuration for chess agents
 */
data class ChessAgentConfig(
    val batchSize: Int = 32,
    val maxBufferSize: Int = 10000,
    val learningRate: Double = 0.001,
    val explorationRate: Double = 0.1,
    val targetUpdateFrequency: Int = 100,
    val gamma: Double = 0.99
)

/**
 * Training metrics for chess agents
 */
data class ChessAgentMetrics(
    val averageReward: Double,
    val explorationRate: Double,
    val experienceBufferSize: Int,
    val episodeCount: Int = 0,
    val totalReward: Double = 0.0,
    val averageLoss: Double = 0.0,
    val policyEntropy: Double = 0.0
)

/**
 * Agent types supported by the factory
 */
enum class AgentType {
    DQN,
    POLICY_GRADIENT,
    ACTOR_CRITIC
}