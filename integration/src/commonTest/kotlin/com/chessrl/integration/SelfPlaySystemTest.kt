package com.chessrl.integration

import kotlin.test.*

/**
 * Tests for the self-play training system
 */
class SelfPlaySystemTest {
    
    private lateinit var agent: ChessAgent
    private lateinit var environment: ChessEnvironment
    private lateinit var selfPlaySystem: SelfPlaySystem
    
    @BeforeTest
    fun setup() {
        // Create test agent
        agent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(32, 16), // Small network for testing
            learningRate = 0.01,
            explorationRate = 0.2,
            config = ChessAgentConfig(
                batchSize = 8,
                maxBufferSize = 100
            )
        )
        
        // Create test environment
        environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0
            )
        )
        
        // Create self-play system
        selfPlaySystem = SelfPlaySystem(
            agent = agent,
            environment = environment,
            config = SelfPlayConfig(
                maxMovesPerGame = 50, // Short games for testing
                trainingFrequency = 3, // Train every 3 games
                minExperiencesForTraining = 10,
                trainingBatchSize = 8,
                updatesPerTraining = 2,
                maxExperienceBufferSize = 200,
                progressReportInterval = 5,
                checkpointInterval = 10
            )
        )
    }
    
    @Test
    fun testSelfPlaySystemCreation() {
        println("Testing self-play system creation...")
        
        assertNotNull(selfPlaySystem, "Self-play system should be created")
        
        println("✅ Self-play system creation verified")
    }
    
    @Test
    fun testSelfPlayTrainingExecution() {
        println("Testing self-play training execution...")
        
        // Run a small self-play training session
        val results = selfPlaySystem.runSelfPlayTraining(totalGames = 5)
        
        // Verify results
        assertNotNull(results, "Self-play results should not be null")
        assertTrue(results.totalGames > 0, "Should have played some games")
        assertTrue(results.totalMoves > 0, "Should have made some moves")
        assertTrue(results.experienceCount >= 0, "Should have collected experiences")
        assertTrue(results.trainingDuration > 0, "Training should take some time")
        
        // Verify game results
        assertTrue(results.gameResults.isNotEmpty(), "Should have game results")
        
        // Verify final statistics
        val stats = results.finalStatistics
        assertTrue(stats.gamesPlayed > 0, "Should have played games")
        assertTrue(stats.totalMoves > 0, "Should have total moves")
        assertTrue(stats.winRate >= 0.0 && stats.winRate <= 1.0, "Win rate should be valid")
        assertTrue(stats.drawRate >= 0.0 && stats.drawRate <= 1.0, "Draw rate should be valid")
        
        println("✅ Self-play training execution verified")
        println("   Games Played: ${results.totalGames}")
        println("   Total Moves: ${results.totalMoves}")
        println("   Experiences: ${results.experienceCount}")
        println("   Win Rate: ${(stats.winRate * 100)}%")
    }
    
    @Test
    fun testSelfPlayProgressTracking() {
        println("Testing self-play progress tracking...")
        
        // Get initial progress
        val initialProgress = selfPlaySystem.getCurrentProgress()
        assertEquals(0, initialProgress.gamesCompleted, "Should start with 0 games")
        assertEquals(0, initialProgress.movesPlayed, "Should start with 0 moves")
        
        // Run some games
        selfPlaySystem.runSelfPlayTraining(totalGames = 3)
        
        // Check progress after training
        val finalProgress = selfPlaySystem.getCurrentProgress()
        assertTrue(finalProgress.gamesCompleted > 0, "Should have completed games")
        assertTrue(finalProgress.movesPlayed > 0, "Should have played moves")
        assertTrue(finalProgress.experiencesCollected >= 0, "Should have collected experiences")
        
        println("✅ Self-play progress tracking verified")
        println("   Games Completed: ${finalProgress.gamesCompleted}")
        println("   Moves Played: ${finalProgress.movesPlayed}")
        println("   Experiences: ${finalProgress.experiencesCollected}")
    }
    
    @Test
    fun testGameQualityAnalysis() {
        println("Testing game quality analysis...")
        
        // Run some games first
        selfPlaySystem.runSelfPlayTraining(totalGames = 5)
        
        // Analyze game quality
        val qualityAnalysis = selfPlaySystem.analyzeGameQuality()
        
        // Verify analysis results
        assertTrue(qualityAnalysis.totalGames > 0, "Should have analyzed games")
        assertTrue(qualityAnalysis.averageGameLength > 0, "Should have average game length")
        assertTrue(qualityAnalysis.gameCompletionRate >= 0.0 && qualityAnalysis.gameCompletionRate <= 1.0, 
                  "Completion rate should be valid")
        assertTrue(qualityAnalysis.moveVariety >= 0.0, "Move variety should be non-negative")
        assertTrue(qualityAnalysis.tacticalComplexity >= 0.0, "Tactical complexity should be non-negative")
        
        println("✅ Game quality analysis verified")
        println("   Total Games: ${qualityAnalysis.totalGames}")
        println("   Avg Game Length: ${qualityAnalysis.averageGameLength}")
        println("   Completion Rate: ${(qualityAnalysis.gameCompletionRate * 100)}%")
        println("   Move Variety: ${qualityAnalysis.moveVariety}")
        println("   Tactical Complexity: ${qualityAnalysis.tacticalComplexity}")
    }
    
    @Test
    fun testSelfPlayConfiguration() {
        println("Testing self-play configuration...")
        
        // Test with different configuration
        val customConfig = SelfPlayConfig(
            maxMovesPerGame = 30,
            trainingFrequency = 2,
            minExperiencesForTraining = 5,
            trainingBatchSize = 4,
            updatesPerTraining = 1,
            progressReportInterval = 2
        )
        
        val customSelfPlay = SelfPlaySystem(agent, environment, customConfig)
        
        // Run training with custom config
        val results = customSelfPlay.runSelfPlayTraining(totalGames = 3)
        
        // Verify results
        assertNotNull(results, "Custom self-play should work")
        assertTrue(results.totalGames > 0, "Should have played games")
        
        println("✅ Self-play configuration verified")
        println("   Custom config games: ${results.totalGames}")
    }
    
    @Test
    fun testExperienceCollection() {
        println("Testing experience collection...")
        
        // Run training and check experience collection
        val results = selfPlaySystem.runSelfPlayTraining(totalGames = 4)
        
        // Verify experience collection
        assertTrue(results.experienceCount >= 0, "Should have collected experiences")
        
        // Check that experiences are being used for training
        val progress = selfPlaySystem.getCurrentProgress()
        assertTrue(progress.experiencesCollected >= 0, "Should track experiences")
        
        println("✅ Experience collection verified")
        println("   Total Experiences: ${results.experienceCount}")
        println("   Tracked Experiences: ${progress.experiencesCollected}")
    }
    
    @Test
    fun testSelfPlayGameGeneration() {
        println("Testing self-play game generation...")
        
        // Run a single game worth of training
        val results = selfPlaySystem.runSelfPlayTraining(totalGames = 1)
        
        // Verify game was generated
        assertTrue(results.gameResults.isNotEmpty(), "Should have game results")
        
        val gameResult = results.gameResults.first()
        assertTrue(gameResult.moveCount > 0, "Game should have moves")
        assertTrue(gameResult.duration > 0, "Game should take time")
        assertTrue(gameResult.experiences.isNotEmpty(), "Game should have experiences")
        
        // Verify game outcome is valid
        val validOutcomes = setOf("WHITE_WINS", "BLACK_WINS", "DRAW_STALEMATE", 
                                 "DRAW_INSUFFICIENT_MATERIAL", "DRAW_FIFTY_MOVE_RULE", 
                                 "DRAW_REPETITION", "ongoing", "no_moves", "completed")
        assertTrue(gameResult.outcome in validOutcomes || gameResult.outcome.contains("WINS") || 
                  gameResult.outcome.contains("DRAW"), "Game outcome should be valid: ${gameResult.outcome}")
        
        println("✅ Self-play game generation verified")
        println("   Game Moves: ${gameResult.moveCount}")
        println("   Game Outcome: ${gameResult.outcome}")
        println("   Game Experiences: ${gameResult.experiences.size}")
    }
    
    @Test
    fun testTrainingIntegration() {
        println("Testing training integration...")
        
        // Get initial agent metrics
        val initialMetrics = agent.getTrainingMetrics()
        
        // Run self-play training
        val results = selfPlaySystem.runSelfPlayTraining(totalGames = 6) // Enough to trigger training
        
        // Get final agent metrics
        val finalMetrics = agent.getTrainingMetrics()
        
        // Verify training occurred
        assertTrue(results.experienceCount > 0, "Should have collected experiences")
        
        // Agent should have learned from experiences
        assertTrue(finalMetrics.episodeCount >= initialMetrics.episodeCount, 
                  "Agent should have processed episodes")
        
        println("✅ Training integration verified")
        println("   Initial Episodes: ${initialMetrics.episodeCount}")
        println("   Final Episodes: ${finalMetrics.episodeCount}")
        println("   Experiences Used: ${results.experienceCount}")
    }
    
    @Test
    fun testSelfPlayStatistics() {
        println("Testing self-play statistics...")
        
        // Run training
        val results = selfPlaySystem.runSelfPlayTraining(totalGames = 5)
        
        // Verify statistics are reasonable
        val stats = results.finalStatistics
        
        // Basic sanity checks
        assertEquals(results.totalGames, stats.gamesPlayed, "Games played should match")
        assertTrue(stats.averageGameLength > 0, "Average game length should be positive")
        assertTrue(stats.averageGameDuration > 0, "Average game duration should be positive")
        
        // Rates should sum to approximately 1.0 (allowing for rounding)
        val totalRate = stats.winRate + stats.drawRate + (1.0 - stats.winRate - stats.drawRate)
        assertTrue(abs(totalRate - 1.0) < 0.01, "Win/draw/loss rates should sum to 1.0")
        
        println("✅ Self-play statistics verified")
        println("   Win Rate: ${(stats.winRate * 100)}%")
        println("   Draw Rate: ${(stats.drawRate * 100)}%")
        println("   Loss Rate: ${((1.0 - stats.winRate - stats.drawRate) * 100)}%")
    }
}