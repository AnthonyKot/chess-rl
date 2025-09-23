package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.integration.output.EnhancedEvaluationResults
import com.chessrl.integration.output.EnhancedComparisonResults
import com.chessrl.integration.output.ColorAlternationStats
import com.chessrl.integration.output.StatisticalUtils
import com.chessrl.teacher.MinimaxTeacher
import com.chessrl.chess.*
import kotlin.random.Random

/**
 * Evaluator for testing agents against baseline opponents.
 * 
 * Provides consistent evaluation against heuristic and minimax opponents
 * with proper statistical reporting.
 */
class BaselineEvaluator(private val config: ChessRLConfig) {
    
    private val logger = ChessRLLogger.forComponent("BaselineEvaluator")
    private val actionEncoder = ChessActionEncoder()
    
    /**
     * Evaluate agent against heuristic baseline.
     */
    fun evaluateAgainstHeuristic(agent: ChessAgent, games: Int): EnhancedEvaluationResults {
        logger.info("Evaluating agent against heuristic baseline over $games games")
        
        val environment = createEvaluationEnvironment()
        var wins = 0
        var draws = 0
        var losses = 0
        var totalLength = 0
        
        // Track color-specific results
        var whiteGames = 0
        var blackGames = 0
        var whiteWins = 0
        var blackWins = 0
        var whiteDraws = 0
        var blackDraws = 0
        var whiteLosses = 0
        var blackLosses = 0
        
        repeat(games) { gameIndex ->
            val agentIsWhite = gameIndex % 2 == 0 // Alternate colors
            val result = playGameAgainstHeuristic(environment, agent, agentIsWhite)
            
            if (agentIsWhite) {
                whiteGames++
                when (result.outcome) {
                    GameOutcome.WHITE_WINS -> { wins++; whiteWins++ }
                    GameOutcome.BLACK_WINS -> { losses++; whiteLosses++ }
                    else -> { draws++; whiteDraws++ }
                }
            } else {
                blackGames++
                when (result.outcome) {
                    GameOutcome.WHITE_WINS -> { losses++; blackLosses++ }
                    GameOutcome.BLACK_WINS -> { wins++; blackWins++ }
                    else -> { draws++; blackDraws++ }
                }
            }
            
            totalLength += result.gameLength
        }
        
        val winRate = wins.toDouble() / games
        val drawRate = draws.toDouble() / games
        val lossRate = losses.toDouble() / games
        val avgLength = totalLength.toDouble() / games
        
        // Calculate statistical measures
        val confidenceInterval = StatisticalUtils.calculateWinRateConfidenceInterval(wins, games)
        val isSignificant = StatisticalUtils.testStatisticalSignificance(wins, games)
        
        val colorStats = ColorAlternationStats(
            whiteGames = whiteGames,
            blackGames = blackGames,
            whiteWins = whiteWins,
            blackWins = blackWins,
            whiteDraws = whiteDraws,
            blackDraws = blackDraws,
            whiteLosses = whiteLosses,
            blackLosses = blackLosses
        )
        
        logger.info("Heuristic evaluation complete: ${String.format("%.1f%%", winRate * 100)} win rate over $games games")
        if (confidenceInterval != null) {
            logger.info("95% confidence interval: ${StatisticalUtils.formatConfidenceInterval(confidenceInterval)}")
        }
        if (isSignificant) {
            logger.info("Result is statistically significant (p < 0.05)")
        }
        
        return EnhancedEvaluationResults(
            totalGames = games,
            wins = wins,
            draws = draws,
            losses = losses,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageGameLength = avgLength,
            confidenceInterval = confidenceInterval,
            statisticalSignificance = isSignificant,
            colorAlternation = colorStats,
            opponentType = "Heuristic"
        )
    }
    
