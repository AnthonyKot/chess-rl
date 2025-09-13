# Chess RL Debugging Tools - Usage Examples

This document provides practical examples of using the debugging tools for common scenarios in chess RL training.

## Table of Contents

1. [Quick Start Examples](#quick-start-examples)
2. [Training Issue Diagnosis](#training-issue-diagnosis)
3. [Game Analysis Examples](#game-analysis-examples)
4. [Network Debugging Examples](#network-debugging-examples)
5. [Performance Optimization Examples](#performance-optimization-examples)
6. [Production Monitoring Examples](#production-monitoring-examples)

## Quick Start Examples

### Basic Debugging Session

```kotlin
import com.chessrl.integration.*

fun basicDebuggingExample() {
    // Setup
    val agent = MyChessAgent()
    val environment = MyChessEnvironment()
    val debuggingInterface = ProductionDebuggingInterface(agent, environment)
    
    // Start session
    val session = debuggingInterface.startDebuggingSession("Basic Analysis")
    
    // Analyze current position
    val board = ChessBoard()
    val analysis = debuggingInterface.analyzeCurrentPosition(board)
    
    println("=== Position Analysis ===")
    println("Agent's top moves:")
    analysis.decisionAnalysis.topMoves.take(3).forEach { move ->
        println("${move.rank}. ${move.move.toAlgebraic()} (${(move.probability * 100).toInt()}%)")
    }
    
    println("\nPosition evaluation: ${analysis.positionEvaluation.netEvaluation}")
    println("Agent confidence: ${(analysis.decisionAnalysis.decisionConfidence * 100).toInt()}%")
    
    // Generate report
    val report = debuggingInterface.generateDebuggingReport(session)
    println("\nSession completed with ${report.analysisResults.size} analyses")
}
```

### Quick Buffer Quality Check

```kotlin
fun quickBufferCheck(experiences: List<Experience<DoubleArray, Int>>) {
    val analyzer = ExperienceBufferAnalyzer()
    val analysis = analyzer.analyzeExperienceBuffer(experiences)
    
    println("=== Buffer Quality Report ===")
    println("Buffer size: ${analysis.bufferSize}")
    println("Quality score: ${(analysis.qualityScore * 100).toInt()}%")
    
    if (analysis.qualityScore < 0.7) {
        println("\n⚠️ Quality Issues:")
        analysis.qualityIssues.forEach { issue ->
            println("  • $issue")
        }
        
        println("\n💡 Recommendations:")
        analysis.recommendations.take(3).forEach { rec ->
            println("  • $rec")
        }
    } else {
        println("✅ Buffer quality is good!")
    }
}
```

## Training Issue Diagnosis

### Diagnosing Poor Performance

```kotlin
fun diagnosePoorPerformance(
    agent: ChessAgent,
    environment: ChessEnvironment,
    recentExperiences: List<Experience<DoubleArray, Int>>,
    trainingMetrics: List<RLMetrics>
) {
    val debuggingInterface = ProductionDebuggingInterface(agent, environment)
    val session = debuggingInterface.startDebuggingSession("Performance Diagnosis")
    
    println("🔍 Diagnosing Poor Performance...")
    
    // 1. Check experience buffer quality
    println("\n1. Analyzing Experience Buffer...")
    val bufferAnalysis = debuggingInterface.inspectExperienceBuffer(
        recentExperiences, AnalysisDepth.DETAILED
    )
    
    val qualityScore = bufferAnalysis.basicAnalysis.qualityScore
    println("Buffer quality: ${(qualityScore * 100).toInt()}%")
    
    if (qualityScore < 0.5) {
        println("❌ Poor buffer quality detected!")
        bufferAnalysis.basicAnalysis.qualityIssues.forEach { issue ->
            println("  Issue: $issue")
        }
    }
    
    // 2. Check network behavior
    println("\n2. Analyzing Network Behavior...")
    val board = ChessBoard()
    val networkAnalysis = debuggingInterface.analyzeNeuralNetworkActivations(board)
    
    if (networkAnalysis.networkIssues.isNotEmpty()) {
        println("❌ Network issues detected!")
        networkAnalysis.networkIssues.forEach { issue ->
            println("  ${issue.type} in ${issue.layer}: ${issue.description}")
        }
    }
    
    // 3. Check training stability
    println("\n3. Analyzing Training Stability...")
    val performanceResult = debuggingInterface.profilePerformance(trainingMetrics)
    val stability = performanceResult.trainingPerformance.trainingStability
    
    println("Training stability: ${(stability * 100).toInt()}%")
    if (stability < 0.7) {
        println("❌ Training instability detected!")
    }
    
    // 4. Generate comprehensive recommendations
    println("\n4. Generating Recommendations...")
    val allRecommendations = mutableListOf<String>()
    allRecommendations.addAll(bufferAnalysis.basicAnalysis.recommendations)
    allRecommendations.addAll(performanceResult.optimizationRecommendations.map { it.description })
    
    println("📋 Action Items:")
    allRecommendations.distinct().take(5).forEach { rec ->
        println("  • $rec")
    }
    
    // Generate final report
    val report = debuggingInterface.generateDebuggingReport(session)
    println("\n📊 Diagnosis complete. Report generated with ${report.recommendations.size} recommendations.")
}
```

### Investigating Training Stagnation

```kotlin
fun investigateTrainingStagnation(
    trainingMetrics: List<RLMetrics>,
    recentExperiences: List<Experience<DoubleArray, Int>>
) {
    val analyzer = ExperienceBufferAnalyzer()
    
    println("🔍 Investigating Training Stagnation...")
    
    // 1. Analyze reward trends
    val rewards = trainingMetrics.map { it.averageReward }
    val recentRewards = rewards.takeLast(20)
    val rewardVariance = calculateVariance(recentRewards)
    
    println("\n1. Reward Analysis:")
    println("Recent reward variance: ${String.format("%.4f", rewardVariance)}")
    
    if (rewardVariance < 0.001) {
        println("❌ Rewards have stagnated!")
        println("💡 Consider:")
        println("  • Increasing exploration rate")
        println("  • Implementing curriculum learning")
        println("  • Adjusting reward function")
    }
    
    // 2. Check experience diversity
    println("\n2. Experience Diversity Analysis:")
    val bufferAnalysis = analyzer.analyzeExperienceBuffer(recentExperiences)
    val actionDiversity = bufferAnalysis.actionAnalysis.diversity
    val stateDiversity = bufferAnalysis.diversityAnalysis.stateDiversity
    
    println("Action diversity: ${(actionDiversity * 100).toInt()}%")
    println("State diversity: ${String.format("%.3f", stateDiversity)}")
    
    if (actionDiversity < 0.3) {
        println("❌ Low action diversity - agent may be stuck in local optimum")
    }
    
    // 3. Analyze quality over time
    println("\n3. Quality Trend Analysis:")
    val timeSeriesAnalysis = analyzer.analyzeQualityOverTime(recentExperiences, 500)
    
    when (timeSeriesAnalysis.overallTrend) {
        TrendDirection.DECLINING -> {
            println("❌ Quality is declining over time!")
            println("💡 Check for:")
            println("  • Catastrophic forgetting")
            println("  • Learning rate too high")
            println("  • Experience buffer corruption")
        }
        TrendDirection.STABLE -> {
            println("⚠️ Quality has plateaued")
            println("💡 Consider:")
            println("  • Increasing model capacity")
            println("  • Adding new training scenarios")
        }
        TrendDirection.IMPROVING -> {
            println("✅ Quality is still improving - patience may be needed")
        }
    }
}

private fun calculateVariance(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val mean = values.average()
    return values.map { (it - mean) * (it - mean) }.average()
}
```

## Game Analysis Examples

### Analyzing a Specific Game

```kotlin
fun analyzeSpecificGame(gameHistory: List<Move>) {
    val agent = MyChessAgent()
    val environment = MyChessEnvironment()
    val debuggingInterface = ProductionDebuggingInterface(agent, environment)
    
    val session = debuggingInterface.startDebuggingSession("Game Analysis")
    
    println("🎮 Analyzing Game (${gameHistory.size} moves)")
    
    // Start interactive analysis
    val analysisSession = debuggingInterface.analyzeGameInteractively(
        gameHistory = gameHistory,
        annotations = mapOf(
            0 to "Opening move",
            10 to "Entering middlegame",
            gameHistory.size - 5 to "Endgame begins"
        )
    )
    
    // Analyze key positions
    val keyMoves = listOf(0, 10, 20, gameHistory.size / 2, gameHistory.size - 5)
        .filter { it < gameHistory.size }
    
    keyMoves.forEach { moveIndex ->
        // Navigate to position
        while (analysisSession.currentMoveIndex < moveIndex) {
            debuggingInterface.stepThroughGame(analysisSession, StepDirection.FORWARD)
        }
        while (analysisSession.currentMoveIndex > moveIndex) {
            debuggingInterface.stepThroughGame(analysisSession, StepDirection.BACKWARD)
        }
        
        val step = debuggingInterface.stepThroughGame(analysisSession, StepDirection.FORWARD)
        
        println("\n=== Move ${step.moveNumber} Analysis ===")
        step.move?.let { move ->
            println("Move played: ${move.toAlgebraic()}")
        }
        
        val analysis = step.positionAnalysis
        println("Agent confidence: ${(analysis.decisionAnalysis.decisionConfidence * 100).toInt()}%")
        println("Position evaluation: ${String.format("%.2f", analysis.positionEvaluation.netEvaluation)}")
        
        // Check if move was agent's top choice
        step.moveAnalysis?.let { moveAnalysis ->
            if (moveAnalysis.wasTopChoice) {
                println("✅ Agent's top choice")
            } else {
                println("❌ Not agent's top choice (rank: ${moveAnalysis.moveRank})")
                println("Agent probability: ${(moveAnalysis.agentProbability * 100).toInt()}%")
            }
        }
        
        // Show top alternatives
        println("Top alternatives:")
        analysis.decisionAnalysis.topMoves.take(3).forEach { topMove ->
            println("  ${topMove.rank}. ${topMove.move.toAlgebraic()} (${(topMove.probability * 100).toInt()}%)")
        }
    }
    
    // Generate game summary
    println("\n📊 Game Summary:")
    val totalMoves = gameHistory.size
    println("Total moves analyzed: $totalMoves")
    
    // Calculate average confidence across the game
    // (This would require storing all analyses - simplified here)
    println("Analysis complete!")
}
```

### Comparing Agent vs Human Games

```kotlin
fun compareAgentVsHumanGames(
    agentGames: List<List<Move>>,
    humanGames: List<List<Move>>
) {
    val agent = MyChessAgent()
    val environment = MyChessEnvironment()
    val debuggingInterface = ProductionDebuggingInterface(agent, environment)
    
    println("🆚 Comparing Agent vs Human Games")
    
    // Analyze sample games from each category
    val agentSample = agentGames.take(5)
    val humanSample = humanGames.take(5)
    
    val agentAnalyses = agentSample.map { game ->
        analyzeGameCharacteristics(debuggingInterface, game, "Agent")
    }
    
    val humanAnalyses = humanSample.map { game ->
        analyzeGameCharacteristics(debuggingInterface, game, "Human")
    }
    
    // Compare characteristics
    println("\n📊 Comparison Results:")
    
    val agentAvgLength = agentAnalyses.map { it.gameLength }.average()
    val humanAvgLength = humanAnalyses.map { it.gameLength }.average()
    
    println("Average game length:")
    println("  Agent: ${agentAvgLength.toInt()} moves")
    println("  Human: ${humanAvgLength.toInt()} moves")
    
    val agentAvgConfidence = agentAnalyses.map { it.averageConfidence }.average()
    val humanAvgConfidence = humanAnalyses.map { it.averageConfidence }.average()
    
    println("Average decision confidence:")
    println("  Agent: ${(agentAvgConfidence * 100).toInt()}%")
    println("  Human: ${(humanAvgConfidence * 100).toInt()}%")
    
    // Identify patterns
    println("\n🔍 Pattern Analysis:")
    if (agentAvgLength < humanAvgLength * 0.8) {
        println("❌ Agent games are significantly shorter - may indicate tactical oversights")
    }
    
    if (agentAvgConfidence > humanAvgConfidence * 1.2) {
        println("⚠️ Agent shows overconfidence compared to human baseline")
    }
}

data class GameCharacteristics(
    val gameLength: Int,
    val averageConfidence: Double,
    val gameType: String
)

fun analyzeGameCharacteristics(
    debuggingInterface: ProductionDebuggingInterface,
    game: List<Move>,
    gameType: String
): GameCharacteristics {
    val session = debuggingInterface.startDebuggingSession("$gameType Game Analysis")
    val analysisSession = debuggingInterface.analyzeGameInteractively(game)
    
    // Sample positions throughout the game
    val samplePositions = 5
    val confidences = mutableListOf<Double>()
    
    repeat(samplePositions) { i ->
        val targetMove = (game.size * i) / samplePositions
        
        // Navigate to position
        while (analysisSession.currentMoveIndex < targetMove && analysisSession.currentMoveIndex < game.size - 1) {
            debuggingInterface.stepThroughGame(analysisSession, StepDirection.FORWARD)
        }
        
        val step = debuggingInterface.stepThroughGame(analysisSession, StepDirection.FORWARD)
        confidences.add(step.positionAnalysis.decisionAnalysis.decisionConfidence)
    }
    
    return GameCharacteristics(
        gameLength = game.size,
        averageConfidence = confidences.average(),
        gameType = gameType
    )
}
```

## Network Debugging Examples

### Debugging Network Confidence Issues

```kotlin
fun debugNetworkConfidence(agent: ChessAgent) {
    val analyzer = NeuralNetworkAnalyzer(agent)
    
    println("🧠 Debugging Network Confidence Issues")
    
    // Test various positions
    val testPositions = listOf(
        ChessBoard(), // Starting position
        createMiddlegamePosition(),
        createEndgamePosition(),
        createTacticalPosition()
    )
    
    testPositions.forEachIndexed { index, board ->
        println("\n=== Position ${index + 1} Analysis ===")
        
        val confidenceAnalysis = analyzer.analyzeNetworkConfidence(board)
        
        println("Overall confidence: ${(confidenceAnalysis.overallConfidence * 100).toInt()}%")
        println("Confidence level: ${confidenceAnalysis.confidenceLevel}")
        println("Policy entropy: ${String.format("%.3f", confidenceAnalysis.policyEntropy)}")
        
        // Analyze uncertainty sources
        if (confidenceAnalysis.uncertaintySources.isNotEmpty()) {
            println("Uncertainty sources:")
            confidenceAnalysis.uncertaintySources.forEach { source ->
                println("  • ${source.type}: ${source.description}")
                println("    Severity: ${(source.severity * 100).toInt()}%")
            }
        }
        
        // Provide recommendations based on confidence level
        when (confidenceAnalysis.confidenceLevel) {
            ConfidenceLevel.VERY_LOW -> {
                println("❌ Very low confidence detected!")
                println("💡 Recommendations:")
                println("  • Increase training data for similar positions")
                println("  • Check if position is within training distribution")
                println("  • Consider ensemble methods")
            }
            ConfidenceLevel.LOW -> {
                println("⚠️ Low confidence - monitor closely")
                println("💡 Consider additional training on similar positions")
            }
            ConfidenceLevel.MEDIUM -> {
                println("✅ Acceptable confidence level")
            }
            ConfidenceLevel.HIGH -> {
                println("✅ High confidence - good network certainty")
            }
        }
    }
}

fun createMiddlegamePosition(): ChessBoard {
    val board = ChessBoard()
    // Set up a typical middlegame position
    board.loadFromFEN("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 4 4")
    return board
}

fun createEndgamePosition(): ChessBoard {
    val board = ChessBoard()
    // Set up a typical endgame position
    board.loadFromFEN("8/8/8/8/8/3k4/3P4/3K4 w - - 0 1")
    return board
}

fun createTacticalPosition(): ChessBoard {
    val board = ChessBoard()
    // Set up a position with tactical opportunities
    board.loadFromFEN("r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4")
    return board
}
```

### Analyzing Network Activation Patterns

```kotlin
fun analyzeActivationPatterns(agent: ChessAgent) {
    val analyzer = NeuralNetworkAnalyzer(agent)
    
    println("🔬 Analyzing Network Activation Patterns")
    
    val testBoard = ChessBoard()
    val activationAnalysis = analyzer.analyzeActivations(
        board = testBoard,
        includeLayerAnalysis = true,
        includeAttentionMaps = true
    )
    
    println("\n=== Layer Analysis ===")
    activationAnalysis.layerActivations.forEach { (layerName, analysis) ->
        println("Layer: $layerName")
        println("  Sparsity: ${(analysis.sparsity * 100).toInt()}%")
        println("  Saturation: ${(analysis.saturation * 100).toInt()}%")
        println("  Mean activation: ${String.format("%.4f", analysis.activationStats.mean)}")
        println("  Activation variance: ${String.format("%.4f", analysis.activationStats.variance)}")
        
        // Flag potential issues
        if (analysis.sparsity > 0.9) {
            println("  ❌ High sparsity - many dead neurons!")
        }
        if (analysis.saturation > 0.5) {
            println("  ❌ High saturation - neurons may be saturated!")
        }
        if (analysis.activationStats.variance < 1e-6) {
            println("  ❌ Very low variance - potential vanishing gradients!")
        }
    }
    
    println("\n=== Activation Health Summary ===")
    val patterns = activationAnalysis.activationPatterns
    println("Dead neurons: ${patterns.deadNeurons}")
    println("Saturated neurons: ${patterns.saturatedNeurons}")
    println("Overall health: ${patterns.activationHealth}")
    
    when (patterns.activationHealth) {
        ActivationHealth.POOR -> {
            println("❌ Poor activation health!")
            println("💡 Immediate actions needed:")
            println("  • Reduce learning rate")
            println("  • Apply gradient clipping")
            println("  • Check network initialization")
        }
        ActivationHealth.CONCERNING -> {
            println("⚠️ Concerning activation patterns")
            println("💡 Monitor closely and consider:")
            println("  • Adjusting learning rate")
            println("  • Reviewing network architecture")
        }
        ActivationHealth.HEALTHY -> {
            println("✅ Healthy activation patterns")
        }
    }
    
    // Network issues
    if (activationAnalysis.networkIssues.isNotEmpty()) {
        println("\n=== Network Issues ===")
        activationAnalysis.networkIssues.forEach { issue ->
            println("${issue.type} in ${issue.layer}:")
            println("  Description: ${issue.description}")
            println("  Severity: ${(issue.severity * 100).toInt()}%")
            
            // Provide specific recommendations
            when (issue.type) {
                NetworkIssueType.DEAD_NEURONS -> {
                    println("  💡 Try: Lower learning rate, check ReLU usage")
                }
                NetworkIssueType.SATURATED_NEURONS -> {
                    println("  💡 Try: Gradient clipping, batch normalization")
                }
                NetworkIssueType.VANISHING_GRADIENTS -> {
                    println("  💡 Try: Skip connections, better initialization")
                }
                NetworkIssueType.EXPLODING_GRADIENTS -> {
                    println("  💡 Try: Gradient clipping, lower learning rate")
                }
            }
        }
    }
}
```

## Performance Optimization Examples

### Comprehensive Performance Analysis

```kotlin
fun comprehensivePerformanceAnalysis(
    trainingMetrics: List<RLMetrics>,
    systemMetrics: SystemMetrics?
) {
    val debuggingInterface = ProductionDebuggingInterface(MyChessAgent(), MyChessEnvironment())
    
    println("⚡ Comprehensive Performance Analysis")
    
    val profilingResult = debuggingInterface.profilePerformance(trainingMetrics, systemMetrics)
    
    // Training performance analysis
    println("\n=== Training Performance ===")
    val trainingPerf = profilingResult.trainingPerformance
    
    println("Average reward: ${String.format("%.3f", trainingPerf.averageReward)}")
    println("Reward trend: ${String.format("%.6f", trainingPerf.rewardTrend)}")
    println("Training stability: ${(trainingPerf.trainingStability * 100).toInt()}%")
    println("Convergence rate: ${String.format("%.6f", trainingPerf.convergenceRate)}")
    
    // Assess training performance
    when {
        trainingPerf.trainingStability < 0.5 -> {
            println("❌ Training is highly unstable!")
            println("💡 Critical actions:")
            println("  • Reduce learning rate immediately")
            println("  • Apply gradient clipping")
            println("  • Check for data quality issues")
        }
        trainingPerf.rewardTrend < -0.001 -> {
            println("❌ Negative reward trend - performance declining!")
            println("💡 Investigate:")
            println("  • Catastrophic forgetting")
            println("  • Learning rate too high")
            println("  • Experience replay issues")
        }
        trainingPerf.convergenceRate < 0.001 -> {
            println("⚠️ Very slow convergence")
            println("💡 Consider:")
            println("  • Increasing learning rate")
            println("  • Improving exploration")
            println("  • Curriculum learning")
        }
        else -> {
            println("✅ Training performance looks good")
        }
    }
    
    // System performance analysis
    systemMetrics?.let { _ ->
        println("\n=== System Performance ===")
        val sysPerf = profilingResult.systemPerformance!!
        
        println("CPU utilization: ${(sysPerf.cpuUtilization * 100).toInt()}%")
        println("Memory usage: ${(sysPerf.memoryUsage * 100).toInt()}%")
        println("GPU utilization: ${(sysPerf.gpuUtilization * 100).toInt()}%")
        println("Throughput: ${String.format("%.2f", sysPerf.throughput)} samples/sec")
        
        // Identify bottlenecks
        if (sysPerf.bottlenecks.isNotEmpty()) {
            println("\n🚫 Bottlenecks detected:")
            sysPerf.bottlenecks.forEach { bottleneck ->
                println("  • $bottleneck")
            }
        }
        
        // Performance recommendations
        when {
            sysPerf.cpuUtilization > 0.95 -> {
                println("❌ CPU bottleneck!")
                println("💡 Try: Reduce batch size, optimize data loading")
            }
            sysPerf.memoryUsage > 0.9 -> {
                println("❌ Memory bottleneck!")
                println("💡 Try: Reduce buffer size, enable gradient checkpointing")
            }
            sysPerf.gpuUtilization < 0.3 -> {
                println("⚠️ GPU underutilized")
                println("💡 Try: Increase batch size, check data pipeline")
            }
            else -> {
                println("✅ System resources well utilized")
            }
        }
    }
    
    // Optimization recommendations
    println("\n=== Optimization Recommendations ===")
    profilingResult.optimizationRecommendations.forEach { recommendation ->
        val priorityIcon = when (recommendation.priority) {
            RecommendationPriority.CRITICAL -> "🔴"
            RecommendationPriority.HIGH -> "🟡"
            RecommendationPriority.MEDIUM -> "🔵"
            RecommendationPriority.LOW -> "⚪"
        }
        
        println("$priorityIcon ${recommendation.priority} - ${recommendation.type}")
        println("   ${recommendation.description}")
        recommendation.actions.forEach { action ->
            println("   → $action")
        }
    }
}
```

### Optimizing Training Pipeline

```kotlin
fun optimizeTrainingPipeline(
    experiences: List<Experience<DoubleArray, Int>>,
    currentConfig: TrainingConfig
) {
    val debuggingInterface = ProductionDebuggingInterface(MyChessAgent(), MyChessEnvironment())
    
    println("🔧 Optimizing Training Pipeline")
    
    val pipelineDebug = debuggingInterface.debugTrainingPipeline(experiences, currentConfig)
    
    // Analyze each pipeline component
    println("\n=== Pipeline Component Analysis ===")
    
    // 1. Experience buffer
    val bufferAnalysis = pipelineDebug.bufferAnalysis
    println("1. Experience Buffer:")
    println("   Quality score: ${(bufferAnalysis.qualityScore * 100).toInt()}%")
    
    if (bufferAnalysis.qualityScore < 0.7) {
        println("   ❌ Buffer quality issues detected")
        bufferAnalysis.qualityIssues.take(3).forEach { issue ->
            println("   • $issue")
        }
    }
    
    // 2. Batch preparation
    val batchAnalysis = pipelineDebug.batchAnalysis
    println("\n2. Batch Preparation:")
    println("   Batch size: ${batchAnalysis.batchSize}")
    println("   Data quality: ${(batchAnalysis.dataQuality * 100).toInt()}%")
    println("   Preprocessing time: ${batchAnalysis.preprocessingTime}ms")
    
    if (batchAnalysis.preprocessingTime > 100) {
        println("   ⚠️ Slow preprocessing - consider optimization")
    }
    
    // 3. Forward pass
    val forwardAnalysis = pipelineDebug.forwardPassAnalysis
    println("\n3. Forward Pass:")
    println("   Input shape: ${forwardAnalysis.inputShape}")
    println("   Output shape: ${forwardAnalysis.outputShape}")
    println("   Computation time: ${forwardAnalysis.computationTime}ms")
    
    if (forwardAnalysis.computationTime > 50) {
        println("   ⚠️ Slow forward pass - check model complexity")
    }
    
    // 4. Loss computation
    val lossAnalysis = pipelineDebug.lossAnalysis
    println("\n4. Loss Computation:")
    println("   Loss value: ${String.format("%.6f", lossAnalysis.lossValue)}")
    println("   Gradient norm: ${String.format("%.6f", lossAnalysis.gradientNorm)}")
    println("   Numerically stable: ${lossAnalysis.numericalStability}")
    
    if (!lossAnalysis.numericalStability) {
        println("   ❌ Numerical instability detected!")
        println("   💡 Apply gradient clipping or reduce learning rate")
    }
    
    // 5. Backward pass
    val backwardAnalysis = pipelineDebug.backwardPassAnalysis
    println("\n5. Backward Pass:")
    println("   Gradient norm: ${String.format("%.6f", backwardAnalysis.gradientNorm)}")
    println("   Gradient clipping: ${backwardAnalysis.gradientClipping}")
    println("   Update magnitude: ${String.format("%.6f", backwardAnalysis.updateMagnitude)}")
    
    if (backwardAnalysis.gradientNorm > 10.0) {
        println("   ❌ Exploding gradients!")
        println("   💡 Enable gradient clipping")
    }
    
    // Generate optimization plan
    println("\n=== Optimization Plan ===")
    val optimizations = generateOptimizationPlan(pipelineDebug)
    optimizations.forEachIndexed { index, optimization ->
        println("${index + 1}. $optimization")
    }
}

data class TrainingConfig(
    val batchSize: Int,
    val learningRate: Double,
    val gradientClipping: Boolean
)

fun generateOptimizationPlan(debugResult: TrainingPipelineDebugResult): List<String> {
    val optimizations = mutableListOf<String>()
    
    // Buffer optimizations
    if (debugResult.bufferAnalysis.qualityScore < 0.7) {
        optimizations.add("Improve experience buffer quality through better exploration")
    }
    
    // Batch optimizations
    if (debugResult.batchAnalysis.preprocessingTime > 100) {
        optimizations.add("Optimize data preprocessing pipeline")
    }
    
    // Forward pass optimizations
    if (debugResult.forwardPassAnalysis.computationTime > 50) {
        optimizations.add("Consider model pruning or quantization")
    }
    
    // Loss optimizations
    if (!debugResult.lossAnalysis.numericalStability) {
        optimizations.add("Implement gradient clipping and learning rate scheduling")
    }
    
    // Backward pass optimizations
    if (debugResult.backwardPassAnalysis.gradientNorm > 10.0) {
        optimizations.add("Enable gradient clipping with threshold 1.0")
    }
    
    return optimizations
}
```

## Production Monitoring Examples

### Continuous Training Monitoring

```kotlin
class ProductionTrainingMonitor(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment
) {
    private val debuggingInterface = ProductionDebuggingInterface(agent, environment)
    private var currentSession: DebuggingSession? = null
    
    fun startMonitoring() {
        currentSession = debuggingInterface.startDebuggingSession("Production Monitoring")
        println("🔍 Production monitoring started")
    }
    
    fun monitorTrainingStep(
        episode: Int,
        experiences: List<Experience<DoubleArray, Int>>,
        metrics: RLMetrics
    ) {
        // Periodic comprehensive analysis
        if (episode % 100 == 0) {
            performPeriodicAnalysis(episode, experiences, metrics)
        }
        
        // Continuous quality monitoring
        if (episode % 10 == 0) {
            performQuickQualityCheck(episode, experiences, metrics)
        }
        
        // Alert on critical issues
        checkForCriticalIssues(episode, metrics)
    }
    
    private fun performPeriodicAnalysis(
        episode: Int,
        experiences: List<Experience<DoubleArray, Int>>,
        metrics: RLMetrics
    ) {
        println("\n📊 Periodic Analysis - Episode $episode")
        
        // Buffer analysis
        val bufferAnalysis = debuggingInterface.inspectExperienceBuffer(
            experiences, AnalysisDepth.STANDARD
        )
        
        val qualityScore = bufferAnalysis.basicAnalysis.qualityScore
        println("Buffer quality: ${(qualityScore * 100).toInt()}%")
        
        // Network confidence check
        val board = ChessBoard()
        val confidenceAnalysis = debuggingInterface.analyzeNeuralNetworkActivations(board)
        
        val hasIssues = confidenceAnalysis.networkIssues.isNotEmpty()
        if (hasIssues) {
            println("⚠️ Network issues detected:")
            confidenceAnalysis.networkIssues.take(3).forEach { issue ->
                println("  • ${issue.type} in ${issue.layer}")
            }
        }
        
        // Store analysis results
        currentSession?.analysisResults?.add(
            PeriodicAnalysisResult(episode, qualityScore, hasIssues)
        )
    }
    
    private fun performQuickQualityCheck(
        episode: Int,
        experiences: List<Experience<DoubleArray, Int>>,
        metrics: RLMetrics
    ) {
        // Quick buffer quality check
        val analyzer = ExperienceBufferAnalyzer()
        val recentExperiences = experiences.takeLast(1000)
        val analysis = analyzer.analyzeExperienceBuffer(recentExperiences)
        
        if (analysis.qualityScore < 0.5) {
            println("⚠️ Episode $episode: Low buffer quality (${(analysis.qualityScore * 100).toInt()}%)")
        }
        
        // Quick stability check
        if (metrics.gradientNorm > 10.0) {
            println("⚠️ Episode $episode: High gradient norm (${String.format("%.2f", metrics.gradientNorm)})")
        }
    }
    
    private fun checkForCriticalIssues(episode: Int, metrics: RLMetrics) {
        val criticalIssues = mutableListOf<String>()
        
        // Check for NaN values
        if (metrics.policyLoss.isNaN() || metrics.policyLoss.isInfinite()) {
            criticalIssues.add("NaN/Infinite loss detected")
        }
        
        // Check for exploding gradients
        if (metrics.gradientNorm > 100.0) {
            criticalIssues.add("Exploding gradients (norm: ${String.format("%.2f", metrics.gradientNorm)})")
        }
        
        // Check for policy collapse
        if (metrics.policyEntropy < 0.01) {
            criticalIssues.add("Policy collapse (entropy: ${String.format("%.4f", metrics.policyEntropy)})")
        }
        
        if (criticalIssues.isNotEmpty()) {
            println("🚨 CRITICAL ISSUES at episode $episode:")
            criticalIssues.forEach { issue ->
                println("  ❌ $issue")
            }
            
            // Add to session issues
            currentSession?.issuesFound?.addAll(criticalIssues)
            
            // Generate immediate recommendations
            val recommendations = generateCriticalRecommendations(criticalIssues)
            currentSession?.recommendations?.addAll(recommendations)
            
            println("💡 Immediate actions:")
            recommendations.forEach { rec ->
                println("  • $rec")
            }
        }
    }
    
    private fun generateCriticalRecommendations(issues: List<String>): List<String> {
        val recommendations = mutableListOf<String>()
        
        issues.forEach { issue ->
            when {
                issue.contains("NaN") -> {
                    recommendations.add("Stop training immediately and check data pipeline")
                    recommendations.add("Reduce learning rate and restart from checkpoint")
                }
                issue.contains("Exploding") -> {
                    recommendations.add("Enable gradient clipping with threshold 1.0")
                    recommendations.add("Reduce learning rate by factor of 10")
                }
                issue.contains("Policy collapse") -> {
                    recommendations.add("Increase exploration rate")
                    recommendations.add("Add entropy regularization")
                }
            }
        }
        
        return recommendations.distinct()
    }
    
    fun generateMonitoringReport(): String {
        val session = currentSession ?: return "No active monitoring session"
        
        val report = debuggingInterface.generateDebuggingReport(session)
        
        return buildString {
            appendLine("=== Production Monitoring Report ===")
            appendLine("Session: ${report.sessionName}")
            appendLine("Duration: ${report.sessionDuration / 1000}s")
            appendLine("Analyses: ${report.analysisResults.size}")
            appendLine("Issues found: ${report.issuesFound.size}")
            appendLine("Recommendations: ${report.recommendations.size}")
            
            if (report.issuesFound.isNotEmpty()) {
                appendLine("\nCritical Issues:")
                report.issuesFound.forEach { issue ->
                    appendLine("  • $issue")
                }
            }
            
            if (report.recommendations.isNotEmpty()) {
                appendLine("\nRecommendations:")
                report.recommendations.take(5).forEach { rec ->
                    appendLine("  • $rec")
                }
            }
        }
    }
}

data class PeriodicAnalysisResult(
    val episode: Int,
    val qualityScore: Double,
    val hasNetworkIssues: Boolean
)

// Usage example
fun runProductionMonitoring() {
    val monitor = ProductionTrainingMonitor(MyChessAgent(), MyChessEnvironment())
    monitor.startMonitoring()
    
    // Simulate training loop
    repeat(1000) { episode ->
        val experiences = generateTrainingExperiences()
        val metrics = simulateTrainingStep()
        
        monitor.monitorTrainingStep(episode, experiences, metrics)
        
        // Generate report every 500 episodes
        if (episode % 500 == 0) {
            val report = monitor.generateMonitoringReport()
            println(report)
        }
    }
}

// Helper functions for examples
fun generateTrainingExperiences(): List<Experience<DoubleArray, Int>> {
    return (0..100).map {
        Experience(
            state = DoubleArray(64) { kotlin.random.Random.nextDouble() },
            action = kotlin.random.Random.nextInt(4096),
            reward = kotlin.random.Random.nextDouble(),
            nextState = DoubleArray(64) { kotlin.random.Random.nextDouble() },
            done = kotlin.random.Random.nextBoolean()
        )
    }
}

fun simulateTrainingStep(): RLMetrics {
    return RLMetrics(
        episode = kotlin.random.Random.nextInt(1000),
        averageReward = kotlin.random.Random.nextDouble(),
        policyLoss = kotlin.random.Random.nextDouble(),
        policyEntropy = kotlin.random.Random.nextDouble(),
        explorationRate = kotlin.random.Random.nextDouble(),
        gradientNorm = kotlin.random.Random.nextDouble() * 5.0,
        qValueStats = null
    )
}
```

These examples demonstrate practical usage of the debugging tools for various scenarios in chess RL training. Each example includes error handling, interpretation of results, and actionable recommendations for improving training performance.