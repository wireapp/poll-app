import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.1.20"
    application
    distribution
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("net.nemerosa.versioning") version "3.1.0"
}

group = "com.wire.apps.polls"
version = versioning.info?.tag ?: versioning.info?.lastTag ?: "development"

val mClass = "com.wire.apps.polls.PollAppKt"

application {
    mainClass.set(mClass)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.wire", "wire-apps-jvm-sdk", "0.0.5")
    // stdlib
    implementation(kotlin("stdlib-jdk8"))
    // extension functions
    implementation("pw.forst", "katlib", "2.0.3")

    // Ktor server dependencies
    val ktorVersion = "1.6.3"
    implementation("io.ktor", "ktor-server-core", ktorVersion)
    implementation("io.ktor", "ktor-server-netty", ktorVersion)
    implementation("io.ktor", "ktor-jackson", ktorVersion)
    implementation("io.ktor", "ktor-websockets", ktorVersion)
    // explicitly set the reflect library to same version as the kotlin
    implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.5.0")
    // Ktor client dependencies
    implementation("io.ktor", "ktor-client-json", ktorVersion)
    implementation("io.ktor", "ktor-client-jackson", ktorVersion) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
    implementation("io.ktor", "ktor-client-apache", ktorVersion)
    implementation("io.ktor", "ktor-client-logging-jvm", ktorVersion)

    // Prometheus metrics
    implementation("io.ktor", "ktor-metrics-micrometer", ktorVersion)
    implementation("io.micrometer", "micrometer-registry-prometheus", "1.6.6")

    // logging
    implementation("io.github.microutils", "kotlin-logging", "2.0.6")
    // if-else in logback.xml
    implementation("org.codehaus.janino", "janino", "3.1.2")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")

    // DI
    val kodeinVersion = "7.5.0"
    implementation("org.kodein.di", "kodein-di-jvm", kodeinVersion)
    implementation("org.kodein.di", "kodein-di-framework-ktor-server-jvm", kodeinVersion)

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

tasks.distTar {
    archiveFileName.set("app.tar")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
    manifest {
        attributes["Main-Class"] = mClass
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("polls.jar")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    from(sourceSets.main.get().output)
}

tasks.register("resolveDependencies") {
    doLast {
        buildscript.configurations.forEach { if (it.isCanBeResolved) it.resolve() }
        configurations.compileClasspath.get().resolve()
        configurations.testCompileClasspath.get().resolve()
    }
}
