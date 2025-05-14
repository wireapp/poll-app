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
     * Token which is used for the auth of proxy.
     */
    const val SERVICE_TOKEN = "SERVICE_TOKEN"

    /**
     * Domain used for sending the messages from the app to proxy eg. "https://proxy.services.zinfra.io/api"
     */
    const val PROXY_DOMAIN = "PROXY_DOMAIN"
}
