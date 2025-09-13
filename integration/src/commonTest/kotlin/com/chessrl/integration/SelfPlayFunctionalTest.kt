package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*
import kotlin.test.*

/**
 * Functional end-to-end test for self-play system focusing on core functionality
 */
class SelfPlayFunctionalTest {
    
    @Test
    fun testCompleteSelfPlayWorkflow() {
        println("🎯 Functional Test: Complete Self-Play Workflow")
        println("=" * 50)
        
        // Step 1: Create and initialize controller
        println("\n📋 Step 1: Initialize Self-Play Controller")
        val config = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(32, 16),
            learningRate = 0.01,
            explorationRate = 0.3,
            gamesPerIteration = 3,
            maxConcurrentGames = 2,
            maxStepsPerGame = 25,
            batchSize = 16,
            iterationReportInterval = 1
        )
        
        val controller = SelfPlayController(config)
        assertTrue(controller.initialize(), "Controller should initialize successfully")
        
        val initialStatus = controller.getTrainingStatus()
        assertFalse(initialStatus.isTraining, "Should not be training initially")
        assertEquals(0, initialStatus.currentIteration, "Initial iteration should be 0")
        
        println("✅ Controller initialized successfully")
        
        // Step 2: Run self-play training
        println("\n🚀 Step 2: Execute Self-Play Training")
        val trainingResults = controller.runSelfPlayTraining(iterations = 2)
        
        // Validate training results
        assertEquals(2, trainingResults.totalIterations, "Should complete 2 iterations")
        assertEquals(2, trainingResults.iterationHistory.size, "Should have 2 iteration results")
        assertTrue(trainingResults.bestPerformance != Double.NEGATIVE_INFINITY, "Should have performance metric")
        
        println("✅ Training completed: ${trainingResults.totalIterations} iterations")
        
        // Step 3: Validate iteration results
        println("\n📊 Step 3: Validate Iteration Results")
        for (iteration in trainingResults.iterationHistory) {
            println("  Iteration ${iteration.iteration}:")
            
            // Validate self-play results
            val selfPlayResults = iteration.selfPlayResults
            assertEquals(config.gamesPerIteration, selfPlayResults.totalGames, 
                        "Should play ${config.gamesPerIteration} games")
            assertTrue(selfPlayResults.totalExperiences >= 0, "Should have non-negative experiences")
            assertTrue(selfPlayResults.averageGameLength >= 0, "Should have non-negative game length")
            
            println("    Self-Play: ${selfPlayResults.totalGames} games, ${selfPlayResults.totalExperiences} experiences")
            
            // Validate training metrics
            val trainingMetrics = iteration.trainingMetrics
            assertTrue(trainingMetrics.totalBatchUpdates >= 0, "Should have non-negative batch updates")
            assertTrue(trainingMetrics.experienceBufferSize >= 0, "Should have non-negative buffer size")
            assertTrue(trainingMetrics.averageLoss >= 0.0, "Should have non-negative loss")
            
            println("    Training: ${trainingMetrics.totalBatchUpdates} updates, loss ${trainingMetrics.averageLoss}")
            
            // Validate evaluation results
            val evaluationResults = iteration.evaluationResults
            assertTrue(evaluationResults.gamesPlayed >= 0, "Should have non-negative evaluation games")
            assertTrue(evaluationResults.averageGameLength >= 0, "Should have non-negative evaluation length")
            assertTrue(evaluationResults.winRate >= 0.0 && evaluationResults.winRate <= 1.0, 
                      "Win rate should be between 0 and 1")
            assertTrue(evaluationResults.drawRate >= 0.0 && evaluationResults.drawRate <= 1.0, 
                      "Draw rate should be between 0 and 1")
            assertTrue(evaluationResults.lossRate >= 0.0 && evaluationResults.lossRate <= 1.0, 
                      "Loss rate should be between 0 and 1")
            
            println("    Evaluation: ${evaluationResults.gamesPlayed} games")
        }
        
        // Step 4: Validate final metrics
        println("\n🎯 Step 4: Validate Final Metrics")
        val finalMetrics = trainingResults.finalMetrics
        
