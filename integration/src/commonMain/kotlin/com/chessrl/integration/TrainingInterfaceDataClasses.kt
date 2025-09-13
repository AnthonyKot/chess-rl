package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*

/**
 * Data classes for the comprehensive training control and visualization interface
 */

// Configuration classes

/**
 * Configuration for the training control interface
 */
data class TrainingInterfaceConfig(
    // Agent configuration
    val agentType: AgentType = AgentType.DQN,
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    val learningRate: Double = 0.001,
    val explorationRate: Double = 0.1,
    val temperature: Double = 1.0,
    
    // Training configuration
    val maxStepsPerEpisode: Int = 200,
    val batchSize: Int = 64,
    val maxBufferSize: Int = 50000,
    
    // Self-play configuration
    val gamesPerIteration: Int = 20,
    val maxConcurrentGames: Int = 4,
    
    // Interface configuration
    val displayUpdateInterval: Int = 5,
    val convergenceAnalysisWindow: Int = 20,
    val convergenceStabilityThreshold: Double = 0.8,
    
    // Visualization configuration
    val topMovesToShow: Int = 5,
    val topInvalidMovesToShow: Int = 3,
    val maxMovesToDisplay: Int = 10,
    val maxMovesToAnalyze: Int = 20,
    
    // Dashboard configuration
    val dashboardUpdateInterval: Long = 5000L,
    val enableRealTimeUpdates: Boolean = true,
    val enablePerformanceMonitoring: Boolean = true
)

/**
 * Configuration for a training session
 */
data class TrainingSessionConfig(
    val trainingType: TrainingType,
    val episodes: Int = 100,
    val iterations: Int? = null, // For self-play training
    val maxDuration: Long? = null, // Maximum duration in milliseconds
    val enableMonitoring: Boolean = true,
    val enableValidation: Boolean = true,
    val checkpointInterval: Int = 100,
    val validationInterval: Int = 50,
    val experimentName: String? = null,
    val description: String? = null
)

// Enums

/**
 * Types of training sessions
 */
enum class TrainingType {
    BASIC,          // Basic RL training
    SELF_PLAY,      // Self-play training
    VALIDATION,     // Validation and testing
    EXPERIMENT      // Experimental training
}

/**
 * Types of analysis
 */
enum class AnalysisType {
    GAME,           // Analyze complete game
    POSITION,       // Analyze single position
    AGENT_DECISION  // Analyze agent decision-making
}

/**
 * Types of validation
 */
enum class ValidationType {
    SCENARIOS,      // Validate against predefined scenarios
    GAME_QUALITY    // Validate game quality
}

// VisualizationType and ReportType are now defined in SharedDataClasses.kt

/**
 * Time control for human vs agent games
 */
enum class TimeControl {
    Unlimited,
    Blitz,      // 5 minutes
    Rapid,      // 15 minutes
    Classical   // 60 minutes
}

// Result classes

/**
 * Result of interface initialization
 */
sealed class InterfaceInitializationResult {
    data class Success(
        val initializationTime: Long,
        val componentResults: List<ComponentInitResult>,
        val interfaceVersion: String
    ) : InterfaceInitializationResult()
    
    data class Failed(
        val error: String,
        val componentResults: List<ComponentInitResult>
    ) : InterfaceInitializationResult()
    
    data class AlreadyInitialized(
        val message: String
    ) : InterfaceInitializationResult()
}

/**
 * Result of component initialization
 */
data class ComponentInitResult(
    val componentName: String,
    val success: Boolean,
    val error: String?
)

/**
 * Result of training session start
 */
sealed class TrainingSessionResult {
    data class Success(
        val sessionId: String,
        val trainingResults: BasicTrainingResult,
        val monitoringSession: MonitoringSession
    ) : TrainingSessionResult()
    
    data class Failed(
        val error: String
    ) : TrainingSessionResult()
}

/**
 * Basic training result wrapper
 */
sealed class BasicTrainingResult {
    data class Success(
        val results: TrainingResults
    ) : BasicTrainingResult()
    
