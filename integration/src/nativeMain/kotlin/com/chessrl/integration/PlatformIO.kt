package com.chessrl.integration

actual fun writeTextFile(path: String, content: String) {
    throw RuntimeException("File I/O not implemented on Native for integration: $path")
}

actual fun appendTextFile(path: String, content: String) {
    throw RuntimeException("File I/O not implemented on Native for integration: $path")
}

