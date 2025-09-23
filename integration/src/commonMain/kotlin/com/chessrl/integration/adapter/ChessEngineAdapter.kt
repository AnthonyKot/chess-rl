package com.chessrl.integration.adapter

/**
 * Chess Engine Adapter Interface
 * 
 * This interface provides a clean abstraction layer for different chess engine implementations,
 * enabling the chess RL system to work with multiple chess rule implementations while
 * maintaining the same RL API.
 * 
 * The adapter uses immutable state management with FEN-based state representation to ensure
 * thread safety and consistent behavior across different engine implementations.
 * 
 * Example usage:
 * ```kotlin
 * val adapter = BuiltinAdapter()
 * val initialState = adapter.initialState()
 * val legalMoves = adapter.getLegalMoves(initialState)
 * val newState = adapter.applyMove(initialState, legalMoves.first())
 * ```
 */
interface ChessEngineAdapter {
    
    /**
     * Get the initial chess position (standard starting position)
     * 
     * @return ChessState representing the standard chess starting position
     */
    fun initialState(): ChessState
    
    /**
     * Get all legal moves for the current position
     * 
     * @param state The current chess position
     * @return List of all legal moves available in the current position
     */
    fun getLegalMoves(state: ChessState): List<ChessMove>
    
    /**
     * Apply a move to the current state and return the new state
     * 
     * This method is immutable - it returns a new ChessState without modifying the input state.
     * 
     * @param state The current chess position
     * @param move The move to apply
     * @return New ChessState after applying the move
     * @throws IllegalArgumentException if the move is not legal in the current position
     */
    fun applyMove(state: ChessState, move: ChessMove): ChessState
    
    /**
     * Check if the current position is terminal (game over)
     * 
     * @param state The current chess position
     * @return true if the game is over (checkmate, stalemate, or draw), false otherwise
     */
    fun isTerminal(state: ChessState): Boolean
    
    /**
     * Get the game outcome and termination reason for the current position
     * 
     * @param state The current chess position
     * @return TerminalInfo containing outcome and reason
     */
    fun getOutcome(state: ChessState): TerminalInfo
    
    /**
     * Convert chess state to FEN (Forsyth-Edwards Notation) string
     * 
     * @param state The chess state to convert
     * @return FEN string representation of the position
     */
    fun toFen(state: ChessState): String
    
    /**
     * Create chess state from FEN (Forsyth-Edwards Notation) string
     * 
     * @param fen The FEN string to parse
     * @return ChessState if FEN is valid, null if invalid
     */
    fun fromFen(fen: String): ChessState?
    
    /**
     * Performance test - count leaf nodes at given depth (optional)
     * 
     * This method is used for engine validation and performance testing.
     * Implementations may return 0 if perft is not supported.
     * 
     * @param state The current chess position
     * @param depth The search depth
     * @return Number of leaf nodes at the specified depth
     */
    fun perft(state: ChessState, depth: Int): Long = 0L
    
    /**
     * Get engine identification string for logging and debugging
     * 
     * @return String identifying the engine implementation (e.g., "builtin", "chesslib")
     */
    fun getEngineName(): String
}