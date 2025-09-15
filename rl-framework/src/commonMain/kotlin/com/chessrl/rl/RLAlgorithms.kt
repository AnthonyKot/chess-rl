package com.chessrl.rl

import kotlin.math.*
import kotlin.random.Random

/**
 * Minimal neural network interface for RL algorithms
 */
interface NeuralNetwork {
    fun forward(input: DoubleArray): DoubleArray
    fun backward(target: DoubleArray): DoubleArray
    fun predict(input: DoubleArray): DoubleArray = forward(input)
}

/**
 * Optional extension for networks that support batched training.
 */
interface TrainableNeuralNetwork : NeuralNetwork {
    /**
     * Train the network on a batch of inputs and targets. Returns average loss.
     */
    fun trainBatch(inputs: Array<DoubleArray>, targets: Array<DoubleArray>): Double
}

/**
 * RL Algorithm interface for policy updates and action selection
 */
interface RLAlgorithm<S, A> {
    // Core RL operations
    fun updatePolicy(experiences: List<Experience<S, A>>): PolicyUpdateResult
    fun getActionValues(state: S, validActions: List<A>): Map<A, Double>
    fun getActionProbabilities(state: S, validActions: List<A>): Map<A, Double>
    
    // Training control
    fun setLearningRate(rate: Double)
    fun setExplorationStrategy(strategy: ExplorationStrategy<A>)
    fun getTrainingMetrics(): RLMetrics
}

/**
 * Optional extension for networks that support copying/synchronizing weights.
 */
interface SynchronizableNetwork {
    fun copyWeightsTo(target: NeuralNetwork)
}

/**
 * Result of a policy update containing training metrics
 */
data class PolicyUpdateResult(
    val loss: Double,
    val gradientNorm: Double,
    val policyEntropy: Double,
    val valueError: Double? = null,  // For actor-critic methods
    val qValueMean: Double? = null,  // For Q-learning methods
    val targetValueMean: Double? = null  // For Q-learning methods
)

/**
 * RL training metrics for monitoring progress
 */
data class RLMetrics(
    val episode: Int,
    val averageReward: Double,
    val episodeLength: Double,
    val explorationRate: Double,
    val policyLoss: Double,
    val valueLoss: Double? = null,
    val policyEntropy: Double,
    val gradientNorm: Double,
    val qValueStats: QValueStats? = null
)

/**
 * Q-value statistics for DQN monitoring
 */
data class QValueStats(
    val meanQValue: Double,
    val maxQValue: Double,
    val minQValue: Double,
    val qValueStd: Double
)

/**
 * Training issue detection for RL algorithms
 */
enum class TrainingIssue {
    EXPLODING_GRADIENTS,
    VANISHING_GRADIENTS,
    POLICY_COLLAPSE,
    VALUE_OVERESTIMATION,
    EXPLORATION_INSUFFICIENT,
    LEARNING_RATE_TOO_HIGH,
    LEARNING_RATE_TOO_LOW,
    TARGET_NETWORK_STALE
}

/**
 * DQN Algorithm implementation with target network and experience replay
 */
