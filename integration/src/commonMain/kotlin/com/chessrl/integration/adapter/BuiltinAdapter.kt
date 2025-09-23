package com.chessrl.integration.adapter

import com.chessrl.chess.*

/**
 * Builtin Chess Engine Adapter
 * 
 * This adapter wraps the existing handmade chess engine without modification,
 * providing a ChessEngineAdapter interface while maintaining 100% compatibility
 * with the current engine behavior.
 * 
 * The adapter uses FEN-based state reconstruction to ensure immutability and
 * thread safety while delegating all chess logic to the existing ChessBoard
 * implementation.
 */
class BuiltinAdapter : ChessEngineAdapter {
    
    override fun initialState(): ChessState {
        val board = ChessBoard()
        return ChessState(
            fen = board.toFEN(),
            activeColor = board.getActiveColor()
        )
    }
    
    override fun getLegalMoves(state: ChessState): List<ChessMove> {
        // Return cached moves if available for performance
        state.legalMoves?.let { return it }
        
        val board = createBoardFromState(state)
        val moves = board.getAllValidMoves(board.getActiveColor())
        return moves.map { convertToChessMove(it) }
    }
    
    override fun applyMove(state: ChessState, move: ChessMove): ChessState {
        val board = createBoardFromState(state)
        val engineMove = convertToEngineMove(move)
        
        val result = board.makeLegalMove(engineMove)
        if (result != MoveResult.SUCCESS) {
            throw IllegalArgumentException("Invalid move: ${move.algebraic}")
        }
        
        board.switchActiveColor()
        return ChessState(
            fen = board.toFEN(),
            activeColor = board.getActiveColor()
        )
    }
    
    override fun isTerminal(state: ChessState): Boolean {
        val board = createBoardFromState(state)
        val gameStatus = board.getGameStatus()
        return gameStatus.isGameOver
    }
    
    override fun getOutcome(state: ChessState): TerminalInfo {
        val board = createBoardFromState(state)
        val gameStatus = board.getGameStatus()
        
        return TerminalInfo(
            isTerminal = gameStatus.isGameOver,
            outcome = when (gameStatus) {
                GameStatus.WHITE_WINS -> GameOutcome.WHITE_WINS
                GameStatus.BLACK_WINS -> GameOutcome.BLACK_WINS
                GameStatus.DRAW_STALEMATE -> GameOutcome.DRAW
                GameStatus.DRAW_INSUFFICIENT_MATERIAL -> GameOutcome.DRAW
                GameStatus.DRAW_FIFTY_MOVE_RULE -> GameOutcome.DRAW
                GameStatus.DRAW_REPETITION -> GameOutcome.DRAW
                else -> GameOutcome.ONGOING
            },
            reason = when (gameStatus) {
                GameStatus.WHITE_WINS -> "checkmate"
                GameStatus.BLACK_WINS -> "checkmate"
                GameStatus.DRAW_STALEMATE -> "stalemate"
                GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "insufficient_material"
                GameStatus.DRAW_FIFTY_MOVE_RULE -> "fifty_move_rule"
                GameStatus.DRAW_REPETITION -> "repetition"
                GameStatus.IN_CHECK -> "in_check"
                GameStatus.ONGOING -> "ongoing"
            }
        )
    }
    
    override fun toFen(state: ChessState): String = state.fen
    
    override fun fromFen(fen: String): ChessState? {
        val board = ChessBoard()
        return if (board.fromFEN(fen)) {
            ChessState(
                fen = fen,
                activeColor = board.getActiveColor()
            )
        } else null
    }
    
    override fun perft(state: ChessState, depth: Int): Long {
        if (depth == 0) return 1L
        
        val board = createBoardFromState(state)
        return calculatePerft(board, depth)
    }
    
    override fun getEngineName(): String = "builtin"
    
    /**
     * Create ChessBoard from ChessState using FEN reconstruction
     * 
     * This method ensures that the adapter maintains immutability by reconstructing
     * the board state from the FEN string rather than maintaining mutable state.
     * 
     * @param state The ChessState to convert
     * @return ChessBoard representing the same position
     * @throws IllegalStateException if the FEN string is invalid
     */
    private fun createBoardFromState(state: ChessState): ChessBoard {
        val board = ChessBoard()
        if (!board.fromFEN(state.fen)) {
            throw IllegalStateException("Invalid FEN in ChessState: ${state.fen}")
        }
        return board
    }
    
    /**
     * Convert engine Move to adapter ChessMove
     * 
     * @param move The engine Move to convert
     * @return ChessMove representation
     */
    private fun convertToChessMove(move: Move): ChessMove {
        return ChessMove(
            from = move.from,
            to = move.to,
            promotion = move.promotion
        )
    }
    
    /**
     * Convert adapter ChessMove to engine Move
     * 
     * @param move The ChessMove to convert
     * @return Engine Move representation
     */
    private fun convertToEngineMove(move: ChessMove): Move {
        return Move(
            from = move.from,
            to = move.to,
            promotion = move.promotion
        )
    }
    
    /**
     * Calculate perft (performance test) for position validation
     * 
     * This recursive function counts all possible positions at a given depth,
     * which is useful for engine validation and performance testing.
     * 
     * @param board The current board position
     * @param depth The remaining search depth
     * @return Number of leaf nodes at the specified depth
     */
    private fun calculatePerft(board: ChessBoard, depth: Int): Long {
        if (depth == 0) return 1L
        
        var nodes = 0L
        val moves = board.getAllValidMoves(board.getActiveColor())
        
        for (move in moves) {
            val testBoard = board.copy()
            val result = testBoard.makeLegalMove(move)
            
            if (result == MoveResult.SUCCESS) {
                testBoard.switchActiveColor()
                nodes += calculatePerft(testBoard, depth - 1)
            }
        }
        
        return nodes
    }
}