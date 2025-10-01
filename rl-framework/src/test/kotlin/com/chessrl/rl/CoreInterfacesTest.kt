package com.chessrl.rl

import kotlin.test.*

class CoreInterfacesTest {
    
    // Simple test environment for testing
    class TestEnvironment : Environment<Int, String> {
        private var currentState = 0
        private val maxSteps = 5
        private var stepCount = 0
        
        override fun reset(): Int {
            currentState = 0
            stepCount = 0
            return currentState
        }
        
        override fun step(action: String): StepResult<Int> {
            stepCount++
            
            val reward = when (action) {
                "good" -> 1.0
                "bad" -> -1.0
                else -> 0.0
            }
            
            currentState += 1
            val done = stepCount >= maxSteps || action == "terminal"
            
            return StepResult(
                nextState = currentState,
                reward = reward,
                done = done,
                info = mapOf("step" to stepCount)
            )
        }
        
        override fun getValidActions(state: Int): List<String> {
            return listOf("good", "bad", "neutral", "terminal")
        }
        
        override fun isTerminal(state: Int): Boolean {
            return stepCount >= maxSteps
        }
        
        override fun getStateSize(): Int = 1
        
        override fun getActionSize(): Int = 4
    }
    
    // Simple test agent for testing
    class TestAgent : Agent<Int, String> {
        private val experiences = mutableListOf<Experience<Int, String>>()
        private var explorationRate = 0.1
        
        override fun selectAction(state: Int, validActions: List<String>): String {
            // Simple policy: prefer "good" action, otherwise random
            return if (validActions.contains("good") && kotlin.random.Random.nextDouble() > explorationRate) {
                "good"
            } else {
                validActions.random()
            }
        }
        
        override fun learn(experience: Experience<Int, String>) {
            experiences.add(experience)
        }
        
        override fun save(path: String) {
            // Mock implementation
        }
        
        override fun load(path: String) {
            // Mock implementation
        }
        
        fun getExperiences(): List<Experience<Int, String>> = experiences.toList()
        
        fun setExplorationRate(rate: Double) {
            explorationRate = rate
        }
    }
    
    @Test
    fun testEnvironmentBasicFunctionality() {
        val env = TestEnvironment()
        
        // Test reset
        val initialState = env.reset()
        assertEquals(0, initialState)
        
        // Test state and action sizes
        assertEquals(1, env.getStateSize())
        assertEquals(4, env.getActionSize())
        
        // Test valid actions
        val validActions = env.getValidActions(initialState)
        assertEquals(4, validActions.size)
        assertTrue(validActions.contains("good"))
        assertTrue(validActions.contains("bad"))
        assertTrue(validActions.contains("neutral"))
        assertTrue(validActions.contains("terminal"))
        
        // Test terminal check
        assertFalse(env.isTerminal(initialState))
    }
    
    @Test
    fun testEnvironmentStepFunction() {
        val env = TestEnvironment()
        env.reset()
        
        // Test good action
        val goodResult = env.step("good")
        assertEquals(1, goodResult.nextState)
        assertEquals(1.0, goodResult.reward)
        assertFalse(goodResult.done)
        assertEquals(1, goodResult.info["step"])
        
        // Test bad action
        val badResult = env.step("bad")
        assertEquals(2, badResult.nextState)
        assertEquals(-1.0, badResult.reward)
        assertFalse(badResult.done)
        
        // Test neutral action
        val neutralResult = env.step("neutral")
        assertEquals(3, neutralResult.nextState)
        assertEquals(0.0, neutralResult.reward)
        assertFalse(neutralResult.done)
        
        // Test terminal action
        val terminalResult = env.step("terminal")
        assertEquals(4, terminalResult.nextState)
        assertEquals(0.0, terminalResult.reward)
        assertTrue(terminalResult.done)
    }
    
    @Test
    fun testEnvironmentEpisodeCompletion() {
        val env = TestEnvironment()
        env.reset()
        
        // Take maximum steps
        repeat(5) {
            val result = env.step("good")
            if (it < 4) {
                assertFalse(result.done, "Episode should not be done before max steps")
            } else {
                assertTrue(result.done, "Episode should be done after max steps")
            }
        }
        
        assertTrue(env.isTerminal(5))
    }
    
    @Test
    fun testAgentBasicFunctionality() {
        val agent = TestAgent()
        val validActions = listOf("good", "bad", "neutral")
        
        // Test action selection
        val action = agent.selectAction(0, validActions)
        assertTrue(validActions.contains(action))
        
        // Test learning
        val experience = Experience(
            state = 0,
            action = "good",
            reward = 1.0,
            nextState = 1,
            done = false
        )
        
        agent.learn(experience)
        val experiences = agent.getExperiences()
        assertEquals(1, experiences.size)
        assertEquals(experience, experiences.first())
    }
    
    @Test
    fun testAgentExplorationBehavior() {
        val agent = TestAgent()
        val validActions = listOf("good", "bad", "neutral")
        
        // Test with high exploration (should be more random)
        agent.setExplorationRate(0.9)
        val actionsHighExploration = mutableSetOf<String>()
        repeat(20) {
            actionsHighExploration.add(agent.selectAction(0, validActions))
        }
        
        // Test with low exploration (should prefer "good")
        agent.setExplorationRate(0.1)
        val actionsLowExploration = mutableListOf<String>()
        repeat(20) {
            actionsLowExploration.add(agent.selectAction(0, validActions))
        }
        
        val goodActionCount = actionsLowExploration.count { it == "good" }
        assertTrue(goodActionCount > 10, "Agent should prefer 'good' action with low exploration")
    }
    
    @Test
    fun testExperienceDataClass() {
        val experience = Experience(
            state = 42,
            action = "test_action",
            reward = 3.14,
            nextState = 43,
            done = true
        )
        
        assertEquals(42, experience.state)
        assertEquals("test_action", experience.action)
        assertEquals(3.14, experience.reward)
        assertEquals(43, experience.nextState)
        assertTrue(experience.done)
    }
    
    @Test
    fun testStepResultDataClass() {
        val info = mapOf("episode" to 1, "score" to 100)
        val stepResult = StepResult(
            nextState = "new_state",
            reward = 5.0,
            done = false,
            info = info
        )
        
        assertEquals("new_state", stepResult.nextState)
        assertEquals(5.0, stepResult.reward)
        assertFalse(stepResult.done)
        assertEquals(info, stepResult.info)
        
        // Test default empty info
        val stepResultNoInfo = StepResult(
            nextState = "state",
            reward = 1.0,
            done = true
        )
        assertTrue(stepResultNoInfo.info.isEmpty())
    }
    
    @Test
    fun testAgentEnvironmentInteraction() {
        val env = TestEnvironment()
        val agent = TestAgent()
        
        // Run a simple episode
        var state = env.reset()
        var totalReward = 0.0
        var stepCount = 0
        
        while (!env.isTerminal(state) && stepCount < 10) {
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
            
            state = stepResult.nextState
            totalReward += stepResult.reward
            stepCount++
            
            if (stepResult.done) break
        }
        
        // Verify interaction worked
        assertTrue(stepCount > 0, "Should have taken at least one step")
        val experiences = agent.getExperiences()
        assertEquals(stepCount, experiences.size, "Agent should have learned from each step")
        
        // Verify experience chain
        for (i in 0 until experiences.size - 1) {
            assertEquals(experiences[i].nextState, experiences[i + 1].state, 
                "Experience chain should be consistent")
        }
    }
}