import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.3.0"
    application
    id("com.gradleup.shadow") version "9.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("net.nemerosa.versioning") version "3.1.0"
}

group = "com.wire.apps.polls"
version = versioning.info?.tag ?: versioning.info?.lastTag ?: "development"

application {
    mainClass.set("com.wire.apps.polls.PollAppKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.wire", "wire-apps-jvm-sdk", "0.0.18")
    // stdlib
    implementation(kotlin("stdlib-jdk8"))
    // extension functions
    implementation("pw.forst", "katlib", "2.0.3")

    // Ktor server dependencies
    val ktorVersion = "3.2.3"
    implementation("io.ktor", "ktor-server-core", ktorVersion)
    implementation("io.ktor", "ktor-server-netty", ktorVersion)

    // logging
    implementation("net.logstash.logback", "logstash-logback-encoder", "8.1")
    implementation("ch.qos.logback", "logback-classic", "1.5.19")
    implementation("io.github.microutils", "kotlin-logging", "2.0.6")

    // metrics
    implementation("io.ktor", "ktor-server-metrics-micrometer", ktorVersion)
    implementation("io.micrometer", "micrometer-registry-prometheus", "1.16.0")

    // DI
    implementation("org.kodein.di", "kodein-di-framework-ktor-server-jvm", "7.28.0")

    // database
    implementation("org.postgresql", "postgresql", "42.2.20")

    val exposedVersion = "0.33.1"
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)
    implementation("pw.forst", "exposed-upsert", "1.1.0")

    // database migrations from the code
    implementation("org.flywaydb", "flyway-core", "7.8.2")

    // testing
    testImplementation(kotlin("test"))
    testImplementation("io.kotest", "kotest-assertions-core", "5.8.1")
    testImplementation("io.mockk", "mockk", "1.13.16")
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", "1.7.3")
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.10.0")
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.JSON)
        reporter(ReporterType.HTML)
    }
}

detekt {
    toolVersion = "1.23.7"
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
    buildUponDefaultConfig = true
    source.setFrom("src/main/kotlin")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        archiveBaseName = "poll-app"
    }
    build {
        dependsOn(shadowJar)
    }
}
