package com.chessrl.nn

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

actual fun writeTextFile(path: String, content: String) {
    val p = Path.of(path)
    Files.createDirectories(p.parent ?: Path.of("."))
    Files.write(p, content.toByteArray(StandardCharsets.UTF_8))
}

actual fun readTextFile(path: String): String {
    val p = Path.of(path)
    return Files.readString(p, StandardCharsets.UTF_8)
}

