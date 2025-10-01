package com.chessrl.chess

/**
 * Minimal feature encoder for a ChessBoard position.
 * Layout:
 * - 12 piece planes (W/B x {P,N,B,R,Q,K}) each 64 cells → 768
 * - Side to move (1)
 * - Castling rights (K,Q,k,q) → 4
 * - En passant square one-hot (64)
 * - Halfmove clock normalized (1)
 * - Fullmove number normalized (1)
 * Total: 768 + 1 + 4 + 64 + 2 = 839
 */
object FeatureEncoding {
    private val pieceOrder = listOf(
        Piece(PieceType.PAWN, PieceColor.WHITE),
        Piece(PieceType.KNIGHT, PieceColor.WHITE),
        Piece(PieceType.BISHOP, PieceColor.WHITE),
        Piece(PieceType.ROOK, PieceColor.WHITE),
        Piece(PieceType.QUEEN, PieceColor.WHITE),
        Piece(PieceType.KING, PieceColor.WHITE),
        Piece(PieceType.PAWN, PieceColor.BLACK),
        Piece(PieceType.KNIGHT, PieceColor.BLACK),
        Piece(PieceType.BISHOP, PieceColor.BLACK),
        Piece(PieceType.ROOK, PieceColor.BLACK),
        Piece(PieceType.QUEEN, PieceColor.BLACK),
        Piece(PieceType.KING, PieceColor.BLACK)
    )

    const val FEATURE_SIZE = 839

    fun boardToFeatures(board: ChessBoard): DoubleArray {
        val out = DoubleArray(FEATURE_SIZE)
        var idx = 0

        // 12 planes
        for (proto in pieceOrder) {
            for (rank in 0..7) {
                for (file in 0..7) {
                    val p = board.getPieceAt(Position(rank, file))
                    out[idx] = if (p != null && p.type == proto.type && p.color == proto.color) 1.0 else 0.0
                    idx += 1
                }
            }
        }

        // Side to move
        out[idx] = if (board.getActiveColor() == PieceColor.WHITE) 1.0 else 0.0
        idx += 1

        // Castling rights KQkq
        val gs = board.getGameState()
        out[idx] = if (gs.whiteCanCastleKingside) 1.0 else 0.0
        idx += 1
        out[idx] = if (gs.whiteCanCastleQueenside) 1.0 else 0.0
        idx += 1
        out[idx] = if (gs.blackCanCastleKingside) 1.0 else 0.0
        idx += 1
        out[idx] = if (gs.blackCanCastleQueenside) 1.0 else 0.0
        idx += 1

        // En passant one-hot
        val ep = gs.enPassantTarget
        for (rank in 0..7) {
            for (file in 0..7) {
                val isEp = ep != null && ep.rank == rank && ep.file == file
                out[idx] = if (isEp) 1.0 else 0.0
                idx += 1
            }
        }

        // Halfmove and fullmove normalized
        out[idx] = (gs.halfmoveClock.coerceIn(0, 100)).toDouble() / 100.0
        idx += 1
        out[idx] = (gs.fullmoveNumber.coerceAtLeast(1).coerceAtMost(200)).toDouble() / 200.0

        return out
    }
}
