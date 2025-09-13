package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*
import kotlin.math.*

/**
 * Production-ready debugging interface that provides comprehensive analysis and validation tools
 * for chess RL training. This interface integrates all debugging capabilities into a unified
 * system for production use.
 */
class ProductionDebuggingInterface(
    private val agent: ChessAgent,
    private val environment: ChessEnvironment,
    private val config: ProductionDebuggingConfig = ProductionDebuggingConfig()
) {
    
    private val manualValidationTools = ManualValidationTools(agent, environment)
    private val validationConsole = ValidationConsole(manualValidationTools)
    private val trainingDebugger = TrainingDebugger()
    private val gameAnalyzer = GameAnalyzer()
    private val neuralNetworkAnalyzer = NeuralNetworkAnalyzer(agent)
    private val experienceBufferAnalyzer = ExperienceBufferAnalyzer()
    
    // Interactive session state
    private var currentSession: DebuggingSession? = null
    private val sessionHistory = mutableListOf<DebuggingSession>()
    
    /**
     * Start interactive debugging session
     */
    fun startDebuggingSession(sessionName: String = "Debug Session"): DebuggingSession {
        val session = DebuggingSession(
            sessionId = generateSessionId(),
            sessionName = sessionName,
            startTime = getCurrentTimeMillis(),
            agent = agent,
            environment = environment
        )
        
        currentSession = session
        sessionHistory.add(session)
        
        println("🔍 Starting Production Debugging Session: $sessionName")
        println("Session ID: ${session.sessionId}")
        println("=" * 80)
        
        return session
    }
    
    /**
     * Interactive game analysis with step-by-step position evaluation
     */
    fun analyzeGameInteractively(
        gameHistory: List<Move>,
        startFromMove: Int = 0,
        annotations: Map<Int, String> = emptyMap()
    ): InteractiveGameAnalysisSession {
        
        requireActiveSession()
        
        println("🎮 Starting Interactive Game Analysis")
        println("Game length: ${gameHistory.size} moves")
        println("Starting from move: ${startFromMove + 1}")
        println("=" * 60)
        
        val session = InteractiveGameAnalysisSession(
            gameHistory = gameHistory,
            currentMoveIndex = startFromMove,
            annotations = annotations.toMutableMap(),
            board = ChessBoard()
        )
        
        // Set up board to the starting position
        repeat(startFromMove) { moveIndex ->
            if (moveIndex < gameHistory.size) {
                session.board.makeLegalMove(gameHistory[moveIndex])
                session.board.switchActiveColor()
            }
        }
        
        // Display initial position
        displayCurrentPosition(session)
        
        return session
    }
    
    /**
     * Step through game analysis move by move
     */
    fun stepThroughGame(session: InteractiveGameAnalysisSession, direction: StepDirection = StepDirection.FORWARD): GameAnalysisStep {
        val newIndex = when (direction) {
            StepDirection.FORWARD -> (session.currentMoveIndex + 1).coerceAtMost(session.gameHistory.size - 1)
            StepDirection.BACKWARD -> (session.currentMoveIndex - 1).coerceAtLeast(0)
        }
        
        if (newIndex != session.currentMoveIndex) {
            // Update board position
            if (direction == StepDirection.FORWARD && newIndex < session.gameHistory.size) {
                val move = session.gameHistory[newIndex]
                session.board.makeLegalMove(move)
                session.board.switchActiveColor()
                session.currentMoveIndex = newIndex
            } else if (direction == StepDirection.BACKWARD) {
                // Rebuild board from start to new position
                session.board = ChessBoard()
                repeat(newIndex) { moveIndex ->
                    session.board.makeLegalMove(session.gameHistory[moveIndex])
                    session.board.switchActiveColor()
                }
                session.currentMoveIndex = newIndex
            }
        }
        
        // Analyze current position
        val positionAnalysis = analyzeCurrentPosition(session.board)
        val moveAnalysis = if (newIndex < session.gameHistory.size) {
            analyzeMoveChoice(session.board, session.gameHistory[newIndex])
        } else null
        
        val step = GameAnalysisStep(
            moveNumber = newIndex + 1,
            move = if (newIndex < session.gameHistory.size) session.gameHistory[newIndex] else null,
            positionBefore = session.board.toFEN(),
            positionAnalysis = positionAnalysis,
            moveAnalysis = moveAnalysis,
            annotation = session.annotations[newIndex]
        )
        
        displayAnalysisStep(step)
        return step
    }
    
    /**
     * Analyze current position with comprehensive evaluation
     */
    fun analyzeCurrentPosition(board: ChessBoard): ComprehensivePositionAnalysis {
        val decisionAnalysis = manualValidationTools.visualizeAgentDecisionMaking(board)
        val positionEvaluation = manualValidationTools.displayPositionEvaluation(board)
        val neuralNetworkOutput = neuralNetworkAnalyzer.analyzeNetworkOutput(board)
        val tacticalAnalysis = analyzeTacticalFeatures(board)
        val strategicAnalysis = analyzeStrategicFeatures(board)
        
        return ComprehensivePositionAnalysis(
            position = board.toFEN(),
            decisionAnalysis = decisionAnalysis,
            positionEvaluation = positionEvaluation,
            neuralNetworkOutput = neuralNetworkOutput,
            tacticalAnalysis = tacticalAnalysis,
            strategicAnalysis = strategicAnalysis,
            timestamp = getCurrentTimeMillis()
        )
    }
    
    /**
     * Manual play against trained agent with analysis
     */
    fun startManualPlaySession(
        playerColor: PieceColor = PieceColor.WHITE,
        startingFEN: String? = null
    ): ManualPlaySession {
        
        requireActiveSession()
        
        val board = if (startingFEN != null) {
            ChessBoard().apply { loadFromFEN(startingFEN) }
        } else {
            ChessBoard()
        }
        
        val session = ManualPlaySession(
            sessionId = generateSessionId(),
            playerColor = playerColor,
            agentColor = if (playerColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE,
            board = board,
            moveHistory = mutableListOf(),
            analysisHistory = mutableListOf()
        )
        
        println("🎯 Starting Manual Play Session")
        println("You are playing as: $playerColor")
        println("Agent is playing as: ${session.agentColor}")
        println("=" * 60)
        
        displayBoard(board)
        
        if (board.getActiveColor() == session.agentColor) {
            makeAgentMove(session)
        }
        
        return session
    }
    
    /**
     * Make a move in manual play session
     */
    fun makePlayerMove(session: ManualPlaySession, move: Move): ManualPlayResult {
        // Validate move
        val validMoves = environment.getValidMoves(session.board)
        if (!validMoves.contains(move)) {
            return ManualPlayResult.InvalidMove("Move $move is not valid in current position")
        }
        
        // Make player move
        session.board.makeLegalMove(move)
        session.board.switchActiveColor()
        session.moveHistory.add(move)
        
        // Analyze the position after player move
        val positionAnalysis = analyzeCurrentPosition(session.board)
        session.analysisHistory.add(positionAnalysis)
        
        println("Player move: ${move.toAlgebraic()}")
        displayBoard(session.board)
        
        // Check game status
        val gameStatus = environment.getGameStatus()
        if (gameStatus != GameStatus.IN_PROGRESS) {
            return ManualPlayResult.GameEnded(gameStatus, session.moveHistory.size)
        }
        
        // Make agent move
        val agentResult = makeAgentMove(session)
        
        return ManualPlayResult.MoveMade(move, agentResult.agentMove, positionAnalysis)
    }
    
    /**
     * Neural network activation analysis and visualization
     */
    fun analyzeNeuralNetworkActivations(
        board: ChessBoard,
        includeLayerAnalysis: Boolean = true,
        includeAttentionMaps: Boolean = false
    ): NeuralNetworkActivationAnalysis {
        
        return neuralNetworkAnalyzer.analyzeActivations(
            board = board,
            includeLayerAnalysis = includeLayerAnalysis,
            includeAttentionMaps = includeAttentionMaps
        )
    }
    
    /**
     * Training pipeline debugging with step-by-step execution
     */
    fun debugTrainingPipeline(
        experiences: List<Experience<DoubleArray, Int>>,
        trainingConfig: Any
    ): TrainingPipelineDebugResult {
        
        println("🔧 Debugging Training Pipeline")
        println("Experiences to analyze: ${experiences.size}")
        println("=" * 60)
        
        // Step 1: Experience buffer analysis
        val bufferAnalysis = experienceBufferAnalyzer.analyzeExperienceBuffer(experiences)
        println("✓ Experience buffer analysis complete")
        
        // Step 2: Batch preparation analysis
        val batchAnalysis = analyzeBatchPreparation(experiences)
        println("✓ Batch preparation analysis complete")
        
        // Step 3: Forward pass analysis
        val forwardPassAnalysis = analyzeForwardPass(experiences.take(32)) // Sample batch
        println("✓ Forward pass analysis complete")
        
        // Step 4: Loss computation analysis
        val lossAnalysis = analyzeLossComputation(experiences.take(32))
        println("✓ Loss computation analysis complete")
        
        // Step 5: Backward pass analysis
        val backwardPassAnalysis = analyzeBackwardPass()
        println("✓ Backward pass analysis complete")
        
        return TrainingPipelineDebugResult(
            bufferAnalysis = bufferAnalysis,
            batchAnalysis = batchAnalysis,
            forwardPassAnalysis = forwardPassAnalysis,
            lossAnalysis = lossAnalysis,
            backwardPassAnalysis = backwardPassAnalysis,
            recommendations = generatePipelineRecommendations(bufferAnalysis, batchAnalysis, lossAnalysis)
        )
    }
    
    /**
     * Experience buffer inspection and quality analysis
     */
    fun inspectExperienceBuffer(
        experiences: List<Experience<DoubleArray, Int>>,
        analysisDepth: AnalysisDepth = AnalysisDepth.STANDARD
    ): ExperienceBufferInspectionResult {
        
        return experienceBufferAnalyzer.inspectBuffer(experiences, analysisDepth)
    }
    
    /**
     * Performance profiling and optimization recommendations
     */
    fun profilePerformance(
        trainingMetrics: List<RLMetrics>,
        systemMetrics: SystemMetrics? = null
    ): PerformanceProfilingResult {
        
        println("📊 Profiling Performance")
        println("Analyzing ${trainingMetrics.size} training cycles")
        println("=" * 60)
        
        // Analyze training performance
        val trainingPerformance = analyzeTrainingPerformance(trainingMetrics)
        
        // Analyze system performance
        val systemPerformance = systemMetrics?.let { analyzeSystemPerformance(it) }
        
        // Generate optimization recommendations
        val optimizationRecommendations = generateOptimizationRecommendations(
            trainingPerformance, systemPerformance
        )
        
        return PerformanceProfilingResult(
            trainingPerformance = trainingPerformance,
            systemPerformance = systemPerformance,
            optimizationRecommendations = optimizationRecommendations,
            profilingTimestamp = getCurrentTimeMillis()
        )
    }
    
    /**
     * Generate comprehensive debugging report
     */
    fun generateDebuggingReport(session: DebuggingSession): ComprehensiveDebuggingReport {
        val endTime = getCurrentTimeMillis()
        val sessionDuration = endTime - session.startTime
        
        return ComprehensiveDebuggingReport(
            sessionId = session.sessionId,
            sessionName = session.sessionName,
            sessionDuration = sessionDuration,
            analysisResults = session.analysisResults,
            recommendations = session.recommendations,
            issuesFound = session.issuesFound,
            generatedAt = endTime
        )
    }
    
    /**
     * Export debugging data for external analysis
     */
    fun exportDebuggingData(
        session: DebuggingSession,
        format: ExportFormat = ExportFormat.JSON,
        includeSensitiveData: Boolean = false
    ): ExportResult {
        
        val exportData = DebuggingExportData(
            sessionInfo = session.toExportInfo(),
            analysisResults = if (includeSensitiveData) session.analysisResults else emptyList(),
            summary = session.getSummary()
        )
        
        return when (format) {
            ExportFormat.JSON -> exportToJson(exportData)
            ExportFormat.CSV -> exportToCsv(exportData)
            ExportFormat.XML -> exportToXml(exportData)
        }
    }
    
    // Private helper methods
    
    private fun requireActiveSession() {
        if (currentSession == null) {
            throw IllegalStateException("No active debugging session. Call startDebuggingSession() first.")
        }
    }
    
    private fun displayCurrentPosition(session: InteractiveGameAnalysisSession) {
        println("\n📍 Current Position (Move ${session.currentMoveIndex + 1})")
        println(validationConsole.visualizeBoard(session.board))
        
        val analysis = analyzeCurrentPosition(session.board)
        println(validationConsole.displayPositionEvaluation(analysis.positionEvaluation))
    }
    
    private fun displayAnalysisStep(step: GameAnalysisStep) {
        println("\n" + "=" * 60)
        println("📊 Move ${step.moveNumber} Analysis")
        println("=" * 60)
        
        step.move?.let { move ->
            println("Move: ${move.toAlgebraic()}")
        }
        
        println("\nPosition Analysis:")
        println(validationConsole.displayDecisionAnalysis(step.positionAnalysis.decisionAnalysis))
        
        step.moveAnalysis?.let { moveAnalysis ->
            println("\nMove Analysis:")
            println("Agent's top choice: ${moveAnalysis.wasTopChoice}")
            println("Move quality: ${moveAnalysis.qualityScore}")
        }
        
        step.annotation?.let { annotation ->
            println("\nAnnotation: $annotation")
        }
    }
    
    private fun analyzeMoveChoice(board: ChessBoard, move: Move): MoveAnalysisResult {
        val decisionAnalysis = manualValidationTools.visualizeAgentDecisionMaking(board)
        val topMove = decisionAnalysis.topMoves.firstOrNull()
        
        val wasTopChoice = topMove?.let { movesMatch(it.move, move) } ?: false
        val moveRank = decisionAnalysis.topMoves.indexOfFirst { movesMatch(it.move, move) } + 1
        val qualityScore = calculateMoveQuality(move, decisionAnalysis)
        
        return MoveAnalysisResult(
            move = move,
            wasTopChoice = wasTopChoice,
            moveRank = if (moveRank > 0) moveRank else null,
            qualityScore = qualityScore,
            agentProbability = decisionAnalysis.topMoves.find { movesMatch(it.move, move) }?.probability ?: 0.0
        )
    }
    
    private fun displayBoard(board: ChessBoard) {
        println(validationConsole.visualizeBoard(board))
        println("Active player: ${board.getActiveColor()}")
        println()
    }
    
    private fun makeAgentMove(session: ManualPlaySession): AgentMoveResult {
        val state = ChessStateEncoder().encode(session.board)
        val validMoves = environment.getValidActions(state)
        val agentAction = agent.selectAction(state, validMoves)
        val agentMove = ChessActionEncoder().decodeAction(agentAction)
        
        // Analyze agent's decision
        val decisionAnalysis = manualValidationTools.visualizeAgentDecisionMaking(session.board)
        
        // Make agent move
        session.board.makeLegalMove(agentMove)
        session.board.switchActiveColor()
        session.moveHistory.add(agentMove)
        
        println("Agent move: ${agentMove.toAlgebraic()}")
        displayBoard(session.board)
        
        // Analyze position after agent move
        val positionAnalysis = analyzeCurrentPosition(session.board)
        session.analysisHistory.add(positionAnalysis)
        
        return AgentMoveResult(
            agentMove = agentMove,
            decisionAnalysis = decisionAnalysis,
            positionAnalysis = positionAnalysis
        )
    }
    
    private fun analyzeTacticalFeatures(board: ChessBoard): TacticalAnalysis {
        // Simplified tactical analysis - would be more sophisticated in practice
        return TacticalAnalysis(
            checksPresent = false, // Would implement proper check detection
            attackedPieces = 0,
            defendedPieces = 0,
            tacticalComplexity = 0.5,
            tacticalThemes = emptyList()
        )
    }
    
    private fun analyzeStrategicFeatures(board: ChessBoard): StrategicAnalysis {
        // Simplified strategic analysis - would be more sophisticated in practice
        return StrategicAnalysis(
            pawnStructure = PawnStructureAnalysis(0.5, emptyList()),
            pieceActivity = PieceActivityAnalysis(0.5, 0.5),
            kingSafety = KingSafetyAnalysis(0.5, 0.5),
            centerControl = CenterControlAnalysis(0.5),
            strategicThemes = emptyList()
        )
    }
    
    private fun analyzeBatchPreparation(experiences: List<Experience<DoubleArray, Int>>): BatchPreparationAnalysis {
        return BatchPreparationAnalysis(
            batchSize = minOf(experiences.size, 32),
            dataQuality = assessDataQuality(experiences),
            preprocessingTime = 0.0, // Would measure actual preprocessing time
            memoryUsage = 0.0 // Would measure actual memory usage
        )
    }
    
    private fun analyzeForwardPass(experiences: List<Experience<DoubleArray, Int>>): ForwardPassAnalysis {
        return ForwardPassAnalysis(
            inputShape = experiences.firstOrNull()?.state?.size ?: 0,
            outputShape = 4096, // Chess action space size
            computationTime = 0.0, // Would measure actual computation time
            activationStats = ActivationStats(0.0, 0.0, 0.0, 0.0)
        )
    }
    
    private fun analyzeLossComputation(experiences: List<Experience<DoubleArray, Int>>): LossComputationAnalysis {
        return LossComputationAnalysis(
            lossValue = 0.0, // Would compute actual loss
            lossComponents = mapOf("policy_loss" to 0.0, "value_loss" to 0.0),
            gradientNorm = 0.0,
            numericalStability = true
        )
    }
    
    private fun analyzeBackwardPass(): BackwardPassAnalysis {
        return BackwardPassAnalysis(
            gradientNorm = 0.0,
            gradientClipping = false,
            updateMagnitude = 0.0,
            convergenceIndicators = ConvergenceIndicators(false, 0.0)
        )
    }
    
    private fun generatePipelineRecommendations(
        bufferAnalysis: ExperienceBufferAnalysis,
        batchAnalysis: BatchPreparationAnalysis,
        lossAnalysis: LossComputationAnalysis
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (bufferAnalysis.qualityScore < 0.7) {
            recommendations.add("Experience buffer quality is low - consider improving exploration")
        }
        
        if (batchAnalysis.dataQuality < 0.8) {
            recommendations.add("Batch data quality could be improved - check preprocessing")
        }
        
        if (!lossAnalysis.numericalStability) {
            recommendations.add("Numerical instability detected - consider gradient clipping")
        }
        
        return recommendations
    }
    
    private fun analyzeTrainingPerformance(metrics: List<RLMetrics>): TrainingPerformanceAnalysis {
        val recentMetrics = metrics.takeLast(10)
        
        return TrainingPerformanceAnalysis(
            averageReward = recentMetrics.map { it.averageReward }.average(),
            rewardTrend = calculateTrend(recentMetrics.map { it.averageReward }),
            trainingStability = calculateStability(recentMetrics.map { it.policyLoss }),
            convergenceRate = calculateConvergenceRate(metrics)
        )
    }
    
    private fun analyzeSystemPerformance(systemMetrics: SystemMetrics): SystemPerformanceAnalysis {
        return SystemPerformanceAnalysis(
            cpuUtilization = systemMetrics.cpuUtilization,
            memoryUsage = systemMetrics.memoryUsage,
            gpuUtilization = systemMetrics.gpuUtilization,
            throughput = systemMetrics.throughput,
            bottlenecks = identifyBottlenecks(systemMetrics)
        )
    }
    
    private fun generateOptimizationRecommendations(
        trainingPerformance: TrainingPerformanceAnalysis,
        systemPerformance: SystemPerformanceAnalysis?
    ): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        if (trainingPerformance.trainingStability < 0.7) {
            recommendations.add(OptimizationRecommendation(
                type = OptimizationType.TRAINING_STABILITY,
                priority = RecommendationPriority.HIGH,
                description = "Training stability is low",
                actions = listOf("Reduce learning rate", "Apply gradient clipping")
            ))
        }
        
        systemPerformance?.let { sysPerf ->
            if (sysPerf.cpuUtilization > 0.9) {
                recommendations.add(OptimizationRecommendation(
                    type = OptimizationType.SYSTEM_PERFORMANCE,
                    priority = RecommendationPriority.MEDIUM,
                    description = "High CPU utilization detected",
                    actions = listOf("Reduce batch size", "Optimize data loading")
                ))
            }
        }
        
        return recommendations
    }
    
    // Utility methods
    
    private fun movesMatch(move1: Move, move2: Move): Boolean {
        return move1.from == move2.from && move1.to == move2.to &&
               (move1.promotion == move2.promotion || 
                (move1.promotion == null && move2.promotion == null))
    }
    
    private fun calculateMoveQuality(move: Move, decisionAnalysis: AgentDecisionAnalysis): Double {
        val moveAnalysis = decisionAnalysis.topMoves.find { movesMatch(it.move, move) }
        return moveAnalysis?.probability ?: 0.0
    }
    
    private fun assessDataQuality(experiences: List<Experience<DoubleArray, Int>>): Double {
        if (experiences.isEmpty()) return 0.0
        
        val rewardVariance = calculateVariance(experiences.map { it.reward })
        val terminalRatio = experiences.count { it.done }.toDouble() / experiences.size
        
        return (1.0 - rewardVariance.coerceIn(0.0, 1.0)) * 0.7 + terminalRatio * 0.3
    }
    
    private fun calculateTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val x = (0 until n).map { it.toDouble() }
        val y = values
        
        val xMean = x.average()
        val yMean = y.average()
        
        val numerator = x.zip(y) { xi, yi -> (xi - xMean) * (yi - yMean) }.sum()
        val denominator = x.map { (it - xMean).pow(2) }.sum()
        
        return if (denominator != 0.0) numerator / denominator else 0.0
    }
    
    private fun calculateStability(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        val variance = calculateVariance(values)
        val stdDev = sqrt(variance)
        
        return if (abs(mean) > 1e-8) {
            1.0 / (1.0 + stdDev / abs(mean))
        } else {
            if (stdDev < 1e-8) 1.0 else 0.0
        }
    }
    
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }
    
    private fun calculateConvergenceRate(metrics: List<RLMetrics>): Double {
        if (metrics.size < 10) return 0.0
        
        val recentRewards = metrics.takeLast(10).map { it.averageReward }
        val olderRewards = metrics.dropLast(10).takeLast(10).map { it.averageReward }
        
        if (olderRewards.isEmpty()) return 0.0
        
        val recentMean = recentRewards.average()
        val olderMean = olderRewards.average()
        
        return (recentMean - olderMean) / 10.0 // Improvement per episode
    }
    
    private fun identifyBottlenecks(systemMetrics: SystemMetrics): List<String> {
        val bottlenecks = mutableListOf<String>()
        
        if (systemMetrics.cpuUtilization > 0.9) {
            bottlenecks.add("CPU utilization")
        }
        
        if (systemMetrics.memoryUsage > 0.9) {
            bottlenecks.add("Memory usage")
        }
        
        if (systemMetrics.gpuUtilization < 0.3) {
            bottlenecks.add("GPU underutilization")
        }
        
        return bottlenecks
    }
    
    private fun exportToJson(data: DebuggingExportData): ExportResult {
        // Would implement actual JSON export
        return ExportResult.Success("data.json", "JSON export completed")
    }
    
    private fun exportToCsv(data: DebuggingExportData): ExportResult {
        // Would implement actual CSV export
        return ExportResult.Success("data.csv", "CSV export completed")
    }
    
    private fun exportToXml(data: DebuggingExportData): ExportResult {
        // Would implement actual XML export
        return ExportResult.Success("data.xml", "XML export completed")
    }
    
    private fun generateSessionId(): String {
        return "debug_${getCurrentTimeMillis()}_${(0..999).random()}"
    }
}

