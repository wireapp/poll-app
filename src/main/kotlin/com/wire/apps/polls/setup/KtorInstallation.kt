package com.wire.apps.polls.setup

import com.fasterxml.jackson.databind.SerializationFeature
import com.wire.apps.polls.dao.DatabaseSetup
import com.wire.apps.polls.setup.conf.DatabaseConfiguration
import com.wire.apps.polls.routing.registerRoutes
import com.wire.apps.polls.setup.errors.registerExceptionHandlers
import com.wire.apps.polls.utils.createLogger
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.routing
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.flywaydb.core.Flyway
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import java.text.DateFormat

private val installationLogger = createLogger("ApplicationSetup")

/**
 * Loads the application.
 */
fun Application.init() {
    setupKodein()
    // now kodein is running and can be used
    installationLogger.debug { "DI container started." }

    // connect to the database
    connectDatabase()

    // configure Ktor
    installFrameworks()

    // register routing
    routing {
        registerRoutes()
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

/**
 * Configure Ktor and install necessary extensions.
 */
fun Application.installFrameworks() {
    install(ContentNegotiation) {
        jackson {
            // enable pretty print for JSONs
            enable(SerializationFeature.INDENT_OUTPUT)
            dateFormat = DateFormat.getDateTimeInstance()
        }
    }

    install(DefaultHeaders)

    registerExceptionHandlers()

    val prometheusRegistry by closestDI().instance<PrometheusMeterRegistry>()
    install(MicrometerMetrics) {
        registry = prometheusRegistry
        distributionStatisticConfig = DistributionStatisticConfig
            .Builder()
            .percentilesHistogram(true)
            .build()
    }
}