class DQNAlgorithm(
    private val qNetwork: NeuralNetwork,
    private val targetNetwork: NeuralNetwork,
    private val experienceReplay: ExperienceReplay<DoubleArray, Int>,
    private val gamma: Double = 0.99,
    private val targetUpdateFrequency: Int = 100,
    private val batchSize: Int = 32
) : RLAlgorithm<DoubleArray, Int> {
    
    private var updateCount = 0
    private var lastTargetSyncAt = 0
    private var learningRate = 0.001
    private var explorationStrategy: ExplorationStrategy<Int> = EpsilonGreedyStrategy(0.1)
    private var lastMetrics = RLMetrics(0, 0.0, 0.0, 0.0, 0.0, policyEntropy = 0.0, gradientNorm = 0.0)
    private var nextActionProvider: ((DoubleArray) -> List<Int>)? = null
    private var printedMaskingEnabled = false
    private var printedMaskSampleInfo = false

    /**
     * Provide a function that returns the list of valid next-state actions for masking.
     * If set, DQN targets will only consider Q-values over valid actions.
     */
    fun setNextActionProvider(provider: (DoubleArray) -> List<Int>) {
        nextActionProvider = provider
        if (!printedMaskingEnabled) {
            println("🧩 DQN next-state action masking enabled (provider set)")
            printedMaskingEnabled = true
        }
    }
    
    override fun updatePolicy(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        // Add experiences to replay buffer
        experiences.forEach { experienceReplay.add(it) }
        
        // Only update if we have enough experiences
        if (experienceReplay.size() < batchSize) {
            return PolicyUpdateResult(0.0, 0.0, 0.0)
        }
        
        // Sample batch from experience replay
        val batch = experienceReplay.sample(batchSize)
        
        // Compute Q-targets using target network
        val qTargets = computeQTargets(batch)
        
        // Prepare training data
        val inputs = batch.map { it.state }.toTypedArray()
        // Capture predicted outputs before modifying targets for a simple output-gradient estimate
        val predictedOutputs = Array(batch.size) { i ->
            val out = qNetwork.forward(batch[i].state)
            require(out.isNotEmpty()) { "qNetwork.forward returned empty vector" }
            out.copyOf()
        }
        val targets = Array(batch.size) { i ->
            val qValues = predictedOutputs[i].copyOf()
            require(batch[i].action in qValues.indices) {
                "Action index ${batch[i].action} out of bounds for Q-values size ${qValues.size}"
            }
            qValues[batch[i].action] = qTargets[i]
            qValues
        }
        
        // Train main network
        var totalLoss = 0.0
        val trainable = qNetwork as? TrainableNeuralNetwork
        if (trainable != null) {
            // Use proper batch training when available
            totalLoss = trainable.trainBatch(inputs, targets)
        } else {
            // Fallback: per-sample forward/backward without optimizer updates
            for (i in 0 until batch.size) {
                val predicted = qNetwork.forward(inputs[i])
                val loss = computeMSELoss(predicted, targets[i])
                totalLoss += loss
                qNetwork.backward(targets[i])
            }
            totalLoss /= batch.size
        }
        
        // Update target network periodically
        updateCount++
        if (updateCount % targetUpdateFrequency == 0) {
            updateTargetNetwork()
            lastTargetSyncAt = updateCount
            println("🔁 DQN target network synchronized at update=$updateCount (freq=$targetUpdateFrequency)")
        }
        
        // Calculate metrics
        val qValueStats = calculateQValueStats(batch)
        val policyEntropy = calculatePolicyEntropy(batch)
        val gradientNormEstimate = estimateOutputGradientNorm(predictedOutputs, targets)
        
        return PolicyUpdateResult(
            loss = if (trainable != null) totalLoss else totalLoss,
            gradientNorm = gradientNormEstimate,
            policyEntropy = policyEntropy,
            qValueMean = qValueStats.meanQValue,
            targetValueMean = qTargets.average()
        )
    }

    /**
     * Estimate gradient norm at the network output using MSE dL/dy = 2/N * (y_pred - y_target).
     * This is a proxy for stability monitoring when true layer gradient norms aren't available.
     */
    private fun estimateOutputGradientNorm(predicted: Array<DoubleArray>, targets: Array<DoubleArray>): Double {
        if (predicted.isEmpty()) return 0.0
        var sum = 0.0
        val batchSize = predicted.size
        for (i in 0 until batchSize) {
            val p = predicted[i]
            val t = targets[i]
            val n = minOf(p.size, t.size)
            if (n == 0) continue
            val scale = 2.0 / n
            var s2 = 0.0
            var j = 0
            while (j < n) {
                val g = scale * (p[j] - t[j])
                s2 += g * g
                j++
            }
            sum += kotlin.math.sqrt(s2)
        }
        return sum / batchSize
    }
    
    private fun computeQTargets(batch: List<Experience<DoubleArray, Int>>): DoubleArray {
        return DoubleArray(batch.size) { i ->
            val experience = batch[i]
            if (experience.done) {
                experience.reward
            } else {
                val nextQValues = targetNetwork.forward(experience.nextState)
                require(nextQValues.isNotEmpty()) { "targetNetwork.forward returned empty vector" }
                val maskedMax = nextActionProvider?.let { provider ->
                    val valid = provider.invoke(experience.nextState)
                    if (!printedMaskSampleInfo) {
                        println("🧩 DQN masking applied: valid next actions for sample=${valid.size}")
                        printedMaskSampleInfo = true
                    }
                    if (valid.isEmpty()) {
                        // No valid actions – treat as terminal-like
                        0.0
                    } else {
                        var maxVal = Double.NEGATIVE_INFINITY
                        for (a in valid) {
                            require(a >= 0) { "Valid action contains negative index" }
                            if (a in nextQValues.indices) {
                                if (nextQValues[a] > maxVal) maxVal = nextQValues[a]
                            }
                        }
                        if (maxVal == Double.NEGATIVE_INFINITY) 0.0 else maxVal
                    }
                } ?: (nextQValues.maxOrNull() ?: 0.0)
                experience.reward + gamma * maskedMax
            }
        }
    }
    
    private fun updateTargetNetwork() {
        // If the underlying networks support synchronization, copy weights
        val sync = qNetwork as? SynchronizableNetwork
        if (sync != null) {
            try {
                sync.copyWeightsTo(targetNetwork)
            } catch (_: Throwable) {
                // Best-effort sync; ignore if unsupported at runtime
            }
        }
    }
    
    private fun calculateQValueStats(batch: List<Experience<DoubleArray, Int>>): QValueStats {
        val qValues = batch.flatMap { experience: Experience<DoubleArray, Int> ->
            qNetwork.forward(experience.state).toList()
        }
        
        return QValueStats(
            meanQValue = qValues.average(),
            maxQValue = qValues.maxOrNull() ?: 0.0,
            minQValue = qValues.minOrNull() ?: 0.0,
            qValueStd = calculateStandardDeviation(qValues)
        )
    }
    
    private fun calculatePolicyEntropy(batch: List<Experience<DoubleArray, Int>>): Double {
        // Calculate entropy of the policy (higher = more exploration)
        var totalEntropy = 0.0
        
        for (experience in batch) {
            val qValues = qNetwork.forward(experience.state)
            val probabilities = softmax(qValues)
            
            var entropy = 0.0
            for (prob in probabilities) {
                if (prob > 0.0) {
                    entropy -= prob * ln(prob)
                }
            }
            totalEntropy += entropy
        }
        
        return totalEntropy / batch.size
    }
    
    private fun softmax(values: DoubleArray): DoubleArray {
        val maxValue = values.maxOrNull() ?: 0.0
        val expValues = values.map { exp(it - maxValue) }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toDoubleArray()
    }
    
    private fun calculateStandardDeviation(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }
    
    private fun computeMSELoss(predicted: DoubleArray, target: DoubleArray): Double {
        var sum = 0.0
        for (i in predicted.indices) {
            val diff = predicted[i] - target[i]
            sum += diff * diff
        }
        return sum / predicted.size
    }
    
    override fun getActionValues(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        val qValues = qNetwork.forward(state)
        require(qValues.isNotEmpty()) { "qNetwork.forward returned empty vector" }
        require(validActions.all { it in qValues.indices }) {
            "Valid actions contain out-of-bounds index for Q-values size ${qValues.size}"
        }
        return validActions.associateWith { action -> qValues[action] }
    }
    
    override fun getActionProbabilities(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        val actionValues = getActionValues(state, validActions)
        return explorationStrategy.let { strategy ->
            // Convert exploration strategy to probabilities
            val selectedAction = strategy.selectAction(validActions, actionValues)
            validActions.associateWith { action ->
                if (action == selectedAction) 1.0 else 0.0
            }
        }
    }
    
    override fun setLearningRate(rate: Double) {
        learningRate = rate
    }
    
    override fun setExplorationStrategy(strategy: ExplorationStrategy<Int>) {
        explorationStrategy = strategy
    }
    
    override fun getTrainingMetrics(): RLMetrics = lastMetrics
    
    fun updateMetrics(metrics: RLMetrics) {
        lastMetrics = metrics
    }

    /**
     * For diagnostics and integration metrics.
     */
    fun getReplaySize(): Int = experienceReplay.size()
}