    data class Failed(
        val error: String
    ) : BasicTrainingResult()
}

/**
 * Result of command execution
 */
sealed class CommandExecutionResult {
    data class Success(
        val result: Any,
        val executionTime: Long,
        val timestamp: Long
    ) : CommandExecutionResult()
    
    data class Failed(
        val error: String,
        val command: String,
        val timestamp: Long
    ) : CommandExecutionResult()
}

/**
 * Result of game analysis
 */
sealed class GameAnalysisResult {
    data class Success(
        val gameHistory: List<Move>,
        val interactiveAnalysis: InteractiveGameAnalysis,
        val gameQuality: GameQualityAssessment,
        val detailedAnalysis: List<PositionAnalysis>?,
        val consoleOutput: String,
        val qualityOutput: String,
        val analysisTime: Long
    ) : GameAnalysisResult()
    
    data class Failed(
        val error: String
    ) : GameAnalysisResult()
}

/**
 * Result of human move in agent vs human session
 */
sealed class HumanMoveResult {
    data class Success(
        val humanMove: Move,
        val agentMove: Move,
        val positionAfterHuman: String,
        val positionAfterAgent: String
    ) : HumanMoveResult()
    
    data class GameEnded(
        val move: Move,
        val gameStatus: GameStatus,
        val finalPosition: String
    ) : HumanMoveResult()
    
    data class Failed(
        val error: String
    ) : HumanMoveResult()
}

/**
 * Result of training report generation
 */
sealed class TrainingReportResult {
    data class Success(
        val report: Any, // Would be specific report type
        val reportType: ReportType,
        val generationTime: Long,
        val timestamp: Long
    ) : TrainingReportResult()
    
    data class Failed(
        val error: String
    ) : TrainingReportResult()
}

/**
 * Result of configuration update
 */
sealed class ConfigurationUpdateResult {
    data class Success(
        val previousConfig: TrainingInterfaceConfig,
        val newConfig: TrainingInterfaceConfig,
        val rollbackAvailable: Boolean,
        val validationPerformed: Boolean
    ) : ConfigurationUpdateResult()
    
    data class Failed(
        val error: String
    ) : ConfigurationUpdateResult()
}

/**
 * Result of configuration rollback
 */
sealed class ConfigurationRollbackResult {
    data class Success(
        val rolledBackFrom: TrainingInterfaceConfig,
        val rolledBackTo: TrainingInterfaceConfig,
        val rollbackTimestamp: Long
    ) : ConfigurationRollbackResult()
    
    data class Failed(
        val error: String
    ) : ConfigurationRollbackResult()
}

/**
 * Result of interface shutdown
 */
sealed class ShutdownResult {
    data class Success(
        val shutdownTime: Long,
        val componentResults: List<ComponentShutdownResult>
    ) : ShutdownResult()
    
    data class Failed(
        val error: String,
        val componentResults: List<ComponentShutdownResult>
    ) : ShutdownResult()
}

/**
 * Result of component shutdown
 */
data class ComponentShutdownResult(
    val componentName: String,
    val success: Boolean,
    val error: String?
)

// Session and state classes

/**
 * Active training session
 */
data class TrainingSession(
    val sessionId: String,
    val config: TrainingSessionConfig,
    val startTime: Long,
    var isActive: Boolean
)

/**
 * Agent vs human play session
 */
data class AgentVsHumanSession(
    val sessionId: String,
    val humanColor: PieceColor,
    val timeControl: TimeControl,
    val startTime: Long,
    val environment: ChessEnvironment,
    val moveHistory: MutableList<Move>,
    var isActive: Boolean
)

/**
 * Training experiment record
 */
data class TrainingExperiment(
    val experimentId: String,
    val name: String,
    val description: String,
    val config: TrainingInterfaceConfig,
    val startTime: Long,
    val endTime: Long?,
    val results: Any?, // Would be specific result type
    val status: ExperimentStatus
)

