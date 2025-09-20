package com.chessrl.integration

import java.nio.file.Files
import java.nio.file.Path

data class ProfileConfig(val name: String, val entries: Map<String, String>)

object ProfilesLoader {
    fun loadProfiles(paths: List<String>): Map<String, Map<String, String>> {
        for (p in paths) {
            val path = Path.of(p)
            if (Files.exists(path)) {
                return parseYaml(Files.readAllLines(path))
            }
        }
        return emptyMap()
    }

    // Very small YAML subset: expects
    // profiles:
    //   profile_name:
    //     key: value
    private fun parseYaml(lines: List<String>): Map<String, Map<String, String>> {
        val result = mutableMapOf<String, MutableMap<String, String>>()
        var inProfiles = false
        var current: MutableMap<String, String>? = null
        for (raw in lines) {
            val line = raw.trimEnd()
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            if (!inProfiles) {
                if (line.trim() == "profiles:") inProfiles = true
                continue
            }
            val indent = raw.indexOfFirst { !it.isWhitespace() }
            if (indent == 2 && line.endsWith(":")) {
                // profile name
                val name = line.removeSuffix(":").trim()
                current = mutableMapOf()
                result[name] = current
            } else if (indent >= 4 && ":" in line && current != null) {
                val idx = line.indexOf(":")
                val key = line.substring(0, idx).trim()
                var value = line.substring(idx + 1).trim()
                
                // Remove inline comments
                val commentIdx = value.indexOf("#")
                if (commentIdx >= 0) {
                    value = value.substring(0, commentIdx).trim()
                }
                
                value = value.trim('"')
                current[key] = value
            }
        }
        return result
    }
}
