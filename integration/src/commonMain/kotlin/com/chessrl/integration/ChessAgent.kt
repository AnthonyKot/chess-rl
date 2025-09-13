package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Chess-specific RL agent that uses neural networks for move selection
 * This agent integrates the neural network package with the RL framework
 * to enable learning chess through reinforcement learning.
 */
class ChessAgent(
    private val neuralNetwork: com.chessrl.rl.NeuralNetwork,
    private val algorithm: RLAlgorithm<DoubleArray, Int>,
    private val explorationStrategy: ExplorationStrategy<Int>,
    private val config: ChessAgentConfig = ChessAgentConfig()
) : Agent<DoubleArray, Int> {
    
    // Experience buffer for collecting training data
    private val experienceBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    
    // Training metrics tracking
    private var episodeCount = 0
    private var totalReward = 0.0
    private var episodeReward = 0.0
    private var episodeLength = 0
    
    // Episode termination tracking
    private var gameEndedEpisodes = 0
    private var stepLimitEpisodes = 0
    private var manualEpisodes = 0
    
    // Performance tracking
    private val recentPerformance = mutableListOf<Double>()
    private var bestPerformance = Double.NEGATIVE_INFINITY
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        require(state.size == ChessStateEncoder.TOTAL_FEATURES) { 
            "State size ${state.size} doesn't match expected ${ChessStateEncoder.TOTAL_FEATURES}" 
        }
        require(validActions.isNotEmpty()) { "Valid actions list cannot be empty" }
        
        // Get Q-values or action probabilities from neural network
        val networkOutput = neuralNetwork.forward(state)
        
        // Create action-value mapping for valid actions only
        val actionValues = validActions.associateWith { action ->
            if (action < networkOutput.size) {
                networkOutput[action]
            } else {
                0.0 // Default value for out-of-bounds actions
            }
        }
        
        // Use exploration strategy to select action
        val selectedAction = explorationStrategy.selectAction(validActions, actionValues)
        
        // Validate selected action
        require(validActions.contains(selectedAction)) { 
            "Selected action $selectedAction not in valid actions list" 
        }
        
        return selectedAction
    }
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        // Add experience to buffer
        experienceBuffer.add(experience)
        
        // Update episode tracking
        episodeReward += experience.reward
        episodeLength++
        
        // Learn from experience buffer when we have enough data or episode ends
        if (experience.done || experienceBuffer.size >= config.batchSize) {
            performPolicyUpdate()
            
            if (experience.done) {
                completeEpisode(EpisodeTerminationReason.GAME_ENDED)
            }
        }
    }
    
    /**
     * Complete episode manually (e.g., when hitting step limits)
     */
    fun completeEpisodeManually(reason: EpisodeTerminationReason = EpisodeTerminationReason.STEP_LIMIT) {
        // Always complete episode when called manually, even if no steps recorded
        // This handles cases where external systems (like training pipelines) manage episodes
        completeEpisode(reason)
    }
    
    /**
     * Perform policy update using collected experiences
     */
    private fun performPolicyUpdate() {
        if (experienceBuffer.isEmpty()) return
        
        try {
            // Update policy using the RL algorithm
            val updateResult = algorithm.updatePolicy(experienceBuffer.toList())
            
            // Validate the update result
            validatePolicyUpdate(updateResult)
            
            // Clear buffer for on-policy methods (like REINFORCE)
            if (algorithm is PolicyGradientAlgorithm) {
                experienceBuffer.clear()
            } else if (experienceBuffer.size >= config.maxBufferSize) {
                // For off-policy methods, limit buffer size
                val excessCount = experienceBuffer.size - config.maxBufferSize
                repeat(excessCount) {
                    experienceBuffer.removeAt(0)
                }
            }
            
        } catch (e: Exception) {
            println("Warning: Policy update failed: ${e.message}")
            // Clear buffer to prevent repeated failures
            experienceBuffer.clear()
        }
    }
    
    /**
     * Complete episode and update metrics
     */
    private fun completeEpisode(reason: EpisodeTerminationReason) {
        episodeCount++
        totalReward += episodeReward
        
        // Track termination reason
        when (reason) {
            EpisodeTerminationReason.GAME_ENDED -> gameEndedEpisodes++
            EpisodeTerminationReason.STEP_LIMIT -> stepLimitEpisodes++
            EpisodeTerminationReason.MANUAL -> manualEpisodes++
        }
        
        // Track recent performance
        recentPerformance.add(episodeReward)
        if (recentPerformance.size > config.performanceWindowSize) {
            recentPerformance.removeAt(0)
        }
        
        // Update best performance
        if (episodeReward > bestPerformance) {
            bestPerformance = episodeReward
        }
        
        // Update exploration strategy
        explorationStrategy.updateExploration(episodeCount)
        
        // Reset episode tracking
        episodeReward = 0.0
        episodeLength = 0
        
        // Print progress periodically
        if (episodeCount % config.progressReportInterval == 0) {
            printProgress()
        }
    }
    
    /**
     * Validate policy update results
     */
    private fun validatePolicyUpdate(updateResult: PolicyUpdateResult) {
        // Check for numerical issues
        if (updateResult.loss.isNaN() || updateResult.loss.isInfinite()) {
            throw IllegalStateException("Invalid loss value: ${updateResult.loss}")
        }
        
        if (updateResult.gradientNorm.isNaN() || updateResult.gradientNorm.isInfinite()) {
            throw IllegalStateException("Invalid gradient norm: ${updateResult.gradientNorm}")
        }
        
        // Warn about potential training issues
        if (updateResult.gradientNorm > config.gradientClipThreshold) {
            println("Warning: Large gradient norm detected: ${updateResult.gradientNorm}")
        }
        
        if (updateResult.policyEntropy < config.minPolicyEntropy) {
            println("Warning: Low policy entropy detected: ${updateResult.policyEntropy}")
        }
    }
    
    /**
     * Print training progress
     */
    private fun printProgress() {
        val averageReward = totalReward / episodeCount
        val recentAverageReward = if (recentPerformance.isNotEmpty()) {
            recentPerformance.average()
        } else {
            0.0
        }
        val explorationRate = explorationStrategy.getExplorationRate()
        
        println("Episode $episodeCount:")
        println("  Average Reward: ${averageReward}")
        println("  Recent Average: ${recentAverageReward}")
        println("  Best Performance: ${bestPerformance}")
        println("  Exploration Rate: ${explorationRate}")
        println("  Experience Buffer: ${experienceBuffer.size}")
    }
    
    /**
     * Get current training metrics
     */
    fun getTrainingMetrics(): ChessAgentMetrics {
        val averageReward = if (episodeCount > 0) totalReward / episodeCount else 0.0
        val recentAverageReward = if (recentPerformance.isNotEmpty()) {
            recentPerformance.average()
        } else {
            0.0
        }
        
        return ChessAgentMetrics(
            episodeCount = episodeCount,
            averageReward = averageReward,
            recentAverageReward = recentAverageReward,
            bestPerformance = bestPerformance,
            explorationRate = explorationStrategy.getExplorationRate(),
            experienceBufferSize = experienceBuffer.size,
            episodeLength = episodeLength,
            gameEndedEpisodes = gameEndedEpisodes,
            stepLimitEpisodes = stepLimitEpisodes,
            manualEpisodes = manualEpisodes
        )
    }
    
    /**
     * Reset agent state for new training session
     */
    fun reset() {
        experienceBuffer.clear()
        episodeCount = 0
        totalReward = 0.0
        episodeReward = 0.0
        episodeLength = 0
        gameEndedEpisodes = 0
        stepLimitEpisodes = 0
        manualEpisodes = 0
        recentPerformance.clear()
        bestPerformance = Double.NEGATIVE_INFINITY
    }
    
    /**
     * Set exploration rate manually
     */
    fun setExplorationRate(rate: Double) {
        if (explorationStrategy is EpsilonGreedyStrategy) {
            explorationStrategy.setEpsilon(rate)
        }
    }
    
    /**
     * Get action probabilities for analysis
     */
    fun getActionProbabilities(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        val networkOutput = neuralNetwork.forward(state)
        
        // Apply softmax to get probabilities
        val validOutputs = validActions.map { action ->
            if (action < networkOutput.size) networkOutput[action] else Double.NEGATIVE_INFINITY
        }
        
        val maxOutput = validOutputs.maxOrNull() ?: 0.0
        val expOutputs = validOutputs.map { exp(it - maxOutput) }
        val sumExp = expOutputs.sum()
        
        return validActions.zip(expOutputs) { action, expOutput ->
            action to (expOutput / sumExp)
        }.toMap()
    }
    
    /**
     * Get Q-values for analysis
     */
    fun getQValues(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        val networkOutput = neuralNetwork.forward(state)
        
        return validActions.associateWith { action ->
            if (action < networkOutput.size) networkOutput[action] else 0.0
        }
    }
    
    /**
     * Force a policy update (for testing/debugging)
     */
    fun forceUpdate() {
        if (experienceBuffer.isNotEmpty()) {
            performPolicyUpdate()
        }
    }
    
    override fun save(path: String) {
        try {
            if (neuralNetwork is ChessNeuralNetwork) {
                neuralNetwork.save(path)
            }
            println("Chess agent saved to $path")
        } catch (e: Exception) {
            println("Failed to save chess agent: ${e.message}")
        }
    }
    
    override fun load(path: String) {
        try {
            if (neuralNetwork is ChessNeuralNetwork) {
                neuralNetwork.load(path)
            }
            println("Chess agent loaded from $path")
        } catch (e: Exception) {
            println("Failed to load chess agent: ${e.message}")
        }
    }
}