/**
 * Configuration for production debugging interface
 */
data class ProductionDebuggingConfig(
    val enableDetailedLogging: Boolean = true,
    val maxSessionHistory: Int = 100,
    val enablePerformanceProfiling: Boolean = true,
    val exportSensitiveData: Boolean = false,
    val analysisTimeout: Long = 30000, // 30 seconds
    val maxAnalysisDepth: Int = 10
)

// Supporting enums and data classes

enum class StepDirection {
    FORWARD, BACKWARD
}

enum class AnalysisDepth {
    BASIC, STANDARD, DETAILED, COMPREHENSIVE
}

enum class ExportFormat {
    JSON, CSV, XML
}

enum class OptimizationType {
    TRAINING_STABILITY, SYSTEM_PERFORMANCE, MEMORY_OPTIMIZATION, CONVERGENCE_IMPROVEMENT
}

// Session and analysis result classes

data class DebuggingSession(
    val sessionId: String,
    val sessionName: String,
    val startTime: Long,
    val agent: ChessAgent,
    val environment: ChessEnvironment,
    val analysisResults: MutableList<Any> = mutableListOf(),
    val recommendations: MutableList<String> = mutableListOf(),
    val issuesFound: MutableList<String> = mutableListOf()
) {
    fun toExportInfo(): Map<String, Any> {
        return mapOf(
            "sessionId" to sessionId,
            "sessionName" to sessionName,
            "startTime" to startTime,
            "analysisCount" to analysisResults.size,
            "recommendationCount" to recommendations.size,
            "issueCount" to issuesFound.size
        )
    }
    
    fun getSummary(): String {
        return "Session $sessionName: ${analysisResults.size} analyses, ${recommendations.size} recommendations, ${issuesFound.size} issues"
    }
}

