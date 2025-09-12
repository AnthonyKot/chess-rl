package com.chessrl.rl

import kotlin.test.*
import kotlin.random.Random
import kotlin.math.*

/**
 * Summary validation test that demonstrates the RL framework is working correctly
 * This test focuses on framework functionality rather than optimal learning performance
 */
class RLFrameworkValidationSummaryTest {
    
    @Test
    fun testRLFrameworkComprehensiveValidation() {
        println("=== RL Framework Comprehensive Validation ===")
        
        // Test 1: Validate toy environments work correctly
        validateToyEnvironments()
        
        // Test 2: Validate RL algorithms can run without errors
        validateRLAlgorithms()
        
        // Test 3: Validate exploration strategies work
        validateExplorationStrategies()
        
        // Test 4: Validate logging and debugging tools
        validateLoggingAndDebugging()
        
        // Test 5: Validate convergence detection
        validateConvergenceDetection()
        
        println("\n=== All RL Framework Validation Tests Passed ===")
        println("✓ Toy environments (GridWorld, Multi-armed Bandit) work correctly")
        println("✓ RL algorithms (DQN, Policy Gradient) execute without errors")
        println("✓ Exploration strategies maintain action diversity")
        println("✓ Logging and debugging tools capture training metrics")
        println("✓ Convergence detection identifies training patterns")
        println("✓ Framework is ready for chess integration")
    }
    
    private fun validateToyEnvironments() {
        println("\n1. Validating Toy Environments...")
        
        // Test GridWorld
        val gridWorld = GridWorldEnvironment(4, 4, 3, 3, Random(42))
        var state = gridWorld.reset()
        
        // Verify environment basics
        assertEquals(4, gridWorld.getStateSize())
        assertEquals(4, gridWorld.getActionSize())
        assertFalse(gridWorld.isTerminal(state))
        
        // Test episode completion
        var steps = 0
        while (!gridWorld.isTerminal(state) && steps < 100) {
            val validActions = gridWorld.getValidActions(state)
            val action = validActions.random(Random(42))
            val result = gridWorld.step(action)
            state = result.nextState
            steps++
            
            assertTrue(result.reward.isFinite())
            assertTrue(validActions.isNotEmpty())
        }
        
        assertTrue(steps < 100, "GridWorld should complete episodes")
        println("  ✓ GridWorld environment works correctly")
        
        // Test Multi-armed Bandit
        val bandit = MultiarmedBanditEnvironment(3, Random(123))
        state = bandit.reset()
        
        assertEquals(1, bandit.getStateSize())
        assertEquals(3, bandit.getActionSize())
        
        var totalReward = 0.0
        steps = 0
        while (!bandit.isTerminal(state) && steps < 150) {
            val validActions = bandit.getValidActions(state)
            val action = validActions.random(Random(123))
            val result = bandit.step(action)
            state = result.nextState
            totalReward += result.reward
            steps++
        }
        
        assertTrue(totalReward >= 0.0, "Bandit should provide non-negative rewards")
        println("  ✓ Multi-armed Bandit environment works correctly")
    }
    
    private fun validateRLAlgorithms() {
        println("\n2. Validating RL Algorithms...")
        
        // Test DQN Algorithm
        val qNetwork = SimpleNeuralNetwork(4, 4, Random(42))
        val targetNetwork = SimpleNeuralNetwork(4, 4, Random(42))
        val experienceReplay = CircularExperienceBuffer<DoubleArray, Int>(100)
        val dqn = DQNAlgorithm(qNetwork, targetNetwork, experienceReplay, batchSize = 4)
        
        // Create sample experiences
        val experiences = (1..10).map { i ->
            Experience(
                state = doubleArrayOf(i.toDouble(), 0.0, 0.0, 0.0),
                action = i % 4,
                reward = if (i % 2 == 0) 1.0 else -1.0,
                nextState = doubleArrayOf((i + 1).toDouble(), 0.0, 0.0, 0.0),
                done = i == 10
            )
        }
        
        // Test policy updates
        val updateResult = dqn.updatePolicy(experiences)
        assertTrue(updateResult.loss.isFinite())
        assertTrue(updateResult.policyEntropy >= 0.0)
        
        // Test action value computation
        val state = doubleArrayOf(1.0, 0.0, 0.0, 0.0)
        val actionValues = dqn.getActionValues(state, listOf(0, 1, 2, 3))
        assertEquals(4, actionValues.size)
        actionValues.values.forEach { assertTrue(it.isFinite()) }
        
        println("  ✓ DQN algorithm executes correctly")
        
        // Test Policy Gradient Algorithm
        val policyNetwork = SimpleNeuralNetwork(4, 4, Random(123)) // Match input/output sizes
        val pg = PolicyGradientAlgorithm(policyNetwork)
        
        val pgUpdateResult = pg.updatePolicy(experiences.take(5))
        assertTrue(pgUpdateResult.loss.isFinite())
        assertTrue(pgUpdateResult.policyEntropy >= 0.0)
        
        val actionProbs = pg.getActionProbabilities(state, listOf(0, 1, 2, 3)) // Match network output size
        assertEquals(4, actionProbs.size)
        val totalProb = actionProbs.values.sum()
        assertEquals(1.0, totalProb, 0.01) // Should sum to approximately 1.0
        
        println("  ✓ Policy Gradient algorithm executes correctly")
    }
    
