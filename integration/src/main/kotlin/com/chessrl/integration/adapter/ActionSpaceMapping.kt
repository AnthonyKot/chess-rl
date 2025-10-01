package com.chessrl.integration.adapter

import com.chessrl.chess.Position
import com.chessrl.chess.PieceType

/**
 * Action Space Mapping for Chess Moves
 * 
 * This class provides encoding and decoding between chess moves and action indices
 * for the 4096 from-to action space used by the neural network.
 * 
 * The action space uses a simple 64x64 mapping where each action represents
 * a from-square to to-square move. Promotions are handled by defaulting to
 * queen promotion in the initial implementation.
 */
object ActionSpaceMapping {
    
    /** Size of the chess board (8x8 = 64 squares) */
    const val BOARD_SIZE = 64
    
    /** Total action space size (64 from squares × 64 to squares) */
    const val ACTION_SPACE_SIZE = BOARD_SIZE * BOARD_SIZE
    
    /**
     * Encode a chess move to an action index
     * 
     * The encoding uses the formula: from_square_index * 64 + to_square_index
     * where square indices are calculated as: rank * 8 + file
     * 
     * @param move The chess move to encode
     * @return Action index in range [0, 4095]
     */
    fun encodeMove(move: ChessMove): Int {
        val fromIndex = positionToIndex(move.from)
        val toIndex = positionToIndex(move.to)
        return fromIndex * BOARD_SIZE + toIndex
    }
    
    /**
     * Decode an action index to a chess move
     * 
     * Note: This creates a basic move without promotion information.
     * Promotion handling is done during move matching in the environment.
     * 
     * @param actionIndex The action index to decode (must be in range [0, 4095])
     * @return ChessMove representing the from-to movement
     * @throws IllegalArgumentException if actionIndex is out of range
     */
    fun decodeAction(actionIndex: Int): ChessMove {
        if (actionIndex < 0 || actionIndex >= ACTION_SPACE_SIZE) {
            throw IllegalArgumentException("Action index $actionIndex out of range [0, ${ACTION_SPACE_SIZE - 1}]")
        }
        
        val fromIndex = actionIndex / BOARD_SIZE
        val toIndex = actionIndex % BOARD_SIZE
        
        return ChessMove(
            from = indexToPosition(fromIndex),
            to = indexToPosition(toIndex)
        )
    }
    
    /**
     * Create action mask for legal moves
     * 
     * This creates a boolean array where true indicates a legal action.
     * Used by RL agents to avoid selecting invalid moves.
     * 
     * @param legalMoves List of legal moves in the current position
     * @return Boolean array of size ACTION_SPACE_SIZE with legal actions marked as true
     */
    fun createActionMask(legalMoves: List<ChessMove>): BooleanArray {
        val mask = BooleanArray(ACTION_SPACE_SIZE) { false }
        
        for (move in legalMoves) {
            val actionIndex = encodeMove(move)
            mask[actionIndex] = true
        }
        
        return mask
    }
    
    /**
     * Get all legal action indices for the given moves
     * 
     * @param legalMoves List of legal moves in the current position
     * @return List of action indices corresponding to legal moves
     */
    fun getLegalActionIndices(legalMoves: List<ChessMove>): List<Int> {
        return legalMoves.map { encodeMove(it) }
    }
    
    /**
     * Find matching legal move for a decoded action
     * 
     * This handles promotion disambiguation by finding the legal move that matches
     * the from-to squares of the decoded action. For pawn promotions, it defaults
     * to queen promotion if multiple promotion options exist.
     * 
     * @param decodedMove The basic move decoded from action index
     * @param legalMoves List of legal moves in the current position
     * @return Matching legal move, or null if no match found
     */
    fun findMatchingMove(decodedMove: ChessMove, legalMoves: List<ChessMove>): ChessMove? {
        // First try exact match (including promotion)
        val exactMatch = legalMoves.find { 
            it.from == decodedMove.from && 
            it.to == decodedMove.to && 
            it.promotion == decodedMove.promotion 
        }
        if (exactMatch != null) return exactMatch
        
        // Find moves with matching from-to squares
        val candidateMoves = legalMoves.filter { 
            it.from == decodedMove.from && it.to == decodedMove.to 
        }
        
        if (candidateMoves.isEmpty()) return null
        if (candidateMoves.size == 1) return candidateMoves.first()
        
        // Multiple candidates - prefer queen promotion for pawn promotions
        val queenPromotion = candidateMoves.find { it.promotion == PieceType.QUEEN }
        if (queenPromotion != null) return queenPromotion
        
        // Return first candidate if no queen promotion found
        return candidateMoves.first()
    }
    
    /**
     * Convert position to square index
     * 
     * @param position Chess board position
     * @return Square index in range [0, 63]
     */
    private fun positionToIndex(position: Position): Int {
        return position.rank * 8 + position.file
    }
    
    /**
     * Convert square index to position
     * 
     * @param index Square index in range [0, 63]
     * @return Chess board position
     */
    private fun indexToPosition(index: Int): Position {
        val rank = index / 8
        val file = index % 8
        return Position(rank, file)
    }
    
    /**
     * Validate that an action index is in the valid range
     * 
     * @param actionIndex The action index to validate
     * @return true if the action index is valid
     */
    fun isValidActionIndex(actionIndex: Int): Boolean {
        return actionIndex in 0 until ACTION_SPACE_SIZE
    }
    
    /**
     * Get human-readable description of an action
     * 
     * @param actionIndex The action index to describe
     * @return String description like "e2 to e4"
     */
    fun describeAction(actionIndex: Int): String {
        if (!isValidActionIndex(actionIndex)) {
            return "Invalid action index: $actionIndex"
        }
        
        val move = decodeAction(actionIndex)
        return "${move.from.toAlgebraic()} to ${move.to.toAlgebraic()}"
    }
    
    /**
     * Promotion handling strategy enumeration
     * 
     * Defines different strategies for handling pawn promotions in the action space.
     */
    enum class PromotionStrategy {
        /** Always promote to queen (default strategy) */
        QUEEN_DEFAULT,
        
        /** Use separate action space for promotions (future enhancement) */
        SEPARATE_ACTIONS,
        
        /** Use post-policy heuristics for promotion selection (future enhancement) */
        POST_POLICY_HEURISTIC
    }
    
    /**
     * Configuration for action space mapping behavior
     * 
     * @property promotionStrategy Strategy for handling pawn promotions
     * @property validateMoves Whether to validate moves during encoding/decoding
     */
    data class ActionSpaceConfig(
        val promotionStrategy: PromotionStrategy = PromotionStrategy.QUEEN_DEFAULT,
        val validateMoves: Boolean = true
    )
}