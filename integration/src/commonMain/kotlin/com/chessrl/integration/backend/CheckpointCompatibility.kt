package com.chessrl.integration.backend

import com.chessrl.integration.logging.ChessRLLogger

/**
 * Checkpoint compatibility system for handling different file formats across backends.
 * 
 * This system provides:
 * - File format detection (.json for manual, .zip for DL4J)
 * - Backend routing based on file extension
 * - Helpful error messages for format mismatches
 * - Checkpoint resolution with fallback logic
 */
object CheckpointCompatibility {
    
    private val logger = ChessRLLogger.forComponent("CheckpointCompatibility")
    
    /**
     * Supported checkpoint file formats and their associated backends
     */
    enum class CheckpointFormat(val extension: String, val backend: BackendType) {
        JSON(".json", BackendType.MANUAL),
        JSON_GZ(".json.gz", BackendType.MANUAL),
        ZIP(".zip", BackendType.DL4J),
        H5(".h5", BackendType.KOTLINDL),
        SAVEDMODEL(".savedmodel", BackendType.KOTLINDL);
        
        companion object {
            /**
             * Detect format from file path
             */
            fun fromPath(path: String): CheckpointFormat? {
                return values().find { path.endsWith(it.extension) }
            }
            
            /**
             * Get all formats for a backend
             */
            fun forBackend(backend: BackendType): List<CheckpointFormat> {
                return values().filter { it.backend == backend }
            }
        }
    }
    
    /**
     * Detect the backend type from a checkpoint file path
     */
    fun detectBackendFromFile(path: String): BackendType? {
        val format = CheckpointFormat.fromPath(path)
        return format?.backend
    }
    
    /**
     * Check if a file format is compatible with a backend
     */
    fun isFormatCompatible(path: String, backend: BackendType): Boolean {
        val detectedBackend = detectBackendFromFile(path)
        return detectedBackend == backend
    }
    
    /**
     * Resolve checkpoint path for loading, handling different file extensions
     * and providing fallback logic when exact path doesn't exist
     */
    fun resolveCheckpointPath(basePath: String, preferredBackend: BackendType): CheckpointResolution {
        logger.debug("Resolving checkpoint path: $basePath for backend: $preferredBackend")
        
        // If the path already has an extension and exists, use it directly
        val format = CheckpointFormat.fromPath(basePath)
        if (format != null) {
            if (fileExists(basePath)) {
                return if (format.backend == preferredBackend) {
                    CheckpointResolution.Success(basePath, format.backend, format)
                } else {
                    CheckpointResolution.FormatMismatch(
                        path = basePath,
                        detectedBackend = format.backend,
                        requestedBackend = preferredBackend,
                        suggestion = "Use --nn ${format.backend.name.lowercase()} to load this checkpoint"
                    )
                }
            } else {
                return CheckpointResolution.NotFound(
                    basePath,
                    "Checkpoint file not found: $basePath"
                )
            }
        }
        
        // Try to find a compatible file by adding extensions
        val compatibleFormats = CheckpointFormat.forBackend(preferredBackend)
        for (checkpointFormat in compatibleFormats) {
            val candidatePath = "$basePath${checkpointFormat.extension}"
            if (fileExists(candidatePath)) {
                logger.info("Found checkpoint: $candidatePath")
                return CheckpointResolution.Success(candidatePath, preferredBackend, checkpointFormat)
            }
        }
        
        // Try to find any compatible file and suggest backend switch
        val allFormats = CheckpointFormat.values()
        for (checkpointFormat in allFormats) {
            val candidatePath = "$basePath${checkpointFormat.extension}"
            if (fileExists(candidatePath)) {
                logger.warn("Found checkpoint for different backend: $candidatePath (${checkpointFormat.backend})")
                return CheckpointResolution.FormatMismatch(
                    path = candidatePath,
                    detectedBackend = checkpointFormat.backend,
                    requestedBackend = preferredBackend,
                    suggestion = "Use --nn ${checkpointFormat.backend.name.lowercase()} to load this checkpoint"
                )
            }
        }
        
        // No checkpoint found
        return CheckpointResolution.NotFound(
            basePath,
            "No checkpoint found for path: $basePath (tried extensions: ${compatibleFormats.joinToString(", ") { it.extension }})"
        )
    }
    
