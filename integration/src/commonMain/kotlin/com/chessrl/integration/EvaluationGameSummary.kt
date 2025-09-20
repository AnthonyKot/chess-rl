package com.chessrl.integration

/**
 * Lightweight summary of a completed evaluation game.
 */
data class EvaluationGameSummary(
    val index: Int,
    val moveCount: Int,
    val score: Double,
    val outcome: GameOutcome,
    val finalPosition: String
)
