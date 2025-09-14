package com.chessrl.nn

actual fun writeTextFile(path: String, content: String) {
    throw RuntimeException("Model save is not implemented on Native platform for path: $path")
}

actual fun readTextFile(path: String): String {
    throw RuntimeException("Model load is not implemented on Native platform for path: $path")
}

