package com.wire.bots.polls.services

import com.wire.bots.polls.dto.UsersInput
import com.wire.integrations.jvm.model.WireMessage
import com.wire.integrations.jvm.service.WireApplicationManager
import io.ktor.features.BadRequestException
import mu.KLogging

class MessagesHandlingService(
    private val pollService: PollService,
    private val userCommunicationService: UserCommunicationService
) {
    private companion object : KLogging()

//    suspend fun handle(message: WireMessage.Text) {
//        logger.debug { "Handling message." }
//        logger.trace { "Message: $message" }
//
//        logger.debug { "Handling type: ${message.type}" }
//        val handled = tokenAwareHandle(message)
//
//        logger.debug {
//            if (handled) "Bot reacted to the message" else "Bot didn't react to the message."
//        }
//        logger.debug { "Message handled." }
//    }

//    private suspend fun tokenAwareHandle(message: WireMessage.Text): Boolean {
//        logger.debug { "Message contains token." }
//        return runCatching {
//            when (message.type) {
//                "conversation.new_text" -> {
//                    logger.debug { "New text message received." }
//                    handleText(message)
//                }
//                "conversation.poll.action" -> {
//                    val poll =
//                        requireNotNull(
//                            message.poll
//                        ) { "Reaction to a poll, poll object must be set!" }
//                    pollService.pollAction(
//                        PollAction(
//                            pollId = poll.id,
//                            optionId = requireNotNull(
//                                poll.offset
//                            ) { "Offset/Option id must be set!" },
//                            userId = requireNotNull(
//                                message.userId
//                            ) { "UserId of user who sent the message must be set." }
//                        ),
//                        message.conversationId
//                    )
//                    true
//                }
//                else -> false.also {
//                    logger.warn { "Unknown message type of ${message.type}. Ignoring." }
//                }
//            }
//        }.onFailure {
//            logger.error(
//                it
//            ) { "Exception during handling the message: $message with token $token." }
//        }.getOrThrow()
//    }

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
