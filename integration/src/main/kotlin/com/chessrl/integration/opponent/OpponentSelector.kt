package com.chessrl.integration.opponent

import kotlin.random.Random

/**
 * Represents the opponent behaviour chosen for a training or evaluation game.
 */
enum class OpponentKind {
    SELF,
    HEURISTIC,
    MINIMAX,
    MINIMAX_SOFTMAX
}

data class OpponentSelection(
    val kind: OpponentKind,
    val minimaxDepth: Int? = null
)

/**
 * Utility for selecting training/evaluation opponents based on configuration.
 */
object OpponentSelector {

    private val randomKeywords = setOf("random", "mixed", "hybrid")

    fun select(type: String?, baseDepth: Int, random: Random): OpponentSelection {
        val normalized = type?.trim()?.lowercase()
        return when {
            normalized == null || normalized.isEmpty() || normalized == "self" ->
                OpponentSelection(OpponentKind.SELF)

            normalized == "heuristic" ->
                OpponentSelection(OpponentKind.HEURISTIC)

            normalized == "minimax" -> {
                val depth = if (baseDepth <= 0) 2 else baseDepth
                OpponentSelection(OpponentKind.MINIMAX, depth)
            }

            normalized == "minimax-softmax" || normalized == "softmax" -> {
                val depth = if (baseDepth <= 0) 2 else baseDepth
                OpponentSelection(OpponentKind.MINIMAX_SOFTMAX, depth)
            }

            normalized in randomKeywords -> {
                val options = listOf(
                    OpponentSelection(OpponentKind.HEURISTIC),
                    OpponentSelection(OpponentKind.MINIMAX, 1),
                    OpponentSelection(OpponentKind.MINIMAX_SOFTMAX, 1),
                    OpponentSelection(OpponentKind.MINIMAX, 2)
                )
                options[random.nextInt(options.size)]
            }

            else -> OpponentSelection(OpponentKind.SELF)
        }
    }
}
