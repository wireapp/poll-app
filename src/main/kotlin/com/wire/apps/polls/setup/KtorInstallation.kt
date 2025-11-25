package com.wire.apps.polls.setup

import com.wire.apps.polls.dao.DatabaseSetup
import com.wire.apps.polls.routing.events
import com.wire.apps.polls.setup.conf.DatabaseConfiguration
import com.wire.apps.polls.utils.createLogger
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.flywaydb.core.Flyway
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI

private val installationLogger = createLogger("ApplicationSetup")

/**
 * Loads the application.
 */
fun Application.init() {
    setupKodein()
    // now kodein is running and can be used
    installationLogger.debug { "DI container started." }

    connectDatabase()
    setupHealthEndpoint()
    setupEventRouting()
    setupMetrics()
}

fun Application.setupHealthEndpoint() {
    routing {
        get("/health") {
            call.response.status(HttpStatusCode.OK)
        }
    }
}

fun Application.setupEventRouting() {
    routing {
        events()
    }
}

fun Application.setupMetrics() {
    val prometheusRegistry by closestDI().instance<PrometheusMeterRegistry>()

    install(MicrometerMetrics) {
        registry = prometheusRegistry
    }

    routing {
        get("/metrics") {
            call.respond(prometheusRegistry.scrape())
        }
    }
}

/**
 * Connect app to the database.
 */
fun Application.connectDatabase() {
    installationLogger.info { "Connecting to the DB" }
    val dbConfig by closestDI().instance<DatabaseConfiguration>()
    DatabaseSetup.connect(dbConfig)

    if (DatabaseSetup.isConnected()) {
        installationLogger.info { "DB connected." }
        migrateDatabase(dbConfig)
    } else {
        // TODO verify handling, maybe exit the App?
        installationLogger.error {
            "It was not possible to connect to db database! " +
                "The application will start but it won't work."
        }
    }
}

/**
 * Migrate database using flyway.
 */
fun migrateDatabase(dbConfig: DatabaseConfiguration) {
    installationLogger.info { "Migrating database." }
    val migrateResult = Flyway
        .configure()
        .dataSource(dbConfig.url, dbConfig.userName, dbConfig.password)
        .load()
        .migrate()

    installationLogger.info {
        if (migrateResult.migrationsExecuted == 0) {
            "No migrations necessary."
        } else {
            "Applied ${migrateResult.migrationsExecuted} migrations."
        }
    }
}