/**
 * Policy Gradient Algorithm (REINFORCE) implementation
 */
class PolicyGradientAlgorithm(
    private val policyNetwork: NeuralNetwork,
    private val valueNetwork: NeuralNetwork? = null,  // Optional baseline
    private val gamma: Double = 0.99
) : RLAlgorithm<DoubleArray, Int> {
    
    private var learningRate = 0.001
    private var explorationStrategy: ExplorationStrategy<Int> = BoltzmannStrategy(1.0)
    private var lastMetrics = RLMetrics(0, 0.0, 0.0, 0.0, 0.0, policyEntropy = 0.0, gradientNorm = 0.0)
    
    override fun updatePolicy(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        // Calculate returns (discounted rewards)
        val returns = calculateReturns(experiences)
        
        // Calculate baselines if value network is available
        val baselines = valueNetwork?.let { vNet ->
            experiences.map { exp: Experience<DoubleArray, Int> -> vNet.forward(exp.state)[0] }
        } ?: List(experiences.size) { 0.0 }
        
        // Calculate advantages
        val advantages = returns.zip(baselines) { ret: Double, baseline: Double -> ret - baseline }
        
        // Policy gradient update (prefer batched train when available)
        val policyLoss = updatePolicyNetwork(experiences, advantages)
        
        // Value network update (if using baseline)
        val valueLoss = if (valueNetwork != null) {
            updateValueNetwork(experiences, returns)
        } else 0.0
        
        // Calculate metrics
        val policyEntropy = calculatePolicyEntropy(experiences)
        val gradientNorm = estimateOutputGradientNorm(experiences, advantages)
        
        return PolicyUpdateResult(
            loss = policyLoss,
            gradientNorm = gradientNorm,
            policyEntropy = policyEntropy,
            valueError = valueLoss
        )
    }
    
    private fun calculateReturns(experiences: List<Experience<DoubleArray, Int>>): List<Double> {
        val returns = mutableListOf<Double>()
        var runningReturn = 0.0
        
        // Calculate returns backwards through the episode
        for (i in experiences.indices.reversed()) {
            runningReturn = experiences[i].reward + gamma * runningReturn
            returns.add(0, runningReturn)
        }
        
        return returns
    }
    
    private fun updatePolicyNetwork(experiences: List<Experience<DoubleArray, Int>>, advantages: List<Double>): Double {
        val trainable = policyNetwork as? TrainableNeuralNetwork
        if (trainable != null) {
            // Batched REINFORCE using MSE proxy: target = p + alpha*A*(one_hot - p)
            val alpha = 1.0 // scaling; effective step size governed by optimizer LR
            val inputs = Array(experiences.size) { i -> experiences[i].state }
            val targets = Array(experiences.size) { i ->
                val logits = policyNetwork.forward(experiences[i].state)
                require(logits.isNotEmpty()) { "policyNetwork.forward returned empty vector" }
                val p = softmaxLocal(logits)
                val a = experiences[i].action
                require(a in p.indices) { "Action ${a} out of bounds for policy output ${p.size}" }
                val adv = advantages[i]
                val t = DoubleArray(p.size) { j ->
                    val oneHot = if (j == a) 1.0 else 0.0
                    p[j] + alpha * adv * (oneHot - p[j])
                }
                t
            }
            return trainable.trainBatch(inputs, targets)
        } else {
            var totalLoss = 0.0
            for (i in experiences.indices) {
                val experience = experiences[i]
                val advantage = advantages[i]
                val logits = policyNetwork.forward(experience.state)
                val probabilities = softmaxLocal(logits)
                val actionProb = probabilities[experience.action]
                val logProb = ln(actionProb.coerceAtLeast(1e-8))
                val loss = -logProb * advantage
                totalLoss += loss
                val target = probabilities.copyOf()
                policyNetwork.backward(target)
            }
            return totalLoss / experiences.size
        }
    }

    /**
     * Estimate gradient norm at the policy output using REINFORCE proxy:
     * ||A|| * ||(softmax(logits) - one_hot(action))||_2 averaged over batch.
     */
    private fun estimateOutputGradientNorm(
        experiences: List<Experience<DoubleArray, Int>>,
        advantages: List<Double>
    ): Double {
        if (experiences.isEmpty()) return 0.0
        var sum = 0.0
        for (i in experiences.indices) {
            val exp = experiences[i]
            val adv = kotlin.math.abs(advantages[i])
            val logits = policyNetwork.forward(exp.state)
            require(logits.isNotEmpty()) { "policyNetwork.forward returned empty vector" }
            require(exp.action in logits.indices) { "Action ${exp.action} out of bounds for logits size ${logits.size}" }
            val p = softmaxLocal(logits)
            var s2 = 0.0
            for (j in p.indices) {
                val oneHot = if (j == exp.action) 1.0 else 0.0
                val g = (p[j] - oneHot) * adv
                s2 += g * g
            }
            sum += kotlin.math.sqrt(s2)
        }
        return sum / experiences.size
    }

    private fun softmaxLocal(values: DoubleArray): DoubleArray {
        val maxValue = values.maxOrNull() ?: 0.0
        val expValues = values.map { kotlin.math.exp(it - maxValue) }
        val sumExp = expValues.sum().let { if (it == 0.0) 1.0 else it }
        return expValues.map { it / sumExp }.toDoubleArray()
    }
    
    private fun updateValueNetwork(experiences: List<Experience<DoubleArray, Int>>, returns: List<Double>): Double {
        val valueNetwork = this.valueNetwork ?: return 0.0
        
        var totalLoss = 0.0
        
        for (i in experiences.indices) {
            val experience = experiences[i]
            val targetReturn = returns[i]
            
            // Forward pass
            val predictedValue = valueNetwork.forward(experience.state)[0]
            
            // MSE loss for value function
            val loss = (predictedValue - targetReturn).pow(2)
            totalLoss += loss
            
            // Backward pass
            val target = doubleArrayOf(targetReturn)
            valueNetwork.backward(target)
        }
        
        return totalLoss / experiences.size
    }
    
    private fun calculatePolicyEntropy(experiences: List<Experience<DoubleArray, Int>>): Double {
        var totalEntropy = 0.0
        
        for (experience in experiences) {
            val logits = policyNetwork.forward(experience.state)
            val probabilities = softmax(logits)
            
            var entropy = 0.0
            for (prob in probabilities) {
                if (prob > 0.0) {
                    entropy -= prob * ln(prob)
                }
            }
            totalEntropy += entropy
        }
        
        return totalEntropy / experiences.size
    }
    
    private fun softmax(values: DoubleArray): DoubleArray {
        val maxValue = values.maxOrNull() ?: 0.0
        val expValues = values.map { exp(it - maxValue) }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toDoubleArray()
    }
    
    override fun getActionValues(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        // For policy gradient, we don't have explicit action values
        // Return uniform values or use value network if available
        return validActions.associateWith { 0.0 }
    }
    
    override fun getActionProbabilities(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        val logits = policyNetwork.forward(state)
        val probabilities = softmaxLocal(logits)
        
        return validActions.associateWith { action ->
            if (action < probabilities.size) probabilities[action] else 0.0
        }
    }
    
    override fun setLearningRate(rate: Double) {
        learningRate = rate
    }
    
    override fun setExplorationStrategy(strategy: ExplorationStrategy<Int>) {
        explorationStrategy = strategy
    }
    
    override fun getTrainingMetrics(): RLMetrics = lastMetrics
    
    fun updateMetrics(metrics: RLMetrics) {
        lastMetrics = metrics
    }
}