/**
 * Configuration for chess agent behavior
 */
data class ChessAgentConfig(
    val batchSize: Int = 32,
    val maxBufferSize: Int = 10000,
    val performanceWindowSize: Int = 100,
    val progressReportInterval: Int = 100,
    val gradientClipThreshold: Double = 10.0,
    val minPolicyEntropy: Double = 0.1
)

/**
 * Episode termination reasons for better tracking
 */
enum class EpisodeTerminationReason {
    GAME_ENDED,     // Natural chess game termination (checkmate, stalemate, draw)
    STEP_LIMIT,     // Episode ended due to maximum steps reached
    MANUAL          // Manually terminated episode
}

/**
 * Training metrics for chess agent
 */
data class ChessAgentMetrics(
    val episodeCount: Int,
    val averageReward: Double,
    val recentAverageReward: Double,
    val bestPerformance: Double,
    val explorationRate: Double,
    val experienceBufferSize: Int,
    val episodeLength: Int,
    val gameEndedEpisodes: Int = 0,
    val stepLimitEpisodes: Int = 0,
    val manualEpisodes: Int = 0
)

/**
 * Chess-specific neural network wrapper that adapts the NN interface for RL
 */
class ChessNeuralNetwork(
    private val network: com.chessrl.nn.FeedforwardNetwork
) : com.chessrl.rl.NeuralNetwork {
    
    override fun forward(input: DoubleArray): DoubleArray {
        return network.forward(input)
    }
    
    override fun backward(target: DoubleArray): DoubleArray {
        return network.backward(target)
    }
    
    override fun predict(input: DoubleArray): DoubleArray {
        return network.predict(input)
    }
    
    fun save(path: String) {
        network.save(path)
    }
    
    fun load(path: String) {
        network.load(path)
    }
    
    /**
     * Get the underlying network for advanced operations
     */
    fun getNetwork(): com.chessrl.nn.FeedforwardNetwork = network
}

