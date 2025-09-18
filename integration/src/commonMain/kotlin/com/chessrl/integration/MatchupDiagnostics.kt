package com.chessrl.integration

/**
 * Diagnostics for head-to-head self-play matchups between two agents.
 * Computes color win rates, termination breakdowns, and pacing stats.
 */
data class MatchupDiagnostics(
    // Outcome breakdown
    val totalGames: Int,
    val whiteWins: Int,
    val blackWins: Int,
    val draws: Int,
    val winRateWhite: Double,
    val winRateBlack: Double,
    val drawRate: Double,
    // Termination breakdown
    val endedNaturally: Int,
    val stepLimitEnded: Int,
    val manualStops: Int,
    // Length & pacing
    val averageGameLength: Double,
    val averageWinLength: Double?,
    val averageDrawLength: Double?,
    // Quality/consistency signals
    val stepLimitRatio: Double,
    val colorBias: Double // |whiteWinRate - blackWinRate|
)

object MatchupDiagnosticsBuilder {
    fun from(results: SelfPlayResults): MatchupDiagnostics {
        val total = results.gameResults.size
        if (total == 0) {
            return MatchupDiagnostics(
                totalGames = 0,
                whiteWins = 0,
                blackWins = 0,
                draws = 0,
                winRateWhite = 0.0,
                winRateBlack = 0.0,
                drawRate = 0.0,
                endedNaturally = 0,
                stepLimitEnded = 0,
                manualStops = 0,
                averageGameLength = 0.0,
                averageWinLength = null,
                averageDrawLength = null,
                stepLimitRatio = 0.0,
                colorBias = 0.0
            )
        }

        val whiteWins = results.gameResults.count { it.gameOutcome == GameOutcome.WHITE_WINS }
        val blackWins = results.gameResults.count { it.gameOutcome == GameOutcome.BLACK_WINS }
        val draws = results.gameResults.count { it.gameOutcome == GameOutcome.DRAW }

        val endedNaturally = results.gameResults.count { it.terminationReason == EpisodeTerminationReason.GAME_ENDED }
        val stepLimitEnded = results.gameResults.count { it.terminationReason == EpisodeTerminationReason.STEP_LIMIT }
        val manualStops = results.gameResults.count { it.terminationReason == EpisodeTerminationReason.MANUAL }

        val lengths = results.gameResults.map { it.gameLength }
        val avgLen = if (lengths.isNotEmpty()) lengths.average() else 0.0
        val winLengths = results.gameResults.filter { it.gameOutcome == GameOutcome.WHITE_WINS || it.gameOutcome == GameOutcome.BLACK_WINS }.map { it.gameLength }
        val drawLengths = results.gameResults.filter { it.gameOutcome == GameOutcome.DRAW }.map { it.gameLength }

        val avgWinLen = if (winLengths.isNotEmpty()) winLengths.average() else null
        val avgDrawLen = if (drawLengths.isNotEmpty()) drawLengths.average() else null

        val wRate = whiteWins.toDouble() / total
        val bRate = blackWins.toDouble() / total
        val dRate = draws.toDouble() / total
        val stepRatio = stepLimitEnded.toDouble() / total

        return MatchupDiagnostics(
            totalGames = total,
            whiteWins = whiteWins,
            blackWins = blackWins,
            draws = draws,
            winRateWhite = wRate,
            winRateBlack = bRate,
            drawRate = dRate,
            endedNaturally = endedNaturally,
            stepLimitEnded = stepLimitEnded,
            manualStops = manualStops,
            averageGameLength = avgLen,
            averageWinLength = avgWinLen,
            averageDrawLength = avgDrawLen,
            stepLimitRatio = stepRatio,
            colorBias = kotlin.math.abs(wRate - bRate)
        )
    }
}

