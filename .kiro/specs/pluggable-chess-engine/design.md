# Design Document - Pluggable Chess Engine Adapter

## Overview

This design document outlines the implementation of a pluggable chess engine adapter system that allows the chess RL bot to use different chess engine implementations while maintaining the same RL API. The system introduces a clean abstraction layer that enables side-by-side operation of the current built-in engine with a new chesslib-based implementation.

The design ensures that the original handmade engine remains permanently available as a fallback option while providing the flexibility to use external chess libraries like bhlangonijr/chesslib for enhanced rule validation and testing.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Chess RL Training System                     │
├─────────────────────────────────────────────────────────────────┤
│  ChessEnvironment (unchanged RL interface)                     │
│  ├─ State encoding (839-dimensional vectors)                   │
│  ├─ Action encoding (4096 from-to mappings)                    │
│  └─ Reward calculation                                          │
├─────────────────────────────────────────────────────────────────┤
│  EngineBackedEnvironment (new adapter-based environment)       │
│  ├─ Delegates rule logic to ChessEngineAdapter                 │
│  ├─ Maintains same RL interface as ChessEnvironment            │
│  └─ Handles action masking and reward shaping                  │
├─────────────────────────────────────────────────────────────────┤
│                  ChessEngineAdapter Interface                   │
│  ├─ initialState() -> ChessState                               │
│  ├─ getLegalMoves(state) -> List<ChessMove>                    │
│  ├─ applyMove(state, move) -> ChessState                       │
│  ├─ isTerminal(state) -> Boolean                               │
│  ├─ getOutcome(state) -> TerminalInfo                          │
│  ├─ toFen(state) / fromFen(fen) -> ChessState                  │
│  └─ perft(state, depth) -> Long (optional)                     │
├─────────────────────────────────────────────────────────────────┤
│  BuiltinAdapter              │  ChesslibAdapter                 │
│  ├─ Wraps existing engine    │  ├─ Uses bhlangonijr/chesslib   │
│  ├─ ChessBoard -> ChessState │  ├─ FEN-based immutable state   │
│  ├─ Move -> ChessMove        │  ├─ Board rehydration pattern   │
│  └─ GameStatus -> TerminalInfo│  └─ MoveGenerator integration   │
├─────────────────────────────────────────────────────────────────┤
│  Current Chess Engine        │  External Chess Library         │
│  ├─ ChessBoard.kt            │  ├─ com.github.bhlangonijr     │
│  ├─ MoveValidation.kt        │  ├─ chesslib.Board             │
│  ├─ GameStateDetection.kt    │  ├─ chesslib.MoveGenerator     │
│  └─ (unchanged)              │  └─ chesslib.game.GameResult   │
└─────────────────────────────────────────────────────────────────┘
```

### Engine Selection Mechanism

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLI / Configuration                        │
│  --engine builtin|chesslib  OR  profile.yaml: engine: chesslib │
├─────────────────────────────────────────────────────────────────┤
│                       EngineSelector                            │
│  ├─ parseEngineFlag(args) -> EngineType                        │
│  ├─ createAdapter(type) -> ChessEngineAdapter                   │
│  └─ validateEngine(adapter) -> Boolean                          │
├─────────────────────────────────────────────────────────────────┤
│                    TrainingPipeline                             │
│  ├─ Uses EngineBackedEnvironment(adapter)                      │
│  ├─ Same training logic regardless of engine                    │
│  └─ Engine choice transparent to RL algorithms                  │
└─────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### Core Data Types

```kotlin
/**
 * Immutable chess state representation for adapter interface
 */
data class ChessState(
    val fen: String,
    val activeColor: PieceColor,
    val legalMoves: List<ChessMove>? = null // Cached for performance
) {
    fun withLegalMoves(moves: List<ChessMove>): ChessState = 
        copy(legalMoves = moves)
}

/**
 * Standardized chess move representation
 */
