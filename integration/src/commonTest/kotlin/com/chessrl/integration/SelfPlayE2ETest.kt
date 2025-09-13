package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*
import kotlin.test.*

/**
 * End-to-end test for self-play system to validate realistic training scenarios
 */
class SelfPlayE2ETest {
    
    @Test
    fun testRealisticSelfPlayTrainingScenario() {
        println("🎯 E2E Test: Realistic Self-Play Training Scenario")
        println("=" * 60)
        
        // Create a realistic training configuration
        val config = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(64, 32, 16), // Reasonable network size
            learningRate = 0.001,
            explorationRate = 0.2,
            
            // Self-play configuration
            gamesPerIteration = 5, // Small but meaningful batch
            maxConcurrentGames = 2, // Test concurrency
            maxStepsPerGame = 50, // Allow for meaningful games
            
            // Training configuration
            batchSize = 32,
            maxExperienceBufferSize = 1000,
            samplingStrategy = SamplingStrategy.MIXED,
            
            // Opponent strategy
            opponentUpdateStrategy = OpponentUpdateStrategy.COPY_MAIN,
            opponentUpdateFrequency = 2,
            
            // Monitoring
            iterationReportInterval = 1,
            progressReportInterval = 2
        )
        
        println("📋 Configuration:")
        println("  Agent Type: ${config.agentType}")
        println("  Network: ${config.hiddenLayers}")
        println("  Games per Iteration: ${config.gamesPerIteration}")
        println("  Max Concurrent Games: ${config.maxConcurrentGames}")
        println("  Max Steps per Game: ${config.maxStepsPerGame}")
        println("  Batch Size: ${config.batchSize}")
        println()
        
        // Initialize controller
        val controller = SelfPlayController(config)
        assertTrue(controller.initialize(), "Controller should initialize successfully")
        
        println("✅ Controller initialized successfully")
        
        // Run a realistic training session (3 iterations)
        println("\n🚀 Starting Self-Play Training (3 iterations)...")
        val trainingResults = controller.runSelfPlayTraining(iterations = 3)
        
        // Validate training completed successfully
        assertEquals(3, trainingResults.totalIterations, "Should complete all 3 iterations")
        assertEquals(3, trainingResults.iterationHistory.size, "Should have 3 iteration results")
        assertTrue(trainingResults.totalDuration >= 0, "Training duration should be non-negative")
        
        println("\n📊 Training Results Summary:")
        println("  Total Iterations: ${trainingResults.totalIterations}")
        println("  Training Duration: ${trainingResults.totalDuration}ms")
        println("  Best Performance: ${trainingResults.bestPerformance}")
        
        // Analyze each iteration for realistic progression
        println("\n📈 Iteration Analysis:")
        for (iteration in trainingResults.iterationHistory) {
            println("  Iteration ${iteration.iteration}:")
            
            // Validate self-play results
            val selfPlayResults = iteration.selfPlayResults
            assertEquals(config.gamesPerIteration, selfPlayResults.totalGames, 
                        "Should play configured number of games")
            assertTrue(selfPlayResults.totalExperiences > 0, 
                      "Should collect experiences from games")
            assertTrue(selfPlayResults.averageGameLength > 0, 
                      "Games should have positive length")
            
            println("    Self-Play: ${selfPlayResults.totalGames} games, ${selfPlayResults.totalExperiences} experiences")
            println("    Avg Game Length: ${selfPlayResults.averageGameLength} moves")
            
            // Validate training metrics
            val trainingMetrics = iteration.trainingMetrics
            assertTrue(trainingMetrics.totalBatchUpdates >= 0, 
                      "Should have non-negative batch updates")
            assertTrue(trainingMetrics.experienceBufferSize >= 0, 
                      "Should have non-negative buffer size")
            
            println("    Training: ${trainingMetrics.totalBatchUpdates} batch updates, buffer size ${trainingMetrics.experienceBufferSize}")
            
            // Validate evaluation results
            val evaluationResults = iteration.evaluationResults
            assertTrue(evaluationResults.gamesPlayed > 0, 
                      "Should play evaluation games")
            assertTrue(evaluationResults.averageGameLength > 0, 
                      "Evaluation games should have positive length")
            
            // Validate win/draw/loss rates sum to 1.0
            val totalRate = evaluationResults.winRate + evaluationResults.drawRate + evaluationResults.lossRate
            assertTrue(kotlin.math.abs(totalRate - 1.0) < 0.01, 
                      "Win/draw/loss rates should sum to approximately 1.0")
            
            println("    Evaluation: ${evaluationResults.gamesPlayed} games, W:${(evaluationResults.winRate*100).toInt()}% D:${(evaluationResults.drawRate*100).toInt()}% L:${(evaluationResults.lossRate*100).toInt()}%")
        }
        