    /**
     * Evaluate agent against minimax baseline.
     */
    fun evaluateAgainstMinimax(agent: ChessAgent, games: Int, depth: Int = 2): EnhancedEvaluationResults {
        logger.info("Evaluating agent against minimax depth-$depth over $games games")
        
        val environment = createEvaluationEnvironment()
        val random = config.seed?.let { Random(it) } ?: Random.Default
        val teacher = MinimaxTeacher(depth = depth, random = random)
        
        var wins = 0
        var draws = 0
        var losses = 0
        var totalLength = 0
        
        // Track color-specific results
        var whiteGames = 0
        var blackGames = 0
        var whiteWins = 0
        var blackWins = 0
        var whiteDraws = 0
        var blackDraws = 0
        var whiteLosses = 0
        var blackLosses = 0
        
        repeat(games) { gameIndex ->
            val agentIsWhite = gameIndex % 2 == 0 // Alternate colors
            val result = playGameAgainstMinimax(environment, agent, teacher, agentIsWhite)
            
            if (agentIsWhite) {
                whiteGames++
                when (result.outcome) {
                    GameOutcome.WHITE_WINS -> { wins++; whiteWins++ }
                    GameOutcome.BLACK_WINS -> { losses++; whiteLosses++ }
                    else -> { draws++; whiteDraws++ }
                }
            } else {
                blackGames++
                when (result.outcome) {
                    GameOutcome.WHITE_WINS -> { losses++; blackLosses++ }
                    GameOutcome.BLACK_WINS -> { wins++; blackWins++ }
                    else -> { draws++; blackDraws++ }
                }
            }
            
            totalLength += result.gameLength
        }
        
        val winRate = wins.toDouble() / games
        val drawRate = draws.toDouble() / games
        val lossRate = losses.toDouble() / games
        val avgLength = totalLength.toDouble() / games
        
        // Calculate statistical measures
        val confidenceInterval = StatisticalUtils.calculateWinRateConfidenceInterval(wins, games)
        val isSignificant = StatisticalUtils.testStatisticalSignificance(wins, games)
        
        val colorStats = ColorAlternationStats(
            whiteGames = whiteGames,
            blackGames = blackGames,
            whiteWins = whiteWins,
            blackWins = blackWins,
            whiteDraws = whiteDraws,
            blackDraws = blackDraws,
            whiteLosses = whiteLosses,
            blackLosses = blackLosses
        )
        
        logger.info("Minimax evaluation complete: ${String.format("%.1f%%", winRate * 100)} win rate over $games games")
        if (confidenceInterval != null) {
            logger.info("95% confidence interval: ${StatisticalUtils.formatConfidenceInterval(confidenceInterval)}")
        }
        if (isSignificant) {
            logger.info("Result is statistically significant (p < 0.05)")
        }
        
        return EnhancedEvaluationResults(
            totalGames = games,
            wins = wins,
            draws = draws,
            losses = losses,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageGameLength = avgLength,
            confidenceInterval = confidenceInterval,
            statisticalSignificance = isSignificant,
            colorAlternation = colorStats,
            opponentType = "Minimax-$depth"
        )
    }
    
    /**
     * Compare two models head-to-head.
     */
    fun compareModels(agentA: ChessAgent, agentB: ChessAgent, games: Int): EnhancedComparisonResults {
        logger.info("Comparing two models over $games games")
        
        val environment = createEvaluationEnvironment()
        var aWins = 0
        var draws = 0
        var bWins = 0
        var totalLength = 0
        
        // Track color-specific results for agent A
        var aWhiteGames = 0
        var aBlackGames = 0
        var aWhiteWins = 0
        var aBlackWins = 0
        var aWhiteDraws = 0
        var aBlackDraws = 0
        var aWhiteLosses = 0
        var aBlackLosses = 0
        
        repeat(games) { gameIndex ->
            val aIsWhite = gameIndex % 2 == 0 // Alternate colors
            val result = playGameBetweenAgents(environment, agentA, agentB, aIsWhite)
            
            if (aIsWhite) {
                aWhiteGames++
                when (result.outcome) {
                    GameOutcome.WHITE_WINS -> { aWins++; aWhiteWins++ }
                    GameOutcome.BLACK_WINS -> { bWins++; aWhiteLosses++ }
                    else -> { draws++; aWhiteDraws++ }
                }
            } else {
                aBlackGames++
                when (result.outcome) {
                    GameOutcome.WHITE_WINS -> { bWins++; aBlackLosses++ }
                    GameOutcome.BLACK_WINS -> { aWins++; aBlackWins++ }
                    else -> { draws++; aBlackDraws++ }
                }
            }
            
            totalLength += result.gameLength
        }
        
        val aWinRate = aWins.toDouble() / games
        val drawRate = draws.toDouble() / games
        val bWinRate = bWins.toDouble() / games
        val avgLength = totalLength.toDouble() / games
        
        // Calculate statistical measures for agent A
        val confidenceInterval = StatisticalUtils.calculateWinRateConfidenceInterval(aWins, games)
        val isSignificant = StatisticalUtils.testStatisticalSignificance(aWins, games)
        val effectSize = StatisticalUtils.calculateEffectSize(aWinRate, bWinRate)
        
        val colorStats = ColorAlternationStats(
            whiteGames = aWhiteGames,
            blackGames = aBlackGames,
            whiteWins = aWhiteWins,
            blackWins = aBlackWins,
            whiteDraws = aWhiteDraws,
            blackDraws = aBlackDraws,
            whiteLosses = aWhiteLosses,
            blackLosses = aBlackLosses
        )
        
        logger.info("Model comparison complete: A=${String.format("%.1f%%", aWinRate * 100)}, B=${String.format("%.1f%%", bWinRate * 100)}")
        if (confidenceInterval != null) {
            logger.info("Agent A 95% confidence interval: ${StatisticalUtils.formatConfidenceInterval(confidenceInterval)}")
        }
        if (isSignificant) {
            logger.info("Performance difference is statistically significant (p < 0.05)")
        }
        logger.info("Effect size: ${String.format("%.3f", effectSize)} (${StatisticalUtils.interpretEffectSize(effectSize)})")
        
        return EnhancedComparisonResults(
            totalGames = games,
            modelAWins = aWins,
            draws = draws,
            modelBWins = bWins,
            modelAWinRate = aWinRate,
            drawRate = drawRate,
            modelBWinRate = bWinRate,
            averageGameLength = avgLength,
            confidenceInterval = confidenceInterval,
            statisticalSignificance = isSignificant,
            effectSize = effectSize,
            colorAlternation = colorStats
        )
    }
    
