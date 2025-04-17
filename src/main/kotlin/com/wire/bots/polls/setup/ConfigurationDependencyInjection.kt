package com.wire.bots.polls.setup

import com.wire.bots.polls.dto.conf.DatabaseConfiguration
import com.wire.bots.polls.services.ProxyConfiguration
import com.wire.bots.polls.setup.EnvConfigVariables.DB_PASSWORD
import com.wire.bots.polls.setup.EnvConfigVariables.DB_URL
import com.wire.bots.polls.setup.EnvConfigVariables.DB_USER
import com.wire.bots.polls.setup.EnvConfigVariables.PROXY_DOMAIN
import com.wire.bots.polls.setup.EnvConfigVariables.SERVICE_TOKEN
import com.wire.bots.polls.utils.createLogger
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

/**
 * Loads the DI container with configuration from the system environment.
 */
// TODO load all config from the file and then allow the replacement with env variables
fun DI.MainBuilder.bindConfiguration() {
    // The default values used in this configuration are for the local development.

    bind<DatabaseConfiguration>() with singleton {
        DatabaseConfiguration(
            userName = getEnvOrLogDefault(DB_USER, "wire-poll-bot"),
            password = getEnvOrLogDefault(DB_PASSWORD, "super-secret-wire-pwd"),
            url = getEnvOrLogDefault(DB_URL, "jdbc:postgresql://localhost:5432/poll-bot")
        )
    }

    bind<String>("proxy-auth") with singleton {
        getEnvOrLogDefault(SERVICE_TOKEN, "local-token")
    }

    bind<String>("version") with singleton {
        loadVersion("development")
    }

    bind<ProxyConfiguration>() with singleton {
        ProxyConfiguration(getEnvOrLogDefault(PROXY_DOMAIN, "http://proxy.services.zinfra.io"))
    }
}
