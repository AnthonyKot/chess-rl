package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.teacher.MinimaxTeacher
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
    fun evaluateAgainstHeuristic(agent: ChessAgent, games: Int): EvaluationResults {
        logger.info("Evaluating agent against heuristic baseline over $games games")
        
        val environment = createEvaluationEnvironment()
        var wins = 0
        var draws = 0
        var losses = 0
        var totalLength = 0
        
        repeat(games) { gameIndex ->
            val agentIsWhite = gameIndex % 2 == 0 // Alternate colors
            val result = playGameAgainstHeuristic(environment, agent, agentIsWhite)
            
            when (result.outcome) {
                GameOutcome.WHITE_WINS -> if (agentIsWhite) wins++ else losses++
                GameOutcome.BLACK_WINS -> if (!agentIsWhite) wins++ else losses++
                else -> draws++
            }
            
            totalLength += result.gameLength
        }
        
        val winRate = wins.toDouble() / games
        val drawRate = draws.toDouble() / games
        val lossRate = losses.toDouble() / games
        val avgLength = totalLength.toDouble() / games
        
        logger.info("Heuristic evaluation complete: ${String.format("%.1f%%", winRate * 100)} win rate over $games games")
        
        return EvaluationResults(
            totalGames = games,
            wins = wins,
            draws = draws,
            losses = losses,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageGameLength = avgLength
        )
    }
    
    /**
     * Evaluate agent against minimax baseline.
     */
    fun evaluateAgainstMinimax(agent: ChessAgent, games: Int, depth: Int = 2): EvaluationResults {
        logger.info("Evaluating agent against minimax depth-$depth over $games games")
        
        val environment = createEvaluationEnvironment()
        val random = config.seed?.let { Random(it) } ?: Random.Default
        val teacher = MinimaxTeacher(depth = depth, random = random)
        
        var wins = 0
        var draws = 0
        var losses = 0
        var totalLength = 0
        
        repeat(games) { gameIndex ->
            val agentIsWhite = gameIndex % 2 == 0 // Alternate colors
            val result = playGameAgainstMinimax(environment, agent, teacher, agentIsWhite)
            
            when (result.outcome) {
                GameOutcome.WHITE_WINS -> if (agentIsWhite) wins++ else losses++
                GameOutcome.BLACK_WINS -> if (!agentIsWhite) wins++ else losses++
                else -> draws++
            }
            
            totalLength += result.gameLength
        }
        
        val winRate = wins.toDouble() / games
        val drawRate = draws.toDouble() / games
        val lossRate = losses.toDouble() / games
        val avgLength = totalLength.toDouble() / games
        
        logger.info("Minimax evaluation complete: ${String.format("%.1f%%", winRate * 100)} win rate over $games games")
        
        return EvaluationResults(
            totalGames = games,
            wins = wins,
            draws = draws,
            losses = losses,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageGameLength = avgLength
        )
    }
    
    /**
     * Compare two models head-to-head.
     */
    fun compareModels(agentA: ChessAgent, agentB: ChessAgent, games: Int): ComparisonResults {
        logger.info("Comparing two models over $games games")
        
        val environment = createEvaluationEnvironment()
        var aWins = 0
        var draws = 0
        var bWins = 0
        var totalLength = 0
        
        repeat(games) { gameIndex ->
            val aIsWhite = gameIndex % 2 == 0 // Alternate colors
            val result = playGameBetweenAgents(environment, agentA, agentB, aIsWhite)
            
            when (result.outcome) {
                GameOutcome.WHITE_WINS -> if (aIsWhite) aWins++ else bWins++
                GameOutcome.BLACK_WINS -> if (!aIsWhite) aWins++ else bWins++
                else -> draws++
            }
            
            totalLength += result.gameLength
        }
        
        val aWinRate = aWins.toDouble() / games
        val drawRate = draws.toDouble() / games
        val bWinRate = bWins.toDouble() / games
        val avgLength = totalLength.toDouble() / games
        
        logger.info("Model comparison complete: A=${String.format("%.1f%%", aWinRate * 100)}, B=${String.format("%.1f%%", bWinRate * 100)}")
        
        return ComparisonResults(
            totalGames = games,
            modelAWins = aWins,
            draws = draws,
            modelBWins = bWins,
            modelAWinRate = aWinRate,
            drawRate = drawRate,
            modelBWinRate = bWinRate,
            averageGameLength = avgLength
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
                enableEarlyAdjudication = false, // Let games play out naturally
                resignMaterialThreshold = 15, // Conservative resignation
                noProgressPlies = 100 // Allow longer games
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
        
        val outcome = when {
            environment.getGameStatus().name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
            environment.getGameStatus().name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
            else -> GameOutcome.DRAW
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
                val actionIndex = actionEncoder.encodeMove(move)
                if (actionIndex in validActions) actionIndex else validActions.first()
            }
            
            val step = environment.step(action)
            state = step.nextState
            steps++
        }
        
        val outcome = when {
            environment.getGameStatus().name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
            environment.getGameStatus().name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
            else -> GameOutcome.DRAW
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
        
        val outcome = when {
            environment.getGameStatus().name.contains("WHITE_WINS") -> GameOutcome.WHITE_WINS
            environment.getGameStatus().name.contains("BLACK_WINS") -> GameOutcome.BLACK_WINS
            else -> GameOutcome.DRAW
        }
        
        return GameResult(outcome, steps)
    }
}

/**
 * Results from evaluating an agent against a baseline.
 */
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
 */
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

/**
 * Result from a single game.
 */
private data class GameResult(
    val outcome: GameOutcome,
    val gameLength: Int
)

