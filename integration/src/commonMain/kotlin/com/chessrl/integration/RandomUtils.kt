package com.chessrl.integration

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Gaussian (normal) random using Box–Muller transform for common code.
 * Kotlin's Random does not provide nextGaussian(), so we add it here.
 */
fun Random.nextGaussian(): Double {
    // Generate two independent uniform(0,1] variables
    var u1: Double
    var u2: Double
    do {
        u1 = this.nextDouble()
    } while (u1 <= 1e-12)
    u2 = this.nextDouble()
    // Box–Muller transform
    return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
}

// Note: call `Random.Default.nextGaussian()` or use an instance extension.
