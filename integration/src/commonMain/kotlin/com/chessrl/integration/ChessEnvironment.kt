package com.chessrl.integration

import com.chessrl.chess.*
import com.chessrl.rl.*

/**
 * Extension function to get opposite color
 */
fun PieceColor.opposite(): PieceColor = when (this) {
    PieceColor.WHITE -> PieceColor.BLACK
    PieceColor.BLACK -> PieceColor.WHITE
}

/**
 * Integration Package - Connects chess engine with RL framework
 * This package provides the integration layer that allows RL agents
 * to interact with the chess environment.
 */

/**
 * Chess state encoder that converts board positions to neural network input format
 * 
 * State encoding specification (aligned with chess-engine FeatureEncoding):
 * - 8x8x12 piece planes (6 piece types × 2 colors) = 768 features
 * - Side to move (1 when White, 0 when Black) = 1 feature
 * - Castling rights (white/black kingside/queenside) = 4 features
 * - En passant target square one-hot (64) = 64 features
 * - Halfmove clock (normalized 0-1) = 1 feature
 * - Fullmove number (normalized 0-1) = 1 feature
 * Total: 768 + 1 + 4 + 64 + 2 = 839 features
 */
class ChessStateEncoder {
    companion object {
        const val BOARD_PLANES = 768 // 8x8x12
        const val ACTIVE_COLOR_FEATURES = 1
        const val CASTLING_FEATURES = 4
        const val EN_PASSANT_FEATURES = 64
        const val HALFMOVE_FEATURES = 1
        const val FULLMOVE_FEATURES = 1
        const val TOTAL_FEATURES = BOARD_PLANES + ACTIVE_COLOR_FEATURES + CASTLING_FEATURES +
                                  EN_PASSANT_FEATURES + HALFMOVE_FEATURES + FULLMOVE_FEATURES
    }
    
    /**
     * Encode chess board position to neural network input format
     */
    fun encode(board: ChessBoard): DoubleArray {
        val state = DoubleArray(TOTAL_FEATURES)
        var index = 0
        
        // Encode piece planes (8x8x12)
        index = encodePiecePlanes(board, state, index)
        
        // Encode game state features
        encodeGameStateFeatures(board, state, index)
        
        return state
    }
    
    /**
     * Encode piece positions as 12 planes (6 piece types × 2 colors)
     */
    private fun encodePiecePlanes(board: ChessBoard, state: DoubleArray, startIndex: Int): Int {
        var index = startIndex
        
        // Order: White pieces first, then black pieces
        // For each color: Pawn, Rook, Knight, Bishop, Queen, King
        val pieceOrder = listOf(
            Pair(PieceColor.WHITE, PieceType.PAWN),
            Pair(PieceColor.WHITE, PieceType.ROOK),
            Pair(PieceColor.WHITE, PieceType.KNIGHT),
            Pair(PieceColor.WHITE, PieceType.BISHOP),
            Pair(PieceColor.WHITE, PieceType.QUEEN),
            Pair(PieceColor.WHITE, PieceType.KING),
            Pair(PieceColor.BLACK, PieceType.PAWN),
            Pair(PieceColor.BLACK, PieceType.ROOK),
            Pair(PieceColor.BLACK, PieceType.KNIGHT),
            Pair(PieceColor.BLACK, PieceType.BISHOP),
            Pair(PieceColor.BLACK, PieceType.QUEEN),
            Pair(PieceColor.BLACK, PieceType.KING)
        )
        
        for ((color, pieceType) in pieceOrder) {
            // Encode 8x8 plane for this piece type and color
            for (rank in 0..7) {
                for (file in 0..7) {
                    val piece = board.getPieceAt(Position(rank, file))
                    state[index] = if (piece?.color == color && piece.type == pieceType) 1.0 else 0.0
                    index++
                }
            }
        }
        
        return index
    }
    
