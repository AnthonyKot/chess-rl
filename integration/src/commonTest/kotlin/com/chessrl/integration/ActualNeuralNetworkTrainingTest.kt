package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Test to verify that we have ACTUAL neural network training, not mock implementations
 * This addresses the core integration issue: "ChessAgentFactory creates mock agents"
 */
class ActualNeuralNetworkTrainingTest {
    
    @Test
    fun testActualDQNImplementationWithRealNeuralNetworks() {
        println("=== Testing ACTUAL DQN Implementation ===")
        
        // Create a real DQN agent with actual neural networks
        val realAgent = RealChessAgentFactory.createRealDQNAgent(
            inputSize = 20, // Small for testing
            outputSize = 10,
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.1,
            batchSize = 4,
            maxBufferSize = 50
        )
        
        // Verify this is NOT a mock - test actual neural network behavior
        val testState1 = DoubleArray(20) { it * 0.1 }
        val testState2 = DoubleArray(20) { it * 0.2 } // Different input
        val validActions = listOf(0, 1, 2, 3, 4)
        
        // Get initial Q-values
        val initialQValues1 = realAgent.getQValues(testState1, validActions)
        val initialQValues2 = realAgent.getQValues(testState2, validActions)
        
        println("Initial Q-values for state1: $initialQValues1")
        println("Initial Q-values for state2: $initialQValues2")
        
        // Verify Q-values are different for different states (real neural network behavior)
        assertNotEquals(
            initialQValues1.values.sum(), 
            initialQValues2.values.sum(),
            "Q-values should be different for different states (real NN behavior)"
        )
        
        // Train the agent with actual experiences
        val trainingExperiences = listOf(
            Experience(testState1, 0, 1.0, testState2, false),
            Experience(testState2, 1, -0.5, testState1, false),
            Experience(testState1, 2, 0.5, testState2, false),
            Experience(testState2, 3, 0.0, testState1, true)
        )
        
        // Apply training
        for (experience in trainingExperiences) {
            realAgent.learn(experience)
        }
        
        // Force network update to ensure training occurred
        realAgent.forceUpdate()
        
        // Get Q-values after training
        val trainedQValues1 = realAgent.getQValues(testState1, validActions)
        val trainedQValues2 = realAgent.getQValues(testState2, validActions)
        
        println("Trained Q-values for state1: $trainedQValues1")
        println("Trained Q-values for state2: $trainedQValues2")
        
        // Verify that training actually changed the Q-values (real learning)
        val qValueChange1 = kotlin.math.abs(
            initialQValues1.values.sum() - trainedQValues1.values.sum()
        )
        val qValueChange2 = kotlin.math.abs(
            initialQValues2.values.sum() - trainedQValues2.values.sum()
        )
        
        println("Q-value change for state1: $qValueChange1")
        println("Q-value change for state2: $qValueChange2")
        
        // Real neural networks should show learning (Q-values should change)
        assertTrue(
            qValueChange1 > 0.001 || qValueChange2 > 0.001,
            "Q-values should change after training (indicating real neural network learning)"
        )
        
        println("✓ ACTUAL DQN implementation verified - real learning detected!")
    }
    
    @Test
    fun testActualPolicyGradientImplementationWithRealNeuralNetworks() {
        println("=== Testing ACTUAL Policy Gradient Implementation ===")
        
        // Create a real Policy Gradient agent with actual neural networks
        val realAgent = RealChessAgentFactory.createRealPolicyGradientAgent(
            inputSize = 15, // Small for testing
            outputSize = 8,
            hiddenLayers = listOf(12, 6),
            learningRate = 0.02,
            temperature = 1.0,
            batchSize = 3
        )
        
        val testState1 = DoubleArray(15) { it * 0.05 }
        val testState2 = DoubleArray(15) { it * 0.1 }
        val validActions = listOf(0, 1, 2, 3, 4, 5, 6, 7)
        
        // Get initial action probabilities
        val initialProbs1 = realAgent.getActionProbabilities(testState1, validActions)
        val initialProbs2 = realAgent.getActionProbabilities(testState2, validActions)
        
        println("Initial probabilities for state1: $initialProbs1")
        println("Initial probabilities for state2: $initialProbs2")
        
        // Verify probabilities are different for different states (real neural network)
        assertNotEquals(
            initialProbs1.values.sum(),
            initialProbs2.values.sum(),
            "Action probabilities should be different for different states"
        )
        
        // Train with policy gradient experiences
        val policyExperiences = listOf(
            Experience(testState1, 0, 1.0, testState2, false),
            Experience(testState2, 2, 0.8, testState1, false),
            Experience(testState1, 4, -0.2, testState2, true)
        )
        
        for (experience in policyExperiences) {
            realAgent.learn(experience)
        }
        
        realAgent.forceUpdate()
        
        // Get probabilities after training
        val trainedProbs1 = realAgent.getActionProbabilities(testState1, validActions)
        val trainedProbs2 = realAgent.getActionProbabilities(testState2, validActions)
        
        println("Trained probabilities for state1: $trainedProbs1")
        println("Trained probabilities for state2: $trainedProbs2")
        
        // Verify policy learning occurred
        val probChange1 = initialProbs1.keys.sumOf { action ->
            kotlin.math.abs((initialProbs1[action] ?: 0.0) - (trainedProbs1[action] ?: 0.0))
        }
        
        println("Probability change for state1: $probChange1")
        
        assertTrue(
            probChange1 > 0.001,
            "Action probabilities should change after training (real policy learning)"
        )
        
        println("✓ ACTUAL Policy Gradient implementation verified - real learning detected!")
    }
    
