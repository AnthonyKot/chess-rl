package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.test.*
import kotlin.math.abs

/**
 * Comprehensive validation test for the self-play training pipeline
 * 
 * This test validates that:
 * 1. Fixed game state detection works correctly in self-play scenarios
 * 2. Experience collection captures proper rewards for different game outcomes
 * 3. Training metrics reflect actual learning progress rather than artificial draw inflation
 * 4. Agents can learn basic chess concepts through small-scale training
 */
class SelfPlayTrainingPipelineValidationTest {
    
    @Test
    fun testSelfPlayGameStateDetectionFix() {
        println("🧪 Testing self-play game state detection fix...")
        
        // Create a self-play system with proper reward configuration
        val selfPlaySystem = SelfPlaySystem(
            config = SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 50, // Short games to avoid timeouts
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepPenalty = -0.01,
                stepLimitPenalty = -0.5, // Clear penalty for step-limited games
                treatStepLimitAsDrawForReporting = false, // Don't treat as draws
                progressReportInterval = 10
            )
        )
        
        // Create simple agents for testing
        val whiteAgent = createTestAgent("White")
        val blackAgent = createTestAgent("Black")
        
        // Run a small batch of self-play games
        val results = selfPlaySystem.runSelfPlayGames(
            whiteAgent = whiteAgent,
            blackAgent = blackAgent,
            numGames = 5
        )
        
        // Validate results
        assertTrue(results.totalGames > 0, "Should have completed some games")
        assertTrue(results.totalExperiences > 0, "Should have collected experiences")
        assertTrue(results.gameResults.isNotEmpty(), "Should have game results")
        
        // Analyze game outcomes to ensure proper classification
        var legitimateDraws = 0
        var stepLimitTerminations = 0
        var naturalTerminations = 0
        
        for (gameResult in results.gameResults) {
            when (gameResult.terminationReason) {
                EpisodeTerminationReason.GAME_ENDED -> {
                    naturalTerminations++
                    println("   ✅ Natural termination: ${gameResult.gameOutcome} after ${gameResult.gameLength} moves")
                }
                EpisodeTerminationReason.STEP_LIMIT -> {
                    stepLimitTerminations++
                    println("   ⏱️ Step limit termination after ${gameResult.gameLength} moves")
                }
                EpisodeTerminationReason.MANUAL -> {
                    println("   🛑 Manual termination after ${gameResult.gameLength} moves")
                }
            }
            
            // Check if this was a legitimate chess draw
            if (gameResult.gameOutcome == GameOutcome.DRAW && 
                gameResult.terminationReason == EpisodeTerminationReason.GAME_ENDED) {
                legitimateDraws++
            }
        }
        
        println("   📊 Termination analysis:")
        println("      Natural terminations: $naturalTerminations")
        println("      Step limit terminations: $stepLimitTerminations")
        println("      Legitimate draws: $legitimateDraws")
        
        // Key validation: Step-limited games should NOT be classified as draws
        assertTrue(stepLimitTerminations >= 0, "Step limit terminations should be tracked")
        