    /**
     * Create evaluation environment with consistent settings.
     */
    private fun createEvaluationEnvironment(): ChessEnvironment {
        return ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = config.winReward,
                lossReward = config.lossReward,
                drawReward = config.drawReward,
                stepPenalty = 0.0, // No step penalty for evaluation
                stepLimitPenalty = config.stepLimitPenalty,
                enablePositionRewards = false, // Keep evaluation simple
                gameLengthNormalization = false,
                maxGameLength = config.maxStepsPerGame,
                enableEarlyAdjudication = config.evalEarlyAdjudication,
                resignMaterialThreshold = config.evalResignMaterialThreshold,
                noProgressPlies = config.evalNoProgressPlies
            )
        )
    }
    
    /**
     * Play a single game against heuristic opponent.
     */
    private fun playGameAgainstHeuristic(
        environment: ChessEnvironment,
        agent: ChessAgent,
        agentIsWhite: Boolean
    ): GameResult {
        var state = environment.reset()
        var steps = 0
        
        while (!environment.isTerminal(state) && steps < config.maxStepsPerGame) {
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            val isWhiteToMove = environment.getCurrentBoard().getActiveColor().name.contains("WHITE")
            val agentTurn = (isWhiteToMove && agentIsWhite) || (!isWhiteToMove && !agentIsWhite)
            
            val action = if (agentTurn) {
                agent.selectAction(state, validActions)
            } else {
                // Use heuristic opponent
                val heuristicAction = BaselineHeuristicOpponent.selectAction(environment, validActions)
                if (heuristicAction >= 0) heuristicAction else validActions.first()
            }
            
            val step = environment.step(action)
            state = step.nextState
            steps++
        }
        
        val status = environment.getEffectiveGameStatus()
        val outcome = if (steps >= config.maxStepsPerGame) {
            // Game hit step limit - adjudicate based on material or call it a draw
            val board = environment.getCurrentBoard()
            val whiteMaterial = calculateMaterial(board, PieceColor.WHITE)
            val blackMaterial = calculateMaterial(board, PieceColor.BLACK)
            val materialDiff = whiteMaterial - blackMaterial
            
            when {
                materialDiff >= 5 -> GameOutcome.WHITE_WINS  // White has significant advantage
                materialDiff <= -5 -> GameOutcome.BLACK_WINS // Black has significant advantage
                else -> GameOutcome.DRAW // Material roughly equal, call it a draw
            }
        } else {
            // Game ended naturally
            when {
                status.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                status.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                status.name.contains("DRAW") -> GameOutcome.DRAW
                else -> GameOutcome.DRAW
            }
        }
        
        return GameResult(outcome, steps)
    }
    
    /**
     * Play a single game against minimax opponent.
     */
    private fun playGameAgainstMinimax(
        environment: ChessEnvironment,
        agent: ChessAgent,
        teacher: MinimaxTeacher,
        agentIsWhite: Boolean
    ): GameResult {
        var state = environment.reset()
        var steps = 0
        
        while (!environment.isTerminal(state) && steps < config.maxStepsPerGame) {
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            val isWhiteToMove = environment.getCurrentBoard().getActiveColor().name.contains("WHITE")
            val agentTurn = (isWhiteToMove && agentIsWhite) || (!isWhiteToMove && !agentIsWhite)
            
            val action = if (agentTurn) {
                agent.selectAction(state, validActions)
            } else {
                // Use minimax opponent
                val move = teacher.act(environment.getCurrentBoard()).bestMove
                val encoded = actionEncoder.encodeMove(move)
                if (encoded in validActions) encoded else {
                    // Robust fallback: try to match by from->to among valid actions
                    val fallback = validActions.firstOrNull { ai ->
                        val m2 = actionEncoder.decodeAction(ai)
                        m2.from == move.from && m2.to == move.to
                    }
                    fallback ?: validActions.first()
                }
            }
            
            val step = environment.step(action)
            state = step.nextState
            steps++
        }
        
        val status = environment.getEffectiveGameStatus()
        val outcome = if (steps >= config.maxStepsPerGame) {
            // Game hit step limit - adjudicate based on material or call it a draw
            val board = environment.getCurrentBoard()
            val whiteMaterial = calculateMaterial(board, PieceColor.WHITE)
            val blackMaterial = calculateMaterial(board, PieceColor.BLACK)
            val materialDiff = whiteMaterial - blackMaterial
            
            when {
                materialDiff >= 5 -> GameOutcome.WHITE_WINS  // White has significant advantage
                materialDiff <= -5 -> GameOutcome.BLACK_WINS // Black has significant advantage
                else -> GameOutcome.DRAW // Material roughly equal, call it a draw
            }
        } else {
            // Game ended naturally
            when {
                status.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                status.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                status.name.contains("DRAW") -> GameOutcome.DRAW
                else -> GameOutcome.DRAW
            }
        }
        
        return GameResult(outcome, steps)
    }
    
    /**
     * Play a single game between two agents.
     */
    private fun playGameBetweenAgents(
        environment: ChessEnvironment,
        agentA: ChessAgent,
        agentB: ChessAgent,
        aIsWhite: Boolean
    ): GameResult {
        var state = environment.reset()
        var steps = 0
        
        while (!environment.isTerminal(state) && steps < config.maxStepsPerGame) {
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            val isWhiteToMove = environment.getCurrentBoard().getActiveColor().name.contains("WHITE")
            val aTurn = (isWhiteToMove && aIsWhite) || (!isWhiteToMove && !aIsWhite)
            
            val action = if (aTurn) {
                agentA.selectAction(state, validActions)
            } else {
                agentB.selectAction(state, validActions)
            }
            
            val step = environment.step(action)
            state = step.nextState
            steps++
        }
        
        val status = environment.getEffectiveGameStatus()
        val outcome = if (steps >= config.maxStepsPerGame) {
            // Game hit step limit - adjudicate based on material or call it a draw
            val board = environment.getCurrentBoard()
            val whiteMaterial = calculateMaterial(board, PieceColor.WHITE)
            val blackMaterial = calculateMaterial(board, PieceColor.BLACK)
            val materialDiff = whiteMaterial - blackMaterial
            
            when {
                materialDiff >= 5 -> GameOutcome.WHITE_WINS  // White has significant advantage
                materialDiff <= -5 -> GameOutcome.BLACK_WINS // Black has significant advantage
                else -> GameOutcome.DRAW // Material roughly equal, call it a draw
            }
        } else {
            // Game ended naturally
            when {
                status.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                status.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                status.name.contains("DRAW") -> GameOutcome.DRAW
                else -> GameOutcome.DRAW
            }
        }
        
        return GameResult(outcome, steps)
    }
    /**
     * Calculate material value for a color
     */
    private fun calculateMaterial(board: ChessBoard, color: PieceColor): Int {
        val pieceValues = mapOf(
            PieceType.PAWN to 1,
            PieceType.KNIGHT to 3,
            PieceType.BISHOP to 3,
            PieceType.ROOK to 5,
            PieceType.QUEEN to 9,
            PieceType.KING to 0
        )
        
        var material = 0
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board.getPieceAt(Position(rank, file))
                if (piece?.color == color) {
                    material += pieceValues[piece.type] ?: 0
                }
            }
        }
        return material
    }

    /**
     * Result from a single game.
     */
    private data class GameResult(
        val outcome: GameOutcome,
        val gameLength: Int
    )
}

/**
 * Results from evaluating an agent against a baseline.
 * @deprecated Use EnhancedEvaluationResults instead
 */
@Deprecated("Use EnhancedEvaluationResults instead", ReplaceWith("EnhancedEvaluationResults"))
data class EvaluationResults(
    val totalGames: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val averageGameLength: Double
)

/**
 * Results from comparing two models.
 * @deprecated Use EnhancedComparisonResults instead
 */
@Deprecated("Use EnhancedComparisonResults instead", ReplaceWith("EnhancedComparisonResults"))
data class ComparisonResults(
    val totalGames: Int,
    val modelAWins: Int,
    val draws: Int,
    val modelBWins: Int,
    val modelAWinRate: Double,
    val drawRate: Double,
    val modelBWinRate: Double,
    val averageGameLength: Double
)