/**
 * Factory for creating chess agents with different configurations
 */
object ChessAgentFactory {
    
    /**
     * Create a DQN-based chess agent
     */
    fun createDQNAgent(
        inputSize: Int = ChessStateEncoder.TOTAL_FEATURES,
        outputSize: Int = ChessActionEncoder.ACTION_SPACE_SIZE,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        explorationRate: Double = 0.1,
        config: ChessAgentConfig = ChessAgentConfig()
    ): ChessAgent {
        
        // Create main Q-network
        val qNetwork = createChessNetwork(inputSize, outputSize, hiddenLayers, learningRate)
        
        // Create target network (copy of main network)
        val targetNetwork = createChessNetwork(inputSize, outputSize, hiddenLayers, learningRate)
        
        // Create experience replay buffer
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(maxSize = 10000)
        
        // Create DQN algorithm
        val dqnAlgorithm = DQNAlgorithm(
            qNetwork = qNetwork,
            targetNetwork = targetNetwork,
            experienceReplay = experienceReplay,
            gamma = 0.99,
            targetUpdateFrequency = 100,
            batchSize = config.batchSize
        )
        
        // Create exploration strategy
        val explorationStrategy = EpsilonGreedyStrategy<Int>(explorationRate)
        
        return ChessAgent(qNetwork, dqnAlgorithm, explorationStrategy, config)
    }
    
