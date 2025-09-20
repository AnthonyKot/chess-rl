package com.chessrl.integration

import com.chessrl.rl.*
import com.chessrl.chess.*

/**
 * Demonstration of the Robust Training Validation System
 * 
 * Shows how to use the comprehensive training validation system including:
 * - Learning vs. stagnation detection
 * - Baseline evaluation against heuristic opponents
 * - Chess progress tracking
 * - Early stopping detection
 * - Integration with existing training pipelines
 */
object RobustValidationDemo {
    
    /**
     * Run comprehensive validation demonstration
     */
    fun runDemo() {
        println("🔍 ROBUST TRAINING VALIDATION SYSTEM DEMO")
        println("=" * 80)
        
        // Initialize components
        val validator = initializeValidator()
        val agent = createDemoAgent()
        val environment = createDemoEnvironment()
        
        // Simulate training with validation
        simulateTrainingWithValidation(validator, agent, environment)
        
        // Generate final report
        generateFinalReport(validator)
        
        println("\n✅ Robust Validation Demo completed successfully!")
    }
    
    /**
     * Initialize the robust training validator
     */
    private fun initializeValidator(): RobustTrainingValidator {
        println("\n📋 Initializing Robust Training Validator...")
        
        val config = RobustValidationConfig(
            learningDetectionWindow = 8,
            baselineEvaluationInterval = 5,
            baselineGamesPerOpponent = 10
        )
        
        val validator = RobustTrainingValidator(config)
        validator.initialize()
        
        println("✅ Validator initialized with configuration:")
        println("   Learning detection window: ${config.learningDetectionWindow}")
        println("   Baseline evaluation interval: ${config.baselineEvaluationInterval}")
        println("   Games per opponent: ${config.baselineGamesPerOpponent}")
        
        return validator
    }
    
    /**
     * Create demo chess agent
     */
    private fun createDemoAgent(): ChessAgent {
        println("\n🤖 Creating demo chess agent...")
        
        val config = ChessAgentConfig()
        
        val agent = ChessAgentFactory.createDQNAgent(listOf(512, 256, 128))
        println("✅ Demo agent created with DQN architecture")
        
        return agent
    }
    
    /**
     * Create demo chess environment
     */
    private fun createDemoEnvironment(): ChessEnvironment {
        println("\n♟️ Creating demo chess environment...")
        
        val environment = ChessEnvironment()
        println("✅ Chess environment created")
        
        return environment
    }
    
    /**
     * Simulate training cycles with comprehensive validation
     */
    private fun simulateTrainingWithValidation(
        validator: RobustTrainingValidator,
        agent: ChessAgent,
        environment: ChessEnvironment
    ) {
        println("\n🎯 Simulating training with robust validation...")
        println("=" * 60)
        
        val trainingScenarios = listOf(
            TrainingScenario("Initial Learning", 5, ScenarioType.IMPROVING),
            TrainingScenario("Steady Progress", 8, ScenarioType.STEADY),
            TrainingScenario("Stagnation Period", 6, ScenarioType.STAGNANT),
            TrainingScenario("Recovery", 4, ScenarioType.IMPROVING),
            TrainingScenario("Instability", 3, ScenarioType.UNSTABLE),
            TrainingScenario("Final Convergence", 5, ScenarioType.CONVERGING)
        )
        
        var cycle = 0
        
        trainingScenarios.forEach { scenario ->
            println("\n📊 Scenario: ${scenario.name} (${scenario.cycles} cycles)")
            println("-" * 40)
            
            repeat(scenario.cycles) { scenarioCycle ->
                val validationResult = performValidationCycle(
                    validator, agent, environment, cycle, scenario.type
                )
                
                displayCycleResults(cycle, validationResult)
                
                // Check for early stopping
                if (validationResult.shouldStop) {
                    println("\n🛑 Early stopping recommended: ${validationResult.earlyStoppingRecommendation.reason}")
                    println("   Confidence: ${String.format("%.3f", validationResult.earlyStoppingRecommendation.confidence)}")
                    
                    if (validationResult.earlyStoppingRecommendation.confidence > 0.8) {
                        println("   High confidence - stopping simulation")
                        return
                    }
                }
                
                cycle++
            }
        }
    }
    