/**
 * Status of training experiment
 */
enum class ExperimentStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Configuration snapshot for rollback
 */
data class ConfigurationSnapshot(
    val config: TrainingInterfaceConfig,
    val timestamp: Long,
    val sessionId: String
)

/**
 * Configuration validation result
 */
data class ConfigurationValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

// Command classes

/**
 * Training commands that can be executed
 */
sealed class TrainingCommand {
    abstract val type: CommandType
    
    data class Start(
        val config: TrainingSessionConfig,
        override val type: CommandType = CommandType.START
    ) : TrainingCommand()
    
    data class Pause(
        override val type: CommandType = CommandType.PAUSE
    ) : TrainingCommand()
    
    data class Resume(
        override val type: CommandType = CommandType.RESUME
    ) : TrainingCommand()
    
    data class Stop(
        override val type: CommandType = CommandType.STOP
    ) : TrainingCommand()
    
    data class Restart(
        val config: TrainingSessionConfig,
        override val type: CommandType = CommandType.RESTART
    ) : TrainingCommand()
    
    data class Configure(
        val newConfig: TrainingInterfaceConfig,
        val validateFirst: Boolean = true,
        override val type: CommandType = CommandType.CONFIGURE
    ) : TrainingCommand()
    
    data class Analyze(
        val analysisType: AnalysisType,
        val gameHistory: List<Move>? = null,
        val position: String? = null,
        override val type: CommandType = CommandType.ANALYZE
    ) : TrainingCommand()
    
    data class Validate(
        val validationType: ValidationType,
        val scenarios: List<TrainingScenario>? = null,
        val games: List<List<Move>>? = null,
        override val type: CommandType = CommandType.VALIDATE
    ) : TrainingCommand()
    
    data class Export(
        val format: String,
        val path: String,
        val includeRawData: Boolean = false,
        override val type: CommandType = CommandType.EXPORT
    ) : TrainingCommand()
    
    data class Visualize(
        val visualizationType: VisualizationType,
        val board: ChessBoard? = null,
        override val type: CommandType = CommandType.VISUALIZE
    ) : TrainingCommand()
    
    data class PlayAgainstAgent(
        val humanColor: PieceColor,
        val timeControl: TimeControl = TimeControl.Unlimited,
        override val type: CommandType = CommandType.PLAY_AGAINST_AGENT
    ) : TrainingCommand()
    
    data class GenerateReport(
        val reportType: ReportType = ReportType.COMPREHENSIVE,
        override val type: CommandType = CommandType.GENERATE_REPORT
    ) : TrainingCommand()
}

/**
 * Types of commands
 */
enum class CommandType {
    START,
    PAUSE,
    RESUME,
    STOP,
    RESTART,
    CONFIGURE,
    ANALYZE,
    VALIDATE,
    EXPORT,
    VISUALIZE,
    PLAY_AGAINST_AGENT,
    GENERATE_REPORT
}

// Analysis classes

/**
 * Position analysis result for training interface
 */
data class TrainingPositionAnalysis(
    val moveNumber: Int,
    val positionFEN: String,
    val decisionAnalysis: AgentDecisionAnalysis,
    val positionEvaluation: PositionEvaluationDisplay
)

/**
 * Position analysis result for commands
 */
data class PositionAnalysisResult(
    val position: String,
    val decisionAnalysis: AgentDecisionAnalysis,
    val positionEvaluation: PositionEvaluationDisplay,
    val consoleOutput: String
)

// Dashboard enhancement classes

// TrainingDashboard and InterfaceInfo are now defined in SharedDataClasses.kt

// Utility classes for chess operations

/**
 * Chess state encoder for converting board positions to neural network input
 */
class ChessStateEncoder {
    companion object {
        const val TOTAL_FEATURES = 776 // 8x8x12 (pieces) + 5 (castling/en passant) + 1 (turn)
    }
    
