package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*

/**
 * Comprehensive demonstration of the production debugging system working end-to-end.
 * This demo shows how all components work together in realistic scenarios.
 */
class ProductionDebuggingDemo {
    
    fun runCompleteDemo() {
        println("🚀 Chess RL Production Debugging System - Complete Demo")
        println("=" * 80)
        
        // Setup
        val agent = DemoChessAgent()
        val environment = DemoChessEnvironment()
        val debuggingInterface = ProductionDebuggingInterface(agent, environment)
        
        // Scenario 1: Training Issue Investigation
        demonstrateTrainingIssueInvestigation(debuggingInterface)
        
        // Scenario 2: Game Analysis Workflow
        demonstrateGameAnalysisWorkflow(debuggingInterface)
        
        // Scenario 3: Network Debugging
        demonstrateNetworkDebugging(debuggingInterface)
        
        // Scenario 4: Production Monitoring
        demonstrateProductionMonitoring(debuggingInterface)
        
        println("\n🎉 Complete Demo Finished Successfully!")
        println("All debugging components working together seamlessly.")
    }
    
    private fun demonstrateTrainingIssueInvestigation(debuggingInterface: ProductionDebuggingInterface) {
        println("\n" + "=" * 60)
        println("📊 SCENARIO 1: Training Issue Investigation")
        println("=" * 60)
        println("Situation: Agent performance has suddenly degraded after 1000 episodes")
        
        val session = debuggingInterface.startDebuggingSession("Training Issue Investigation")
        
        // Step 1: Analyze recent training data
        println("\n🔍 Step 1: Analyzing Recent Training Data...")
        val recentExperiences = generateProblematicExperiences(1000)
        val bufferAnalysis = debuggingInterface.inspectExperienceBuffer(
            recentExperiences, AnalysisDepth.COMPREHENSIVE
        )
        
        val qualityScore = bufferAnalysis.basicAnalysis.qualityScore
        println("Buffer Quality Score: ${(qualityScore * 100).toInt()}%")
        
        if (qualityScore < 0.6) {
            println("❌ ISSUE DETECTED: Poor buffer quality!")
            println("Quality Issues Found:")
            bufferAnalysis.basicAnalysis.qualityIssues.forEach { issue ->
                println("  • $issue")
            }
        }
        
        // Step 2: Analyze training metrics
        println("\n📈 Step 2: Analyzing Training Metrics...")
        val degradedMetrics = generateDegradedMetrics(50)
        val performanceAnalysis = debuggingInterface.profilePerformance(degradedMetrics)
        
        val stability = performanceAnalysis.trainingPerformance.trainingStability
        println("Training Stability: ${(stability * 100).toInt()}%")
        
        if (stability < 0.7) {
            println("❌ ISSUE DETECTED: Training instability!")
        }
        
        // Step 3: Investigate network behavior
        println("\n🧠 Step 3: Investigating Network Behavior...")
        val board = ChessBoard()
        val networkAnalysis = debuggingInterface.analyzeNeuralNetworkActivations(board)
        
        if (networkAnalysis.networkIssues.isNotEmpty()) {
            println("❌ NETWORK ISSUES DETECTED:")
            networkAnalysis.networkIssues.forEach { issue ->
                println("  • ${issue.type} in ${issue.layer}: ${issue.description}")
            }
        }
        
        // Step 4: Generate action plan
        println("\n💡 Step 4: Generating Action Plan...")
        val allRecommendations = mutableListOf<String>()
        allRecommendations.addAll(bufferAnalysis.basicAnalysis.recommendations)
        allRecommendations.addAll(performanceAnalysis.optimizationRecommendations.map { it.description })
        
        println("RECOMMENDED ACTIONS:")
        allRecommendations.distinct().take(5).forEachIndexed { index, rec ->
            println("  ${index + 1}. $rec")
        }
        
        // Step 5: Generate report
        val report = debuggingInterface.generateDebuggingReport(session)
        println("\n📋 Investigation Report Generated:")
        println("  • Session Duration: ${report.sessionDuration}ms")
        println("  • Issues Found: ${report.issuesFound.size}")
        println("  • Recommendations: ${report.recommendations.size}")
        
        println("\n✅ Training issue investigation completed!")
    }
    
