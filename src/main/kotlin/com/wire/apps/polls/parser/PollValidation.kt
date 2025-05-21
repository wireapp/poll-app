package com.wire.apps.polls.parser

import com.wire.apps.polls.dto.Option
import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.common.Text
import mu.KLogging

private typealias QuestionRule = (Text) -> String?
private typealias OptionRule = (Option) -> String?
private typealias PollRule = (PollDto) -> String?

/**
 * Class validating the polls.
 */
class PollValidation {
    private companion object : KLogging()

    private val questionRules =
        listOf<QuestionRule> { question ->
            if (question.data.isNotBlank()) null else "The question must not be empty!"
        }

    private val optionRules =
        listOf<OptionRule> { option ->
            if (option.content.isNotBlank()) null else "The option must not be empty!"
        }

    private val pollRules =
        listOf<PollRule> { poll ->
            if (poll.options.isNotEmpty()) {
                null
            } else {
                "There must be at least one option for answering the poll."
            }
        }

    /**
     * Validates given poll, returns pair when the boolean signalizes whether the poll is valid or not.
     *
     * The string collection contains violated constraints when the poll is invalid.
     */
    fun validate(poll: PollDto): Pair<Boolean, Collection<String>> {
        val pollValidation = pollRules.mapNotNull { it(poll) }
        val questionValidation = questionRules.mapNotNull { it(poll.question) }
        val optionsValidation = optionRules.flatMap { poll.options.mapNotNull(it) }

        return (
            pollValidation.isEmpty() &&
                questionValidation.isEmpty() &&
                optionsValidation.isEmpty()
        ) to
            pollValidation + questionValidation + optionsValidation
    }
}