        // Validate final metrics make sense
        val finalMetrics = trainingResults.finalMetrics
        assertEquals(config.gamesPerIteration * 3, finalMetrics.totalGamesPlayed, 
                    "Total games should match iterations × games per iteration")
        assertTrue(finalMetrics.totalExperiencesCollected > 0, 
                  "Should have collected experiences")
        assertTrue(finalMetrics.totalBatchUpdates >= 0, 
                  "Should have performed batch updates")
        
        println("\n🎯 Final Metrics:")
        println("  Total Games Played: ${finalMetrics.totalGamesPlayed}")
        println("  Total Experiences: ${finalMetrics.totalExperiencesCollected}")
        println("  Total Batch Updates: ${finalMetrics.totalBatchUpdates}")
        println("  Average Reward: ${finalMetrics.averageReward}")
        println("  Average Win Rate: ${(finalMetrics.averageWinRate * 100).toInt()}%")
        println("  Average Game Length: ${finalMetrics.averageGameLength} moves")
        
        println("\n✅ E2E Test: Realistic Self-Play Training - PASSED")
    }
    
    @Test
    fun testSelfPlayExperienceQualityProgression() {
        println("🎯 E2E Test: Self-Play Experience Quality Progression")
        println("=" * 60)
        
        // Create agents with different exploration rates to see learning
        val mainAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            explorationRate = 0.3 // Higher exploration for learning
        )
        
        val opponentAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            explorationRate = 0.1 // Lower exploration for more consistent play
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 2,
                maxStepsPerGame = 30,
                maxExperienceBufferSize = 500,
                experienceCleanupStrategy = ExperienceCleanupStrategy.LOWEST_QUALITY,
                progressReportInterval = 3
            )
        )
        
        println("📋 Testing experience quality progression over multiple game batches...")
        
        val gameBatches = listOf(3, 4, 5) // Increasing game counts
        val qualityProgression = mutableListOf<Double>()
        
        for ((batchIndex, numGames) in gameBatches.withIndex()) {
            println("\n🎮 Game Batch ${batchIndex + 1}: $numGames games")
            
            val results = selfPlaySystem.runSelfPlayGames(mainAgent, opponentAgent, numGames)
            
            // Validate basic results
            assertEquals(numGames, results.totalGames, "Should complete all games in batch")
            assertTrue(results.totalExperiences > 0, "Should collect experiences")
            
            // Analyze experience quality
            val qualityMetrics = results.experienceQualityMetrics
            qualityProgression.add(qualityMetrics.averageQualityScore)
            
            println("  Games: ${results.totalGames}")
            println("  Experiences: ${results.totalExperiences}")
            println("  Average Quality Score: ${qualityMetrics.averageQualityScore}")
            println("  High Quality: ${qualityMetrics.highQualityExperiences}")
            println("  Medium Quality: ${qualityMetrics.mediumQualityExperiences}")
            println("  Low Quality: ${qualityMetrics.lowQualityExperiences}")
            
            // Validate quality distribution
            val totalCategorized = qualityMetrics.highQualityExperiences + 
                                  qualityMetrics.mediumQualityExperiences + 
                                  qualityMetrics.lowQualityExperiences
            assertEquals(results.totalExperiences, totalCategorized, 
                        "All experiences should be categorized by quality")
            
            // Validate outcome distribution
            val totalByOutcome = qualityMetrics.experiencesFromWins + 
                               qualityMetrics.experiencesFromDraws + 
                               qualityMetrics.experiencesFromIncomplete
            assertEquals(results.totalExperiences, totalByOutcome, 
                        "All experiences should be categorized by outcome")
            
            // Analyze game outcomes
            val gameOutcomes = results.gameOutcomes
            val totalOutcomes = gameOutcomes.values.sum()
            assertEquals(numGames, totalOutcomes, "All games should have outcomes")
            
            println("  Game Outcomes: $gameOutcomes")
            
            // Validate individual experiences
            if (results.experiences.isNotEmpty()) {
                val sampleExperience = results.experiences.first()
                
                // Validate enhanced metadata
                assertTrue(sampleExperience.gameId > 0, "Experience should have valid game ID")
                assertTrue(sampleExperience.moveNumber > 0, "Experience should have valid move number")
                assertNotNull(sampleExperience.playerColor, "Experience should have player color")
                assertTrue(sampleExperience.qualityScore >= 0.0 && sampleExperience.qualityScore <= 1.0, 
                          "Quality score should be in valid range")
                
                // Validate game phase classification
                val hasPhaseClassification = sampleExperience.isEarlyGame || 
                                           sampleExperience.isMidGame || 
                                           sampleExperience.isEndGame
                assertTrue(hasPhaseClassification, "Experience should be classified by game phase")
                
                println("  Sample Experience: Game ${sampleExperience.gameId}, Move ${sampleExperience.moveNumber}, Quality ${sampleExperience.qualityScore}")
            }
        }
        
        println("\n📈 Quality Progression Analysis:")
        for ((index, quality) in qualityProgression.withIndex()) {
            println("  Batch ${index + 1}: Quality Score ${quality}")
        }
        
        // Validate that we're collecting meaningful data
        assertTrue(qualityProgression.all { it > 0.0 }, "All quality scores should be positive")
        assertTrue(qualityProgression.all { it <= 1.0 }, "All quality scores should be <= 1.0")
        
        println("\n✅ E2E Test: Experience Quality Progression - PASSED")
    }
    
    @Test
    fun testSelfPlayWithDifferentAgentTypes() {
        println("🎯 E2E Test: Self-Play with Different Agent Types")
        println("=" * 60)
        
        val agentTypes = listOf(AgentType.DQN, AgentType.POLICY_GRADIENT)
        
        for (agentType in agentTypes) {
            println("\n🤖 Testing Agent Type: $agentType")
            
            val config = SelfPlayControllerConfig(
                agentType = agentType,
                hiddenLayers = listOf(16, 8),
                learningRate = 0.02,
                explorationRate = if (agentType == AgentType.DQN) 0.2 else 0.0,
                temperature = if (agentType == AgentType.POLICY_GRADIENT) 1.5 else 1.0,
                gamesPerIteration = 2,
                maxConcurrentGames = 1,
                maxStepsPerGame = 20,
                batchSize = 16
            )
            
            val controller = SelfPlayController(config)
            assertTrue(controller.initialize(), "Controller should initialize for $agentType")
            
            val results = controller.runSelfPlayTraining(iterations = 1)
            
            // Validate results for this agent type
            assertEquals(1, results.totalIterations, "Should complete 1 iteration for $agentType")
            
            val iteration = results.iterationHistory.first()
            assertTrue(iteration.selfPlayResults.totalGames > 0, "Should play games with $agentType")
            assertTrue(iteration.trainingMetrics.totalBatchUpdates >= 0, "Should have batch updates for $agentType")
            assertTrue(iteration.evaluationResults.gamesPlayed > 0, "Should evaluate $agentType")
            
            println("  ✅ $agentType: ${iteration.selfPlayResults.totalGames} games, ${iteration.selfPlayResults.totalExperiences} experiences")
        }
        
        println("\n✅ E2E Test: Different Agent Types - PASSED")
    }
    
    @Test
    fun testSelfPlayMemoryManagement() {
        println("🎯 E2E Test: Self-Play Memory Management")
        println("=" * 60)
        
        // Test with small buffer to trigger cleanup
        val config = SelfPlayConfig(
            maxConcurrentGames = 1,
            maxStepsPerGame = 25,
            maxExperienceBufferSize = 50, // Small buffer
            experienceCleanupStrategy = ExperienceCleanupStrategy.OLDEST_FIRST,
            progressReportInterval = 2
        )
        
        val agent1 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.3
        )
        
        val agent2 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.3
        )
        
        val selfPlaySystem = SelfPlaySystem(config)
        
        println("📋 Testing memory management with buffer size: ${config.maxExperienceBufferSize}")
        
        // Run enough games to exceed buffer capacity
        val results = selfPlaySystem.runSelfPlayGames(agent1, agent2, numGames = 5)
        
        println("\n📊 Memory Management Results:")
        println("  Total Games: ${results.totalGames}")
        println("  Total Experiences Generated: ${results.totalExperiences}")
        println("  Experiences in Buffer: ${results.experiences.size}")
        println("  Buffer Limit: ${config.maxExperienceBufferSize}")
        
        // Validate memory management
        assertTrue(results.experiences.size <= config.maxExperienceBufferSize, 
                  "Experience buffer should not exceed maximum size")
        assertTrue(results.experiences.isNotEmpty(), 
                  "Should still have experiences after cleanup")
        
        // Test different cleanup strategies
        val cleanupStrategies = listOf(
            ExperienceCleanupStrategy.OLDEST_FIRST,
            ExperienceCleanupStrategy.LOWEST_QUALITY,
            ExperienceCleanupStrategy.RANDOM
        )
        
        for (strategy in cleanupStrategies) {
            val testConfig = config.copy(experienceCleanupStrategy = strategy)
            val testSystem = SelfPlaySystem(testConfig)
            
            val testResults = testSystem.runSelfPlayGames(agent1, agent2, numGames = 3)
            
            assertTrue(testResults.experiences.size <= testConfig.maxExperienceBufferSize,
                      "Buffer should be managed with strategy: $strategy")
            
            println("  ✅ Cleanup Strategy $strategy: ${testResults.experiences.size} experiences retained")
        }
        
        println("\n✅ E2E Test: Memory Management - PASSED")
    }
    
    @Test
    fun testSelfPlayIntegrationWithExistingPipeline() {
        println("🎯 E2E Test: Integration with Existing Training Pipeline")
        println("=" * 60)
        
        // Test that self-play can work alongside existing training
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(24, 12),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val environment = ChessEnvironment()
        
        println("📋 Testing integration with existing training components...")
        
        // 1. Run traditional training pipeline
        println("\n🔄 Phase 1: Traditional Training Pipeline")
        val pipelineConfig = TrainingPipelineConfig(
            maxStepsPerEpisode = 20,
            batchSize = 16,
            batchTrainingFrequency = 1,
            maxBufferSize = 200,
            progressReportInterval = 2
        )
        
        val trainingPipeline = ChessTrainingPipeline(agent, environment, pipelineConfig)
        val pipelineResults = trainingPipeline.train(totalEpisodes = 3)
        
        assertEquals(3, pipelineResults.totalEpisodes, "Pipeline should complete episodes")
        
        val initialMetrics = agent.getTrainingMetrics()
        println("  Traditional Training: ${pipelineResults.totalEpisodes} episodes")
        println("  Agent Episodes: ${initialMetrics.episodeCount}")
        println("  Agent Buffer Size: ${initialMetrics.experienceBufferSize}")
        
        // 2. Run self-play training with the same agent
        println("\n🔄 Phase 2: Self-Play Training")
        val opponentAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(24, 12),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 20,
                maxExperienceBufferSize = 200
            )
        )
        
        val selfPlayResults = selfPlaySystem.runSelfPlayGames(agent, opponentAgent, numGames = 2)
        
        assertEquals(2, selfPlayResults.totalGames, "Self-play should complete games")
        
        val finalMetrics = agent.getTrainingMetrics()
        println("  Self-Play Training: ${selfPlayResults.totalGames} games")
        println("  Agent Episodes After: ${finalMetrics.episodeCount}")
        println("  Agent Buffer Size After: ${finalMetrics.experienceBufferSize}")
        
        // 3. Validate integration worked correctly
        assertTrue(finalMetrics.episodeCount >= initialMetrics.episodeCount, 
                  "Agent episode count should increase or stay same")
        
        // 4. Test that experiences are compatible
        if (selfPlayResults.experiences.isNotEmpty()) {
            val experience = selfPlayResults.experiences.first()
            val basicExperience = experience.toBasicExperience()
            
            // Validate experience format
            assertEquals(ChessStateEncoder.TOTAL_FEATURES, basicExperience.state.size, 
                        "Experience state should have correct size")
            assertTrue(basicExperience.action >= 0, "Experience action should be valid")
            assertEquals(ChessStateEncoder.TOTAL_FEATURES, basicExperience.nextState.size,
                        "Experience next state should have correct size")
            
            println("  ✅ Experience Compatibility: State size ${basicExperience.state.size}, Action ${basicExperience.action}")
        }
        
        println("\n✅ E2E Test: Integration with Existing Pipeline - PASSED")
    }
}