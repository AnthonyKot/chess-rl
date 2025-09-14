plugins {
    kotlin("jvm") version "1.9.20"
    application
}

group = "com.chessrl"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin { jvmToolchain(21) }

sourceSets {
    val main by getting
    val test by getting
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