    private fun demonstrateGameAnalysisWorkflow(debuggingInterface: ProductionDebuggingInterface) {
        println("\n" + "=" * 60)
        println("🎮 SCENARIO 2: Interactive Game Analysis")
        println("=" * 60)
        println("Situation: Analyzing a game where the agent made questionable moves")
        
        val session = debuggingInterface.startDebuggingSession("Game Analysis")
        
        // Create a realistic game with some suboptimal moves
        val gameHistory = generateRealisticGame()
        
        println("\n🔍 Starting Interactive Game Analysis...")
        println("Game Length: ${gameHistory.size} moves")
        
        val analysisSession = debuggingInterface.analyzeGameInteractively(
            gameHistory = gameHistory,
            annotations = mapOf(
                0 to "Standard opening",
                8 to "Entering middlegame",
                15 to "Critical position",
                gameHistory.size - 3 to "Endgame phase"
            )
        )
        
        // Analyze key positions
        val keyPositions = listOf(0, 8, 15, gameHistory.size - 3)
            .filter { it < gameHistory.size }
        
        println("\n📊 Analyzing Key Positions:")
        keyPositions.forEach { moveIndex ->
            // Navigate to position
            while (analysisSession.currentMoveIndex < moveIndex) {
                debuggingInterface.stepThroughGame(analysisSession, StepDirection.FORWARD)
            }
            
            val step = debuggingInterface.stepThroughGame(analysisSession, StepDirection.FORWARD)
            
            println("\n--- Move ${step.moveNumber} Analysis ---")
            step.move?.let { move ->
                println("Move Played: ${move.toAlgebraic()}")
            }
            
            val analysis = step.positionAnalysis
            val confidence = analysis.decisionAnalysis.decisionConfidence
            val evaluation = analysis.positionEvaluation.netEvaluation
            
            println("Agent Confidence: ${(confidence * 100).toInt()}%")
            println("Position Evaluation: ${String.format("%.2f", evaluation)}")
            
            // Check if move was optimal
            step.moveAnalysis?.let { moveAnalysis ->
                if (moveAnalysis.wasTopChoice) {
                    println("✅ Agent's top choice")
                } else {
                    println("❌ Not optimal (rank: ${moveAnalysis.moveRank})")
                    println("Agent gave this move ${(moveAnalysis.agentProbability * 100).toInt()}% probability")
                }
            }
            
            // Show alternatives
            println("Top Alternatives:")
            analysis.decisionAnalysis.topMoves.take(3).forEach { topMove ->
                println("  ${topMove.rank}. ${topMove.move.toAlgebraic()} (${(topMove.probability * 100).toInt()}%)")
            }
            
            step.annotation?.let { annotation ->
                println("Note: $annotation")
            }
        }
        
        println("\n📈 Game Summary:")
        println("  • Total positions analyzed: ${keyPositions.size}")
        println("  • Interactive navigation demonstrated")
        println("  • Move quality assessment completed")
        
        println("\n✅ Game analysis workflow completed!")
    }
    
