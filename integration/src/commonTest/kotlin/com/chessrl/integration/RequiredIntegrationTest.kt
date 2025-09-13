package com.chessrl.integration

import kotlin.test.*
import com.chessrl.rl.*

/**
 * Specific tests to verify all required integration improvements are properly implemented
 */
class RequiredIntegrationTest {
    
    @Test
    fun testRequirement1_ChessAgentConnectionToSelfPlayController() {
        println("Testing Requirement 1: Connect SelfPlayController to actual ChessAgent instances")
        
        // Create real chess agents (not mocks)
        val mainAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            config = ChessAgentConfig(batchSize = 4)
        )
        
        val opponentAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            config = ChessAgentConfig(batchSize = 4)
        )
        
        // Verify these are real agents with actual neural networks
        assertTrue(mainAgent is ChessAgentAdapter, "Main agent should be real ChessAgentAdapter")
        assertTrue(opponentAgent is ChessAgentAdapter, "Opponent agent should be real ChessAgentAdapter")
        
        // Test agent can interact with chess environment
        val environment = ChessEnvironment()
        val state = environment.reset()
        val validActions = environment.getValidActions(state)
        
        assertFalse(validActions.isEmpty(), "Should have valid actions in initial chess position")
        
        val action = mainAgent.selectAction(state, validActions)
        assertTrue(action in validActions, "Agent should select valid action")
        
        // Test SelfPlayController can use these agents for actual gameplay
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 5
            )
        )
        
        val gameResults = selfPlaySystem.runSelfPlayGames(mainAgent, opponentAgent, 1)
        
        assertEquals(1, gameResults.totalGames, "Should play exactly 1 game")
        assertTrue(gameResults.totalExperiences > 0, "Should generate experiences from gameplay")
        assertTrue(gameResults.gameResults.isNotEmpty(), "Should have game result records")
        
        // Verify game result contains actual chess data
        val gameResult = gameResults.gameResults.first()
        assertTrue(gameResult.gameLength > 0, "Game should have moves")
        assertTrue(gameResult.experiences.isNotEmpty(), "Game should generate experiences")
        assertNotNull(gameResult.finalPosition, "Game should have final position")
    }
    
    @Test
    fun testRequirement2_ExperienceFlowFromSelfPlayToReplayBuffer() {
        println("Testing Requirement 2: Proper experience flow from self-play to ExperienceReplay buffer")
        
        // Create agent with real experience replay buffer
        val realAgent = RealChessAgentFactory.createRealDQNAgent(
            inputSize = 776,
            outputSize = 4096,
            hiddenLayers = listOf(32, 16),
            maxBufferSize = 100
        )
        
        val chessAgent = ChessAgentAdapter(realAgent, ChessAgentConfig())
        
        // Generate self-play experiences
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(maxStepsPerGame = 5)
        )
        
        val initialMetrics = chessAgent.getTrainingMetrics()
        val initialBufferSize = initialMetrics.experienceBufferSize
        
        val gameResults = selfPlaySystem.runSelfPlayGames(chessAgent, chessAgent, 1)
        
        // Verify experiences were generated
        assertTrue(gameResults.totalExperiences > 0, "Self-play should generate experiences")
        assertTrue(gameResults.experiences.isNotEmpty(), "Should have enhanced experiences")
        
        // Verify experiences have proper enhancement
        val enhancedExp = gameResults.experiences.first()
        assertTrue(enhancedExp.gameId > 0, "Experience should have game ID")
        assertTrue(enhancedExp.moveNumber > 0, "Experience should have move number")
        assertTrue(enhancedExp.qualityScore >= 0.0, "Experience should have quality score")
        assertNotNull(enhancedExp.playerColor, "Experience should have player color")
        
        // Convert to basic experiences and add to agent
        val basicExperiences = gameResults.experiences.map { it.toBasicExperience() }
        basicExperiences.forEach { exp ->
            chessAgent.learn(exp)
        }
        
        // Verify experiences flowed to replay buffer
        val updatedMetrics = chessAgent.getTrainingMetrics()
        val updatedBufferSize = updatedMetrics.experienceBufferSize
        
        assertTrue(updatedBufferSize > initialBufferSize, 
                  "Experience buffer should grow after adding experiences: $initialBufferSize -> $updatedBufferSize")
        
        // Verify experiences can be used for training
        chessAgent.forceUpdate()
        
        // Should not throw exception and should maintain buffer
        val finalMetrics = chessAgent.getTrainingMetrics()
        assertTrue(finalMetrics.experienceBufferSize >= 0, "Buffer should remain valid after training")
    }
    
    @Test
    fun testRequirement3_TrainingIterationLoopsAlternatingSelfPlayAndNetworkTraining() {
        println("Testing Requirement 3: Training iteration loops alternating between self-play and network training")
        
        // Create integrated controller with minimal config for testing
        val config = IntegratedSelfPlayConfig(
            gamesPerIteration = 2,
            maxStepsPerGame = 3,
            batchSize = 2,
            hiddenLayers = listOf(16, 8),
            iterationReportInterval = 1
        )
        
        val controller = IntegratedSelfPlayController(config)
        val initResult = controller.initialize()
        
        assertTrue(initResult is InitializationResult.Success, "Controller should initialize successfully")
        
        // Run training iterations
        val trainingResults = controller.runIntegratedTraining(2)
        
        assertEquals(2, trainingResults.totalIterations, "Should complete 2 training iterations")
        assertEquals(2, trainingResults.iterationResults.size, "Should have 2 iteration results")
        
        // Verify each iteration has both self-play and training phases
        for ((index, iteration) in trainingResults.iterationResults.withIndex()) {
            val iterationNum = index + 1
            
            // Verify self-play phase
            assertTrue(iteration.selfPlayResults.totalGames > 0, 
                      "Iteration $iterationNum should have self-play games")
            assertTrue(iteration.selfPlayResults.totalExperiences > 0, 
                      "Iteration $iterationNum should generate experiences")
            
            // Verify training phase
            assertTrue(iteration.trainingResults.batchUpdates > 0, 
                      "Iteration $iterationNum should have training updates")
            assertTrue(iteration.trainingResults.experiencesProcessed > 0, 
                      "Iteration $iterationNum should process experiences")
            
            // Verify evaluation phase
            assertTrue(iteration.evaluationResults.episodesPlayed > 0, 
                      "Iteration $iterationNum should have evaluation episodes")
            
            // Verify performance tracking
            assertTrue(iteration.performance >= 0.0, 
                      "Iteration $iterationNum should have performance score")
        }
        
        // Verify alternating pattern by checking that experiences from one iteration
        // are used for training in the same iteration
        val totalGames = trainingResults.finalMetrics.totalGamesPlayed
        val totalExperiences = trainingResults.finalMetrics.totalExperiencesCollected
        val totalUpdates = trainingResults.finalMetrics.totalBatchUpdates
        
        assertTrue(totalGames > 0, "Should have played games across iterations")
        assertTrue(totalExperiences > 0, "Should have collected experiences across iterations")
        assertTrue(totalUpdates > 0, "Should have performed training updates across iterations")
        
        // Verify training status tracking
        val finalStatus = controller.getTrainingStatus()
        assertFalse(finalStatus.isTraining, "Should not be training after completion")
        assertEquals(2, finalStatus.completedIterations, "Should show 2 completed iterations")
    }
    
    @Test
    fun testRequirement4_RealNeuralNetworkTrainingWithSelfPlayExperiences() {
        println("Testing Requirement 4: Real neural network training with collected self-play experiences")
        
        // Create real neural network agent (not mock)
        val realAgent = RealChessAgentFactory.createRealDQNAgent(
            inputSize = 776,
            outputSize = 4096,
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            batchSize = 4
        )
        
        // Verify it's using real neural networks
        assertNotNull(realAgent, "Should create real agent")
        
        // Test neural network forward pass
        val chessState = DoubleArray(776) { kotlin.random.Random.nextDouble(-1.0, 1.0) }
        val validActions = listOf(0, 1, 2, 3, 4)
        
        val qValuesBefore = realAgent.getQValues(chessState, validActions)
        assertEquals(validActions.size, qValuesBefore.size, "Should get Q-values for all valid actions")
        
        // Generate self-play experiences
        val chessAgent = ChessAgentAdapter(realAgent, ChessAgentConfig())
        val selfPlaySystem = SelfPlaySystem(SelfPlayConfig(maxStepsPerGame = 5))
        
        val gameResults = selfPlaySystem.runSelfPlayGames(chessAgent, chessAgent, 1)
        val experiences = gameResults.experiences.map { it.toBasicExperience() }
        
        assertTrue(experiences.isNotEmpty(), "Should generate experiences from self-play")
        
        // Train neural network on self-play experiences
        val initialMetrics = realAgent.getTrainingMetrics()
        
        experiences.forEach { exp ->
            realAgent.learn(exp)
        }
        
        // Force network update to trigger actual neural network training
        realAgent.forceUpdate()
        
        val updatedMetrics = realAgent.getTrainingMetrics()
        
        // Verify neural network learned from experiences
        assertTrue(updatedMetrics.experienceBufferSize > initialMetrics.experienceBufferSize,
                  "Experience buffer should grow after learning")
        
        // Verify neural network weights changed (indicating actual training)
        val qValuesAfter = realAgent.getQValues(chessState, validActions)
        
        // Add more experiences to ensure weight updates
        repeat(10) {
            val experience = Experience(
                state = DoubleArray(776) { kotlin.random.Random.nextDouble(-1.0, 1.0) },
                action = validActions.random(),
                reward = kotlin.random.Random.nextDouble(-1.0, 1.0),
                nextState = DoubleArray(776) { kotlin.random.Random.nextDouble(-1.0, 1.0) },
                done = kotlin.random.Random.nextBoolean()
            )
            realAgent.learn(experience)
        }
        
        realAgent.forceUpdate()
        val qValuesAfterTraining = realAgent.getQValues(chessState, validActions)
        
        // Check if Q-values changed (indicating network weight updates)
        val qValuesChanged = validActions.any { action ->
            kotlin.math.abs(qValuesBefore[action]!! - qValuesAfterTraining[action]!!) > 1e-6
        }
        
        assertTrue(qValuesChanged, "Neural network Q-values should change after training")
        
        // Test integrated training pipeline
        val trainingPipeline = ChessTrainingPipeline(
            agent = chessAgent,
            environment = ChessEnvironment(),
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 5,
                batchSize = 2
            )
        )
        
        val episodeResult = trainingPipeline.runEpisode()
        
        assertTrue(episodeResult.steps > 0, "Training pipeline should execute steps")
        assertTrue(episodeResult.experiences.isNotEmpty(), "Training pipeline should generate experiences")
        assertTrue(episodeResult.reward != 0.0 || episodeResult.steps > 0, "Training pipeline should show activity")
    }
    
    @Test
    fun testCompleteIntegrationFlow() {
        println("Testing Complete Integration Flow: All requirements working together")
        
        // Test with deterministic seeding for reproducibility
        val seedManager = SeedManager.apply {
            initializeWithMasterSeed(54321L)
        }
        
        val config = IntegratedSelfPlayConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(32, 16),
            gamesPerIteration = 1,
            maxStepsPerGame = 3,
            batchSize = 2,
            enableDeterministicTraining = true,
            checkpointFrequency = 1
        )
        
        val controller = IntegratedSelfPlayController(config)
        val initResult = controller.initialize(seedManager)
        
        assertTrue(initResult is InitializationResult.Success, "Complete integration should initialize")
        
        // Run complete training cycle
        val trainingResults = controller.runIntegratedTraining(1)
        
        // Verify all requirements are satisfied in the complete flow
        
        // Requirement 1: Real agents connected
        assertTrue(trainingResults.finalMetrics.totalGamesPlayed > 0, "Real agents should play games")
        
        // Requirement 2: Experience flow
        assertTrue(trainingResults.finalMetrics.totalExperiencesCollected > 0, "Should collect experiences")
        
        // Requirement 3: Training iterations
        assertEquals(1, trainingResults.totalIterations, "Should complete training iteration")
        assertTrue(trainingResults.finalMetrics.totalBatchUpdates > 0, "Should perform training updates")
        
        // Requirement 4: Real neural network training
        assertTrue(trainingResults.iterationResults.isNotEmpty(), "Should have iteration results")
        val iteration = trainingResults.iterationResults.first()
        assertTrue(iteration.trainingResults.experiencesProcessed > 0, "Should process experiences with real networks")
        
        // Additional verification: Checkpoints created
        assertTrue(trainingResults.checkpointSummary.totalCheckpoints > 0, "Should create checkpoints")
        
        // Test reproducibility (deterministic training)
        seedManager.initializeWithMasterSeed(54321L) // Reset to same seed
        val controller2 = IntegratedSelfPlayController(config)
        controller2.initialize(seedManager)
        val trainingResults2 = controller2.runIntegratedTraining(1)
        
        assertEquals(trainingResults.finalMetrics.totalGamesPlayed, 
                    trainingResults2.finalMetrics.totalGamesPlayed,
                    "Deterministic training should be reproducible")
    }
    
    @Test
    fun testIntegrationVerificationDemo() {
        println("Testing Integration Verification Demo")
        
        val verificationResults = IntegrationVerificationDemo.verifyAllIntegrationRequirements()
        
        assertTrue(verificationResults.allPassed, "All integration requirements should be verified")
        
        // Check each specific requirement
        assertTrue(verificationResults.results["ChessAgent Connection"]?.success == true,
                  "ChessAgent connection should be verified")
        assertTrue(verificationResults.results["Experience Flow"]?.success == true,
                  "Experience flow should be verified")
        assertTrue(verificationResults.results["Training Iteration Loops"]?.success == true,
                  "Training iteration loops should be verified")
        assertTrue(verificationResults.results["Neural Network Integration"]?.success == true,
                  "Neural network integration should be verified")
        assertTrue(verificationResults.results["End-to-End Integration"]?.success == true,
                  "End-to-end integration should be verified")
    }
}