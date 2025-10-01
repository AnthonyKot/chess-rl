package com.chessrl.rl

import kotlin.test.*
import kotlin.random.Random
import kotlin.math.*

/**
 * Simple GridWorld environment for testing RL algorithms
 */
class GridWorldEnvironment(
    private val width: Int = 4,
    private val height: Int = 4,
    private val goalX: Int = 3,
    private val goalY: Int = 3,
    private val random: Random = Random.Default
) : Environment<DoubleArray, Int> {
    
    private var agentX = 0
    private var agentY = 0
    private var stepCount = 0
    private val maxSteps = 50
    
    // Actions: 0=up, 1=right, 2=down, 3=left
    override fun reset(): DoubleArray {
        agentX = 0
        agentY = 0
        stepCount = 0
        return getState()
    }
    
    override fun step(action: Int): StepResult<DoubleArray> {
        stepCount++
        
        // Move agent based on action
        when (action) {
            0 -> agentY = maxOf(0, agentY - 1) // up
            1 -> agentX = minOf(width - 1, agentX + 1) // right
            2 -> agentY = minOf(height - 1, agentY + 1) // down
            3 -> agentX = maxOf(0, agentX - 1) // left
        }
        
        // Calculate reward
        val reward = when {
            agentX == goalX && agentY == goalY -> 10.0 // Goal reached
            stepCount >= maxSteps -> -5.0 // Timeout penalty
            else -> -0.1 // Small step penalty
        }
        
        val done = (agentX == goalX && agentY == goalY) || stepCount >= maxSteps
        
        return StepResult(
            nextState = getState(),
            reward = reward,
            done = done
        )
    }
    
    override fun getValidActions(state: DoubleArray): List<Int> {
        return listOf(0, 1, 2, 3) // All actions always valid
    }
    
    override fun isTerminal(state: DoubleArray): Boolean {
        return (agentX == goalX && agentY == goalY) || stepCount >= maxSteps
    }
    
    override fun getStateSize(): Int = 4 // [agentX, agentY, goalX, goalY]
    
    override fun getActionSize(): Int = 4
    
    private fun getState(): DoubleArray {
        return doubleArrayOf(
            agentX.toDouble() / width,
            agentY.toDouble() / height,
            goalX.toDouble() / width,
            goalY.toDouble() / height
        )
    }
    
    fun getAgentPosition(): Pair<Int, Int> = Pair(agentX, agentY)
    fun getStepCount(): Int = stepCount
}

/**
 * Simple Bandit environment for testing policy gradient algorithms
 */
class MultiarmedBanditEnvironment(
    private val numArms: Int = 3,
    private val random: Random = Random.Default
) : Environment<DoubleArray, Int> {
    
    // True reward probabilities for each arm (unknown to agent)
    private val armRewards = DoubleArray(numArms) { random.nextDouble() }
    private var stepCount = 0
    private val maxSteps = 100
    
    override fun reset(): DoubleArray {
        stepCount = 0
        return getState()
    }
    
    override fun step(action: Int): StepResult<DoubleArray> {
        require(action in 0 until numArms) { "Invalid action: $action" }
        
        stepCount++
        
        // Reward is 1 with probability armRewards[action], 0 otherwise
        val reward = if (random.nextDouble() < armRewards[action]) 1.0 else 0.0
        val done = stepCount >= maxSteps
        
        return StepResult(
            nextState = getState(),
            reward = reward,
            done = done
        )
    }
    
    override fun getValidActions(state: DoubleArray): List<Int> {
        return (0 until numArms).toList()
    }
    
    override fun isTerminal(state: DoubleArray): Boolean {
        return stepCount >= maxSteps
    }
    
    override fun getStateSize(): Int = 1 // Just step count normalized
    
    override fun getActionSize(): Int = numArms
    
    private fun getState(): DoubleArray {
        return doubleArrayOf(stepCount.toDouble() / maxSteps)
    }
    
    fun getArmRewards(): DoubleArray = armRewards.copyOf()
    fun getBestArm(): Int = armRewards.indices.maxByOrNull { armRewards[it] } ?: 0
}

class ToyEnvironmentTest {
    
    @Test
    fun testGridWorldEnvironmentBasics() {
        val env = GridWorldEnvironment(4, 4, 3, 3)
        
        assertEquals(4, env.getStateSize())
        assertEquals(4, env.getActionSize())
        
        val initialState = env.reset()
        assertEquals(4, initialState.size)
        assertEquals(0.0, initialState[0]) // Agent starts at (0,0)
        assertEquals(0.0, initialState[1])
        assertEquals(0.75, initialState[2]) // Goal at (3,3) normalized
        assertEquals(0.75, initialState[3])
        
        assertFalse(env.isTerminal(initialState))
        assertEquals(Pair(0, 0), env.getAgentPosition())
    }
    
    @Test
    fun testGridWorldMovement() {
        val env = GridWorldEnvironment(4, 4, 3, 3)
        env.reset()
        
        // Move right
        val result1 = env.step(1)
        assertEquals(Pair(1, 0), env.getAgentPosition())
        assertFalse(result1.done)
        assertTrue(result1.reward < 0) // Step penalty
        
        // Move down
        val result2 = env.step(2)
        assertEquals(Pair(1, 1), env.getAgentPosition())
        assertFalse(result2.done)
        
        // Try to move out of bounds (left from edge)
        env.reset()
        env.step(3) // left from (0,0)
        assertEquals(Pair(0, 0), env.getAgentPosition()) // Should stay in place
    }
    