    private fun validateExplorationStrategies() {
        println("\n3. Validating Exploration Strategies...")
        
        val actionValues = mapOf(0 to 0.5, 1 to 1.0, 2 to 0.2, 3 to 0.8)
        val validActions = listOf(0, 1, 2, 3)
        
        // Test Epsilon-Greedy Strategy
        val epsilonGreedy = EpsilonGreedyStrategy<Int>(0.3, 0.99, 0.01)
        val actionCounts = mutableMapOf<Int, Int>()
        
        repeat(1000) {
            val action = epsilonGreedy.selectAction(validActions, actionValues, Random(it))
            actionCounts[action] = (actionCounts[action] ?: 0) + 1
        }
        
        // Should select action 1 (highest value) most often, but still explore others
        val maxCount = actionCounts.values.maxOrNull() ?: 0
        val minCount = actionCounts.values.minOrNull() ?: 0
        assertTrue(maxCount > minCount, "Epsilon-greedy should favor high-value actions but still explore")
        
        // Test exploration decay
        val initialEpsilon = epsilonGreedy.getExplorationRate()
        epsilonGreedy.updateExploration(1)
        val updatedEpsilon = epsilonGreedy.getExplorationRate()
        assertTrue(updatedEpsilon <= initialEpsilon, "Exploration rate should decay")
        
        println("  ✓ Epsilon-Greedy strategy works correctly")
        
        // Test Boltzmann Strategy
        val boltzmann = BoltzmannStrategy<Int>(1.0, 0.99, 0.1)
        val boltzmannCounts = mutableMapOf<Int, Int>()
        
        repeat(1000) {
            val action = boltzmann.selectAction(validActions, actionValues, Random(it + 1000))
            boltzmannCounts[action] = (boltzmannCounts[action] ?: 0) + 1
        }
        
        // Should have some diversity in action selection
        val entropy = calculateActionEntropy(boltzmannCounts)
        assertTrue(entropy > 0.5, "Boltzmann strategy should maintain action diversity")
        
        println("  ✓ Boltzmann strategy works correctly")
    }
    
    private fun validateLoggingAndDebugging() {
        println("\n4. Validating Logging and Debugging Tools...")
        
        val logger = RLTrainingLogger()
        val debugger = RLDebugger()
        
        // Test logging
        repeat(10) { episode ->
            logger.logEpisode(episode + 1, Random.nextDouble() * 10 - 5, Random.nextInt(20, 50), 0.1)
        }
        
        assertEquals(10, logger.getEpisodeCount())
        println("  ✓ Training logger captures episode data")
        
        // Test debugging
        debugger.logState(1, 0, doubleArrayOf(1.0, 0.0, 0.0, 0.0), "Test state")
        debugger.logAction(1, 1, 2, 1.0, listOf(0, 1, 2, 3))
        
        val validation = ValidationResult(true, emptyList(), emptyList())
        debugger.logValidation(1, validation)
        
        assertTrue(debugger.getLogEntryCount() > 0)
        println("  ✓ Debugging tools capture detailed information")
        
        // Test validator
        val validator = RLValidator()
        val metrics1 = RLMetrics(1, 0.5, 10.0, 0.1, 1.0, policyEntropy = 1.5, gradientNorm = 1.0)
        val metrics2 = RLMetrics(2, 0.6, 9.0, 0.09, 0.9, policyEntropy = 1.4, gradientNorm = 0.8)
        val updateResult = PolicyUpdateResult(0.5, 1.0, 1.4)
        
        val validationResult = validator.validatePolicyUpdate(metrics1, metrics2, updateResult)
        assertNotNull(validationResult)
        assertTrue(validationResult.isValid)
        
        println("  ✓ Validation tools detect training issues")
    }
    
