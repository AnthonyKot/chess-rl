package com.chessrl.integration

import com.chessrl.integration.adapter.ChessEngineFactory
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.integration.output.EnhancedEvaluationResults
import com.chessrl.integration.output.EnhancedComparisonResults
import com.chessrl.integration.output.ColorAlternationStats
import com.chessrl.integration.output.StatisticalUtils
import com.chessrl.teacher.MinimaxTeacher
import com.chessrl.chess.*
import kotlin.random.Random
import com.chessrl.integration.opponent.OpponentKind
import com.chessrl.integration.opponent.OpponentSelector

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

    fun evaluateAgainstMixedOpponents(agent: ChessAgent, games: Int): EnhancedEvaluationResults {
        logger.info("Evaluating agent against mixed opponents (heuristic + minimax d1/d2) over $games games")

        val environment = createEvaluationEnvironment()
        val baseRandom = config.seed?.let { Random(it) } ?: Random.Default
        val selectionRandom = Random(baseRandom.nextLong())
        val teacherDepth1 = MinimaxTeacher(depth = 1, random = Random(baseRandom.nextLong()))
        val teacherDepth2 = MinimaxTeacher(depth = 2, random = Random(baseRandom.nextLong()))

        var wins = 0
        var draws = 0
        var losses = 0
        var totalLength = 0

        var whiteGames = 0
        var blackGames = 0
        var whiteWins = 0
        var blackWins = 0
        var whiteDraws = 0
        var blackDraws = 0
        var whiteLosses = 0
        var blackLosses = 0

        var heuristicGames = 0
        var minimaxDepth1Games = 0
        var minimaxDepth2Games = 0

        repeat(games) { gameIndex ->
            val agentIsWhite = gameIndex % 2 == 0
            val selection = OpponentSelector.select("random", 2, selectionRandom)
            val result = when (selection.kind) {
                OpponentKind.HEURISTIC -> {
                    heuristicGames++
                    playGameAgainstHeuristic(environment, agent, agentIsWhite)
                }
                OpponentKind.MINIMAX -> {
                    val depth = selection.minimaxDepth ?: 2
                    if (depth <= 1) {
                        minimaxDepth1Games++
                        playGameAgainstMinimax(environment, agent, teacherDepth1, agentIsWhite)
                    } else {
                        minimaxDepth2Games++
                        playGameAgainstMinimax(environment, agent, teacherDepth2, agentIsWhite)
                    }
                }
                else -> playGameAgainstHeuristic(environment, agent, agentIsWhite)
            }

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

        logger.info(
            "Mixed opponent breakdown: heuristic=$heuristicGames, " +
                "minimax(d=1)=$minimaxDepth1Games, minimax(d=2)=$minimaxDepth2Games"
        )
        logger.info("Mixed evaluation complete: ${String.format("%.1f%%", winRate * 100)} win rate over $games games")
        confidenceInterval?.let {
            logger.info("95% confidence interval: ${StatisticalUtils.formatConfidenceInterval(it)}")
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
            opponentType = "Mixed"
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

        val decisiveGames = aWins + bWins
        val decisivePValue = if (decisiveGames > 0) {
            StatisticalUtils.binomialTwoTailedPValue(aWins, decisiveGames, 0.5)
        } else 1.0
        val winRateCI = StatisticalUtils.calculateWinRateConfidenceInterval(aWins, games)
        val isSignificant = decisivePValue < 0.05
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
        winRateCI?.let {
            logger.info("Agent A 95% confidence interval: ${StatisticalUtils.formatConfidenceInterval(it)}")
        }
        logger.info("Two-tailed binomial p-value (decisive games): ${"%.4g".format(decisivePValue)}")
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
            confidenceInterval = winRateCI,
            statisticalSignificance = isSignificant,
            pValue = decisivePValue,
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
            ),
            adapter = ChessEngineFactory.create(config.engine)
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
        var invalidMoves = 0

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
            if (step.info.containsKey("error")) {
                invalidMoves++
                if (invalidMoves <= 5) {
                    logger.warn(
                        "Invalid move detected (agentTurn=$agentTurn, whiteToMove=$isWhiteToMove): ${step.info["error"]}; fen=${step.info["fen"]}; legal=${step.info["legal_moves"]}"
                    )
                }
            }
            state = step.nextState
            steps++
        }
        
        val status = environment.getEffectiveGameStatus()
        val outcome = determineOutcome(environment, status, steps >= config.maxStepsPerGame, agentIsWhite)
        
        if (invalidMoves > 0) {
            logger.warn(
                "Encountered $invalidMoves invalid moves during head-to-head game (agentIsWhite=$agentIsWhite, outcome=${outcome.name})"
            )
        }

        return GameResult(outcome, steps)
    }

    private fun determineOutcome(
        environment: ChessEnvironment,
        status: GameStatus,
        hitStepLimit: Boolean,
        agentIsWhite: Boolean
    ): GameOutcome {
        return if (hitStepLimit) {
            val board = environment.getCurrentBoard()
            val whiteMaterial = calculateMaterial(board, PieceColor.WHITE)
            val blackMaterial = calculateMaterial(board, PieceColor.BLACK)
            val materialDiff = whiteMaterial - blackMaterial
            logger.info("Step-limit adjudication (agentIsWhite=$agentIsWhite): white=$whiteMaterial, black=$blackMaterial, diff=$materialDiff")

            when {
                materialDiff >= 5 -> GameOutcome.WHITE_WINS
                materialDiff <= -5 -> GameOutcome.BLACK_WINS
                else -> GameOutcome.DRAW
            }
        } else {
            when {
                status.name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
                status.name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
                status.name.contains("DRAW") -> {
                    logger.info("Natural draw detected (agentIsWhite=$agentIsWhite): status=${status.name}")
                    GameOutcome.DRAW
                }
                else -> GameOutcome.DRAW
            }
        }
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
        val outcome = determineOutcome(environment, status, steps >= config.maxStepsPerGame, agentIsWhite)
        
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
        var invalidMoves = 0

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

             if (step.info.containsKey("error")) {
                 invalidMoves++
                 if (invalidMoves <= 5) {
                    logger.warn(
                        "Invalid move detected (agentAturn=$aTurn, whiteToMove=$isWhiteToMove): ${step.info["error"]}; fen=${step.info["fen"]}; legal=${step.info["legal_moves"]}"
                    )
                 }
             }
        }
        
        val status = environment.getEffectiveGameStatus()
        val outcome = determineOutcome(environment, status, steps >= config.maxStepsPerGame, aIsWhite)
        
        if (invalidMoves > 0) {
            logger.warn("Encountered $invalidMoves invalid moves during head-to-head game (aIsWhite=$aIsWhite, outcome=${outcome.name})")
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
