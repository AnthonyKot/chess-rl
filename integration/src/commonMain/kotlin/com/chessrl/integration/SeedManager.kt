package com.chessrl.integration

import kotlin.random.Random
import kotlin.jvm.Volatile

/**
 * Centralized seed management system for deterministic training runs.
 * Manages seeding for all stochastic components: neural network initialization,
 * replay sampling, exploration strategies, and random data generation.
 */
class SeedManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: SeedManager? = null
        
        fun getInstance(): SeedManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SeedManager().also { INSTANCE = it }
            }
        }
        
        /**
         * Initialize with a specific seed for deterministic runs
         */
        fun initializeWithSeed(seed: Long): SeedManager {
            val instance = getInstance()
            instance.setSeed(seed)
            return instance
        }
        
        // Alias for older call sites
        fun initializeWithMasterSeed(seed: Long): SeedManager = initializeWithSeed(seed)

        // Convenience getters for static-style access
        fun getNeuralNetworkRandom() = getInstance().getNeuralNetworkRandom()
        fun getExplorationRandom() = getInstance().getExplorationRandom()
        fun getReplayBufferRandom() = getInstance().getReplayBufferRandom()
        
        /**
         * Initialize with random seed for non-deterministic runs
         */
        fun initializeWithRandomSeed(): SeedManager {
            val instance = getInstance()
            instance.setRandomSeed()
            return instance
        }
    }
    
    // Core seed state
    private var masterSeed: Long = 0L
    private var isSeeded: Boolean = false
    private var isDeterministicMode: Boolean = false
    
    // Component-specific random generators
    private var neuralNetworkRandom: Random = Random.Default
    private var explorationRandom: Random = Random.Default
    private var replayBufferRandom: Random = Random.Default
    private var dataGenerationRandom: Random = Random.Default
    private var generalRandom: Random = Random.Default
    
    // Seed tracking for components
    private val componentSeeds = mutableMapOf<String, Long>()
    private val seedHistory = mutableListOf<SeedEvent>()
    
    /**
     * Set the master seed and initialize all component random generators
     */
    fun setSeed(seed: Long) {
        masterSeed = seed
        isSeeded = true
        isDeterministicMode = true
        
        // Generate deterministic seeds for each component
        val seedGenerator = Random(seed)
        
        val nnSeed = seedGenerator.nextLong()
        val explorationSeed = seedGenerator.nextLong()
        val replaySeed = seedGenerator.nextLong()
        val dataSeed = seedGenerator.nextLong()
        val generalSeed = seedGenerator.nextLong()
        
        // Initialize component-specific random generators
        neuralNetworkRandom = Random(nnSeed)
        explorationRandom = Random(explorationSeed)
        replayBufferRandom = Random(replaySeed)
        dataGenerationRandom = Random(dataSeed)
        generalRandom = Random(generalSeed)
        
        // Track component seeds
        componentSeeds["neural_network"] = nnSeed
        componentSeeds["exploration"] = explorationSeed
        componentSeeds["replay_buffer"] = replaySeed
        componentSeeds["data_generation"] = dataSeed
        componentSeeds["general"] = generalSeed
        
        // Record seed event
        recordSeedEvent(SeedEventType.SEED_SET, seed, "Master seed set to $seed")
        
        println("🎲 Seed Manager: Master seed set to $seed (deterministic mode enabled)")
        println("   Neural Network seed: $nnSeed")
        println("   Exploration seed: $explorationSeed")
        println("   Replay Buffer seed: $replaySeed")
        println("   Data Generation seed: $dataSeed")
        println("   General seed: $generalSeed")
    }
    
    /**
     * Set a random seed (non-deterministic mode)
     */
    fun setRandomSeed() {
        val randomSeed = Random.nextLong()
        setSeed(randomSeed)
        isDeterministicMode = false
        
        recordSeedEvent(SeedEventType.RANDOM_SEED_SET, randomSeed, "Random seed generated: $randomSeed")
        println("🎲 Seed Manager: Random seed generated: $randomSeed (non-deterministic mode)")
    }
    
    /**
     * Get the current master seed
     */
    fun getMasterSeed(): Long {
        if (!isSeeded) {
            throw IllegalStateException("Seed manager not initialized. Call setSeed() or setRandomSeed() first.")
        }
        return masterSeed
    }
    
    /**
     * Check if running in deterministic mode
     */
    fun isDeterministic(): Boolean = isDeterministicMode
    
    /**
     * Get random generator for neural network operations
     */
    fun getNeuralNetworkRandom(): Random {
        ensureSeeded()
        return neuralNetworkRandom
    }
    
    /**
     * Get random generator for exploration strategies
     */
    fun getExplorationRandom(): Random {
        ensureSeeded()
        return explorationRandom
    }
    
    /**
     * Get random generator for replay buffer sampling
     */
    fun getReplayBufferRandom(): Random {
        ensureSeeded()
        return replayBufferRandom
    }
    
    /**
     * Get random generator for data generation
     */
    fun getDataGenerationRandom(): Random {
        ensureSeeded()
        return dataGenerationRandom
    }
    
    /**
     * Get general purpose random generator
     */
    fun getGeneralRandom(): Random {
        ensureSeeded()
        return generalRandom
    }
    
    /**
     * Get seed for a specific component
     */
    fun getComponentSeed(component: String): Long? {
        return componentSeeds[component]
    }
    
    /**
     * Get all component seeds
     */
    fun getAllComponentSeeds(): Map<String, Long> {
        return componentSeeds.toMap()
    }
    
    /**
     * Create a new seeded random generator for a custom component
     */
    fun createSeededRandom(componentName: String): Random {
        ensureSeeded()
        
        val componentSeed = generalRandom.nextLong()
        componentSeeds[componentName] = componentSeed
        
        recordSeedEvent(SeedEventType.COMPONENT_SEEDED, componentSeed, "Component '$componentName' seeded with $componentSeed")
        
        return Random(componentSeed)
    }
    
    /**
     * Get seed configuration for checkpointing
     */
    fun getSeedConfiguration(): SeedConfiguration {
        ensureSeeded()
        
        return SeedConfiguration(
            masterSeed = masterSeed,
            isDeterministicMode = isDeterministicMode,
            componentSeeds = componentSeeds.toMap(),
            seedHistory = seedHistory.toList()
        )
    }
    
    /**
     * Restore seed configuration from checkpoint
     */
    fun restoreSeedConfiguration(config: SeedConfiguration) {
        masterSeed = config.masterSeed
        isDeterministicMode = config.isDeterministicMode
        isSeeded = true
        
        // Restore component seeds and random generators
        componentSeeds.clear()
        componentSeeds.putAll(config.componentSeeds)
        
        // Recreate random generators from seeds
        neuralNetworkRandom = Random(componentSeeds["neural_network"] ?: Random.nextLong())
        explorationRandom = Random(componentSeeds["exploration"] ?: Random.nextLong())
        replayBufferRandom = Random(componentSeeds["replay_buffer"] ?: Random.nextLong())
        dataGenerationRandom = Random(componentSeeds["data_generation"] ?: Random.nextLong())
        generalRandom = Random(componentSeeds["general"] ?: Random.nextLong())
        
        // Restore seed history
        seedHistory.clear()
        seedHistory.addAll(config.seedHistory)
        
        recordSeedEvent(SeedEventType.CONFIGURATION_RESTORED, masterSeed, "Seed configuration restored from checkpoint")
        
        println("🎲 Seed Manager: Configuration restored from checkpoint")
        println("   Master seed: $masterSeed")
        println("   Deterministic mode: $isDeterministicMode")
        println("   Component seeds: ${componentSeeds.size}")
    }
    
    /**
     * Enable deterministic test mode for CI
     */
    fun enableDeterministicTestMode(testSeed: Long = 12345L) {
        setSeed(testSeed)
        isDeterministicMode = true
        
        recordSeedEvent(SeedEventType.TEST_MODE_ENABLED, testSeed, "Deterministic test mode enabled with seed $testSeed")
        
        println("🧪 Seed Manager: Deterministic test mode enabled (seed: $testSeed)")
    }
    
    /**
     * Get seed summary for logging and reports
     */
    fun getSeedSummary(): SeedSummary {
        return SeedSummary(
            isSeeded = isSeeded,
            masterSeed = if (isSeeded) masterSeed else null,
            isDeterministicMode = isDeterministicMode,
            componentCount = componentSeeds.size,
            seedEvents = seedHistory.size,
            lastSeedEvent = seedHistory.lastOrNull()
        )
    }
    
    /**
     * Validate seed consistency (for debugging)
     */
    fun validateSeedConsistency(): SeedValidationResult {
        val issues = mutableListOf<String>()
        
        if (!isSeeded) {
            issues.add("Seed manager not initialized")
        }
        
        if (componentSeeds.isEmpty()) {
            issues.add("No component seeds configured")
        }
        
        // Check for duplicate seeds (which could indicate issues)
        val seedValues = componentSeeds.values
        val uniqueSeeds = seedValues.toSet()
        if (seedValues.size != uniqueSeeds.size) {
            issues.add("Duplicate seeds detected in components")
        }
        
        // Validate that random generators are properly initialized
        try {
            neuralNetworkRandom.nextDouble()
            explorationRandom.nextDouble()
            replayBufferRandom.nextDouble()
            dataGenerationRandom.nextDouble()
            generalRandom.nextDouble()
        } catch (e: Exception) {
            issues.add("Random generator validation failed: ${e.message}")
        }
        
        return SeedValidationResult(
            isValid = issues.isEmpty(),
            issues = issues,
            componentSeeds = componentSeeds.toMap(),
            masterSeed = if (isSeeded) masterSeed else null
        )
    }
    
    // Private helper methods
    
    private fun ensureSeeded() {
        if (!isSeeded) {
            throw IllegalStateException("Seed manager not initialized. Call setSeed() or setRandomSeed() first.")
        }
    }
    
    private fun recordSeedEvent(type: SeedEventType, seed: Long, description: String) {
        val event = SeedEvent(
            type = type,
            seed = seed,
            timestamp = getCurrentTimeMillis(),
            description = description
        )
        seedHistory.add(event)
        
        // Keep only recent events to prevent memory issues
        if (seedHistory.size > 100) {
            seedHistory.removeAt(0)
        }
    }
}

