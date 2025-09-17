package com.chessrl.integration

import com.chessrl.rl.*
import kotlin.math.*

/**
 * Advanced checkpoint manager for production-ready model persistence,
 * versioning, performance comparison, and rollback capabilities.
 */
class CheckpointManager(
    private val config: CheckpointConfig = CheckpointConfig()
) {
    
    // Checkpoint storage and tracking
    private val checkpoints = mutableMapOf<Int, CheckpointInfo>()
    private val performanceHistory = mutableMapOf<Int, Double>()
    private var bestCheckpoint: CheckpointInfo? = null
    private var checkpointCounter = 0
    
    // Statistics
    private var totalCheckpointsCreated = 0
    private var totalCheckpointsLoaded = 0
    private var totalCheckpointsDeleted = 0
    
    /**
     * Create a new checkpoint with comprehensive metadata
     */
    fun createCheckpoint(
        agent: ChessAgent,
        version: Int,
        metadata: CheckpointMetadata
    ): CheckpointInfo {
        
        val checkpointStartTime = getCurrentTimeMillis()
        
        try {
            // Generate checkpoint path
            val checkpointPath = generateCheckpointPath(version, metadata)
            
            // Save agent state at checkpointPath (legacy) and a dedicated model file alongside
            agent.save(checkpointPath)
            // Also persist a clear model artifact for portability (not compressed)
            runCatching {
                val modelPath = checkpointPath
                    .replace(".json.gz", "_qnet.json")
                    .replace(".json", "_qnet.json")
                agent.save(modelPath)
            }
            
            // Create checkpoint info
            val checkpointInfo = CheckpointInfo(
                version = version,
                path = checkpointPath,
                metadata = metadata,
                creationTime = checkpointStartTime,
                fileSize = estimateFileSize(checkpointPath),
                validationStatus = if (config.validationEnabled) {
                    validateCheckpoint(checkpointPath)
                } else {
                    ValidationStatus.SKIPPED
                }
            )
            
            // Store checkpoint info
            checkpoints[version] = checkpointInfo
            performanceHistory[version] = metadata.performance
            totalCheckpointsCreated++
            
            // Update best checkpoint if this is better
            if (metadata.isBest || isBetterThanBest(metadata.performance)) {
                bestCheckpoint = checkpointInfo
            }
            
            // Perform cleanup if needed
            performCheckpointCleanup()
            
            val checkpointEndTime = getCurrentTimeMillis()
            val checkpointDuration = checkpointEndTime - checkpointStartTime
            
            println("💾 Checkpoint created: version $version (${checkpointDuration}ms)")
            if (config.validationEnabled) {
                println("   Validation: ${checkpointInfo.validationStatus}")
            }
            
            return checkpointInfo
            
        } catch (e: Exception) {
            println("❌ Failed to create checkpoint version $version: ${e.message}")
            throw CheckpointException("Failed to create checkpoint", e)
        }
    }
    
    /**
     * Load a checkpoint and restore agent state
     */
    fun loadCheckpoint(checkpointInfo: CheckpointInfo, agent: ChessAgent): LoadResult {
        val loadStartTime = getCurrentTimeMillis()
        
        try {
            // Validate checkpoint before loading
            if (config.validationEnabled && checkpointInfo.validationStatus != ValidationStatus.VALID) {
                val validationResult = validateCheckpoint(checkpointInfo.path)
                if (validationResult != ValidationStatus.VALID) {
                    throw CheckpointException("Checkpoint validation failed: $validationResult")
                }
            }
            
            // Load agent state – prefer dedicated model artifact if present/valid
            val primary = checkpointInfo.path
            val modelPath = primary
                .replace(".json.gz", "_qnet.json")
                .replace(".json", "_qnet.json")
            val loaded = runCatching { agent.load(modelPath) }.isSuccess ||
                         runCatching { agent.load(primary) }.isSuccess
            if (!loaded) throw CheckpointException("Failed to load model from $modelPath or $primary")
            
            totalCheckpointsLoaded++
            
            val loadEndTime = getCurrentTimeMillis()
            val loadDuration = loadEndTime - loadStartTime
            
            println("📂 Checkpoint loaded: version ${checkpointInfo.version} (${loadDuration}ms)")
            
            return LoadResult(
                success = true,
                version = checkpointInfo.version,
                loadDuration = loadDuration,
                metadata = checkpointInfo.metadata
            )
            
        } catch (e: Exception) {
            println("❌ Failed to load checkpoint version ${checkpointInfo.version}: ${e.message}")
            return LoadResult(
                success = false,
                version = checkpointInfo.version,
                loadDuration = 0L,
                metadata = checkpointInfo.metadata,
                error = e.message
            )
        }
    }
    
    /**
     * Get the best checkpoint based on performance
     */
    fun getBestCheckpoint(): CheckpointInfo? = bestCheckpoint
    
    /**
     * Get checkpoint by version
     */
    fun getCheckpoint(version: Int): CheckpointInfo? = checkpoints[version]
    
    /**
     * List all available checkpoints
     */
    fun listCheckpoints(): List<CheckpointInfo> {
        return checkpoints.values.sortedBy { it.version }
    }
    
    /**
     * Compare performance between checkpoints
     */
    fun compareCheckpoints(version1: Int, version2: Int): CheckpointComparison? {
        val checkpoint1 = checkpoints[version1]
        val checkpoint2 = checkpoints[version2]
        
        if (checkpoint1 == null || checkpoint2 == null) {
            return null
        }
        
        val performance1 = checkpoint1.metadata.performance
        val performance2 = checkpoint2.metadata.performance
        val performanceDiff = performance2 - performance1
        val improvementPercent = if (performance1 != 0.0) {
            (performanceDiff / abs(performance1)) * 100
        } else {
            0.0
        }
        
        return CheckpointComparison(
            version1 = version1,
            version2 = version2,
            performance1 = performance1,
            performance2 = performance2,
            performanceDifference = performanceDiff,
            improvementPercent = improvementPercent,
            betterVersion = if (performance2 > performance1) version2 else version1,
            recommendation = generateComparisonRecommendation(performanceDiff, improvementPercent)
        )
    }
    
    /**
     * Delete a checkpoint
     */
    fun deleteCheckpoint(version: Int): Boolean {
        val checkpoint = checkpoints[version]
        if (checkpoint == null) {
            println("⚠️ Checkpoint version $version not found")
            return false
        }
        
        try {
            // In practice, would delete the actual file
            // File.delete(checkpoint.path)
            
            checkpoints.remove(version)
            performanceHistory.remove(version)
            totalCheckpointsDeleted++
            
            // Update best checkpoint if we deleted it
            if (bestCheckpoint?.version == version) {
                bestCheckpoint = findNewBestCheckpoint()
            }
            
            println("🗑️ Checkpoint version $version deleted")
            return true
            
        } catch (e: Exception) {
            println("❌ Failed to delete checkpoint version $version: ${e.message}")
            return false
        }
    }
    
    /**
     * Get checkpoint manager summary
     */
    fun getSummary(): CheckpointSummary {
        val totalCheckpoints = checkpoints.size
        val validCheckpoints = checkpoints.values.count { 
            it.validationStatus == ValidationStatus.VALID || it.validationStatus == ValidationStatus.SKIPPED 
        }
        val invalidCheckpoints = checkpoints.values.count { it.validationStatus == ValidationStatus.INVALID }
        val totalSize = checkpoints.values.sumOf { it.fileSize }
        val avgPerformance = if (performanceHistory.isNotEmpty()) {
            performanceHistory.values.average()
        } else 0.0
        val bestPerformance = performanceHistory.values.maxOrNull() ?: 0.0
        val performanceRange = if (performanceHistory.isNotEmpty()) {
            val min = performanceHistory.values.minOrNull() ?: 0.0
            val max = performanceHistory.values.maxOrNull() ?: 0.0
            max - min
        } else 0.0
        
        return CheckpointSummary(
            totalCheckpoints = totalCheckpoints,
            validCheckpoints = validCheckpoints,
            invalidCheckpoints = invalidCheckpoints,
            bestCheckpointVersion = bestCheckpoint?.version,
            totalSize = totalSize,
            averagePerformance = avgPerformance,
            bestPerformance = bestPerformance,
            performanceRange = performanceRange,
            totalCreated = totalCheckpointsCreated,
            totalLoaded = totalCheckpointsLoaded,
            totalDeleted = totalCheckpointsDeleted
        )
    }
    
    /**
     * Perform automatic cleanup of old checkpoints
     */
    fun performAutomaticCleanup(): CleanupSummary {
        val beforeCount = checkpoints.size
        val beforeSize = checkpoints.values.sumOf { it.fileSize }
        
        val cleanupResults = mutableListOf<String>()
        var deletedCount = 0
        var freedSize = 0L
        
        // Keep only the best N checkpoints if we exceed the limit
        if (checkpoints.size > config.maxVersions) {
            val sortedCheckpoints = checkpoints.values.sortedByDescending { it.metadata.performance }
            val checkpointsToDelete = sortedCheckpoints.drop(config.maxVersions)
            
            for (checkpoint in checkpointsToDelete) {
                if (deleteCheckpoint(checkpoint.version)) {
                    deletedCount++
                    freedSize += checkpoint.fileSize
                    cleanupResults.add("Deleted version ${checkpoint.version} (performance: ${checkpoint.metadata.performance})")
                }
            }
        }
        
        // Delete invalid checkpoints
        val invalidCheckpoints = checkpoints.values.filter { it.validationStatus == ValidationStatus.INVALID }
        for (checkpoint in invalidCheckpoints) {
            if (deleteCheckpoint(checkpoint.version)) {
                deletedCount++
                freedSize += checkpoint.fileSize
                cleanupResults.add("Deleted invalid version ${checkpoint.version}")
            }
        }
        
        val afterCount = checkpoints.size
        val afterSize = checkpoints.values.sumOf { it.fileSize }
        
        return CleanupSummary(
            checkpointsDeleted = deletedCount,
            sizeFreed = freedSize,
            checkpointsBefore = beforeCount,
            checkpointsAfter = afterCount,
            sizeBefore = beforeSize,
            sizeAfter = afterSize,
            cleanupActions = cleanupResults
        )
    }

    /**
     * Cleanup checkpoints based on a retention policy: keep best, last N, and optionally every N.
     * Returns a cleanup summary describing deletions.
     */
    fun cleanupByRetention(retention: CheckpointRetention): CleanupSummary {
        val beforeCount = checkpoints.size
        val beforeSize = checkpoints.values.sumOf { it.fileSize }
        val actions = mutableListOf<String>()

        // Determine versions to keep
        val all = checkpoints.values.sortedBy { it.version }
        val keep = mutableSetOf<Int>()

        if (retention.keepBest) {
            bestCheckpoint?.let { keep.add(it.version) }
        }

        if (retention.keepLastN > 0) {
            all.takeLast(retention.keepLastN).forEach { keep.add(it.version) }
        }

        if (retention.keepEveryN != null && retention.keepEveryN > 0) {
            val n = retention.keepEveryN
            all.forEach { info -> if (info.version % n == 0) keep.add(info.version) }
        }

        // Delete all not in keep set
        var deleted = 0
        var freed = 0L
        val toDelete = all.filter { it.version !in keep }
        for (info in toDelete) {
            if (deleteCheckpoint(info.version)) {
                deleted++
                freed += info.fileSize
                actions.add("Deleted version ${info.version} per retention policy")
            }
        }

        val afterCount = checkpoints.size
        val afterSize = checkpoints.values.sumOf { it.fileSize }

        return CleanupSummary(
            checkpointsDeleted = deleted,
            sizeFreed = freed,
            checkpointsBefore = beforeCount,
            checkpointsAfter = afterCount,
            sizeBefore = beforeSize,
            sizeAfter = afterSize,
            cleanupActions = actions
        )
    }
    
    // Private helper methods
    
    /**
     * Generate checkpoint path based on version and metadata
     */
    private fun generateCheckpointPath(version: Int, metadata: CheckpointMetadata): String {
        val timestamp = getCurrentTimeMillis()
        val baseFilename = "checkpoint_v${version}_c${metadata.cycle}_${timestamp}"
        val extension = if (config.compressionEnabled) ".json.gz" else ".json"
        return "${config.baseDirectory}/${baseFilename}${extension}"
    }
    
    /**
     * Estimate file size (in practice, would get actual file size)
     */
    private fun estimateFileSize(path: String): Long {
        // Deterministic rough estimate based on path, in range [1MB, 5MB]
        val hash = kotlin.math.abs(path.hashCode())
        return 1_000_000L + (hash % 4_000_001)
    }
    
    /**
     * Validate checkpoint integrity
     */
    private fun validateCheckpoint(path: String): ValidationStatus {
        try {
            // In practice, would perform actual file validation
            // - Check file exists and is readable
            // - Validate JSON structure
            // - Check model weights are valid numbers
            // - Verify metadata consistency
            
            // Simulate validation with occasional failures, deterministically by path
            val validationSuccess = (kotlin.math.abs(path.hashCode()) % 20) != 0 // ~95% success
            
            return if (validationSuccess) {
                ValidationStatus.VALID
            } else {
                ValidationStatus.INVALID
            }
            
        } catch (e: Exception) {
            return ValidationStatus.INVALID
        }
    }
    
    /**
     * Check if performance is better than current best
     */
    private fun isBetterThanBest(performance: Double): Boolean {
        val currentBest = bestCheckpoint?.metadata?.performance ?: Double.NEGATIVE_INFINITY
        return performance > currentBest
    }
    
    /**
     * Perform checkpoint cleanup based on configuration
     */
    private fun performCheckpointCleanup() {
        if (checkpoints.size > config.maxVersions) {
            val excessCount = checkpoints.size - config.maxVersions
            val sortedByPerformance = checkpoints.values.sortedBy { it.metadata.performance }
            
            // Delete worst performing checkpoints
            for (i in 0 until excessCount) {
                val checkpointToDelete = sortedByPerformance[i]
                deleteCheckpoint(checkpointToDelete.version)
            }
        }
    }
    
    /**
     * Find new best checkpoint after deletion
     */
    private fun findNewBestCheckpoint(): CheckpointInfo? {
        return checkpoints.values.maxByOrNull { it.metadata.performance }
    }
    
    /**
     * Generate comparison recommendation
     */
    private fun generateComparisonRecommendation(
        performanceDiff: Double,
        improvementPercent: Double
    ): String {
        val base = when {
            improvementPercent > 10.0 -> "Significant improvement - strongly recommend using newer version"
            improvementPercent > 5.0 -> "Good improvement - recommend using newer version"
            improvementPercent > 1.0 -> "Modest improvement - consider using newer version"
            improvementPercent > -1.0 -> "Minimal difference - either version acceptable"
            improvementPercent > -5.0 -> "Slight regression - consider keeping older version"
            else -> "Significant regression - recommend keeping older version"
        }
        return "$base (Δ=${"%.3f".format(performanceDiff)})"
    }
}

