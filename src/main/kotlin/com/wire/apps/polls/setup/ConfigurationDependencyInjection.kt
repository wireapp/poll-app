package com.wire.apps.polls.setup

import com.wire.apps.polls.setup.conf.DatabaseConfiguration
import com.wire.apps.polls.setup.conf.SDKConfiguration
import com.wire.apps.polls.setup.EnvConfigVariables.API_HOST_URL
import com.wire.apps.polls.setup.EnvConfigVariables.CRYPTO_PASSWORD
import com.wire.apps.polls.setup.EnvConfigVariables.DB_PASSWORD
import com.wire.apps.polls.setup.EnvConfigVariables.DB_URL
import com.wire.apps.polls.setup.EnvConfigVariables.DB_USER
import com.wire.apps.polls.setup.EnvConfigVariables.SDK_APP_ID
import com.wire.apps.polls.setup.EnvConfigVariables.SDK_APP_TOKEN
import com.wire.apps.polls.utils.createLogger
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import pw.forst.katlib.getEnv
import pw.forst.katlib.whenNull
import java.io.File
import java.util.UUID

private val logger = createLogger("EnvironmentLoaderLogger")

private fun getEnvOrLogDefault(
    env: String,
    defaultValue: String
) = getEnv(env).whenNull {
    logger.warn { "Env variable $env not set! Using default value - $defaultValue" }
} ?: defaultValue

private fun getEnvOrThrow(env: String): String {
    val value = getEnv(env)
    require(value != null) { "Env variable $env is required but not set" }
    return value
}

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

    bind<SDKConfiguration>() with singleton {
        SDKConfiguration(
            appId = UUID.fromString(getEnvOrThrow(SDK_APP_ID)),
            appToken = getEnvOrThrow(SDK_APP_TOKEN),
            apiHostUrl = getEnvOrLogDefault(API_HOST_URL, "https://prod-nginz-https.wire.com"),
            cryptoPassword = getEnvOrThrow(CRYPTO_PASSWORD)
        )
    }

    bind<String>("version") with singleton {
        loadVersion("development")
    }
}
