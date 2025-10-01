package com.chessrl.rl

import kotlin.random.Random
import kotlin.test.*

class RLFrameworkIntegrationTest {
    
    // Simple grid world environment for integration testing
    class GridWorldEnvironment(
        private val gridSize: Int = 4,
        private val goalPosition: Pair<Int, Int> = Pair(3, 3)
    ) : Environment<Pair<Int, Int>, String> {
        
        private var currentPosition = Pair(0, 0)
        private var stepCount = 0
        private val maxSteps = 20
        
        override fun reset(): Pair<Int, Int> {
            currentPosition = Pair(0, 0)
            stepCount = 0
            return currentPosition
        }
        
        override fun step(action: String): StepResult<Pair<Int, Int>> {
            stepCount++
            
            val newPosition = when (action) {
                "up" -> Pair(maxOf(0, currentPosition.first - 1), currentPosition.second)
                "down" -> Pair(minOf(gridSize - 1, currentPosition.first + 1), currentPosition.second)
                "left" -> Pair(currentPosition.first, maxOf(0, currentPosition.second - 1))
                "right" -> Pair(currentPosition.first, minOf(gridSize - 1, currentPosition.second + 1))
                else -> currentPosition
            }
            
            currentPosition = newPosition
            
            val reward = when {
                currentPosition == goalPosition -> 10.0
                stepCount >= maxSteps -> -1.0
                else -> -0.1  // Small negative reward for each step
            }
            
            val done = currentPosition == goalPosition || stepCount >= maxSteps
            
            return StepResult(
                nextState = currentPosition,
                reward = reward,
                done = done,
                info = mapOf("steps" to stepCount)
            )
        }
        
        override fun getValidActions(state: Pair<Int, Int>): List<String> {
            return listOf("up", "down", "left", "right")
        }
        
        override fun isTerminal(state: Pair<Int, Int>): Boolean {
            return state == goalPosition || stepCount >= maxSteps
        }
        
        override fun getStateSize(): Int = 2  // x, y coordinates
        
        override fun getActionSize(): Int = 4  // up, down, left, right
    }
    
    // Simple Q-learning agent for integration testing
    class SimpleQLearningAgent(
        private val learningRate: Double = 0.1,
        private val discountFactor: Double = 0.9
    ) : Agent<Pair<Int, Int>, String> {
        
        private val qTable = mutableMapOf<Pair<Pair<Int, Int>, String>, Double>()
        private val explorationStrategy = EpsilonGreedyStrategy<String>(epsilon = 0.1)
        
        override fun selectAction(state: Pair<Int, Int>, validActions: List<String>): String {
            val actionValues = validActions.associateWith { action ->
                qTable[Pair(state, action)] ?: 0.0
            }
            return explorationStrategy.selectAction(validActions, actionValues)
        }
        
        override fun learn(experience: Experience<Pair<Int, Int>, String>) {
            val currentQ = qTable[Pair(experience.state, experience.action)] ?: 0.0
            
            val maxNextQ = if (experience.done) {
                0.0
            } else {
                val nextActions = listOf("up", "down", "left", "right")
                nextActions.maxOfOrNull { action ->
                    qTable[Pair(experience.nextState, action)] ?: 0.0
                } ?: 0.0
            }
            
            val targetQ = experience.reward + discountFactor * maxNextQ
            val newQ = currentQ + learningRate * (targetQ - currentQ)
            
            qTable[Pair(experience.state, experience.action)] = newQ
        }
        
        override fun save(path: String) {
            // Mock implementation
        }
        
        override fun load(path: String) {
            // Mock implementation
        }
        
        fun getQValue(state: Pair<Int, Int>, action: String): Double {
            return qTable[Pair(state, action)] ?: 0.0
        }
        
        fun updateExploration(episode: Int) {
            explorationStrategy.updateExploration(episode)
        }
    }
    