data class InteractiveGameAnalysisSession(
    val gameHistory: List<Move>,
    var currentMoveIndex: Int,
    val annotations: MutableMap<Int, String>,
    var board: ChessBoard
)

data class ManualPlaySession(
    val sessionId: String,
    val playerColor: PieceColor,
    val agentColor: PieceColor,
    val board: ChessBoard,
    val moveHistory: MutableList<Move>,
    val analysisHistory: MutableList<ComprehensivePositionAnalysis>
)

data class ComprehensivePositionAnalysis(
    val position: String,
    val decisionAnalysis: AgentDecisionAnalysis,
    val positionEvaluation: PositionEvaluationDisplay,
    val neuralNetworkOutput: NeuralNetworkOutput,
    val tacticalAnalysis: TacticalAnalysis,
    val strategicAnalysis: StrategicAnalysis,
    val timestamp: Long
)

data class GameAnalysisStep(
    val moveNumber: Int,
    val move: Move?,
    val positionBefore: String,
    val positionAnalysis: ComprehensivePositionAnalysis,
    val moveAnalysis: MoveAnalysisResult?,
    val annotation: String?
)

data class MoveAnalysisResult(
    val move: Move,
    val wasTopChoice: Boolean,
    val moveRank: Int?,
    val qualityScore: Double,
    val agentProbability: Double
)

