plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    // JVM target only
    jvm {
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
        val jvmMain by getting
        val jvmTest by getting
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
