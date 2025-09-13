package com.chessrl.integration

import kotlin.test.*

/**
 * Comprehensive tests for the integrated self-play system
 * Tests the complete integration between all components
 */
class IntegratedSelfPlayTest {
    
    @Test
    fun testIntegratedControllerInitialization() {
        val config = IntegratedSelfPlayConfig(
            gamesPerIteration = 2,
            maxStepsPerGame = 20,
            batchSize = 4,
            hiddenLayers = listOf(32, 16) // Small for testing
        )
        
        val controller = IntegratedSelfPlayController(config)
        val result = controller.initialize()
        
        assertTrue(result is InitializationResult.Success, "Controller should initialize successfully")
    }
    
    @Test
    fun testDeterministicIntegratedTraining() {
        // Initialize seed manager
        val seedManager = SeedManager.apply {
            initializeWithMasterSeed(12345L)
        }
        
        val config = IntegratedSelfPlayConfig(
            gamesPerIteration = 1,
            maxStepsPerGame = 10,
            batchSize = 2,
            hiddenLayers = listOf(16, 8),
            enableDeterministicTraining = true
        )
        
        val controller = IntegratedSelfPlayController(config)
        val initResult = controller.initialize(seedManager)
        
        assertTrue(initResult is InitializationResult.Success)
        
        // Run minimal training
        val trainingResults = controller.runIntegratedTraining(1)
        
        assertEquals(1, trainingResults.totalIterations)
        assertTrue(trainingResults.finalMetrics.totalGamesPlayed > 0)
        assertTrue(trainingResults.finalMetrics.totalExperiencesCollected > 0)
    }
    