/**
 * RL Agent implementation that uses neural networks for decision making
 */
class NeuralNetworkAgent(
    private val algorithm: RLAlgorithm<DoubleArray, Int>,
    private val explorationStrategy: ExplorationStrategy<Int>
) : Agent<DoubleArray, Int> {
    
    private val experienceBuffer = mutableListOf<Experience<DoubleArray, Int>>()
    
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        val actionValues = algorithm.getActionValues(state, validActions)
        return explorationStrategy.selectAction(validActions, actionValues)
    }
    
    override fun learn(experience: Experience<DoubleArray, Int>) {
        experienceBuffer.add(experience)
        
        // Update policy when we have enough experiences or at episode end
        if (experience.done || experienceBuffer.size >= 32) {
            algorithm.updatePolicy(experienceBuffer.toList())
            
            // Clear buffer after learning (for on-policy methods like REINFORCE)
            if (algorithm is PolicyGradientAlgorithm) {
                experienceBuffer.clear()
            }
        }
    }
    
    override fun save(path: String) {
        // Simplified save - in full implementation would serialize the neural networks
        println("Saving agent to $path")
    }
    
    override fun load(path: String) {
        // Simplified load - in full implementation would deserialize the neural networks
        println("Loading agent from $path")
    }
    
    fun getExperienceBufferSize(): Int = experienceBuffer.size
    
    fun clearExperienceBuffer() {
        experienceBuffer.clear()
    }
    
    fun updateExploration(episode: Int) {
        explorationStrategy.updateExploration(episode)
    }
    
    fun getExplorationRate(): Double = explorationStrategy.getExplorationRate()
}