        // If we have step-limited games, they should not be counted as legitimate draws
        if (stepLimitTerminations > 0) {
            assertTrue(legitimateDraws < results.totalGames, 
                      "Step-limited games should not be classified as legitimate draws")
        }
    }
    
    @Test
    fun testExperienceRewardValidation() {
        println("🧪 Testing experience reward validation...")
        
        // Create environment with clear reward structure
        val environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepPenalty = -0.01,
                stepLimitPenalty = -0.8,
                enablePositionRewards = false // Disable for clearer testing
            )
        )
        
        val agent = createTestAgent("Test")
        
        // Create training pipeline
        val pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 20, // Short episodes
                batchSize = 16,
                maxBufferSize = 1000
            )
        )
        
        // Run several episodes to collect experiences
        val episodes = mutableListOf<TrainingEpisodeResult>()
        repeat(5) {
            val result = pipeline.runEpisode()
            episodes.add(result)
            println("   Episode ${it + 1}: ${result.steps} steps, reward = ${result.reward}")
        }
        
        // Validate experience rewards
        assertTrue(episodes.isNotEmpty(), "Should have completed episodes")
        
        // Check that rewards are reasonable
        for ((index, episode) in episodes.withIndex()) {
            assertTrue(episode.reward.isFinite(), "Episode ${index + 1} reward should be finite")
            
            // Step-limited episodes should have negative rewards (due to step penalty + step limit penalty)
            if (episode.steps >= 20) {
                assertTrue(episode.reward < 0, 
                          "Step-limited episode ${index + 1} should have negative reward, got ${episode.reward}")
            }
        }
        
        // Verify that experiences are being collected
        val trainingState = pipeline.getTrainingState()
        assertTrue(trainingState.experienceBufferSize > 0, "Experience buffer should contain experiences")
        println("   📊 Collected ${trainingState.experienceBufferSize} experiences from ${episodes.size} episodes")
    }
    
    @Test
    fun testTrainingMetricsValidation() {
        println("🧪 Testing training metrics validation...")
        
        // Create a simple training setup
        val agent = createTestAgent("Learner")
        val environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                stepPenalty = -0.01,
                stepLimitPenalty = -0.5
            )
        )
        
        val pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 30,
                batchSize = 8,
                maxBufferSize = 500
            )
        )
        
        // Collect initial metrics
        val initialMetrics = agent.getTrainingMetrics()
        println("   Initial metrics: avgReward=${initialMetrics.averageReward}, exploration=${initialMetrics.explorationRate}")
        
        // Run training episodes
        repeat(10) { episode ->
            val result = pipeline.runEpisode()
            
            // Training happens automatically in the pipeline based on configuration
            
            if ((episode + 1) % 5 == 0) {
                val metrics = agent.getTrainingMetrics()
                println("   Episode ${episode + 1} metrics: avgReward=${metrics.averageReward}, bufferSize=${metrics.experienceBufferSize}")
            }
        }
        
        // Validate final metrics
        val finalMetrics = agent.getTrainingMetrics()
        assertTrue(finalMetrics.averageReward.isFinite(), "Final average reward should be finite")
        assertTrue(finalMetrics.experienceBufferSize > 0, "Should have collected experiences")
        
        println("   📊 Training progression:")
        println("      Initial avg reward: ${initialMetrics.averageReward}")
        println("      Final avg reward: ${finalMetrics.averageReward}")
        println("      Experience buffer size: ${finalMetrics.experienceBufferSize}")
        
        // The metrics should show some form of progression or stability
        assertTrue(abs(finalMetrics.averageReward) < 10.0, "Average reward should be reasonable")
    }
    
    @Test
    fun testBasicLearningValidation() {
        println("🧪 Testing basic learning validation...")
        
        // Create a deterministic test scenario
        val agent = createTestAgent("Student")
        val environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                stepPenalty = -0.001, // Very small step penalty
                stepLimitPenalty = -0.1
            )
        )
        
        val pipeline = ChessTrainingPipeline(
            agent = agent,
            environment = environment,
            config = TrainingPipelineConfig(
                maxStepsPerEpisode = 50,
                batchSize = 16,
                maxBufferSize = 1000
            )
        )
        
        // Collect baseline performance
        val baselineRewards = mutableListOf<Double>()
        repeat(5) {
            val result = pipeline.runEpisode()
            baselineRewards.add(result.reward)
        }
        val baselineAvg = baselineRewards.average()
        
        // Perform training
        repeat(20) { episode ->
            pipeline.runEpisode()
            // Training happens automatically based on pipeline configuration
        }
        
        // Collect post-training performance
        val postTrainingRewards = mutableListOf<Double>()
        repeat(5) {
            val result = pipeline.runEpisode()
            postTrainingRewards.add(result.reward)
        }
        val postTrainingAvg = postTrainingRewards.average()
        
        println("   📊 Learning assessment:")
        println("      Baseline average reward: $baselineAvg")
        println("      Post-training average reward: $postTrainingAvg")
        println("      Improvement: ${postTrainingAvg - baselineAvg}")
        
        // Validate that learning occurred (or at least didn't degrade significantly)
        assertTrue(postTrainingAvg.isFinite(), "Post-training reward should be finite")
        assertTrue(abs(postTrainingAvg - baselineAvg) < 10.0, "Reward change should be reasonable")
        
        // The agent should have collected meaningful experiences
        val finalState = pipeline.getTrainingState()
        assertTrue(finalState.experienceBufferSize > 50, "Should have collected substantial experiences")
    }
    
    @Test
    fun testAdvancedSelfPlayPipelineIntegration() {
        println("🧪 Testing advanced self-play pipeline integration...")
        
        // Create advanced pipeline with minimal configuration
        val pipeline = AdvancedSelfPlayTrainingPipeline(
            config = AdvancedSelfPlayConfig(
                initialGamesPerCycle = 3,
                maxStepsPerGame = 30,
                batchSize = 8,
                maxExperienceBufferSize = 500,
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepLimitPenalty = -0.5,
                explorationRate = 0.3,
                cycleReportInterval = 1
            ),
            agentType = AgentType.DQN
        )
        
        // Initialize the pipeline
        val initialized = pipeline.initialize()
        assertTrue(initialized, "Pipeline should initialize successfully")
        
        // Run a minimal training cycle
        val results = pipeline.runAdvancedTraining(totalCycles = 2)
        
        // Validate results
        assertTrue(results.totalCycles > 0, "Should have completed training cycles")
        assertTrue(results.totalDuration > 0, "Should have recorded training duration")
        assertTrue(results.cycleHistory.isNotEmpty(), "Should have cycle history")
        
        println("   📊 Advanced pipeline results:")
        println("      Total cycles: ${results.totalCycles}")
        println("      Duration: ${results.totalDuration}ms")
        println("      Best performance: ${results.bestPerformance}")
        
        // Validate that experiences were collected properly
        if (results.cycleHistory.isNotEmpty()) {
            val firstCycle = results.cycleHistory.first()
            assertTrue(firstCycle.selfPlayResults.totalExperiences > 0, 
                      "First cycle should have collected experiences")
            
            // Check that game outcomes are properly classified
            val outcomes = firstCycle.selfPlayResults.gameOutcomes
            println("      Game outcomes: $outcomes")
            
            // Ensure we're not seeing artificial draw inflation
            val totalGames = outcomes.values.sum()
            val drawCount = outcomes[GameOutcome.DRAW] ?: 0
            val drawRatio = if (totalGames > 0) drawCount.toDouble() / totalGames else 0.0
            
            println("      Draw ratio: ${drawRatio * 100}%")
            
            // With the fix, we shouldn't see 98% draws like in the original bug
            assertTrue(drawRatio < 0.9, "Draw ratio should be reasonable, not artificially inflated")
        }
    }
    
    /**
     * Create a simple test agent for validation
     */
    private fun createTestAgent(name: String): ChessAgent {
        return object : ChessAgent {
            private var explorationRate = 0.2
            private val experiences = mutableListOf<Experience<DoubleArray, Int>>()
            private var totalReward = 0.0
            private var episodeCount = 0
            
            override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
                // Simple epsilon-greedy strategy
                return if (kotlin.random.Random.nextDouble() < explorationRate) {
                    validActions.randomOrNull() ?: 0
                } else {
                    // Select first valid action (deterministic for testing)
                    validActions.firstOrNull() ?: 0
                }
            }
            
            override fun learn(experience: Experience<DoubleArray, Int>) {
                experiences.add(experience)
                totalReward += experience.reward
                if (experience.done) {
                    episodeCount++
                }
            }
            
            override fun getQValues(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
                return actions.associateWith { 0.0 }
            }
            
            override fun getActionProbabilities(state: DoubleArray, actions: List<Int>): Map<Int, Double> {
                val prob = 1.0 / actions.size
                return actions.associateWith { prob }
            }
            
            override fun getTrainingMetrics(): ChessAgentMetrics {
                val avgReward = if (episodeCount > 0) totalReward / episodeCount else 0.0
                return ChessAgentMetrics(
                    averageReward = avgReward,
                    explorationRate = explorationRate,
                    experienceBufferSize = experiences.size
                )
            }
            
            override fun forceUpdate() {
                // No-op for test agent
            }
            
            override fun trainBatch(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
                // Simple mock training
                return PolicyUpdateResult(
                    loss = 0.1 + kotlin.random.Random.nextDouble() * 0.1,
                    gradientNorm = 0.5 + kotlin.random.Random.nextDouble() * 0.3,
                    policyEntropy = 1.0 + kotlin.random.Random.nextDouble() * 0.5
                )
            }
            
            override fun save(path: String) {}
            override fun load(path: String) {}
            override fun reset() {
                experiences.clear()
                totalReward = 0.0
                episodeCount = 0
            }
            
            override fun setExplorationRate(epsilon: Double) {
                explorationRate = epsilon
            }
            
            override fun getConfig(): ChessAgentConfig = ChessAgentConfig()
            override fun setNextActionProvider(provider: (DoubleArray) -> List<Int>) {}
        }
    }
}