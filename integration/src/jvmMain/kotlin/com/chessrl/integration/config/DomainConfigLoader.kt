package com.chessrl.integration.config

import java.nio.file.Files
import java.nio.file.Path

/**
 * Loader for domain-scoped configuration profiles located under the configurable
 * config directory (default: ./config).
 *
 * Supports composing multiple profiles across domains via a single --profile string
 * (comma-separated; optionally domain-qualified as "domain:name"). Also supports
 * loading root meta-configs via --config which specify a list of profiles and
 * inline overrides. Finally, generic overrides can be applied via --override key=value.
 */
object DomainConfigLoader {
    enum class Domain(val dirName: String, val allowedKeys: Set<String>) {
        NETWORK(
            dirName = "network",
            allowedKeys = setOf("hiddenLayers", "learningRate", "batchSize")
        ),
        RL(
            dirName = "rl",
            allowedKeys = setOf("explorationRate", "targetUpdateFrequency", "doubleDqn", "gamma", "maxExperienceBuffer", "replayType", "maxBatchesPerCycle")
        ),
        SELFPLAY(
            dirName = "selfplay",
            allowedKeys = setOf("gamesPerCycle", "maxConcurrentGames", "maxStepsPerGame", "maxCycles")
        ),
        REWARDS(
            dirName = "rewards",
            allowedKeys = setOf("winReward", "lossReward", "drawReward", "stepLimitPenalty")
        ),
        SYSTEM(
            dirName = "system",
            allowedKeys = setOf(
                "seed", "checkpointInterval", "checkpointDirectory", "evaluationGames",
                // Checkpoint manager controls
                "checkpointMaxVersions", "checkpointValidationEnabled", "checkpointCompressionEnabled", "checkpointAutoCleanupEnabled",
                // Logging controls (optional via config)
                "logInterval", "summaryOnly", "metricsFile"
            )
        );

