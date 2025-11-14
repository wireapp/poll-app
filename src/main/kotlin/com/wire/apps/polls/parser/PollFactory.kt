package com.wire.apps.polls.parser

import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.UsersInput
import mu.KLogging
import pw.forst.katlib.newLine
import pw.forst.katlib.whenNull

/**
 * Class used for creating the polls from the text. Parsing and creating the poll objects.
 */
class PollFactory(
    private val inputParser: InputParser,
    private val pollValidation: PollValidation
) {
    private companion object : KLogging()

    /**
     * Parse and create poll for the [usersInput].
     */
    fun forUserInput(usersInput: UsersInput): PollDto? {
        // TODO : Create better error handling and throw exceptions instead of returning nulls.
        //  Then move these logs to Service layer.
        val poll = inputParser.parsePoll(usersInput).whenNull {
            logger.warn {
                "Poll creation is failed. Reason: Command is not parseable. " +
                    "conversationId: ${usersInput.conversationId}, " +
                    "sender: ${usersInput.sender}"
            }
        } ?: return null

        val (valid, errors) = pollValidation.validate(poll)
        return if (valid) {
            logger.info {
                "Poll creation is successful." +
                    "conversationId: ${usersInput.conversationId}, " +
                    "sender: ${usersInput.sender}"
            }
            poll
        } else {
            logger.warn {
                "Poll creation failed. Reason: Command is not valid. " +
                    "conversationId: ${usersInput.conversationId}, " +
                    "sender: ${usersInput.sender}," +
                    "errors: ${errors.joinToString(newLine)}"
            }
            null
        }
    }
}
