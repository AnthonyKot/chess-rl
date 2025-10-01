package com.chessrl.integration

/**
 * Centralized defaults for chess environment adjudication.
 * Keep these stable to maintain comparable results across runs and CI.
 * Promote to configuration only if you need to sweep per-profile.
 */
object EnvironmentDefaults {
    const val ENABLE_EARLY_ADJUDICATION: Boolean = true
    const val RESIGN_MATERIAL_THRESHOLD: Int = 9
    const val NO_PROGRESS_PLIES: Int = 40
}

