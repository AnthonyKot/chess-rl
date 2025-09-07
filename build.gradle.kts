plugins {
    kotlin("multiplatform") version "1.9.20"
    application
}

group = "com.chessrl"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    // Native target for the current platform
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Common dependencies
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by getting {
            dependencies {
                // Native-specific dependencies
            }
        }
        val nativeTest by getting {
            dependencies {
                // Native test dependencies
            }
        }
    }
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}