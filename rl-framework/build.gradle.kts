plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    // JVM target only
    jvm {
        withJava()
        testRuns["test"].executionTask.configure { useJUnitPlatform() }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
        val jvmMain by getting
        val jvmTest by getting { dependencies { implementation("org.junit.jupiter:junit-jupiter:5.8.2") } }
    }
}