data class ChessMove(
    val from: Position,
    val to: Position,
    val promotion: PieceType? = null,
    val algebraic: String = "${from.toAlgebraic()}${to.toAlgebraic()}${promotion?.let { it.name.lowercase().first() } ?: ""}"
) {
    companion object {
        fun fromAlgebraic(algebraic: String): ChessMove? {
            // Parse algebraic notation like "e2e4" or "e7e8q"
            if (algebraic.length < 4) return null
            val from = Position.fromAlgebraic(algebraic.substring(0, 2)) ?: return null
            val to = Position.fromAlgebraic(algebraic.substring(2, 4)) ?: return null
            val promotion = if (algebraic.length == 5) {
                when (algebraic[4].lowercase()) {
                    'q' -> PieceType.QUEEN
                    'r' -> PieceType.ROOK
                    'b' -> PieceType.BISHOP
                    'n' -> PieceType.KNIGHT
                    else -> null
                }
            } else null
            return ChessMove(from, to, promotion)
        }
    }
}

/**
 * Terminal game information with standardized outcomes
 */
data class TerminalInfo(
    val isTerminal: Boolean,
    val outcome: GameOutcome,
    val reason: String
)

enum class GameOutcome {
    WHITE_WINS, BLACK_WINS, DRAW, ONGOING
}
```

### ChessEngineAdapter Interface

```kotlin
/**
 * Abstract interface for chess engine implementations
 * Provides immutable state management and standardized chess operations
 */
interface ChessEngineAdapter {
    /**
     * Get initial chess position
     */
    fun initialState(): ChessState
    
    /**
     * Get all legal moves for current position
     */
    fun getLegalMoves(state: ChessState): List<ChessMove>
    
    /**
     * Apply move and return new state (immutable)
     */
    fun applyMove(state: ChessState, move: ChessMove): ChessState
    
    /**
     * Check if position is terminal (game over)
     */
    fun isTerminal(state: ChessState): Boolean
    
    /**
     * Get game outcome and termination reason
     */
    fun getOutcome(state: ChessState): TerminalInfo
    
    /**
     * Convert state to FEN notation
     */
    fun toFen(state: ChessState): String
    
    /**
     * Create state from FEN notation
     */
    fun fromFen(fen: String): ChessState?
    
    /**
     * Performance test - count leaf nodes at given depth (optional)
     */
    fun perft(state: ChessState, depth: Int): Long = 0L
    
    /**
     * Engine identification for logging and debugging
     */
    fun getEngineName(): String
}
```

### BuiltinAdapter Implementation

```kotlin
/**
 * Adapter that wraps the existing chess engine without modification
 * Maintains 100% compatibility with current engine behavior
 */
class BuiltinAdapter : ChessEngineAdapter {
    
    override fun initialState(): ChessState {
        val board = ChessBoard() // Uses existing ChessBoard initialization
        return ChessState(
            fen = board.toFEN(),
            activeColor = board.getActiveColor()
        )
    }
    
    override fun getLegalMoves(state: ChessState): List<ChessMove> {
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
                GameStatus.DRAW_STALEMATE, GameStatus.DRAW_INSUFFICIENT_MATERIAL,
                GameStatus.DRAW_FIFTY_MOVE_RULE, GameStatus.DRAW_REPETITION -> GameOutcome.DRAW
                else -> GameOutcome.ONGOING
            },
            reason = gameStatus.name
        )
    }
    
    override fun toFen(state: ChessState): String = state.fen
    
    override fun fromFen(fen: String): ChessState? {
        val board = ChessBoard()
        return if (board.fromFEN(fen)) {
            ChessState(fen = fen, activeColor = board.getActiveColor())
        } else null
    }
    
    override fun getEngineName(): String = "builtin"
    
    /**
     * Create ChessBoard from ChessState (FEN-based reconstruction)
     */
    private fun createBoardFromState(state: ChessState): ChessBoard {
        val board = ChessBoard()
        if (!board.fromFEN(state.fen)) {
            throw IllegalStateException("Invalid FEN in ChessState: ${state.fen}")
        }
        return board
    }
    
    /**
     * Convert engine Move to ChessMove
     */
    private fun convertToChessMove(move: Move): ChessMove {
        return ChessMove(
            from = move.from,
            to = move.to,
            promotion = move.promotion
        )
    }
    
    /**
     * Convert ChessMove to engine Move
     */
    private fun convertToEngineMove(move: ChessMove): Move {
        return Move(
            from = move.from,
            to = move.to,
            promotion = move.promotion
        )
    }
}
```

### ChesslibAdapter Implementation

```kotlin
/**
 * Adapter implementation using bhlangonijr/chesslib
 * Provides FEN-based immutable state management with external library validation
 */