    @Test
    fun testCompleteRLPipeline() {
        val env = GridWorldEnvironment()
        val agent = SimpleQLearningAgent()
        val experienceBuffer = CircularExperienceBuffer<Pair<Int, Int>, String>(maxSize = 1000)
        
        var totalReward = 0.0
        var episodeCount = 0
        val maxEpisodes = 50
        
        // Run multiple episodes
        repeat(maxEpisodes) { episode ->
            var state = env.reset()
            var episodeReward = 0.0
            var stepCount = 0
            val maxStepsPerEpisode = 25
            
            while (!env.isTerminal(state) && stepCount < maxStepsPerEpisode) {
                val validActions = env.getValidActions(state)
                val action = agent.selectAction(state, validActions)
                val stepResult = env.step(action)
                
                val experience = Experience(
                    state = state,
                    action = action,
                    reward = stepResult.reward,
                    nextState = stepResult.nextState,
                    done = stepResult.done
                )
                
                agent.learn(experience)
                experienceBuffer.add(experience)
                
                state = stepResult.nextState
                episodeReward += stepResult.reward
                stepCount++
                
                if (stepResult.done) break
            }
            
            agent.updateExploration(episode)
            totalReward += episodeReward
            episodeCount++
        }
        
        // Verify that learning occurred
        assertTrue(episodeCount == maxEpisodes, "Should have completed all episodes")
        assertTrue(experienceBuffer.size() > 0, "Should have collected experiences")
        
        // Test that the agent learned something (Q-values should be non-zero for some state-action pairs)
        val startState = Pair(0, 0)
        val qValues = listOf("up", "down", "left", "right").map { action ->
            agent.getQValue(startState, action)
        }
        
        val hasLearnedValues = qValues.any { it != 0.0 }
        assertTrue(hasLearnedValues, "Agent should have learned some Q-values")
        
        // Test experience buffer functionality
        val sampleSize = minOf(10, experienceBuffer.size())
        val sample = experienceBuffer.sample(sampleSize)
        assertEquals(sampleSize, sample.size, "Should sample correct number of experiences")
        
        println("Integration test completed successfully!")
        println("Total episodes: $episodeCount")
        println("Total experiences collected: ${experienceBuffer.size()}")
        println("Average reward per episode: ${totalReward / episodeCount}")
    }
    
    @Test
    fun testExplorationStrategiesIntegration() {
        val validActions = listOf("action1", "action2", "action3")
        val actionValues = mapOf(
            "action1" to 1.0,
            "action2" to 5.0,  // Best action
            "action3" to 2.0
        )
        
        // Test epsilon-greedy strategy
        val epsilonGreedy = EpsilonGreedyStrategy<String>(epsilon = 0.3)
        val selectedActions = mutableListOf<String>()
        
        repeat(100) {
            selectedActions.add(epsilonGreedy.selectAction(validActions, actionValues))
        }
        
        // Should have selected all actions due to exploration
        assertTrue(selectedActions.contains("action1"))
        assertTrue(selectedActions.contains("action2"))
        assertTrue(selectedActions.contains("action3"))
        
        // Best action should be selected most frequently
        val bestActionCount = selectedActions.count { it == "action2" }
        assertTrue(bestActionCount > 30, "Best action should be selected frequently")
        
        // Test Boltzmann strategy
        // Use a moderate temperature to ensure probabilistic exploration
        val boltzmann = BoltzmannStrategy<String>(temperature = 2.0)
        val boltzmannActions = mutableListOf<String>()
        
        repeat(100) {
            boltzmannActions.add(boltzmann.selectAction(validActions, actionValues))
        }
        
        // Should have selected multiple actions
        val uniqueBoltzmannActions = boltzmannActions.toSet()
        assertTrue(uniqueBoltzmannActions.size > 1, "Boltzmann should explore multiple actions")
    }
    
    @Test
    fun testPrioritizedExperienceBufferIntegration() {
        val buffer = PrioritizedExperienceBuffer<String, String>(maxSize = 10)
        
        // Add experiences
        repeat(15) { i ->
            val experience = Experience(
                state = "state$i",
                action = "action$i",
                reward = i.toDouble(),
                nextState = "nextState$i",
                done = i % 5 == 0
            )
            buffer.add(experience)
        }
        
        assertEquals(10, buffer.size(), "Buffer should be at capacity")
        assertTrue(buffer.isFull(), "Buffer should be full")
        
        // Test sampling
        val sample = buffer.sample(5)
        assertEquals(5, sample.size, "Should sample correct number of experiences")
        
        // Test priority updates
        val indices = listOf(0, 1, 2)
        val tdErrors = listOf(0.5, 1.0, 0.2)
        buffer.updatePriorities(indices, tdErrors)
        
        // Test importance sampling weights
        val weights = buffer.getImportanceSamplingWeights(indices)
        assertEquals(3, weights.size, "Should return correct number of weights")
        weights.forEach { weight ->
            assertTrue(weight > 0.0, "Weights should be positive")
        }
    }
}