        companion object {
            fun fromName(name: String): Domain? {
                return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.dirName.equals(name, ignoreCase = true) }
            }
        }
    }

    data class ProfileSelection(val domain: Domain?, val name: String)

    /** Compose a config from a composite profile spec (e.g., "big-network,long-games" or
     *  "network:big,selfplay:long"). If a domain doesn't have an explicit selection, a
     *  domain default file (default.yaml) will be applied when present. */
    fun composeFromProfiles(
        profileSpec: String,
        baseDir: String = "config",
        base: ChessRLConfig = ChessRLConfig()
    ): ChessRLConfig {
        val selections = parseProfileSpec(profileSpec)
        return composeFromSelections(selections, baseDir, base)
    }

    /** Compose a config from a parsed list of selections. */
    fun composeFromSelections(
        selections: List<ProfileSelection>,
        baseDir: String = "config",
        base: ChessRLConfig = ChessRLConfig()
    ): ChessRLConfig {
        var config = base

        // Map unqualified names to domains by unique match across domain dirs
        val resolved = resolveSelections(selections, baseDir)

        // For each domain, apply selected profile, or default if present
        for (domain in Domain.entries) {
            val selectedName = resolved[domain]
            if (selectedName == null) {
                val defaultPath = domainFilePath(baseDir, domain, "default")
                if (defaultPath != null) {
                    config = applyDomainFile(defaultPath, domain, config)
                }
                continue
            }
            val path = domainFilePath(baseDir, domain, selectedName)
                ?: throw IllegalArgumentException("Profile '$selectedName' not found in ${domain.dirName}")
            config = applyDomainFile(path, domain, config)
        }

        return config
    }

    /** Load a root meta-config which can specify profiles and overrides. */
    fun loadRootConfig(
        nameOrPath: String,
        baseDir: String = "config",
        base: ChessRLConfig = ChessRLConfig()
    ): ChessRLConfig {
        val path = resolveRootConfigPath(nameOrPath, baseDir)
            ?: throw IllegalArgumentException("Root config '$nameOrPath' not found in '$baseDir' or as path")

        val lines = Files.readAllLines(path)

        val profiles = mutableListOf<String>()
        val overrides = mutableMapOf<String, String>()

        var section: String? = null
        for (raw in lines) {
            val line = raw.trimEnd()
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (!line.startsWith(" ")) {
                // section header
                section = when {
                    trimmed.equals("profiles:", ignoreCase = true) -> "profiles"
                    trimmed.equals("use:", ignoreCase = true) -> "profiles"
                    trimmed.equals("overrides:", ignoreCase = true) -> "overrides"
                    else -> null
                }
                continue
            }
            when (section) {
                "profiles" -> {
                    // Expect list items: - network:big or - big-network
                    val item = trimmed.removePrefix("-").trim()
                    if (item.isNotEmpty()) profiles.add(item)
                }
                "overrides" -> {
                    val idx = trimmed.indexOf(":")
                    if (idx > 0) {
                        val key = trimmed.substring(0, idx).trim()
                        val value = trimmed.substring(idx + 1).trim().trim('"')
                        overrides[key] = value
                    }
                }
            }
        }

        val selections = profiles.flatMap { parseProfileSpec(it) }
        var config = composeFromSelections(selections, baseDir, base)
        if (overrides.isNotEmpty()) {
            config = applyOverrides(config, overrides)
        }
        return config
    }

    /** Parse profile spec into selections. */
    fun parseProfileSpec(spec: String): List<ProfileSelection> {
        if (spec.isBlank()) return emptyList()
        return spec.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { token ->
                val parts = token.split(':', limit = 2)
                if (parts.size == 2) {
                    val dom = Domain.fromName(parts[0].trim())
                        ?: throw IllegalArgumentException("Unknown domain '${parts[0]}' in profile '$token'")
                    ProfileSelection(dom, parts[1].trim())
                } else {
                    ProfileSelection(null, parts[0].trim())
                }
            }
    }

    /** Resolve selections: unqualified names are matched against all domains and must be unique. */
    private fun resolveSelections(
        selections: List<ProfileSelection>,
        baseDir: String
    ): Map<Domain, String> {
        val result = mutableMapOf<Domain, String>()
        for (sel in selections) {
            if (sel.domain != null) {
                result[sel.domain] = sel.name
            } else {
                // Try to find unique domain containing this profile name
                val matching = Domain.entries.filter { hasDomainProfile(baseDir, it, sel.name) }
                if (matching.isEmpty()) {
                    throw IllegalArgumentException("Profile '${sel.name}' not found in any domain under $baseDir")
                }
                if (matching.size > 1) {
                    val domains = matching.joinToString(", ") { it.dirName }
                    throw IllegalArgumentException("Profile '${sel.name}' is ambiguous across: $domains. Use domain:name.")
                }
                result[matching.first()] = sel.name
            }
        }
        return result
    }

    private fun hasDomainProfile(baseDir: String, domain: Domain, name: String): Boolean {
        return domainFilePath(baseDir, domain, name) != null
    }

    private fun domainFilePath(baseDir: String, domain: Domain, name: String): Path? {
        val candidates = listOf("$name.yaml", "$name.yml", "$name.json")
        val bases = listOf(
            Path.of(baseDir, domain.dirName),
            Path.of("..", baseDir, domain.dirName)
        )
        for (b in bases) {
            for (c in candidates) {
                val p = b.resolve(c)
                if (Files.exists(p)) return p
            }
        }
        return null
    }

    private fun resolveRootConfigPath(nameOrPath: String, baseDir: String): Path? {
        // Absolute or relative direct path
        val direct = Path.of(nameOrPath)
        if (Files.exists(direct)) return direct

        val candidates = listOf("$nameOrPath.yaml", "$nameOrPath.yml", "$nameOrPath.json")
        val bases = listOf(
            Path.of(baseDir),
            Path.of("..", baseDir)
        )
        for (b in bases) {
            for (c in candidates) {
                val r = b.resolve(c)
                if (Files.exists(r)) return r
            }
        }
        return null
    }

    private fun applyDomainFile(path: Path, domain: Domain, base: ChessRLConfig): ChessRLConfig {
        val content = Files.readString(path)
        val presentKeys = parseKeys(content)
        val filteredKeys = presentKeys.filter { it in domain.allowedKeys }.toSet()
        if (filteredKeys.isEmpty()) return base

        // Parse full config from YAML content using existing parser, then overlay only present keys
        val parsed = ConfigParser.parseYaml(content)
        return overlay(base, parsed, filteredKeys)
    }

    /** Apply typed overrides (key -> string value), supporting camelCase or kebab-case keys. */
    fun applyOverrides(base: ChessRLConfig, overrides: Map<String, String>): ChessRLConfig {
        var cfg = base
        for ((rawKey, value) in overrides) {
            val key = canonicalizeKey(rawKey)
            cfg = applySingle(cfg, key, value)
        }
        return cfg
    }

    // Extract YAML keys present in the content (simple parser: key: value per line)
    private fun parseKeys(yaml: String): Set<String> {
        val keys = mutableSetOf<String>()
        yaml.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
            if (trimmed.startsWith("- ")) return@forEach // lists unsupported here
            val idx = trimmed.indexOf(":")
            if (idx <= 0) return@forEach
            val key = trimmed.substring(0, idx).trim()
            if (key.isNotEmpty()) keys.add(key)
        }
        return keys
    }

    private fun overlay(base: ChessRLConfig, from: ChessRLConfig, keys: Set<String>): ChessRLConfig {
        var cfg = base
        keys.forEach { key ->
            cfg = applyFrom(cfg, from, key)
        }
        return cfg
    }

    private fun applyFrom(base: ChessRLConfig, from: ChessRLConfig, key: String): ChessRLConfig {
        return when (key) {
            "hiddenLayers" -> base.copy(hiddenLayers = from.hiddenLayers)
            "learningRate" -> base.copy(learningRate = from.learningRate)
            "batchSize" -> base.copy(batchSize = from.batchSize)
            "explorationRate" -> base.copy(explorationRate = from.explorationRate)
            "targetUpdateFrequency" -> base.copy(targetUpdateFrequency = from.targetUpdateFrequency)
            "maxExperienceBuffer" -> base.copy(maxExperienceBuffer = from.maxExperienceBuffer)
            "doubleDqn" -> base.copy(doubleDqn = from.doubleDqn)
            "gamma" -> base.copy(gamma = from.gamma)
            "maxBatchesPerCycle" -> base.copy(maxBatchesPerCycle = from.maxBatchesPerCycle)
            "gamesPerCycle" -> base.copy(gamesPerCycle = from.gamesPerCycle)
            "maxConcurrentGames" -> base.copy(maxConcurrentGames = from.maxConcurrentGames)
            "maxStepsPerGame" -> base.copy(maxStepsPerGame = from.maxStepsPerGame)
            "maxCycles" -> base.copy(maxCycles = from.maxCycles)
            "winReward" -> base.copy(winReward = from.winReward)
            "lossReward" -> base.copy(lossReward = from.lossReward)
            "drawReward" -> base.copy(drawReward = from.drawReward)
            "stepLimitPenalty" -> base.copy(stepLimitPenalty = from.stepLimitPenalty)
            "seed" -> base.copy(seed = from.seed)
            "checkpointInterval" -> base.copy(checkpointInterval = from.checkpointInterval)
            "checkpointDirectory" -> base.copy(checkpointDirectory = from.checkpointDirectory)
            "evaluationGames" -> base.copy(evaluationGames = from.evaluationGames)
            // checkpoint and logging
            "checkpointMaxVersions" -> base.copy(checkpointMaxVersions = from.checkpointMaxVersions)
            "checkpointValidationEnabled" -> base.copy(checkpointValidationEnabled = from.checkpointValidationEnabled)
            "checkpointCompressionEnabled" -> base.copy(checkpointCompressionEnabled = from.checkpointCompressionEnabled)
            "checkpointAutoCleanupEnabled" -> base.copy(checkpointAutoCleanupEnabled = from.checkpointAutoCleanupEnabled)
            "logInterval" -> base.copy(logInterval = from.logInterval)
            "summaryOnly" -> base.copy(summaryOnly = from.summaryOnly)
            "metricsFile" -> base.copy(metricsFile = from.metricsFile)
            else -> base
        }
    }

    private fun applySingle(base: ChessRLConfig, key: String, raw: String): ChessRLConfig {
        return when (key) {
            "hiddenLayers" -> base.copy(hiddenLayers = parseHiddenLayers(raw))
            "learningRate" -> base.copy(learningRate = raw.toDouble())
            "batchSize" -> base.copy(batchSize = raw.toInt())
            "explorationRate" -> base.copy(explorationRate = raw.toDouble())
            "targetUpdateFrequency" -> base.copy(targetUpdateFrequency = raw.toInt())
            "maxExperienceBuffer" -> base.copy(maxExperienceBuffer = raw.toInt())
            "doubleDqn" -> base.copy(doubleDqn = raw.equals("true", true))
            "gamma" -> base.copy(gamma = raw.toDouble())
            "maxBatchesPerCycle" -> base.copy(maxBatchesPerCycle = raw.toInt())
            "gamesPerCycle" -> base.copy(gamesPerCycle = raw.toInt())
            "maxConcurrentGames" -> base.copy(maxConcurrentGames = raw.toInt())
            "maxStepsPerGame" -> base.copy(maxStepsPerGame = raw.toInt())
            "maxCycles" -> base.copy(maxCycles = raw.toInt())
            "winReward" -> base.copy(winReward = raw.toDouble())
            "lossReward" -> base.copy(lossReward = raw.toDouble())
            "drawReward" -> base.copy(drawReward = raw.toDouble())
            "stepLimitPenalty" -> base.copy(stepLimitPenalty = raw.toDouble())
            "seed" -> base.copy(seed = if (raw.equals("null", true)) null else raw.toLong())
            "checkpointInterval" -> base.copy(checkpointInterval = raw.toInt())
            "checkpointDirectory" -> base.copy(checkpointDirectory = raw)
            "evaluationGames" -> base.copy(evaluationGames = raw.toInt())
            // checkpoint and logging
            "checkpointMaxVersions" -> base.copy(checkpointMaxVersions = raw.toInt())
            "checkpointValidationEnabled" -> base.copy(checkpointValidationEnabled = raw.equals("true", true))
            "checkpointCompressionEnabled" -> base.copy(checkpointCompressionEnabled = raw.equals("true", true))
            "checkpointAutoCleanupEnabled" -> base.copy(checkpointAutoCleanupEnabled = raw.equals("true", true))
            "logInterval" -> base.copy(logInterval = raw.toInt())
            "summaryOnly" -> base.copy(summaryOnly = raw.equals("true", true))
            "metricsFile" -> base.copy(metricsFile = raw)
            else -> base
        }
    }

    private fun parseHiddenLayers(value: String): List<Int> {
        val clean = value.trim()
        return when {
            clean.startsWith("[") && clean.endsWith("]") -> {
                clean.removeSurrounding("[", "]").split(',').map { it.trim().toInt() }
            }
            "," in clean -> clean.split(',').map { it.trim().toInt() }
            clean.contains(" ") -> clean.split("\\s+".toRegex()).map { it.trim().toInt() }
            clean.isNotEmpty() -> listOf(clean.toInt())
            else -> emptyList()
        }
    }

    private fun canonicalizeKey(raw: String): String {
        if (!raw.contains('-')) return raw
        val parts = raw.split('-')
        return buildString {
            parts.forEachIndexed { idx, seg ->
                if (idx == 0) append(seg) else append(seg.replaceFirstChar { it.uppercase() })
            }
        }
    }
}
