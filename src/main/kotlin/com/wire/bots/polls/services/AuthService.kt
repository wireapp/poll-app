package com.wire.bots.polls.services

import io.ktor.http.Headers
import mu.KLogging
import pw.forst.katlib.whenNull

/**
 * Authentication service.
 */
class AuthService(
    private val proxyToken: String
) {
    private companion object : KLogging() {
        const val authHeader = "Authorization"
        const val bearerPrefix = "Bearer "
    }

    /**
     * Validates token.
     */
    fun isTokenValid(headersGet: () -> Headers) =
        runCatching { isTokenValid(headersGet()) }.getOrNull() ?: false

    private fun isTokenValid(headers: Headers): Boolean {
        val header = headers[authHeader].whenNull {
            logger.info { "Request did not have authorization header." }
        } ?: return false

        return if (!header.startsWith(bearerPrefix)) {
            false
        } else {
            header.substringAfter(bearerPrefix) == proxyToken
        }
    }
}