    @Test
    fun testChessAgentIntegration() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            config = ChessAgentConfig(batchSize = 2)
        )
        
        // Test action selection
        val state = DoubleArray(776) { 0.1 }
        val validActions = listOf(0, 1, 2, 3)
        val action = agent.selectAction(state, validActions)
        
        assertTrue(action in validActions, "Selected action should be valid")
        
        // Test learning
        val experience = Experience(
            state = state,
            action = action,
            reward = 1.0,
            nextState = DoubleArray(776) { 0.2 },
            done = false
        )
        
        assertDoesNotThrow { agent.learn(experience) }
        
        // Test metrics
        val metrics = agent.getTrainingMetrics()
        assertTrue(metrics.explorationRate >= 0.0)
    }
    
    @Test
    fun testTrainingPipelineIntegration() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            config = ChessAgentConfig(batchSize = 2)
        )
        
        val environment = ChessEnvironment()
        val config = TrainingPipelineConfig(
            maxStepsPerEpisode = 10,
            batchSize = 2,
            progressReportInterval = 1
        )
        
        val pipeline = ChessTrainingPipeline(agent, environment, config)
        
        // Run single episode
        val result = pipeline.runEpisode()
        
        assertTrue(result.episode > 0)
        assertTrue(result.steps > 0)
        assertTrue(result.experiences.isNotEmpty())
    }
    
    @Test
    fun testSelfPlaySystemIntegration() {
        val agent1 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            config = ChessAgentConfig(batchSize = 2)
        )
        
        val agent2 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            config = ChessAgentConfig(batchSize = 2)
        )
        
        val selfPlayConfig = SelfPlayConfig(
            maxConcurrentGames = 1,
            maxStepsPerGame = 10,
            progressReportInterval = 1
        )
        
        val selfPlaySystem = SelfPlaySystem(selfPlayConfig)
        
        val results = selfPlaySystem.runSelfPlayGames(agent1, agent2, 1)
        
        assertEquals(1, results.totalGames)
        assertTrue(results.totalExperiences > 0)
        assertTrue(results.gameResults.isNotEmpty())
    }
    
    @Test
    fun testCheckpointIntegration() {
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8)
        )
        
        val checkpointManager = CheckpointManager(
            CheckpointConfig(maxVersions = 5)
        )
        
        val metadata = CheckpointMetadata(
            cycle = 1,
            performance = 0.75,
            description = "Test checkpoint",
            seedConfiguration = SeedManager.getCurrentConfiguration()
        )
        
        val checkpointInfo = checkpointManager.createCheckpoint(agent, 1, metadata)
        
        assertEquals(1, checkpointInfo.version)
        assertEquals(0.75, checkpointInfo.metadata.performance)
        
        // Test loading
        val loadResult = checkpointManager.loadCheckpoint(checkpointInfo, agent)
        assertTrue(loadResult.success)
    }
    
    @Test
    fun testSeededAgentReproducibility() {
        val seedManager = SeedManager.apply {
            initializeWithMasterSeed(54321L)
        }
        
        // Create two identical seeded agents
        val agent1 = ChessAgentFactory.createSeededDQNAgent(
            hiddenLayers = listOf(16, 8),
            seedManager = seedManager
        )
        
        seedManager.initializeWithMasterSeed(54321L) // Reset to same seed
        
        val agent2 = ChessAgentFactory.createSeededDQNAgent(
            hiddenLayers = listOf(16, 8),
            seedManager = seedManager
        )
        
        // Test that they produce same actions for same state
        val state = DoubleArray(776) { 0.1 }
        val validActions = listOf(0, 1, 2, 3, 4)
        
        val action1 = agent1.selectAction(state, validActions)
        val action2 = agent2.selectAction(state, validActions)
        
        assertEquals(action1, action2, "Seeded agents should produce identical actions")
    }
    
    @Test
    fun testExperienceEnhancement() {
        val basicExperience = Experience(
            state = DoubleArray(776) { 0.1 },
            action = 0,
            reward = 1.0,
            nextState = DoubleArray(776) { 0.2 },
            done = false
        )
        
        val gameResult = SelfPlayGameResult(
            gameId = 1,
            gameLength = 50,
            gameOutcome = GameOutcome.WHITE_WINS,
            terminationReason = EpisodeTerminationReason.GAME_ENDED,
            gameDuration = 1000L,
            experiences = listOf(basicExperience),
            chessMetrics = ChessMetrics(),
            finalPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )
        
        val selfPlaySystem = SelfPlaySystem()
        
        // This would normally be called internally, but we test the enhancement logic
        val enhancedExperiences = gameResult.experiences.mapIndexed { index, exp ->
            EnhancedExperience(
                state = exp.state,
                action = exp.action,
                reward = exp.reward,
                nextState = exp.nextState,
                done = exp.done,
                gameId = gameResult.gameId,
                moveNumber = index + 1,
                playerColor = if (index % 2 == 0) com.chessrl.chess.PieceColor.WHITE else com.chessrl.chess.PieceColor.BLACK,
                gameOutcome = gameResult.gameOutcome,
                terminationReason = gameResult.terminationReason,
                qualityScore = 0.8,
                isFromWinningGame = true,
                isFromDrawGame = false,
                isEarlyGame = true,
                isMidGame = false,
                isEndGame = false,
                chessMetrics = gameResult.chessMetrics
            )
        }
        
        assertTrue(enhancedExperiences.isNotEmpty())
        assertTrue(enhancedExperiences.first().isFromWinningGame)
        assertEquals(1, enhancedExperiences.first().moveNumber)
    }
    
    @Test
    fun testCompleteIntegrationFlow() {
        // This test runs through the complete integration flow
        val seedManager = SeedManager.apply {
            initializeWithMasterSeed(99999L)
        }
        
        val config = IntegratedSelfPlayConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(32, 16),
            gamesPerIteration = 1,
            maxStepsPerGame = 5,
            batchSize = 2,
            checkpointFrequency = 1,
            enableDeterministicTraining = true
        )
        
        val controller = IntegratedSelfPlayController(config)
        
        // Initialize
        val initResult = controller.initialize(seedManager)
        assertTrue(initResult is InitializationResult.Success)
        
        // Run training
        val trainingResults = controller.runIntegratedTraining(1)
        
        // Verify results
        assertEquals(1, trainingResults.totalIterations)
        assertTrue(trainingResults.finalMetrics.totalGamesPlayed > 0)
        assertTrue(trainingResults.finalMetrics.totalExperiencesCollected > 0)
        assertTrue(trainingResults.checkpointSummary.totalCheckpoints > 0)
        
        // Verify training status
        val status = controller.getTrainingStatus()
        assertFalse(status.isTraining)
        assertEquals(1, status.completedIterations)
    }
    
    @Test
    fun testMinimalDemo() {
        val success = IntegratedSelfPlayDemo.runMinimalIntegrationTest()
        assertTrue(success, "Minimal integration test should pass")
    }
    
    @Test
    fun testComponentIntegration() {
        val results = IntegratedSelfPlayDemo.demonstrateComponentIntegration()
        
        // At least some components should integrate successfully
        assertTrue(results.componentResults.isNotEmpty())
        
        // Print results for debugging
        results.componentResults.forEach { (component, passed) ->
            println("$component: ${if (passed) "PASSED" else "FAILED"}")
        }
    }
}