    /**
     * Encode game state features (castling, en passant, active color, clocks)
     */
    private fun encodeGameStateFeatures(board: ChessBoard, state: DoubleArray, startIndex: Int): Int {
        var index = startIndex
        val gameState = board.getGameState()

        // Active color (1 feature): 1.0 for White, 0.0 for Black
        state[index++] = if (gameState.activeColor == PieceColor.WHITE) 1.0 else 0.0

        // Castling rights (4 features)
        state[index++] = if (gameState.whiteCanCastleKingside) 1.0 else 0.0
        state[index++] = if (gameState.whiteCanCastleQueenside) 1.0 else 0.0
        state[index++] = if (gameState.blackCanCastleKingside) 1.0 else 0.0
        state[index++] = if (gameState.blackCanCastleQueenside) 1.0 else 0.0

        // En passant one-hot over 64 squares
        val ep = gameState.enPassantTarget
        for (rank in 0..7) {
            for (file in 0..7) {
                val isEp = ep != null && ep.rank == rank && ep.file == file
                state[index++] = if (isEp) 1.0 else 0.0
            }
        }

        // Halfmove clock (1 feature, normalized)
        state[index++] = gameState.halfmoveClock.coerceIn(0, 100).toDouble() / 100.0

        // Fullmove number (1 feature, normalized)
        state[index++] = gameState.fullmoveNumber.coerceAtLeast(1).coerceAtMost(200).toDouble() / 200.0

        return index
    }
}

/**
 * Chess action encoder/decoder for mapping between moves and neural network outputs
 * 
 * Action encoding specification:
 * - From square (0-63) × To square (0-63) = 4096 base moves
 * - Promotion moves: additional encoding for queen, rook, bishop, knight promotions
 * - Total action space: 4096 (simplified, promotions handled by move validation)
 */
class ChessActionEncoder {
    companion object {
        const val BOARD_SIZE = 64
        const val ACTION_SPACE_SIZE = BOARD_SIZE * BOARD_SIZE // 4096
    }
    
    /**
     * Encode a chess move to action index
     */
    fun encodeMove(move: Move): Int {
        val fromIndex = move.from.rank * 8 + move.from.file
        val toIndex = move.to.rank * 8 + move.to.file
        return fromIndex * BOARD_SIZE + toIndex
    }
    
    /**
     * Decode action index to chess move (without promotion info)
     */
    fun decodeAction(actionIndex: Int): Move {
        val fromIndex = actionIndex / BOARD_SIZE
        val toIndex = actionIndex % BOARD_SIZE
        
        val fromRank = fromIndex / 8
        val fromFile = fromIndex % 8
        val toRank = toIndex / 8
        val toFile = toIndex % 8
        
        return Move(
            from = Position(fromRank, fromFile),
            to = Position(toRank, toFile)
        )
    }
    
    /**
     * Get all possible action indices for legal moves
     */
    fun encodeValidMoves(moves: List<Move>): List<Int> {
        return moves.map { encodeMove(it) }
    }
    
    /**
     * Create action mask for valid moves (1.0 for valid, 0.0 for invalid)
     */
    fun createActionMask(validMoves: List<Move>): DoubleArray {
        val mask = DoubleArray(ACTION_SPACE_SIZE) { 0.0 }
        for (move in validMoves) {
            mask[encodeMove(move)] = 1.0
        }
        return mask
    }
}

/**
 * Chess-specific reward configuration for RL training
 */
data class ChessRewardConfig(
    val winReward: Double = 1.0,
    val lossReward: Double = -1.0,
    val drawReward: Double = 0.0,
    // Small per-step penalty to discourage excessively long games (default 0.0 for tests)
    val stepPenalty: Double = 0.0,
    // Penalty applied when games hit step limits (incomplete games)
    val stepLimitPenalty: Double = -0.5,
    val invalidMoveReward: Double = -0.1,
    val gameLengthNormalization: Boolean = true,
    val maxGameLength: Int = 200,
    
    // Early adjudication settings
    val enableEarlyAdjudication: Boolean = false,
    val resignMaterialThreshold: Int = 9, // Material point difference for resignation
    val noProgressPlies: Int = 40, // Plies without capture/check before draw adjudication
    
    // Position-based reward shaping (optional)
    val enablePositionRewards: Boolean = false,
    val materialWeight: Double = 0.01,
    val pieceActivityWeight: Double = 0.005,
    val kingSafetyWeight: Double = 0.01,
    val centerControlWeight: Double = 0.005,
    val developmentWeight: Double = 0.005
)

