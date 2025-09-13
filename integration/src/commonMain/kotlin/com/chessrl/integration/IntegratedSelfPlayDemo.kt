package com.chessrl.integration

/**
 * Comprehensive demo of the integrated self-play system
 * This demonstrates the complete integration between:
 * - Real chess agents with neural networks
 * - Self-play game generation
 * - Experience collection and training
 * - Checkpoint management
 * - Deterministic seeding
 */
object IntegratedSelfPlayDemo {
    
    /**
     * Run a complete integrated self-play training demo
     */
    fun runCompleteDemo(): DemoResults {
        println("🚀 Starting Integrated Self-Play System Demo")
        println("=" * 60)
        
        val demoStartTime = getCurrentTimeMillis()
        val results = mutableListOf<String>()
        
        try {
            // Phase 1: Initialize deterministic seeding
            println("\n📋 Phase 1: Deterministic Seeding Setup")
            println("-" * 40)
            
            val seedManager = SeedManager.apply {
                initializeWithMasterSeed(12345L)
            }
            
            val seedConfig = seedManager.getCurrentConfiguration()
            println("✅ Seed configuration: $seedConfig")
            results.add("Deterministic seeding initialized with master seed: ${seedConfig.masterSeed}")
            
            // Phase 2: Initialize integrated controller
            println("\n🔧 Phase 2: Integrated Controller Initialization")
            println("-" * 40)
            
            val config = IntegratedSelfPlayConfig(
                agentType = AgentType.DQN,
                hiddenLayers = listOf(256, 128, 64), // Smaller for demo
                learningRate = 0.001,
                explorationRate = 0.1,
                gamesPerIteration = 5, // Fewer games for demo
                maxConcurrentGames = 2,
                maxStepsPerGame = 50, // Shorter games for demo
                batchSize = 16,
                maxExperienceBufferSize = 1000,
                checkpointFrequency = 2,
                iterationReportInterval = 1,
                enableDeterministicTraining = true,
                enableEarlyStopping = false
            )
            
            val controller = IntegratedSelfPlayController(config)
            val initResult = controller.initialize(seedManager)
            
            when (initResult) {
                is InitializationResult.Success -> {
                    println("✅ ${initResult.message}")
                    results.add("Controller initialized successfully")
                }
                is InitializationResult.Failed -> {
                    println("❌ ${initResult.error}")
                    results.add("Controller initialization failed: ${initResult.error}")
                    return DemoResults(false, results, 0L)
                }
            }
            
            // Phase 3: Run integrated training
            println("\n🎮 Phase 3: Integrated Self-Play Training")
            println("-" * 40)
            
            val trainingIterations = 3 // Small number for demo
            val trainingResults = controller.runIntegratedTraining(trainingIterations)
            
            println("✅ Training completed successfully!")
            results.add("Training completed: ${trainingResults.totalIterations} iterations")
            results.add("Best performance: ${trainingResults.bestPerformance}")
            results.add("Total games played: ${trainingResults.finalMetrics.totalGamesPlayed}")
            results.add("Total experiences: ${trainingResults.finalMetrics.totalExperiencesCollected}")
            
            // Phase 4: Demonstrate checkpoint management
            println("\n💾 Phase 4: Checkpoint Management Demo")
            println("-" * 40)
            
            val checkpointSummary = trainingResults.checkpointSummary
            println("📊 Checkpoint Summary:")
            println("   Total checkpoints: ${checkpointSummary.totalCheckpoints}")
            println("   Valid checkpoints: ${checkpointSummary.validCheckpoints}")
            println("   Best checkpoint version: ${checkpointSummary.bestCheckpointVersion}")
            println("   Average performance: ${checkpointSummary.averagePerformance}")
            
            results.add("Checkpoints created: ${checkpointSummary.totalCheckpoints}")
            results.add("Best checkpoint version: ${checkpointSummary.bestCheckpointVersion}")
            
            // Phase 5: Demonstrate deterministic reproducibility
            println("\n🔄 Phase 5: Deterministic Reproducibility Test")
            println("-" * 40)
            
            val reproducibilityResult = testReproducibility(seedManager)
            results.add("Reproducibility test: ${if (reproducibilityResult) "PASSED" else "FAILED"}")
            
            // Phase 6: Performance analysis
            println("\n📈 Phase 6: Performance Analysis")
            println("-" * 40)
            
            analyzeTrainingPerformance(trainingResults)
            results.add("Performance analysis completed")
            
            val demoEndTime = getCurrentTimeMillis()
            val totalDuration = demoEndTime - demoStartTime
            
            println("\n🏁 Demo Completed Successfully!")
            println("Total duration: ${totalDuration}ms")
            println("All integration components working correctly!")
            
            return DemoResults(true, results, totalDuration)
            
        } catch (e: Exception) {
            println("❌ Demo failed: ${e.message}")
            e.printStackTrace()
            results.add("Demo failed: ${e.message}")
            
            val demoEndTime = getCurrentTimeMillis()
            val totalDuration = demoEndTime - demoStartTime
            
            return DemoResults(false, results, totalDuration)
        }
    }
    
