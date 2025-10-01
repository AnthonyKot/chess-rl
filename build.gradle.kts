plugins {
    kotlin("jvm") version "1.9.20"
}

group = "com.chessrl"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin { jvmToolchain(21) }

sourceSets {
    val main by getting
    val test by getting
}

// Root project acts as aggregator; no runnable application entrypoint

tasks.withType<Test> {
    useJUnitPlatform()
}
