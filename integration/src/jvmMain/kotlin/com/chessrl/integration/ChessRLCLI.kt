package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.config.ConfigParser
import com.chessrl.integration.config.JvmConfigParser
import com.chessrl.integration.config.DomainConfigLoader
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.chess.PieceColor
import com.chessrl.integration.output.EnhancedComparisonResults
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
            // Configure logging level early, if provided
            applyLoggingLevel(args)
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

    private fun applyLoggingLevel(args: Array<String>) {
        val lvl = getStringArg(args, "--log-level")?.lowercase()
        val level = when (lvl) {
            "debug" -> ChessRLLogger.LogLevel.DEBUG
            "warn" -> ChessRLLogger.LogLevel.WARN
            "error" -> ChessRLLogger.LogLevel.ERROR
            else -> ChessRLLogger.LogLevel.INFO
        }
        ChessRLLogger.configure(level = level)
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
        
        // Optional resume: --load <path> or --resume (auto-load best from checkpointDirectory)
        val explicitLoad = getStringArg(args, "--load")
        val resumeRequested = args.contains("--resume")
        if (explicitLoad != null || resumeRequested) {
            val modelPath = explicitLoad ?: resolveBestModelPath(config)
            if (modelPath == null) {
                println("Warning: Resume requested but no model found to load. Continuing without resume.")
            } else {
                val ok = pipeline.loadInitialModel(modelPath)
                if (ok) println("Resumed training from $modelPath") else println("Warning: Failed to resume from $modelPath")
            }
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
        val provided = getStringArg(args, "--model")
        val modelPath = (if (provided != null) resolveModelPathArg(provided) else resolveBestModelPath(config))
            ?: run {
                println("Error: No --model provided and no best checkpoint found in '${config.checkpointDirectory}'.")
                println("Hint: Provide --model <path> or ensure checkpoints exist in ${config.checkpointDirectory}")
                exitProcess(1)
            }
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
        
        printEvaluationResults(results)
    }
    
    /**
     * Handle model comparison: --evaluate --compare
     */
    private fun handleEvaluateCompare(args: Array<String>) {
        val config = parseEvaluationConfig(args)
        val modelAArg = getRequiredArg(args, "--modelA", "Model A path is required for comparison")
        val modelBArg = getRequiredArg(args, "--modelB", "Model B path is required for comparison")
        val modelAPath = resolveModelPathArg(modelAArg) ?: run {
            println("Error: Could not resolve model A from '$modelAArg'")
            exitProcess(1)
        }
        val modelBPath = resolveModelPathArg(modelBArg) ?: run {
            println("Error: Could not resolve model B from '$modelBArg'")
            exitProcess(1)
        }
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
        val verboseEngine = getBooleanArg(args, "--verbose") ?: false
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
        
        // Create play session config
        val profileName = getStringArg(args, "--profile") ?: getStringArg(args, "--config") ?: "fast-debug"
        val playConfig = com.chessrl.integration.output.PlaySessionConfig(
            profileUsed = profileName,
            maxSteps = config.maxStepsPerGame,
            verboseEngineOutput = verboseEngine
        )
        
        val gameInterface = InteractiveGameInterface(agent, humanColor, config, playConfig)
        
        gameInterface.playGame()
    }
    
    /**
     * Parse configuration for training command
     */
    private fun parseTrainingConfig(args: Array<String>): ChessRLConfig {
        // Highest priority: root config if provided
        val configName = getStringArg(args, "--config")
        if (configName != null) {
            var cfg = DomainConfigLoader.loadRootConfig(configName)
            cfg = applyCliOverrides(cfg, args)
            cfg = applyGenericOverrides(cfg, args)
            return cfg
        }

        val profileSpec = getStringArg(args, "--profile")
        if (profileSpec != null) {
            val baseConfig = try {
                if (profileSpec.contains(",") || profileSpec.contains(":")) {
                    DomainConfigLoader.composeFromProfiles(profileSpec)
                } else {
                    JvmConfigParser.loadProfile(profileSpec)
                }
            } catch (_: Exception) {
                DomainConfigLoader.composeFromProfiles(profileSpec)
            }
            var cfg = applyCliOverrides(baseConfig, args)
            cfg = applyGenericOverrides(cfg, args)
            return cfg
        }

        // No profile/config provided: parse essential flags only, then generic overrides
        var cfg = ConfigParser.parseArgs(args)
        cfg = applyGenericOverrides(cfg, args)
        return cfg
    }
    
    /**
     * Parse configuration for evaluation command
     */
    private fun parseEvaluationConfig(args: Array<String>): ChessRLConfig {
        val configName = getStringArg(args, "--config")
        if (configName != null) {
            var cfg = DomainConfigLoader.loadRootConfig(configName)
            cfg = applyCliOverrides(cfg, args)
            cfg = applyGenericOverrides(cfg, args)
            return cfg
        }

        val profileSpec = getStringArg(args, "--profile")
        val baseConfig = if (profileSpec != null) {
            try {
                if (profileSpec.contains(",") || profileSpec.contains(":")) {
                    DomainConfigLoader.composeFromProfiles(profileSpec)
                } else {
                    JvmConfigParser.loadProfile(profileSpec)
                }
            } catch (_: Exception) {
                DomainConfigLoader.composeFromProfiles(profileSpec)
            }
        } else {
            JvmConfigParser.loadProfile("eval-only")
        }

        var cfg = applyCliOverrides(baseConfig, args)
        // Eval epsilon convenience
        getDoubleArg(args, "--eval-epsilon")?.let { ee -> cfg = cfg.copy(explorationRate = ee) }
        // Evaluation environment knobs
        getBoolArg(args, "--eval-early-adjudication")?.let { v -> cfg = cfg.copy(evalEarlyAdjudication = v) }
        getIntArg(args, "--eval-resign-threshold")?.let { v -> cfg = cfg.copy(evalResignMaterialThreshold = v) }
        getIntArg(args, "--eval-no-progress-plies")?.let { v -> cfg = cfg.copy(evalNoProgressPlies = v) }
        cfg = applyGenericOverrides(cfg, args)
        return cfg
    }
    
    /**
     * Parse configuration for play command
     */
    private fun parsePlayConfig(args: Array<String>): ChessRLConfig {
        val configName = getStringArg(args, "--config")
        if (configName != null) {
            var cfg = DomainConfigLoader.loadRootConfig(configName)
            cfg = applyCliOverrides(cfg, args)
            cfg = applyGenericOverrides(cfg, args)
            return cfg
        }

        val profileSpec = getStringArg(args, "--profile")
        val baseConfig = if (profileSpec != null) {
            try {
                if (profileSpec.contains(",") || profileSpec.contains(":")) {
                    DomainConfigLoader.composeFromProfiles(profileSpec)
                } else {
                    JvmConfigParser.loadProfile(profileSpec)
                }
            } catch (_: Exception) {
                DomainConfigLoader.composeFromProfiles(profileSpec)
            }
        } else {
            JvmConfigParser.loadProfile("fast-debug").copy(
                maxStepsPerGame = 200, // Longer games for human play
                maxConcurrentGames = 1, // Single-threaded for interactive play
                seed = null // Random for variety
            )
        }

        var cfg = applyCliOverrides(baseConfig, args)
        cfg = applyGenericOverrides(cfg, args)
        return cfg
    }
    
    /**
     * Apply CLI argument overrides to base configuration
     */
    private fun applyCliOverrides(baseConfig: ChessRLConfig, args: Array<String>): ChessRLConfig {
        var config = baseConfig

        // Essential overrides
        getIntArg(args, "--cycles")?.let { config = config.copy(maxCycles = it) }
        getIntArg(args, "--games")?.let { config = config.copy(gamesPerCycle = it) }
        getIntArg(args, "--max-steps")?.let { config = config.copy(maxStepsPerGame = it) }
        getIntArg(args, "--batch-size")?.let { config = config.copy(batchSize = it) }
        getDoubleArg(args, "--learning-rate")?.let { config = config.copy(learningRate = it) }
        getLongArg(args, "--seed")?.let { config = config.copy(seed = it) }
        getStringArg(args, "--checkpoint-dir")?.let { config = config.copy(checkpointDirectory = it) }

        // Expanded set for fine-tuning
        getStringArg(args, "--hidden-layers")?.let { config = config.copy(hiddenLayers = parseHiddenLayers(it)) }
        getIntArg(args, "--max-concurrent-games")?.let { config = config.copy(maxConcurrentGames = it) }
        getDoubleArg(args, "--exploration-rate")?.let { config = config.copy(explorationRate = it) }
        getIntArg(args, "--target-update-frequency")?.let { config = config.copy(targetUpdateFrequency = it) }
        getIntArg(args, "--max-experience-buffer")?.let { config = config.copy(maxExperienceBuffer = it) }
        getIntArg(args, "--checkpoint-interval")?.let { config = config.copy(checkpointInterval = it) }
        if (args.contains("--double-dqn")) { config = config.copy(doubleDqn = true) }
        getStringArg(args, "--replay-type")?.let { config = config.copy(replayType = it) }
        getDoubleArg(args, "--gamma")?.let { config = config.copy(gamma = it) }
        getIntArg(args, "--max-batches-per-cycle")?.let { config = config.copy(maxBatchesPerCycle = it) }
        getDoubleArg(args, "--win-reward")?.let { config = config.copy(winReward = it) }
        getDoubleArg(args, "--loss-reward")?.let { config = config.copy(lossReward = it) }
        getDoubleArg(args, "--draw-reward")?.let { config = config.copy(drawReward = it) }
        getDoubleArg(args, "--step-limit-penalty")?.let { config = config.copy(stepLimitPenalty = it) }
        getIntArg(args, "--evaluation-games")?.let { config = config.copy(evaluationGames = it) }
        // Training environment controls
        getBoolArg(args, "--train-early-adjudication")?.let { config = config.copy(trainEarlyAdjudication = it) }
        getIntArg(args, "--train-resign-threshold")?.let { config = config.copy(trainResignMaterialThreshold = it) }
        getIntArg(args, "--train-no-progress-plies")?.let { config = config.copy(trainNoProgressPlies = it) }
        // Training opponent controls
        getStringArg(args, "--train-opponent")?.let { config = config.copy(trainOpponentType = it) }
        getIntArg(args, "--train-opponent-depth")?.let { config = config.copy(trainOpponentDepth = it) }

        // Checkpoint manager controls
        getIntArg(args, "--checkpoint-max-versions")?.let { config = config.copy(checkpointMaxVersions = it) }
        getBoolArg(args, "--checkpoint-validation")?.let { config = config.copy(checkpointValidationEnabled = it) }
        getBoolArg(args, "--checkpoint-compression")?.let { config = config.copy(checkpointCompressionEnabled = it) }
        getBoolArg(args, "--checkpoint-autocleanup")?.let { config = config.copy(checkpointAutoCleanupEnabled = it) }

        // Logging controls
        getIntArg(args, "--log-interval")?.let { config = config.copy(logInterval = it) }
        if (args.contains("--summary-only")) { config = config.copy(summaryOnly = true) }
        getStringArg(args, "--metrics-file")?.let { config = config.copy(metricsFile = it) }

        return config
    }

    private fun applyGenericOverrides(base: ChessRLConfig, args: Array<String>): ChessRLConfig {
        val pairs = getAllArgs(args, "--override")
        if (pairs.isEmpty()) return base
        val overrides = mutableMapOf<String, String>()
        pairs.forEach { kv ->
            val idx = kv.indexOf('=')
            if (idx > 0) {
                val key = kv.substring(0, idx).trim()
                val value = kv.substring(idx + 1).trim()
                if (key.isNotEmpty()) overrides[key] = value
            }
        }
        return if (overrides.isEmpty()) base else DomainConfigLoader.applyOverrides(base, overrides)
    }

    private fun getBoolArg(args: Array<String>, flag: String): Boolean? {
        val raw = getStringArg(args, flag) ?: return null
        return when (raw.lowercase()) {
            "true", "1", "yes", "on" -> true
            "false", "0", "no", "off" -> false
            else -> null
        }
    }
    
    /**
     * Load agent from model file
     */
    private fun loadAgent(modelPath: String, config: ChessRLConfig): ChessAgent {
        // Initialize SeedManager for consistent agent creation
        if (config.seed != null) {
            SeedManager.initializeWithSeed(config.seed)
        } else {
            SeedManager.initializeWithRandomSeed()
        }
        
        val agent = ChessAgentFactory.createSeededDQNAgent(
            hiddenLayers = config.hiddenLayers,
            learningRate = config.learningRate,
            explorationRate = config.explorationRate,
            config = ChessAgentConfig(
                batchSize = config.batchSize,
                maxBufferSize = config.maxExperienceBuffer,
                targetUpdateFrequency = config.targetUpdateFrequency
            ),
            replayType = config.replayType,
            gamma = config.gamma
        )
        
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
    private fun printEvaluationResults(results: com.chessrl.integration.output.EnhancedEvaluationResults) {
        val formatter = com.chessrl.integration.output.EvaluationResultFormatter()
        println(formatter.formatEvaluationResult(results))
    }
    
    /**
     * Print model comparison results
     */
    private fun printComparisonResults(results: EnhancedComparisonResults, modelAPath: String, modelBPath: String) {
        val formatter = com.chessrl.integration.output.EvaluationResultFormatter()
        println(formatter.formatComparisonResult(results, modelAPath, modelBPath))
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
    
    private fun getBooleanArg(args: Array<String>, flag: String): Boolean? {
        return if (args.contains(flag)) true else null
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

    // Try to resolve the best model from CheckpointManager artifacts first, then fallback to compat best_model.json
    private fun resolveBestModelPath(config: ChessRLConfig): String? {
        val dir = java.nio.file.Path.of(config.checkpointDirectory)
        if (!java.nio.file.Files.exists(dir)) return null

        // 1) Prefer manager sidecar meta files *_qnet_meta.json and pick best
        val metas = try {
            java.nio.file.Files.list(dir)
                .filter { p -> p.fileName.toString().endsWith("_qnet_meta.json") }
                .collect(java.util.stream.Collectors.toList())
        } catch (_: Throwable) { emptyList<java.nio.file.Path>() }

        if (metas.isNotEmpty()) {
            data class Candidate(val model: String, val perf: Double, val isBest: Boolean)
            val candidates = metas.mapNotNull { metaPath ->
                val text = runCatching { java.nio.file.Files.readString(metaPath) }.getOrNull() ?: return@mapNotNull null
                val perf = extractDouble(text, "\"performance\"\\s*:\\s*([-0-9.eE]+)") ?: return@mapNotNull null
                val isBest = extractBool(text, "\"isBest\"\\s*:\\s*(true|false)") ?: false
                val modelPath = metaPath.toString().replace("_qnet_meta.json", "_qnet.json")
                Candidate(modelPath, perf, isBest)
            }
            if (candidates.isNotEmpty()) {
                val best = candidates
                    .sortedWith(compareByDescending<Candidate> { it.isBest }.thenByDescending { it.perf })
                    .first()
                if (java.nio.file.Files.exists(java.nio.file.Path.of(best.model))) {
                    println("Auto-loaded best model from CheckpointManager: ${best.model} (perf=${"%.4f".format(best.perf)})")
                    return best.model
                }
            }
        }

        // 2) Fallback to compat best_model.json
        val compat = dir.resolve("best_model.json")
        if (java.nio.file.Files.exists(compat)) {
            println("Auto-loaded compat best model: ${compat}")
            return compat.toString()
        }
        return null
    }

    private fun extractDouble(text: String, regex: String): Double? {
        return runCatching {
            val m = Regex(regex).find(text) ?: return null
            m.groupValues[1].toDouble()
        }.getOrNull()
    }

    private fun extractBool(text: String, regex: String): Boolean? {
        return runCatching {
            val m = Regex(regex).find(text) ?: return null
            m.groupValues[1].equals("true", ignoreCase = true)
        }.getOrNull()
    }

    // Resolve a model argument that may be a file or directory.
    // - If a directory: prefer *_qnet_meta.json -> corresponding *_qnet.json with isBest true or highest performance;
    //   else newest *_qnet.json; else best_model.json.
    // - If a file: return it as-is.
    // - If prefixed with "integration/" and not found, retry without the prefix (common invocation mistake).
    private fun resolveModelPathArg(pathOrDir: String): String? {
        fun exists(p: String): Boolean = runCatching { java.nio.file.Files.exists(java.nio.file.Path.of(p)) }.getOrNull() == true
        fun isDir(p: String): Boolean = runCatching { java.nio.file.Files.isDirectory(java.nio.file.Path.of(p)) }.getOrNull() == true

        var candidate = pathOrDir
        if (!exists(candidate) && candidate.startsWith("integration/")) {
            candidate = candidate.removePrefix("integration/")
        }

        if (!exists(candidate)) {
            // Try relative to repo root (one level up)
            val up = "../$candidate"
            if (exists(up)) candidate = up else return null
        }

        if (!isDir(candidate)) return candidate // file path

        // Directory: scan for best model
        val dir = java.nio.file.Path.of(candidate)
        val entries = try {
            java.nio.file.Files.list(dir).use { it.toList() }
        } catch (_: Throwable) { emptyList<java.nio.file.Path>() }

        // 0) Prefer compat best_model.json if present (most explicit “best”)
        run {
            val compat = dir.resolve("best_model.json")
            if (java.nio.file.Files.exists(compat)) {
                logger.info("Resolved model in $candidate -> ${compat.toString()} (best_model.json)")
                return compat.toString()
            }
        }

        // 1) Prefer meta files *_qnet_meta.json
        val metas = entries.filter { it.fileName.toString().endsWith("_qnet_meta.json") }
        if (metas.isNotEmpty()) {
            data class Cand(val json: String, val perf: Double, val isBest: Boolean, val mtime: Long)
            val cands = metas.mapNotNull { meta ->
                val text = runCatching { java.nio.file.Files.readString(meta) }.getOrNull() ?: return@mapNotNull null
                val perf = extractDouble(text, "\\\"performance\\\"\\s*:\\s*([-0-9.eE]+)") ?: 0.0
                val isBest = extractBool(text, "\\\"isBest\\\"\\s*:\\s*(true|false)") ?: false
                val json = meta.toString().replace("_qnet_meta.json", "_qnet.json")
                val mt = runCatching { java.nio.file.Files.getLastModifiedTime(meta).toMillis() }.getOrNull() ?: 0L
                if (exists(json)) Cand(json, perf, isBest, mt) else null
            }
            if (cands.isNotEmpty()) {
                val best = cands.sortedWith(compareByDescending<Cand> { it.isBest }
                    .thenByDescending { it.perf }
                    .thenByDescending { it.mtime }).first()
                logger.info("Resolved model in $candidate -> ${best.json} (meta: isBest=${best.isBest}, perf=${"%.4f".format(best.perf)})")
                return best.json
            }
        }

        // 2) Fallback to newest *_qnet.json
        val qnets = entries.filter { it.fileName.toString().endsWith("_qnet.json") }
        if (qnets.isNotEmpty()) {
            val best = qnets.maxByOrNull { runCatching { java.nio.file.Files.getLastModifiedTime(it).toMillis() }.getOrNull() ?: 0L }
            if (best != null) {
                logger.info("Resolved model in $candidate -> ${best.toString()} (newest *_qnet.json)")
                return best.toString()
            }
        }

        // 3) Fallback to compat best_model.json
        // (handled above)

        // 4) Last resort: checkpoint_cycle_*.json (uncompressed)
        val cycles = entries.filter { val n = it.fileName.toString(); n.startsWith("checkpoint_cycle_") && n.endsWith(".json") }
        if (cycles.isNotEmpty()) {
            val latest = cycles.maxByOrNull { runCatching { java.nio.file.Files.getLastModifiedTime(it).toMillis() }.getOrNull() ?: 0L }
            if (latest != null) {
                logger.info("Resolved model in $candidate -> ${latest.toString()} (latest checkpoint_cycle_*.json)")
                return latest.toString()
            }
        }

        return null
    }

    private fun getAllArgs(args: Array<String>, flag: String): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            if (args[i] == flag && i + 1 < args.size) {
                list.add(args[i + 1])
                i += 2
            } else i++
        }
        return list
    }

    private fun parseHiddenLayers(value: String): List<Int> {
        val clean = value.trim()
        return when {
            clean.startsWith("[") && clean.endsWith("]") -> clean.removeSurrounding("[", "]").split(',').map { it.trim().toInt() }
            "," in clean -> clean.split(',').map { it.trim().toInt() }
            clean.contains(" ") -> clean.split("\\s+".toRegex()).map { it.trim().toInt() }
            else -> listOf(clean.toInt())
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
        println("    --profile <spec>         Profile(s): legacy name (fast-debug), or composed 'network:big,selfplay:long' or 'big,long'")
        println("    --config <name|path>     Root config (in ./config or path) that composes profiles + overrides")
        println("    --override k=v           Override any config key (repeatable; kebab or camelCase)")
        println("    --cycles <n>             Number of training cycles (default: from profile)")
        println("    --games <n>              Games per cycle (default: from profile)")
        println("    --seed <n>               Random seed for reproducibility")
        println("    --checkpoint-dir <dir>   Checkpoint directory")
        println("    --learning-rate <rate>   Learning rate override")
        println("    --batch-size <size>      Batch size override")
        println("    --hidden-layers <list>   Hidden layers (e.g., 512,256,128)")
        println("    --max-concurrent-games n Concurrency for self-play")
        println("    --exploration-rate <x>   Exploration epsilon")
        println("    --target-update-frequency n  Target net update frequency")
        println("    --max-experience-buffer n    Replay buffer size")
        println("    --max-batches-per-cycle n    Upper cap on training batches per cycle")
        println("    --replay-type <type>     Replay buffer: UNIFORM or PRIORITIZED")
        println("    --gamma <x>              Discount factor (0,1), default 0.99")
        println("    --double-dqn             Enable Double DQN target evaluation")
        println("    --train-early-adjudication <true|false>  Enable early adjudication in training")
        println("    --train-resign-threshold <n>            Material threshold (training)")
        println("    --train-no-progress-plies <n>           No-progress plies (training)")
        println("    --train-opponent <type>  Training opponent: self|minimax|heuristic")
        println("    --train-opponent-depth n Minimax depth during training (default: 2)")
        println("    --resume                 Resume training by auto-loading best_model.json from checkpoint dir")
        println("    --load <path>            Resume training from an explicit model file path")
        println("    --checkpoint-interval n  Save frequency (cycles)")
        println("    --checkpoint-max-versions n    Retain up to n checkpoints (manager)")
        println("    --checkpoint-validation <true|false>  Enable checkpoint validation")
        println("    --checkpoint-compression <true|false> Enable compressed artifacts")
        println("    --checkpoint-autocleanup <true|false> Enable auto cleanup policy")
        println("    --log-level <lvl>        debug|info|warn|error")
        println("    --log-interval n         Print detailed block every n cycles")
        println("    --summary-only           Only show summaries and final output")
        println("    --metrics-file <path>    Append per-cycle CSV metrics to path")
        println("    --win-reward <x>         Reward shaping (win)")
        println("    --loss-reward <x>        Reward shaping (loss)")
        println("    --draw-reward <x>        Reward shaping (draw)")
        println("    --step-limit-penalty <x> Penalty at step limit")
        println()
        println("EVALUATION:")
        println("  --evaluate --baseline [OPTIONS]")
        println("    --model <path|dir>       Model file or directory (auto-selects best inside dir; falls back to best in checkpoint dir if omitted)")
        println("    --games <n>              Number of evaluation games (default: 100)")
        println("    --opponent <type>        Opponent type: heuristic, minimax (default: heuristic)")
        println("    --depth <n>              Minimax depth for minimax opponent (default: 2)")
        println("    --seed <n>               Random seed for reproducibility")
        println("    --eval-epsilon <x>       Evaluation exploration (0.0 = greedy)")
        println("    --eval-early-adjudication <true|false>  Enable early adjudication (resign on large material deficit)")
        println("    --eval-resign-threshold <n>            Material threshold for resignation (default: 15)")
        println("    --eval-no-progress-plies <n>           No-progress plies before draw (default: 100)")
        println("    --profile <spec>         Optional: legacy or composed profiles for evaluation")
        println("    --config <name|path>     Optional: root config for evaluation")
        println("    --override k=v           Optional: overrides for evaluation config")
        println()
        println("  --evaluate --compare [OPTIONS]")
        println("    --modelA <path|dir>      First model file or directory (auto-selects best inside dir)")
        println("    --modelB <path|dir>      Second model file or directory (auto-selects best inside dir)")
        println("    --games <n>              Number of comparison games (default: 100)")
        println("    --seed <n>               Random seed for reproducibility")
        println()
        println("PLAY:")
        println("  --play [OPTIONS]")
        println("    --model <path>           Model file to play against")
        println("    --profile <spec>         Optional profile(s) for play settings")
        println("    --config <name|path>     Optional root config for play settings")
        println("    --override k=v           Optional overrides for play settings")
        println("    --as <color>             Human player color: white, black (default: white)")
        println("    --verbose                Show detailed engine analysis and diagnostics")
        println()
        println("PROFILES:")
        println("  Legacy: fast-debug, long-train, eval-only")
        println("  Composed examples (from ./config):")
        println("    --profile network:big,selfplay:long | --profile big,long")
        println("    --config long-term-learning  # loads config/long-term-learning.yaml")
        println()
        println("EXAMPLES:")
        println("  # Quick development training")
        println("  chess-rl --train --profile fast-debug --seed 12345")
        println()
        println("  # Production training with custom parameters")
        println("  chess-rl --train --profile long-train --cycles 100 --learning-rate 0.001")
        println()
        println("  # Composed profiles from domain configs")
        println("  chess-rl --train --profile network:big,selfplay:long --override checkpointDirectory=experiments/run1")
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