/**
 * Seed configuration for checkpointing
 */
data class SeedConfiguration(
    val masterSeed: Long,
    val isDeterministicMode: Boolean,
    val componentSeeds: Map<String, Long>,
    val seedHistory: List<SeedEvent>
)

/**
 * Seed event for tracking seed operations
 */
data class SeedEvent(
    val type: SeedEventType,
    val seed: Long,
    val timestamp: Long,
    val description: String
)

/**
 * Types of seed events
 */
enum class SeedEventType {
    SEED_SET,
    RANDOM_SEED_SET,
    COMPONENT_SEEDED,
    CONFIGURATION_RESTORED,
    TEST_MODE_ENABLED
}

/**
 * Seed summary for reporting
 */
data class SeedSummary(
    val isSeeded: Boolean,
    val masterSeed: Long?,
    val isDeterministicMode: Boolean,
    val componentCount: Int,
    val seedEvents: Int,
    val lastSeedEvent: SeedEvent?
)

/**
 * Seed validation result
 */
data class SeedValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val componentSeeds: Map<String, Long>,
    val masterSeed: Long?
)

/**
 * Utility functions for seeded operations
 */
object SeededOperations {
    
    /**
     * Generate seeded Gaussian random number
     */
    fun nextGaussian(random: Random): Double = random.nextGaussian()
    
