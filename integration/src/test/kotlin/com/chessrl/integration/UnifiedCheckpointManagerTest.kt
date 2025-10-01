package com.chessrl.integration

import com.chessrl.integration.backend.BackendType
import com.chessrl.integration.logging.ChessRLLogger
import kotlin.test.*

/**
 * Test for UnifiedCheckpointManager functionality.
 */
class UnifiedCheckpointManagerTest {
    
    private val logger = ChessRLLogger.forComponent("UnifiedCheckpointManagerTest")
    
    @Test
    fun testUnifiedCheckpointManagerCreation() {
        val manager = UnifiedCheckpointManager()
        assertNotNull(manager)
        
        val summary = manager.getSummary()
        assertEquals(0, summary.totalCheckpoints)
        assertEquals(0, summary.totalCreated)
        assertEquals(0, summary.totalLoaded)
        assertEquals(0, summary.totalDeleted)
        
        logger.info("UnifiedCheckpointManager created successfully")
    }
    
    @Test
    fun testCheckpointFormatDetection() {
        val manager = UnifiedCheckpointManager()
        
        // Test format detection through reflection (accessing private method)
        val detectMethod = manager.javaClass.getDeclaredMethod("detectCheckpointFormat", String::class.java)
        detectMethod.isAccessible = true
        
        assertEquals(CheckpointFormat.JSON, detectMethod.invoke(manager, "model.json"))
        assertEquals(CheckpointFormat.JSON_COMPRESSED, detectMethod.invoke(manager, "model.json.gz"))
        assertEquals(CheckpointFormat.ZIP, detectMethod.invoke(manager, "model.zip"))
        assertEquals(CheckpointFormat.UNKNOWN, detectMethod.invoke(manager, "model.bin"))
        
        logger.info("Checkpoint format detection working correctly")
    }
    
    @Test
    fun testCheckpointPathGeneration() {
        val manager = UnifiedCheckpointManager()
        val metadata = UnifiedCheckpointMetadata(
            checkpointId = "test-checkpoint",
            cycle = 10,
            performance = 0.75,
            description = "Test checkpoint"
        )
        
        // Test path generation through reflection
        val generateMethod = manager.javaClass.getDeclaredMethod(
            "generateCheckpointPath", 
            String::class.java, 
            UnifiedCheckpointMetadata::class.java, 
            BackendType::class.java
        )
        generateMethod.isAccessible = true
        
        val manualPath = generateMethod.invoke(manager, "test", metadata, BackendType.MANUAL) as String
        val rl4jPath = generateMethod.invoke(manager, "test", metadata, BackendType.RL4J) as String
        val dl4jPath = generateMethod.invoke(manager, "test", metadata, BackendType.DL4J) as String
        
        assertTrue(manualPath.contains("manual"))
        assertTrue(manualPath.endsWith(".json.gz")) // Default compression enabled
        
        assertTrue(rl4jPath.contains("rl4j"))
        assertTrue(rl4jPath.endsWith(".zip"))
        
        assertTrue(dl4jPath.contains("dl4j"))
        assertTrue(dl4jPath.endsWith(".zip")) // Default compression enabled
        
        logger.info("Checkpoint path generation working correctly")
        logger.info("Manual path: $manualPath")
        logger.info("RL4J path: $rl4jPath")
        logger.info("DL4J path: $dl4jPath")
    }
    
    @Test
    fun testMetadataJsonParsing() {
        val manager = UnifiedCheckpointManager()
        
        val testJson = """
        {
          "checkpointId": "test-123",
          "cycle": 42,
          "performance": 0.85,
          "isBest": true,
          "description": "Test checkpoint metadata",
          "backendType": "MANUAL",
          "timestamp": 1234567890,
          "version": "1.0"
        }
        """.trimIndent()
        
        // Test JSON parsing through reflection
        val parseMethod = manager.javaClass.getDeclaredMethod("parseMetadataJson", String::class.java)
        parseMethod.isAccessible = true
        
        val metadata = parseMethod.invoke(manager, testJson) as UnifiedCheckpointMetadata
        
        assertEquals("test-123", metadata.checkpointId)
        assertEquals(42, metadata.cycle)
        assertEquals(0.85, metadata.performance, 0.001)
        assertTrue(metadata.isBest)
        assertEquals("Test checkpoint metadata", metadata.description)
        
        logger.info("Metadata JSON parsing working correctly")
        logger.info("Parsed metadata: $metadata")
    }
    
