package com.wire.apps.polls.services

import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.dto.confirmVote
import com.wire.apps.polls.dto.newPoll
import com.wire.apps.polls.dto.statsMessage
import com.wire.apps.polls.dto.textMessage
import com.wire.apps.polls.dto.updateStatsMessage
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
    suspend fun reactionToWrongCommand(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        message: String
    ) {
        textMessage(conversationId, "$message $USAGE").send(manager)
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

    suspend fun sendButtonConfirmation(
        manager: WireApplicationManager,
        pollAction: PollAction,
        conversationId: QualifiedId
    ) {
        confirmVote(
            pollId = pollAction.pollId,
            conversationId = conversationId,
            offset = pollAction.optionId
        ).send(manager)
    }

    suspend fun sendStats(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        text: Text
    ): String {
        val stats = statsMessage(text, conversationId)

        stats.send(manager)

        return stats.id.toString()
    }

    suspend fun sendUpdatedStats(
        manager: WireApplicationManager,
        conversationId: QualifiedId,
        text: Text,
        statsMessageId: String
    ): String {
        val updatedStats = updateStatsMessage(
            text = text,
            originalMessageId = statsMessageId,
            conversationId = conversationId
        )

        updatedStats.send(manager)

        return updatedStats.id.toString()
    }

    private suspend fun WireMessage.send(manager: WireApplicationManager) {
        proxySenderService.send(
            manager = manager,
            message = this
        )
    }
}
