package com.wire.bots.polls.services

import com.wire.bots.polls.dto.bot.BotMessage
import com.wire.bots.polls.dto.roman.Response
import com.wire.bots.polls.utils.appendPath
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.*
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import mu.KLogging
import pw.forst.katlib.createJson
import java.nio.charset.Charset

/**
 * Service responsible for sending requests to the proxy service Roman.
 */
class ProxySenderService(private val client: HttpClient, config: ProxyConfiguration) {

    private companion object : KLogging() {
        const val conversationPath = "/conversation"
    }

    private val conversationEndpoint = config.baseUrl appendPath conversationPath

    /**
     * Send given message with provided token.
     */
    suspend fun send(token: String, message: BotMessage): Response? {
        logger.debug { "Sending: ${createJson(message)}" }

        return client.post<HttpStatement>(body = message) {
            url(conversationEndpoint)
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
        }.execute {
            logger.debug { "Message sent." }
            when {
                it.status.isSuccess() -> {
                    it.receive<Response>().also {
                        logger.info { "Message sent successfully: message id: ${it.messageId}" }
                    }
                }
                else -> {
                    val body = it.readText(Charset.defaultCharset())
                    logger.error { "Error in communication with proxy. Status: ${it.status}, body: $body." }
                    null
                }
            }
        }
    }
}

/**
 * Configuration used to connect to the proxy.
 */
data class ProxyConfiguration(val baseUrl: String)
