package com.wire.apps.polls.parser

import com.wire.apps.polls.dto.Option
import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.UsersInput
import com.wire.apps.polls.dto.common.Mention
import com.wire.apps.polls.dto.common.Text
import mu.KLogging

class InputParser {
    private companion object : KLogging() {
        val delimiters = charArrayOf('\"', '“')

        val delimitersSet = delimiters.toSet()
    }

    fun parsePoll(userInput: UsersInput): PollDto? {
        val arguments = userInput.text.substringAfter("/poll", "")
        val quote = String(delimiters)
        val inQuotes = "[$quote](.*?)[$quote]".toRegex()

        val inputs = inQuotes.findAll(arguments)
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() }.toList()

        val inverseMatch = inQuotes.replace(arguments, "")

        if (inputs.isEmpty() || inverseMatch.trim().isNotBlank()) {
            logger.warn { "Given user input does not contain valid poll." }
            return null
        }

        return PollDto(
            question = Text(
                data = inputs.first(),
                mentions = shiftMentions(userInput)
            ),
            options = parseButtons(inputs.takeLast(inputs.size - 1))
        )
    }

    /**
     * Preserves compatibility with existing database schema by using index as the button ID.
     */
    private fun parseButtons(buttons: List<String>): List<Option> {
        return buttons.mapIndexed { index, text ->
            Option(text, index)
        }
    }

    /**
     * In the command issued by user, mention offset includes "/poll" prefix and whitespaces.
     * These are not needed in the actual poll representation. We align them in poll question.
     */
    private fun shiftMentions(usersInput: UsersInput): List<Mention> {
        val delimiterIndex = usersInput.text.indexOfFirst { delimitersSet.contains(it) }
        if (delimiterIndex == -1) return emptyList()
        val emptyCharsInQuestion = usersInput.text
            .substringAfter(usersInput.text[delimiterIndex])
            .takeWhile { it == ' ' }
            .count()

        val offsetChange = delimiterIndex + 1 + emptyCharsInQuestion
        return usersInput.mentions.map { mention ->
            mention.copy(offset = mention.offset - offsetChange)
        }
    }
}
