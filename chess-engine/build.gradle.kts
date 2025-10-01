plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":nn-package"))
    implementation("com.github.bhlangonijr:chesslib:1.3.3")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

val mainSourceSet = sourceSets.named("main")

fun registerCliTask(name: String, mainClassName: String, descriptionText: String) {
    tasks.register<JavaExec>(name) {
        group = "application"
        description = descriptionText
        dependsOn(tasks.named("classes"))
        mainClass.set(mainClassName)
        classpath = mainSourceSet.get().runtimeClasspath
        System.getProperty("args")
            ?.split(" ")
            ?.filter { it.isNotBlank() }
            ?.let { args(it) }
    }
}

registerCliTask(
    name = "runTeacherCollector",
    mainClassName = "com.chessrl.teacher.TeacherDataCollectorKt",
    descriptionText = "Run the teacher data collector (NDJSON output)"
)

registerCliTask(
    name = "runImitationTrainer",
    mainClassName = "com.chessrl.teacher.ImitationTrainerKt",
    descriptionText = "Run imitation pretraining on a NDJSON dataset"
)

registerCliTask(
    name = "runDiversityReport",
    mainClassName = "com.chessrl.teacher.DiversityReportKt",
    descriptionText = "Analyze NDJSON teacher dataset diversity metrics"
)

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