    fun encode(board: ChessBoard): DoubleArray {
        // Simplified encoding - would implement full 776-feature encoding
        // This should be replaced with actual chess position encoding
        return DoubleArray(TOTAL_FEATURES) { 0.0 }
    }
}

/**
 * Chess action encoder for converting between moves and action indices
 */
class ChessActionEncoder {
    companion object {
        const val ACTION_SPACE_SIZE = 4096
    }
    
    fun encodeMove(move: Move): Int {
        // Simplified encoding - would implement full move encoding
        return move.from.rank * 64 + move.from.file * 8 + move.to.rank + move.to.file
    }
    
    fun decodeAction(actionIndex: Int): Move {
        // Simplified decoding - would implement full action decoding
        val fromRank = actionIndex / 64
        val remainder = actionIndex % 64
        val fromFile = remainder / 8
        val toRank = remainder % 8
        val toFile = actionIndex % 8
        
        return Move(
            from = Position(fromRank.coerceIn(0, 7), fromFile.coerceIn(0, 7)),
            to = Position(toRank.coerceIn(0, 7), toFile.coerceIn(0, 7))
        )
    }
}

/**
 * Chess agent factory for creating agents with different configurations
 */
object ChessAgentFactory {
    fun createDQNAgent(
        hiddenLayers: List<Int>,
        learningRate: Double,
        explorationRate: Double,
        config: ChessAgentConfig? = null
    ): ChessAgent {
        // Create real DQN agent using actual neural networks
        val realAgent = RealChessAgentFactory.createRealDQNAgent(
            inputSize = ChessStateEncoder.TOTAL_FEATURES,
            outputSize = ChessActionEncoder.ACTION_SPACE_SIZE,
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            explorationRate = explorationRate,
            batchSize = config?.batchSize ?: 32,
            maxBufferSize = config?.maxBufferSize ?: 10000
        )
        
        return RealChessAgentAdapter(realAgent)
    }
    
    fun createPolicyGradientAgent(
        hiddenLayers: List<Int>,
        learningRate: Double,
        temperature: Double,
        config: ChessAgentConfig? = null
    ): ChessAgent {
        // Create real Policy Gradient agent using actual neural networks
        val realAgent = RealChessAgentFactory.createRealPolicyGradientAgent(
            inputSize = ChessStateEncoder.TOTAL_FEATURES,
            outputSize = ChessActionEncoder.ACTION_SPACE_SIZE,
            hiddenLayers = hiddenLayers,
            learningRate = learningRate,
            temperature = temperature,
            batchSize = config?.batchSize ?: 32
        )
        
        return RealChessAgentAdapter(realAgent)
    }
}

/**
 * Chess agent configuration
 */
data class ChessAgentConfig(
    val batchSize: Int = 64,
    val maxBufferSize: Int = 50000,
    val targetUpdateFrequency: Int = 100,
    val enableDoubleQ: Boolean = true
)

// Extension functions for chess operations

/**
 * Convert move to algebraic notation
 */
fun Move.toAlgebraic(): String {
    val files = "abcdefgh"
    val ranks = "12345678"
    
    val fromSquare = "${files[from.file]}${ranks[from.rank]}"
    val toSquare = "${files[to.file]}${ranks[to.rank]}"
    
    return if (promotion != null) {
        "$fromSquare$toSquare=${promotion.toString().lowercase().first()}"
    } else {
        "$fromSquare$toSquare"
    }
}

/**
 * Load board from FEN string
 */
fun ChessBoard.loadFromFEN(fen: String) {
    // Simplified FEN loading - would implement full FEN parsing
}

/**
 * Get current state as encoded array
 */
fun ChessEnvironment.getCurrentState(): DoubleArray {
    return ChessStateEncoder().encode(getCurrentBoard())
}

/**
 * Make a move and return result
 */
fun ChessEnvironment.makeMove(move: Move): MoveResult {
    // Simplified move execution - would implement full move validation and execution
    return MoveResult(success = true, error = null)
}

/**
 * Move execution result
 */
data class MoveResult(
    val success: Boolean,
    val error: String?
)