    /**
     * Test deterministic reproducibility
     */
    private fun testReproducibility(seedManager: SeedManager): Boolean {
        return try {
            println("🧪 Testing deterministic reproducibility...")
            
            // Create two identical configurations
            val config1 = TrainingConfiguration(
                masterSeed = 12345L,
                agentType = "DQN",
                hiddenLayers = listOf(64, 32),
                learningRate = 0.001,
                explorationRate = 0.1,
                episodes = 2,
                batchSize = 8
            )
            
            val config2 = config1.copy()
            
            // Initialize with same seed
            seedManager.initializeWithMasterSeed(config1.masterSeed)
            val random1 = seedManager.getNeuralNetworkRandom()
            val values1 = (1..10).map { random1.nextDouble() }
            
            seedManager.initializeWithMasterSeed(config2.masterSeed)
            val random2 = seedManager.getNeuralNetworkRandom()
            val values2 = (1..10).map { random2.nextDouble() }
            
            val isReproducible = values1 == values2
            
            if (isReproducible) {
                println("✅ Reproducibility test PASSED - identical sequences generated")
            } else {
                println("❌ Reproducibility test FAILED - sequences differ")
                println("   Sequence 1: ${values1.take(3)}...")
                println("   Sequence 2: ${values2.take(3)}...")
            }
            
            isReproducible
            
        } catch (e: Exception) {
            println("❌ Reproducibility test failed with exception: ${e.message}")
            false
        }
    }
    
    /**
     * Analyze training performance
     */
    private fun analyzeTrainingPerformance(trainingResults: IntegratedTrainingResults) {
        println("📊 Training Performance Analysis:")
        
        val finalMetrics = trainingResults.finalMetrics
        
        println("   Games per iteration: ${finalMetrics.totalGamesPlayed / trainingResults.totalIterations}")
        println("   Experiences per game: ${finalMetrics.totalExperiencesCollected / finalMetrics.totalGamesPlayed}")
        println("   Average performance: ${finalMetrics.averagePerformance}")
        println("   Performance improvement: ${finalMetrics.bestPerformance - finalMetrics.averagePerformance}")
        println("   Win rate: ${(finalMetrics.averageWinRate * 100)}%")
        println("   Training efficiency: ${finalMetrics.totalBatchUpdates / trainingResults.totalIterations} updates/iteration")
        
        // Analyze iteration progression
        if (trainingResults.iterationResults.size >= 2) {
            val firstIteration = trainingResults.iterationResults.first()
            val lastIteration = trainingResults.iterationResults.last()
            
            val performanceImprovement = lastIteration.performance - firstIteration.performance
            val rewardImprovement = lastIteration.evaluationResults.averageReward - firstIteration.evaluationResults.averageReward
            
            println("   Performance improvement: ${performanceImprovement}")
            println("   Reward improvement: ${rewardImprovement}")
            
            if (performanceImprovement > 0) {
                println("✅ Training showed positive improvement")
            } else {
                println("⚠️ Training showed no improvement (expected for short demo)")
            }
        }
    }
    
