package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Comprehensive unit tests for the SelfPlaySystem
 */
class SelfPlaySystemTest {
    
    @Test
    fun testSelfPlaySystemInitialization() {
        println("🧪 Testing SelfPlaySystem initialization...")
        
        val config = SelfPlayConfig(
            maxConcurrentGames = 2,
            maxStepsPerGame = 50,
            maxExperienceBufferSize = 1000
        )
        
        val selfPlaySystem = SelfPlaySystem(config)
        assertNotNull(selfPlaySystem, "SelfPlaySystem should be created successfully")
        
        val initialStats = selfPlaySystem.getCurrentStatistics()
        assertEquals(0, initialStats.totalGamesCompleted, "Initial games completed should be 0")
        assertEquals(0, initialStats.totalExperiencesCollected, "Initial experiences should be 0")
        assertEquals(0, initialStats.activeGames, "Initial active games should be 0")
        
        println("✅ SelfPlaySystem initialization test passed")
    }
    
    @Test
    fun testSingleSelfPlayGame() {
        println("🧪 Testing single self-play game execution...")
        
        // Create agents
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.3
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.3
        )
        
        val config = SelfPlayConfig(
            maxConcurrentGames = 1,
            maxStepsPerGame = 20, // Short game for testing
            progressReportInterval = 1
        )
        
        val selfPlaySystem = SelfPlaySystem(config)
        
