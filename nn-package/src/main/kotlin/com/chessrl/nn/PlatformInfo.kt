package com.chessrl.nn

fun platformRuntimeInfo(): String {
    val javaVersion = System.getProperty("java.version") ?: "unknown"
    val javaVendor = System.getProperty("java.vendor") ?: "unknown"
    val jvmName = System.getProperty("java.vm.name") ?: "unknown"
    val osName = System.getProperty("os.name") ?: "unknown"
    val osArch = System.getProperty("os.arch") ?: "unknown"
    return "JDK $javaVersion ($javaVendor, $jvmName) on $osName/$osArch"
}

fun platformHardwareInfo(): String {
    val cores = Runtime.getRuntime().availableProcessors()
    val memMb = (Runtime.getRuntime().maxMemory() / (1024 * 1024)).toString() + "MB max"
    return "cores=$cores, heap_max=$memMb"
}