    private fun demonstrateNetworkDebugging(debuggingInterface: ProductionDebuggingInterface) {
        println("\n" + "=" * 60)
        println("🧠 SCENARIO 3: Neural Network Debugging")
        println("=" * 60)
        println("Situation: Agent showing inconsistent confidence across similar positions")
        
        val session = debuggingInterface.startDebuggingSession("Network Debugging")
        
        // Test different position types
        val testPositions = listOf(
            Pair(ChessBoard(), "Starting Position"),
            Pair(createMiddlegamePosition(), "Middlegame Position"),
            Pair(createEndgamePosition(), "Endgame Position"),
            Pair(createTacticalPosition(), "Tactical Position")
        )
        
        println("\n🔬 Analyzing Network Behavior Across Position Types...")
        
        testPositions.forEach { (board, description) ->
            println("\n--- $description ---")
            
            // Analyze network confidence
            val confidenceAnalysis = debuggingInterface.analyzeNeuralNetworkActivations(board)
            
            println("Network Health: ${confidenceAnalysis.activationPatterns.activationHealth}")
            
            if (confidenceAnalysis.networkIssues.isNotEmpty()) {
                println("Issues Detected:")
                confidenceAnalysis.networkIssues.forEach { issue ->
                    println("  • ${issue.type}: ${issue.description}")
                }
            }
            
            // Analyze decision confidence
            val positionAnalysis = debuggingInterface.analyzeCurrentPosition(board)
            val confidence = positionAnalysis.decisionAnalysis.decisionConfidence
            val entropy = positionAnalysis.decisionAnalysis.entropy
            
            println("Decision Confidence: ${(confidence * 100).toInt()}%")
            println("Policy Entropy: ${String.format("%.3f", entropy)}")
            
            // Check for uncertainty sources
            val networkOutput = positionAnalysis.neuralNetworkOutput
            if (networkOutput.policyAnalysis.entropy > 3.0) {
                println("⚠️ High uncertainty detected in this position type")
            }
        }
        
        // Compare network outputs across positions
        println("\n🔍 Comparing Network Outputs...")
        val boards = testPositions.map { it.first }
        val neuralAnalyzer = NeuralNetworkAnalyzer(debuggingInterface.agent)
        val comparison = neuralAnalyzer.compareNetworkOutputs(boards, ComparisonType.POLICY_SIMILARITY)
        
        println("Position Similarity Analysis:")
        comparison.similarities.forEachIndexed { index, similarity ->
            println("  Position ${index + 1}: ${(similarity * 100).toInt()}% similar to baseline")
        }
        
        if (comparison.outliers.isNotEmpty()) {
            println("Outlier Positions Detected:")
            comparison.outliers.forEach { outlierIndex ->
                println("  • Position ${outlierIndex + 1}: ${testPositions[outlierIndex].second}")
            }
        }
        
        println("\n✅ Network debugging completed!")
    }
    
