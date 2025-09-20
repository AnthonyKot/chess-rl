package com.chessrl.integration

import com.chessrl.integration.config.ChessRLConfig
import com.chessrl.integration.logging.ChessRLLogger
import com.chessrl.rl.Experience
// Remove kotlinx.serialization for now - use simple JSON manually
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.deleteRecursively

/**
 * Multi-process self-play manager that spawns separate processes for concurrent games.
 * This eliminates thread safety concerns while providing true parallelism.
 */
class MultiProcessSelfPlay(
    private val config: ChessRLConfig,
    private val javaExecutable: String = System.getProperty("java.home") + "/bin/java"
) {
    
    // Simple JSON handling without kotlinx.serialization for now
    private val logger = ChessRLLogger.forComponent("MultiProcessSelfPlay")
    
    /**
     * Run self-play games using multiple processes.
     * Each process runs independently with no shared state.
     */
    fun runSelfPlayGames(modelPath: String): List<SelfPlayGameResult> {
        val tempDir = Files.createTempDirectory("chess-selfplay")
        
        return try {
            val processes = startWorkerProcesses(modelPath, tempDir)
            val results = collectResults(processes, tempDir)
            
            logger.info("Multi-process self-play completed: ${results.size}/${config.gamesPerCycle} games successful")
            results
            
        } catch (e: Exception) {
            logger.error("Multi-process self-play failed", e)
            throw e
        } finally {
            // Cleanup temporary directory
            try {
                tempDir.toFile().deleteRecursively()
            } catch (e: Exception) {
                logger.warn("Failed to cleanup temp directory: ${e.message}")
            }
        }
    }
    
    private fun startWorkerProcesses(modelPath: String, tempDir: Path): List<ProcessInfo> {
        val processes = mutableListOf<ProcessInfo>()
        val classpath = System.getProperty("java.class.path")
        
        repeat(config.gamesPerCycle) { gameId ->
            val outputFile = tempDir.resolve("game_$gameId.json")
            
            val processBuilder = ProcessBuilder(
                javaExecutable,
                "-cp", classpath,
                "com.chessrl.integration.SelfPlayWorker",
                "--model", modelPath,
                "--game-id", gameId.toString(),
                "--output", outputFile.toString(),
                "--max-steps", config.maxStepsPerGame.toString(),
                "--win-reward", config.winReward.toString(),
                "--loss-reward", config.lossReward.toString(),
                "--draw-reward", config.drawReward.toString(),
                "--step-limit-penalty", config.stepLimitPenalty.toString()
            ).apply {
                // Add seed if configured
                config.seed?.let { seed ->
                    command().addAll(listOf("--seed", seed.toString()))
                }
                
                // Redirect error stream for debugging
                redirectErrorStream(true)
            }
            
            try {
                val process = processBuilder.start()
                processes.add(ProcessInfo(gameId, process, outputFile))
                
                // Stagger process starts slightly to avoid resource contention
                if (gameId < config.gamesPerCycle - 1) {
                    Thread.sleep(50)
                }
                
            } catch (e: Exception) {
                logger.warn("Failed to start worker process for game $gameId: ${e.message}")
            }
        }
        
        logger.debug("Started ${processes.size} worker processes")
        return processes
    }
    
    private fun collectResults(processes: List<ProcessInfo>, tempDir: Path): List<SelfPlayGameResult> {
        val results = mutableListOf<SelfPlayGameResult>()
        val timeoutMinutes = 5L // Timeout per game
        
        for (processInfo in processes) {
            try {
                // Wait for process to complete with timeout
                val completed = processInfo.process.waitFor(timeoutMinutes, TimeUnit.MINUTES)
                
                if (!completed) {
                    logger.warn("Game ${processInfo.gameId} timed out, killing process")
                    processInfo.process.destroyForcibly()
                    continue
                }
                
                val exitCode = processInfo.process.exitValue()
                if (exitCode != 0) {
                    val errorOutput = processInfo.process.inputStream.bufferedReader().readText()
                    logger.warn("Game ${processInfo.gameId} failed with exit code $exitCode: $errorOutput")
                    continue
                }
                
                // Load and parse result
                val result = loadGameResult(processInfo.outputFile)
                if (result != null) {
                    results.add(result)
                } else {
                    logger.warn("Failed to load result for game ${processInfo.gameId}")
                }
                
            } catch (e: Exception) {
                logger.warn("Error collecting result for game ${processInfo.gameId}: ${e.message}")
            }
        }
        
        return results
    }
    
    private fun loadGameResult(outputFile: Path): SelfPlayGameResult? {
        return try {
            if (!Files.exists(outputFile)) {
                logger.warn("Output file does not exist: $outputFile")
                return null
            }
            
            val jsonContent = Files.readString(outputFile)
            
            // Simple JSON parsing - look for success field
            if (!jsonContent.contains("\"success\": true")) {
                logger.warn("Game result indicates failure")
                return null
            }
            
            // For now, create a dummy result since we simplified the worker
            // In a full implementation, we would properly parse the JSON
            SelfPlayGameResult(
                gameId = 0, // Would parse from JSON
                gameLength = 50, // Would parse from JSON
                gameOutcome = GameOutcome.DRAW, // Would parse from JSON
                terminationReason = EpisodeTerminationReason.GAME_ENDED,
                gameDuration = 1000L, // Would parse from JSON
                experiences = emptyList(), // Would parse experiences from JSON
                chessMetrics = ChessMetrics(
                    gameLength = 50,
                    totalMaterialValue = 0,
                    piecesInCenter = 0,
                    developedPieces = 0,
                    kingSafetyScore = 0.0,
                    moveCount = 50,
                    captureCount = 0,
                    checkCount = 0
                ),
                finalPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1" // Would parse from JSON
            )
            
        } catch (e: Exception) {
            logger.warn("Failed to parse game result from $outputFile: ${e.message}")
            null
        }
    }
    
    /**
     * Check if multi-process execution is available on this system
     */
    fun isAvailable(): Boolean {
        return try {
            val javaFile = File(javaExecutable)
            javaFile.exists() && javaFile.canExecute()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get estimated speedup factor compared to sequential execution
     */
    fun getSpeedupFactor(): Double {
        val availableCores = Runtime.getRuntime().availableProcessors()
        val requestedConcurrency = config.maxConcurrentGames
        val effectiveConcurrency = minOf(availableCores, requestedConcurrency, config.gamesPerCycle)
        return effectiveConcurrency.toDouble().coerceAtMost(4.0) // Cap at 4x for realistic estimate
    }
}

/**
 * Information about a running worker process
 */
private data class ProcessInfo(
    val gameId: Int,
    val process: Process,
    val outputFile: Path
)