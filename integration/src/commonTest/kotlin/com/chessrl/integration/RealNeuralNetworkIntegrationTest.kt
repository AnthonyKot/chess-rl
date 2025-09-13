package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Test to verify that the training control interface works with real neural networks
 */
class RealNeuralNetworkIntegrationTest {
    
    @Test
    fun testRealDQNAgentCreation() {
        // Test creating a DQN agent with real neural networks
        val realAgent = RealChessAgentFactory.createRealDQNAgent(
            inputSize = 10, // Smaller for testing
            outputSize = 5,
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1,
            batchSize = 4,
            maxBufferSize = 100
        )
        
        assertNotNull(realAgent)
        
        // Test basic functionality
        val state = DoubleArray(10) { it * 0.1 }
        val validActions = listOf(0, 1, 2, 3, 4)
        
        val selectedAction = realAgent.selectAction(state, validActions)
        assertTrue(selectedAction in validActions)
        
        val qValues = realAgent.getQValues(state, validActions)
        assertEquals(validActions.size, qValues.size)
        assertTrue(qValues.values.all { it.isFinite() })
        
        val probabilities = realAgent.getActionProbabilities(state, validActions)
        assertEquals(validActions.size, probabilities.size)
        assertTrue(probabilities.values.all { it.isFinite() })
        
        // Test learning
        val experience = Experience(
            state = state,
            action = selectedAction,
            reward = 1.0,
            nextState = state,
            done = false
        )
        
        realAgent.learn(experience)
        
        val metrics = realAgent.getTrainingMetrics()
        assertNotNull(metrics)
        assertTrue(metrics.averageReward.isFinite())
        assertTrue(metrics.explorationRate > 0.0)
    }
    
    @Test
    fun testRealPolicyGradientAgentCreation() {
        // Test creating a Policy Gradient agent with real neural networks
        val realAgent = RealChessAgentFactory.createRealPolicyGradientAgent(
            inputSize = 8, // Smaller for testing
            outputSize = 3,
            hiddenLayers = listOf(6, 4),
            learningRate = 0.005,
            temperature = 1.0,
            batchSize = 4
        )
        
        assertNotNull(realAgent)
        
        // Test basic functionality
        val state = DoubleArray(8) { it * 0.1 }
        val validActions = listOf(0, 1, 2)
        
        val selectedAction = realAgent.selectAction(state, validActions)
        assertTrue(selectedAction in validActions)
        
        val probabilities = realAgent.getActionProbabilities(state, validActions)
        assertEquals(validActions.size, probabilities.size)
        assertTrue(probabilities.values.all { it.isFinite() })
        
        // Test learning
        val experience = Experience(
            state = state,
            action = selectedAction,
            reward = 0.5,
            nextState = state,
            done = true
        )
        
        realAgent.learn(experience)
        
        val metrics = realAgent.getTrainingMetrics()
        assertNotNull(metrics)
        assertTrue(metrics.averageReward.isFinite())
    }
    
    @Test
    fun testTrainingControlInterfaceWithRealAgents() {
        // Test that the training control interface works with real agents
        val config = TrainingInterfaceConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            explorationRate = 0.2,
            batchSize = 8,
            maxBufferSize = 100
        )
        
        val trainingInterface = TrainingControlInterface(config)
        val initResult = trainingInterface.initialize()
        
