package com.wire.bots.polls.services

import com.wire.bots.polls.dto.bot.BotMessage
import com.wire.bots.polls.dto.bot.fallBackMessage
import com.wire.bots.polls.dto.bot.goodBotMessage
import com.wire.bots.polls.dto.bot.greeting
import com.wire.bots.polls.dto.bot.helpMessage
import com.wire.bots.polls.dto.bot.versionMessage
import mu.KLogging

/**
 * Service used for handling init message.
 */
class UserCommunicationService(
    private val proxySenderService: ProxySenderService,
    private val version: String
) {
    private companion object : KLogging() {
        const val usage = "To create poll please text: `/poll \"Question\" \"Option 1\" \"Option 2\"`. To display usage write `/help`"
        val commands = """
            Following commands are available:
            `/poll "Question" "Option 1" "Option 2"` will create poll
            `/stats` will send result of the last poll in the conversation
            `/help` to show help
        """.trimIndent()
    }

    /**
     * Sends hello message with instructions to the conversation.
     */
    suspend fun sayHello(token: String) = greeting("Hello, I'm Poll Bot. $usage").send(token)

    /**
     * Sends message with help.
     */
    suspend fun reactionToWrongCommand(token: String) =
        fallBackMessage("I couldn't recognize your command. $usage").send(token)

    /**
     * Sends message containing help
     */
    suspend fun sendHelp(token: String) = helpMessage(commands).send(token)

    /**
     * Sends good bot message.
     */
    suspend fun goodBot(token: String) = goodBotMessage("\uD83D\uDE07").send(token)

    /**
     * Sends version of the bot to the user.
     */
    suspend fun sendVersion(token: String) = versionMessage("My version is: *$version*").send(token)

    private suspend fun BotMessage.send(token: String) = proxySenderService.send(token, this)
}