/**
 * Chess-specific metrics for training analysis
 */
data class ChessMetrics(
    val gameLength: Int,
    val totalMaterialValue: Int,
    val piecesInCenter: Int,
    val developedPieces: Int,
    val kingSafetyScore: Double,
    val moveCount: Int,
    val captureCount: Int,
    val checkCount: Int
)

/**
 * Position evaluator for chess-specific reward shaping
 */
class ChessPositionEvaluator {
    companion object {
        // Piece values for material evaluation
        val PIECE_VALUES = mapOf(
            PieceType.PAWN to 1,
            PieceType.KNIGHT to 3,
            PieceType.BISHOP to 3,
            PieceType.ROOK to 5,
            PieceType.QUEEN to 9,
            PieceType.KING to 0
        )
        
        // Center squares for positional evaluation
        val CENTER_SQUARES = setOf(
            Position(3, 3), Position(3, 4), Position(4, 3), Position(4, 4) // d4, e4, d5, e5
        )
        
        val EXTENDED_CENTER_SQUARES = setOf(
            Position(2, 2), Position(2, 3), Position(2, 4), Position(2, 5),
            Position(3, 2), Position(3, 3), Position(3, 4), Position(3, 5),
            Position(4, 2), Position(4, 3), Position(4, 4), Position(4, 5),
            Position(5, 2), Position(5, 3), Position(5, 4), Position(5, 5)
        )
    }
    
    /**
     * Evaluate material balance for a color
     */
    fun evaluateMaterial(board: ChessBoard, color: PieceColor): Int {
        var materialValue = 0
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = board.getPieceAt(Position(rank, file))
                if (piece?.color == color) {
                    materialValue += PIECE_VALUES[piece.type] ?: 0
                }
            }
        }
        return materialValue
    }
    
    /**
     * Evaluate piece activity (pieces in center and extended center)
     */
    fun evaluatePieceActivity(board: ChessBoard, color: PieceColor): Int {
        var activityScore = 0
        for (rank in 0..7) {
            for (file in 0..7) {
                val position = Position(rank, file)
                val piece = board.getPieceAt(position)
                if (piece?.color == color && piece.type != PieceType.PAWN) {
                    when {
                        CENTER_SQUARES.contains(position) -> activityScore += 3
                        EXTENDED_CENTER_SQUARES.contains(position) -> activityScore += 1
                    }
                }
            }
        }
        return activityScore
    }
    
    /**
     * Evaluate king safety (distance from center, pieces around king)
     */
    fun evaluateKingSafety(board: ChessBoard, color: PieceColor): Double {
        val kingPosition = findKing(board, color) ?: return -10.0 // King missing = very unsafe
        
        var safetyScore = 0.0
        
        // King should generally stay away from center in opening/middlegame
        val distanceFromCenter = kotlin.math.min(
            kotlin.math.abs(kingPosition.rank - 3.5),
            kotlin.math.abs(kingPosition.file - 3.5)
        )
        safetyScore += distanceFromCenter
        
        // Count friendly pieces around king (within 1 square)
        var protectingPieces = 0
        for (rankOffset in -1..1) {
            for (fileOffset in -1..1) {
                if (rankOffset == 0 && fileOffset == 0) continue
                val adjacentPos = Position(
                    kingPosition.rank + rankOffset,
                    kingPosition.file + fileOffset
                )
                if (adjacentPos.isValid()) {
                    val piece = board.getPieceAt(adjacentPos)
                    if (piece?.color == color) {
                        protectingPieces++
                    }
                }
            }
        }
        safetyScore += protectingPieces * 0.5
        
        return safetyScore
    }
    
    /**
     * Evaluate piece development (knights and bishops off starting squares)
     */
    fun evaluateDevelopment(board: ChessBoard, color: PieceColor): Int {
        var developmentScore = 0
        
        val startingRank = if (color == PieceColor.WHITE) 0 else 7
        val knightFiles = listOf(1, 6) // b and g files
        val bishopFiles = listOf(2, 5) // c and f files
        
        // Check if knights are developed
        for (file in knightFiles) {
            val piece = board.getPieceAt(Position(startingRank, file))
            if (piece?.type != PieceType.KNIGHT || piece.color != color) {
                developmentScore++ // Knight has moved from starting square
            }
        }
        
        // Check if bishops are developed
        for (file in bishopFiles) {
            val piece = board.getPieceAt(Position(startingRank, file))
            if (piece?.type != PieceType.BISHOP || piece.color != color) {
                developmentScore++ // Bishop has moved from starting square
            }
        }
        
        return developmentScore
    }
    
    /**
     * Find king position for a color
     */
    private fun findKing(board: ChessBoard, color: PieceColor): Position? {
        for (rank in 0..7) {
            for (file in 0..7) {
                val position = Position(rank, file)
                val piece = board.getPieceAt(position)
                if (piece?.type == PieceType.KING && piece.color == color) {
                    return position
                }
            }
        }
        return null
    }
    
    /**
     * Calculate comprehensive position evaluation
     */
    fun evaluatePosition(board: ChessBoard, color: PieceColor, config: ChessRewardConfig): Double {
        if (!config.enablePositionRewards) return 0.0
        
        val material = evaluateMaterial(board, color)
        val opponentMaterial = evaluateMaterial(board, color.opposite())
        val materialAdvantage = material - opponentMaterial
        
        val activity = evaluatePieceActivity(board, color)
        val kingSafety = evaluateKingSafety(board, color)
        val development = evaluateDevelopment(board, color)
        
        return materialAdvantage * config.materialWeight +
               activity * config.pieceActivityWeight +
               kingSafety * config.kingSafetyWeight +
               development * config.developmentWeight
    }
}

