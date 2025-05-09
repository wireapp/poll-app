package com.wire.apps.polls.parser

import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.Question
import com.wire.apps.polls.dto.UsersInput
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

    /**
     * Preserves compatibility with existing database schema by using index as the button ID.
     */
    private fun parseButtons(buttons: List<String>): List<WireMessage.Composite.Button> {
        return buttons.mapIndexed { index, text ->
            WireMessage.Composite.Button(text, index.toString())
        }
    }

    private fun shiftMentions(usersInput: UsersInput): List<WireMessage.Text.Mention> {
        val delimiterIndex = usersInput.input.indexOfFirst { delimitersSet.contains(it) }
        if (delimiterIndex == -1) return emptyList()
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
