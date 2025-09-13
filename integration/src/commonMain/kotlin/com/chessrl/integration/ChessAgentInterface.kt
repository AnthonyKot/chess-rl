package com.chessrl.integration

import com.chessrl.rl.*

/**
 * Interface for chess RL agents used by the training control system
 */
interface ChessAgent {
    // Core RL operations
    fun selectAction(state: DoubleArray, validActions: List<Int>): Int
    fun learn(experience: Experience<DoubleArray, Int>)
    
    // Analysis and inspection
    fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double>
    fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double>
    
    // Training management
    fun getTrainingMetrics(): RLMetrics
    fun forceUpdate()
    
    // Model persistence
    fun save(path: String)
    fun load(path: String)
    
    // Additional methods for chess-specific functionality
    fun reset() {
        // Default implementation - can be overridden
    }
    
    fun setExplorationRate(rate: Double) {
        // Default implementation - can be overridden
    }
    
    fun completeEpisodeManually(reason: EpisodeTerminationReason = EpisodeTerminationReason.MANUAL) {
        // Default implementation - can be overridden
    }
    
    // Extended analysis methods for debugging and validation
    fun getPolicyOutput(state: DoubleArray): DoubleArray {
        // Default implementation using action probabilities
        val validActions = (0 until ChessActionEncoder.ACTION_SPACE_SIZE).toList()
        val probabilities = getActionProbabilities(state, validActions)
        return DoubleArray(ChessActionEncoder.ACTION_SPACE_SIZE) { action ->
            probabilities[action] ?: 0.0
        }
    }
    
    fun getValueOutput(state: DoubleArray): DoubleArray {
        // Default implementation using Q-values
        val validActions = (0 until ChessActionEncoder.ACTION_SPACE_SIZE).toList()
        val qValues = getQValues(state, validActions)
        val maxQ = qValues.values.maxOrNull() ?: 0.0
        return doubleArrayOf(maxQ)
    }
}

/**
 * Adapter to make the existing ChessAgent class implement the interface
 */
class ChessAgentAdapter(
    private val agent: com.chessrl.integration.ChessAgent
) : ChessAgent {
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        return agent.selectAction(state, validActions)
    }
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        agent.learn(experience)
    }
    
    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return agent.getQValues(state, actions)
    }
    
    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return agent.getActionProbabilities(state, actions)
    }
    
    override fun getTrainingMetrics(): RLMetrics {
        val chessMetrics = agent.getTrainingMetrics()
        return RLMetrics(
            episode = chessMetrics.episodeCount,
            averageReward = chessMetrics.averageReward,
            episodeLength = chessMetrics.episodeLength.toDouble(),
            explorationRate = chessMetrics.explorationRate,
            policyLoss = 0.0, // Would need to be tracked in ChessAgent
            policyEntropy = 0.0, // Would need to be tracked in ChessAgent
            gradientNorm = 0.0 // Would need to be tracked in ChessAgent
        )
    }
    
    override fun forceUpdate() {
        agent.forceUpdate()
    }
    
    override fun save(path: String) {
        agent.save(path)
    }
    
    override fun load(path: String) {
        agent.load(path)
    }
    
    override fun reset() {
        agent.reset()
    }
    
    override fun setExplorationRate(rate: Double) {
        agent.setExplorationRate(rate)
    }
    
    override fun completeEpisodeManually(reason: EpisodeTerminationReason) {
        agent.completeEpisodeManually(reason)
    }
    
    override fun getPolicyOutput(state: DoubleArray): DoubleArray {
        val validActions = (0 until ChessActionEncoder.ACTION_SPACE_SIZE).toList()
        val probabilities = agent.getActionProbabilities(state, validActions)
        return DoubleArray(ChessActionEncoder.ACTION_SPACE_SIZE) { action ->
            probabilities[action] ?: 0.0
        }
    }
    
    override fun getValueOutput(state: DoubleArray): DoubleArray {
        val validActions = (0 until ChessActionEncoder.ACTION_SPACE_SIZE).toList()
        val qValues = agent.getQValues(state, validActions)
        val maxQ = qValues.values.maxOrNull() ?: 0.0
        return doubleArrayOf(maxQ)
    }
}

/**
 * Enhanced RLMetrics for chess training
 */
data class RLMetrics(
    val episode: Int = 0,
    val averageReward: Double,
    val episodeLength: Double,
    val explorationRate: Double,
    val policyLoss: Double,
    val policyEntropy: Double,
    val gradientNorm: Double,
    val experienceBufferSize: Int = 0
)

/**
 * Adapter for the real chess agent implementation
 */
class RealChessAgentAdapter(
    private val realAgent: RealChessAgent
) : ChessAgent {
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        return realAgent.selectAction(state, validActions)
    }
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        realAgent.learn(experience)
    }
    
    override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return realAgent.getQValues(state, actions)
    }
    
    override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
        return realAgent.getActionProbabilities(state, actions)
    }
    
    override fun getTrainingMetrics(): RLMetrics {
        val metrics = realAgent.getTrainingMetrics()
        return RLMetrics(
            episode = 0, // Would need to be tracked
            averageReward = metrics.averageReward,
            episodeLength = 0.0, // Would need to be tracked
            explorationRate = metrics.explorationRate,
            policyLoss = 0.0, // Would need to be tracked
            policyEntropy = 0.0, // Would need to be tracked
            gradientNorm = 0.0, // Would need to be tracked
            experienceBufferSize = metrics.experienceBufferSize
        )
    }
    
    override fun forceUpdate() {
        realAgent.forceUpdate()
    }
    
    override fun save(path: String) {
        realAgent.save(path)
    }
    
    override fun load(path: String) {
        realAgent.load(path)
    }
    
    override fun reset() {
        realAgent.reset()
    }
    
    override fun setExplorationRate(rate: Double) {
        realAgent.setExplorationRate(rate)
    }
}