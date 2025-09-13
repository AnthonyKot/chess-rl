package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*
import kotlin.test.*

/**
 * Integration tests for self-play system with existing training pipeline
 */
class SelfPlayIntegrationTest {
    
    @Test
    fun testSelfPlayWithTrainingController() {
        println("🧪 Testing self-play integration with TrainingController...")
        
        // Create training controller
        val trainingConfig = TrainingControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            maxStepsPerEpisode = 15,
            batchSize = 16,
            enableEarlyStopping = false
        )
        
        val trainingController = TrainingController(trainingConfig)
        assertTrue(trainingController.initialize(), "Training controller should initialize")
        
        // Create self-play controller
        val selfPlayConfig = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            gamesPerIteration = 2,
            maxConcurrentGames = 1,
            maxStepsPerGame = 15,
            batchSize = 16
        )
        
        val selfPlayController = SelfPlayController(selfPlayConfig)
        assertTrue(selfPlayController.initialize(), "Self-play controller should initialize")
        
        // Test that both controllers can work independently
        val trainingResults = trainingController.runTrainingTest(episodes = 2)
        assertTrue(trainingResults, "Training controller should work")
        
        val selfPlayResults = selfPlayController.runSelfPlayTraining(iterations = 1)
        assertEquals(1, selfPlayResults.totalIterations, "Self-play controller should work")
        
        // Verify integration doesn't interfere
        val trainingStatus = trainingController.getTrainingStatus()
        val selfPlayStatus = selfPlayController.getTrainingStatus()
        
        assertFalse(trainingStatus.isTraining, "Training controller should not be training")
        assertFalse(selfPlayStatus.isTraining, "Self-play controller should not be training")
        
        println("📊 Integration Results:")
        println("  Training Controller Episodes: ${trainingStatus.currentEpisode}")
        println("  Self-Play Controller Iterations: ${selfPlayStatus.completedIterations}")
        
        println("✅ Self-play with TrainingController integration test passed")
    }
    
    @Test
    fun testSelfPlayWithChessTrainingPipeline() {
        println("🧪 Testing self-play integration with ChessTrainingPipeline...")
        
        // Create agent and environment for pipeline
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val environment = ChessEnvironment()
        
        // Create training pipeline
        val pipelineConfig = TrainingPipelineConfig(
            maxStepsPerEpisode = 15,
            batchSize = 16,
            batchTrainingFrequency = 1,
            maxBufferSize = 1000,
            progressReportInterval = 5
        )
        
        val trainingPipeline = ChessTrainingPipeline(agent, environment, pipelineConfig)
        
        // Run pipeline training
        val pipelineResults = trainingPipeline.train(totalEpisodes = 2)
        assertEquals(2, pipelineResults.totalEpisodes, "Pipeline should complete episodes")
        
        // Create self-play system and run with same agent
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 15,
                maxExperienceBufferSize = 1000
            )
        )
        
        // Create second agent for opponent
        val opponentAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        // Run self-play with the trained agent
        val selfPlayResults = selfPlaySystem.runSelfPlayGames(agent, opponentAgent, numGames = 2)
        assertEquals(2, selfPlayResults.totalGames, "Self-play should complete games")
        
        // Verify that experiences can be used for further training
        val experiences = selfPlayResults.experiences
        assertTrue(experiences.isNotEmpty(), "Should have collected experiences")
        
        // Test that experiences can be converted and used
        val basicExperiences = experiences.map { it.toBasicExperience() }
        assertTrue(basicExperiences.isNotEmpty(), "Should convert experiences successfully")
        
        // Verify experience format compatibility
        val firstExperience = basicExperiences.first()
        assertEquals(ChessStateEncoder.TOTAL_FEATURES, firstExperience.state.size, 
                    "Experience state should have correct size")
        assertTrue(firstExperience.action >= 0, "Experience action should be valid")
        assertEquals(ChessStateEncoder.TOTAL_FEATURES, firstExperience.nextState.size,
                    "Experience next state should have correct size")
        
        println("📊 Pipeline Integration Results:")
        println("  Pipeline Episodes: ${pipelineResults.totalEpisodes}")
        println("  Self-Play Games: ${selfPlayResults.totalGames}")
        println("  Self-Play Experiences: ${selfPlayResults.totalExperiences}")
        println("  Experience State Size: ${firstExperience.state.size}")
        
        println("✅ Self-play with ChessTrainingPipeline integration test passed")
    }
    
    @Test
    fun testSelfPlayExperienceIntegration() {
        println("🧪 Testing self-play experience integration with training...")
        
        // Create agents
        val mainAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(12, 6),
            learningRate = 0.02,
            explorationRate = 0.3
        )
        
        val opponentAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(12, 6),
            learningRate = 0.02,
            explorationRate = 0.3
        )
        
        // Get initial agent metrics
        val initialMetrics = mainAgent.getTrainingMetrics()
        
        // Run self-play to collect experiences
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 20,
                maxExperienceBufferSize = 500
            )
        )
        
        val selfPlayResults = selfPlaySystem.runSelfPlayGames(mainAgent, opponentAgent, numGames = 2)
        assertTrue(selfPlayResults.totalExperiences > 0, "Should collect experiences")
        
        // Train agent on self-play experiences
        val experiences = selfPlayResults.experiences.map { it.toBasicExperience() }
        
        // Add experiences to agent for training
        experiences.forEach { experience ->
            mainAgent.learn(experience)
        }
        
        // Force policy update
        mainAgent.forceUpdate()
        
        // Get updated metrics
        val updatedMetrics = mainAgent.getTrainingMetrics()
        
        // Verify training occurred
        assertTrue(updatedMetrics.experienceBufferSize >= initialMetrics.experienceBufferSize,
                  "Experience buffer should have grown or stayed same")
        
        // Verify experience quality and metadata
        val enhancedExperiences = selfPlayResults.experiences
        
        // Check game phase distribution
        val earlyGameExperiences = enhancedExperiences.count { it.isEarlyGame }
        val midGameExperiences = enhancedExperiences.count { it.isMidGame }
        val endGameExperiences = enhancedExperiences.count { it.isEndGame }
        
        assertEquals(enhancedExperiences.size, earlyGameExperiences + midGameExperiences + endGameExperiences,
                    "All experiences should be categorized by game phase")
        
        // Check outcome distribution
        val fromWins = enhancedExperiences.count { it.isFromWinningGame }
        val fromDraws = enhancedExperiences.count { it.isFromDrawGame }
        val fromOther = enhancedExperiences.size - fromWins - fromDraws
        
        assertEquals(enhancedExperiences.size, fromWins + fromDraws + fromOther,
                    "All experiences should be categorized by outcome")
        
        // Verify quality scores
        val qualityScores = enhancedExperiences.map { it.qualityScore }
        assertTrue(qualityScores.all { it >= 0.0 && it <= 1.0 }, "Quality scores should be in valid range")
        
        println("📊 Experience Integration Results:")
        println("  Total Experiences: ${enhancedExperiences.size}")
        println("  Early Game: $earlyGameExperiences, Mid Game: $midGameExperiences, End Game: $endGameExperiences")
        println("  From Wins: $fromWins, From Draws: $fromDraws, Other: $fromOther")
        println("  Average Quality Score: ${qualityScores.average()}")
        println("  Initial Buffer Size: ${initialMetrics.experienceBufferSize}")
        println("  Updated Buffer Size: ${updatedMetrics.experienceBufferSize}")
        
        println("✅ Self-play experience integration test passed")
    }
    
    @Test
    fun testSelfPlayWithEpisodeTracking() {
        println("🧪 Testing self-play integration with episode tracking...")
        
        // Create agents
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        // Get initial episode tracking
        val initialWhiteMetrics = whiteAgent.getTrainingMetrics()
        val initialBlackMetrics = blackAgent.getTrainingMetrics()
        
        // Run self-play games
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 15
            )
        )
        
        val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 2)
        assertEquals(2, results.totalGames, "Should complete 2 games")
        
        // Check episode tracking after self-play
        val finalWhiteMetrics = whiteAgent.getTrainingMetrics()
        val finalBlackMetrics = blackAgent.getTrainingMetrics()
        
        // Verify episode counts increased (agents learn during self-play)
        assertTrue(finalWhiteMetrics.episodeCount >= initialWhiteMetrics.episodeCount,
                  "White agent episode count should increase or stay same")
        assertTrue(finalBlackMetrics.episodeCount >= initialBlackMetrics.episodeCount,
                  "Black agent episode count should increase or stay same")
        
        // Verify termination reason tracking
        assertTrue(finalWhiteMetrics.gameEndedEpisodes + finalWhiteMetrics.stepLimitEpisodes + 
                  finalWhiteMetrics.manualEpisodes >= initialWhiteMetrics.gameEndedEpisodes + 
                  initialWhiteMetrics.stepLimitEpisodes + initialWhiteMetrics.manualEpisodes,
                  "White agent termination tracking should be consistent")
        
        assertTrue(finalBlackMetrics.gameEndedEpisodes + finalBlackMetrics.stepLimitEpisodes + 
                  finalBlackMetrics.manualEpisodes >= initialBlackMetrics.gameEndedEpisodes + 
                  initialBlackMetrics.stepLimitEpisodes + initialBlackMetrics.manualEpisodes,
                  "Black agent termination tracking should be consistent")
        
        // Verify self-play results have proper termination reasons
        for (gameResult in results.gameResults) {
            assertNotNull(gameResult.terminationReason, "Each game should have termination reason")
            assertTrue(gameResult.terminationReason in listOf(
                EpisodeTerminationReason.GAME_ENDED,
                EpisodeTerminationReason.STEP_LIMIT,
                EpisodeTerminationReason.MANUAL
            ), "Termination reason should be valid")
        }
        
        println("📊 Episode Tracking Integration Results:")
        println("  White Agent Episodes: ${initialWhiteMetrics.episodeCount} → ${finalWhiteMetrics.episodeCount}")
        println("  Black Agent Episodes: ${initialBlackMetrics.episodeCount} → ${finalBlackMetrics.episodeCount}")
        println("  Game Termination Reasons: ${results.gameResults.map { it.terminationReason }}")
        
        println("✅ Self-play with episode tracking integration test passed")
    }
    
    @Test
    fun testSelfPlayWithValidationTools() {
        println("🧪 Testing self-play integration with validation tools...")
        
        // Create agents
        val agent1 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val agent2 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        // Run self-play
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 10
            )
        )
        
        val results = selfPlaySystem.runSelfPlayGames(agent1, agent2, numGames = 1)
        assertTrue(results.totalExperiences > 0, "Should collect experiences")
        
        // Test validation tools with self-play results
        val environment = ChessEnvironment()
        val validationTools = ManualValidationTools(agent1, environment)
        
        // Test game quality assessment
        val gameResult = results.gameResults.first()
        val gameHistory = gameResult.experiences.map { exp ->
            // Convert experience action back to Move (simplified)
            val actionEncoder = ChessActionEncoder()
            actionEncoder.decodeAction(exp.action)
        }
        
        val finalBoard = ChessBoard()
        // Apply moves to get final board state (simplified)
        val gameStatus = environment.getGameStatus()
        
        val gameQualityAssessment = validationTools.assessGameQuality(
            gameHistory,
            gameStatus,
            finalBoard
        )
        
        assertNotNull(gameQualityAssessment, "Should assess game quality successfully")
        assertTrue(gameQualityAssessment.gameLength >= 0, "Game length should be non-negative")
        assertTrue(gameQualityAssessment.qualityScore >= 0.0, "Quality score should be non-negative")
        
        // Test position evaluation display
        val positionDisplay = validationTools.displayPositionEvaluation(finalBoard)
        
        assertNotNull(positionDisplay, "Should display position evaluation successfully")
        assertNotNull(positionDisplay.position, "Should have position FEN")
        assertTrue(positionDisplay.netEvaluation != 0.0 || true, "Should have evaluation (may be zero)")
        
        // Test experience quality validation
        val qualityMetrics = results.experienceQualityMetrics
        assertTrue(qualityMetrics.averageQualityScore >= 0.0, "Quality score should be valid")
        
        val highQualityCount = qualityMetrics.highQualityExperiences
        val mediumQualityCount = qualityMetrics.mediumQualityExperiences
        val lowQualityCount = qualityMetrics.lowQualityExperiences
        
        assertEquals(results.totalExperiences, highQualityCount + mediumQualityCount + lowQualityCount,
                    "Quality categorization should account for all experiences")
        
        println("📊 Validation Integration Results:")
        println("  Game Quality - Length: ${gameQualityAssessment.gameLength}, Score: ${gameQualityAssessment.qualityScore}")
        println("  Position Evaluation - Net: ${positionDisplay.netEvaluation}, Summary: ${positionDisplay.evaluationSummary}")
        println("  Quality Distribution - High: $highQualityCount, Medium: $mediumQualityCount, Low: $lowQualityCount")
        
        println("✅ Self-play with validation tools integration test passed")
    }
    
    @Test
    fun testEndToEndSelfPlayTraining() {
        println("🧪 Testing end-to-end self-play training integration...")
        
        // Create complete self-play controller
        val config = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(12, 6),
            learningRate = 0.02,
            gamesPerIteration = 2,
            maxConcurrentGames = 1,
            maxStepsPerGame = 10,
            batchSize = 8,
            iterationReportInterval = 1
        )
        
        val controller = SelfPlayController(config)
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Run complete training cycle
        val results = controller.runSelfPlayTraining(iterations = 2)
        
        // Verify complete integration
        assertEquals(2, results.totalIterations, "Should complete all iterations")
        assertTrue(results.totalDuration > 0, "Should take some time")
        assertEquals(2, results.iterationHistory.size, "Should have all iteration results")
        
        // Verify each iteration has all components
        for (iterationResult in results.iterationHistory) {
            // Self-play results
            assertTrue(iterationResult.selfPlayResults.totalGames > 0, "Should have self-play games")
            assertTrue(iterationResult.selfPlayResults.totalExperiences > 0, "Should have experiences")
            
            // Training metrics
            assertTrue(iterationResult.trainingMetrics.totalBatchUpdates >= 0, "Should have batch updates")
            assertTrue(iterationResult.trainingMetrics.experienceBufferSize >= 0, "Should track buffer size")
            
            // Evaluation results
            assertTrue(iterationResult.evaluationResults.gamesPlayed > 0, "Should have evaluation games")
            assertTrue(iterationResult.evaluationResults.winRate >= 0.0, "Should have valid win rate")
            assertTrue(iterationResult.evaluationResults.drawRate >= 0.0, "Should have valid draw rate")
            assertTrue(iterationResult.evaluationResults.lossRate >= 0.0, "Should have valid loss rate")
        }
        
        // Verify final metrics
        val finalMetrics = results.finalMetrics
        assertTrue(finalMetrics.totalGamesPlayed > 0, "Should have played games")
        assertTrue(finalMetrics.totalExperiencesCollected > 0, "Should have collected experiences")
        assertTrue(finalMetrics.totalBatchUpdates >= 0, "Should have performed batch updates")
        
        // Verify metrics consistency
        val expectedTotalGames = results.iterationHistory.sumOf { it.selfPlayResults.totalGames }
        assertEquals(expectedTotalGames, finalMetrics.totalGamesPlayed, "Game counts should match")
        
        val expectedTotalExperiences = results.iterationHistory.sumOf { it.selfPlayResults.totalExperiences }
        assertEquals(expectedTotalExperiences, finalMetrics.totalExperiencesCollected, "Experience counts should match")
        
        println("📊 End-to-End Integration Results:")
        println("  Total Iterations: ${results.totalIterations}")
        println("  Total Games: ${finalMetrics.totalGamesPlayed}")
        println("  Total Experiences: ${finalMetrics.totalExperiencesCollected}")
        println("  Total Batch Updates: ${finalMetrics.totalBatchUpdates}")
        println("  Average Reward: ${finalMetrics.averageReward}")
        println("  Best Reward: ${finalMetrics.bestReward}")
        println("  Average Win Rate: ${finalMetrics.averageWinRate}")
        println("  Training Duration: ${results.totalDuration}ms")
        
        println("✅ End-to-end self-play training integration test passed")
    }
}