package com.chessrl.integration

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun writeTextFile(path: String, content: String) {
    val p = Path.of(path)
    Files.createDirectories(p.parent ?: Path.of("."))
    Files.write(p, content.toByteArray(StandardCharsets.UTF_8))
}

fun appendTextFile(path: String, content: String) {
    val p = Path.of(path)
    Files.createDirectories(p.parent ?: Path.of("."))
    Files.write(
        p,
        content.toByteArray(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE, StandardOpenOption.APPEND
    )
}
