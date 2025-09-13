package com.chessrl.integration

/**
 * Shared utility functions for the training pipeline
 */

/**
 * String repeat operator for formatting
 */
operator fun String.times(n: Int): String = this.repeat(n)

// Using platform-specific getCurrentTimeMillis from PlatformTime.kt