/**
 * Chess environment wrapper for reinforcement learning.
 * 
 * Converts chess game state to neural network input format and handles
 * action decoding from network outputs to legal chess moves. Provides
 * the standard RL environment interface for training agents.
 * 
 * State Encoding:
 * - 839-dimensional state vector representing chess position
 * - 8x8x12 board representation plus additional features
 * - Includes castling rights, en passant, and move counts
 * 
 * Action Space:
 * - 4096 possible actions (64x64 from-to square combinations)
 * - Action masking ensures only legal moves are selected
 * - Automatic fallback to random legal move if invalid action
 * 
 * Reward Structure:
 * - Terminal rewards based on game outcomes (win/loss/draw)
 * - Optional step penalties to encourage efficient play
 * - Configurable reward shaping for position evaluation
 * 
 * @param rewardConfig Configuration for reward structure and game rules
 */
class ChessEnvironment(
    private val rewardConfig: ChessRewardConfig = ChessRewardConfig()
) : Environment<DoubleArray, Int> {
    private var chessBoard = ChessBoard()
    private val stateEncoder = ChessStateEncoder()
    private val actionEncoder = ChessActionEncoder()
    private val gameStateDetector = GameStateDetector()
    private val legalMoveValidator = LegalMoveValidator()
    private val positionEvaluator = ChessPositionEvaluator()
    
    // Game history for draw detection
    private val gameHistory = mutableListOf<String>()
    
    // Game metrics tracking
    private var moveCount = 0
    private var captureCount = 0
    
    // Early adjudication tracking
    private var lastMaterialTotals = Pair(0, 0)
    private var noProgressPlies = 0
    private var adjudicatedOutcome: GameStatus? = null
    private var checkCount = 0
    private var previousMaterialValue = 0
    
    /**
     * Resets the chess environment to the starting position.
     * 
     * Initializes a new chess game, clears game history, and resets
     * all tracking variables for metrics and adjudication.
     * 
     * @return 839-dimensional encoded starting position
     */
    override fun reset(): DoubleArray {
        chessBoard = ChessBoard()
        gameHistory.clear()
        gameHistory.add(chessBoard.toFEN())
        
        // Reset game metrics
        moveCount = 0
        captureCount = 0
        checkCount = 0
        previousMaterialValue = calculateTotalMaterialValue()
        
        // Reset adjudication state
        val startFen = chessBoard.toFEN()
        lastMaterialTotals = calculateMaterialTotals(startFen)
        noProgressPlies = 0
        adjudicatedOutcome = null
        
        return stateEncoder.encode(chessBoard)
    }
    
    /**
     * Executes a chess move and returns the resulting state.
     * 
     * Decodes the action index to a chess move, validates legality,
     * executes the move, and computes rewards based on game outcome.
     * Applies action masking to ensure only legal moves are executed.
     * 
     * @param action Action index in range [0, 4096)
     * @return StepResult containing next state, reward, done flag, and metadata
     * @throws IllegalArgumentException if action index is out of range
     */
    override fun step(action: Int): StepResult<DoubleArray> {
        require(action in 0 until ChessActionEncoder.ACTION_SPACE_SIZE) {
            "Action index $action out of range [0, ${ChessActionEncoder.ACTION_SPACE_SIZE})"
        }
        // Decode action to move
        val baseMove = actionEncoder.decodeAction(action)
        
        // Find the actual legal move that matches this action
        val legalMoves = legalMoveValidator.getAllLegalMoves(chessBoard, chessBoard.getActiveColor())
        val actualMove = findMatchingMove(baseMove, legalMoves)
        
        if (actualMove == null) {
            // Invalid move - return current state with negative reward
            val penalty = -1.0
            require(penalty.isFinite()) { "Invalid reward computed for invalid move" }
            return StepResult(
                nextState = stateEncoder.encode(chessBoard),
                reward = penalty, // Penalty for invalid move
                done = false,
                info = mapOf("error" to "Invalid move: ${baseMove.toAlgebraic()}")
            )
        }
        
        // Execute the move. The selected move comes from the current legal set,
        // so we can apply it directly to avoid redundant re-validation that may
        // diverge from the enumerator in rare edge cases.
        val moveResult = chessBoard.makeMove(actualMove)
        if (moveResult != MoveResult.SUCCESS) {
            return StepResult(
                nextState = stateEncoder.encode(chessBoard),
                reward = -1.0,
                done = false,
                info = mapOf("error" to "Move execution failed")
            )
        }
        
        // Switch active color
        chessBoard.switchActiveColor()
        
        // Add position to history for draw detection
        gameHistory.add(chessBoard.toFEN())
        
        // Update game metrics
        updateGameMetrics()
        
        // Check for early adjudication first
        val adjudicatedStatus = checkEarlyAdjudication()
        
        // Get game status and calculate reward
        val gameStatus = adjudicatedStatus ?: gameStateDetector.getGameStatus(chessBoard, gameHistory)
        val reward = calculateReward(gameStatus, actualMove, chessBoard.getActiveColor().opposite())
        require(reward.isFinite()) { "Non-finite reward computed: $reward" }
        val done = gameStatus.isGameOver
        
        val nextState = stateEncoder.encode(chessBoard)
        
        return StepResult(
            nextState = nextState,
            reward = reward,
            done = done,
            info = mapOf(
                "game_status" to gameStatus.name,
                "move" to actualMove.toAlgebraic(),
                "fen" to chessBoard.toFEN()
            )
        )
    }
    
    /**
     * Apply step limit penalty for games that hit the maximum step limit
     * This should be called by the training pipeline when a game is terminated due to step limits
     */
    fun applyStepLimitPenalty(): Double {
        return rewardConfig.stepLimitPenalty
    }
    
    /**
     * Calculate material totals from FEN string for adjudication
     */
    private fun calculateMaterialTotals(fen: String): Pair<Int, Int> {
        var white = 0
        var black = 0
        for (ch in fen.takeWhile { it != ' ' }) {
            when (ch) {
                'P' -> white += 1; 'p' -> black += 1
                'N', 'B' -> white += 3; 'n', 'b' -> black += 3
                'R' -> white += 5; 'r' -> black += 5
                'Q' -> white += 9; 'q' -> black += 9
            }
        }
        return white to black
    }
    
    /**
     * Check for early adjudication based on material advantage or no progress
     */
    private fun checkEarlyAdjudication(): GameStatus? {
        if (!rewardConfig.enableEarlyAdjudication || adjudicatedOutcome != null) {
            return adjudicatedOutcome
        }
        
        val currentFen = chessBoard.toFEN()
        val (whiteTotal, blackTotal) = calculateMaterialTotals(currentFen)
        val (prevWhite, prevBlack) = lastMaterialTotals
        
        // Check for capture or check to reset no-progress counter
        val capture = (whiteTotal + blackTotal) < (prevWhite + prevBlack)
        val inCheck = gameStateDetector.getGameStatus(chessBoard, gameHistory).name.contains("IN_CHECK")
        
        noProgressPlies = if (capture || inCheck) 0 else noProgressPlies + 1
        lastMaterialTotals = whiteTotal to blackTotal
        
        // Check material advantage for resignation
        val materialDiff = whiteTotal - blackTotal
        if (kotlin.math.abs(materialDiff) >= rewardConfig.resignMaterialThreshold) {
            adjudicatedOutcome = if (materialDiff > 0) GameStatus.WHITE_WINS else GameStatus.BLACK_WINS
            return adjudicatedOutcome
        }

        // Detect simple, typically forced endgames and adjudicate as a win
        detectSimpleForcedWin(noProgressPlies)?.let { forced ->
            adjudicatedOutcome = forced
            return adjudicatedOutcome
        }

        // Check no progress for draw adjudication
        if (noProgressPlies >= rewardConfig.noProgressPlies) {
            adjudicatedOutcome = GameStatus.DRAW_FIFTY_MOVE_RULE // Use fifty-move as proxy for no progress
            return adjudicatedOutcome
        }
        
        return null
    }

    /**
     * Detect simple endgames (KQ vs K, KR vs K, KBB vs K, KBN vs K with some no-progress) and adjudicate as win
     * Returns WHITE_WINS/BLACK_WINS or null if no adjudication should be applied.
     */
    private fun detectSimpleForcedWin(noProgressPlies: Int): GameStatus? {
        val fen = chessBoard.toFEN()
        // Count pieces by color
        var wP = 0; var wN = 0; var wB = 0; var wR = 0; var wQ = 0
        var bP = 0; var bN = 0; var bB = 0; var bR = 0; var bQ = 0
        for (ch in fen.takeWhile { it != ' ' }) {
            when (ch) {
                'P' -> wP++
                'N' -> wN++
                'B' -> wB++
                'R' -> wR++
                'Q' -> wQ++
                'p' -> bP++
                'n' -> bN++
                'b' -> bB++
                'r' -> bR++
                'q' -> bQ++
            }
        }
        val whiteOnlyKing = (wP + wN + wB + wR + wQ) == 0
        val blackOnlyKing = (bP + bN + bB + bR + bQ) == 0

        // If one side has only king, and the other has a simple mating set, adjudicate win for that side
        if (blackOnlyKing) {
            // White has mating material
            if (wQ >= 1 || wR >= 1 || wB >= 2) return GameStatus.WHITE_WINS
            // KBN vs K is theoretically won; require some no-progress plies to avoid too-early adjudication
            if (wB >= 1 && wN >= 1 && noProgressPlies >= 20) return GameStatus.WHITE_WINS
        }
        if (whiteOnlyKing) {
            if (bQ >= 1 || bR >= 1 || bB >= 2) return GameStatus.BLACK_WINS
            if (bB >= 1 && bN >= 1 && noProgressPlies >= 20) return GameStatus.BLACK_WINS
        }
        return null
    }
    
    /**
     * Find legal move that matches the decoded action (handles promotions)
     */
    private fun findMatchingMove(baseMove: Move, legalMoves: List<Move>): Move? {
        // First try exact match
        val exactMatch = legalMoves.find { it.from == baseMove.from && it.to == baseMove.to && it.promotion == null }
        if (exactMatch != null) return exactMatch
        
        // If it's a pawn promotion, default to queen promotion
        val promotionMatch = legalMoves.find { 
            it.from == baseMove.from && it.to == baseMove.to && it.promotion == PieceType.QUEEN 
        }
        if (promotionMatch != null) return promotionMatch
        
        // Try any promotion
        return legalMoves.find { it.from == baseMove.from && it.to == baseMove.to }
    }
    
    /**
     * Calculate reward based on game outcome and position evaluation
     * This method handles legitimate chess outcomes only - step limit penalties are applied externally
     */
    private fun calculateReward(gameStatus: GameStatus, move: Move, movingColor: PieceColor): Double {
        var reward: Double
        
        // Primary outcome-based rewards - only for legitimate chess game endings
        when (gameStatus) {
            GameStatus.WHITE_WINS -> {
                reward = if (movingColor == PieceColor.WHITE) {
                    rewardConfig.winReward
                } else {
                    rewardConfig.lossReward
                }
                
                // Apply game length normalization for wins/losses
                if (rewardConfig.gameLengthNormalization) {
                    val lengthFactor = 1.0 - (moveCount.toDouble() / rewardConfig.maxGameLength).coerceAtMost(0.5)
                    reward *= lengthFactor
                }
            }
            GameStatus.BLACK_WINS -> {
                reward = if (movingColor == PieceColor.BLACK) {
                    rewardConfig.winReward
                } else {
                    rewardConfig.lossReward
                }
                
                // Apply game length normalization for wins/losses
                if (rewardConfig.gameLengthNormalization) {
                    val lengthFactor = 1.0 - (moveCount.toDouble() / rewardConfig.maxGameLength).coerceAtMost(0.5)
                    reward *= lengthFactor
                }
            }
            GameStatus.DRAW_STALEMATE, 
            GameStatus.DRAW_INSUFFICIENT_MATERIAL,
            GameStatus.DRAW_FIFTY_MOVE_RULE,
            GameStatus.DRAW_REPETITION -> {
                // Only legitimate chess draws get draw reward
                reward = rewardConfig.drawReward
            }
            else -> {
                // Ongoing game: positional shaping and step penalty only
                // NO step limit penalty here - that's handled by the training pipeline
                reward = if (rewardConfig.enablePositionRewards) {
                    var r = positionEvaluator.evaluatePosition(chessBoard, movingColor, rewardConfig)
                    if (isCapture()) r += 0.02
                    if (gameStateDetector.isInCheck(chessBoard, movingColor.opposite())) r += 0.01
                    r
                } else 0.0
                
                // Apply per-step penalty to discourage very long episodes
                reward += rewardConfig.stepPenalty
            }
        }
        
        return reward
    }
    
    /**
     * Update game metrics after a move
     */
    private fun updateGameMetrics() {
        moveCount++
        
        // Check if move was a capture by comparing material values
        val currentMaterialValue = calculateTotalMaterialValue()
        if (currentMaterialValue < previousMaterialValue) {
            captureCount++
        }
        
        // Check if move gives check
        if (gameStateDetector.isInCheck(chessBoard, chessBoard.getActiveColor())) {
            checkCount++
        }
        
        // Update material tracking
        previousMaterialValue = currentMaterialValue
    }
    
    /**
     * Check if a move is a capture by comparing material values
     */
    private fun isCapture(): Boolean {
        val currentMaterialValue = calculateTotalMaterialValue()
        return currentMaterialValue < previousMaterialValue
    }
    
    /**
     * Calculate total material value on the board
     */
    private fun calculateTotalMaterialValue(): Int {
        var totalValue = 0
        for (rank in 0..7) {
            for (file in 0..7) {
                val piece = chessBoard.getPieceAt(Position(rank, file))
                if (piece != null) {
                    totalValue += ChessPositionEvaluator.PIECE_VALUES[piece.type] ?: 0
                }
            }
        }
        return totalValue
    }
    
    /**
     * Get comprehensive chess-specific metrics for the current game
     */
    fun getChessMetrics(): ChessMetrics {
        val whiteMaterial = positionEvaluator.evaluateMaterial(chessBoard, PieceColor.WHITE)
        val blackMaterial = positionEvaluator.evaluateMaterial(chessBoard, PieceColor.BLACK)
        val totalMaterial = whiteMaterial + blackMaterial
        
        val whitePiecesInCenter = positionEvaluator.evaluatePieceActivity(chessBoard, PieceColor.WHITE)
        val blackPiecesInCenter = positionEvaluator.evaluatePieceActivity(chessBoard, PieceColor.BLACK)
        val totalPiecesInCenter = whitePiecesInCenter + blackPiecesInCenter
        
        val whiteDevelopment = positionEvaluator.evaluateDevelopment(chessBoard, PieceColor.WHITE)
        val blackDevelopment = positionEvaluator.evaluateDevelopment(chessBoard, PieceColor.BLACK)
        val totalDevelopment = whiteDevelopment + blackDevelopment
        
        val whiteKingSafety = positionEvaluator.evaluateKingSafety(chessBoard, PieceColor.WHITE)
        val blackKingSafety = positionEvaluator.evaluateKingSafety(chessBoard, PieceColor.BLACK)
        val averageKingSafety = (whiteKingSafety + blackKingSafety) / 2.0
        
        return ChessMetrics(
            gameLength = moveCount,
            totalMaterialValue = totalMaterial,
            piecesInCenter = totalPiecesInCenter,
            developedPieces = totalDevelopment,
            kingSafetyScore = averageKingSafety,
            moveCount = moveCount,
            captureCount = captureCount,
            checkCount = checkCount
        )
    }
    
    /**
     * Get position evaluation for a specific color
     */
    fun getPositionEvaluation(color: PieceColor): Double {
        return positionEvaluator.evaluatePosition(chessBoard, color, rewardConfig)
    }
    
    override fun getValidActions(state: DoubleArray): List<Int> {
        require(state.size == getStateSize()) { "State size ${state.size} does not match expected ${getStateSize()}" }
        val legalMoves = legalMoveValidator.getAllLegalMoves(chessBoard, chessBoard.getActiveColor())
        return actionEncoder.encodeValidMoves(legalMoves)
    }
    
    override fun isTerminal(state: DoubleArray): Boolean {
        val gameStatus = gameStateDetector.getGameStatus(chessBoard, gameHistory)
        return gameStatus.isGameOver
    }
    
    override fun getStateSize(): Int = ChessStateEncoder.TOTAL_FEATURES
    
    override fun getActionSize(): Int = ChessActionEncoder.ACTION_SPACE_SIZE
    
    /**
     * Get current chess board (for debugging/visualization)
     */
    fun getCurrentBoard(): ChessBoard = chessBoard.copy()
    
    /**
     * Get action mask for current position
     */
    fun getActionMask(): DoubleArray {
        val legalMoves = legalMoveValidator.getAllLegalMoves(chessBoard, chessBoard.getActiveColor())
        return actionEncoder.createActionMask(legalMoves)
    }
    
    /**
     * Get current game status
     */
    fun getGameStatus(): GameStatus {
        return gameStateDetector.getGameStatus(chessBoard, gameHistory)
    }

    /**
     * Get last early-adjudicated outcome if any. Null when no early adjudication has occurred.
     */
    fun getAdjudicatedOutcome(): GameStatus? = adjudicatedOutcome

    /**
     * Effective status that respects early adjudication when present; falls back to computed status otherwise.
     */
    fun getEffectiveGameStatus(): GameStatus = adjudicatedOutcome ?: getGameStatus()
    
    /**
     * Check if the current game state represents a legitimate chess ending
     * (as opposed to an artificial termination like step limits)
     */
    fun isLegitimateChessEnding(): Boolean {
        val status = getGameStatus()
        return when (status) {
            GameStatus.WHITE_WINS,
            GameStatus.BLACK_WINS,
            GameStatus.DRAW_STALEMATE,
            GameStatus.DRAW_INSUFFICIENT_MATERIAL,
            GameStatus.DRAW_FIFTY_MOVE_RULE,
            GameStatus.DRAW_REPETITION -> true
            else -> false
        }
    }
    
    /**
     * Get termination reason for the current game state
     */
    fun getTerminationReason(): String {
        val status = getGameStatus()
        return when {
            status.isGameOver -> "GAME_ENDED"
            else -> "ONGOING"
        }
    }
    
    /**
     * Load position from FEN string
     */
    fun loadFromFEN(fen: String): Boolean {
        val newBoard = ChessBoard()
        if (newBoard.fromFEN(fen)) {
            chessBoard = newBoard
            gameHistory.clear()
            gameHistory.add(fen)
            return true
        }
        return false
    }
}