    /**
     * Perform a single validation cycle
     */
    private fun performValidationCycle(
        validator: RobustTrainingValidator,
        agent: ChessAgent,
        environment: ChessEnvironment,
        cycle: Int,
        scenarioType: ScenarioType
    ): RobustValidationResult {
        
        // Generate scenario-appropriate metrics
        val trainingMetrics = generateScenarioMetrics(cycle, scenarioType)
        val updateResult = generateScenarioUpdateResult(cycle, scenarioType)
        val gameResults = generateScenarioGameResults(cycle, scenarioType)
        
        // Perform validation
        return validator.validateTrainingCycle(
            cycle = cycle,
            agent = agent,
            environment = environment,
            trainingMetrics = trainingMetrics,
            updateResult = updateResult,
            gameResults = gameResults
        )
    }
    
    /**
     * Generate scenario-appropriate training metrics
     */
    private fun generateScenarioMetrics(cycle: Int, scenarioType: ScenarioType): RLMetrics {
        return when (scenarioType) {
            ScenarioType.IMPROVING -> RLMetrics(
                episode = cycle,
                averageReward = 0.3 + (cycle * 0.05).coerceAtMost(0.4),
                episodeLength = 40.0 + cycle * 2,
                explorationRate = 0.3 - (cycle * 0.02).coerceAtLeast(0.05),
                policyLoss = 0.2 - (cycle * 0.01).coerceAtLeast(0.05),
                policyEntropy = 0.8 - (cycle * 0.02).coerceAtLeast(0.3),
                gradientNorm = 1.0 + kotlin.random.Random.nextDouble() * 0.5,
                qValueStats = QValueStats(1.0 + cycle * 0.1, 2.0, 0.5, 0.8)
            )
            ScenarioType.STEADY -> RLMetrics(
                episode = cycle,
                averageReward = 0.6 + (kotlin.random.Random.nextDouble() - 0.5) * 0.05,
                episodeLength = 50.0,
                explorationRate = 0.1,
                policyLoss = 0.08 + (kotlin.random.Random.nextDouble() - 0.5) * 0.02,
                policyEntropy = 0.5 + (kotlin.random.Random.nextDouble() - 0.5) * 0.1,
                gradientNorm = 0.8 + kotlin.random.Random.nextDouble() * 0.4,
                qValueStats = QValueStats(1.5, 2.5, 0.3, 1.0)
            )
            ScenarioType.STAGNANT -> RLMetrics(
                episode = cycle,
                averageReward = 0.5 + (kotlin.random.Random.nextDouble() - 0.5) * 0.01,
                episodeLength = 45.0,
                explorationRate = 0.05,
                policyLoss = 0.1 + (kotlin.random.Random.nextDouble() - 0.5) * 0.005,
                policyEntropy = 0.2 + (kotlin.random.Random.nextDouble() - 0.5) * 0.02,
                gradientNorm = 0.3 + kotlin.random.Random.nextDouble() * 0.2,
                qValueStats = QValueStats(1.2, 2.0, 0.2, 0.9)
            )
            ScenarioType.UNSTABLE -> RLMetrics(
                episode = cycle,
                averageReward = 0.4 + (kotlin.random.Random.nextDouble() - 0.5) * 0.3,
                episodeLength = 30.0 + kotlin.random.Random.nextDouble() * 40,
                explorationRate = 0.2 + kotlin.random.Random.nextDouble() * 0.2,
                policyLoss = 0.15 + kotlin.random.Random.nextDouble() * 0.2,
                policyEntropy = 0.1 + kotlin.random.Random.nextDouble() * 0.8,
                gradientNorm = 5.0 + kotlin.random.Random.nextDouble() * 10.0,
                qValueStats = QValueStats(2.0 + kotlin.random.Random.nextDouble() * 3, 5.0, 1.0, 1.5)
            )
            ScenarioType.CONVERGING -> RLMetrics(
                episode = cycle,
                averageReward = 0.8 + (kotlin.random.Random.nextDouble() - 0.5) * 0.02,
                episodeLength = 60.0,
                explorationRate = 0.02,
                policyLoss = 0.03 + (kotlin.random.Random.nextDouble() - 0.5) * 0.005,
                policyEntropy = 0.6 + (kotlin.random.Random.nextDouble() - 0.5) * 0.05,
                gradientNorm = 0.5 + kotlin.random.Random.nextDouble() * 0.2,
                qValueStats = QValueStats(2.5, 3.0, 0.1, 2.0)
            )
        }
    }
    
