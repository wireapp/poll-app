package com.wire.apps.polls.setup

/**
 * Contains variables that are loaded from the environment.
 */
object EnvConfigVariables {
    /**
     * Username for the database.
     */
    const val DB_USER = "DB_USER"

    /**
     * Password for the database.
     */
    const val DB_PASSWORD = "DB_PASSWORD"

    /**
     * URL for the database.
     *
     * Example:
     * `jdbc:postgresql://localhost:5432/app-database`
     */
    const val DB_URL = "DB_URL"

    /**
     * SDK
     */
    const val SDK_APP_ID = "SDK_APP_ID"
    const val SDK_APP_TOKEN = "SDK_APP_TOKEN"
    const val API_HOST_URL = "API_HOST_URL"
    const val CRYPTO_PASSWORD = "CRYPTO_PASSWORD"
}
