package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.test.*

/**
 * Basic functionality tests for self-play system
 */
class SelfPlayBasicTest {
    
    @Test
    fun testSelfPlaySystemCreation() {
        println("🧪 Testing basic SelfPlaySystem creation...")
        
        val config = SelfPlayConfig(
            maxConcurrentGames = 1,
            maxStepsPerGame = 10
        )
        
        val selfPlaySystem = SelfPlaySystem(config)
        assertNotNull(selfPlaySystem, "SelfPlaySystem should be created")
        
        val stats = selfPlaySystem.getCurrentStatistics()
        assertEquals(0, stats.totalGamesCompleted, "Initial games should be 0")
        assertEquals(0, stats.totalExperiencesCollected, "Initial experiences should be 0")
        
        println("✅ SelfPlaySystem creation test passed")
    }
    
    @Test
    fun testSelfPlayControllerCreation() {
        println("🧪 Testing basic SelfPlayController creation...")
        
        val config = SelfPlayControllerConfig(
            gamesPerIteration = 1,
            maxStepsPerGame = 5
        )
        
        val controller = SelfPlayController(config)
        assertNotNull(controller, "SelfPlayController should be created")
        
        val initResult = controller.initialize()
        assertTrue(initResult, "Controller should initialize successfully")
        
        val status = controller.getTrainingStatus()
        assertFalse(status.isTraining, "Should not be training initially")
        assertEquals(0, status.currentIteration, "Initial iteration should be 0")
        
        println("✅ SelfPlayController creation test passed")
    }
    
    @Test
    fun testBasicSelfPlayExecution() {
        println("🧪 Testing basic self-play execution...")
        
        // Create simple agents
        val agent1 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.1,
            explorationRate = 0.5
        )
        
        val agent2 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(8, 4),
            learningRate = 0.1,
            explorationRate = 0.5
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 5, // Very short games
                progressReportInterval = 1
            )
        )
        
        // Run one very short game
        val results = selfPlaySystem.runSelfPlayGames(agent1, agent2, numGames = 1)
        
        // Basic verification
        assertEquals(1, results.totalGames, "Should complete 1 game")
        assertTrue(results.totalExperiences >= 0, "Should have non-negative experiences")
        assertEquals(1, results.gameResults.size, "Should have 1 game result")
        
        val gameResult = results.gameResults.first()
        assertTrue(gameResult.gameLength >= 0, "Game length should be non-negative")
        assertNotNull(gameResult.gameOutcome, "Game should have outcome")
        assertNotNull(gameResult.terminationReason, "Game should have termination reason")
        
        println("📊 Basic Execution Results:")
        println("  Games: ${results.totalGames}")
        println("  Experiences: ${results.totalExperiences}")
        println("  Game Length: ${gameResult.gameLength}")
        println("  Outcome: ${gameResult.gameOutcome}")
        println("  Termination: ${gameResult.terminationReason}")
        
        println("✅ Basic self-play execution test passed")
    }
    
    @Test
    fun testExperienceEnhancementBasic() {
        println("🧪 Testing basic experience enhancement...")
        
        val agent1 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(4, 2),
            learningRate = 0.1,
            explorationRate = 0.5
        )
        
        val agent2 = ChessAgentFactory.createDQNAgent(
            hiddenLayers = listOf(4, 2),
            learningRate = 0.1,
            explorationRate = 0.5
        )
        
        val selfPlaySystem = SelfPlaySystem(
            SelfPlayConfig(
                maxConcurrentGames = 1,
                maxStepsPerGame = 3
            )
        )
        
        val results = selfPlaySystem.runSelfPlayGames(agent1, agent2, numGames = 1)
        
        if (results.totalExperiences > 0) {
            val experience = results.experiences.first()
            
            // Verify enhanced metadata exists
            assertTrue(experience.gameId > 0, "Experience should have game ID")
            assertTrue(experience.moveNumber > 0, "Experience should have move number")
            assertNotNull(experience.playerColor, "Experience should have player color")
            assertNotNull(experience.gameOutcome, "Experience should have game outcome")
            assertNotNull(experience.terminationReason, "Experience should have termination reason")
            
            // Verify quality score is in valid range
            assertTrue(experience.qualityScore >= 0.0, "Quality score should be non-negative")
            assertTrue(experience.qualityScore <= 1.0, "Quality score should not exceed 1.0")
            
            // Verify conversion works
            val basicExperience = experience.toBasicExperience()
            assertEquals(experience.state.size, basicExperience.state.size, "State sizes should match")
            assertEquals(experience.action, basicExperience.action, "Actions should match")
            assertEquals(experience.reward, basicExperience.reward, "Rewards should match")
            
            println("📊 Experience Enhancement Results:")
            println("  Game ID: ${experience.gameId}")
            println("  Move Number: ${experience.moveNumber}")
            println("  Player Color: ${experience.playerColor}")
            println("  Quality Score: ${experience.qualityScore}")
        }
        
        println("✅ Basic experience enhancement test passed")
    }
    
    @Test
    fun testSelfPlayControllerBasicTraining() {
        println("🧪 Testing basic self-play controller training...")
        
        val config = SelfPlayControllerConfig(
            agentType = AgentType.DQN,
            hiddenLayers = listOf(4, 2),
            learningRate = 0.1,
            gamesPerIteration = 1,
            maxConcurrentGames = 1,
            maxStepsPerGame = 3,
            batchSize = 4
        )
        
        val controller = SelfPlayController(config)
        assertTrue(controller.initialize(), "Controller should initialize")
        
        // Run minimal training
        val results = controller.runSelfPlayTraining(iterations = 1)
        
        // Basic verification
        assertEquals(1, results.totalIterations, "Should complete 1 iteration")
        assertEquals(1, results.iterationHistory.size, "Should have 1 iteration result")
        
        val iterationResult = results.iterationHistory.first()
        assertEquals(1, iterationResult.iteration, "Iteration number should be 1")
        
        // Verify components exist
        assertNotNull(iterationResult.selfPlayResults, "Should have self-play results")
        assertNotNull(iterationResult.trainingMetrics, "Should have training metrics")
        assertNotNull(iterationResult.evaluationResults, "Should have evaluation results")
        
        // Verify self-play results
        assertTrue(iterationResult.selfPlayResults.totalGames >= 0, "Should have non-negative games")
        assertTrue(iterationResult.selfPlayResults.totalExperiences >= 0, "Should have non-negative experiences")
        
        // Verify training metrics
        assertTrue(iterationResult.trainingMetrics.totalBatchUpdates >= 0, "Should have non-negative batch updates")
        assertTrue(iterationResult.trainingMetrics.experienceBufferSize >= 0, "Should have non-negative buffer size")
        
        // Verify evaluation results
        assertTrue(iterationResult.evaluationResults.gamesPlayed >= 0, "Should have non-negative evaluation games")
        
        println("📊 Controller Training Results:")
        println("  Iterations: ${results.totalIterations}")
        println("  Self-Play Games: ${iterationResult.selfPlayResults.totalGames}")
        println("  Experiences: ${iterationResult.selfPlayResults.totalExperiences}")
        println("  Batch Updates: ${iterationResult.trainingMetrics.totalBatchUpdates}")
        println("  Evaluation Games: ${iterationResult.evaluationResults.gamesPlayed}")
        
        println("✅ Basic self-play controller training test passed")
    }
}