    /**
     * Generate appropriate file path for saving based on backend
     */
    fun generateSavePath(basePath: String, backend: BackendType): String {
        // Remove any existing extension
        val cleanPath = removeKnownExtensions(basePath)
        
        // Add appropriate extension for backend
        val format = CheckpointFormat.forBackend(backend).first() // Use primary format
        return "$cleanPath${format.extension}"
    }
    
    /**
     * Validate that a checkpoint can be loaded with the specified backend
     */
    fun validateCheckpointCompatibility(path: String, backend: BackendType): CompatibilityResult {
        if (!fileExists(path)) {
            return CompatibilityResult.Invalid("File does not exist: $path")
        }
        
        if (!fileCanRead(path)) {
            return CompatibilityResult.Invalid("File is not readable: $path")
        }
        
        val detectedBackend = detectBackendFromFile(path)
        if (detectedBackend == null) {
            return CompatibilityResult.Invalid("Unknown file format: $path")
        }
        
        if (detectedBackend != backend) {
            return CompatibilityResult.Incompatible(
                detectedBackend = detectedBackend,
                requestedBackend = backend,
                message = "Checkpoint format (${detectedBackend.name}) is not compatible with requested backend (${backend.name})"
            )
        }
        
        return CompatibilityResult.Compatible(detectedBackend)
    }
    
    /**
     * Get helpful error message for format mismatches
     */
    fun getFormatMismatchMessage(path: String, requestedBackend: BackendType): String {
        val detectedBackend = detectBackendFromFile(path)
        
        return when {
            detectedBackend == null -> {
                "Unknown checkpoint format for file: $path. " +
                "Supported formats: ${CheckpointFormat.values().joinToString(", ") { it.extension }}"
            }
            detectedBackend == BackendType.MANUAL && requestedBackend == BackendType.DL4J -> {
                "Cannot load JSON checkpoint with DL4J backend. " +
                "JSON checkpoints are from the manual backend. " +
                "Use --nn manual to load this checkpoint, or train a new model with --nn dl4j."
            }
            detectedBackend == BackendType.DL4J && requestedBackend == BackendType.MANUAL -> {
                "Cannot load ZIP checkpoint with manual backend. " +
                "ZIP checkpoints are from the DL4J backend. " +
                "Use --nn dl4j to load this checkpoint, or train a new model with --nn manual."
            }
            else -> {
                "Checkpoint format mismatch: file is ${detectedBackend.name} format but ${requestedBackend.name} backend was requested. " +
                "Use --nn ${detectedBackend.name.lowercase()} to load this checkpoint."
            }
        }
    }
    
    /**
     * Remove known checkpoint extensions from a path
     */
    private fun removeKnownExtensions(path: String): String {
        var cleanPath = path
        for (format in CheckpointFormat.values()) {
            if (cleanPath.endsWith(format.extension)) {
                cleanPath = cleanPath.removeSuffix(format.extension)
                break
            }
        }
        return cleanPath
    }
}

/**
 * Result of checkpoint path resolution
 */
sealed class CheckpointResolution {
    data class Success(
        val path: String,
        val backend: BackendType,
        val format: CheckpointCompatibility.CheckpointFormat
    ) : CheckpointResolution()
    
    data class FormatMismatch(
        val path: String,
        val detectedBackend: BackendType,
        val requestedBackend: BackendType,
        val suggestion: String
    ) : CheckpointResolution()
    
    data class NotFound(
        val basePath: String,
        val message: String
    ) : CheckpointResolution()
}

/**
 * Result of checkpoint compatibility validation
 */
sealed class CompatibilityResult {
    data class Compatible(val backend: BackendType) : CompatibilityResult()
    
    data class Incompatible(
        val detectedBackend: BackendType,
        val requestedBackend: BackendType,
        val message: String
    ) : CompatibilityResult()
    
    data class Invalid(val message: String) : CompatibilityResult()
}

/**
 * File operations using Java NIO (JVM-specific)
 */
private fun fileExists(path: String): Boolean {
    return try {
        java.nio.file.Files.exists(java.nio.file.Path.of(path))
    } catch (e: Exception) {
        false
    }
}

private fun fileCanRead(path: String): Boolean {
    return try {
        val p = java.nio.file.Path.of(path)
        java.nio.file.Files.exists(p) && java.nio.file.Files.isReadable(p)
    } catch (e: Exception) {
        false
    }
}