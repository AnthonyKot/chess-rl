package com.chessrl.integration.logging

import com.chessrl.integration.getCurrentTimeMillis
import kotlin.jvm.Volatile

object Log {
    enum class Level { DEBUG, INFO, WARN, ERROR }

    @Volatile private var currentLevel: Level = Level.INFO
    @Volatile private var withTimestamp: Boolean = true

    fun setLevel(level: Level) { currentLevel = level }
    fun showTimestamp(enabled: Boolean) { withTimestamp = enabled }

    fun debug(message: String) { log(Level.DEBUG, message) }
    fun info(message: String) { log(Level.INFO, message) }
    fun warn(message: String) { log(Level.WARN, message) }
    fun error(message: String) { log(Level.ERROR, message) }

    private fun log(level: Level, message: String) {
        if (level.ordinal < currentLevel.ordinal) return
        val prefix = if (withTimestamp) "[${formatTime(getCurrentTimeMillis())}] [${level.name}]" else "[${level.name}]"
        println("$prefix $message")
    }

    private fun formatTime(ms: Long): String {
        val s = (ms / 1000) % 60
        val m = (ms / (1000 * 60)) % 60
        val h = (ms / (1000 * 60 * 60)) % 24
        val msPart = (ms % 1000)
        return String.format("%02d:%02d:%02d.%03d", h, m, s, msPart)
    }
}