    /**
     * Generate scenario-appropriate update results
     */
    private fun generateScenarioUpdateResult(cycle: Int, scenarioType: ScenarioType): PolicyUpdateResult {
        return when (scenarioType) {
            ScenarioType.IMPROVING -> PolicyUpdateResult(
                loss = 0.1 - (cycle * 0.005).coerceAtLeast(0.02),
                gradientNorm = 1.0 + kotlin.random.Random.nextDouble() * 0.5,
                policyEntropy = 0.8 - (cycle * 0.02).coerceAtLeast(0.3)
            )
            ScenarioType.STEADY -> PolicyUpdateResult(
                loss = 0.05 + (kotlin.random.Random.nextDouble() - 0.5) * 0.01,
                gradientNorm = 0.8 + kotlin.random.Random.nextDouble() * 0.4,
                policyEntropy = 0.5 + (kotlin.random.Random.nextDouble() - 0.5) * 0.1
            )
            ScenarioType.STAGNANT -> PolicyUpdateResult(
                loss = 0.08 + (kotlin.random.Random.nextDouble() - 0.5) * 0.005,
                gradientNorm = 0.3 + kotlin.random.Random.nextDouble() * 0.2,
                policyEntropy = 0.2 + (kotlin.random.Random.nextDouble() - 0.5) * 0.02
            )
            ScenarioType.UNSTABLE -> PolicyUpdateResult(
                loss = 0.2 + kotlin.random.Random.nextDouble() * 0.5,
                gradientNorm = 5.0 + kotlin.random.Random.nextDouble() * 10.0,
                policyEntropy = 0.1 + kotlin.random.Random.nextDouble() * 0.8
            )
            ScenarioType.CONVERGING -> PolicyUpdateResult(
                loss = 0.02 + (kotlin.random.Random.nextDouble() - 0.5) * 0.005,
                gradientNorm = 0.5 + kotlin.random.Random.nextDouble() * 0.2,
                policyEntropy = 0.6 + (kotlin.random.Random.nextDouble() - 0.5) * 0.05
            )
        }
    }
    
    /**
     * Generate scenario-appropriate game results
     */
    private fun generateScenarioGameResults(cycle: Int, scenarioType: ScenarioType): List<ChessGameResult> {
        val gameCount = 5
        
        return (0 until gameCount).map { gameIndex ->
            when (scenarioType) {
                ScenarioType.IMPROVING -> ChessGameResult(
                    outcome = if (gameIndex < 3 + (cycle / 3)) "WIN" else "DRAW",
                    moveCount = 30 + cycle * 2 + kotlin.random.Random.nextInt(-5, 6),
                    moves = generateMockMoves(30 + cycle * 2),
                    legalMoveRate = 0.95 + kotlin.random.Random.nextDouble() * 0.05
                )
                ScenarioType.STEADY -> ChessGameResult(
                    outcome = if (gameIndex < 3) "WIN" else if (gameIndex < 4) "DRAW" else "LOSS",
                    moveCount = 45 + kotlin.random.Random.nextInt(-10, 11),
                    moves = generateMockMoves(45),
                    legalMoveRate = 0.98
                )
                ScenarioType.STAGNANT -> ChessGameResult(
                    outcome = if (gameIndex < 2) "WIN" else if (gameIndex < 4) "DRAW" else "LOSS",
                    moveCount = 40 + kotlin.random.Random.nextInt(-5, 6),
                    moves = generateMockMoves(40),
                    legalMoveRate = 0.96
                )
                ScenarioType.UNSTABLE -> ChessGameResult(
                    outcome = listOf("WIN", "LOSS", "DRAW").random(),
                    moveCount = kotlin.random.Random.nextInt(15, 80),
                    moves = generateMockMoves(kotlin.random.Random.nextInt(15, 80)),
                    legalMoveRate = 0.85 + kotlin.random.Random.nextDouble() * 0.15
                )
                ScenarioType.CONVERGING -> ChessGameResult(
                    outcome = if (gameIndex < 4) "WIN" else "DRAW",
                    moveCount = 55 + kotlin.random.Random.nextInt(-5, 6),
                    moves = generateMockMoves(55),
                    legalMoveRate = 0.99
                )
            }
        }
    }
    
