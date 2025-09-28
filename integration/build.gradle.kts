plugins {
    kotlin("jvm")
    application
}

repositories { 
    mavenCentral()
    // JitPack repository for GitHub-hosted libraries
    maven { url = uri("https://jitpack.io") }
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    val main by getting {
        kotlin.srcDirs("src/commonMain/kotlin", "src/jvmMain/kotlin")
        resources.srcDirs("src/commonMain/resources", "src/jvmMain/resources")
    }
    val test by getting {
        kotlin.srcDirs("src/commonTest/kotlin", "src/jvmTest/kotlin")
        resources.srcDirs("src/commonTest/resources", "src/jvmTest/resources")
    }
}

dependencies {
    implementation(project(":nn-package"))
    implementation(project(":chess-engine"))
    implementation(project(":rl-framework"))
    
    // Chess library for alternative engine implementation
    implementation("com.github.bhlangonijr:chesslib:1.3.3")
    
    // DL4J dependencies for neural network backend
    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-beta7")
    implementation("org.nd4j:nd4j-native-platform:1.0.0-beta7")
    
    // RL4J dependencies (guarded by Gradle property)
    if (project.hasProperty("enableRL4J") && project.property("enableRL4J") == "true") {
        implementation("org.deeplearning4j:rl4j-core:1.0.0-beta7")
        implementation("org.deeplearning4j:rl4j-api:1.0.0-beta7")
    }

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.chessrl.integration.ChessRLCLI")
}

// Simple CLI runner task for consistency with docs
tasks.register<JavaExec>("runCli") {
    group = "application"
    description = "Runs the Chess RL CLI"
    mainClass.set("com.chessrl.integration.ChessRLCLI")
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    val raw = System.getProperty("args")
    if (raw != null) {
        val cliArgs: List<String> = raw.split(" ").filter { it.isNotBlank() }
        args(cliArgs)
    }
}