    /**
     * Create a policy gradient-based chess agent
     */
    fun createPolicyGradientAgent(
        inputSize: Int = ChessStateEncoder.TOTAL_FEATURES,
        outputSize: Int = ChessActionEncoder.ACTION_SPACE_SIZE,
        hiddenLayers: List<Int> = listOf(512, 256, 128),
        learningRate: Double = 0.001,
        temperature: Double = 1.0,
        config: ChessAgentConfig = ChessAgentConfig()
    ): ChessAgent {
        
        // Create policy network
        val policyNetwork = createChessNetwork(inputSize, outputSize, hiddenLayers, learningRate)
        
        // Create policy gradient algorithm
        val pgAlgorithm = PolicyGradientAlgorithm(
            policyNetwork = policyNetwork,
            valueNetwork = null, // No baseline for simplicity
            gamma = 0.99
        )
        
        // Create exploration strategy
        val explorationStrategy = BoltzmannStrategy<Int>(temperature)
        
        return ChessAgent(policyNetwork, pgAlgorithm, explorationStrategy, config)
    }
    
    /**
     * Create a neural network suitable for chess
     */
    private fun createChessNetwork(
        inputSize: Int,
        outputSize: Int,
        hiddenLayers: List<Int>,
        learningRate: Double
    ): ChessNeuralNetwork {
        
        // Create network architecture using the actual neural network package
        val layers = mutableListOf<com.chessrl.nn.Layer>()
        
        // Input layer to first hidden layer
        var currentInputSize = inputSize
        for (hiddenSize in hiddenLayers) {
            layers.add(com.chessrl.nn.DenseLayer(currentInputSize, hiddenSize, com.chessrl.nn.ReLUActivation()))
            currentInputSize = hiddenSize
        }
        
        // Final layer (output layer)
        layers.add(com.chessrl.nn.DenseLayer(currentInputSize, outputSize, com.chessrl.nn.LinearActivation()))
        
        // Create network using the actual FeedforwardNetwork from nn package
        val network = com.chessrl.nn.FeedforwardNetwork(
            _layers = layers,
            lossFunction = com.chessrl.nn.HuberLoss(delta = 1.0), // Robust to outliers
            optimizer = com.chessrl.nn.AdamOptimizer(learningRate = learningRate),
            regularization = com.chessrl.nn.L2Regularization(lambda = 0.001)
        )
        
        return ChessNeuralNetwork(network)
    }
}

/**
 * Training loop for chess RL agent
 */