        // Run single game
        val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 1)
        
        // Verify results
        assertEquals(1, results.totalGames, "Should complete exactly 1 game")
        assertTrue(results.totalExperiences > 0, "Should collect some experiences")
        assertTrue(results.totalDuration > 0, "Should take some time")
        assertEquals(1, results.gameResults.size, "Should have 1 game result")
        
        val gameResult = results.gameResults.first()
        assertTrue(gameResult.gameLength > 0, "Game should have some moves")
        assertTrue(gameResult.experiences.isNotEmpty(), "Game should have experiences")
        assertNotNull(gameResult.chessMetrics, "Game should have chess metrics")
        
        println("📊 Game Result:")
        println("  Game Length: ${gameResult.gameLength} moves")
        println("  Experiences: ${gameResult.experiences.size}")
        println("  Outcome: ${gameResult.gameOutcome}")
        println("  Termination: ${gameResult.terminationReason}")
        
        println("✅ Single self-play game test passed")
    }
    
    @Test
    fun testMultipleSelfPlayGames() {
        println("🧪 Testing multiple self-play games...")
        
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val config = SelfPlayConfig(
            maxConcurrentGames = 2,
            maxStepsPerGame = 15,
            progressReportInterval = 2
        )
        
        val selfPlaySystem = SelfPlaySystem(config)
        
        // Run multiple games
        val numGames = 3
        val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames)
        
        // Verify results
        assertEquals(numGames, results.totalGames, "Should complete all requested games")
        assertEquals(numGames, results.gameResults.size, "Should have result for each game")
        assertTrue(results.totalExperiences > 0, "Should collect experiences from all games")
        
        // Verify game outcomes are tracked
        val totalOutcomes = results.gameOutcomes.values.sum()
        assertEquals(numGames, totalOutcomes, "All games should be accounted for in outcomes")
        
        // Verify experience quality metrics
        val qualityMetrics = results.experienceQualityMetrics
        assertTrue(qualityMetrics.averageQualityScore >= 0.0, "Quality score should be non-negative")
        assertTrue(qualityMetrics.averageQualityScore <= 1.0, "Quality score should not exceed 1.0")
        
        val totalQualityExperiences = qualityMetrics.highQualityExperiences + 
                                     qualityMetrics.mediumQualityExperiences + 
                                     qualityMetrics.lowQualityExperiences
        assertEquals(results.totalExperiences, totalQualityExperiences, 
                    "All experiences should be categorized by quality")
        
        println("📊 Multiple Games Results:")
        println("  Total Games: ${results.totalGames}")
        println("  Total Experiences: ${results.totalExperiences}")
        println("  Average Game Length: ${results.averageGameLength}")
        println("  Game Outcomes: ${results.gameOutcomes}")
        println("  Quality Metrics: ${qualityMetrics}")
        
        println("✅ Multiple self-play games test passed")
    }
    
    @Test
    fun testExperienceEnhancement() {
        println("🧪 Testing experience enhancement and metadata...")
        
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 10,
                maxExperienceBufferSize = 500
            )
        )
        
        val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 1)
        
        // Verify enhanced experiences
        assertTrue(results.experiences.isNotEmpty(), "Should have enhanced experiences")
        
        val firstExperience = results.experiences.first()
        
        // Verify core experience data
        assertNotNull(firstExperience.state, "Experience should have state")
        assertTrue(firstExperience.action >= 0, "Experience should have valid action")
        assertNotNull(firstExperience.nextState, "Experience should have next state")
        
        // Verify enhanced metadata
        assertTrue(firstExperience.gameId > 0, "Experience should have game ID")
        assertTrue(firstExperience.moveNumber > 0, "Experience should have move number")
        assertNotNull(firstExperience.playerColor, "Experience should have player color")
        assertNotNull(firstExperience.gameOutcome, "Experience should have game outcome")
        assertNotNull(firstExperience.terminationReason, "Experience should have termination reason")
        
        // Verify quality metrics
        assertTrue(firstExperience.qualityScore >= 0.0, "Quality score should be non-negative")
        assertTrue(firstExperience.qualityScore <= 1.0, "Quality score should not exceed 1.0")
        
        // Verify game phase classification
        val hasEarlyGame = results.experiences.any { it.isEarlyGame }
        val hasMidGame = results.experiences.any { it.isMidGame }
        val hasEndGame = results.experiences.any { it.isEndGame }
        
        // At least one phase should be represented
        assertTrue(hasEarlyGame || hasMidGame || hasEndGame, "Should classify game phases")
        
        // Verify chess metrics
        assertNotNull(firstExperience.chessMetrics, "Experience should have chess metrics")
        
        // Test conversion to basic experience
        val basicExperience = firstExperience.toBasicExperience()
        assertEquals(firstExperience.state, basicExperience.state, "State should match")
        assertEquals(firstExperience.action, basicExperience.action, "Action should match")
        assertEquals(firstExperience.reward, basicExperience.reward, "Reward should match")
        assertEquals(firstExperience.nextState, basicExperience.nextState, "Next state should match")
        assertEquals(firstExperience.done, basicExperience.done, "Done flag should match")
        
        println("📊 Experience Enhancement Results:")
        println("  Total Enhanced Experiences: ${results.experiences.size}")
        println("  First Experience Quality: ${firstExperience.qualityScore}")
        println("  Game Phases - Early: $hasEarlyGame, Mid: $hasMidGame, End: $hasEndGame")
        println("  Player Colors: ${results.experiences.map { it.playerColor }.distinct()}")
        
        println("✅ Experience enhancement test passed")
    }
    
    @Test
    fun testExperienceBufferManagement() {
        println("🧪 Testing experience buffer management and cleanup...")
        
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        // Test with small buffer to trigger cleanup
        val config = SelfPlayConfig(
            maxConcurrentGames = 1,
            maxStepsPerGame = 20,
            maxExperienceBufferSize = 30, // Small buffer to test cleanup
            experienceCleanupStrategy = ExperienceCleanupStrategy.OLDEST_FIRST
        )
        
        val selfPlaySystem = SelfPlaySystem(config)
        
        // Run enough games to exceed buffer size
        val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 3)
        
        // Verify buffer size is managed
        assertTrue(results.experiences.size <= config.maxExperienceBufferSize, 
                  "Experience buffer should not exceed maximum size")
        
        // Verify we still collected experiences
        assertTrue(results.experiences.isNotEmpty(), "Should still have experiences after cleanup")
        
        // Test different cleanup strategies
        val strategies = listOf(
            ExperienceCleanupStrategy.OLDEST_FIRST,
            ExperienceCleanupStrategy.LOWEST_QUALITY,
            ExperienceCleanupStrategy.RANDOM
        )
        
        for (strategy in strategies) {
            val testConfig = config.copy(experienceCleanupStrategy = strategy)
            val testSystem = SelfPlaySystem(testConfig)
            
            val testResults = testSystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 2)
            
            assertTrue(testResults.experiences.size <= testConfig.maxExperienceBufferSize,
                      "Buffer should be managed with strategy: $strategy")
        }
        
        println("📊 Buffer Management Results:")
        println("  Final Buffer Size: ${results.experiences.size}")
        println("  Max Buffer Size: ${config.maxExperienceBufferSize}")
        println("  Total Experiences Generated: ${results.totalExperiences}")
        
        println("✅ Experience buffer management test passed")
    }
    
    @Test
    fun testConcurrentGameExecution() {
        println("🧪 Testing concurrent game execution simulation...")
        
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(16, 8),
            learningRate = 0.01,
            explorationRate = 0.2
        )
        
        // Test different concurrency levels
        val concurrencyLevels = listOf(1, 2, 4)
        
        for (concurrency in concurrencyLevels) {
            val config = SelfPlayConfig(
                maxConcurrentGames = concurrency,
                maxStepsPerGame = 15,
                progressReportInterval = 5
            )
            
            val selfPlaySystem = SelfPlaySystem(config)
            val startTime = getCurrentTimeMillis()
            
            val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 4)
            
            val endTime = getCurrentTimeMillis()
            val duration = endTime - startTime
            
            // Verify results
            assertEquals(4, results.totalGames, "Should complete all games with concurrency $concurrency")
            assertTrue(results.totalExperiences > 0, "Should collect experiences")
            
            println("📊 Concurrency $concurrency Results:")
            println("  Games: ${results.totalGames}")
            println("  Duration: ${duration}ms")
            println("  Experiences: ${results.totalExperiences}")
        }
        
        println("✅ Concurrent game execution test passed")
    }
    
    @Test
    fun testGameOutcomeTracking() {
        println("🧪 Testing game outcome tracking...")
        
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 25
            )
        )
        
        val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 3)
        
        // Verify outcome tracking
        assertNotNull(results.gameOutcomes, "Should track game outcomes")
        
        val totalTrackedGames = results.gameOutcomes.values.sum()
        assertEquals(results.totalGames, totalTrackedGames, "All games should be tracked")
        
        // Verify each game has an outcome
        for (gameResult in results.gameResults) {
            assertNotNull(gameResult.gameOutcome, "Each game should have an outcome")
            assertNotNull(gameResult.terminationReason, "Each game should have termination reason")
            
            // Verify outcome is valid
            assertTrue(
                gameResult.gameOutcome in listOf(
                    GameOutcome.WHITE_WINS, 
                    GameOutcome.BLACK_WINS, 
                    GameOutcome.DRAW, 
                    GameOutcome.ONGOING
                ),
                "Game outcome should be valid"
            )
        }
        
        // Verify statistics consistency
        val stats = selfPlaySystem.getCurrentStatistics()
        assertEquals(results.totalGames, stats.totalGamesCompleted, "Statistics should match results")
        assertEquals(results.totalExperiences, stats.totalExperiencesCollected, "Experience count should match")
        
        println("📊 Outcome Tracking Results:")
        println("  Game Outcomes: ${results.gameOutcomes}")
        println("  Total Games: ${results.totalGames}")
        println("  Average Game Length: ${results.averageGameLength}")
        
        println("✅ Game outcome tracking test passed")
    }
    
    @Test
    fun testSelfPlaySystemStop() {
        println("🧪 Testing self-play system stop functionality...")
        
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 50
            )
        )
        
        // Test stop functionality (in real implementation would test actual stopping)
        selfPlaySystem.stop()
        
        // Verify system can still be used after stop
        val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 1)
        assertEquals(1, results.totalGames, "System should work after stop")
        
        println("✅ Self-play system stop test passed")
    }
    
    @Test
    fun testExperienceQualityCalculation() {
        println("🧪 Testing experience quality calculation...")
        
        val whiteAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val blackAgent = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.01,
            explorationRate = 0.1
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 20
            )
        )
        
        val results = selfPlaySystem.runSelfPlayGames(whiteAgent, blackAgent, numGames = 2)
        
        // Verify quality metrics
        val qualityMetrics = results.experienceQualityMetrics
        
        assertTrue(qualityMetrics.averageQualityScore >= 0.0, "Average quality should be non-negative")
        assertTrue(qualityMetrics.averageQualityScore <= 1.0, "Average quality should not exceed 1.0")
        
        val totalCategorized = qualityMetrics.highQualityExperiences + 
                              qualityMetrics.mediumQualityExperiences + 
                              qualityMetrics.lowQualityExperiences
        assertEquals(results.totalExperiences, totalCategorized, "All experiences should be categorized")
        
        val totalByOutcome = qualityMetrics.experiencesFromWins + 
                            qualityMetrics.experiencesFromDraws + 
                            qualityMetrics.experiencesFromIncomplete
        assertEquals(results.totalExperiences, totalByOutcome, "All experiences should be categorized by outcome")
        
        // Verify individual experience quality scores
        for (experience in results.experiences) {
            assertTrue(experience.qualityScore >= 0.0, "Individual quality score should be non-negative")
            assertTrue(experience.qualityScore <= 1.0, "Individual quality score should not exceed 1.0")
        }
        
        println("📊 Quality Calculation Results:")
        println("  Average Quality Score: ${qualityMetrics.averageQualityScore}")
        println("  High Quality: ${qualityMetrics.highQualityExperiences}")
        println("  Medium Quality: ${qualityMetrics.mediumQualityExperiences}")
        println("  Low Quality: ${qualityMetrics.lowQualityExperiences}")
        println("  From Wins: ${qualityMetrics.experiencesFromWins}")
        println("  From Draws: ${qualityMetrics.experiencesFromDraws}")
        println("  From Incomplete: ${qualityMetrics.experiencesFromIncomplete}")
        
        println("✅ Experience quality calculation test passed")
    }
}