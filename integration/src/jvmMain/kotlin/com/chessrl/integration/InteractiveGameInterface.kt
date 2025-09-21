package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger

/**
 * Interactive game interface for human vs agent play.
 * 
 * Provides a console-based chess interface where humans can play against trained agents.
 */
class InteractiveGameInterface(
    private val agent: ChessAgent,
    private val humanColor: PieceColor,
    private val config: ChessRLConfig,
    private val playConfig: com.chessrl.integration.output.PlaySessionConfig? = null
) {
    
    private val logger = ChessRLLogger.forComponent("InteractiveGame")
    private val actionEncoder = ChessActionEncoder()
    
    /**
     * Start an interactive chess game.
     */
    fun playGame() {
        // Show concise header with session information
        showGameHeader()
        
        val environment = ChessEnvironment(
            rewardConfig = ChessRewardConfig(
                winReward = 1.0,
                lossReward = -1.0,
                drawReward = 0.0,
                stepPenalty = 0.0,
                stepLimitPenalty = -0.5,
                enablePositionRewards = false,
                gameLengthNormalization = false,
                maxGameLength = config.maxStepsPerGame,
                enableEarlyAdjudication = false,
                resignMaterialThreshold = 15,
                noProgressPlies = 100
            )
        )
        
        var state = environment.reset()
        var moveCount = 0
        val moveHistory = mutableListOf<String>()
        
        println("\nGame started!")
        printBoard(environment.getCurrentBoard())
        
        while (!environment.isTerminal(state) && moveCount < config.maxStepsPerGame) {
            val currentBoard = environment.getCurrentBoard()
            val activeColor = currentBoard.getActiveColor()
            val validActions = environment.getValidActions(state)
            
            if (validActions.isEmpty()) {
                println("No valid moves available. Game over.")
                break
            }
            
            val isHumanTurn = activeColor == humanColor
            
            if (isHumanTurn) {
                println("\nYour turn (${activeColor.name.lowercase()}):")
                val action = getHumanMove(currentBoard, validActions)
                if (action == -1) {
                    println("Game ended by player.")
                    break
                }
                
                val step = environment.step(action)
                state = step.nextState
                
                val move = step.info["move"]?.toString() ?: "unknown"
                moveHistory.add(move)
                println("You played: $move")
                
            } else {
                println("\nAgent's turn (${activeColor.name.lowercase()}):")
                val action = agent.selectAction(state, validActions)
                val step = environment.step(action)
                state = step.nextState
                
                val move = step.info["move"]?.toString() ?: "unknown"
                moveHistory.add(move)
                
                // Clean move display - show engine diagnostics only if verbose
                if (playConfig?.verboseEngineOutput == true) {
                    showEngineAnalysis(action, validActions, step)
                } else {
                    println("Agent played: $move")
                }
            }
            
            moveCount++
            printBoard(environment.getCurrentBoard())
            
            // Check for game end
            val gameStatus = environment.getGameStatus()
            if (gameStatus != GameStatus.ONGOING) {
                printGameResult(gameStatus, humanColor)
                break
            }
        }
        
        if (moveCount >= config.maxStepsPerGame) {
            println("\nGame ended due to move limit (${config.maxStepsPerGame} moves)")
        }
        
        println("\nGame history:")
        moveHistory.chunked(2).forEachIndexed { index, pair ->
            val moveNumber = index + 1
            val whiteMove = pair.getOrNull(0) ?: ""
            val blackMove = pair.getOrNull(1) ?: ""
            println("$moveNumber. $whiteMove $blackMove")
        }
    }
    
    /**
     * Get human move input and convert to action index.
     */
    private fun getHumanMove(board: ChessBoard, validActions: List<Int>): Int {
        while (true) {
            print("Enter your move (or 'quit' to exit): ")
            val input = readLine()?.trim() ?: ""
            
            if (input.lowercase() == "quit") {
                return -1
            }
            
            if (input.isEmpty()) {
                continue
            }
            
            try {
                // Try to parse as algebraic notation
                val move = parseAlgebraicMove(input, board)
                if (move != null) {
                    val actionIndex = actionEncoder.encodeMove(move)
                    if (actionIndex in validActions) {
                        return actionIndex
                    } else {
                        println("That move is not valid in the current position.")
                        showValidMoves(board, validActions)
                    }
                } else {
                    println("Could not parse move '$input'. Use algebraic notation (e.g., e4, Nf3, O-O)")
                    showMoveHelp()
                }
            } catch (e: Exception) {
                println("Error parsing move: ${e.message}")
                showMoveHelp()
            }
        }
    }
    
    /**
     * Parse algebraic notation move.
     */
    private fun parseAlgebraicMove(notation: String, board: ChessBoard): Move? {
        return try {
            // Try to parse using the chess engine's move parsing
            val validMoves = board.getAllValidMoves(board.getActiveColor())
            validMoves.find { move ->
                move.toAlgebraic() == notation || 
                move.toAlgebraic().replace("+", "").replace("#", "") == notation ||
                move.toString() == notation
            }
        } catch (e: Exception) {
            logger.debug("Failed to parse algebraic move: $notation", e)
            null
        }
    }
    
    /**
     * Show valid moves to help the player.
     */
    private fun showValidMoves(board: ChessBoard, validActions: List<Int>) {
        println("Valid moves:")
        val validMoves = validActions.mapNotNull { actionIndex ->
            try {
                val move = actionEncoder.decodeAction(actionIndex)
                move.toAlgebraic()
            } catch (e: Exception) {
                null
            }
        }.sorted()
        
        validMoves.chunked(8).forEach { chunk ->
            println("  ${chunk.joinToString("  ")}")
        }
    }
    
    /**
     * Show move input help.
     */
    private fun showMoveHelp() {
        println("Move notation examples:")
        println("  Pawn moves: e4, d5, exd5 (capture)")
        println("  Piece moves: Nf3, Bb5, Qh5, Rd1")
        println("  Castling: O-O (kingside), O-O-O (queenside)")
        println("  Promotion: e8=Q, a1=N")
    }
    
    /**
     * Print the current board state.
     */
    private fun printBoard(board: ChessBoard) {
        println("\n" + board.toASCII())
        
        // Show additional game info
        val activeColor = board.getActiveColor()
        println("To move: ${activeColor.name.lowercase()}")
        
        val gameStatus = board.getGameStatus()
        if (gameStatus.name.contains("CHECK")) {
            println("${activeColor.name.lowercase()} is in check!")
        }
    }
    
    /**
     * Print the final game result.
     */
    private fun printGameResult(gameStatus: GameStatus, humanColor: PieceColor) {
        println("\nGame Over!")
        
        when {
            gameStatus.name.contains("WHITE_WINS") -> {
                if (humanColor == PieceColor.WHITE) {
                    println("🎉 You won! White wins by checkmate.")
                } else {
                    println("😞 You lost. White wins by checkmate.")
                }
            }
            gameStatus.name.contains("BLACK_WINS") -> {
                if (humanColor == PieceColor.BLACK) {
                    println("🎉 You won! Black wins by checkmate.")
                } else {
                    println("😞 You lost. Black wins by checkmate.")
                }
            }
            gameStatus.name.contains("STALEMATE") -> {
                println("🤝 Draw by stalemate.")
            }
            gameStatus.name.contains("REPETITION") -> {
                println("🤝 Draw by threefold repetition.")
            }
            gameStatus.name.contains("FIFTY_MOVE_RULE") -> {
                println("🤝 Draw by fifty-move rule.")
            }
            gameStatus.name.contains("INSUFFICIENT_MATERIAL") -> {
                println("🤝 Draw by insufficient material.")
            }
            else -> {
                println("🤝 Game ended in a draw.")
            }
        }
    }
    
    /**
     * Show concise game header with session information.
     */
    private fun showGameHeader() {
        println("═══════════════════════════════════════════════════════════")
        println("                    CHESS RL INTERACTIVE PLAY")
        println("═══════════════════════════════════════════════════════════")
        
        playConfig?.let { config ->
            println("Profile: ${config.profileUsed}")
            println("Max steps: ${config.maxSteps}")
            if (config.verboseEngineOutput) {
                println("Engine diagnostics: enabled")
            }
        }
        
        println("You are playing as: ${humanColor.name.lowercase()}")
        println("Enter moves in algebraic notation (e.g., e4, Nf3, O-O)")
        println("Type 'quit' to exit")
        println("═══════════════════════════════════════════════════════════")
    }
    
    /**
     * Show detailed engine analysis when verbose mode is enabled.
     */
    private fun showEngineAnalysis(action: Int, validActions: List<Int>, step: Any) {
        // Extract move from step info
        val stepInfo = try {
            val infoField = step::class.java.getDeclaredField("info")
            infoField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            infoField.get(step) as? Map<String, Any>
        } catch (e: Exception) {
            null
        }
        
        val move = stepInfo?.get("move")?.toString() ?: "unknown"
        println("Agent played: $move")
        
        // Show additional engine diagnostics
        println("  Action index: $action")
        println("  Valid actions: ${validActions.size}")
        
        // Try to get reward
        try {
            val rewardField = step::class.java.getDeclaredField("reward")
            rewardField.isAccessible = true
            val reward = rewardField.get(step)
            println("  Reward: $reward")
        } catch (e: Exception) {
            // Ignore if reward field not available
        }
        
        // Show Q-value if available in step info
        stepInfo?.get("qValue")?.let { qValue ->
            println("  Q-value: $qValue")
        }
        
        // Show evaluation if available
        stepInfo?.get("evaluation")?.let { eval ->
            println("  Position evaluation: $eval")
        }
    }
}