        val expectedTotalGames = config.gamesPerIteration * trainingResults.totalIterations
        assertEquals(expectedTotalGames, finalMetrics.totalGamesPlayed, 
                    "Total games should match iterations × games per iteration")
        
        assertTrue(finalMetrics.totalExperiencesCollected >= 0, "Should have collected experiences")
        assertTrue(finalMetrics.totalBatchUpdates >= 0, "Should have performed batch updates")
        assertTrue(finalMetrics.averageGameLength >= 0, "Should have positive average game length")
        
        println("✅ Final Metrics Validated:")
        println("  Total Games: ${finalMetrics.totalGamesPlayed}")
        println("  Total Experiences: ${finalMetrics.totalExperiencesCollected}")
        println("  Average Game Length: ${finalMetrics.averageGameLength}")
        
        // Step 5: Validate controller status after training
        println("\n📈 Step 5: Validate Post-Training Status")
        val finalStatus = controller.getTrainingStatus()
        assertFalse(finalStatus.isTraining, "Should not be training after completion")
        assertEquals(2, finalStatus.completedIterations, "Should have completed 2 iterations")
        
        println("✅ Complete Self-Play Workflow - PASSED")
    }
    
    @Test
    fun testSelfPlayExperienceCollection() {
        println("🎯 Functional Test: Self-Play Experience Collection")
        println("=" * 50)
        
        // Create agents with different configurations
        val agent1 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(24, 12),
            learningRate = 0.02,
            explorationRate = 0.4
        )
        
        val agent2 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(24, 12),
            learningRate = 0.02,
            explorationRate = 0.2
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 2,
                maxStepsPerGame = 20,
                maxExperienceBufferSize = 300,
                experienceCleanupStrategy = ExperienceCleanupStrategy.LOWEST_QUALITY,
                progressReportInterval = 2
            )
        )
        
        println("\n🎮 Running self-play games for experience collection...")
        val results = selfPlaySystem.runSelfPlayGames(agent1, agent2, numGames = 4)
        
        // Validate basic results
        assertEquals(4, results.totalGames, "Should complete 4 games")
        assertTrue(results.totalExperiences >= 0, "Should collect experiences")
        assertEquals(4, results.gameResults.size, "Should have 4 game results")
        
        println("✅ Games completed: ${results.totalGames}")
        println("✅ Experiences collected: ${results.totalExperiences}")
        
        // Validate experience enhancement
        if (results.experiences.isNotEmpty()) {
            println("\n📊 Validating Experience Enhancement:")
            
            val sampleExperience = results.experiences.first()
            
            // Validate core experience data
            assertNotNull(sampleExperience.state, "Experience should have state")
            assertTrue(sampleExperience.action >= 0, "Experience should have valid action")
            assertNotNull(sampleExperience.nextState, "Experience should have next state")
            
            // Validate enhanced metadata
            assertTrue(sampleExperience.gameId > 0, "Experience should have game ID")
            assertTrue(sampleExperience.moveNumber > 0, "Experience should have move number")
            assertNotNull(sampleExperience.playerColor, "Experience should have player color")
            assertNotNull(sampleExperience.gameOutcome, "Experience should have game outcome")
            assertNotNull(sampleExperience.terminationReason, "Experience should have termination reason")
            
            // Validate quality metrics
            assertTrue(sampleExperience.qualityScore >= 0.0 && sampleExperience.qualityScore <= 1.0, 
                      "Quality score should be between 0 and 1")
            
            // Validate game phase classification
            val hasPhaseClassification = sampleExperience.isEarlyGame || 
                                       sampleExperience.isMidGame || 
                                       sampleExperience.isEndGame
            assertTrue(hasPhaseClassification, "Experience should be classified by game phase")
            
            // Validate conversion to basic experience
            val basicExperience = sampleExperience.toBasicExperience()
            assertEquals(sampleExperience.state.size, basicExperience.state.size, "State sizes should match")
            assertEquals(sampleExperience.action, basicExperience.action, "Actions should match")
            assertEquals(sampleExperience.reward, basicExperience.reward, "Rewards should match")
            
            println("✅ Experience enhancement validated")
            println("  Game ID: ${sampleExperience.gameId}")
            println("  Move Number: ${sampleExperience.moveNumber}")
            println("  Quality Score: ${sampleExperience.qualityScore}")
            println("  Player Color: ${sampleExperience.playerColor}")
        }
        
        // Validate quality metrics
        val qualityMetrics = results.experienceQualityMetrics
        assertTrue(qualityMetrics.averageQualityScore >= 0.0 && qualityMetrics.averageQualityScore <= 1.0, 
                  "Average quality score should be between 0 and 1")
        
        val totalCategorized = qualityMetrics.highQualityExperiences + 
                              qualityMetrics.mediumQualityExperiences + 
                              qualityMetrics.lowQualityExperiences
        assertEquals(results.totalExperiences, totalCategorized, 
                    "All experiences should be categorized by quality")
        
        println("✅ Quality metrics validated:")
        println("  Average Quality: ${qualityMetrics.averageQualityScore}")
        println("  High Quality: ${qualityMetrics.highQualityExperiences}")
        println("  Medium Quality: ${qualityMetrics.mediumQualityExperiences}")
        println("  Low Quality: ${qualityMetrics.lowQualityExperiences}")
        
        println("\n✅ Self-Play Experience Collection - PASSED")
    }
    
    @Test
    fun testSelfPlayWithExistingTrainingIntegration() {
        println("🎯 Functional Test: Integration with Existing Training")
        println("=" * 50)
        
        // Create agent for both traditional and self-play training
        val agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(20, 10),
            learningRate = 0.015,
            explorationRate = 0.25
        )
        
        println("\n📊 Initial Agent State:")
        val initialMetrics = agent.getTrainingMetrics()
        println("  Episodes: ${initialMetrics.episodeCount}")
        println("  Buffer Size: ${initialMetrics.experienceBufferSize}")
        
        // Run traditional training first
        println("\n🔄 Phase 1: Traditional Training")
        val environment = ChessEnvironment()
        val pipelineConfig = TrainingPipelineConfig(
            maxStepsPerEpisode = 15,
            batchSize = 16,
            batchTrainingFrequency = 1,
            maxBufferSize = 200
        )
        
        val trainingPipeline = ChessTrainingPipeline(agent, environment, pipelineConfig)
        val pipelineResults = trainingPipeline.train(totalEpisodes = 2)
        
        assertEquals(2, pipelineResults.totalEpisodes, "Should complete traditional training")
        
        val afterTraditionalMetrics = agent.getTrainingMetrics()
        println("✅ Traditional training completed")
        println("  Episodes: ${afterTraditionalMetrics.episodeCount}")
        println("  Buffer Size: ${afterTraditionalMetrics.experienceBufferSize}")
        
        // Run self-play training
        println("\n🎮 Phase 2: Self-Play Training")
        val opponentAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(20, 10),
            learningRate = 0.015,
            explorationRate = 0.25
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 15,
                maxExperienceBufferSize = 200
            )
        )
        
        val selfPlayResults = selfPlaySystem.runSelfPlayGames(agent, opponentAgent, numGames = 2)
        
        assertEquals(2, selfPlayResults.totalGames, "Should complete self-play games")
        
        val finalMetrics = agent.getTrainingMetrics()
        println("✅ Self-play training completed")
        println("  Episodes: ${finalMetrics.episodeCount}")
        println("  Buffer Size: ${finalMetrics.experienceBufferSize}")
        
        // Validate integration worked
        assertTrue(finalMetrics.episodeCount >= initialMetrics.episodeCount, 
                  "Episode count should not decrease")
        
        // Validate experience compatibility
        if (selfPlayResults.experiences.isNotEmpty()) {
            val experience = selfPlayResults.experiences.first()
            val basicExperience = experience.toBasicExperience()
            
            assertEquals(ChessStateEncoder.TOTAL_FEATURES, basicExperience.state.size, 
                        "Experience should have correct state size")
            assertTrue(basicExperience.action >= 0, "Experience should have valid action")
            
            println("✅ Experience compatibility validated")
        }
        
        println("\n✅ Integration with Existing Training - PASSED")
    }
    
    @Test
    fun testSelfPlayMemoryEfficiency() {
        println("🎯 Functional Test: Memory Efficiency")
        println("=" * 50)
        
        // Test with constrained memory settings
        val config = SelfPlayConfig(
            maxConcurrentGames = 1,
            maxStepsPerGame = 15,
            maxExperienceBufferSize = 40, // Small buffer
            experienceCleanupStrategy = ExperienceCleanupStrategy.OLDEST_FIRST
        )
        
        val agent1 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(12, 6),
            learningRate = 0.02,
            explorationRate = 0.3
        )
        
        val agent2 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(12, 6),
            learningRate = 0.02,
            explorationRate = 0.3
        )
        
        val selfPlaySystem = SelfPlaySystem(config)
        
        println("\n💾 Testing memory management with buffer limit: ${config.maxExperienceBufferSize}")
        
        // Run multiple batches to test memory management
        var totalExperiencesGenerated = 0
        
        for (batch in 1..3) {
            println("\n  Batch $batch:")
            val results = selfPlaySystem.runSelfPlayGames(agent1, agent2, numGames = 2)
            
            totalExperiencesGenerated += results.totalExperiences
            
            println("    Games: ${results.totalGames}")
            println("    Experiences Generated: ${results.totalExperiences}")
            println("    Experiences in Buffer: ${results.experiences.size}")
            
            // Validate memory constraints
            assertTrue(results.experiences.size <= config.maxExperienceBufferSize, 
                      "Buffer should not exceed maximum size")
            
            if (totalExperiencesGenerated > config.maxExperienceBufferSize) {
                assertTrue(results.experiences.size < totalExperiencesGenerated, 
                          "Buffer should be smaller than total generated when cleanup occurs")
            }
        }
        
        println("\n✅ Memory management validated:")
        println("  Total Generated: $totalExperiencesGenerated")
        println("  Buffer Limit: ${config.maxExperienceBufferSize}")
        println("  Memory efficiently managed")
        
        println("\n✅ Memory Efficiency - PASSED")
    }
    
    @Test
    fun testSelfPlayAgentTypes() {
        println("🎯 Functional Test: Different Agent Types")
        println("=" * 50)
        
        val agentTypes = listOf(AgentType.DQN, AgentType.POLICY_GRADIENT)
        
        for (agentType in agentTypes) {
            println("\n🤖 Testing Agent Type: $agentType")
            
            val config = SelfPlayControllerConfig(
                agentType = agentType,
                hiddenLayers = listOf(16, 8),
                learningRate = 0.02,
                explorationRate = if (agentType == AgentType.DQN) 0.3 else 0.0,
                temperature = if (agentType == AgentType.POLICY_GRADIENT) 1.2 else 1.0,
                gamesPerIteration = 2,
                maxConcurrentGames = 1,
                maxStepsPerGame = 15,
                batchSize = 8
            )
            
            val controller = SelfPlayController(config)
            assertTrue(controller.initialize(), "Controller should initialize for $agentType")
            
            val results = controller.runSelfPlayTraining(iterations = 1)
            
            assertEquals(1, results.totalIterations, "Should complete 1 iteration for $agentType")
            
            val iteration = results.iterationHistory.first()
            assertTrue(iteration.selfPlayResults.totalGames > 0, "Should play games with $agentType")
            assertTrue(iteration.trainingMetrics.totalBatchUpdates >= 0, "Should have updates for $agentType")
            assertTrue(iteration.evaluationResults.gamesPlayed >= 0, "Should evaluate $agentType")
            
            println("  ✅ $agentType validated:")
            println("    Games: ${iteration.selfPlayResults.totalGames}")
            println("    Experiences: ${iteration.selfPlayResults.totalExperiences}")
            println("    Batch Updates: ${iteration.trainingMetrics.totalBatchUpdates}")
        }
        
        println("\n✅ Different Agent Types - PASSED")
    }
}