package com.chessrl.integration

import com.chessrl.integration.backend.RL4JAvailability
import com.chessrl.integration.backend.rl4j.RL4JChessAgent
import kotlin.test.Test
import kotlin.test.assertTrue

class RL4JImplementationStatusTest {
    
    @Test
    fun testRL4JImplementationStatus() {
        // Skip test if RL4J is not available
        if (!RL4JAvailability.isAvailable()) {
            println("RL4J not available, skipping test")
            return
        }
        
        println("=== Testing RL4J Implementation Status ===")
        
        val config = ChessAgentConfig()
        val agent = RL4JChessAgent(config)
        
        // Capture initialization output
        var initializationSuccessful = false
        var usedRealImplementation = false
        var usedMockImplementation = false
        
        try {
            agent.initialize()
            initializationSuccessful = true
            println("✅ RL4J agent initialized successfully")
            
            // Try to determine which implementation was used by testing behavior
            // Real QLearningDiscreteDense would have different characteristics than mocks
            
            // Test save/load to see if we get real files
            val checkpointPath = "test_implementation_status.zip"
            agent.save(checkpointPath)
            
            val checkpointFile = java.io.File(checkpointPath)
            if (checkpointFile.exists()) {
                val fileSize = checkpointFile.length()
                println("📁 Checkpoint file created: $fileSize bytes")
                
                // Examine file contents to determine if it's real or mock
                val zipInputStream = java.util.zip.ZipInputStream(java.io.FileInputStream(checkpointFile))
                var hasModelJson = false
                var hasMetadataJson = false
                var entry = zipInputStream.nextEntry
                
                while (entry != null) {
                    when (entry.name) {
                        "model.json" -> hasModelJson = true
                        "metadata.json" -> hasMetadataJson = true
                    }
                    entry = zipInputStream.nextEntry
                }
                zipInputStream.close()
                
                if (hasModelJson && hasMetadataJson) {
                    println("📋 Checkpoint contains model.json and metadata.json")
                    
                    // Read model.json to see if it contains real RL4J data or mock data
                    val zipInputStream2 = java.util.zip.ZipInputStream(java.io.FileInputStream(checkpointFile))
                    var modelEntry = zipInputStream2.nextEntry
                    while (modelEntry != null && modelEntry.name != "model.json") {
                        modelEntry = zipInputStream2.nextEntry
                    }
                    
                    if (modelEntry != null) {
                        val modelContent = zipInputStream2.readBytes().toString(Charsets.UTF_8)
                        println("📄 Model content: $modelContent")
                        
                        if (modelContent.contains("RL4J_DQN") && modelContent.contains("hiddenLayers")) {
                            if (modelContent.contains("\"type\":\"RL4J_DQN\"")) {
                                usedMockImplementation = true
                                println("🎭 Using MOCK implementation (contains mock JSON structure)")
                            } else {
                                usedRealImplementation = true
                                println("⚡ Using REAL implementation (contains real RL4J data)")
                            }
                        }
                    }
                    zipInputStream2.close()
                }
                
                // Clean up
                checkpointFile.delete()
            }
            
        } catch (e: Exception) {
            println("❌ RL4J implementation test failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
        
        // Summary
        println("\n=== Implementation Status Summary ===")
        println("✅ Initialization successful: $initializationSuccessful")
        println("⚡ Real QLearningDiscreteDense: $usedRealImplementation")
        println("🎭 Mock implementation: $usedMockImplementation")
        
        if (usedRealImplementation) {
            println("🎉 SUCCESS: Real QLearningDiscreteDense instantiation is working!")
        } else if (usedMockImplementation) {
            println("⚠️  FALLBACK: Using mock implementation (real RL4J creation failed)")
        } else {
            println("❓ UNKNOWN: Could not determine implementation type")
        }
        
        assertTrue(initializationSuccessful, "RL4J agent should initialize successfully")
    }
}