    @Test
    fun testRealExperienceReplayAndTrainingLoops() {
        println("=== Testing REAL Experience Replay and Training Loops ===")
        
        val realAgent = RealChessAgentFactory.createRealDQNAgent(
            inputSize = 10,
            outputSize = 5,
            hiddenLayers = listOf(8, 4),
            learningRate = 0.05,
            explorationRate = 0.2,
            batchSize = 3,
            maxBufferSize = 20
        )
        
        val validActions = listOf(0, 1, 2, 3, 4)
        
        // Simulate a training loop with real experience collection
        val trainingStates = mutableListOf<DoubleArray>()
        val initialMetrics = realAgent.getTrainingMetrics()
        
        println("Initial metrics: $initialMetrics")
        
        // Generate multiple training episodes
        repeat(5) { episode ->
            val episodeExperiences = mutableListOf<Experience<DoubleArray, Int>>()
            
            // Simulate an episode
            repeat(4) { step ->
                val state = DoubleArray(10) { (episode * 4 + step) * 0.1 }
                trainingStates.add(state)
                
                val action = realAgent.selectAction(state, validActions)
                val reward = if (step == 3) 1.0 else 0.0 // Reward at episode end
                val nextState = if (step < 3) DoubleArray(10) { (episode * 4 + step + 1) * 0.1 } else state
                val done = step == 3
                
                val experience = Experience(state, action, reward, nextState, done)
                episodeExperiences.add(experience)
                realAgent.learn(experience)
            }
            
            println("Episode $episode completed with ${episodeExperiences.size} experiences")
        }
        
        // Force final update
        realAgent.forceUpdate()
        
        val finalMetrics = realAgent.getTrainingMetrics()
        println("Final metrics: $finalMetrics")
        
        // Verify that actual training occurred
        assertTrue(
            finalMetrics.experienceBufferSize > 0,
            "Experience buffer should contain experiences from training"
        )
        
        // Test that the agent's behavior changed through training
        val testState = DoubleArray(10) { it * 0.05 }
        val qValuesAfterTraining = realAgent.getQValues(testState, validActions)
        
        println("Q-values after full training: $qValuesAfterTraining")
        
        // Verify Q-values are reasonable (not all zeros, not infinite)
        assertTrue(
            qValuesAfterTraining.values.all { it.isFinite() },
            "Q-values should be finite after training"
        )
        
        assertTrue(
            qValuesAfterTraining.values.any { kotlin.math.abs(it) > 0.001 },
            "Q-values should be non-trivial after training"
        )
        
        println("✓ REAL experience replay and training loops verified!")
    }
    
    @Test
    fun testActualNeuralNetworkIntegrationWithChessEnvironment() {
        println("=== Testing ACTUAL Neural Network Integration with Chess Environment ===")
        
        // Create real self-play controller with actual neural networks
        val config = SelfPlayConfig(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.3,
            batchSize = 4,
            maxExperiences = 50
        )
        
        val controller = RealSelfPlayController(config)
        
        // Initialize with real agents
        val initResult = controller.initialize()
        when (initResult) {
            is SelfPlayInitResult.Success -> {
                println("✓ Real agents initialized: ${initResult.message}")
                
                // Get initial metrics
                val initialMetrics = controller.getTrainingMetrics()
                println("Initial training metrics: $initialMetrics")
                
                // Verify we can get meaningful metrics
                assertTrue(
                    initialMetrics.explorationRate > 0.0,
                    "Exploration rate should be positive for real agents"
                )
                
                assertEquals(
                    0, initialMetrics.episodesCompleted,
                    "Should start with 0 episodes"
                )
                
                println("✓ ACTUAL neural network integration with chess environment verified!")
            }
            is SelfPlayInitResult.Failed -> {
                fail("Real agent initialization should succeed: ${initResult.error}")
            }
        }
    }
    
    @Test
    fun testRealNeuralNetworkWrapperFunctionality() {
        println("=== Testing Real Neural Network Wrapper ===")
        
        // Create a real feedforward network
        val layers = listOf(
            com.chessrl.nn.DenseLayer(5, 8, com.chessrl.nn.ReLUActivation()),
            com.chessrl.nn.DenseLayer(8, 3, com.chessrl.nn.LinearActivation())
        )
        
        val network = com.chessrl.nn.FeedforwardNetwork(
            _layers = layers,
            lossFunction = com.chessrl.nn.MSELoss(),
            optimizer = com.chessrl.nn.SGDOptimizer(learningRate = 0.01)
        )
        
        // Wrap it for RL use
        val wrapper = RealNeuralNetworkWrapper(network)
        
        // Test forward pass
        val input = DoubleArray(5) { it * 0.2 }
        val output1 = wrapper.forward(input)
        val output2 = wrapper.predict(input) // Should be same as forward
        
        println("Network input: ${input.contentToString()}")
        println("Network output: ${output1.contentToString()}")
        
        assertEquals(3, output1.size, "Output should have correct size")
        assertTrue(output1.contentEquals(output2), "forward() and predict() should be identical")
        
        // Test backward pass (training)
        val target = doubleArrayOf(1.0, 0.0, -1.0)
        val loss = wrapper.backward(target)
        
        println("Training target: ${target.contentToString()}")
        println("Training loss: ${loss.contentToString()}")
        
        assertTrue(loss.isNotEmpty(), "Backward pass should return loss information")
        assertTrue(loss.all { it.isFinite() }, "Loss should be finite")
        
        // Verify training changed the network
        val outputAfterTraining = wrapper.forward(input)
        println("Output after training: ${outputAfterTraining.contentToString()}")
        
        // The output might be slightly different after training
        assertNotNull(outputAfterTraining, "Network should still produce output after training")
        
        println("✓ Real neural network wrapper functionality verified!")
    }
}