package com.wire.apps.polls.services

import com.wire.apps.polls.dto.app.textMessage
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
    suspend fun reactionToWrongCommand(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, "I couldn't recognize your command. $USAGE").send(manager)
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

    private suspend fun WireMessage.Text.send(manager: WireApplicationManager) {
        proxySenderService.send(manager, this, conversationId)
    }
}