class ChesslibAdapter : ChessEngineAdapter {
    
    override fun initialState(): ChessState {
        val board = Board()
        return ChessState(
            fen = board.fen,
            activeColor = if (board.sideToMove == Side.WHITE) PieceColor.WHITE else PieceColor.BLACK
        )
    }
    
    override fun getLegalMoves(state: ChessState): List<ChessMove> {
        // Use cached moves if available for performance
        state.legalMoves?.let { return it }
        
        val board = Board()
        board.loadFromFen(state.fen)
        
        val legalMoves = MoveGenerator.generateLegalMoves(board)
        return legalMoves.map { convertToChessMove(it) }
    }
    
    override fun applyMove(state: ChessState, move: ChessMove): ChessState {
        val board = Board()
        board.loadFromFen(state.fen)
        
        val chesslibMove = convertToChesslibMove(move, board)
        if (!board.legalMoves().contains(chesslibMove)) {
            throw IllegalArgumentException("Invalid move: ${move.algebraic}")
        }
        
        board.doMove(chesslibMove)
        
        return ChessState(
            fen = board.fen,
            activeColor = if (board.sideToMove == Side.WHITE) PieceColor.WHITE else PieceColor.BLACK
        )
    }
    
    override fun isTerminal(state: ChessState): Boolean {
        val board = Board()
        board.loadFromFen(state.fen)
        
        return board.isMated || board.isStaleMate || 
               board.isDraw || board.isRepetition || 
               board.halfMoveCounter >= 100
    }
    
    override fun getOutcome(state: ChessState): TerminalInfo {
        val board = Board()
        board.loadFromFen(state.fen)
        
        val (isTerminal, outcome, reason) = when {
            board.isMated -> {
                val winner = if (board.sideToMove == Side.WHITE) GameOutcome.BLACK_WINS else GameOutcome.WHITE_WINS
                Triple(true, winner, "checkmate")
            }
            board.isStaleMate -> Triple(true, GameOutcome.DRAW, "stalemate")
            board.isInsufficientMaterial -> Triple(true, GameOutcome.DRAW, "insufficient_material")
            board.halfMoveCounter >= 100 -> Triple(true, GameOutcome.DRAW, "fifty_move_rule")
            board.isRepetition -> Triple(true, GameOutcome.DRAW, "repetition")
            board.isDraw -> Triple(true, GameOutcome.DRAW, "draw")
            else -> Triple(false, GameOutcome.ONGOING, "ongoing")
        }
        
        return TerminalInfo(isTerminal, outcome, reason)
    }
    
    override fun toFen(state: ChessState): String = state.fen
    
