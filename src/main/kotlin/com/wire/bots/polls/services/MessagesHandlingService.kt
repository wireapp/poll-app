package com.wire.bots.polls.services

import com.wire.bots.polls.dto.PollAction
import com.wire.bots.polls.dto.UsersInput
import com.wire.integrations.jvm.model.WireMessage
import io.ktor.features.BadRequestException
import mu.KLogging

class MessagesHandlingService(
    private val pollService: PollService,
    private val userCommunicationService: UserCommunicationService
) {
    private companion object : KLogging()

    suspend fun handle(message: Message) {
        logger.debug { "Handling message." }
        logger.trace { "Message: $message" }

        val handled = when (message.type) {
            "conversation.bot_request" -> false.also {
                logger.debug { "Bot was added to conversation." }
            }
            "conversation.bot_removed" -> false.also {
                logger.debug { "Bot was removed from the conversation." }
            }
            else -> {
                logger.debug { "Handling type: ${message.type}" }
                if (message.token != null) {
                    tokenAwareHandle(message.token, message)
                } else {
                    logger.warn {
                        "Proxy didn't send token along side the message " +
                            "with type ${message.type}. Message:$message"
                    }
                    false
                }
            }
        }

        logger.debug {
            if (handled) "Bot reacted to the message" else "Bot didn't react to the message."
        }
        logger.debug { "Message handled." }
    }

    suspend fun handleText(
        manager: WireApplicationManager,
        message: WireMessage.Text
    ): Boolean {
        var handled = true

        fun ignore(reason: () -> String) {
            logger.debug(reason)
            handled = false
        }

        with(message) {
            if (sender == null) {
                throw BadRequestException("UserId must be set for text messages.")
            } else if (text != null) {
                val trimmed = (text as String).trim()
                when {
                    // poll request
                    trimmed.startsWith("/poll") ->
                        pollService.createPoll(
                            manager,
                            message.conversationId,
                            UsersInput(
                                sender,
                                trimmed,
                                mentions
                            )
                        )
                    // stats request
                    trimmed.startsWith("/stats") -> {
                        pollService.sendStatsForLatest(manager, conversationId)
                    }
                    // send version when asked
                    trimmed.startsWith("/version") -> {
                        userCommunicationService.sendVersion(manager, conversationId)
                    }
                    // send version when asked
                    trimmed.startsWith("/help") -> {
                        userCommunicationService.sendHelp(manager, conversationId)
                    }
                    // Easter egg, good app is good
                    trimmed == "good app" -> {
                        userCommunicationService.goodApp(manager, conversationId)
                    }
//                    else -> ignore { "Ignoring the message, unrecognized command." }
                    else -> null
                }
            } else {
                ignore { "Ignoring message as it does not have correct fields set." }
            }
        }
        return handled
    }
}
