package com.chessrl.rl

import kotlin.random.Random
import kotlin.test.*

class ExplorationStrategyTest {
    
    @Test
    fun testEpsilonGreedyExploration() {
        val strategy = EpsilonGreedyStrategy<String>(epsilon = 0.5)
        val validActions = listOf("action1", "action2", "action3")
        val actionValues = mapOf(
            "action1" to 1.0,
            "action2" to 5.0,  // Best action
            "action3" to 2.0
        )
        
        // Test with fixed random seed for deterministic behavior
        val random = Random(42)
        
        // With epsilon = 0.5, we should see both exploration and exploitation
        val selectedActions = mutableListOf<String>()
        repeat(100) {
            selectedActions.add(strategy.selectAction(validActions, actionValues, random))
        }
        
        // Should have selected all actions (exploration)
        assertTrue(selectedActions.contains("action1"))
        assertTrue(selectedActions.contains("action2"))
        assertTrue(selectedActions.contains("action3"))
        
        // action2 should be selected most often (exploitation)
        val action2Count = selectedActions.count { it == "action2" }
        assertTrue(action2Count > 20, "Best action should be selected frequently")
    }
    
    @Test
    fun testEpsilonGreedyPureExploitation() {
        val strategy = EpsilonGreedyStrategy<String>(epsilon = 0.0)
        val validActions = listOf("action1", "action2", "action3")
        val actionValues = mapOf(
            "action1" to 1.0,
            "action2" to 5.0,  // Best action
            "action3" to 2.0
        )
        
        // With epsilon = 0, should always select best action
        repeat(10) {
            val selected = strategy.selectAction(validActions, actionValues)
            assertEquals("action2", selected)
        }
    }
    
    @Test
    fun testEpsilonGreedyPureExploration() {
        val strategy = EpsilonGreedyStrategy<String>(epsilon = 1.0)
        val validActions = listOf("action1", "action2", "action3")
        val actionValues = mapOf(
            "action1" to 1.0,
            "action2" to 5.0,
            "action3" to 2.0
        )
        
        val random = Random(123)
        val selectedActions = mutableSetOf<String>()
        
        // With epsilon = 1, should explore randomly
        repeat(50) {
            selectedActions.add(strategy.selectAction(validActions, actionValues, random))
        }
        
        // Should have selected multiple different actions
        assertTrue(selectedActions.size > 1, "Should explore different actions")
    }
    
    @Test
    fun testEpsilonDecay() {
        val strategy = EpsilonGreedyStrategy<String>(
            epsilon = 1.0,
            epsilonDecay = 0.9,
            minEpsilon = 0.1
        )
        
        assertEquals(1.0, strategy.getExplorationRate(), 0.001)
        
        strategy.updateExploration(1)
        assertEquals(0.9, strategy.getExplorationRate(), 0.001)
        
        strategy.updateExploration(2)
        assertEquals(0.81, strategy.getExplorationRate(), 0.001)
        
        // Test minimum epsilon
        repeat(100) {
            strategy.updateExploration(it)
        }
        assertTrue(strategy.getExplorationRate() >= 0.1)
    }
    
    @Test
    fun testEpsilonGreedyWithEmptyActions() {
        val strategy = EpsilonGreedyStrategy<String>()
        val emptyActions = emptyList<String>()
        val actionValues = emptyMap<String, Double>()
        
        assertFailsWith<IllegalArgumentException> {
            strategy.selectAction(emptyActions, actionValues)
        }
    }
    
    @Test
    fun testBoltzmannExploration() {
        val strategy = BoltzmannStrategy<String>(temperature = 1.0)
        val validActions = listOf("action1", "action2", "action3")
        val actionValues = mapOf(
            "action1" to 1.0,
            "action2" to 3.0,  // Best action
            "action3" to 2.0
        )
        
        val random = Random(42)
        val selectedActions = mutableListOf<String>()
        
        repeat(100) {
            selectedActions.add(strategy.selectAction(validActions, actionValues, random))
        }
        
        // Should select all actions but favor better ones
        assertTrue(selectedActions.contains("action1"))
        assertTrue(selectedActions.contains("action2"))
        assertTrue(selectedActions.contains("action3"))
        
        // action2 should be selected most often
        val action2Count = selectedActions.count { it == "action2" }
        val action1Count = selectedActions.count { it == "action1" }
        assertTrue(action2Count > action1Count, "Better action should be selected more often")
    }
    
    @Test
    fun testBoltzmannTemperatureDecay() {
        val strategy = BoltzmannStrategy<String>(
            temperature = 2.0,
            temperatureDecay = 0.9,
            minTemperature = 0.1
        )
        
        assertEquals(2.0, strategy.getExplorationRate(), 0.001)
        
        strategy.updateExploration(1)
        assertEquals(1.8, strategy.getExplorationRate(), 0.001)
        
        // Test minimum temperature
        repeat(100) {
            strategy.updateExploration(it)
        }
        assertTrue(strategy.getExplorationRate() >= 0.1)
    }
    
    @Test
    fun testBoltzmannWithSingleAction() {
        val strategy = BoltzmannStrategy<String>()
        val singleAction = listOf("only_action")
        val actionValues = mapOf("only_action" to 1.0)
        
        val selected = strategy.selectAction(singleAction, actionValues)
        assertEquals("only_action", selected)
    }
    
    @Test
    fun testBoltzmannWithEmptyActions() {
        val strategy = BoltzmannStrategy<String>()
        val emptyActions = emptyList<String>()
        val actionValues = emptyMap<String, Double>()
        
        assertFailsWith<IllegalArgumentException> {
            strategy.selectAction(emptyActions, actionValues)
        }
    }
}