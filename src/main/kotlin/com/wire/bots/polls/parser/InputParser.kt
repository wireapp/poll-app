package com.wire.bots.polls.parser

import com.wire.bots.polls.dto.PollDto
import com.wire.bots.polls.dto.Question
import com.wire.bots.polls.dto.UsersInput
import com.wire.integrations.jvm.model.WireMessage
import mu.KLogging

class InputParser {
    private companion object : KLogging() {
        val delimiters = charArrayOf('\"', '“')

        val delimitersSet = delimiters.toSet()
    }

    fun parsePoll(userInput: UsersInput): PollDto? {
        // TODO currently not supporting char " in the strings
        val inputs = userInput.input
            .substringAfter("/poll", "")
            .substringBeforeLast("\"")
            .split(*delimiters)
            .filter { it.isNotBlank() }
            .map { it.trim() }

        if (inputs.isEmpty()) {
            logger.warn { "Given user input does not contain valid poll." }
            return null
        }

        return PollDto(
            question = Question(
                body = inputs.first(),
                mentions = shiftMentions(userInput)
            ),
            options = parseButtons(inputs.takeLast(inputs.size - 1))
        )
    }

    private fun parseButtons(buttons: List<String>): List<WireMessage.Composite.Button> {
        return buttons.map { WireMessage.Composite.Button.create(it, isSelected = false) }
    }

    private fun shiftMentions(usersInput: UsersInput): List<WireMessage.Text.Mention> {
        val delimiterIndex = usersInput.input.indexOfFirst { delimitersSet.contains(it) }
        val emptyCharsInQuestion = usersInput.input
            .substringAfter(usersInput.input[delimiterIndex])
            .takeWhile { it == ' ' }
            .count()

        val offsetChange = delimiterIndex + 1 + emptyCharsInQuestion
        return usersInput.mentions.map { mention ->
            mention.copy(offset = mention.offset - offsetChange)
        }
    }
}
