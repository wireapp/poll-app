package com.wire.bots.polls.services

import com.wire.integrations.jvm.model.WireMessage
import com.wire.integrations.jvm.service.WireApplicationManager
import mu.KLogging
import pw.forst.katlib.createJson

/**
 * Service responsible for sending requests to the proxy service Roman.
 */
class ProxySenderService {
    private companion object : KLogging()

    /**
     * Send given message with provided token.
     */
    suspend fun send(
        manager: WireApplicationManager,
        message: WireMessage
    ) {
        logger.debug { "Sending: ${createJson(message)}" }

        return client
            .post<HttpStatement>(body = message) {
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
                        logger.error {
                            "Error in communication with proxy. Status: ${it.status}, body: $body."
                        }
                        null
                    }
                }
            }
    }
}

/**
 * Configuration used to connect to the proxy.
 */
data class ProxyConfiguration(
    val baseUrl: String
)
        val conversationId = when (message) {
            is WireMessage.Composite -> message.textContent?.conversationId
            else -> null
        }
        manager.sendMessageSuspending(
            conversationId = conversationId!!,
            message = message
        )
    }
}
