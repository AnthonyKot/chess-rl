package com.chessrl.integration

import com.chessrl.rl.Experience
import kotlin.random.Random

/**
 * Baseline Evaluator for Chess RL Training
 * 
 * Evaluates trained agents against various baseline opponents to measure
 * absolute performance and improvement over time.
 */
class BaselineEvaluator(
    private val config: BaselineEvaluationConfig = BaselineEvaluationConfig()
) {
    
    private val random = Random(config.randomSeed)
    
    /**
     * Initialize the baseline evaluator
     */
    fun initialize() {
        println("🎯 Initializing Baseline Evaluator")
        println("Configuration: $config")
    }
    
    /**
     * Evaluate agent against random opponent
     */
    fun evaluateAgainstRandomOpponent(
        agent: ChessAgent,
        environment: ChessEnvironment
    ): OpponentEvaluationResult {
        
        println("🎲 Evaluating against random opponent...")
        
        val results = mutableListOf<GameResult>()
        val startTime = System.currentTimeMillis()
        
        repeat(config.gamesPerOpponent) { gameIndex ->
            val gameResult = playGameAgainstRandomOpponent(agent, environment, gameIndex)
            results.add(gameResult)
        }
        
        val winRate = results.count { it.outcome == BaselineOutcome.WIN }.toDouble() / results.size
        val drawRate = results.count { it.outcome == BaselineOutcome.DRAW }.toDouble() / results.size
        val lossRate = results.count { it.outcome == BaselineOutcome.LOSS }.toDouble() / results.size
        val averageGameLength = results.map { it.moveCount }.average()
        val averageReward = results.map { it.reward }.average()
        
        return OpponentEvaluationResult(
            opponentType = "Random",
            gamesPlayed = results.size,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageGameLength = averageGameLength,
            averageReward = averageReward,
            gameResults = results,
            evaluationTime = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * Evaluate agent against heuristic opponent
     */
    fun evaluateAgainstHeuristicOpponent(
        agent: ChessAgent,
        environment: ChessEnvironment
    ): OpponentEvaluationResult {
        
        println("🧠 Evaluating against heuristic opponent...")
        
        val results = mutableListOf<GameResult>()
        val startTime = System.currentTimeMillis()
        
        repeat(config.gamesPerOpponent) { gameIndex ->
            val gameResult = playGameAgainstHeuristicOpponent(agent, environment, gameIndex)
            results.add(gameResult)
        }
        
        val winRate = results.count { it.outcome == BaselineOutcome.WIN }.toDouble() / results.size
        val drawRate = results.count { it.outcome == BaselineOutcome.DRAW }.toDouble() / results.size
        val lossRate = results.count { it.outcome == BaselineOutcome.LOSS }.toDouble() / results.size
        val averageGameLength = results.map { it.moveCount }.average()
        val averageReward = results.map { it.reward }.average()
        
        return OpponentEvaluationResult(
            opponentType = "Heuristic",
            gamesPlayed = results.size,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageGameLength = averageGameLength,
            averageReward = averageReward,
            gameResults = results,
            evaluationTime = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * Evaluate agent against material-focused opponent
     */
    fun evaluateAgainstMaterialOpponent(
        agent: ChessAgent,
        environment: ChessEnvironment
    ): OpponentEvaluationResult {
        
        println("♛ Evaluating against material-focused opponent...")
        
        val results = mutableListOf<GameResult>()
        val startTime = System.currentTimeMillis()
        
        repeat(config.gamesPerOpponent) { gameIndex ->
            val gameResult = playGameAgainstMaterialOpponent(agent, environment, gameIndex)
            results.add(gameResult)
        }
        
        val winRate = results.count { it.outcome == BaselineOutcome.WIN }.toDouble() / results.size
        val drawRate = results.count { it.outcome == BaselineOutcome.DRAW }.toDouble() / results.size
        val lossRate = results.count { it.outcome == BaselineOutcome.LOSS }.toDouble() / results.size
        val averageGameLength = results.map { it.moveCount }.average()
        val averageReward = results.map { it.reward }.average()
        
        return OpponentEvaluationResult(
            opponentType = "Material",
            gamesPlayed = results.size,
            winRate = winRate,
            drawRate = drawRate,
            lossRate = lossRate,
            averageGameLength = averageGameLength,
            averageReward = averageReward,
            gameResults = results,
            evaluationTime = System.currentTimeMillis() - startTime
        )
    }
    
    // Private game playing methods
    
    private fun playGameAgainstRandomOpponent(
        agent: ChessAgent,
        environment: ChessEnvironment,
        gameIndex: Int
    ): GameResult {
        
        val gameStartTime = System.currentTimeMillis()
        var state = environment.reset()
        var totalReward = 0.0
        var moveCount = 0
        val maxMoves = config.maxMovesPerGame
        
        // Randomly assign colors (agent plays both white and black)
        val agentPlaysWhite = gameIndex % 2 == 0
        
        while (!environment.isTerminal(state) && moveCount < maxMoves) {
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            val action = if ((moveCount % 2 == 0) == agentPlaysWhite) {
                // Agent's turn
                agent.selectAction(state, validActions)
            } else {
                // Random opponent's turn
                validActions[random.nextInt(validActions.size)]
            }
            
            val stepResult = environment.step(action)
            state = stepResult.nextState
            
            if ((moveCount % 2 == 0) == agentPlaysWhite) {
                totalReward += stepResult.reward
            }
            
            moveCount++
        }
        
        // Determine game outcome from agent's perspective
        val outcome = determineGameOutcome(state, environment, agentPlaysWhite, totalReward)
        
        return GameResult(
            gameId = "random_$gameIndex",
            outcome = outcome,
            moveCount = moveCount,
            reward = totalReward,
            gameLength = System.currentTimeMillis() - gameStartTime,
            agentPlayedWhite = agentPlaysWhite
        )
    }
    
    private fun playGameAgainstHeuristicOpponent(
        agent: ChessAgent,
        environment: ChessEnvironment,
        gameIndex: Int
    ): GameResult {
        
        val gameStartTime = System.currentTimeMillis()
        var state = environment.reset()
        var totalReward = 0.0
        var moveCount = 0
        val maxMoves = config.maxMovesPerGame
        
        // Randomly assign colors
        val agentPlaysWhite = gameIndex % 2 == 0
        
        while (!environment.isTerminal(state) && moveCount < maxMoves) {
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            val action = if ((moveCount % 2 == 0) == agentPlaysWhite) {
                // Agent's turn
                agent.selectAction(state, validActions)
            } else {
                // Heuristic opponent's turn
                BaselineHeuristicOpponent.selectAction(environment, validActions)
            }
            
            val stepResult = environment.step(action)
            state = stepResult.nextState
            
            if ((moveCount % 2 == 0) == agentPlaysWhite) {
                totalReward += stepResult.reward
            }
            
            moveCount++
        }
        
        val outcome = determineGameOutcome(state, environment, agentPlaysWhite, totalReward)
        
        return GameResult(
            gameId = "heuristic_$gameIndex",
            outcome = outcome,
            moveCount = moveCount,
            reward = totalReward,
            gameLength = System.currentTimeMillis() - gameStartTime,
            agentPlayedWhite = agentPlaysWhite
        )
    }
    
    private fun playGameAgainstMaterialOpponent(
        agent: ChessAgent,
        environment: ChessEnvironment,
        gameIndex: Int
    ): GameResult {
        
        val gameStartTime = System.currentTimeMillis()
        var state = environment.reset()
        var totalReward = 0.0
        var moveCount = 0
        val maxMoves = config.maxMovesPerGame
        
        // Randomly assign colors
        val agentPlaysWhite = gameIndex % 2 == 0
        
        while (!environment.isTerminal(state) && moveCount < maxMoves) {
            val validActions = environment.getValidActions(state)
            if (validActions.isEmpty()) break
            
            val action = if ((moveCount % 2 == 0) == agentPlaysWhite) {
                // Agent's turn
                agent.selectAction(state, validActions)
            } else {
                // Material-focused opponent (simplified heuristic focusing on captures)
                selectMaterialFocusedAction(environment, validActions)
            }
            
            val stepResult = environment.step(action)
            state = stepResult.nextState
            
            if ((moveCount % 2 == 0) == agentPlaysWhite) {
                totalReward += stepResult.reward
            }
            
            moveCount++
        }
        
        val outcome = determineGameOutcome(state, environment, agentPlaysWhite, totalReward)
        
        return GameResult(
            gameId = "material_$gameIndex",
            outcome = outcome,
            moveCount = moveCount,
            reward = totalReward,
            gameLength = System.currentTimeMillis() - gameStartTime,
            agentPlayedWhite = agentPlaysWhite
        )
    }
    
    private fun selectMaterialFocusedAction(
        environment: ChessEnvironment,
        validActions: List<Int>
    ): Int {
        // Simple material-focused strategy: prefer captures, then random
        // This is a simplified version - in practice, would analyze each move
        
        if (validActions.isEmpty()) return -1
        
        // For now, use the heuristic opponent as material-focused
        // In a full implementation, this would specifically focus on material gain
        return BaselineHeuristicOpponent.selectAction(environment, validActions)
    }
    
    private fun determineGameOutcome(
        state: DoubleArray,
        environment: ChessEnvironment,
        agentPlaysWhite: Boolean,
        totalReward: Double
    ): BaselineOutcome {
        
        if (!environment.isTerminal(state)) {
            // Game didn't finish naturally (hit move limit)
            return when {
                totalReward > 0.1 -> BaselineOutcome.WIN
                totalReward < -0.1 -> BaselineOutcome.LOSS
                else -> BaselineOutcome.DRAW
            }
        }
        
        // Use reward to determine outcome
        return when {
            totalReward > 0.5 -> BaselineOutcome.WIN
            totalReward < -0.5 -> BaselineOutcome.LOSS
            else -> BaselineOutcome.DRAW
        }
    }
}

/**
 * Configuration for baseline evaluation
 */
data class BaselineEvaluationConfig(
    val gamesPerOpponent: Int = 20,
    val maxMovesPerGame: Int = 200,
    val randomSeed: Int = 42,
    val timeoutPerGame: Long = 30000L // 30 seconds per game
)

/**
 * Result of evaluation against a specific opponent type
 */
data class OpponentEvaluationResult(
    val opponentType: String,
    val gamesPlayed: Int,
    val winRate: Double,
    val drawRate: Double,
    val lossRate: Double,
    val averageGameLength: Double,
    val averageReward: Double,
    val gameResults: List<GameResult>,
    val evaluationTime: Long
)

/**
 * Individual game result
 */
data class GameResult(
    val gameId: String,
    val outcome: BaselineOutcome,
    val moveCount: Int,
    val reward: Double,
    val gameLength: Long,
    val agentPlayedWhite: Boolean
)

/**
 * Game outcome from agent's perspective
 */
enum class BaselineOutcome {
    WIN,
    DRAW,
    LOSS
}
