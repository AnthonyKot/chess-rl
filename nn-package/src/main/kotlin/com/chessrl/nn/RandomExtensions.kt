package com.chessrl.nn

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Multiplatform-compatible Gaussian sampler using Box-Muller transform.
 */
fun Random.nextGaussian(): Double {
    // Ensure u1 is in (0,1] to avoid ln(0)
    var u1 = this.nextDouble()
    var u2 = this.nextDouble()
    if (u1 <= 1e-12) u1 = 1e-12
    // Box-Muller transform
    return sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
}