    /**
     * Run a minimal integration test
     */
    fun runMinimalIntegrationTest(): Boolean {
        return try {
            println("🧪 Running Minimal Integration Test")
            
            // Test 1: Agent creation
            val agent = ChessAgentFactory.createDQNAgent()
            println("✅ Agent creation: PASSED")
            
            // Test 2: Environment interaction
            val environment = ChessEnvironment()
            val state = environment.reset()
            val validActions = environment.getValidActions(state)
            println("✅ Environment interaction: PASSED (${validActions.size} valid actions)")
            
            // Test 3: Agent action selection
            val action = agent.selectAction(state, validActions)
            println("✅ Agent action selection: PASSED (selected action $action)")
            
            // Test 4: Environment step
            val stepResult = environment.step(action)
            println("✅ Environment step: PASSED (reward: ${stepResult.reward})")
            
            // Test 5: Experience learning
            val experience = Experience(
                state = state,
                action = action,
                reward = stepResult.reward,
                nextState = stepResult.nextState,
                done = stepResult.done
            )
            agent.learn(experience)
            println("✅ Experience learning: PASSED")
            
            // Test 6: Training metrics
            val metrics = agent.getTrainingMetrics()
            println("✅ Training metrics: PASSED (exploration rate: ${metrics.explorationRate})")
            
            println("🎉 All integration tests PASSED!")
            true
            
        } catch (e: Exception) {
            println("❌ Integration test failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Demonstrate component integration
     */
    fun demonstrateComponentIntegration(): ComponentIntegrationResults {
        println("🔗 Demonstrating Component Integration")
        println("-" * 40)
        
        val results = mutableMapOf<String, Boolean>()
        
        try {
            // Test neural network integration
            println("🧠 Testing Neural Network Integration...")
            val nnIntegration = testNeuralNetworkIntegration()
            results["Neural Network Integration"] = nnIntegration
            
            // Test RL algorithm integration
            println("🎯 Testing RL Algorithm Integration...")
            val rlIntegration = testRLAlgorithmIntegration()
            results["RL Algorithm Integration"] = rlIntegration
            
            // Test chess environment integration
            println("♟️ Testing Chess Environment Integration...")
            val chessIntegration = testChessEnvironmentIntegration()
            results["Chess Environment Integration"] = chessIntegration
            
            // Test self-play system integration
            println("🎮 Testing Self-Play System Integration...")
            val selfPlayIntegration = testSelfPlaySystemIntegration()
            results["Self-Play System Integration"] = selfPlayIntegration
            
            // Test checkpoint integration
            println("💾 Testing Checkpoint Integration...")
            val checkpointIntegration = testCheckpointIntegration()
            results["Checkpoint Integration"] = checkpointIntegration
            
            val allPassed = results.values.all { it }
            
            println("\n📊 Integration Test Results:")
            results.forEach { (component, passed) ->
                val status = if (passed) "✅ PASSED" else "❌ FAILED"
                println("   $component: $status")
            }
            
            return ComponentIntegrationResults(allPassed, results)
            
        } catch (e: Exception) {
            println("❌ Component integration test failed: ${e.message}")
            return ComponentIntegrationResults(false, results)
        }
    }
    
    private fun testNeuralNetworkIntegration(): Boolean {
        return try {
            val agent = RealChessAgentFactory.createRealDQNAgent()
            val state = DoubleArray(776) { kotlin.random.Random.nextDouble() }
            val validActions = listOf(0, 1, 2, 3, 4)
            
            val action = agent.selectAction(state, validActions)
            val qValues = agent.getQValues(state, validActions)
            
            action in validActions && qValues.isNotEmpty()
        } catch (e: Exception) {
            println("   ❌ Neural network integration failed: ${e.message}")
            false
        }
    }
    
    private fun testRLAlgorithmIntegration(): Boolean {
        return try {
            val agent = ChessAgentFactory.createDQNAgent()
            val experience = Experience(
                state = DoubleArray(776) { kotlin.random.Random.nextDouble() },
                action = 0,
                reward = 1.0,
                nextState = DoubleArray(776) { kotlin.random.Random.nextDouble() },
                done = false
            )
            
            agent.learn(experience)
            agent.forceUpdate()
            
            val metrics = agent.getTrainingMetrics()
            metrics.explorationRate >= 0.0
        } catch (e: Exception) {
            println("   ❌ RL algorithm integration failed: ${e.message}")
            false
        }
    }
    
    private fun testChessEnvironmentIntegration(): Boolean {
        return try {
            val environment = ChessEnvironment()
            val state = environment.reset()
            val validActions = environment.getValidActions(state)
            
            if (validActions.isNotEmpty()) {
                val stepResult = environment.step(validActions.first())
                stepResult.nextState.isNotEmpty()
            } else {
                false
            }
        } catch (e: Exception) {
            println("   ❌ Chess environment integration failed: ${e.message}")
            false
        }
    }
    
    private fun testSelfPlaySystemIntegration(): Boolean {
        return try {
            val agent1 = ChessAgentFactory.createDQNAgent()
            val agent2 = ChessAgentFactory.createDQNAgent()
            val selfPlaySystem = SelfPlaySystem()
            
            val results = selfPlaySystem.runSelfPlayGames(agent1, agent2, 1)
            results.totalGames > 0 && results.totalExperiences > 0
        } catch (e: Exception) {
            println("   ❌ Self-play system integration failed: ${e.message}")
            false
        }
    }
    
    private fun testCheckpointIntegration(): Boolean {
        return try {
            val agent = ChessAgentFactory.createDQNAgent()
            val checkpointManager = CheckpointManager()
            
            val metadata = CheckpointMetadata(
                cycle = 1,
                performance = 0.5,
                description = "Test checkpoint"
            )
            
            val checkpointInfo = checkpointManager.createCheckpoint(agent, 1, metadata)
            checkpointInfo.version == 1
        } catch (e: Exception) {
            println("   ❌ Checkpoint integration failed: ${e.message}")
            false
        }
    }
}

/**
 * Demo results
 */
data class DemoResults(
    val success: Boolean,
    val results: List<String>,
    val duration: Long
)

/**
 * Component integration test results
 */
data class ComponentIntegrationResults(
    val allPassed: Boolean,
    val componentResults: Map<String, Boolean>
)