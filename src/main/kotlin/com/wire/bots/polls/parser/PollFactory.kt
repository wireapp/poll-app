package com.wire.bots.polls.parser

import com.wire.bots.polls.dto.PollDto
import com.wire.bots.polls.dto.UsersInput
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
        // TODO create better error handling and return probably Either
        val poll = inputParser.parsePoll(usersInput).whenNull {
            logger.warn { "It was not possible to create poll for user input $usersInput." }
        } ?: return null

        val (valid, errors) = pollValidation.validate(poll)
        return if (valid) {
            logger.debug { "Poll successfully created for user input: $usersInput" }
            poll
        } else {
            logger.warn {
                "It was not possible to create poll for user input: $usersInput due to errors listed bellow:" +
                    "$newLine${errors.joinToString(newLine)}"
            }
            null
        }
    }
}
