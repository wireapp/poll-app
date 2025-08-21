package com.wire.apps.polls.services

import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.PollOverviewDto
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.dto.newPoll
import com.wire.apps.polls.dto.textMessage
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import com.wire.integrations.jvm.service.WireApplicationManager
import mu.KLogging

/**
 * Service used for handling user commands.
 */
class UserCommunicationService(
    private val proxySenderService: ProxySenderService,
    private val version: String
) {
    private companion object : KLogging() {
        const val USAGE = "To create poll please text: " +
            "`/poll \"Question\" \"Option 1\" \"Option 2\"`. To display usage write `/poll help`"
        val commands = """
            Following commands are available:
            `/poll "Question" "Option 1" "Option 2"` will create poll
            `/poll stats` will send result of the last poll in the conversation
            `/poll help` to show help
        """.trimIndent()
    }

    /**
     * Introduce utility of an app to the user.
     */
    suspend fun sayHello(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, "Hello, I'm Poll App. $USAGE").send(manager)
    }

    /**
     * Fallback message in case of user providing invalid command.
     */
    suspend fun sendFallbackMessage(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        messageType: FallbackMessageType
    ) {
        val message = when (messageType) {
            FallbackMessageType.WRONG_COMMAND -> "I couldn't recognize your command. $USAGE"
            FallbackMessageType.MISSING_DATA -> "No data for poll. Please create a new one. $USAGE"
        }
        textMessage(conversationId, message).send(manager)
    }

    enum class FallbackMessageType {
        WRONG_COMMAND,
        MISSING_DATA
    }

    /**
     * Remind user of possible commands.
     */
    suspend fun sendHelp(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, commands).send(manager)
    }

    /**
     * Show gratitude to the app.
     */
    suspend fun goodApp(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, "\uD83D\uDE07").send(manager)
    }

    /**
     * Informs the user whether they are interacting with a development or deployed version.
     */
    suspend fun sendVersion(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, "My version is: *$version*").send(manager)
    }

    suspend fun sendPoll(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        poll: PollDto
    ): String {
        val message = newPoll(
            conversationId = conversationId,
            body = poll.question.data,
            buttons = poll.options,
            mentions = poll.question.mentions
        )
        message.send(manager)

        return message.id.toString()
    }

    suspend fun sendOrUpdatePollOverview(
        manager: WireApplicationManager,
        participationMessageId: String?,
        pollOverviewDto: PollOverviewDto,
        stats: Text?
    ): String {
        val wireMessage = if (participationMessageId == null) {
            logger.debug {
                "Sending initial Poll overview for conversation ${pollOverviewDto.conversationId}"
            }
            pollOverviewDto.createInitialMessage()
        } else {
            pollOverviewDto.update(
                overviewMessageId = participationMessageId,
                statsMessage = stats
            )
        }

        wireMessage.send(manager)

        return wireMessage.id.toString()
    }

    private suspend fun WireMessage.send(manager: WireApplicationManager) {
        proxySenderService.send(
            manager = manager,
            message = this
        )
    }
}
