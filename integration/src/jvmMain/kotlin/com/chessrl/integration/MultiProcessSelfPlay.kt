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
class MultiProcessSelfPlay @JvmOverloads constructor(
    private val config: ChessRLConfig,
    private val defaultJavaExecutable: String = System.getProperty("java.home") + "/bin/java"
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
        val classpath = resolveEffectiveClasspath()
        runCatching {
            val entryCount = classpath.split(System.getProperty("path.separator") ?: ":").size
            logger.debug("Multi-process: resolved worker classpath entries: $entryCount")
        }
        val totalGames = config.gamesPerCycle
        val concurrency = minOf(config.maxConcurrentGames, totalGames)
        val processes = mutableListOf<ProcessInfo>()

        var nextGameId = 0
        val active = mutableListOf<ProcessInfo>()

        fun startOne(id: Int): ProcessInfo? {
            val outputFile = tempDir.resolve("game_$id.json")
            val java = findJavaExecutable() ?: return null
            // Try to propagate the master seed from the parent process for deterministic behavior
            val masterSeed: Long? = try {
                SeedManager.getInstance().getMasterSeed()
            } catch (_: Exception) {
                config.seed // May be null
            }
            val pb = ProcessBuilder(
                java,
                "-cp", classpath,
                "com.chessrl.integration.SelfPlayWorker",
                "--model", modelPath,
                "--game-id", id.toString(),
                "--output", outputFile.toString(),
                "--max-steps", config.maxStepsPerGame.toString(),
                "--win-reward", config.winReward.toString(),
                "--loss-reward", config.lossReward.toString(),
                "--draw-reward", config.drawReward.toString(),
                "--step-limit-penalty", config.stepLimitPenalty.toString(),
                "--hidden-layers", config.hiddenLayers.joinToString(",")
            ).apply {
                masterSeed?.let { seed -> command().addAll(listOf("--seed", seed.toString())) }
                config.trainOpponentType?.let { opt -> command().addAll(listOf("--opponent", opt)) }
                if ((config.trainOpponentType?.equals("minimax", true) == true)) {
                    command().addAll(listOf("--opponent-depth", config.trainOpponentDepth.toString()))
                }
                // Training environment flags for parity with parent process
                command().addAll(listOf(
                    "--train-early-adjudication", config.trainEarlyAdjudication.toString(),
                    "--train-resign-threshold", config.trainResignMaterialThreshold.toString(),
                    "--train-no-progress-plies", config.trainNoProgressPlies.toString()
                ))
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

    /**
     * Resolve a robust classpath for worker processes.
     *
     * In Gradle, JavaExec may use a "pathing JAR" (a small jar whose MANIFEST.MF
     * contains a long Class-Path). When the parent is launched with a single
     * JAR on -cp, System.getProperty("java.class.path") will only include that
     * pathing JAR. Passing that JAR again via -cp to child processes does NOT
     * honor its manifest Class-Path, leading to ClassNotFound errors.
     *
     * This method detects that scenario and expands the pathing JAR's manifest
     * Class-Path entries into an explicit OS-separated classpath string.
     */
    private fun resolveEffectiveClasspath(): String {
        val sep = System.getProperty("path.separator")
        val raw = System.getProperty("java.class.path") ?: return ""

        // If classpath already contains multiple entries, use it as-is.
        if (raw.contains(sep)) return raw

        // If it's a single JAR, attempt to expand its manifest Class-Path
        val f = File(raw)
        if (f.isFile && raw.endsWith(".jar", ignoreCase = true)) {
            val expanded: String? = try {
                java.util.jar.JarFile(f).use { jar ->
                    val mf = jar.manifest ?: return@use null
                    val cp = mf.mainAttributes.getValue("Class-Path") ?: return@use null
                    val baseDir = f.parentFile?.toPath() ?: Path.of(".")
                    val entries = cp.trim().split(" ").filter { it.isNotBlank() }
                    if (entries.isNotEmpty()) {
                        val resolved = entries.map { e ->
                            val p = Path.of(e)
                            val abs = if (p.isAbsolute) p else baseDir.resolve(e).normalize()
                            abs.toFile().path
                        }
                        // Include the pathing jar itself and expanded entries
                        return@use (listOf(f.path) + resolved).joinToString(sep)
                    }
                    null
                }
            } catch (_: Exception) { null }

            if (expanded != null) return expanded
        }

        return raw
    }

    // -------------------- Availability & Java resolution --------------------

    private var cachedJavaExecutable: String? = null

    private fun findJavaExecutable(): String? {
        val cached = cachedJavaExecutable
        if (cached != null) return cached
        val candidates = mutableListOf<String>()
        System.getenv("CHESSRL_JAVA_EXE")?.let { if (it.isNotBlank()) candidates += it }
        candidates += defaultJavaExecutable
        System.getenv("JAVA_HOME")?.let { candidates += (it.trimEnd('/', '\\') + "/bin/java") }
        candidates += "java"
        for (cand in candidates) {
            if (probeJava(cand)) {
                cachedJavaExecutable = cand
                logger.debug("Multi-process: using Java executable: $cand")
                return cand
            }
        }
        logger.warn("Multi-process not available: no working Java found. Tried: ${candidates.joinToString(", ")}")
        return null
    }

    private fun probeJava(exe: String): Boolean {
        return try {
            val p = ProcessBuilder(exe, "-version").redirectErrorStream(true).start()
            if (!p.waitFor(5, TimeUnit.SECONDS)) { p.destroyForcibly(); false } else p.exitValue() == 0
        } catch (_: Exception) { false }
    }
    
    private fun collectResults(processes: List<ProcessInfo>, @Suppress("UNUSED_PARAMETER") tempDir: Path): List<SelfPlayGameResult> {
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
            // Optional diagnostic: check summary count vs parsed count
            runCatching {
                val declared = Regex("\"experienceCount\"\\s*:\\s*(\\d+)").find(jsonContent)?.groupValues?.get(1)?.toIntOrNull()
                if (declared != null && declared != experiences.size) {
                    logger.debug("Experience count mismatch for ${outputFile.fileName}: summary=$declared, parsed=${experiences.size}")
                }
            }

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
                val l = line.trim()
                if (l.isEmpty()) continue
                try {
                    val sStr = Regex("\"s\"\\s*:\\s*(\\[[^\\]]*\\])").find(l)?.groupValues?.get(1) ?: continue
                    val nsStr = Regex("\"ns\"\\s*:\\s*(\\[[^\\]]*\\])").find(l)?.groupValues?.get(1) ?: continue
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
    fun isAvailable(): Boolean = findJavaExecutable() != null
    
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
