package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.nn.*

/**
 * Demo showing chess RL agent in action
 * This demonstrates the integration between neural networks, RL algorithms, and chess environment
 */
object ChessAgentDemo {
    
    /**
     * Run a simple demonstration of the chess agent
     */
    fun runDemo() {
        println("🏁 Chess RL Agent Demo")
        println("=" * 50)
        
        // Create a chess agent
        println("\n🤖 Creating chess agent...")
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(128, 64, 32), // Moderate size for demo
            learningRate = 0.001,
            explorationRate = 0.2
        )
        
        // Create chess environment
        println("♟️  Creating chess environment...")
        val environment = ChessEnvironment()
        
        // Run a single game demonstration
        println("\n🎮 Running single game demonstration...")
        runSingleGame(agent, environment)
        
        // Run short training session
        println("\n🏋️  Running short training session...")
        runTrainingDemo(agent, environment)
        
        println("\n✅ Demo completed!")
    }
    
    /**
     * Run a single game to show agent-environment interaction
     */
    private fun runSingleGame(agent: ChessAgent, environment: ChessEnvironment) {
        var state = environment.reset()
        var stepCount = 0
        val maxSteps = 50 // Limit for demo
        
        println("Starting new chess game...")
        
        while (!environment.isTerminal(state) && stepCount < maxSteps) {
            // Get valid actions
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) {
                println("No valid moves available!")
                break
            }
            
            // Agent selects action
            val action = agent.selectAction(state, validActions)
            
            // Take step in environment
            val stepResult = environment.step(action)
            
            // Create experience for learning
            val experience = Experience(
                state = state,
                action = action,
                reward = stepResult.reward,
                nextState = stepResult.nextState,
                done = stepResult.done
            )
            
            // Agent learns from experience
            agent.learn(experience)
            
            // Print step information
            val moveInfo = stepResult.info["move"]?.toString() ?: "unknown"
            println("Step $stepCount: Move=$moveInfo, Reward=${stepResult.reward}")
            
            // Update for next step
            state = stepResult.nextState
            stepCount++
            
            if (stepResult.done) {
                val gameResult = stepResult.info["game_status"]?.toString() ?: "unknown"
                println("Game ended: $gameResult")
                break
            }
        }
        
        // Show final metrics
        val metrics = agent.getTrainingMetrics()
        println("Game completed in $stepCount steps")
        println("Agent metrics: Episodes=${metrics.episodeCount}, Buffer=${metrics.experienceBufferSize}")
    }
    
    /**
     * Run a short training session to show learning progress
     */
    private fun runTrainingDemo(agent: ChessAgent, environment: ChessEnvironment) {
        val trainingConfig = ChessTrainingConfig(
            maxStepsPerEpisode = 30, // Short episodes for demo
            earlyStoppingWindow = 10,
            earlyStoppingThreshold = 5.0
        )
        
        val trainingLoop = ChessTrainingLoop(agent, environment, trainingConfig)
        
        println("Training agent for 5 episodes...")
        val history = trainingLoop.train(episodes = 5)
        
        // Show training results
        println("\nTraining Results:")
        for ((index, episodeMetrics) in history.withIndex()) {
            println("Episode ${index + 1}: " +
                   "Reward=${episodeMetrics.totalReward}, " +
                   "Steps=${episodeMetrics.stepCount}, " +
                   "Result=${episodeMetrics.gameResult}")
        }
        
        // Show overall statistics
        val stats = trainingLoop.getTrainingStatistics()
        println("\nOverall Statistics:")
        println("  Total Episodes: ${stats.totalEpisodes}")
        println("  Average Reward: ${stats.averageReward}")
        println("  Best Reward: ${stats.bestReward}")
        println("  Average Game Length: ${stats.averageGameLength}")
        println("  Win Rate: ${(stats.winRate * 100)}%")
        println("  Draw Rate: ${(stats.drawRate * 100)}%")
        println("  Loss Rate: ${(stats.lossRate * 100)}%")
    }
    
    /**
     * Demonstrate different agent types
     */
    fun compareAgentTypes() {
        println("\n🔬 Comparing Agent Types")
        println("=" * 30)
        
        val environment = ChessEnvironment()
        
        // Test DQN agent
        println("\n🧠 Testing DQN Agent...")
        val dqnAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(64, 32),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        testAgent(dqnAgent, environment, "DQN")
        
        // Test Policy Gradient agent
        println("\n🎯 Testing Policy Gradient Agent...")
        val pgAgent = ChessAgentFactory.createPolicyGradientAgent(
            hiddenLayers = listOf(64, 32),
            learningRate = 0.01,
            temperature = 1.0
        )
        testAgent(pgAgent, environment, "Policy Gradient")
    }
    
    /**
     * Test a specific agent type
     */
    private fun testAgent(agent: ChessAgent, environment: ChessEnvironment, agentType: String) {
        val state = environment.reset()
        val validActions = environment.getValidActions(state).take(5) // Test with first 5 actions
        
        if (validActions.isNotEmpty()) {
            val action = agent.selectAction(state, validActions)
            val qValues = agent.getQValues(state, validActions)
            val probabilities = agent.getActionProbabilities(state, validActions)
            
            println("$agentType Agent Results:")
            println("  Selected Action: $action")
            println("  Q-Values: ${qValues.values.map { it }}")
            println("  Probabilities: ${probabilities.values.map { it }}")
        }
    }
    
    /**
     * Show neural network analysis
     */
    fun analyzeNeuralNetwork() {
        println("\n🔍 Neural Network Analysis")
        println("=" * 30)
        
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 0.001,
            explorationRate = 0.0 // No exploration for analysis
        )
        
        // Create test states
        val emptyState = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { 0.0 }
        val randomState = DoubleArray(ChessStateEncoder.TOTAL_FEATURES) { kotlin.random.Random.nextDouble(-1.0, 1.0) }
        
        val testActions = listOf(0, 1, 2, 3, 4)
        
        println("Empty board state analysis:")
        val emptyQValues = agent.getQValues(emptyState, testActions)
        println("  Q-Values: ${emptyQValues.values.map { it }}")
        
        println("\nRandom state analysis:")
        val randomQValues = agent.getQValues(randomState, testActions)
        println("  Q-Values: ${randomQValues.values.map { it }}")
        
        // Show network response consistency
        val emptyQValues2 = agent.getQValues(emptyState, testActions)
        val consistent = emptyQValues.keys.all { 
            kotlin.math.abs(emptyQValues[it]!! - emptyQValues2[it]!!) < 1e-10 
        }
        println("\nNetwork consistency: ${if (consistent) "✅ Consistent" else "❌ Inconsistent"}")
    }
}