class ChessTrainingLoop(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment,
    private val config: ChessTrainingConfig = ChessTrainingConfig()
) {
    
    private val trainingHistory = mutableListOf<ChessTrainingMetrics>()
    
    /**
     * Run training for specified number of episodes
     */
    fun train(episodes: Int): List<ChessTrainingMetrics> {
        println("Starting chess RL training for $episodes episodes...")
        
        for (episode in 1..episodes) {
            val episodeMetrics = runEpisode(episode)
            trainingHistory.add(episodeMetrics)
            
            // Early stopping check
            if (shouldStopEarly()) {
                println("Early stopping at episode $episode")
                break
            }
        }
        
        println("Training completed. Total episodes: ${trainingHistory.size}")
        return trainingHistory.toList()
    }
    
    /**
     * Run a single training episode
     */
    private fun runEpisode(episodeNumber: Int): ChessTrainingMetrics {
        val startTime = getCurrentTimeMillis()
        
        // Reset environment
        var state = environment.reset()
        var totalReward = 0.0
        var stepCount = 0
        var gameResult = "ongoing"
        
        while (!environment.isTerminal(state) && stepCount < config.maxStepsPerEpisode) {
            // Get valid actions
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) {
                break // No valid moves available
            }
            
            // Agent selects action
            val action = agent.selectAction(state, validActions)
            
            // Take step in environment
            val stepResult = environment.step(action)
            
            // Create experience
            val experience = Experience(
                state = state,
                action = action,
                reward = stepResult.reward,
                nextState = stepResult.nextState,
                done = stepResult.done
            )
            
            // Agent learns from experience
            agent.learn(experience)
            
            // Update for next step
            state = stepResult.nextState
            totalReward += stepResult.reward
            stepCount++
            
            if (stepResult.done) {
                gameResult = stepResult.info["game_status"]?.toString() ?: "unknown"
            }
        }
        
        val endTime = getCurrentTimeMillis()
        val duration = endTime - startTime
        
        // Get agent metrics
        val agentMetrics = agent.getTrainingMetrics()
        
        // Get chess-specific metrics
        val chessMetrics = environment.getChessMetrics()
        
        return ChessTrainingMetrics(
            episode = episodeNumber,
            totalReward = totalReward,
            stepCount = stepCount,
            gameResult = gameResult,
            duration = duration,
            explorationRate = agentMetrics.explorationRate,
            chessMetrics = chessMetrics
        )
    }
    
    /**
     * Check if training should stop early
     */
    private fun shouldStopEarly(): Boolean {
        if (trainingHistory.size < config.earlyStoppingWindow) {
            return false
        }
        
        val recentRewards = trainingHistory.takeLast(config.earlyStoppingWindow)
            .map { it.totalReward }
        
        val averageReward = recentRewards.average()
        return averageReward >= config.earlyStoppingThreshold
    }
    
    /**
     * Get training statistics
     */
    fun getTrainingStatistics(): ChessTrainingStatistics {
        if (trainingHistory.isEmpty()) {
            return ChessTrainingStatistics(
                totalEpisodes = 0,
                averageReward = 0.0,
                bestReward = 0.0,
                averageGameLength = 0.0,
                winRate = 0.0,
                drawRate = 0.0,
                lossRate = 0.0
            )
        }
        
        val rewards = trainingHistory.map { it.totalReward }
        val gameLengths = trainingHistory.map { it.stepCount.toDouble() }
        
        val wins = trainingHistory.count { it.gameResult.contains("WHITE_WINS") || it.gameResult.contains("BLACK_WINS") }
        val draws = trainingHistory.count { it.gameResult.contains("DRAW") }
        val losses = trainingHistory.size - wins - draws
        
        return ChessTrainingStatistics(
            totalEpisodes = trainingHistory.size,
            averageReward = rewards.average(),
            bestReward = rewards.maxOrNull() ?: 0.0,
            averageGameLength = gameLengths.average(),
            winRate = wins.toDouble() / trainingHistory.size,
            drawRate = draws.toDouble() / trainingHistory.size,
            lossRate = losses.toDouble() / trainingHistory.size
        )
    }
}

/**
 * Configuration for chess training
 */
data class ChessTrainingConfig(
    val maxStepsPerEpisode: Int = 200,
    val earlyStoppingWindow: Int = 100,
    val earlyStoppingThreshold: Double = 0.8
)

/**
 * Metrics for a single training episode
 */
data class ChessTrainingMetrics(
    val episode: Int,
    val totalReward: Double,
    val stepCount: Int,
    val gameResult: String,
    val duration: Long,
    val explorationRate: Double,
    val chessMetrics: ChessMetrics
)

/**
 * Overall training statistics
 */
data class ChessTrainingStatistics(
    val totalEpisodes: Int,
    val averageReward: Double,
    val bestReward: Double,
    val averageGameLength: Double,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double
)