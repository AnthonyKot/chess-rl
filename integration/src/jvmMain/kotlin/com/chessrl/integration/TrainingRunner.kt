package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.config.ConfigParser

/**
 * Minimal runner for the consolidated TrainingPipeline.
 *
 * Examples:
 *  - java -cp <all-jvm-jars> com.chessrl.integration.TrainingRunnerKt --profile fast-debug --max-cycles 2
 *  - java -cp <all-jvm-jars> com.chessrl.integration.TrainingRunnerKt --games-per-cycle 5 --max-cycles 2 --evaluation-games 20
 */
fun main(args: Array<String>) {
    // Parse config from CLI (built-in profiles supported: fast-debug, long-train, eval-only)
    val cfg = safeParse(args)

    println("Using configuration:\n${cfg.getSummary()}")

    val pipeline = TrainingPipeline(cfg)
    if (!pipeline.initialize()) {
        println("Initialization failed; aborting")
        return
    }

    val results = pipeline.runTraining()

    println("\n===== Training Summary =====")
    println("Cycles: ${results.totalCycles}")
    println("Best performance: ${"%.4f".format(results.bestPerformance)}")
    println("Games played: ${results.totalGamesPlayed}")
    println("Experiences: ${results.totalExperiencesCollected}")
    println("Duration: ${results.totalDuration} ms")
}

private fun safeParse(args: Array<String>): ChessRLConfig {
    return try {
        ConfigParser.parseArgs(args)
    } catch (e: Exception) {
        println("⚠️ Failed to parse args with ConfigParser; falling back to fast-debug. Reason: ${e.message}")
        ChessRLConfig().forFastDebug()
    }
}
