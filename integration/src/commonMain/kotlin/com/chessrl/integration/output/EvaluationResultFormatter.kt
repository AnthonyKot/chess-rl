package com.chessrl.integration.output

import kotlin.math.abs

/**
 * Formatter for evaluation results with statistical confidence and clear summarization
 */
class EvaluationResultFormatter(
    private val formatManager: FormatManager = FormatManager()
) {
    
    /**
     * Format a single evaluation result with statistical information
     */
    fun formatEvaluationResult(result: EnhancedEvaluationResults): String {
        val builder = StringBuilder()
        
        // Header
        builder.appendLine("═══════════════════════════════════════════════════════════")
        builder.appendLine("           EVALUATION RESULTS vs ${result.opponentType}")
        builder.appendLine("═══════════════════════════════════════════════════════════")
        
        // Basic statistics
        builder.appendLine("Games played: ${result.totalGames}")
        builder.appendLine("Win rate: ${formatManager.formatPercentage(result.winRate)} (${result.wins} wins)")
        builder.appendLine("Draw rate: ${formatManager.formatPercentage(result.drawRate)} (${result.draws} draws)")
        builder.appendLine("Loss rate: ${formatManager.formatPercentage(result.lossRate)} (${result.losses} losses)")
        builder.appendLine("Average game length: ${formatManager.formatNumber(result.averageGameLength, 1)} moves")
        
        // Statistical confidence
        result.confidenceInterval?.let { interval ->
            builder.appendLine()
            builder.appendLine("Statistical Analysis:")
            builder.appendLine("  95% confidence interval: ${StatisticalUtils.formatConfidenceInterval(interval)}")
            if (result.statisticalSignificance) {
                builder.appendLine("  Result is statistically significant (p < 0.05)")
            } else {
                builder.appendLine("  Result is not statistically significant (p ≥ 0.05)")
            }
        }
        
        // Color performance breakdown
        val colorStats = result.colorAlternation
        if (colorStats.whiteGames > 0 && colorStats.blackGames > 0) {
            builder.appendLine()
            builder.appendLine("Color Performance:")
            builder.appendLine("  As White: ${colorStats.whiteWins}/${colorStats.whiteGames} " +
                    "(${formatManager.formatPercentage(colorStats.whiteWinRate)}) " +
                    "- ${colorStats.whiteDraws} draws, ${colorStats.whiteLosses} losses")
            builder.appendLine("  As Black: ${colorStats.blackWins}/${colorStats.blackGames} " +
                    "(${formatManager.formatPercentage(colorStats.blackWinRate)}) " +
                    "- ${colorStats.blackDraws} draws, ${colorStats.blackLosses} losses")
            
            // Color bias analysis
            val colorBias = colorStats.whiteWinRate - colorStats.blackWinRate
            if (abs(colorBias) > 0.1) {
                val strongerColor = if (colorBias > 0) "White" else "Black"
                builder.appendLine("  Color bias: ${formatManager.formatPercentage(abs(colorBias))} advantage as $strongerColor")
            } else {
                builder.appendLine("  Color performance is balanced")
            }
        }
        
        // Performance assessment
        builder.appendLine()
        builder.appendLine("Performance Assessment:")
        when {
            result.winRate >= 0.6 -> builder.appendLine("  ✓ Excellent performance (≥60% win rate)")
            result.winRate >= 0.4 -> builder.appendLine("  ✓ Competitive performance (≥40% win rate)")
            result.winRate >= 0.25 -> builder.appendLine("  ⚠ Below competitive threshold (25-40% win rate)")
            else -> builder.appendLine("  ✗ Poor performance (<25% win rate)")
        }
        
        builder.appendLine("═══════════════════════════════════════════════════════════")
        
        return builder.toString()
    }
    
    /**
     * Format comparison results between two agents
     */
    fun formatComparisonResult(result: EnhancedComparisonResults, modelAName: String, modelBName: String): String {
        val builder = StringBuilder()
        
        // Header
        builder.appendLine("═══════════════════════════════════════════════════════════")
        builder.appendLine("                    MODEL COMPARISON")
        builder.appendLine("═══════════════════════════════════════════════════════════")
        builder.appendLine("Model A: $modelAName")
        builder.appendLine("Model B: $modelBName")
        builder.appendLine()
        
        // Basic statistics
        builder.appendLine("Games played: ${result.totalGames}")
        builder.appendLine("Model A wins: ${result.modelAWins} (${formatManager.formatPercentage(result.modelAWinRate)})")
        builder.appendLine("Draws: ${result.draws} (${formatManager.formatPercentage(result.drawRate)})")
        builder.appendLine("Model B wins: ${result.modelBWins} (${formatManager.formatPercentage(result.modelBWinRate)})")
        builder.appendLine("Average game length: ${formatManager.formatNumber(result.averageGameLength, 1)} moves")
        
        // Statistical analysis
        builder.appendLine()
        builder.appendLine("Statistical Analysis:")
        result.confidenceInterval?.let { interval ->
            builder.appendLine("  Model A 95% confidence interval: ${StatisticalUtils.formatConfidenceInterval(interval)}")
        }
        if (!result.pValue.isNaN()) {
            builder.appendLine("  p-value (two-tailed, decisive games): ${formatManager.formatScientific(result.pValue, 4)}")
        }
        if (result.statisticalSignificance) {
            builder.appendLine("  Performance difference is statistically significant (p < 0.05)")
        } else {
            builder.appendLine("  Performance difference is not statistically significant (p ≥ 0.05)")
        }
        builder.appendLine("  Effect size: ${formatManager.formatNumber(result.effectSize, 3)} " +
                "(${StatisticalUtils.interpretEffectSize(result.effectSize)})")
        
        // Color performance for Model A
        val colorStats = result.colorAlternation
        if (colorStats.whiteGames > 0 && colorStats.blackGames > 0) {
            builder.appendLine()
            builder.appendLine("Model A Color Performance:")
            builder.appendLine("  As White: ${colorStats.whiteWins}/${colorStats.whiteGames} " +
                    "(${formatManager.formatPercentage(colorStats.whiteWinRate)})")
            builder.appendLine("  As Black: ${colorStats.blackWins}/${colorStats.blackGames} " +
                    "(${formatManager.formatPercentage(colorStats.blackWinRate)})")
        }
        
        // Performance verdict
        builder.appendLine()
        builder.appendLine("Verdict:")
        val performanceDelta = result.modelAWinRate - result.modelBWinRate
        when {
            performanceDelta > 0.15 -> builder.appendLine("  ✓ Model A is significantly better (+${formatManager.formatPercentage(performanceDelta)})")
            performanceDelta > 0.05 -> builder.appendLine("  ✓ Model A is moderately better (+${formatManager.formatPercentage(performanceDelta)})")
            performanceDelta > -0.05 -> builder.appendLine("  ≈ Models have similar performance (±${formatManager.formatPercentage(abs(performanceDelta))})")
            performanceDelta > -0.15 -> builder.appendLine("  ⚠ Model B is moderately better (+${formatManager.formatPercentage(-performanceDelta)})")
            else -> builder.appendLine("  ✗ Model B is significantly better (+${formatManager.formatPercentage(-performanceDelta)})")
        }
        
        builder.appendLine("═══════════════════════════════════════════════════════════")
        
        return builder.toString()
    }
    
    /**
     * Format multiple evaluation results for comparison
     */
    fun formatMultipleEvaluations(results: List<Pair<String, EnhancedEvaluationResults>>): String {
        if (results.isEmpty()) return "No evaluation results to display."
        if (results.size == 1) return formatEvaluationResult(results.first().second)
        
        val builder = StringBuilder()
        
        // Header
        builder.appendLine("═══════════════════════════════════════════════════════════")
        builder.appendLine("              MULTI-OPPONENT EVALUATION SUMMARY")
        builder.appendLine("═══════════════════════════════════════════════════════════")
        
        // Summary table
        builder.appendLine("Opponent".padEnd(20) + "Games".padEnd(8) + "Win Rate".padEnd(12) + "Avg Length".padEnd(12) + "Significance")
        builder.appendLine("─".repeat(65))
        
        for ((opponentName, result) in results) {
            val significance = if (result.statisticalSignificance) "Yes" else "No"
            builder.appendLine(
                opponentName.take(19).padEnd(20) +
                result.totalGames.toString().padEnd(8) +
                formatManager.formatPercentage(result.winRate).padEnd(12) +
                formatManager.formatNumber(result.averageGameLength, 1).padEnd(12) +
                significance
            )
        }
        
        builder.appendLine()
        
        // Overall performance assessment
        val totalWins = results.sumOf { it.second.wins }
        val totalGames = results.sumOf { it.second.totalGames }
        val overallWinRate = totalWins.toDouble() / totalGames
        
        builder.appendLine("Overall Performance:")
        builder.appendLine("  Total games: $totalGames")
        builder.appendLine("  Overall win rate: ${formatManager.formatPercentage(overallWinRate)}")
        
        // Best and worst performance
        val bestResult = results.maxByOrNull { it.second.winRate }
        val worstResult = results.minByOrNull { it.second.winRate }
        
        bestResult?.let { (name, result) ->
            builder.appendLine("  Best vs: $name (${formatManager.formatPercentage(result.winRate)})")
        }
        worstResult?.let { (name, result) ->
            builder.appendLine("  Worst vs: $name (${formatManager.formatPercentage(result.winRate)})")
        }
        
        builder.appendLine("═══════════════════════════════════════════════════════════")
        
        return builder.toString()
    }
    
    /**
     * Create a concise one-line summary for logging
     */
    fun formatConciseSummary(result: EnhancedEvaluationResults): String {
        val significance = if (result.statisticalSignificance) " (sig)" else ""
        val confidence = result.confidenceInterval?.let { interval ->
            " [${StatisticalUtils.formatConfidenceInterval(interval, 2)}]"
        } ?: ""
        
        return "${result.opponentType}: ${result.wins}/${result.totalGames} " +
               "(${formatManager.formatPercentage(result.winRate)})$confidence$significance"
    }
}
