package com.wire.apps.polls.services

import com.wire.apps.polls.dto.app.textMessage
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import com.wire.integrations.jvm.service.WireApplicationManager
import mu.KLogging

/**
 * Service used for handling init message.
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
     * Sends hello message with instructions to the conversation.
     */
    suspend fun sayHello(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, "Hello, I'm Poll App. $USAGE").send(manager)
    }

    /**
     * Sends message with help.
     */
    suspend fun reactionToWrongCommand(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, "I couldn't recognize your command. $USAGE").send(manager)
    }

    /**
     * Sends message containing help
     */
    suspend fun sendHelp(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, commands).send(manager)
    }

    /**
     * Sends good app message.
     */
    suspend fun goodApp(
        manager: WireApplicationManager,
        conversationId: QualifiedId
    ) {
        textMessage(conversationId, "\uD83D\uDE07").send(manager)
    }

    /**
     * Sends version of the app to the user.
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