    private fun demonstrateProductionMonitoring(debuggingInterface: ProductionDebuggingInterface) {
        println("\n" + "=" * 60)
        println("📊 SCENARIO 4: Production Monitoring")
        println("=" * 60)
        println("Situation: Continuous monitoring during live training")
        
        val session = debuggingInterface.startDebuggingSession("Production Monitoring")
        
        println("\n🔄 Simulating Continuous Training Monitoring...")
        
        val monitoringResults = mutableListOf<MonitoringResult>()
        
        // Simulate 20 training episodes with monitoring
        repeat(20) { episode ->
            // Generate training data for this episode
            val episodeExperiences = generateEpisodeExperiences(episode)
            val episodeMetrics = generateEpisodeMetrics(episode)
            
            // Perform monitoring checks
            val bufferAnalysis = debuggingInterface.inspectExperienceBuffer(
                episodeExperiences, AnalysisDepth.BASIC
            )
            
            val qualityScore = bufferAnalysis.basicAnalysis.qualityScore
            val hasIssues = bufferAnalysis.basicAnalysis.qualityIssues.isNotEmpty()
            
            // Check for critical issues
            val criticalIssues = mutableListOf<String>()
            
            if (episodeMetrics.gradientNorm > 10.0) {
                criticalIssues.add("Exploding gradients")
            }
            
            if (episodeMetrics.policyEntropy < 0.1) {
                criticalIssues.add("Policy collapse")
            }
            
            if (qualityScore < 0.4) {
                criticalIssues.add("Poor data quality")
            }
            
            val result = MonitoringResult(
                episode = episode,
                qualityScore = qualityScore,
                hasIssues = hasIssues,
                criticalIssues = criticalIssues,
                gradientNorm = episodeMetrics.gradientNorm,
                policyEntropy = episodeMetrics.policyEntropy
            )
            
            monitoringResults.add(result)
            
            // Real-time alerting
            if (criticalIssues.isNotEmpty()) {
                println("🚨 Episode $episode ALERT:")
                criticalIssues.forEach { issue ->
                    println("  ❌ $issue")
                }
            } else if (episode % 5 == 0) {
                println("✅ Episode $episode: Quality ${(qualityScore * 100).toInt()}%, No issues")
            }
        }
        
        // Analyze monitoring trends
        println("\n📈 Monitoring Analysis:")
        val avgQuality = monitoringResults.map { it.qualityScore }.average()
        val issueRate = monitoringResults.count { it.hasIssues }.toDouble() / monitoringResults.size
        val criticalEpisodes = monitoringResults.count { it.criticalIssues.isNotEmpty() }
        
        println("Average Quality Score: ${(avgQuality * 100).toInt()}%")
        println("Issue Rate: ${(issueRate * 100).toInt()}%")
        println("Critical Episodes: $criticalEpisodes/${monitoringResults.size}")
        
        // Quality trend analysis
        val firstHalf = monitoringResults.take(10).map { it.qualityScore }.average()
        val secondHalf = monitoringResults.drop(10).map { it.qualityScore }.average()
        val trend = secondHalf - firstHalf
        
        println("Quality Trend: ${if (trend > 0.05) "Improving" else if (trend < -0.05) "Declining" else "Stable"}")
        
        // Generate monitoring report
        println("\n📋 Generating Production Monitoring Report...")
        val report = debuggingInterface.generateDebuggingReport(session)
        
        println("Monitoring Session Summary:")
        println("  • Episodes Monitored: ${monitoringResults.size}")
        println("  • Total Analyses: ${report.analysisResults.size}")
        println("  • Issues Tracked: ${report.issuesFound.size}")
        println("  • Recommendations Generated: ${report.recommendations.size}")
        
        // Export monitoring data
        val exportResult = debuggingInterface.exportDebuggingData(
            session, ExportFormat.JSON, false
        )
        
        when (exportResult) {
            is ExportResult.Success -> {
                println("  • Monitoring data exported: ${exportResult.filename}")
            }
            is ExportResult.Error -> {
                println("  • Export failed: ${exportResult.message}")
            }
        }
        
        println("\n✅ Production monitoring demonstration completed!")
    }
    
    // Helper methods for generating realistic demo data
    
    private fun generateProblematicExperiences(count: Int): List<Experience<DoubleArray, Int>> {
        return (0 until count).map { i ->
            Experience(
                state = DoubleArray(64) { 
                    // Create low-variance states (problematic)
                    0.1 + kotlin.random.Random.nextGaussian() * 0.05
                },
                action = i % 3, // Low action diversity (problematic)
                reward = if (i % 50 == 0) 1.0 else 0.0, // Sparse rewards (problematic)
                nextState = DoubleArray(64) { 0.1 + kotlin.random.Random.nextGaussian() * 0.05 },
                done = i % 100 == 99 // Very long episodes (problematic)
            )
        }
    }
    
    private fun generateDegradedMetrics(count: Int): List<RLMetrics> {
        return (0 until count).map { i ->
            RLMetrics(
                episode = i,
                averageReward = kotlin.random.Random.nextGaussian() * 0.1, // No improvement
                policyLoss = 2.0 + kotlin.random.Random.nextGaussian() * 1.5, // High, unstable loss
                policyEntropy = 0.05 + kotlin.random.Random.nextGaussian() * 0.02, // Very low entropy
                explorationRate = 0.01, // Minimal exploration
                gradientNorm = 8.0 + kotlin.random.Random.nextGaussian() * 3.0, // High gradient norm
                qValueStats = null
            )
        }
    }
    
