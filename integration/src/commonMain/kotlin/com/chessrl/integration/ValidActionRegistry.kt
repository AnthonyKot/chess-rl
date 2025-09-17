package com.chessrl.integration

/**
 * Simple in-memory registry that maps a concrete DoubleArray state instance
 * to the list of valid action indices computed at that time.
 *
 * Notes:
 * - Uses reference identity semantics for DoubleArray keys (default in Kotlin).
 * - Best-effort cache to support correct next-state masking during replay.
 */
object ValidActionRegistry {
    private val map = mutableMapOf<DoubleArray, List<Int>>()

    fun put(state: DoubleArray, actions: List<Int>) {
        map[state] = actions
    }

    fun get(state: DoubleArray): List<Int>? = map[state]

    fun clear() = map.clear()
}

