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
        // Backward compatibility: start all at once (deprecated). Use batched execution instead.
        return runGamesBatched(modelPath, tempDir)
    }

    private fun runGamesBatched(modelPath: String, tempDir: Path): List<ProcessInfo> {
        val classpath = System.getProperty("java.class.path")
        val totalGames = config.gamesPerCycle
        val concurrency = minOf(config.maxConcurrentGames, totalGames)
        val processes = mutableListOf<ProcessInfo>()

        var nextGameId = 0
        val active = mutableListOf<ProcessInfo>()

        fun startOne(id: Int): ProcessInfo? {
            val outputFile = tempDir.resolve("game_$id.json")
            val pb = ProcessBuilder(
                javaExecutable,
                "-cp", classpath,
                "com.chessrl.integration.SelfPlayWorker",
                "--model", modelPath,
                "--game-id", id.toString(),
                "--output", outputFile.toString(),
                "--max-steps", config.maxStepsPerGame.toString(),
                "--win-reward", config.winReward.toString(),
                "--loss-reward", config.lossReward.toString(),
                "--draw-reward", config.drawReward.toString(),
                "--step-limit-penalty", config.stepLimitPenalty.toString()
            ).apply {
                config.seed?.let { seed -> command().addAll(listOf("--seed", seed.toString())) }
                redirectErrorStream(true)
            }
            return try {
                val proc = pb.start()
                ProcessInfo(id, proc, outputFile)
            } catch (e: Exception) {
                logger.warn("Failed to start worker process for game $id: ${e.message}")
                null
            }
        }

        // Fill initial slots
        while (nextGameId < totalGames && active.size < concurrency) {
            startOne(nextGameId)?.let { active.add(it) }
            nextGameId++
        }

        // Refill as processes finish
        while (processes.size < totalGames) {
            val iter = active.iterator()
            while (iter.hasNext()) {
                val pi = iter.next()
                if (!pi.process.isAlive) {
                    processes.add(pi)
                    iter.remove()
                }
            }
            while (nextGameId < totalGames && active.size < concurrency) {
                startOne(nextGameId)?.let { active.add(it) }
                nextGameId++
            }
            if (processes.size < totalGames) Thread.sleep(20)
        }

        logger.debug("Started and completed ${processes.size} worker processes in batched mode (concurrency=$concurrency)")
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
            if (!jsonContent.contains("\"success\": true")) {
                logger.warn("Game result indicates failure")
                return null
            }

            fun extractInt(re: String): Int? = Regex(re).find(jsonContent)?.groupValues?.get(1)?.toIntOrNull()
            fun extractLong(re: String): Long? = Regex(re).find(jsonContent)?.groupValues?.get(1)?.toLongOrNull()
            fun extractStr(re: String): String? = Regex(re).find(jsonContent)?.groupValues?.get(1)

            val gid = extractInt("\"gameId\"\\s*:\\s*(\\d+)") ?: 0
            val glen = extractInt("\"gameLength\"\\s*:\\s*(\\d+)") ?: 0
            val gout = extractStr("\"gameOutcome\"\\s*:\\s*\"([^\"]+)\"") ?: "DRAW"
            val treason = extractStr("\"terminationReason\"\\s*:\\s*\"([^\"]+)\"") ?: "GAME_ENDED"
            val gdur = extractLong("\"gameDuration\"\\s*:\\s*(\\d+)") ?: 0L
            val finalFen = extractStr("\"finalPosition\"\\s*:\\s*\"([^\"]+)\"") ?: ""

            val experiences = parseNdjsonExperiences(Path.of(outputFile.toString() + ".ndjson"))

            SelfPlayGameResult(
                gameId = gid,
                gameLength = glen,
                gameOutcome = GameOutcome.valueOf(gout),
                terminationReason = EpisodeTerminationReason.valueOf(treason),
                gameDuration = gdur,
                experiences = experiences,
                chessMetrics = ChessMetrics(
                    gameLength = glen,
                    totalMaterialValue = 0,
                    piecesInCenter = 0,
                    developedPieces = 0,
                    kingSafetyScore = 0.0,
                    moveCount = glen,
                    captureCount = 0,
                    checkCount = 0
                ),
                finalPosition = finalFen
            )
            
        } catch (e: Exception) {
            logger.warn("Failed to parse game result from $outputFile: ${e.message}")
            null
        }
    }

    private fun parseNdjsonExperiences(path: Path): List<Experience<DoubleArray, Int>> {
        if (!Files.exists(path)) return emptyList()
        val exps = mutableListOf<Experience<DoubleArray, Int>>()
        Files.newBufferedReader(path).use { reader ->
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                val l = line!!.trim()
                if (l.isEmpty()) continue
                try {
                    val sStr = Regex("\"s\"\\s*:\\s*(\\[[^]]*\\))").find(l)?.groupValues?.get(1) ?: continue
                    val nsStr = Regex("\"ns\"\\s*:\\s*(\\[[^]]*\\))").find(l)?.groupValues?.get(1) ?: continue
                    val a = Regex("\"a\"\\s*:\\s*(\\-?\\d+)").find(l)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                    val r = Regex("\"r\"\\s*:\\s*([\\-0-9.eE]+)").find(l)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                    val d = Regex("\"d\"\\s*:\\s*(true|false)").find(l)?.groupValues?.get(1)?.toBoolean() ?: false
                    val s = parseDoubleArray(sStr)
                    val ns = parseDoubleArray(nsStr)
                    exps.add(Experience(state = s, action = a, reward = r, nextState = ns, done = d))
                } catch (_: Exception) {
                    // skip malformed lines
                }
            }
        }
        return exps
    }

    private fun parseDoubleArray(arr: String): DoubleArray {
        val inner = arr.trim().removePrefix("[").removeSuffix("]").trim()
        if (inner.isEmpty()) return DoubleArray(0)
        val parts = inner.split(',')
        val out = DoubleArray(parts.size)
        var i = 0
        for (p in parts) {
            out[i++] = p.trim().toDoubleOrNull() ?: 0.0
        }
        return out
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