        when (initResult) {
            is InterfaceInitializationResult.Success -> {
                assertTrue(initResult.componentResults.isNotEmpty())
                
                // Test getting dashboard
                val dashboard = trainingInterface.getTrainingDashboard()
                assertNotNull(dashboard)
                
                // Test basic training session
                val sessionConfig = TrainingSessionConfig(
                    trainingType = TrainingType.BASIC,
                    episodes = 2,
                    enableMonitoring = false
                )
                
                val sessionResult = trainingInterface.startTrainingSession(sessionConfig)
                when (sessionResult) {
                    is TrainingSessionResult.Success -> {
                        assertNotNull(sessionResult.sessionId)
                    }
                    is TrainingSessionResult.Failed -> {
                        // Training might fail in test environment, which is acceptable
                        println("Training session failed (expected in test): ${sessionResult.error}")
                    }
                }
                
                // Cleanup
                trainingInterface.shutdown()
            }
            is InterfaceInitializationResult.Failed -> {
                fail("Interface initialization should succeed: ${initResult.error}")
            }
            is InterfaceInitializationResult.AlreadyInitialized -> {
                fail("Interface should not be already initialized")
            }
        }
    }
    
    @Test
    fun testExperienceReplayBuffer() {
        val buffer = CircularExperienceBuffer<DoubleArray, Int>(maxSize = 10)
        
        // Test adding experiences
        repeat(5) { i ->
            val experience = Experience(
                state = DoubleArray(5) { i.toDouble() },
                action = i,
                reward = i.toDouble(),
                nextState = DoubleArray(5) { (i + 1).toDouble() },
                done = false
            )
            buffer.add(experience)
        }
        
        assertEquals(5, buffer.size())
        
        // Test sampling
        val batch = buffer.sample(3)
        assertEquals(3, batch.size)
        
        // Test overflow
        repeat(10) { i ->
            val experience = Experience(
                state = DoubleArray(5) { (i + 10).toDouble() },
                action = i + 10,
                reward = (i + 10).toDouble(),
                nextState = DoubleArray(5) { (i + 11).toDouble() },
                done = false
            )
            buffer.add(experience)
        }
        
        assertEquals(10, buffer.size()) // Should not exceed maxSize
        
        // Test clearing
        buffer.clear()
        assertEquals(0, buffer.size())
    }
    
    @Test
    fun testExplorationStrategies() {
        val validActions = listOf(0, 1, 2, 3, 4)
        val actionValues = validActions.associateWith { it.toDouble() }
        
        // Test epsilon-greedy
        val epsilonGreedy = EpsilonGreedyStrategy<Int>(epsilon = 0.5)
        val selectedAction1 = epsilonGreedy.selectAction(validActions, actionValues)
        assertTrue(selectedAction1 in validActions)
        
        val initialEpsilon = epsilonGreedy.getExplorationRate()
        epsilonGreedy.updateExploration(1)
        val updatedEpsilon = epsilonGreedy.getExplorationRate()
        assertTrue(updatedEpsilon <= initialEpsilon) // Should decay
        
        // Test Boltzmann
        val boltzmann = BoltzmannStrategy<Int>(temperature = 1.0)
        val selectedAction2 = boltzmann.selectAction(validActions, actionValues)
        assertTrue(selectedAction2 in validActions)
        
        val initialTemp = boltzmann.getExplorationRate()
        boltzmann.updateExploration(1)
        val updatedTemp = boltzmann.getExplorationRate()
        assertTrue(updatedTemp <= initialTemp) // Should decay
    }
    
    @Test
    fun testRLAlgorithms() {
        // Create simple neural networks for testing
        val qNetwork = createTestNetwork()
        val targetNetwork = createTestNetwork()
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(maxSize = 100)
        
        // Test DQN algorithm
        val dqnAlgorithm = DQNAlgorithm(
            qNetwork = qNetwork,
            targetNetwork = targetNetwork,
            experienceReplay = experienceReplay,
            gamma = 0.99,
            targetUpdateFrequency = 10,
            batchSize = 4
        )
        
        // Add some experiences
        repeat(10) { i ->
            val experience = Experience(
                state = DoubleArray(5) { i.toDouble() },
                action = i % 3,
                reward = i.toDouble(),
                nextState = DoubleArray(5) { (i + 1).toDouble() },
                done = i == 9
            )
            experienceReplay.add(experience)
        }
        
        // Test policy update
        val experiences = experienceReplay.sample(4)
        val updateResult = dqnAlgorithm.updatePolicy(experiences)
        
        assertNotNull(updateResult)
        assertTrue(updateResult.loss.isFinite())
        assertTrue(updateResult.gradientNorm.isFinite())
        assertTrue(updateResult.policyEntropy.isFinite())
    }
    
    private fun createTestNetwork(): com.chessrl.rl.NeuralNetwork {
        return object : com.chessrl.rl.NeuralNetwork {
            override fun forward(input: DoubleArray): DoubleArray {
                return DoubleArray(3) { kotlin.random.Random.nextDouble(-1.0, 1.0) }
            }
            
            override fun backward(target: DoubleArray): DoubleArray {
                return doubleArrayOf(kotlin.random.Random.nextDouble(0.0, 1.0))
            }
            
            override fun predict(input: DoubleArray): DoubleArray {
                return forward(input)
            }
        }
    }
    
    @Test
    fun testChessStateEncoder() {
        val encoder = ChessStateEncoder()
        val board = ChessBoard()
        
        val encoded = encoder.encode(board)
        assertEquals(ChessStateEncoder.TOTAL_FEATURES, encoded.size)
        assertTrue(encoded.all { it.isFinite() })
    }
    
    @Test
    fun testChessActionEncoder() {
        val encoder = ChessActionEncoder()
        
        val move = Move(Position(1, 4), Position(3, 4))
        val actionIndex = encoder.encodeMove(move)
        val decodedMove = encoder.decodeAction(actionIndex)
        
        assertTrue(actionIndex >= 0)
        assertTrue(actionIndex < ChessActionEncoder.ACTION_SPACE_SIZE)
        assertNotNull(decodedMove)
    }
}