package com.chessrl.integration.config

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
    /** Network architecture - impacts model capacity and training speed */
    val hiddenLayers: List<Int> = listOf(512, 256, 128),
    /** Learning rate for neural network training - critical for convergence */
    val learningRate: Double = 0.001,
    /** Batch size for training - affects memory usage and gradient stability */
    val batchSize: Int = 64,
    
    // RL Training Configuration (3 parameters)
    /** Exploration rate for epsilon-greedy action selection */
    val explorationRate: Double = 0.1,
    /** Frequency of target network updates in DQN algorithm */
    val targetUpdateFrequency: Int = 100,
    /** Maximum size of experience replay buffer */
    val maxExperienceBuffer: Int = 50000,
    
    // Self-Play Configuration (4 parameters)
    /** Number of self-play games per training cycle */
    val gamesPerCycle: Int = 20,
    /** Maximum number of concurrent self-play games */
    val maxConcurrentGames: Int = 4,
    /** Maximum steps per game before forced termination */
    val maxStepsPerGame: Int = 80,
    /** Maximum number of training cycles */
    val maxCycles: Int = 100,
    
    // Reward Structure (4 parameters)
    /** Reward for winning a game */
    val winReward: Double = 1.0,
    /** Reward for losing a game */
    val lossReward: Double = -1.0,
    /** Reward for drawing a game (negative to discourage draws) */
    val drawReward: Double = -0.2,
    /** Penalty when game reaches step limit (encourages decisive play) */
    val stepLimitPenalty: Double = -1.0,
    
    // System Configuration (4 parameters)
    /** Random seed for reproducible training (null for random) */
    val seed: Long? = null,
    /** Interval between checkpoint saves (in cycles) */
    val checkpointInterval: Int = 5,
    /** Directory for saving checkpoints */
    val checkpointDirectory: String = "checkpoints",
    /** Number of games for evaluation runs */
    val evaluationGames: Int = 100
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
        if (maxExperienceBuffer <= 0) {
            errors.add("Max experience buffer must be positive, got: $maxExperienceBuffer")
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
        return buildString {
            appendLine("Chess RL Configuration Summary:")
            appendLine("  Network: ${hiddenLayers.joinToString("x")} layers, lr=$learningRate, batch=$batchSize")
            appendLine("  RL: exploration=$explorationRate, target_update=$targetUpdateFrequency, buffer=$maxExperienceBuffer")
            appendLine("  Self-play: $gamesPerCycle games/cycle, $maxConcurrentGames concurrent, $maxStepsPerGame max_steps")
            appendLine("  Rewards: win=$winReward, loss=$lossReward, draw=$drawReward, step_penalty=$stepLimitPenalty")
            appendLine("  System: ${seed?.let { "seed=$it" } ?: "random"}, checkpoints every $checkpointInterval cycles")
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