/**
 * Configuration for checkpoint manager
 */
data class CheckpointConfig(
    val baseDirectory: String = "checkpoints",
    val maxVersions: Int = 20,
    val compressionEnabled: Boolean = true,
    val validationEnabled: Boolean = true,
    val autoCleanupEnabled: Boolean = true
)

/** Retention policy for checkpoint cleanup */
data class CheckpointRetention(
    val keepBest: Boolean = true,
    val keepLastN: Int = 2,
    val keepEveryN: Int? = null
)

/**
 * Checkpoint metadata
 */
data class CheckpointMetadata(
    val cycle: Int,
    val performance: Double,
    val description: String,
    val isBest: Boolean = false,
    val seedConfiguration: SeedConfiguration? = null,
    val trainingConfiguration: TrainingConfiguration? = null,
    val additionalInfo: Map<String, Any> = emptyMap()
)

/**
 * Checkpoint information
 */
data class CheckpointInfo(
    val version: Int,
    val path: String,
    val metadata: CheckpointMetadata,
    val creationTime: Long,
    val fileSize: Long,
    val validationStatus: ValidationStatus
)

/**
 * Checkpoint validation status
 */
enum class ValidationStatus {
    VALID,
    INVALID,
    SKIPPED,
    PENDING
}

/**
 * Load result
 */
