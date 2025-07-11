package com.wire.apps.polls.parser

import com.wire.apps.polls.dto.Option
import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.common.Mention
import com.wire.apps.polls.dto.common.Text
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import com.wire.apps.polls.utils.Stub
import io.kotest.matchers.collections.shouldContainAllIgnoringFields
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class InputParserTest {
    private val inputParser = InputParser()

    @ParameterizedTest
    @ValueSource(strings = ["/poll", "/poll \"\"", "/poll \" \""])
    fun `returns null when input is empty`(command: String) {
        // arrange
        val userInput = Stub.userInput(command)

        // act
        val result = inputParser.parsePoll(userInput)

        // assert
        result.shouldBeNull()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/poll \"Question\" \"Option 1\" \"Option 2\"",
            "/poll “Question“ “Option 1“ “Option 2“"
        ]
    )
    fun `parses input with question and options`(command: String) {
        // arrange
        val userInput = Stub.userInput(command)
        val expected = PollDto(
            Text("Question", emptyList()),
            listOf(
                Option("Option 1", 0),
                Option("Option 2", 1)
            )
        )

        // act
        val result = inputParser.parsePoll(userInput)

        // assert
        expected.shouldBe(result)
    }

    @Test
    fun `adds and shifts mentions`() {
        // arrange
        val user1 = "@user1"
        val user2 = "@user2"
        val command = "/poll \"$user1 is a $user2?\" \"yes\" \"no\""
        val userInput = Stub.userInput(
            text = command,
            mentions = listOf(
                Stub.mention(command, user1),
                Stub.mention(command, user2)
            )
        )

        // act
        val result = inputParser.parsePoll(userInput)?.question

        // assert
        result.shouldNotBeNull()
        result.mentions.shouldContainAllIgnoringFields(
            listOf(
                Stub.mention(result.data, user1),
                Stub.mention(result.data, user2)
            ),
            Mention::userId
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/poll \"a\" \"b\" c",
            "/poll \"a\" \"b\" \"c",
            "/poll “a“ “b“ c",
            "/poll “a“ “b“ “c",
            "/poll “a“ “b“ c“"
        ]
    )
    fun `informs user that quotes are missing`(command: String) {
        // arrange
        val userInput = Stub.userInput(command)

        // act
        val result = inputParser.parsePoll(userInput)

        // assert
        result.shouldBeNull()
    }
}
