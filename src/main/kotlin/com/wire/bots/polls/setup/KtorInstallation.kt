package com.wire.bots.polls.setup

import com.fasterxml.jackson.databind.SerializationFeature
import com.wire.bots.polls.dao.DatabaseSetup
import com.wire.bots.polls.dto.conf.DatabaseConfiguration
import com.wire.bots.polls.routing.registerRoutes
import com.wire.bots.polls.setup.errors.registerExceptionHandlers
import com.wire.bots.polls.setup.logging.APP_REQUEST
import com.wire.bots.polls.setup.logging.INFRA_REQUEST
import com.wire.bots.polls.utils.createLogger
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.CallLogging
import io.ktor.features.CallId
import io.ktor.features.callId
import io.ktor.jackson.jackson
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.request.header
import io.ktor.request.uri
import io.ktor.routing.routing
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.flywaydb.core.Flyway
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import org.slf4j.event.Level
import java.text.DateFormat
import java.util.UUID

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
 * Connect bot to the database.
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
            "It was not possible to connect to db database! The application will start but it won't work."
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

    install(CallLogging) {
        // insert nginx id to MDC
        mdc(INFRA_REQUEST) {
            it.request.header("X-Request-Id")
        }

        // use generated call id and insert it to MDC
        mdc(APP_REQUEST) {
            it.callId
        }

        // enable logging just for /messages
        // this filter does not influence MDC
        filter {
            it.request.uri == "/messages"
        }
        level = Level.DEBUG
        logger = createLogger("HttpCallLogger")
    }

    install(CallId) {
        generate {
            UUID.randomUUID().toString()
        }
    }

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
