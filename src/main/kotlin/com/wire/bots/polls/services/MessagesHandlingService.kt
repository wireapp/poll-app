package com.wire.bots.polls.services

import com.wire.bots.polls.dto.PollAction
import com.wire.bots.polls.dto.UsersInput
import com.wire.bots.polls.dto.roman.Message
import io.ktor.features.*
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
            "conversation.bot_request" -> false.also { logger.debug { "Bot was added to conversation." } }
            "conversation.bot_removed" -> false.also { logger.debug { "Bot was removed from the conversation." } }
            else -> {
                logger.debug { "Handling type: ${message.type}" }
                when {
                    message.token != null -> tokenAwareHandle(message.token, message)
                    else -> false.also { logger.warn { "Proxy didn't send token along side the message with type ${message.type}. Message:$message" } }
                }
            }
        }

        logger.debug { if (handled) "Bot reacted to the message" else "Bot didn't react to the message." }
        logger.debug { "Message handled." }
    }

    private suspend fun tokenAwareHandle(token: String, message: Message): Boolean {
        logger.debug { "Message contains token." }
        return runCatching {
            when (message.type) {
                "conversation.init" -> {
                    logger.debug { "Init message received." }
                    userCommunicationService.sayHello(token)
                    true
                }
                "conversation.new_text" -> {
                    logger.debug { "New text message received." }
                    handleText(token, message)
                }
                "conversation.new_image" -> true.also { logger.debug { "New image posted to conversation, ignoring." } }
                "conversation.reaction" -> {
                    logger.debug { "Reaction message" }
                    pollService.sendStats(
                        token = token,
                        pollId = requireNotNull(message.refMessageId) { "Reaction must contain refMessageId" }
                    )
                    true
                }
                "conversation.poll.action" -> {
                    val poll = requireNotNull(message.poll) { "Reaction to a poll, poll object must be set!" }
                    pollService.pollAction(
                        token,
                        PollAction(
                            pollId = poll.id,
                            optionId = requireNotNull(poll.offset) { "Offset/Option id must be set!" },
                            userId = requireNotNull(message.userId) { "UserId of user who sent the message must be set." }
                        )
                    )
                    true
                }
                else -> false.also { logger.warn { "Unknown message type of ${message.type}. Ignoring." } }
            }
        }.onFailure {
            logger.error(it) { "Exception during handling the message: $message with token $token." }
        }.getOrThrow()
    }

    private suspend fun handleText(token: String, message: Message): Boolean {
        var handled = true

        fun ignore(reason: () -> String) {
            logger.debug(reason)
            handled = false
        }

        with(message) {
            when {
                userId == null -> throw BadRequestException("UserId must be set for text messages.")
                // it is a reply on something
                refMessageId != null && text != null -> when {
                    // request for stats
                    text.data.trim().startsWith("/stats") -> pollService.sendStats(token, refMessageId)
                    // integer vote where the text contains offset
                    text.data.trim().toIntOrNull() != null -> vote(token, userId, refMessageId, text.data)
                    else -> ignore { "Ignoring the message as it is reply unrelated to the bot" }
                }
                // text message with just text
                text != null -> {
                    val trimmed = text.data.trim()
                    when {
                        // poll request
                        trimmed.startsWith("/poll") ->
                            pollService.createPoll(token, UsersInput(userId, trimmed, text.mentions ?: emptyList()), botId)
                        // stats request
                        trimmed.startsWith("/stats") -> pollService.sendStatsForLatest(token, botId)
                        // send version when asked
                        trimmed.startsWith("/version") -> userCommunicationService.sendVersion(token)
                        // send version when asked
                        trimmed.startsWith("/help") -> userCommunicationService.sendHelp(token)
                        // easter egg, good bot is good
                        trimmed == "good bot" -> userCommunicationService.goodBot(token)
                        else -> ignore { "Ignoring the message, unrecognized command." }
                    }
                }
                else -> ignore { "Ignoring message as it does not have correct fields set." }
            }
        }
        return handled
    }

    private suspend fun vote(token: String, userId: String, refMessageId: String, text: String) = pollService.pollAction(
        token,
        PollAction(
            pollId = refMessageId,
            optionId = requireNotNull(text.toIntOrNull()) { "Text message must be a valid integer." },
            userId = userId
        )
    )
}