    @Test
    fun testGridWorldGoalReaching() {
        val env = GridWorldEnvironment(2, 2, 1, 1) // Smaller grid for easier testing
        env.reset()
        
        // Move to goal in 2 steps
        val result1 = env.step(1) // right to (1,0)
        assertFalse(result1.done)
        
        val result2 = env.step(2) // down to (1,1) - goal
        assertTrue(result2.done)
        assertTrue(result2.reward > 0) // Goal reward
        
        assertTrue(env.isTerminal(result2.nextState))
    }
    
    @Test
    fun testGridWorldValidActions() {
        val env = GridWorldEnvironment()
        val state = env.reset()
        
        val validActions = env.getValidActions(state)
        assertEquals(listOf(0, 1, 2, 3), validActions)
    }
    
    @Test
    fun testMultiarmedBanditBasics() {
        val bandit = MultiarmedBanditEnvironment(3)
        
        assertEquals(1, bandit.getStateSize())
        assertEquals(3, bandit.getActionSize())
        
        val initialState = bandit.reset()
        assertEquals(1, initialState.size)
        assertEquals(0.0, initialState[0]) // Step count starts at 0
        
        assertFalse(bandit.isTerminal(initialState))
    }
    
    @Test
    fun testMultiarmedBanditActions() {
        val bandit = MultiarmedBanditEnvironment(4)
        bandit.reset()
        
        val validActions = bandit.getValidActions(doubleArrayOf(0.0))
        assertEquals(listOf(0, 1, 2, 3), validActions)
        
        // Test each action
        for (action in validActions) {
            bandit.reset()
            val result = bandit.step(action)
            assertTrue(result.reward == 0.0 || result.reward == 1.0) // Binary reward
            assertFalse(result.done) // Should not be done after 1 step
        }
    }
    
    @Test
    fun testMultiarmedBanditTermination() {
        val bandit = MultiarmedBanditEnvironment(2)
        bandit.reset()
        
        // Run for many steps to reach termination
        var done = false
        var stepCount = 0
        
        while (!done && stepCount < 150) { // Safety limit
            val result = bandit.step(0) // Always choose arm 0
            done = result.done
            stepCount++
        }
        
        assertTrue(done)
        assertTrue(stepCount <= 100) // Should terminate at max steps
    }
    
    @Test
    fun testDQNOnGridWorld() {
        val env = GridWorldEnvironment(3, 3, 2, 2)
        
        // Create DQN components
        val qNetwork = SimpleNeuralNetwork(4, 4)
        val targetNetwork = SimpleNeuralNetwork(4, 4)
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(1000)
        val dqn = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay, batchSize = 4)
        val exploration = EpsilonGreedyStrategy<Int>(0.3, 0.99, 0.01)
        
        val agent = NeuralNetworkAgent(dqn, exploration)
        
        // Run a few episodes
        var totalReward = 0.0
        val numEpisodes = 5
        
        for (episode in 1..numEpisodes) {
            var state = env.reset()
            var episodeReward = 0.0
            var done = false
            
            while (!done) {
                val validActions = env.getValidActions(state)
                val action = agent.selectAction(state, validActions)
                val result = env.step(action)
                
                val experience = Experience(
                    state = state,
                    action = action,
                    reward = result.reward,
                    nextState = result.nextState,
                    done = result.done
                )
                
                agent.learn(experience)
                
                state = result.nextState
                episodeReward += result.reward
                done = result.done
            }
            
            totalReward += episodeReward
            agent.updateExploration(episode)
        }
        
        // Basic sanity checks
        assertTrue(totalReward.isFinite())
        assertTrue(agent.getExplorationRate() <= 0.3) // Should have decayed
    }
    
    // Policy Gradient bandit test removed
    
    @Test
    fun testRLValidationOnToyProblem() {
        val validator = RLValidator()
        
        // Simulate training metrics over time
        val trainingHistory = mutableListOf<RLMetrics>()
        
        for (episode in 1..20) {
            // Simulate improving performance
            val averageReward = 0.3 + (episode * 0.02) + (Random.nextDouble() - 0.5) * 0.1
            val explorationRate = maxOf(0.01, 0.5 * 0.95.pow(episode.toDouble()))
            
            val metrics = RLMetrics(
                episode = episode,
                averageReward = averageReward,
                episodeLength = 50.0,
                explorationRate = explorationRate,
                policyLoss = maxOf(0.1, 2.0 - episode * 0.05),
                policyEntropy = maxOf(0.5, 2.0 - episode * 0.05),
                gradientNorm = 1.0 + (Random.nextDouble() - 0.5) * 0.5
            )
            
            trainingHistory.add(metrics)
        }
        
        // Test convergence detection
        val convergenceStatus = validator.validateConvergence(trainingHistory)
        assertTrue(convergenceStatus in listOf(ConvergenceStatus.IMPROVING, ConvergenceStatus.CONVERGED))
        
        // Test issue detection on normal metrics
        val normalMetrics = trainingHistory.last()
        val issues = validator.detectTrainingIssues(normalMetrics)
        assertTrue(issues.isEmpty() || issues.size <= 1) // Should have few or no issues
        
        // Test validation of policy updates
        val beforeMetrics = trainingHistory[trainingHistory.size - 2]
        val afterMetrics = trainingHistory.last()
        val updateResult = PolicyUpdateResult(
            loss = afterMetrics.policyLoss,
            gradientNorm = afterMetrics.gradientNorm,
            policyEntropy = afterMetrics.policyEntropy
        )
        
        val validation = validator.validatePolicyUpdate(beforeMetrics, afterMetrics, updateResult)
        assertTrue(validation.isValid) // Should be valid for normal training
    }
}
