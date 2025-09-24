package com.chessrl.integration.config

import com.chessrl.integration.adapter.EngineBackend

/**
 * Central configuration class for Chess RL training system.
 * 
 * This replaces the scattered configuration parameters from profiles.yaml and TrainingConfiguration.kt
 * with a single, focused set of essential parameters that directly impact training effectiveness.
 * 
 * Based on CONFIGURATION_AUDIT.md analysis, this includes only the 18 essential parameters
 * that have proven impact on training competitive chess agents.
 */
data class ChessRLConfig(
    // Neural Network Configuration (3 parameters)
    /** 
     * Network architecture - impacts model capacity and training speed.
     * Larger networks learn more complex patterns but train slower.
     * Recommended: [256,128] for fast training, [512,256,128] for balanced, [768,512,256] for maximum capacity.
     */
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    
    /** 
     * Learning rate for neural network training - critical for convergence.
     * Higher values learn faster but may be unstable. Lower values are more stable but slower.
     * Recommended: 0.0005 for balanced, 0.0003 for stable, 0.001 for aggressive.
     */
    val learningRate: Double = 0.0005,
    
    /** 
     * Batch size for training - affects memory usage and gradient stability.
     * Larger batches provide more stable gradients but use more memory.
     * Recommended: 32-128 depending on available memory.
     */
    val batchSize: Int = 64,
    
    // RL Training Configuration (6 parameters)
    /** 
     * Exploration rate for epsilon-greedy action selection.
     * Higher values explore more but may play suboptimally. Lower values exploit learned knowledge.
     * Recommended: 0.05 for balanced, 0.02 for exploitation, 0.1 for exploration.
     */
    val explorationRate: Double = 0.05,
    
    /** 
     * Frequency of target network updates in DQN algorithm.
     * Lower values update targets more frequently (less stable). Higher values are more stable but slower.
     * Recommended: 200 for balanced, 100 for fast learning, 500 for stability.
     */
    val targetUpdateFrequency: Int = 200,
    
    /** 
     * Enable Double DQN target evaluation to reduce overestimation bias.
     * Generally improves training stability, especially with larger networks.
     */
    val doubleDqn: Boolean = true,
    
    /** 
     * Discount factor for future rewards - how much to value future vs immediate rewards.
     * Higher values consider long-term strategy more. Lower values focus on immediate gains.
     * Recommended: 0.99 for chess (long-term strategy important).
     */
    val gamma: Double = 0.99,
    
    /** 
     * Maximum size of experience replay buffer.
     * Larger buffers provide more diverse training data but use more memory.
     * Recommended: 50K for balanced, 100K+ for large-scale training.
     */
    val maxExperienceBuffer: Int = 50000,
    
    /** 
     * Replay buffer type: UNIFORM (random sampling) or PRIORITIZED (importance sampling).
     * PRIORITIZED can improve learning efficiency but adds computational overhead.
     */
    val replayType: String = "UNIFORM",
    
    // Self-Play Configuration (4 parameters)
    /** 
     * Number of self-play games per training cycle.
     * More games provide more training data but take longer per cycle.
     * Recommended: 30 for development, 50+ for production training.
     */
    val gamesPerCycle: Int = 30,
    
    /** 
     * Maximum number of concurrent self-play games.
     * Higher values speed up training but use more CPU/memory. Limited by available cores.
     * Recommended: 2-4 for development, 4-8 for production (based on CPU cores).
     */
    val maxConcurrentGames: Int = 4,
    
    /** 
     * Maximum steps per game before forced termination.
     * Prevents infinite games but may cut off legitimate long games.
     * Recommended: 100-150 moves (50-75 moves per side) for better learning.
     */
    val maxStepsPerGame: Int = 120,
    
    /** 
     * Maximum number of training cycles.
     * More cycles allow more learning but take longer overall.
     * Recommended: 10-50 for development, 100-500 for production.
     */
    val maxCycles: Int = 100,
    
    // Reward Structure (4 parameters)
    /** Reward for winning a game */
    val winReward: Double = 1.0,
    /** Reward for losing a game */
    val lossReward: Double = -1.0,
    /** Reward for drawing a game (neutral to allow natural play) */
    val drawReward: Double = 0.0,
    /** Penalty when game reaches step limit (encourages decisive play) */
    val stepLimitPenalty: Double = -0.5,
    
    // Engine selection
    /** Chess engine backend to use for environments */
    val engine: EngineBackend = EngineBackend.CHESSLIB,

    // System Configuration (4 parameters)
    /** Random seed for reproducible training (null for random) */
    val seed: Long? = null,
    /** Interval between checkpoint saves (in cycles) */
    val checkpointInterval: Int = 5,
    /** Directory for saving checkpoints */
    val checkpointDirectory: String = "checkpoints",
    /** Number of games for evaluation runs */
    val evaluationGames: Int = 100,
    
    // Training opponent controls (optional)
    /** Opponent policy during training: null/self, "minimax", or "heuristic" */
    val trainOpponentType: String? = null,
    /** Depth for minimax opponent during training */
    val trainOpponentDepth: Int = 2,
    
    // Training environment controls (optional)
    /** Enable early adjudication during training to reduce long/stalled games */
    val trainEarlyAdjudication: Boolean = false,
    /** Material threshold to auto-resign in training when early adjudication is enabled */
    val trainResignMaterialThreshold: Int = 15,
    /** No-progress plies threshold for training draw (50-move rule is 100 plies) */
    val trainNoProgressPlies: Int = 100,
    
    // Evaluation environment controls (optional)
    /** Enable early adjudication during evaluation to reduce drawn-out games */
    val evalEarlyAdjudication: Boolean = false,
    /** Material threshold to auto-resign in evaluation when early adjudication is enabled */
    val evalResignMaterialThreshold: Int = 15,
    /** No-progress plies threshold for evaluation draw (50-move rule is 100 plies) */
    val evalNoProgressPlies: Int = 100,

    // Checkpoint Manager controls
    val checkpointMaxVersions: Int = 20,
    val checkpointValidationEnabled: Boolean = false,
    val checkpointCompressionEnabled: Boolean = true,
    val checkpointAutoCleanupEnabled: Boolean = true,

    // Training controls
    val maxBatchesPerCycle: Int = 100,
    
    // Logging controls (lightweight)
    val logInterval: Int = 1,
    val summaryOnly: Boolean = false,
    val metricsFile: String? = null
) {
    
    /**
     * Validate configuration parameters and return validation result.
     * 
     * @return ConfigurationValidationResult with errors and warnings
     */
    fun validate(): ConfigurationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Neural Network validation
        if (hiddenLayers.isEmpty()) {
            errors.add("Hidden layers cannot be empty")
        }
        if (hiddenLayers.any { it <= 0 }) {
            errors.add("All hidden layer sizes must be positive")
        }
        if (learningRate <= 0.0 || learningRate > 1.0) {
            errors.add("Learning rate must be between 0 and 1, got: $learningRate")
        }
        if (batchSize <= 0) {
            errors.add("Batch size must be positive, got: $batchSize")
        }
        
        // RL Training validation
        if (explorationRate < 0.0 || explorationRate > 1.0) {
            errors.add("Exploration rate must be between 0 and 1, got: $explorationRate")
        }
        if (targetUpdateFrequency <= 0) {
            errors.add("Target update frequency must be positive, got: $targetUpdateFrequency")
        }
        if (gamma <= 0.0 || gamma >= 1.0) {
            errors.add("Gamma must be in (0,1), got: $gamma")
        }
        if (maxBatchesPerCycle <= 0) {
            errors.add("Max batches per cycle must be positive, got: $maxBatchesPerCycle")
        }
        if (logInterval <= 0) {
            errors.add("Log interval must be positive, got: $logInterval")
        }
        if (maxExperienceBuffer <= 0) {
            errors.add("Max experience buffer must be positive, got: $maxExperienceBuffer")
        }
        val replayUpper = replayType.uppercase()
        if (replayUpper != "UNIFORM" && replayUpper != "PRIORITIZED") {
            errors.add("Replay type must be UNIFORM or PRIORITIZED, got: $replayType")
        }
        
        // Self-Play validation
        if (gamesPerCycle <= 0) {
            errors.add("Games per cycle must be positive, got: $gamesPerCycle")
        }
        if (maxConcurrentGames <= 0) {
            errors.add("Max concurrent games must be positive, got: $maxConcurrentGames")
        }
        if (maxStepsPerGame <= 0) {
            errors.add("Max steps per game must be positive, got: $maxStepsPerGame")
        }
        if (maxCycles <= 0) {
            errors.add("Max cycles must be positive, got: $maxCycles")
        }
        
        // Reward validation
        if (winReward <= lossReward) {
            warnings.add("Win reward ($winReward) should be greater than loss reward ($lossReward)")
        }
        if (drawReward > winReward) {
            warnings.add("Draw reward ($drawReward) should not be greater than win reward ($winReward)")
        }
        if (stepLimitPenalty > 0.0) {
            warnings.add("Step limit penalty ($stepLimitPenalty) should be negative or zero")
        }
        
        // System validation
        if (checkpointInterval <= 0) {
            errors.add("Checkpoint interval must be positive, got: $checkpointInterval")
        }
        if (evaluationGames <= 0) {
            errors.add("Evaluation games must be positive, got: $evaluationGames")
        }
        
        // Performance warnings
        if (maxConcurrentGames > 8) {
            warnings.add("High concurrent games count ($maxConcurrentGames) may impact performance")
        }
        if (maxConcurrentGames > Runtime.getRuntime().availableProcessors()) {
            warnings.add("Concurrent games ($maxConcurrentGames) exceeds available CPU cores (${Runtime.getRuntime().availableProcessors()})")
        }
        if (hiddenLayers.any { it > 1024 }) {
            warnings.add("Large hidden layers (${hiddenLayers.filter { it > 1024 }}) may require significant memory")
        }
        if (batchSize > maxExperienceBuffer) {
            warnings.add("Batch size ($batchSize) is larger than experience buffer ($maxExperienceBuffer)")
        }
        if (maxStepsPerGame > 200) {
            warnings.add("Very long games ($maxStepsPerGame steps) may slow training")
        }
        
        return ConfigurationValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Create a copy with a specific seed for deterministic training.
     */
    fun withSeed(newSeed: Long): ChessRLConfig {
        return copy(seed = newSeed)
    }
    
    /**
     * Create a copy optimized for fast debugging (fewer games, cycles).
     */
    fun forFastDebug(): ChessRLConfig {
        return copy(
            gamesPerCycle = 5,
            maxCycles = 10,
            maxConcurrentGames = 2,
            maxStepsPerGame = 40,
            evaluationGames = 20
        )
    }
    
    /**
     * Create a copy optimized for long training runs.
     */
    fun forLongTraining(): ChessRLConfig {
        return copy(
            gamesPerCycle = 50,
            maxCycles = 200,
            maxConcurrentGames = 8,
            hiddenLayers = listOf(768, 512, 256),
            maxStepsPerGame = 120
        )
    }
    
    /**
     * Create a copy optimized for evaluation only.
     */
    fun forEvaluationOnly(): ChessRLConfig {
        return copy(
            evaluationGames = 500,
            maxConcurrentGames = 1,
            seed = 12345L // Deterministic for consistent evaluation
        )
    }
    
    /**
     * Get a summary of key configuration parameters for logging.
     */
    fun getSummary(): String {
        val seedLabel = seed?.let { "seed=$it" } ?: "random"
        return buildString {
            appendLine("Chess RL Configuration Summary:")
            appendLine("  Engine: ${engine.name.lowercase()}")
            appendLine("  Network: ${hiddenLayers.joinToString("x")} layers, lr=$learningRate, batch=$batchSize")
            appendLine("  RL: exploration=$explorationRate, target_update=$targetUpdateFrequency, doubleDQN=$doubleDqn, gamma=$gamma, buffer=$maxExperienceBuffer")
            appendLine("      replay=$replayType")
            appendLine("  Self-play: $gamesPerCycle games/cycle, $maxConcurrentGames concurrent, $maxStepsPerGame max_steps")
            appendLine("  Rewards: win=$winReward, loss=$lossReward, draw=$drawReward, step_limit_penalty=$stepLimitPenalty")
            appendLine("  System: $seedLabel, checkpoints every $checkpointInterval cycles")
            appendLine("  Training: max_batches_per_cycle=$maxBatchesPerCycle, log_interval=$logInterval")
            appendLine("  TrainOpponent: ${trainOpponentType ?: "self"}${if (trainOpponentType?.equals("minimax", true) == true) "(d=$trainOpponentDepth)" else ""}")
            appendLine("  TrainEnv: early_adjudication=$trainEarlyAdjudication, resign_threshold=$trainResignMaterialThreshold, no_progress_plies=$trainNoProgressPlies")
            appendLine("  Eval: early_adjudication=$evalEarlyAdjudication, resign_threshold=$evalResignMaterialThreshold, no_progress_plies=$evalNoProgressPlies")
        }
    }
}

/**
 * Result of configuration validation.
 */
data class ConfigurationValidationResult(
    /** True if configuration is valid (no errors) */
    val isValid: Boolean,
    /** List of validation errors that prevent training */
    val errors: List<String>,
    /** List of validation warnings about potentially suboptimal settings */
    val warnings: List<String>
) {
    
    /**
     * Print validation results to console.
     */
    fun printResults() {
        if (errors.isNotEmpty()) {
            println("Configuration Errors:")
            errors.forEach { println("  ERROR: $it") }
        }
        
        if (warnings.isNotEmpty()) {
            println("Configuration Warnings:")
            warnings.forEach { println("  WARNING: $it") }
        }
        
        if (isValid && warnings.isEmpty()) {
            println("Configuration is valid with no warnings.")
        }
    }
    
    /**
     * Throw exception if configuration is invalid.
     */
    fun throwIfInvalid() {
        if (!isValid) {
            throw IllegalArgumentException("Invalid configuration: ${errors.joinToString(", ")}")
        }
    }
}
