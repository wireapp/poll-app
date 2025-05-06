package com.wire.apps.polls.setup

import com.wire.apps.polls.dto.conf.DatabaseConfiguration
import com.wire.apps.polls.setup.EnvConfigVariables.DB_PASSWORD
import com.wire.apps.polls.setup.EnvConfigVariables.DB_URL
import com.wire.apps.polls.setup.EnvConfigVariables.DB_USER
import com.wire.apps.polls.setup.EnvConfigVariables.SERVICE_TOKEN
import com.wire.apps.polls.utils.createLogger
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import pw.forst.katlib.getEnv
import pw.forst.katlib.whenNull
import java.io.File

private val logger = createLogger("EnvironmentLoaderLogger")

private fun getEnvOrLogDefault(
    env: String,
    defaultValue: String
) = getEnv(env).whenNull {
    logger.warn { "Env variable $env not set! Using default value - $defaultValue" }
} ?: defaultValue

@Suppress("SameParameterValue") // we don't care...
private fun loadVersion(defaultVersion: String): String = runCatching {
    getEnv("RELEASE_FILE_PATH")
        ?.let { File(it).readText().trim() }
        ?: defaultVersion
}.getOrNull() ?: defaultVersion

// TODO load all config from the file and then allow the replacement with env variables

/**
 * Loads the DI container with configuration from the system environment.
 */
fun DI.MainBuilder.bindConfiguration() {
    // The default values used in this configuration are for the local development.

    bind<DatabaseConfiguration>() with singleton {
        DatabaseConfiguration(
            userName = getEnvOrLogDefault(DB_USER, "wire-poll-app"),
            password = getEnvOrLogDefault(DB_PASSWORD, "super-secret-wire-pwd"),
            url = getEnvOrLogDefault(DB_URL, "jdbc:postgresql://localhost:5432/poll-app")
        )
    }

    bind<String>("proxy-auth") with singleton {
        getEnvOrLogDefault(SERVICE_TOKEN, "local-token")
    }

    bind<String>("version") with singleton {
        loadVersion("development")
    }
}
