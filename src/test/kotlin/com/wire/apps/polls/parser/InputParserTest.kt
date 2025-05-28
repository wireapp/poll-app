package com.wire.apps.polls.parser

import com.wire.apps.polls.dto.Option
import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.common.Text
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import com.wire.apps.polls.utils.Stub
import kotlin.test.Ignore
import kotlin.test.assertEquals

class InputParserTest {
    private val inputParser = InputParser()

    @ParameterizedTest
    @ValueSource(strings = ["/poll", "/poll \"\"", "/poll \" \""])
    fun `returns null when input is empty`(command: String) {
        val userInput = Stub.userInput(command)

        assertNull(inputParser.parsePoll(userInput))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/poll \"Question\" \"Option 1\" \"Option 2\"",
            "/poll “Question“ “Option 1“ “Option 2“"
        ]
    )
    fun `parses input with question and options`(command: String) {
        val userInput = Stub.userInput(command)
        val expected = PollDto(
            Text("Question", emptyList()),
            listOf(
                Option("Option 1", 0),
                Option("Option 2", 1)
            )
        )

        assertEquals(expected, inputParser.parsePoll(userInput))
    }

    @Test
    fun `adds and shifts mentions`() {
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

        val result = inputParser.parsePoll(userInput)?.question

        assertEquals(2, result?.mentions?.size)
        assertEquals(result?.data?.indexOf(user1), result?.mentions?.get(0)?.offset)
        assertEquals(result?.data?.indexOf(user2), result?.mentions?.get(1)?.offset)
    }

    @Ignore("parser should inform that the quotes were incorrect")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "/poll \"a\" \"b\" c",
            "/poll \"a\" \"b\" \"c",
            "/poll “a“ “b“ c",
            "/poll “a“ “b“ “c",
            "/poll “a“ “b“ c“",
        ]
    )
    fun `informs user that quotes are missing`(command: String) {
        val userInput = Stub.userInput(command)

        val result = inputParser.parsePoll(userInput)

        assertNull(result)
    }
}
