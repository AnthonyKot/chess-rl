package com.chessrl.integration

import com.chessrl.integration.backend.BackendFactory
import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.config.ConfigParser
import com.chessrl.integration.logging.ChessRLLogger

// Main entry point for the Chess RL system.
// Run with --help to see all options.
object ChessRLCLI {

    private val logger = ChessRLLogger.forComponent("CLI")

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty() || "--help" in args || "-h" in args) {
            printHelp()
            return
        }

        val config = ConfigParser.parseArgs(args)

        when {
            "--train" in args -> runTraining(config)
            "--evaluate" in args -> runEvaluation(config, args)
            else -> {
                System.err.println("No command specified. Use --train, --evaluate, or --help.")
                System.exit(1)
            }
        }
    }

    private fun runTraining(config: ChessRLConfig) {
        logger.info("Chess RL Training")
        logger.info("Backend: ${config.nnBackend}")

        when (config.nnBackend) {
            BackendType.ALPHAZERO -> {
                logger.info("Using AlphaZero MCTS pipeline")
                val pipeline = AlphaZeroTrainingPipeline(config)
                if (!pipeline.initialize()) {
                    System.err.println("AlphaZero pipeline initialization failed.")
                    System.exit(1)
                }
                pipeline.runTraining()
            }
            else -> {
                logger.info("Using DQN pipeline (backend=${config.nnBackend})")
                val backend = BackendFactory.createBackend(config.nnBackend, config)
                val pipeline = TrainingPipeline(config, backend)
                if (!pipeline.initialize()) {
                    System.err.println("Training pipeline initialization failed.")
                    System.exit(1)
                }
                pipeline.runTraining()
            }
        }
    }

    private fun runEvaluation(config: ChessRLConfig, args: Array<String>) {
        val modelPath = args.indexOf("--model").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }
        logger.info("Evaluation mode — model: ${modelPath ?: "(none)"}")
        println("Evaluation not yet wired to CLI; use --train with eval enabled in the pipeline.")
    }

    private fun printHelp() {
        println("""
Chess RL — AlphaZero & DQN training platform

USAGE
  ./gradlew :integration:run --args="<command> [flags]"

COMMANDS
  --train       Run the training pipeline
  --evaluate    Evaluate a trained model against the heuristic baseline
  --help        Print this help

BACKEND SELECTION
  --nn alphazero        AlphaZero MCTS (recommended — much stronger than DQN)
  --nn rl4j             RL4J DQN (default)
  --nn manual           Manual DQN implementation

COMMON FLAGS
  --profile NAME           Load a profile from integration/profiles.yaml
                           Profiles: alphazero-fast, alphazero-train,
                                     fast-debug, long-train, eval-only
  --cycles N               Override max training cycles
  --games-per-cycle N      Override games per cycle
  --seed N                 Fix random seed for reproducibility

ALPHAZERO FLAGS  (only when --nn alphazero)
  --mcts-simulations N     Simulations per move  (default 100)
  --mcts-c-puct F          UCB exploration constant  (default 1.5)
  --mcts-temperature F     Sampling temperature  (1.0 = train, 0.0 = greedy)
  --mcts-recent-games N    Rolling training buffer size  (default 500)

EXAMPLES
  # Fast AlphaZero run — verify the pipeline
  ./gradlew :integration:run --args="--train --profile alphazero-fast"

  # Full AlphaZero production run
  ./gradlew :integration:run --args="--train --profile alphazero-train"

  # Legacy DQN training
  ./gradlew :integration:run --args="--train --profile fast-debug"

  # Custom AlphaZero with 200 simulations
  ./gradlew :integration:run --args="--train --nn alphazero --mcts-simulations 200 --cycles 50"
        """.trimIndent())
    }
}