    private fun generateRealisticGame(): List<Move> {
        return listOf(
            Move(Position(1, 4), Position(3, 4)), // e2-e4
            Move(Position(6, 4), Position(4, 4)), // e7-e5
            Move(Position(0, 6), Position(2, 5)), // Ng1-f3
            Move(Position(7, 1), Position(5, 2)), // Nb8-c6
            Move(Position(0, 5), Position(3, 2)), // Bf1-c4
            Move(Position(7, 5), Position(4, 2)), // Bf8-c5
            Move(Position(1, 3), Position(3, 3)), // d2-d3
            Move(Position(6, 3), Position(4, 3)), // d7-d6
            Move(Position(0, 2), Position(2, 4)), // Bc1-e3
            Move(Position(7, 2), Position(5, 4)), // Bc8-e6
            Move(Position(3, 2), Position(5, 4)), // Bxe6 (capture)
            Move(Position(6, 5), Position(5, 4)), // fxe6
            Move(Position(2, 5), Position(4, 4)), // Nf3xe5 (questionable move)
            Move(Position(5, 2), Position(4, 4)), // Nxe5
            Move(Position(1, 5), Position(2, 5)), // f2-f3 (weakening move)
            Move(Position(7, 3), Position(3, 7)), // Qd8-h4+ (check)
            Move(Position(0, 4), Position(1, 5)), // Ke1-f2 (forced)
            Move(Position(3, 7), Position(2, 6)), // Qh4-g3+ (continuing attack)
        )
    }
    
    private fun createMiddlegamePosition(): ChessBoard {
        val board = ChessBoard()
        board.loadFromFEN("r1bq1rk1/ppp2ppp/2n1bn2/2bpp3/2B1P3/3P1N2/PPP2PPP/RNBQ1RK1 w - - 0 7")
        return board
    }
    
    private fun createEndgamePosition(): ChessBoard {
        val board = ChessBoard()
        board.loadFromFEN("8/8/3k4/8/3K4/8/8/8 w - - 0 1")
        return board
    }
    
    private fun createTacticalPosition(): ChessBoard {
        val board = ChessBoard()
        board.loadFromFEN("r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4")
        return board
    }
    
    private fun generateEpisodeExperiences(episode: Int): List<Experience<DoubleArray, Int>> {
        val quality = 0.3 + (episode.toDouble() / 20.0) * 0.6 // Quality improves over time
        
        return (0 until 50).map { i ->
            Experience(
                state = DoubleArray(64) { 
                    kotlin.random.Random.nextGaussian() * quality
                },
                action = kotlin.random.Random.nextInt((quality * 20).toInt() + 1),
                reward = if (kotlin.random.Random.nextDouble() < quality * 0.3) {
                    kotlin.random.Random.nextDouble(-1.0, 1.0)
                } else 0.0,
                nextState = DoubleArray(64) { kotlin.random.Random.nextGaussian() * quality },
                done = i == 49
            )
        }
    }
    
    private fun generateEpisodeMetrics(episode: Int): RLMetrics {
        val progress = episode.toDouble() / 20.0
        
        return RLMetrics(
            episode = episode,
            averageReward = -0.5 + progress * 1.0 + kotlin.random.Random.nextGaussian() * 0.2,
            policyLoss = 3.0 * (1.0 - progress * 0.7) + kotlin.random.Random.nextGaussian() * 0.3,
            policyEntropy = 2.0 * (1.0 - progress * 0.5) + kotlin.random.Random.nextGaussian() * 0.2,
            explorationRate = 1.0 - progress * 0.8,
            gradientNorm = 2.0 + kotlin.random.Random.nextGaussian() * (3.0 * (1.0 - progress)),
            qValueStats = null
        )
    }
}

/**
 * Demo implementations that provide realistic behavior
 */
class DemoChessAgent : ChessAgent {
    override fun selectAction(state: DoubleArray, validActions: List<Int>): Int {
        // Simulate realistic action selection with some randomness
        val weights = validActions.map { kotlin.random.Random.nextDouble() }
        val maxWeight = weights.maxOrNull() ?: 0.0
        val bestActions = validActions.zip(weights).filter { it.second >= maxWeight * 0.8 }
        return bestActions.randomOrNull()?.first ?: validActions.first()
    }
    
