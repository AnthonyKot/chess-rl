package com.chessrl.integration.adapter

import com.chessrl.chess.Move

/** Utility conversions between legacy engine Move and ChessEngineAdapter move types. */
object MoveConversion {
    fun toChessMove(move: Move): ChessMove = ChessMove(
        from = move.from,
        to = move.to,
        promotion = move.promotion
    )

    fun toEngineMove(move: ChessMove): Move = Move(
        from = move.from,
        to = move.to,
        promotion = move.promotion
    )
}
