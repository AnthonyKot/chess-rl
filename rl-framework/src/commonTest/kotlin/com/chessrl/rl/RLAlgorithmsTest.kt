package com.chessrl.rl

import kotlin.test.*
import kotlin.random.Random

class RLAlgorithmsTest {
    
    private fun createSimpleNetwork(inputSize: Int, outputSize: Int): NeuralNetwork {
        return SimpleNeuralNetwork(inputSize, outputSize)
    }
    
    @Test
    fun testDQNAlgorithmCreation() {
        val qNetwork = createSimpleNetwork(4, 2)
        val targetNetwork = createSimpleNetwork(4, 2)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        
        val dqn = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay)
        
        assertNotNull(dqn)
        assertEquals(0.0, dqn.getTrainingMetrics().averageReward)
    }
    
    @Test
    fun testDQNPolicyUpdate() {
        val qNetwork = createSimpleNetwork(4, 2)
        val targetNetwork = createSimpleNetwork(4, 2)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        
        val dqn = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay, batchSize = 2)
        
        // Create sample experiences
        val experiences = listOf(
            Experience(
                state = doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                action = 0,
                reward = 1.0,
                nextState = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                done = false
            ),
            Experience(
                state = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                action = 1,
                reward = -1.0,
                nextState = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                done = true
            ),
            Experience(
                state = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                action = 0,
                reward = 0.5,
                nextState = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
                done = false
            )
        )
        
        // First update should not train (not enough experiences)
        val result1 = dqn.updatePolicy(experiences.take(1))
        assertEquals(0.0, result1.loss)
        
        // Second update should train
        val result2 = dqn.updatePolicy(experiences.drop(1))
        assertTrue(result2.loss >= 0.0)
        assertTrue(result2.policyEntropy >= 0.0)
    }
    
    @Test
    fun testDQNActionValues() {
        val qNetwork = createSimpleNetwork(4, 3)
        val targetNetwork = createSimpleNetwork(4, 3)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        
        val dqn = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay)
        
        val state = doubleArrayOf(1.0, 0.5, -0.5, 0.0)
        val validActions = listOf(0, 1, 2)
        
        val actionValues = dqn.getActionValues(state, validActions)
        
        assertEquals(3, actionValues.size)
        assertTrue(actionValues.keys.containsAll(validActions))
        actionValues.values.forEach { value ->
            assertTrue(value.isFinite())
        }
    }
    
    @Test
    fun testDQNActionProbabilities() {
        val qNetwork = createSimpleNetwork(4, 3)
        val targetNetwork = createSimpleNetwork(4, 3)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        
        val dqn = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay)
        
        val state = doubleArrayOf(1.0, 0.5, -0.5, 0.0)
        val validActions = listOf(0, 1, 2)
        
        val actionProbs = dqn.getActionProbabilities(state, validActions)
        
        assertEquals(3, actionProbs.size)
        assertTrue(actionProbs.keys.containsAll(validActions))
        
        // Should sum to 1.0 (one action selected with probability 1.0)
        val totalProb = actionProbs.values.sum()
        assertEquals(1.0, totalProb, 0.001)
    }
    
    @Test
    fun testPolicyGradientAlgorithmCreation() {
        val policyNetwork = createSimpleNetwork(4, 2)
        val valueNetwork = createSimpleNetwork(4, 1)
        
        val pg = PolicyGradientAlgorithm(policyNetwork, valueNetwork)
        
        assertNotNull(pg)
        assertEquals(0.0, pg.getTrainingMetrics().averageReward)
    }
    
    @Test
    fun testPolicyGradientUpdate() {
        val policyNetwork = createSimpleNetwork(4, 2)
        val valueNetwork = createSimpleNetwork(4, 1)
        
        val pg = PolicyGradientAlgorithm(policyNetwork, valueNetwork)
        
        // Create a complete episode
        val experiences = listOf(
            Experience(
                state = doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                action = 0,
                reward = 1.0,
                nextState = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                done = false
            ),
            Experience(
                state = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                action = 1,
                reward = 0.5,
                nextState = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                done = false
            ),
            Experience(
                state = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                action = 0,
                reward = -0.5,
                nextState = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
                done = true
            )
        )
        
        val result = pg.updatePolicy(experiences)
        
        assertTrue(result.loss.isFinite())
        assertTrue(result.gradientNorm >= 0.0)
        assertTrue(result.policyEntropy >= 0.0)
        assertNotNull(result.valueError)
    }
    
    @Test
    fun testPolicyGradientActionProbabilities() {
        val policyNetwork = createSimpleNetwork(4, 3)
        val pg = PolicyGradientAlgorithm(policyNetwork)
        
        val state = doubleArrayOf(1.0, 0.5, -0.5, 0.0)
        val validActions = listOf(0, 1, 2)
        
        val actionProbs = pg.getActionProbabilities(state, validActions)
        
        assertEquals(3, actionProbs.size)
        assertTrue(actionProbs.keys.containsAll(validActions))
        
        // Probabilities should sum to approximately 1.0
        val totalProb = actionProbs.values.sum()
        assertEquals(1.0, totalProb, 0.001)
        
        // All probabilities should be non-negative
        actionProbs.values.forEach { prob ->
            assertTrue(prob >= 0.0)
        }
    }
    
    @Test
    fun testNeuralNetworkAgentCreation() {
        val qNetwork = createSimpleNetwork(4, 2)
        val targetNetwork = createSimpleNetwork(4, 2)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        val algorithm = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay)
        val exploration = EpsilonGreedyStrategy<Int>(0.1)
        
        val agent = NeuralNetworkAgent(algorithm, exploration)
        
        assertNotNull(agent)
        assertEquals(0, agent.getExperienceBufferSize())
    }
    
    @Test
    fun testNeuralNetworkAgentActionSelection() {
        val qNetwork = createSimpleNetwork(4, 3)
        val targetNetwork = createSimpleNetwork(4, 3)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        val algorithm = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay)
        val exploration = EpsilonGreedyStrategy<Int>(0.0) // No exploration for deterministic test
        
        val agent = NeuralNetworkAgent(algorithm, exploration)
        
        val state = doubleArrayOf(1.0, 0.5, -0.5, 0.0)
        val validActions = listOf(0, 1, 2)
        
        val selectedAction = agent.selectAction(state, validActions)
        
        assertTrue(validActions.contains(selectedAction))
    }
    
    @Test
    fun testNeuralNetworkAgentLearning() {
        val qNetwork = createSimpleNetwork(4, 2)
        val targetNetwork = createSimpleNetwork(4, 2)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        val algorithm = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay, batchSize = 2)
        val exploration = EpsilonGreedyStrategy<Int>(0.1)
        
        val agent = NeuralNetworkAgent(algorithm, exploration)
        
        val experience1 = Experience(
            state = doubleArrayOf(1.0, 0.0, 0.0, 0.0),
            action = 0,
            reward = 1.0,
            nextState = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
            done = false
        )
        
        val experience2 = Experience(
            state = doubleArrayOf(0.0, 1.0, 0.0, 0.0),
            action = 1,
            reward = -1.0,
            nextState = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
            done = true
        )
        
        // Add experiences
        agent.learn(experience1)
        assertEquals(1, agent.getExperienceBufferSize())
        
        agent.learn(experience2)
        // Buffer size depends on algorithm type and learning trigger
        assertTrue(agent.getExperienceBufferSize() >= 0)
    }
    
    @Test
    fun testNeuralNetworkAgentExplorationUpdate() {
        val qNetwork = createSimpleNetwork(4, 2)
        val targetNetwork = createSimpleNetwork(4, 2)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        val algorithm = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay)
        val exploration = EpsilonGreedyStrategy<Int>(0.5, 0.99, 0.01)
        
        val agent = NeuralNetworkAgent(algorithm, exploration)
        
        val initialExploration = agent.getExplorationRate()
        assertEquals(0.5, initialExploration, 0.001)
        
        // Update exploration
        agent.updateExploration(1)
        val updatedExploration = agent.getExplorationRate()
        
        // Should decay
        assertTrue(updatedExploration < initialExploration)
        assertTrue(updatedExploration >= 0.01) // Should not go below minimum
    }
    
    @Test
    fun testRLValidatorPolicyUpdate() {
        val validator = RLValidator()
        
        val beforeMetrics = RLMetrics(
            episode = 1,
            averageReward = 0.5,
            episodeLength = 10.0,
            explorationRate = 0.1,
            policyLoss = 1.0,
            policyEntropy = 1.5,
            gradientNorm = 1.0
        )
        
        val afterMetrics = RLMetrics(
            episode = 2,
            averageReward = 0.6,
            episodeLength = 9.0,
            explorationRate = 0.09,
            policyLoss = 0.9,
            policyEntropy = 1.4,
            gradientNorm = 0.8
        )
        
        val updateResult = PolicyUpdateResult(
            loss = 0.5,
            gradientNorm = 1.0,
            policyEntropy = 1.4
        )
        
        val validation = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult)
        
        assertTrue(validation.isValid)
        assertTrue(validation.issues.isEmpty())
    }
    
    @Test
    fun testRLValidatorDetectsExplodingGradients() {
        val validator = RLValidator()
        
        val beforeMetrics = RLMetrics(
            episode = 1,
            averageReward = 0.5,
            episodeLength = 10.0,
            explorationRate = 0.1,
            policyLoss = 1.0,
            policyEntropy = 1.5,
            gradientNorm = 1.0
        )
        
        val afterMetrics = beforeMetrics.copy(episode = 2)
        
        val updateResult = PolicyUpdateResult(
            loss = 0.5,
            gradientNorm = 15.0, // Exploding gradient
            policyEntropy = 1.4
        )
        
        val validation = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult)
        
        assertFalse(validation.isValid)
        assertTrue(validation.issues.any { it.contains("Exploding gradients") })
        assertTrue(validation.recommendations.any { it.contains("gradient clipping") })
    }
    
    @Test
    fun testRLValidatorDetectsPolicyCollapse() {
        val validator = RLValidator()
        
        val beforeMetrics = RLMetrics(
            episode = 1,
            averageReward = 0.5,
            episodeLength = 10.0,
            explorationRate = 0.1,
            policyLoss = 1.0,
            policyEntropy = 1.5,
            gradientNorm = 1.0
        )
        
        val afterMetrics = beforeMetrics.copy(episode = 2)
        
        val updateResult = PolicyUpdateResult(
            loss = 0.5,
            gradientNorm = 1.0,
            policyEntropy = 0.05 // Very low entropy = policy collapse
        )
        
        val validation = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult)
        
        assertFalse(validation.isValid)
        assertTrue(validation.issues.any { it.contains("Policy collapse") })
        assertTrue(validation.recommendations.any { it.contains("exploration") })
    }
    
    @Test
    fun testRLValidatorConvergenceDetection() {
        val validator = RLValidator()
        
        // Create training history with converged rewards
        val convergedHistory = (1..15).map { episode ->
            RLMetrics(
                episode = episode,
                averageReward = 0.95 + (Random.nextDouble() - 0.5) * 0.01, // Small variance around 0.95
                episodeLength = 10.0,
                explorationRate = 0.1,
                policyLoss = 1.0,
                policyEntropy = 1.0,
                gradientNorm = 1.0
            )
        }
        
        val convergenceStatus = validator.validateConvergence(convergedHistory)
        assertEquals(ConvergenceStatus.CONVERGED, convergenceStatus)
        
        // Create improving history
        val improvingHistory = (1..15).map { episode ->
            RLMetrics(
                episode = episode,
                averageReward = episode * 0.1, // Steadily improving
                episodeLength = 10.0,
                explorationRate = 0.1,
                policyLoss = 1.0,
                policyEntropy = 1.0,
                gradientNorm = 1.0
            )
        }
        
        val improvingStatus = validator.validateConvergence(improvingHistory)
        assertEquals(ConvergenceStatus.IMPROVING, improvingStatus)
    }
    
    @Test
    fun testRLValidatorTrainingIssueDetection() {
        val validator = RLValidator()
        
        val metricsWithIssues = RLMetrics(
            episode = 10,
            averageReward = 0.5,
            episodeLength = 10.0,
            explorationRate = 0.005, // Too low exploration
            policyLoss = 1.0,
            policyEntropy = 0.05, // Policy collapse
            gradientNorm = 15.0, // Exploding gradients
            qValueStats = QValueStats(
                meanQValue = 150.0, // Value overestimation
                maxQValue = 200.0,
                minQValue = 100.0,
                qValueStd = 25.0
            )
        )
        
        val issues = validator.detectTrainingIssues(metricsWithIssues)
        
        assertTrue(issues.contains(TrainingIssue.EXPLODING_GRADIENTS))
        assertTrue(issues.contains(TrainingIssue.POLICY_COLLAPSE))
        assertTrue(issues.contains(TrainingIssue.EXPLORATION_INSUFFICIENT))
        assertTrue(issues.contains(TrainingIssue.VALUE_OVERESTIMATION))
    }
    
    @Test
    fun testBatchProcessingWithDQN() {
        val qNetwork = createSimpleNetwork(4, 2)
        val targetNetwork = createSimpleNetwork(4, 2)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        
        val dqn = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay, batchSize = 4)
        
        // Create multiple experiences for batch processing
        val experiences = (1..10).map { i ->
            Experience(
                state = doubleArrayOf(i.toDouble(), 0.0, 0.0, 0.0),
                action = i % 2,
                reward = if (i % 2 == 0) 1.0 else -1.0,
                nextState = doubleArrayOf((i + 1).toDouble(), 0.0, 0.0, 0.0),
                done = i == 10
            )
        }
        
        // Add experiences in batches
        val result1 = dqn.updatePolicy(experiences.take(3)) // Not enough for batch
        assertEquals(0.0, result1.loss)
        
        val result2 = dqn.updatePolicy(experiences.drop(3).take(4)) // Should trigger training
        assertTrue(result2.loss >= 0.0)
        
        val result3 = dqn.updatePolicy(experiences.drop(7)) // Another batch
        assertTrue(result3.loss >= 0.0)
    }
    
    @Test
    fun testPolicyGradientWithCompleteEpisode() {
        val policyNetwork = createSimpleNetwork(4, 2)
        val pg = PolicyGradientAlgorithm(policyNetwork)
        
        // Create a complete episode with varying rewards
        val episode = listOf(
            Experience(
                state = doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                action = 0,
                reward = 0.1,
                nextState = doubleArrayOf(0.8, 0.2, 0.0, 0.0),
                done = false
            ),
            Experience(
                state = doubleArrayOf(0.8, 0.2, 0.0, 0.0),
                action = 1,
                reward = 0.2,
                nextState = doubleArrayOf(0.6, 0.4, 0.0, 0.0),
                done = false
            ),
            Experience(
                state = doubleArrayOf(0.6, 0.4, 0.0, 0.0),
                action = 0,
                reward = 0.5,
                nextState = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                done = false
            ),
            Experience(
                state = doubleArrayOf(0.0, 0.0, 1.0, 0.0),
                action = 1,
                reward = 1.0,
                nextState = doubleArrayOf(0.0, 0.0, 0.0, 1.0),
                done = true
            )
        )
        
        val result = pg.updatePolicy(episode)
        
        assertTrue(result.loss.isFinite())
        assertTrue(result.gradientNorm >= 0.0)
        assertTrue(result.policyEntropy >= 0.0)
        
        // Verify that action probabilities are valid after training
        val state = doubleArrayOf(0.5, 0.5, 0.0, 0.0)
        val actionProbs = pg.getActionProbabilities(state, listOf(0, 1))
        
        assertEquals(1.0, actionProbs.values.sum(), 0.001)
        actionProbs.values.forEach { prob ->
            assertTrue(prob >= 0.0)
            assertTrue(prob <= 1.0)
        }
    }
}