/**
 * RL Validator for detecting training issues and validating policy updates
 */
class RLValidator {
    
    fun validatePolicyUpdate(
        beforeMetrics: RLMetrics,
        afterMetrics: RLMetrics,
        updateResult: PolicyUpdateResult
    ): ValidationResult {
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Check for exploding gradients
        if (updateResult.gradientNorm > 10.0) {
            issues.add("Exploding gradients detected (norm: ${updateResult.gradientNorm})")
            recommendations.add("Consider gradient clipping or reducing learning rate")
        }
        
        // Check for vanishing gradients
        if (updateResult.gradientNorm < 1e-6) {
            issues.add("Vanishing gradients detected (norm: ${updateResult.gradientNorm})")
            recommendations.add("Consider increasing learning rate or changing network architecture")
        }
        
        // Check for policy collapse (very low entropy)
        if (updateResult.policyEntropy < 0.1) {
            issues.add("Policy collapse detected (entropy: ${updateResult.policyEntropy})")
            recommendations.add("Increase exploration or add entropy regularization")
        }
        
        // Check for insufficient exploration
        if (beforeMetrics.explorationRate < 0.01 && afterMetrics.averageReward <= beforeMetrics.averageReward) {
            issues.add("Insufficient exploration with poor performance")
            recommendations.add("Increase exploration rate or use different exploration strategy")
        }
        
        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            recommendations = recommendations
        )
    }
    
    fun validateConvergence(trainingHistory: List<RLMetrics>): ConvergenceStatus {
        if (trainingHistory.size < 10) {
            return ConvergenceStatus.INSUFFICIENT_DATA
        }
        
        val recentRewards = trainingHistory.takeLast(10).map { it.averageReward }
        val rewardVariance = calculateVariance(recentRewards)
        
        return when {
            rewardVariance < 0.01 -> ConvergenceStatus.CONVERGED
            recentRewards.last() > recentRewards.first() -> ConvergenceStatus.IMPROVING
            else -> ConvergenceStatus.STAGNANT
        }
    }
    
    fun detectTrainingIssues(metrics: RLMetrics): List<TrainingIssue> {
        val issues = mutableListOf<TrainingIssue>()
        
        if (metrics.gradientNorm > 10.0) issues.add(TrainingIssue.EXPLODING_GRADIENTS)
        if (metrics.gradientNorm < 1e-6) issues.add(TrainingIssue.VANISHING_GRADIENTS)
        if (metrics.policyEntropy < 0.1) issues.add(TrainingIssue.POLICY_COLLAPSE)
        if (metrics.explorationRate < 0.01) issues.add(TrainingIssue.EXPLORATION_INSUFFICIENT)
        
        metrics.qValueStats?.let { qStats ->
            if (qStats.meanQValue > 100.0) issues.add(TrainingIssue.VALUE_OVERESTIMATION)
        }
        
        return issues
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
}

/**
 * Validation result for policy updates
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val recommendations: List<String>
)

/**
 * Convergence status for training monitoring
 */
enum class ConvergenceStatus {
    CONVERGED,
    IMPROVING,
    STAGNANT,
    INSUFFICIENT_DATA
}