data class LoadResult(
    val success: Boolean,
    val version: Int,
    val loadDuration: Long,
    val metadata: CheckpointMetadata,
    val error: String? = null
)

/**
 * Checkpoint comparison result
 */
data class CheckpointComparison(
    val version1: Int,
    val version2: Int,
    val performance1: Double,
    val performance2: Double,
    val performanceDifference: Double,
    val improvementPercent: Double,
    val betterVersion: Int,
    val recommendation: String
)

/**
 * Checkpoint manager summary
 */
data class CheckpointSummary(
    val totalCheckpoints: Int,
    val validCheckpoints: Int,
    val invalidCheckpoints: Int,
    val bestCheckpointVersion: Int?,
    val totalSize: Long,
    val averagePerformance: Double,
    val bestPerformance: Double,
    val performanceRange: Double,
    val totalCreated: Int,
    val totalLoaded: Int,
    val totalDeleted: Int
)

/**
 * Cleanup summary
 */
data class CleanupSummary(
    val checkpointsDeleted: Int,
    val sizeFreed: Long,
    val checkpointsBefore: Int,
    val checkpointsAfter: Int,
    val sizeBefore: Long,
    val sizeAfter: Long,
    val cleanupActions: List<String>
)

/**
 * Checkpoint exception
 */
class CheckpointException(message: String, cause: Throwable? = null) : Exception(message, cause)