    @Test
    fun testJsonValueExtraction() {
        val manager = UnifiedCheckpointManager()
        
        val testJson = """{"key1": "value1", "key2": 123, "key3": true}"""
        
        // Test JSON value extraction through reflection
        val extractMethod = manager.javaClass.getDeclaredMethod("extractJsonValue", String::class.java, String::class.java)
        extractMethod.isAccessible = true
        
        assertEquals("value1", extractMethod.invoke(manager, testJson, "key1"))
        assertEquals("123", extractMethod.invoke(manager, testJson, "key2"))
        assertEquals("true", extractMethod.invoke(manager, testJson, "key3"))
        assertNull(extractMethod.invoke(manager, testJson, "nonexistent"))
        
        logger.info("JSON value extraction working correctly")
    }
    
    @Test
    fun testCheckpointListingByBackend() {
        val manager = UnifiedCheckpointManager()
        
        // Initially empty
        assertEquals(0, manager.listCheckpointsByBackend(BackendType.MANUAL).size)
        assertEquals(0, manager.listCheckpointsByBackend(BackendType.RL4J).size)
        assertEquals(0, manager.listCheckpointsByBackend(BackendType.DL4J).size)
        
        logger.info("Checkpoint listing by backend working correctly")
    }
    
    @Test
    fun testCheckpointListingByFormat() {
        val manager = UnifiedCheckpointManager()
        
        // Initially empty
        assertEquals(0, manager.listCheckpointsByFormat(CheckpointFormat.JSON).size)
        assertEquals(0, manager.listCheckpointsByFormat(CheckpointFormat.JSON_COMPRESSED).size)
        assertEquals(0, manager.listCheckpointsByFormat(CheckpointFormat.ZIP).size)
        
        logger.info("Checkpoint listing by format working correctly")
    }
    
    @Test
    fun testUnifiedCheckpointSummary() {
        val manager = UnifiedCheckpointManager()
        val summary = manager.getSummary()
        
        assertNotNull(summary)
        assertEquals(0, summary.totalCheckpoints)
        assertEquals(0, summary.validCheckpoints)
        assertEquals(0, summary.invalidCheckpoints)
        assertNull(summary.bestCheckpointId)
        assertEquals(0L, summary.totalSize)
        assertEquals(0.0, summary.averagePerformance)
        assertEquals(0.0, summary.bestPerformance)
        
        // Check backend counts
        BackendType.values().forEach { backend ->
            assertEquals(0, summary.backendCounts[backend])
        }
        
        // Check format counts
        CheckpointFormat.values().forEach { format ->
            assertEquals(0, summary.formatCounts[format])
        }
        
        logger.info("Unified checkpoint summary working correctly")
        logger.info("Summary: $summary")
    }
    
    @Test
    fun testUnifiedCheckpointMetadata() {
        val metadata = UnifiedCheckpointMetadata(
            checkpointId = "test-checkpoint",
            cycle = 100,
            performance = 0.92,
            description = "High-performance checkpoint",
            isBest = true,
            additionalInfo = mapOf("extra" to "data")
        )
        
        assertEquals("test-checkpoint", metadata.checkpointId)
        assertEquals(100, metadata.cycle)
        assertEquals(0.92, metadata.performance, 0.001)
        assertEquals("High-performance checkpoint", metadata.description)
        assertTrue(metadata.isBest)
        assertEquals("data", metadata.additionalInfo["extra"])
        
        logger.info("Unified checkpoint metadata working correctly")
    }
    
    @Test
    fun testUnifiedLoadResult() {
        val metadata = UnifiedCheckpointMetadata(
            checkpointId = "test",
            cycle = 50,
            performance = 0.8,
            description = "Test"
        )
        
        val successResult = UnifiedLoadResult(
            success = true,
            checkpointId = "test-checkpoint",
            loadDuration = 1500L,
            metadata = metadata,
            originalBackend = BackendType.MANUAL,
            loadedBackend = BackendType.RL4J,
            resolvedPath = "/path/to/checkpoint.zip"
        )
        
        assertTrue(successResult.success)
        assertEquals("test-checkpoint", successResult.checkpointId)
        assertEquals(1500L, successResult.loadDuration)
        assertEquals(metadata, successResult.metadata)
        assertEquals(BackendType.MANUAL, successResult.originalBackend)
        assertEquals(BackendType.RL4J, successResult.loadedBackend)
        assertEquals("/path/to/checkpoint.zip", successResult.resolvedPath)
        assertNull(successResult.error)
        
        val failureResult = UnifiedLoadResult(
            success = false,
            checkpointId = "failed-checkpoint",
            loadDuration = 0L,
            error = "File not found"
        )
        
        assertFalse(failureResult.success)
        assertEquals("failed-checkpoint", failureResult.checkpointId)
        assertEquals(0L, failureResult.loadDuration)
        assertEquals("File not found", failureResult.error)
        assertNull(failureResult.metadata)
        
        logger.info("Unified load result working correctly")
    }
}