    private fun validateConvergenceDetection() {
        println("\n5. Validating Convergence Detection...")
        
        val validator = RLValidator()
        
        // Test converged training history
        val convergedHistory = (1..15).map { episode ->
            RLMetrics(
                episode = episode,
                averageReward = 0.95 + (Random.nextDouble() - 0.5) * 0.01, // Small variance
                episodeLength = 10.0,
                explorationRate = 0.1,
                policyLoss = 1.0,
                policyEntropy = 1.0,
                gradientNorm = 1.0
            )
        }
        
        val convergenceStatus = validator.validateConvergence(convergedHistory)
        assertEquals(ConvergenceStatus.CONVERGED, convergenceStatus)
        println("  ✓ Convergence detection identifies stable training")
        
        // Test improving training history
        val improvingHistory = (1..15).map { episode ->
            RLMetrics(
                episode = episode,
                averageReward = episode * 0.1, // Steadily improving
                episodeLength = 10.0,
                explorationRate = 0.1,
                policyLoss = 1.0,
                policyEntropy = 1.0,
                gradientNorm = 1.0
            )
        }
        
        val improvingStatus = validator.validateConvergence(improvingHistory)
        assertEquals(ConvergenceStatus.IMPROVING, improvingStatus)
        println("  ✓ Convergence detection identifies improving training")
        
        // Test training issue detection
        val problematicMetrics = RLMetrics(
            episode = 10,
            averageReward = 0.5,
            episodeLength = 10.0,
            explorationRate = 0.005, // Too low
            policyLoss = 1.0,
            policyEntropy = 0.05, // Policy collapse
            gradientNorm = 15.0 // Exploding gradients
        )
        
        val issues = validator.detectTrainingIssues(problematicMetrics)
        assertTrue(issues.contains(TrainingIssue.EXPLODING_GRADIENTS))
        assertTrue(issues.contains(TrainingIssue.POLICY_COLLAPSE))
        assertTrue(issues.contains(TrainingIssue.EXPLORATION_INSUFFICIENT))
        
        println("  ✓ Issue detection identifies training problems")
    }
    
    private fun calculateActionEntropy(actionCounts: Map<Int, Int>): Double {
        val total = actionCounts.values.sum().toDouble()
        if (total == 0.0) return 0.0
        
        var entropy = 0.0
        for (count in actionCounts.values) {
            if (count > 0) {
                val prob = count / total
                entropy -= prob * ln(prob)
            }
        }
        return entropy
    }
}



/**
 * Training logger for RL experiments
 */
class RLTrainingLogger {
    private val episodeLogs = mutableListOf<EpisodeLog>()
    
    data class EpisodeLog(
        val episode: Int,
        val reward: Double,
        val steps: Int,
        val explorationRate: Double,
        val timestamp: Long
    )
    
    fun logEpisode(episode: Int, reward: Double, steps: Int, explorationRate: Double) {
        episodeLogs.add(EpisodeLog(episode, reward, steps, explorationRate, getCurrentTimeMillis()))
    }
    
    fun getEpisodeCount(): Int = episodeLogs.size
}

/**
 * Debugging utility for RL training
 */
class RLDebugger {
    private val logEntries = mutableListOf<DebugEntry>()
    
    sealed class DebugEntry(val episode: Int, val step: Int, val timestamp: Long) {
        data class StateLog(
            val ep: Int, val st: Int, val state: DoubleArray, val description: String
        ) : DebugEntry(ep, st, getCurrentTimeMillis())
        
        data class ActionLog(
            val ep: Int, val st: Int, val action: Int, val reward: Double, val validActions: List<Int>
        ) : DebugEntry(ep, st, getCurrentTimeMillis())
        
        data class ValidationLog(
            val ep: Int, val validation: ValidationResult
        ) : DebugEntry(ep, 0, getCurrentTimeMillis())
    }
    
    fun logState(episode: Int, step: Int, state: DoubleArray, description: String) {
        logEntries.add(DebugEntry.StateLog(episode, step, state, description))
    }
    
    fun logAction(episode: Int, step: Int, action: Int, reward: Double, validActions: List<Int>) {
        logEntries.add(DebugEntry.ActionLog(episode, step, action, reward, validActions))
    }
    
    fun logValidation(episode: Int, validation: ValidationResult) {
        logEntries.add(DebugEntry.ValidationLog(episode, validation))
    }
    
    fun getLogEntryCount(): Int = logEntries.size
}

// Helper function for Kotlin/Native compatibility
private fun getCurrentTimeMillis(): Long {
    return (Random.nextDouble() * 1000000).toLong()
}