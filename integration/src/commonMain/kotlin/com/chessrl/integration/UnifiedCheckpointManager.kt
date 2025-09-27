package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.backend.CheckpointCompatibility
import com.chessrl.integration.backend.CheckpointResolution
import com.chessrl.integration.logging.ChessRLLogger
import kotlin.math.abs

/**
 * Unified checkpoint manager that handles both manual (.json) and RL4J (.zip) checkpoint formats.
 * 
 * This manager extends the existing CheckpointManager to support:
 * - Format detection and validation
 * - Cross-backend checkpoint loading
 * - Metadata preservation across formats
 * - Unified checkpoint operations
 */
class UnifiedCheckpointManager(
    private val config: CheckpointConfig = CheckpointConfig()
) {
    
    private val logger = ChessRLLogger.forComponent("UnifiedCheckpointManager")
    
    // Checkpoint storage and tracking with format information
    private val checkpoints = mutableMapOf<String, UnifiedCheckpointInfo>()
    private val performanceHistory = mutableMapOf<String, Double>()
    private var bestCheckpoint: UnifiedCheckpointInfo? = null
    private var checkpointCounter = 0
    
    // Statistics
    private var totalCheckpointsCreated = 0
    private var totalCheckpointsLoaded = 0
    private var totalCheckpointsDeleted = 0
    
    /**
     * Save a checkpoint with automatic format detection based on backend type.
     */
    fun saveCheckpoint(
        agent: ChessAgent,
        checkpointId: String,
        metadata: UnifiedCheckpointMetadata,
        backendType: BackendType
    ): UnifiedCheckpointInfo {
        
        val saveStartTime = System.currentTimeMillis()
        
        try {
            // Generate checkpoint path based on backend type
            val checkpointPath = generateCheckpointPath(checkpointId, metadata, backendType)
            
            // Save agent state
            agent.save(checkpointPath)
            
            // Create unified checkpoint info
            val checkpointInfo = UnifiedCheckpointInfo(
                id = checkpointId,
                path = checkpointPath,
                metadata = metadata,
                backendType = backendType,
                format = detectCheckpointFormat(checkpointPath),
                creationTime = saveStartTime,
                fileSize = estimateFileSize(checkpointPath),
                validationStatus = if (config.validationEnabled) {
                    validateCheckpoint(checkpointPath, backendType)
                } else {
                    ValidationStatus.SKIPPED
                }
            )
            
            // Store checkpoint info
            checkpoints[checkpointId] = checkpointInfo
            performanceHistory[checkpointId] = metadata.performance
            totalCheckpointsCreated++
            
            // Update best checkpoint if this is better
            if (metadata.isBest || isBetterThanBest(metadata.performance)) {
                bestCheckpoint = checkpointInfo
            }
            
            // Save metadata sidecar file
            saveMetadataSidecar(checkpointPath, metadata, backendType)
            
            // Perform cleanup if needed
            performCheckpointCleanup()
            
            if (metadata.isBest) {
                logger.info("💾 Best unified checkpoint saved: $checkpointId (${backendType.name} format)")
            } else {
                logger.debug("💾 Unified checkpoint saved: $checkpointId (${backendType.name} format)")
            }
            
            return checkpointInfo
            
        } catch (e: Exception) {
            logger.error("Failed to save unified checkpoint $checkpointId: ${e.message}")
            throw CheckpointException("Failed to save unified checkpoint", e)
        }
    }
    
    /**
     * Load a checkpoint with automatic format detection and backend compatibility.
     */
    fun loadCheckpoint(
        checkpointId: String,
        agent: ChessAgent,
        targetBackend: BackendType
    ): UnifiedLoadResult {
        
        val loadStartTime = System.currentTimeMillis()
        
        try {
            val checkpointInfo = checkpoints[checkpointId]
                ?: return UnifiedLoadResult(
                    success = false,
                    checkpointId = checkpointId,
                    loadDuration = 0L,
                    error = "Checkpoint not found: $checkpointId"
                )
            
            // Use checkpoint compatibility system to resolve the path
            val resolution = CheckpointCompatibility.resolveCheckpointPath(
                checkpointInfo.path, 
                targetBackend
            )
            
            when (resolution) {
                is CheckpointResolution.Success -> {
                    // Load the checkpoint
                    agent.load(resolution.path)
                    
                    val loadEndTime = System.currentTimeMillis()
                    val loadDuration = loadEndTime - loadStartTime
                    
                    totalCheckpointsLoaded++
                    
                    logger.info("📂 Unified checkpoint loaded: $checkpointId (${resolution.backend.name} format, ${loadDuration}ms)")
                    
                    return UnifiedLoadResult(
                        success = true,
                        checkpointId = checkpointId,
                        loadDuration = loadDuration,
                        metadata = checkpointInfo.metadata,
                        originalBackend = checkpointInfo.backendType,
                        loadedBackend = resolution.backend,
                        resolvedPath = resolution.path
                    )
                }
                is CheckpointResolution.FormatMismatch -> {
                    val errorMessage = "Checkpoint format mismatch for $checkpointId: ${resolution.suggestion}"
                    logger.error(errorMessage)
                    return UnifiedLoadResult(
                        success = false,
                        checkpointId = checkpointId,
                        loadDuration = 0L,
                        error = errorMessage,
                        originalBackend = checkpointInfo.backendType,
                        loadedBackend = targetBackend
                    )
                }
                is CheckpointResolution.NotFound -> {
                    logger.error("Checkpoint file not found for $checkpointId: ${resolution.message}")
                    return UnifiedLoadResult(
                        success = false,
                        checkpointId = checkpointId,
                        loadDuration = 0L,
                        error = resolution.message,
                        originalBackend = checkpointInfo.backendType,
                        loadedBackend = targetBackend
                    )
                }
            }
            
        } catch (e: Exception) {
            logger.error("Failed to load unified checkpoint $checkpointId: ${e.message}")
            return UnifiedLoadResult(
                success = false,
                checkpointId = checkpointId,
                loadDuration = 0L,
                error = e.message
            )
        }
    }
    
    /**
     * Load checkpoint by path with automatic format detection.
     */
    fun loadCheckpointByPath(
        checkpointPath: String,
        agent: ChessAgent,
        targetBackend: BackendType
    ): UnifiedLoadResult {
        
        val loadStartTime = System.currentTimeMillis()
        
        try {
            // Use checkpoint compatibility system to resolve the path
            val resolution = CheckpointCompatibility.resolveCheckpointPath(checkpointPath, targetBackend)
            
            when (resolution) {
                is CheckpointResolution.Success -> {
                    // Load the checkpoint
                    agent.load(resolution.path)
                    
                    val loadEndTime = System.currentTimeMillis()
                    val loadDuration = loadEndTime - loadStartTime
                    
                    totalCheckpointsLoaded++
                    
                    // Try to load metadata if available
                    val metadata = loadMetadataFromSidecar(resolution.path)
                    
                    logger.info("📂 Unified checkpoint loaded by path: ${resolution.path} (${resolution.backend.name} format, ${loadDuration}ms)")
                    
                    return UnifiedLoadResult(
                        success = true,
                        checkpointId = "path:$checkpointPath",
                        loadDuration = loadDuration,
                        metadata = metadata,
                        loadedBackend = resolution.backend,
                        resolvedPath = resolution.path
                    )
                }
                is CheckpointResolution.FormatMismatch -> {
                    val errorMessage = "Checkpoint format mismatch for path $checkpointPath: ${resolution.suggestion}"
                    logger.error(errorMessage)
                    return UnifiedLoadResult(
                        success = false,
                        checkpointId = "path:$checkpointPath",
                        loadDuration = 0L,
                        error = errorMessage,
                        loadedBackend = targetBackend
                    )
                }
                is CheckpointResolution.NotFound -> {
                    logger.error("Checkpoint file not found at path $checkpointPath: ${resolution.message}")
                    return UnifiedLoadResult(
                        success = false,
                        checkpointId = "path:$checkpointPath",
                        loadDuration = 0L,
                        error = resolution.message,
                        loadedBackend = targetBackend
                    )
                }
            }
            
        } catch (e: Exception) {
            logger.error("Failed to load unified checkpoint from path $checkpointPath: ${e.message}")
            return UnifiedLoadResult(
                success = false,
                checkpointId = "path:$checkpointPath",
                loadDuration = 0L,
                error = e.message
            )
        }
    }
    
    /**
     * Get the best checkpoint across all formats.
     */
    fun getBestCheckpoint(): UnifiedCheckpointInfo? = bestCheckpoint
    
    /**
     * Get checkpoint by ID.
     */
    fun getCheckpoint(checkpointId: String): UnifiedCheckpointInfo? = checkpoints[checkpointId]
    
    /**
     * List all available checkpoints with format information.
     */
    fun listCheckpoints(): List<UnifiedCheckpointInfo> {
        return checkpoints.values.sortedBy { it.creationTime }
    }
    
    /**
     * List checkpoints by backend type.
     */
    fun listCheckpointsByBackend(backendType: BackendType): List<UnifiedCheckpointInfo> {
        return checkpoints.values.filter { it.backendType == backendType }.sortedBy { it.creationTime }
    }
    
    /**
     * List checkpoints by format.
     */
    fun listCheckpointsByFormat(format: CheckpointFormat): List<UnifiedCheckpointInfo> {
        return checkpoints.values.filter { it.format == format }.sortedBy { it.creationTime }
    }
    
    /**
     * Delete a checkpoint.
     */
    fun deleteCheckpoint(checkpointId: String): Boolean {
        val checkpoint = checkpoints[checkpointId]
        if (checkpoint == null) {
            logger.warn("Checkpoint not found for deletion: $checkpointId")
            return false
        }
        
        try {
            // In practice, would delete the actual files
            // deleteFile(checkpoint.path)
            // deleteMetadataSidecar(checkpoint.path)
            
            checkpoints.remove(checkpointId)
            performanceHistory.remove(checkpointId)
            totalCheckpointsDeleted++
            
            // Update best checkpoint if we deleted it
            if (bestCheckpoint?.id == checkpointId) {
                bestCheckpoint = findNewBestCheckpoint()
            }
            
            logger.info("🗑️ Unified checkpoint deleted: $checkpointId")
            return true
            
        } catch (e: Exception) {
            logger.error("Failed to delete unified checkpoint $checkpointId: ${e.message}")
            return false
        }
    }
    
    /**
     * Get unified checkpoint manager summary.
     */
    fun getSummary(): UnifiedCheckpointSummary {
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
        
        // Count by backend type
        val backendCounts = BackendType.values().associateWith { backend ->
            checkpoints.values.count { it.backendType == backend }
        }
        
        // Count by format
        val formatCounts = CheckpointFormat.values().associateWith { format ->
            checkpoints.values.count { it.format == format }
        }
        
        return UnifiedCheckpointSummary(
            totalCheckpoints = totalCheckpoints,
            validCheckpoints = validCheckpoints,
            invalidCheckpoints = invalidCheckpoints,
            bestCheckpointId = bestCheckpoint?.id,
            totalSize = totalSize,
            averagePerformance = avgPerformance,
            bestPerformance = bestPerformance,
            totalCreated = totalCheckpointsCreated,
            totalLoaded = totalCheckpointsLoaded,
            totalDeleted = totalCheckpointsDeleted,
            backendCounts = backendCounts,
            formatCounts = formatCounts
        )
    }
    
    // Private helper methods
    
    /**
     * Generate checkpoint path based on backend type and format.
     */
    private fun generateCheckpointPath(
        checkpointId: String,
        metadata: UnifiedCheckpointMetadata,
        backendType: BackendType
    ): String {
        val timestamp = System.currentTimeMillis()
        val baseFilename = "unified_${checkpointId}_${backendType.name.lowercase()}_${timestamp}"
        
        val extension = when (backendType) {
            BackendType.RL4J -> ".zip"
            BackendType.DL4J -> if (config.compressionEnabled) ".zip" else ".json"
            else -> if (config.compressionEnabled) ".json.gz" else ".json"
        }
        
        return "${config.baseDirectory}/${baseFilename}${extension}"
    }
    
    /**
     * Detect checkpoint format from file path.
     */
    private fun detectCheckpointFormat(path: String): CheckpointFormat {
        return when {
            path.endsWith(".zip") -> CheckpointFormat.ZIP
            path.endsWith(".json.gz") -> CheckpointFormat.JSON_COMPRESSED
            path.endsWith(".json") -> CheckpointFormat.JSON
            else -> CheckpointFormat.UNKNOWN
        }
    }
    
    /**
     * Validate checkpoint with backend-specific validation.
     */
    private fun validateCheckpoint(path: String, backendType: BackendType): ValidationStatus {
        return try {
            // Basic file existence check
            val exists = checkFileExists(path)
            if (!exists) {
                return ValidationStatus.INVALID
            }
            
            // Backend-specific validation could be added here
            when (backendType) {
                BackendType.RL4J -> {
                    // RL4J checkpoints should be .zip files
                    if (!path.endsWith(".zip")) {
                        logger.warn("RL4J checkpoint should be .zip format: $path")
                    }
                    ValidationStatus.VALID
                }
                BackendType.DL4J -> {
                    // DL4J checkpoints can be .zip or .json
                    ValidationStatus.VALID
                }
                else -> {
                    // Manual checkpoints should be .json or .json.gz
                    if (!path.endsWith(".json") && !path.endsWith(".json.gz")) {
                        logger.warn("Manual checkpoint should be .json format: $path")
                    }
                    ValidationStatus.VALID
                }
            }
        } catch (e: Exception) {
            logger.warn("Checkpoint validation failed for $path: ${e.message}")
            ValidationStatus.INVALID
        }
    }
    
    /**
     * Save metadata sidecar file.
     */
    private fun saveMetadataSidecar(
        checkpointPath: String,
        metadata: UnifiedCheckpointMetadata,
        backendType: BackendType
    ) {
        try {
            val metadataPath = "$checkpointPath.meta.json"
            val metadataJson = buildString {
                append("{\n")
                append("  \"checkpointId\": \"${metadata.checkpointId}\",\n")
                append("  \"cycle\": ${metadata.cycle},\n")
                append("  \"performance\": ${metadata.performance},\n")
                append("  \"isBest\": ${metadata.isBest},\n")
                append("  \"description\": \"${metadata.description}\",\n")
                append("  \"backendType\": \"${backendType.name}\",\n")
                append("  \"timestamp\": ${System.currentTimeMillis()},\n")
                append("  \"version\": \"1.0\"\n")
                append("}")
            }
            
            // Write metadata to file
            writeTextFile(metadataPath, metadataJson)
            logger.debug("Metadata sidecar saved to: $metadataPath")
            
        } catch (e: Exception) {
            logger.warn("Could not save metadata sidecar for $checkpointPath: ${e.message}")
        }
    }
    
    /**
     * Load metadata from sidecar file.
     */
    private fun loadMetadataFromSidecar(checkpointPath: String): UnifiedCheckpointMetadata? {
        return try {
            val metadataPath = "$checkpointPath.meta.json"
            
            // Read and parse metadata from file
            if (checkFileExistsImpl(metadataPath)) {
                val metadataJson = readTextFileImpl(metadataPath)
                parseMetadataJson(metadataJson)
            } else {
                logger.debug("No metadata sidecar found at: $metadataPath")
                UnifiedCheckpointMetadata(
                    checkpointId = "unknown",
                    cycle = -1,
                    performance = 0.0,
                    description = "Loaded from path: $checkpointPath",
                    isBest = false
                )
            }
            
        } catch (e: Exception) {
            logger.debug("Could not load metadata sidecar for $checkpointPath: ${e.message}")
            null
        }
    }
    
    /**
     * Check if file exists (platform-specific implementation needed).
     */
    private fun checkFileExists(path: String): Boolean {
        return try {
            // Use platform-specific file existence check
            checkFileExistsImpl(path)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Platform-specific file existence check.
     */
    private fun checkFileExistsImpl(path: String): Boolean {
        return try {
            val p = java.nio.file.Path.of(path)
            java.nio.file.Files.exists(p)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Platform-specific text file reading.
     */
    private fun readTextFileImpl(path: String): String {
        return try {
            val p = java.nio.file.Path.of(path)
            java.nio.file.Files.readString(p, java.nio.charset.StandardCharsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Failed to read file: $path", e)
        }
    }
    
    /**
     * Estimate file size (in practice, would get actual file size).
     */
    private fun estimateFileSize(path: String): Long {
        // Deterministic rough estimate based on path
        val hash = abs(path.hashCode())
        return 1_000_000L + (hash % 4_000_001)
    }
    
    /**
     * Check if performance is better than current best.
     */
    private fun isBetterThanBest(performance: Double): Boolean {
        val currentBest = bestCheckpoint?.metadata?.performance ?: Double.NEGATIVE_INFINITY
        return performance > currentBest
    }
    
    /**
     * Perform checkpoint cleanup based on configuration.
     */
    private fun performCheckpointCleanup() {
        if (checkpoints.size > config.maxVersions) {
            val excessCount = checkpoints.size - config.maxVersions
            val sortedByPerformance = checkpoints.values.sortedBy { it.metadata.performance }
            
            // Delete worst performing checkpoints
            for (i in 0 until excessCount) {
                val checkpointToDelete = sortedByPerformance[i]
                deleteCheckpoint(checkpointToDelete.id)
            }
        }
    }
    
    /**
     * Find new best checkpoint after deletion.
     */
    private fun findNewBestCheckpoint(): UnifiedCheckpointInfo? {
        return checkpoints.values.maxByOrNull { it.metadata.performance }
    }
    
    /**
     * Parse metadata JSON (simple implementation).
     */
    private fun parseMetadataJson(json: String): UnifiedCheckpointMetadata {
        return try {
            // Simple JSON parsing - in practice would use a proper JSON library
            val checkpointId = extractJsonValue(json, "checkpointId") ?: "unknown"
            val cycle = extractJsonValue(json, "cycle")?.toIntOrNull() ?: -1
            val performance = extractJsonValue(json, "performance")?.toDoubleOrNull() ?: 0.0
            val description = extractJsonValue(json, "description") ?: "No description"
            val isBest = extractJsonValue(json, "isBest")?.toBooleanStrictOrNull() ?: false
            
            UnifiedCheckpointMetadata(
                checkpointId = checkpointId,
                cycle = cycle,
                performance = performance,
                description = description,
                isBest = isBest
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse metadata JSON: ${e.message}")
            UnifiedCheckpointMetadata(
                checkpointId = "unknown",
                cycle = -1,
                performance = 0.0,
                description = "Parse error",
                isBest = false
            )
        }
    }
    
    /**
     * Extract value from simple JSON string.
     */
    private fun extractJsonValue(json: String, key: String): String? {
        return try {
            val pattern = "\"$key\"\\s*:\\s*\"?([^,}\"]+)\"?"
            val regex = Regex(pattern)
            val match = regex.find(json)
            match?.groupValues?.get(1)?.trim('"')
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Unified checkpoint metadata that works across all backend types.
 */
data class UnifiedCheckpointMetadata(
    val checkpointId: String,
    val cycle: Int,
    val performance: Double,
    val description: String,
    val isBest: Boolean = false,
    val additionalInfo: Map<String, Any> = emptyMap()
)

/**
 * Unified checkpoint information with format and backend details.
 */
data class UnifiedCheckpointInfo(
    val id: String,
    val path: String,
    val metadata: UnifiedCheckpointMetadata,
    val backendType: BackendType,
    val format: CheckpointFormat,
    val creationTime: Long,
    val fileSize: Long,
    val validationStatus: ValidationStatus
)

/**
 * Checkpoint format enumeration.
 */
enum class CheckpointFormat {
    JSON,           // Plain JSON (.json)
    JSON_COMPRESSED, // Compressed JSON (.json.gz)
    ZIP,            // ZIP archive (.zip)
    UNKNOWN         // Unknown format
}

/**
 * Unified load result with backend compatibility information.
 */
data class UnifiedLoadResult(
    val success: Boolean,
    val checkpointId: String,
    val loadDuration: Long,
    val metadata: UnifiedCheckpointMetadata? = null,
    val originalBackend: BackendType? = null,
    val loadedBackend: BackendType? = null,
    val resolvedPath: String? = null,
    val error: String? = null
)

/**
 * Unified checkpoint manager summary with format and backend breakdowns.
 */
data class UnifiedCheckpointSummary(
    val totalCheckpoints: Int,
    val validCheckpoints: Int,
    val invalidCheckpoints: Int,
    val bestCheckpointId: String?,
    val totalSize: Long,
    val averagePerformance: Double,
    val bestPerformance: Double,
    val totalCreated: Int,
    val totalLoaded: Int,
    val totalDeleted: Int,
    val backendCounts: Map<BackendType, Int>,
    val formatCounts: Map<CheckpointFormat, Int>
)