    override fun getActionProbabilities(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        // Generate realistic probability distribution with some concentration
        val rawProbs = validActions.associateWith { 
            kotlin.random.Random.nextDouble().pow(2.0) // Concentrate probability
        }
        val sum = rawProbs.values.sum()
        return rawProbs.mapValues { it.value / sum }
    }
    
    override fun getQValues(state: DoubleArray, validActions: List<Int>): Map<Int, Double> {
        // Generate realistic Q-values with some structure
        return validActions.associateWith { action ->
            val baseValue = kotlin.random.Random.nextGaussian() * 0.5
            val actionBonus = if (action % 10 == 0) 0.2 else 0.0 // Some actions are better
            baseValue + actionBonus
        }
    }
    
    override fun updatePolicy(experiences: List<Experience<DoubleArray, Int>>): PolicyUpdateResult {
        val experienceQuality = experiences.map { it.reward }.average()
        
        return PolicyUpdateResult(
            loss = maxOf(0.1, 2.0 - experienceQuality * 0.5 + kotlin.random.Random.nextGaussian() * 0.3),
            gradientNorm = maxOf(0.1, 1.5 + kotlin.random.Random.nextGaussian() * 1.0),
            policyEntropy = maxOf(0.1, 2.0 + kotlin.random.Random.nextGaussian() * 0.5),
            qValueMean = kotlin.random.Random.nextGaussian() * 0.3,
            targetValueMean = kotlin.random.Random.nextGaussian() * 0.3
        )
    }
    
    fun getPolicyOutput(state: DoubleArray): DoubleArray {
        return DoubleArray(4096) { i ->
            if (i < 100) {
                kotlin.random.Random.nextDouble() * 0.1 // Valid moves get higher probability
            } else {
                kotlin.random.Random.nextDouble() * 0.001 // Invalid moves get low probability
            }
        }
    }
    
    fun getValueOutput(state: DoubleArray): Double {
        // Generate realistic value based on state
        val stateSum = state.sum()
        return kotlin.random.Random.nextGaussian() * 0.3 + stateSum * 0.01
    }
}

class DemoChessEnvironment : ChessEnvironment {
    override fun reset(): DoubleArray {
        return DoubleArray(64) { kotlin.random.Random.nextGaussian() * 0.1 }
    }
    
    override fun step(action: Int): StepResult {
        return StepResult(
            nextState = DoubleArray(64) { kotlin.random.Random.nextGaussian() * 0.2 },
            reward = when {
                action < 10 -> kotlin.random.Random.nextDouble(0.0, 1.0) // Good actions
                action < 50 -> kotlin.random.Random.nextDouble(-0.2, 0.2) // Neutral actions
                else -> kotlin.random.Random.nextDouble(-1.0, 0.0) // Bad actions
            },
            done = kotlin.random.Random.nextDouble() < 0.05, // 5% chance of episode end
            info = emptyMap()
        )
    }
    
    override fun getValidActions(state: DoubleArray): List<Int> {
        // Return realistic number of valid actions
        return (0..kotlin.random.Random.nextInt(20, 100)).toList()
    }
    
    override fun getCurrentBoard(): ChessBoard {
        return ChessBoard()
    }
    
    override fun getGameStatus(): GameStatus {
        return GameStatus.IN_PROGRESS
    }
    
    override fun getPositionEvaluation(color: PieceColor): Double {
        return kotlin.random.Random.nextGaussian() * 0.2
    }
    
    override fun loadFromFEN(fen: String) {
        // Mock implementation
    }
}

data class MonitoringResult(
    val episode: Int,
    val qualityScore: Double,
    val hasIssues: Boolean,
    val criticalIssues: List<String>,
    val gradientNorm: Double,
    val policyEntropy: Double
)

// Main function to run the demo
fun main() {
    val demo = ProductionDebuggingDemo()
    demo.runCompleteDemo()
}