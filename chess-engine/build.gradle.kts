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
            // Use the full JVM runtime classpath to ensure all dependencies (including subprojects) are present
            // Qualify with 'project' to avoid receiver clash inside the compilation scope
            val runtimeCp = project.configurations.getByName("jvmRuntimeClasspath") + output.allOutputs

            tasks.register("runDemo", JavaExec::class) {
                dependsOn("jvmMainClasses")
                classpath = runtimeCp
                mainClass.set("com.chessrl.chess.DemoKt")
                group = "application"
                description = "Run the chess engine demo"
            }
            tasks.register("runVisualizationDemo", JavaExec::class) {
                dependsOn("jvmMainClasses")
                classpath = runtimeCp
                mainClass.set("com.chessrl.chess.GameVisualizationDemoKt")
                group = "application"
                description = "Run the game visualization and replay tools demo"
            }
            tasks.register("runCli", JavaExec::class) {
                dependsOn("jvmMainClasses")
                classpath = runtimeCp
                mainClass.set("com.chessrl.chess.InteractiveCliKt")
                group = "application"
                description = "Run the interactive chess engine CLI"
                // Forward stdin so readLine() works interactively when running via Gradle
                standardInput = System.`in`
                val raw = System.getProperty("args")
                if (raw != null) {
                    val cliArgs: List<String> = raw.split(" ").filter { it.isNotBlank() }
                    args(cliArgs)
                }
            }
            tasks.register("runTeacherCollector", JavaExec::class) {
                dependsOn("jvmMainClasses")
                classpath = runtimeCp
                mainClass.set("com.chessrl.teacher.TeacherDataCollectorKt")
                group = "application"
                description = "Run the teacher data collector (NDJSON output)"
                val raw = System.getProperty("args")
                if (raw != null) {
                    val cliArgs: List<String> = raw.split(" ").filter { it.isNotBlank() }
                    args(cliArgs)
                }
            }
            tasks.register("runImitationTrainer", JavaExec::class) {
                dependsOn("jvmMainClasses")
                classpath = runtimeCp
                mainClass.set("com.chessrl.teacher.ImitationTrainerKt")
                group = "application"
                description = "Run imitation pretraining on a NDJSON dataset"
                val raw = System.getProperty("args")
                if (raw != null) {
                    val cliArgs: List<String> = raw.split(" ").filter { it.isNotBlank() }
                    args(cliArgs)
                }
            }
            tasks.register("runDiversityReport", JavaExec::class) {
                dependsOn("jvmMainClasses")
                classpath = runtimeCp
                mainClass.set("com.chessrl.teacher.DiversityReportKt")
                group = "application"
                description = "Analyze NDJSON teacher dataset diversity metrics"
                val raw = System.getProperty("args")
                if (raw != null) {
                    val cliArgs: List<String> = raw.split(" ").filter { it.isNotBlank() }
                    args(cliArgs)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":nn-package"))
            }
        }
        val commonTest by getting {
            dependencies { implementation(kotlin("test")) }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project(":nn-package"))
            }
        }
        val jvmTest by getting
    }
}

// Ensure consistent Java toolchain for Kotlin/JVM compilations
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