    /**
     * Shuffle array with seeded random
     */
    fun <T> shuffleArray(array: Array<T>, random: Random) {
        for (i in array.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = array[i]
            array[i] = array[j]
            array[j] = temp
        }
    }
    
    /**
     * Shuffle list with seeded random
     */
    fun <T> shuffleList(list: MutableList<T>, random: Random) {
        for (i in list.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = list[i]
            list[i] = list[j]
            list[j] = temp
        }
    }
    
    /**
     * Sample from array with seeded random
     */
    fun <T> sampleFromArray(array: Array<T>, count: Int, random: Random): List<T> {
        require(count <= array.size) { "Cannot sample $count items from array of size ${array.size}" }
        
        val indices = (0 until array.size).toMutableList()
        shuffleList(indices, random)
        
        return indices.take(count).map { array[it] }
    }
    
    /**
     * Generate seeded weights for neural network initialization
     */
    fun generateWeights(size: Int, initType: WeightInitType, random: Random): DoubleArray {
        return when (initType) {
            WeightInitType.XAVIER -> {
                val scale = kotlin.math.sqrt(1.0 / size)
                DoubleArray(size) { random.nextGaussian() * scale }
            }
            WeightInitType.HE -> {
                val scale = kotlin.math.sqrt(2.0 / size)
                DoubleArray(size) { random.nextGaussian() * scale }
            }
            WeightInitType.UNIFORM -> {
                val bound = kotlin.math.sqrt(6.0 / size)
                DoubleArray(size) { random.nextDouble(-bound, bound) }
            }
            WeightInitType.ZERO -> {
                DoubleArray(size) { 0.0 }
            }
        }
    }
}

/**
 * Weight initialization types
 */
enum class WeightInitType {
    XAVIER,
    HE,
    UNIFORM,
    ZERO
}
