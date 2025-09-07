plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    // JVM target for testing
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
        }
    }
    
    // Native target for the current platform
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Chess engine specific dependencies
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}