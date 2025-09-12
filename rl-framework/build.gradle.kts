plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    // Native target for the current platform
    val hostOs = System.getProperty("os.name")
    val hostArch = System.getProperty("os.arch")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && (hostArch == "aarch64" || hostArch == "arm64") -> macosArm64("native")
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Keep RL framework independent for now
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val nativeMain by getting
        val nativeTest by getting
    }
}
