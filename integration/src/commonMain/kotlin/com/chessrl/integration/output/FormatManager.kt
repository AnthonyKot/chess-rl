package com.chessrl.integration.output

import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * Handles consistent formatting and styling of output messages.
 * Provides locale-aware number formatting and standardized display formats.
 */
class FormatManager(
    private val decimalPlaces: Int = 2
) {
    
    init {
        require(decimalPlaces in 0..4) { "Decimal places must be between 0 and 4" }
    }
    
    /**
     * Format cycle summary in the ideal one-liner format:
     * "Cycle X/Y | games=Z | win/draw/loss=A/B/C | avgLen=D.E | reward=F.GH | batches=I | loss=J.KL | grad=M.NO | buf=P.Qk/Rs | T.Us"
     */
    fun formatCycleSummary(data: CycleProgressData, bestDelta: Double? = null): String {
        val (wins, draws, losses) = data.winDrawLoss
        val bufferStats = formatBufferStats(data.bufferUtilization)
        val duration = formatDuration(data.cycleDuration)
        
        val parts = mutableListOf<String>().apply {
            add("Cycle ${data.cycleNumber}/${data.totalCycles}")
            add("games=${data.gamesPlayed}")
            add("win/draw/loss=$wins/$draws/$losses")
            add("avgLen=${formatNumber(data.averageGameLength)}")
            add("reward=${formatNumber(data.averageReward)}")
            add("batches=${data.batchesProcessed}")
            add("loss=${formatNumber(data.averageLoss)}")
            add("grad=${formatNumber(data.gradientNorm)}")
            add("buf=$bufferStats")
            add(duration)
            
            // Add best performance delta if available
            bestDelta?.let { delta ->
                val sign = if (delta >= 0) "+" else ""
                add("bestΔ=$sign${formatNumber(delta)}")
                if (data.checkpointEvent?.isBest == true) {
                    add("(saved)")
                }
            }
        }
        
        return parts.joinToString(" | ")
    }
    
    /**
     * Format cycle summary with trend indicators in the enhanced format:
     * "Cycle X/Y | games=Z | win/draw/loss=A/B/C | avgLen=D.E | reward=F.GH↑ | batches=I | loss=J.KL↓ | grad=M.NO | buf=P.Qk/Rs | T.Us | bestΔ=±V.WXY (saved)"
     */
    fun formatCycleSummaryWithTrends(data: CycleProgressData, trendData: TrendData? = null, eta: Duration? = null): String {
        val (wins, draws, losses) = data.winDrawLoss
        val bufferStats = formatBufferStats(data.bufferUtilization)
        val duration = formatDuration(data.cycleDuration)
        
        val parts = mutableListOf<String>().apply {
            add("Cycle ${data.cycleNumber}/${data.totalCycles}")
            add("games=${data.gamesPlayed}")
            add("win/draw/loss=$wins/$draws/$losses")
            add("avgLen=${formatNumber(data.averageGameLength)}")
            
            // Add reward with trend indicator
            val rewardText = "reward=${formatNumber(data.averageReward)}"
            trendData?.let { trends ->
                val trendSymbol = formatTrendSymbol(trends.rewardTrend)
                add("$rewardText$trendSymbol")
            } ?: add(rewardText)
            
            add("batches=${data.batchesProcessed}")
            
            // Add loss with trend indicator (inverted for loss)
            val lossText = "loss=${formatNumber(data.averageLoss)}"
            trendData?.let { trends ->
                val trendSymbol = formatTrendSymbol(trends.lossTrend)
                add("$lossText$trendSymbol")
            } ?: add(lossText)
            
            add("grad=${formatNumber(data.gradientNorm)}")
            add("buf=$bufferStats")
            add(duration)
            
            // Add ETA if available
            eta?.let { etaValue ->
                add("ETA=${formatDuration(etaValue)}")
            }
            
            // Add best performance delta if available
            trendData?.bestPerformanceDelta?.let { delta ->
                val sign = if (delta >= 0) "+" else ""
                add("bestΔ=$sign${formatNumber(delta)}")
                if (data.checkpointEvent?.isBest == true) {
                    add("(saved)")
                }
            }
        }
        
        return parts.joinToString(" | ")
    }
    
    /**
     * Format detailed block for verbose output
     */
    fun formatDetailedBlock(data: DetailedBlockData): String {
        val lines = mutableListOf<String>()
        
        // Episode metrics
        lines.add("Episode Metrics:")
        lines.add("  Game range: ${data.episodeMetrics.gameRange.first}-${data.episodeMetrics.gameRange.last}")
        lines.add("  Average length: ${formatNumber(data.episodeMetrics.averageLength)} moves")
        
        if (data.episodeMetrics.terminationBreakdown.isNotEmpty()) {
            val breakdown = data.episodeMetrics.terminationBreakdown.entries
                .joinToString(", ") { "${it.key.name.lowercase()}=${it.value}" }
            lines.add("  Termination: $breakdown")
        }
        
        // Training metrics
        lines.add("Training Metrics:")
        lines.add("  Batch updates: ${data.trainingMetrics.batchUpdates}")
        lines.add("  Average loss: ${formatNumber(data.trainingMetrics.averageLoss)}")
        lines.add("  Gradient norm: ${formatNumber(data.trainingMetrics.gradientNorm)}")
        lines.add("  Entropy: ${formatNumber(data.trainingMetrics.entropy)}")
        
        // Q-value statistics
        lines.add("Q-Value Statistics:")
        lines.add("  Mean: ${formatNumber(data.qValueStats.mean)}")
        lines.add("  Range: ${formatNumber(data.qValueStats.range.start)} to ${formatNumber(data.qValueStats.range.endInclusive)}")
        lines.add("  Variance: ${formatNumber(data.qValueStats.variance)}")
        
        // Efficiency metrics
        lines.add("Efficiency Metrics:")
        lines.add("  Games/sec: ${formatNumber(data.efficiencyMetrics.gamesPerSecond)}")
        lines.add("  Total games: ${data.efficiencyMetrics.totalGames}")
        lines.add("  Total experiences: ${data.efficiencyMetrics.totalExperiences}")
        
        // Validation results
        if (data.validationResults.isNotEmpty()) {
            lines.add("Validation Issues:")
            data.validationResults.forEach { validation ->
                val severityIcon = when (validation.severity) {
                    ValidationSeverity.INFO -> "ℹ️"
                    ValidationSeverity.WARNING -> "⚠️"
                    ValidationSeverity.ERROR -> "❌"
                }
                val countSuffix = if (validation.count > 1) " (×${validation.count})" else ""
                lines.add("  $severityIcon ${validation.message}$countSuffix")
            }
        }
        
        return lines.joinToString("\n")
    }
    
    /**
     * Format final summary for training completion
     */
    fun formatFinalSummary(data: FinalSummaryData): String {
        val lines = mutableListOf<String>()
        
        lines.add("🏁 Training Completed!")
        lines.add("=" * 50)
        lines.add("Total cycles: ${data.totalCycles}")
        lines.add("Total duration: ${formatDuration(data.totalDuration)}")
        lines.add("Best performance: ${formatNumber(data.bestPerformance)}")
        lines.add("Total games played: ${data.totalGamesPlayed}")
        lines.add("Total experiences collected: ${data.totalExperiencesCollected}")
        lines.add("")
        lines.add("Final Performance:")
        lines.add("  Average reward: ${formatNumber(data.finalAverageReward)}")
        lines.add("  Win rate: ${formatPercentage(data.finalWinRate)}")
        lines.add("  Draw rate: ${formatPercentage(data.finalDrawRate)}")
        lines.add("  Loss rate: ${formatPercentage(1.0 - data.finalWinRate - data.finalDrawRate)}")
        
        return lines.joinToString("\n")
    }
    
    /**
     * Format configuration summary for startup display
     */
    fun formatConfigSummary(
        profiles: List<String>,
        overrides: Map<String, Any>,
        keyParameters: Map<String, Any>
    ): String {
        val lines = mutableListOf<String>()
        
        lines.add("🚀 Training Configuration")
        lines.add("=" * 40)
        
        if (profiles.isNotEmpty()) {
            lines.add("Profiles: ${profiles.joinToString(", ")}")
        }
        
        if (overrides.isNotEmpty()) {
            lines.add("CLI Overrides:")
            overrides.forEach { (key, value) ->
                lines.add("  $key = $value")
            }
        }
        
        if (keyParameters.isNotEmpty()) {
            lines.add("Key Parameters:")
            keyParameters.forEach { (key, value) ->
                lines.add("  $key = $value")
            }
        }
        
        return lines.joinToString("\n")
    }
    
    /**
     * Format number with consistent decimal places and locale (US format: 0.25 not 0,25)
     */
    fun formatNumber(value: Double, places: Int = decimalPlaces): String {
        return when {
            value.isNaN() -> "NaN"
            value.isInfinite() -> if (value > 0) "∞" else "-∞"
            places == 0 -> value.roundToInt().toString()
            else -> {
                // Ensure US locale formatting (0.25 not 0,25)
                val formatted = "%.${places}f".format(value)
                // Replace comma with period if needed (for non-US locales)
                formatted.replace(',', '.')
            }
        }
    }

    fun formatScientific(value: Double, significantDigits: Int = 3): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        val digits = significantDigits.coerceIn(1, 6)
        return String.format("%1.${digits.coerceAtLeast(1) - 1}e", value)
            .replace(',', '.')
    }
    
    /**
     * Format duration in consistent time display (3.4s format)
     */
    fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val millis = duration.inWholeMilliseconds
        
        return when {
            millis < 1000 -> "${millis}ms"
            totalSeconds < 60 -> "${formatNumber(millis / 1000.0, 1)}s"
            totalSeconds < 3600 -> {
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                "${minutes}m${seconds}s"
            }
            else -> {
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                "${hours}h${minutes}m"
            }
        }
    }
    
    /**
     * Format percentage with consistent display
     */
    fun formatPercentage(value: Double): String {
        return "${formatNumber(value * 100.0, 1)}%"
    }
    
    /**
     * Format buffer statistics in compact form (P.Qk/Rs format)
     */
    fun formatBufferStats(stats: BufferStats): String {
        val current = formatCompactSize(stats.currentSize)
        val max = formatCompactSize(stats.maxSize)
        return "$current/$max"
    }
    
    /**
     * Format size in compact form (k for thousands, M for millions)
     */
    private fun formatCompactSize(size: Int): String {
        return when {
            size < 1000 -> "${formatNumber(size / 1000.0, 1)}k"
            size < 1000000 -> "${formatNumber(size / 1000.0, 1)}k"
            else -> "${formatNumber(size / 1000000.0, 1)}M"
        }
    }
    
    /**
     * Format trend indicator with visual symbols and magnitude
     */
    fun formatTrendIndicator(trend: TrendIndicator): String {
        val symbol = when (trend.direction) {
            TrendDirection.UP -> "↑"
            TrendDirection.DOWN -> "↓"
            TrendDirection.STABLE -> "→"
        }
        
        return if (trend.magnitude > 0.001) { // Only show magnitude for significant changes
            "$symbol${formatNumber(trend.magnitude)}"
        } else {
            symbol
        }
    }
    
    /**
     * Format trend symbol only (for inline display in cycle summaries)
     */
    fun formatTrendSymbol(trend: TrendIndicator): String {
        // Only show symbol if trend is significant and confident
        return if (trend.magnitude > 0.001 && trend.confidence > 0.3) {
            when (trend.direction) {
                TrendDirection.UP -> "↑"
                TrendDirection.DOWN -> "↓"
                TrendDirection.STABLE -> ""
            }
        } else {
            ""
        }
    }
    
    /**
     * Format checkpoint event information
     */
    fun formatCheckpointEvent(event: CheckpointEvent): String {
        val typeIcon = when (event.type) {
            CheckpointType.BEST -> "🏆"
            CheckpointType.REGULAR -> "💾"
            CheckpointType.FINAL -> "🏁"
        }
        
        val parts = mutableListOf<String>().apply {
            add("$typeIcon Saved ${event.type.name.lowercase()} checkpoint")
            event.performanceDelta?.let { delta ->
                val sign = if (delta >= 0) "+" else ""
                add("(Δ$sign${formatNumber(delta)})")
            }
        }
        
        return parts.joinToString(" ")
    }
}



/**
 * Extension operator for string repetition
 */
private operator fun String.times(count: Int): String {
    return this.repeat(count)
}
