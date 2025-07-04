import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.1.21"
    application
    id("com.gradleup.shadow") version "9.0.0-beta13"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
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
    implementation("com.wire", "wire-apps-jvm-sdk", "0.0.12")
    // stdlib
    implementation(kotlin("stdlib-jdk8"))
    // extension functions
    implementation("pw.forst", "katlib", "2.0.3")

    // Ktor server dependencies
    val ktorVersion = "1.6.3"
    implementation("io.ktor", "ktor-server-netty", ktorVersion)
    implementation("io.ktor", "ktor-jackson", ktorVersion)

    // Prometheus metrics
    implementation("io.ktor", "ktor-metrics-micrometer", ktorVersion)
    implementation("io.micrometer", "micrometer-registry-prometheus", "1.6.6")

    // logging
    implementation("net.logstash.logback", "logstash-logback-encoder", "8.1")
    implementation("io.github.microutils", "kotlin-logging", "2.0.6")
    // if-else in logback.xml
    implementation("org.codehaus.janino", "janino", "3.1.2")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")

    // DI
    implementation("org.kodein.di", "kodein-di-framework-ktor-server-jvm", "7.5.0")

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
    testImplementation("io.mockk", "mockk", "1.13.3")
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
    jvmToolchain(17)
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        mergeServiceFiles()
        archiveBaseName = "poll-app"
    }
    build {
        dependsOn(shadowJar)
    }
}