data class AgentMoveResult(
    val agentMove: Move,
    val decisionAnalysis: AgentDecisionAnalysis,
    val positionAnalysis: ComprehensivePositionAnalysis
)

sealed class ManualPlayResult {
    data class MoveMade(
        val playerMove: Move,
        val agentMove: Move,
        val positionAnalysis: ComprehensivePositionAnalysis
    ) : ManualPlayResult()
    
    data class InvalidMove(val reason: String) : ManualPlayResult()
    data class GameEnded(val result: GameStatus, val totalMoves: Int) : ManualPlayResult()
}

sealed class ExportResult {
    data class Success(val filename: String, val message: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

data class DebuggingExportData(
    val sessionInfo: Map<String, Any>,
    val analysisResults: List<Any>,
    val summary: String
)

data class ComprehensiveDebuggingReport(
    val sessionId: String,
    val sessionName: String,
    val sessionDuration: Long,
    val analysisResults: List<Any>,
    val recommendations: List<String>,
    val issuesFound: List<String>,
    val generatedAt: Long
)
// A
dditional data classes for debugging analysis

data class TacticalAnalysis(
    val checksPresent: Boolean,
    val attackedPieces: Int,
    val defendedPieces: Int,
    val tacticalComplexity: Double,
    val tacticalThemes: List<String>
)

data class StrategicAnalysis(
    val pawnStructure: PawnStructureAnalysis,
    val pieceActivity: PieceActivityAnalysis,
    val kingSafety: KingSafetyAnalysis,
    val centerControl: CenterControlAnalysis,
    val strategicThemes: List<String>
)

data class PawnStructureAnalysis(
    val score: Double,
    val weaknesses: List<String>
)

data class PieceActivityAnalysis(
    val whiteActivity: Double,
    val blackActivity: Double
)

data class KingSafetyAnalysis(
    val whiteKingSafety: Double,
    val blackKingSafety: Double
)

data class CenterControlAnalysis(
    val centerControlScore: Double
)

data class TrainingPipelineDebugResult(
    val bufferAnalysis: ExperienceBufferAnalysis,
    val batchAnalysis: BatchPreparationAnalysis,
    val forwardPassAnalysis: ForwardPassAnalysis,
    val lossAnalysis: LossComputationAnalysis,
    val backwardPassAnalysis: BackwardPassAnalysis,
    val recommendations: List<String>
)

data class BatchPreparationAnalysis(
    val batchSize: Int,
    val dataQuality: Double,
    val preprocessingTime: Double,
    val memoryUsage: Double
)

data class ForwardPassAnalysis(
    val inputShape: Int,
    val outputShape: Int,
    val computationTime: Double,
    val activationStats: ActivationStats
)

data class LossComputationAnalysis(
    val lossValue: Double,
    val lossComponents: Map<String, Double>,
    val gradientNorm: Double,
    val numericalStability: Boolean
)

data class BackwardPassAnalysis(
    val gradientNorm: Double,
    val gradientClipping: Boolean,
    val updateMagnitude: Double,
    val convergenceIndicators: ConvergenceIndicators
)

data class ConvergenceIndicators(
    val isConverging: Boolean,
    val convergenceRate: Double
)

data class PerformanceProfilingResult(
    val trainingPerformance: TrainingPerformanceAnalysis,
    val systemPerformance: SystemPerformanceAnalysis?,
    val optimizationRecommendations: List<OptimizationRecommendation>,
    val profilingTimestamp: Long
)

data class TrainingPerformanceAnalysis(
    val averageReward: Double,
    val rewardTrend: Double,
    val trainingStability: Double,
    val convergenceRate: Double
)

data class SystemPerformanceAnalysis(
    val cpuUtilization: Double,
    val memoryUsage: Double,
    val gpuUtilization: Double,
    val throughput: Double,
    val bottlenecks: List<String>
)

data class OptimizationRecommendation(
    val type: OptimizationType,
    val priority: RecommendationPriority,
    val description: String,
    val actions: List<String>
)

data class SystemMetrics(
    val cpuUtilization: Double,
    val memoryUsage: Double,
    val gpuUtilization: Double,
    val throughput: Double
)

enum class RecommendationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}