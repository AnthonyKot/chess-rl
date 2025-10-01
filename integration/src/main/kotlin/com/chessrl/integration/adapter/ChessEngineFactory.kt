package com.chessrl.integration.adapter

/**
 * Supported engine backends for the pluggable adapter system.
 */
enum class EngineBackend {
    /** Built-in engine implemented in the project */
    BUILTIN,

    /** External chesslib engine */
    CHESSLIB;

    companion object {
        fun fromString(value: String): EngineBackend {
            return when (value.lowercase()) {
                "builtin" -> BUILTIN
                "chesslib" -> CHESSLIB
                else -> throw IllegalArgumentException("Unknown engine backend: $value")
            }
        }
    }
}

/**
 * Factory for creating chess engine adapters based on selected backend.
 */
object ChessEngineFactory {
    fun create(engine: EngineBackend): ChessEngineAdapter {
        return when (engine) {
            EngineBackend.BUILTIN -> BuiltinAdapter()
            EngineBackend.CHESSLIB -> ChesslibAdapter()
        }
    }
}