    /**
     * Generate mock chess moves
     */
    private fun generateMockMoves(count: Int): List<String> {
        val moves = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O", "Be7")
        return (0 until count).map { moves[it % moves.size] + if (it > 9) "${it}" else "" }
    }
    
    /**
     * Display cycle validation results
     */
    private fun displayCycleResults(cycle: Int, result: RobustValidationResult) {
        val status = if (result.isValid) "✅" else "❌"
        val learningStatus = result.learningStatus.status.name.lowercase().replaceFirstChar { it.uppercase() }
        
        println("Cycle $cycle: $status Valid | Learning: $learningStatus | Score: ${String.format("%.3f", result.overallPerformanceScore)}")
        
        if (result.baselineEvaluation != null) {
            println("   🎯 Baseline: ${String.format("%.3f", result.baselineEvaluation.overallScore)} " +
                   "(vs Heuristic: ${String.format("%.1f%%", result.baselineEvaluation.heuristicOpponentResult.winRate * 100)})")
        }
        
        if (result.recommendations.isNotEmpty()) {
            val topRec = result.recommendations.first()
            println("   💡 ${topRec.priority.name}: ${topRec.title}")
        }
    }
    
    /**
     * Generate final comprehensive report
     */
    private fun generateFinalReport(validator: RobustTrainingValidator) {
        println("\n📊 FINAL VALIDATION REPORT")
        println("=" * 60)
        
        val summary = validator.getValidationSummary()
        val exportData = validator.exportValidationData("json")
        
        println("📈 Training Summary:")
        println("   Total Validations: ${summary.totalValidations}")
        println("   Valid Validations: ${summary.validValidations} (${String.format("%.1f%%", summary.validValidations.toDouble() / summary.totalValidations * 100)})")
        println("   Early Stopping Suggestions: ${summary.earlyStoppingSuggestions}")
        println("   Best Performance Score: ${String.format("%.3f", summary.bestPerformanceScore)}")
        println("   Current Learning Status: ${summary.currentLearningStatus}")
        
        println("\n🎯 Baseline Evaluation:")
        println("   Total Evaluations: ${summary.baselineEvaluations}")
        println("   Average Baseline Score: ${String.format("%.3f", summary.averageBaselineScore)}")
        
        println("\n📊 Validation Performance:")
        println("   Total Validation Time: ${summary.totalValidationTime}ms")
        println("   Average Time per Validation: ${if (summary.totalValidations > 0) summary.totalValidationTime / summary.totalValidations else 0}ms")
        
        println("\n💡 Final Recommendations:")
        summary.recommendations.take(5).forEach { rec ->
            println("   ${rec.priority.name}: ${rec.title}")
        }
        
        println("\n📁 Data Export:")
        println("   Format: ${exportData.format}")
        println("   Validation History: ${exportData.validationHistory.size} entries")
        println("   Baseline Results: ${exportData.baselineResults.size} entries")
        println("   Progress Snapshots: ${exportData.progressSnapshots.size} entries")
        
        println("\n✅ Robust Training Validation System demonstrated successfully!")
        println("   The system provides comprehensive validation including:")
        println("   • Learning vs. stagnation detection")
        println("   • Baseline evaluation against heuristic opponents")
        println("   • Real chess improvement tracking")
        println("   • Early stopping and training issue detection")
        println("   • Integration with existing validation components")
    }
}

// Supporting data classes for demo

/**
 * Training scenario for demonstration
 */
data class TrainingScenario(
    val name: String,
    val cycles: Int,
    val type: ScenarioType
)

/**
 * Types of training scenarios
 */
enum class ScenarioType {
    IMPROVING,
    STEADY,
    STAGNANT,
    UNSTABLE,
    CONVERGING
}

/**
 * Main function to run the demo
 */
fun main() {
    RobustValidationDemo.runDemo()
}