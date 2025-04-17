package com.wire.bots.polls.services

import com.wire.bots.polls.dto.roman.ConversationInformation
import com.wire.bots.polls.utils.appendPath
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import mu.KLogging

/**
 * Provides possibility to check the conversation details.
 */
class ConversationService(
    private val client: HttpClient,
    config: ProxyConfiguration
) {
    private companion object : KLogging() {
        const val conversationPath = "/conversation"
    }

    private val endpoint = config.baseUrl appendPath conversationPath

    /**
     * Returns the number of members of conversation.
     */
    suspend fun getNumberOfConversationMembers(token: String): Int? =
        runCatching {
            client.get<ConversationInformation> {
                url(endpoint)
                header("Authorization", "Bearer $token")
            }
        }.onFailure {
            logger.error(it) { "It was not possible to fetch conversation information!" }
        }.onSuccess {
            logger.debug { "Successfully got conversation information." }
        }.getOrNull()
            ?.members
            ?.filter { it.service == null }
            ?.size
}
