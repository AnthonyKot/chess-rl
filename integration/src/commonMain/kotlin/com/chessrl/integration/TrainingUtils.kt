package com.chessrl.integration

/**
 * Shared utility functions for the training pipeline
 */

/**
 * String repeat operator for formatting
 */
operator fun String.times(n: Int): String = this.repeat(n)

/**
 * Get current time in milliseconds (simplified for Kotlin/Native)
 */
fun getCurrentTimeMillis(): Long {
    // Simplified time function for Kotlin/Native compatibility
    return 0L // In practice, would use platform-specific time
}