    override fun fromFen(fen: String): ChessState? {
        return try {
            val board = Board()
            board.loadFromFen(fen)
            ChessState(
                fen = fen,
                activeColor = if (board.sideToMove == Side.WHITE) PieceColor.WHITE else PieceColor.BLACK
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun perft(state: ChessState, depth: Int): Long {
        val board = Board()
        board.loadFromFen(state.fen)
        return calculatePerft(board, depth)
    }
    
    override fun getEngineName(): String = "chesslib"
    
    /**
     * Convert chesslib Move to ChessMove
     */
    private fun convertToChessMove(move: com.github.bhlangonijr.chesslib.move.Move): ChessMove {
        val from = Position(
            rank = move.from.rank.ordinal,
            file = move.from.file.ordinal
        )
        val to = Position(
            rank = move.to.rank.ordinal,
            file = move.to.file.ordinal
        )
        val promotion = when (move.promotion) {
            com.github.bhlangonijr.chesslib.Piece.WHITE_QUEEN,
            com.github.bhlangonijr.chesslib.Piece.BLACK_QUEEN -> PieceType.QUEEN
            com.github.bhlangonijr.chesslib.Piece.WHITE_ROOK,
            com.github.bhlangonijr.chesslib.Piece.BLACK_ROOK -> PieceType.ROOK
            com.github.bhlangonijr.chesslib.Piece.WHITE_BISHOP,
            com.github.bhlangonijr.chesslib.Piece.BLACK_BISHOP -> PieceType.BISHOP
            com.github.bhlangonijr.chesslib.Piece.WHITE_KNIGHT,
            com.github.bhlangonijr.chesslib.Piece.BLACK_KNIGHT -> PieceType.KNIGHT
            else -> null
        }
        
        return ChessMove(from, to, promotion)
    }
    
    /**
     * Convert ChessMove to chesslib Move
     */
    private fun convertToChesslibMove(
        move: ChessMove, 
        board: Board
    ): com.github.bhlangonijr.chesslib.move.Move {
        val from = Square.encode(
            com.github.bhlangonijr.chesslib.Rank.values()[move.from.rank],
            com.github.bhlangonijr.chesslib.File.values()[move.from.file]
        )
        val to = Square.encode(
            com.github.bhlangonijr.chesslib.Rank.values()[move.to.rank],
            com.github.bhlangonijr.chesslib.File.values()[move.to.file]
        )
        
        // Find matching legal move (handles promotion disambiguation)
        val legalMoves = board.legalMoves()
        return legalMoves.find { it.from == from && it.to == to } 
            ?: throw IllegalArgumentException("No legal move found for ${move.algebraic}")
    }
    
    /**
     * Calculate perft (performance test) for position validation
     */
    private fun calculatePerft(board: Board, depth: Int): Long {
        if (depth == 0) return 1L
        
        var nodes = 0L
        val moves = board.legalMoves()
        
        for (move in moves) {
            board.doMove(move)
            nodes += calculatePerft(board, depth - 1)
            board.undoMove()
        }
        
        return nodes
    }
}
```

### EngineBackedEnvironment

```kotlin
/**
 * Chess environment that delegates rule logic to ChessEngineAdapter
 * Maintains same interface as ChessEnvironment for RL compatibility
 */
class EngineBackedEnvironment(
    private val adapter: ChessEngineAdapter,
    private val rewardConfig: ChessRewardConfig = ChessRewardConfig()
) : Environment<DoubleArray, Int> {
    
    private var currentState: ChessState = adapter.initialState()
    private val stateEncoder = ChessStateEncoder()
    private val actionEncoder = ChessActionEncoder()
    private val gameHistory = mutableListOf<String>()
    
    override fun reset(): DoubleArray {
        currentState = adapter.initialState()
        gameHistory.clear()
        gameHistory.add(adapter.toFen(currentState))
        
        return encodeStateForRL(currentState)
    }
    
    override fun step(action: Int): StepResult<DoubleArray> {
        // Decode action to move
        val baseMove = actionEncoder.decodeAction(action)
        val chessMoves = adapter.getLegalMoves(currentState)
        
        // Find matching legal move (handles promotions)
        val actualMove = findMatchingMove(baseMove, chessMoves)
        
        if (actualMove == null) {
            return StepResult(
                nextState = encodeStateForRL(currentState),
                reward = rewardConfig.invalidMoveReward,
                done = false,
                info = mapOf("error" to "Invalid move", "engine" to adapter.getEngineName())
            )
        }
        
        // Apply move using adapter
        currentState = adapter.applyMove(currentState, actualMove)
        gameHistory.add(adapter.toFen(currentState))
        
        // Check terminal state and calculate reward
        val terminalInfo = adapter.getOutcome(currentState)
        val reward = calculateReward(terminalInfo, actualMove)
        
        return StepResult(
            nextState = encodeStateForRL(currentState),
            reward = reward,
            done = terminalInfo.isTerminal,
            info = mapOf(
                "move" to actualMove.algebraic,
                "outcome" to terminalInfo.reason,
                "engine" to adapter.getEngineName(),
                "fen" to adapter.toFen(currentState)
            )
        )
    }
    
    /**
     * Encode ChessState for RL using existing ChessBoard conversion
     */
    private fun encodeStateForRL(state: ChessState): DoubleArray {
        // Convert ChessState back to ChessBoard for encoding compatibility
        val board = ChessBoard()
        board.fromFEN(state.fen)
        return stateEncoder.encode(board)
    }
    
    /**
     * Find legal move matching decoded action
     */
    private fun findMatchingMove(baseMove: ChessMove, legalMoves: List<ChessMove>): ChessMove? {
        // Exact match first
        val exactMatch = legalMoves.find { 
            it.from == baseMove.from && it.to == baseMove.to && it.promotion == null 
        }
        if (exactMatch != null) return exactMatch
        
        // Default to queen promotion for pawn promotions
        return legalMoves.find { 
            it.from == baseMove.from && it.to == baseMove.to && it.promotion == PieceType.QUEEN 
        } ?: legalMoves.find { 
            it.from == baseMove.from && it.to == baseMove.to 
        }
    }
    
    /**
     * Calculate reward based on terminal info
     */
    private fun calculateReward(terminalInfo: TerminalInfo, move: ChessMove): Double {
        if (!terminalInfo.isTerminal) {
            return rewardConfig.stepPenalty
        }
        
        return when (terminalInfo.outcome) {
            GameOutcome.WHITE_WINS -> if (currentState.activeColor == PieceColor.BLACK) rewardConfig.winReward else rewardConfig.lossReward
            GameOutcome.BLACK_WINS -> if (currentState.activeColor == PieceColor.WHITE) rewardConfig.winReward else rewardConfig.lossReward
            GameOutcome.DRAW -> rewardConfig.drawReward
            GameOutcome.ONGOING -> 0.0
        }
    }
}
```

### Engine Selection and Configuration

```kotlin
/**
 * Engine type enumeration
 */
enum class EngineType {
    BUILTIN, CHESSLIB;
    
    companion object {
        fun fromString(name: String): EngineType? {
            return values().find { it.name.lowercase() == name.lowercase() }
        }
    }
}

/**
 * Engine selector and factory
 */
object EngineSelector {
    
    /**
     * Create adapter based on engine type
     */
    fun createAdapter(engineType: EngineType): ChessEngineAdapter {
        return when (engineType) {
            EngineType.BUILTIN -> BuiltinAdapter()
            EngineType.CHESSLIB -> ChesslibAdapter()
        }
    }
    
    /**
     * Parse engine selection from CLI arguments
     */
    fun parseEngineFromArgs(args: Array<String>): EngineType {
        val engineIndex = args.indexOf("--engine")
        if (engineIndex >= 0 && engineIndex + 1 < args.size) {
            val engineName = args[engineIndex + 1]
            return EngineType.fromString(engineName) ?: EngineType.BUILTIN
        }
        return EngineType.BUILTIN
    }
    
    /**
     * Parse engine selection from profile configuration
     */
    fun parseEngineFromProfile(profileConfig: Map<String, Any>): EngineType {
        val engineName = profileConfig["engine"] as? String
        return if (engineName != null) {
            EngineType.fromString(engineName) ?: EngineType.BUILTIN
        } else {
            EngineType.BUILTIN
        }
    }
    
    /**
     * Validate engine adapter functionality
     */
    fun validateAdapter(adapter: ChessEngineAdapter): Boolean {
        return try {
            // Basic validation: starting position should have 20 legal moves
            val initialState = adapter.initialState()
            val legalMoves = adapter.getLegalMoves(initialState)
            
            if (legalMoves.size != 20) {
                println("Warning: ${adapter.getEngineName()} engine reports ${legalMoves.size} legal moves in starting position (expected 20)")
                return false
            }
            
            // Test e2e4 move
            val e2e4 = ChessMove.fromAlgebraic("e2e4")
            if (e2e4 != null && legalMoves.contains(e2e4)) {
                val newState = adapter.applyMove(initialState, e2e4)
                val expectedFen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
                if (adapter.toFen(newState) != expectedFen) {
                    println("Warning: ${adapter.getEngineName()} engine produces incorrect FEN after e2e4")
                    return false
                }
            }
            
            true
        } catch (e: Exception) {
            println("Error validating ${adapter.getEngineName()} engine: ${e.message}")
            false
        }
    }
}
```

## Action Space Mapping

### 4096 From-To Action Space

The system uses a simplified 4096-action space where each action represents a from-square to to-square move:

```kotlin
/**
 * Action space mapping for chess moves
 * Maps 64x64 = 4096 possible from-to combinations to action indices
 */
object ActionSpaceMapping {
    const val BOARD_SIZE = 64
    const val ACTION_SPACE_SIZE = BOARD_SIZE * BOARD_SIZE
    
    /**
     * Encode move to action index: from_square * 64 + to_square
     */
    fun encodeMove(move: ChessMove): Int {
        val fromIndex = move.from.rank * 8 + move.from.file
        val toIndex = move.to.rank * 8 + move.to.file
        return fromIndex * BOARD_SIZE + toIndex
    }
    
    /**
     * Decode action index to move (promotion handling in move matching)
     */
    fun decodeAction(actionIndex: Int): ChessMove {
        val fromIndex = actionIndex / BOARD_SIZE
        val toIndex = actionIndex % BOARD_SIZE
        
        return ChessMove(
            from = Position(fromIndex / 8, fromIndex % 8),
            to = Position(toIndex / 8, toIndex % 8)
        )
    }
}
```

### Promotion Handling Strategy

For the initial implementation, promotions are handled by defaulting to queen promotion when a pawn reaches the promotion rank. This simplifies the action space while maintaining functionality:

1. **Phase 1**: Default queen promotion for all pawn promotions
2. **Phase 2**: Expand action space or use post-policy selection for specific promotion pieces
3. **Future**: Implement full 4096 + promotion actions or heuristic promotion selection

## Error Handling

### Adapter Error Recovery

```kotlin
/**
 * Error handling wrapper for chess engine adapters
 */
class SafeChessEngineAdapter(
    private val primary: ChessEngineAdapter,
    private val fallback: ChessEngineAdapter? = null
) : ChessEngineAdapter {
    
    override fun getLegalMoves(state: ChessState): List<ChessMove> {
        return try {
            primary.getLegalMoves(state)
        } catch (e: Exception) {
            println("Primary engine ${primary.getEngineName()} failed: ${e.message}")
            fallback?.getLegalMoves(state) ?: emptyList()
        }
    }
    
    override fun applyMove(state: ChessState, move: ChessMove): ChessState {
        return try {
            primary.applyMove(state, move)
        } catch (e: Exception) {
            println("Primary engine ${primary.getEngineName()} failed to apply move: ${e.message}")
            fallback?.applyMove(state, move) ?: throw e
        }
    }
    
    // Similar error handling for other methods...
}
```

### Invalid Move Handling

The system provides graceful handling of invalid moves:

1. **Action Masking**: Only legal moves are presented to the RL agent
2. **Fallback Selection**: If decoded action is invalid, select random legal move
3. **Penalty System**: Invalid moves receive negative reward but don't crash training
4. **Logging**: All invalid moves are logged with engine information for debugging

## Testing Strategy

### Parity Testing Framework

```kotlin
/**
 * Cross-engine validation for ensuring identical behavior
 */
class EngineParityTester {
    
    fun testBasicParity(engine1: ChessEngineAdapter, engine2: ChessEngineAdapter): ParityResult {
        val results = mutableListOf<String>()
        
        // Test starting position
        val state1 = engine1.initialState()
        val state2 = engine2.initialState()
        
        if (engine1.toFen(state1) != engine2.toFen(state2)) {
            results.add("Different starting positions")
        }
        
        // Test legal move count
        val moves1 = engine1.getLegalMoves(state1)
        val moves2 = engine2.getLegalMoves(state2)
        
        if (moves1.size != moves2.size) {
            results.add("Different legal move counts: ${moves1.size} vs ${moves2.size}")
        }
        
        // Test e2e4 application
        val e2e4 = ChessMove.fromAlgebraic("e2e4")!!
        val newState1 = engine1.applyMove(state1, e2e4)
        val newState2 = engine2.applyMove(state2, e2e4)
        
        if (engine1.toFen(newState1) != engine2.toFen(newState2)) {
            results.add("Different FEN after e2e4")
        }
        
        return ParityResult(results.isEmpty(), results)
    }
    
    fun testComplexPositions(engine1: ChessEngineAdapter, engine2: ChessEngineAdapter): ParityResult {
        val testPositions = listOf(
            "rnbqkb1r/pppp1ppp/5n2/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR w KQkq - 2 3", // Italian Game
            "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3", // Italian Game
            "8/8/8/8/8/8/8/4K2k w - - 0 1" // King vs King endgame
        )
        
        val results = mutableListOf<String>()
        
        for (fen in testPositions) {
            val state1 = engine1.fromFen(fen)
            val state2 = engine2.fromFen(fen)
            
            if (state1 == null || state2 == null) {
                results.add("Failed to parse FEN: $fen")
                continue
            }
            
            val moves1 = engine1.getLegalMoves(state1)
            val moves2 = engine2.getLegalMoves(state2)
            
            if (moves1.size != moves2.size) {
                results.add("Move count mismatch for $fen: ${moves1.size} vs ${moves2.size}")
            }
            
            val terminal1 = engine1.isTerminal(state1)
            val terminal2 = engine2.isTerminal(state2)
            
            if (terminal1 != terminal2) {
                results.add("Terminal status mismatch for $fen: $terminal1 vs $terminal2")
            }
        }
        
        return ParityResult(results.isEmpty(), results)
    }
}

data class ParityResult(val passed: Boolean, val errors: List<String>)
```

### Performance Testing

```kotlin
/**
 * Performance comparison between engines
 */
class EnginePerformanceTester {
    
    fun benchmarkLegalMoveGeneration(adapter: ChessEngineAdapter, iterations: Int = 1000): BenchmarkResult {
        val state = adapter.initialState()
        
        val startTime = System.nanoTime()
        repeat(iterations) {
            adapter.getLegalMoves(state)
        }
        val endTime = System.nanoTime()
        
        val avgTimeNanos = (endTime - startTime) / iterations
        return BenchmarkResult(
            operation = "getLegalMoves",
            engine = adapter.getEngineName(),
            avgTimeNanos = avgTimeNanos,
            iterations = iterations
        )
    }
    
    fun benchmarkMoveApplication(adapter: ChessEngineAdapter, iterations: Int = 1000): BenchmarkResult {
        val state = adapter.initialState()
        val moves = adapter.getLegalMoves(state)
        val testMove = moves.first()
        
        val startTime = System.nanoTime()
        repeat(iterations) {
            adapter.applyMove(state, testMove)
        }
        val endTime = System.nanoTime()
        
        val avgTimeNanos = (endTime - startTime) / iterations
        return BenchmarkResult(
            operation = "applyMove",
            engine = adapter.getEngineName(),
            avgTimeNanos = avgTimeNanos,
            iterations = iterations
        )
    }
}

data class BenchmarkResult(
    val operation: String,
    val engine: String,
    val avgTimeNanos: Long,
    val iterations: Int
) {
    val avgTimeMicros: Double get() = avgTimeNanos / 1000.0
    val avgTimeMillis: Double get() = avgTimeNanos / 1_000_000.0
}
```

## Migration Strategy

### Phase 1: Foundation (Milestone 1-2)
1. Implement ChessEngineAdapter interface and data types
2. Create BuiltinAdapter wrapping existing engine
3. Implement basic ChesslibAdapter
4. Add engine selection mechanism
5. Validate basic parity between engines

### Phase 2: Integration (Milestone 3-4)
1. Create EngineBackedEnvironment
2. Integrate with existing training pipeline
3. Add comprehensive testing suite
4. Implement performance benchmarking
5. Validate training effectiveness with both engines

### Phase 3: Enhancement (Milestone 5-7)
1. Add repetition detection
2. Optimize performance (caching, reuse patterns)
3. Expand promotion handling
4. Add observability and debugging tools
5. Complete feature gap analysis

### Phase 4: Production (Milestone 8+)
1. Comprehensive validation testing
2. Performance optimization
3. Documentation and examples
4. CI/CD integration for dual-engine testing
5. Long-term maintenance planning

## Performance Considerations

### Optimization Strategies

1. **State Caching**: Cache legal moves in ChessState for repeated access
2. **FEN Reuse**: Minimize FEN parsing by reusing Board objects where safe
3. **Move Validation**: Pre-validate moves during generation to avoid runtime checks
4. **Memory Management**: Use object pooling for frequently created objects

### Thread Safety

- **BuiltinAdapter**: Thread-safe through immutable state and FEN reconstruction
- **ChesslibAdapter**: Thread-safe through per-call Board instantiation
- **Future Optimization**: Consider thread-local Board pools for high-throughput scenarios

This design provides a robust foundation for pluggable chess engines while maintaining backward compatibility and enabling comprehensive testing and validation of different chess rule implementations.