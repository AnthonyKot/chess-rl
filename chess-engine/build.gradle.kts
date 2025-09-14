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
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        compilations.getByName("main") {
            tasks.register("runDemo", JavaExec::class) {
                dependsOn("jvmMainClasses")
                classpath = runtimeDependencyFiles + output.allOutputs
                mainClass.set("com.chessrl.chess.DemoKt")
                group = "application"
                description = "Run the chess engine demo"
            }
            tasks.register("runVisualizationDemo", JavaExec::class) {
                dependsOn("jvmMainClasses")
                classpath = runtimeDependencyFiles + output.allOutputs
                mainClass.set("com.chessrl.chess.GameVisualizationDemoKt")
                group = "application"
                description = "Run the game visualization and replay tools demo"
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies { implementation(kotlin("test")) }
        }
        val jvmMain by getting
        val jvmTest by getting
    }
}

// Ensure consistent Java toolchain for Kotlin/JVM compilations
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
