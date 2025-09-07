package com.chessrl.nn

import kotlinx.cinterop.*
import platform.posix.*

/**
 * Native implementation of time function
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getCurrentTimeMillis(): Long {
    memScoped {
        val timeVal = alloc<timeval>()
        gettimeofday(timeVal.ptr, null)
        return timeVal.tv_sec * 1000L + timeVal.tv_usec / 1000L
    }
}