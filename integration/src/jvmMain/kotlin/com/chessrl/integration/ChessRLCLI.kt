package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.config.ConfigParser
import com.chessrl.integration.config.JvmConfigParser
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.chess.PieceColor
import kotlin.system.exitProcess

/**
 * Simplified Chess RL CLI with 3 essential commands: train, evaluate, play.
 * 
 * Replaces the complex CLIRunner with a clean, focused interface that uses
 * the consolidated TrainingPipeline and configuration system.
 */
object ChessRLCLI {
    
    private val logger = ChessRLLogger.forComponent("ChessRLCLI")
    
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            if (args.isEmpty()) {
                printHelp()
                return
            }
            
            when (args[0]) {
                "--train" -> handleTrain(args)
                "--evaluate" -> handleEvaluate(args)
                "--play" -> handlePlay(args)
                "--help", "-h" -> printHelp()
                else -> {
                    println("Unknown command: ${args[0]}")
                    println("Use --help for usage information")
                    exitProcess(1)
                }
            }
        } catch (e: Exception) {
            logger.error("CLI execution failed", e)
            println("Error: ${e.message}")
            exitProcess(1)
        }
    }
    
    /**
     * Handle training command: --train
     */
    private fun handleTrain(args: Array<String>) {
        logger.info("Starting training command")
        
        val config = parseTrainingConfig(args)
        logger.info("Training configuration loaded: profile=${getProfileName(args)}")
        
        val pipeline = TrainingPipeline(config)
        
        if (!pipeline.initialize()) {
            logger.error("Failed to initialize training pipeline")
            println("Error: Failed to initialize training pipeline")
            exitProcess(1)
        }
        
        logger.info("Training pipeline initialized successfully")
        println("Starting training with ${config.maxCycles} cycles, ${config.gamesPerCycle} games per cycle")
        
        val results = pipeline.runTraining()
        
        logger.info("Training completed successfully")
        println("Training completed!")
        println("Total cycles: ${results.totalCycles}")
        println("Total games: ${results.totalGamesPlayed}")
        println("Final performance: ${results.finalMetrics.averageReward}")
        println("Best performance: ${results.bestPerformance}")
        
        // Note: Best model path is handled by the training pipeline's checkpoint system
        println("Models saved to: ${config.checkpointDirectory}")
    }
    
    /**
     * Handle evaluation command: --evaluate
     */
    private fun handleEvaluate(args: Array<String>) {
        logger.info("Starting evaluation command")
        
        when {
            args.contains("--baseline") -> handleEvaluateBaseline(args)
            args.contains("--compare") -> handleEvaluateCompare(args)
            else -> {
                println("Error: --evaluate requires either --baseline or --compare")
                println("Examples:")
                println("  --evaluate --baseline --model best-model.json --games 100")
                println("  --evaluate --compare --modelA model1.json --modelB model2.json --games 50")
                exitProcess(1)
            }
        }
    }
    
    /**
     * Handle baseline evaluation: --evaluate --baseline
     */
    private fun handleEvaluateBaseline(args: Array<String>) {
        val config = parseEvaluationConfig(args)
        val modelPath = getRequiredArg(args, "--model", "Model path is required for baseline evaluation")
        val games = getIntArg(args, "--games") ?: config.evaluationGames
        val opponent = getStringArg(args, "--opponent") ?: "heuristic"
        
        logger.info("Evaluating model $modelPath against $opponent baseline over $games games")
        
        val evaluator = BaselineEvaluator(config)
        val agent = loadAgent(modelPath, config)
        
        val results = when (opponent.lowercase()) {
            "heuristic" -> evaluator.evaluateAgainstHeuristic(agent, games)
            "minimax" -> {
                val depth = getIntArg(args, "--depth") ?: 2
                evaluator.evaluateAgainstMinimax(agent, games, depth)
            }
            else -> {
                println("Error: Unknown opponent type '$opponent'. Use 'heuristic' or 'minimax'")
                exitProcess(1)
            }
        }
        
        printEvaluationResults(results, opponent)
    }
    
    /**
     * Handle model comparison: --evaluate --compare
     */
    private fun handleEvaluateCompare(args: Array<String>) {
        val config = parseEvaluationConfig(args)
        val modelAPath = getRequiredArg(args, "--modelA", "Model A path is required for comparison")
        val modelBPath = getRequiredArg(args, "--modelB", "Model B path is required for comparison")
        val games = getIntArg(args, "--games") ?: 100
        
        logger.info("Comparing models: $modelAPath vs $modelBPath over $games games")
        
        val evaluator = BaselineEvaluator(config)
        val agentA = loadAgent(modelAPath, config)
        val agentB = loadAgent(modelBPath, config)
        
        val results = evaluator.compareModels(agentA, agentB, games)
        
        printComparisonResults(results, modelAPath, modelBPath)
    }
    
    /**
     * Handle play command: --play
     */
    private fun handlePlay(args: Array<String>) {
        val config = parsePlayConfig(args)
        val modelPath = getRequiredArg(args, "--model", "Model path is required for play")
        val humanColor = getStringArg(args, "--as")?.let { 
            when (it.lowercase()) {
                "white" -> PieceColor.WHITE
                "black" -> PieceColor.BLACK
                else -> {
                    println("Error: --as must be 'white' or 'black'")
                    exitProcess(1)
                }
            }
        } ?: PieceColor.WHITE
        
        logger.info("Starting interactive play: human as $humanColor vs model $modelPath")
        
        val agent = loadAgent(modelPath, config)
        val gameInterface = InteractiveGameInterface(agent, humanColor, config)
        
        println("Starting interactive chess game!")
        println("You are playing as ${humanColor.name.lowercase()}")
        println("Enter moves in algebraic notation (e.g., e4, Nf3, O-O)")
        println("Type 'quit' to exit")
        
        gameInterface.playGame()
    }
    
    /**
     * Parse configuration for training command
     */
    private fun parseTrainingConfig(args: Array<String>): ChessRLConfig {
        val profileName = getStringArg(args, "--profile")
        
        return if (profileName != null) {
            // Load from profile first, then apply CLI overrides
            val baseConfig = JvmConfigParser.loadProfile(profileName)
            applyCliOverrides(baseConfig, args)
        } else {
            // Parse from CLI args with defaults
            ConfigParser.parseArgs(args)
        }
    }
    
    /**
     * Parse configuration for evaluation command
     */
    private fun parseEvaluationConfig(args: Array<String>): ChessRLConfig {
        val profileName = getStringArg(args, "--profile") ?: "eval-only"
        val baseConfig = JvmConfigParser.loadProfile(profileName)
        return applyCliOverrides(baseConfig, args)
    }
    
    /**
     * Parse configuration for play command
     */
    private fun parsePlayConfig(args: Array<String>): ChessRLConfig {
        val baseConfig = ChessRLConfig(
            maxStepsPerGame = 200, // Longer games for human play
            maxConcurrentGames = 1, // Single-threaded for interactive play
            seed = null // Random for variety
        )
        return applyCliOverrides(baseConfig, args)
    }
    
    /**
     * Apply CLI argument overrides to base configuration
     */
    private fun applyCliOverrides(baseConfig: ChessRLConfig, args: Array<String>): ChessRLConfig {
        var config = baseConfig
        
        getIntArg(args, "--cycles")?.let { config = config.copy(maxCycles = it) }
        getIntArg(args, "--games")?.let { config = config.copy(gamesPerCycle = it) }
        getIntArg(args, "--max-steps")?.let { config = config.copy(maxStepsPerGame = it) }
        getIntArg(args, "--batch-size")?.let { config = config.copy(batchSize = it) }
        getDoubleArg(args, "--learning-rate")?.let { config = config.copy(learningRate = it) }
        getLongArg(args, "--seed")?.let { config = config.copy(seed = it) }
        getStringArg(args, "--checkpoint-dir")?.let { config = config.copy(checkpointDirectory = it) }
        
        return config
    }
    
    /**
     * Load agent from model file
     */
    private fun loadAgent(modelPath: String, config: ChessRLConfig): ChessAgent {
        val agent = ChessAgentFactory.createDQNAgent(hiddenLayers = config.hiddenLayers)
        
        try {
            agent.load(modelPath)
            logger.info("Successfully loaded model from $modelPath")
        } catch (e: Exception) {
            logger.error("Failed to load model from $modelPath", e)
            println("Error: Failed to load model from $modelPath: ${e.message}")
            exitProcess(1)
        }
        
        return agent
    }
    
    /**
     * Print evaluation results
     */
    private fun printEvaluationResults(results: EvaluationResults, opponent: String) {
        println("\nEvaluation Results vs $opponent:")
        println("Games played: ${results.totalGames}")
        println("Win rate: ${String.format("%.1f%%", results.winRate * 100)}")
        println("Draw rate: ${String.format("%.1f%%", results.drawRate * 100)}")
        println("Loss rate: ${String.format("%.1f%%", results.lossRate * 100)}")
        println("Average game length: ${String.format("%.1f", results.averageGameLength)}")
        
        if (results.winRate >= 0.4) {
            println("✓ Agent performance is competitive (≥40% win rate)")
        } else {
            println("⚠ Agent performance below competitive threshold (<40% win rate)")
        }
    }
    
    /**
     * Print model comparison results
     */
    private fun printComparisonResults(results: ComparisonResults, modelAPath: String, modelBPath: String) {
        println("\nModel Comparison Results:")
        println("Model A: $modelAPath")
        println("Model B: $modelBPath")
        println("Games played: ${results.totalGames}")
        println("Model A wins: ${results.modelAWins} (${String.format("%.1f%%", results.modelAWinRate * 100)})")
        println("Draws: ${results.draws} (${String.format("%.1f%%", results.drawRate * 100)})")
        println("Model B wins: ${results.modelBWins} (${String.format("%.1f%%", results.modelBWinRate * 100)})")
        println("Average game length: ${String.format("%.1f", results.averageGameLength)}")
        
        when {
            results.modelAWinRate > 0.55 -> println("✓ Model A is significantly better")
            results.modelBWinRate > 0.55 -> println("✓ Model B is significantly better")
            else -> println("≈ Models have similar performance")
        }
    }
    
    /**
     * Get profile name from arguments for logging
     */
    private fun getProfileName(args: Array<String>): String? {
        return getStringArg(args, "--profile")
    }
    
    // Utility functions for argument parsing
    private fun getStringArg(args: Array<String>, flag: String): String? {
        val index = args.indexOf(flag)
        return if (index >= 0 && index + 1 < args.size) args[index + 1] else null
    }
    
    private fun getIntArg(args: Array<String>, flag: String): Int? {
        return getStringArg(args, flag)?.toIntOrNull()
    }
    
    private fun getDoubleArg(args: Array<String>, flag: String): Double? {
        return getStringArg(args, flag)?.toDoubleOrNull()
    }
    
    private fun getLongArg(args: Array<String>, flag: String): Long? {
        return getStringArg(args, flag)?.toLongOrNull()
    }
    
    private fun getRequiredArg(args: Array<String>, flag: String, errorMessage: String): String {
        return getStringArg(args, flag) ?: run {
            println("Error: $errorMessage")
            exitProcess(1)
        }
    }
    
    /**
     * Print help information
     */
    private fun printHelp() {
        println("Chess RL Bot - Simplified CLI")
        println()
        println("USAGE:")
        println("  chess-rl <COMMAND> [OPTIONS]")
        println()
        println("COMMANDS:")
        println("  --train                    Train a chess RL agent")
        println("  --evaluate                 Evaluate trained agents")
        println("  --play                     Play against a trained agent")
        println("  --help, -h                 Show this help message")
        println()
        println("TRAINING:")
        println("  --train [OPTIONS]")
        println("    --profile <name>         Configuration profile (fast-debug, long-train)")
        println("    --cycles <n>             Number of training cycles (default: from profile)")
        println("    --games <n>              Games per cycle (default: from profile)")
        println("    --seed <n>               Random seed for reproducibility")
        println("    --checkpoint-dir <dir>   Checkpoint directory")
        println("    --learning-rate <rate>   Learning rate override")
        println("    --batch-size <size>      Batch size override")
        println()
        println("EVALUATION:")
        println("  --evaluate --baseline [OPTIONS]")
        println("    --model <path>           Model file to evaluate")
        println("    --games <n>              Number of evaluation games (default: 100)")
        println("    --opponent <type>        Opponent type: heuristic, minimax (default: heuristic)")
        println("    --depth <n>              Minimax depth for minimax opponent (default: 2)")
        println("    --seed <n>               Random seed for reproducibility")
        println()
        println("  --evaluate --compare [OPTIONS]")
        println("    --modelA <path>          First model file")
        println("    --modelB <path>          Second model file")
        println("    --games <n>              Number of comparison games (default: 100)")
        println("    --seed <n>               Random seed for reproducibility")
        println()
        println("PLAY:")
        println("  --play [OPTIONS]")
        println("    --model <path>           Model file to play against")
        println("    --as <color>             Human player color: white, black (default: white)")
        println()
        println("PROFILES:")
        println("  fast-debug                Quick training for development (5 games, 10 cycles)")
        println("  long-train                Production training (50 games, 200 cycles)")
        println("  eval-only                 Evaluation settings (500 games, deterministic)")
        println()
        println("EXAMPLES:")
        println("  # Quick development training")
        println("  chess-rl --train --profile fast-debug --seed 12345")
        println()
        println("  # Production training with custom parameters")
        println("  chess-rl --train --profile long-train --cycles 100 --learning-rate 0.001")
        println()
        println("  # Evaluate against heuristic baseline")
        println("  chess-rl --evaluate --baseline --model checkpoints/best-model.json --games 200")
        println()
        println("  # Evaluate against minimax depth-3")
        println("  chess-rl --evaluate --baseline --model best-model.json --opponent minimax --depth 3")
        println()
        println("  # Compare two models")
        println("  chess-rl --evaluate --compare --modelA model1.json --modelB model2.json --games 100")
        println()
        println("  # Play as black against trained agent")
        println("  chess-rl --play --model checkpoints/best-model.json --as black")
    }
}