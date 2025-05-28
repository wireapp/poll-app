package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.dto.common.toWireMention
import com.wire.apps.polls.setup.configureContainer
import com.wire.integrations.jvm.model.QualifiedId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import pw.forst.katlib.newLine
import utils.Stub
import java.util.UUID

class StatsFormattingServiceTest : FunSpec({

    val pollRepository = mockk<PollRepository>()

    // Create a test Kodein module that overrides the PollRepository binding
    val testModule = DI.Module("testModule") {
        bind<PollRepository>(overrides = true) with singleton { pollRepository }
    }

    val di = DI {
        // Import the main container configuration
        configureContainer()
        import(testModule, allowOverride = true)
    }

    // Get the StatsFormattingService from the DI container
    val statsFormattingService by di.instance<StatsFormattingService>()

    val testPollId = "test-poll-id"
    val testConversationId = QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString())

    test("formatStats returns null when poll doesn't exist") {
        // Arrange
        coEvery { pollRepository.getPollQuestion(testPollId) } returns null
        coEvery { pollRepository.stats(testPollId) } returns emptyMap()

        // Act
        val result = statsFormattingService.formatStats(testPollId, testConversationId, null)

        // Assert
        result.shouldBeNull()
    }

    test("formatStats returns null when stats are empty") {
        // Arrange
        val pollQuestion = Text(data = "What's your favorite color?", mentions = emptyList())
        coEvery { pollRepository.getPollQuestion(testPollId) } returns pollQuestion
        coEvery { pollRepository.stats(testPollId) } returns emptyMap()

        // Act
        val result = statsFormattingService.formatStats(testPollId, testConversationId, null)

        // Assert
        result.shouldBeNull()
    }

    // TODO conversation's size should be always available
    test("formatStats properly formats poll results based on max votes").config(enabled = false) {
        // Arrange
        val pollQuestion = Text(data = "What's your favorite color?", mentions = emptyList())
        val stats = mapOf(
            Pair(1, "Red") to 3,
            Pair(2, "Blue") to 1,
            Pair(3, "Green") to 0
        )

        coEvery { pollRepository.getPollQuestion(testPollId) } returns pollQuestion
        coEvery { pollRepository.stats(testPollId) } returns stats

        // Act
        val result = statsFormattingService.formatStats(testPollId, testConversationId, null)

        // Assert
        result.shouldNotBeNull()
        result.text.shouldBe(
            "**Results** for poll *\"What's your favorite color?\"*$newLine" +
                "🟢🟢🟢 **Red** (3)$newLine" +
                "🟢⚪⚪ *Blue* (1)$newLine" +
                "⚪⚪⚪ *Green* (0)"
        )
    }

    test("formatStats handles mentions correctly") {
        // Arrange
        val userId1 = Stub.id()
        val userId2 = Stub.id()
        val userName1 = "@messi"
        val userName2 = "@ronaldo"
        val question = "who is the best? $userName1 or $userName2"

        val pollQuestion = Text(
            data = question,
            mentions = listOf(
                Stub.mention(question, userName1, userId1),
                Stub.mention(question, userName2, userId2)
            )
        )
        val stats = mapOf(
            Pair(1, "first") to 2,
            Pair(2, "second") to 1
        )

        coEvery { pollRepository.getPollQuestion(testPollId) } returns pollQuestion
        coEvery { pollRepository.stats(testPollId) } returns stats

        // Act
        val result = statsFormattingService.formatStats(testPollId, testConversationId, null)

        // Assert
        result.shouldNotBeNull()

        val resultText = result.text as String
        result.mentions shouldContainExactly listOf(
            Stub.mention(resultText, userName1, userId1).toWireMention(),
            Stub.mention(resultText, userName2, userId2).toWireMention()
        )
    }

    test("formatStats respects conversation members limit") {
        // Arrange
        val pollQuestion = Text(data = "Do you like cookies?", mentions = emptyList())
        val stats = mapOf(
            Pair(1, "Yes") to 2,
            Pair(2, "No") to 1
        )

        coEvery { pollRepository.getPollQuestion(testPollId) } returns pollQuestion
        coEvery { pollRepository.stats(testPollId) } returns stats

        // Act
        val result = statsFormattingService.formatStats(testPollId, testConversationId, 3)

        // Assert
        result.shouldNotBeNull()
        result.text.shouldBe(
            "**Results** for poll *\"Do you like cookies?\"*$newLine" +
                "🟢🟢⚪ **Yes** (2)$newLine" +
                "🟢⚪⚪ *No* (1)"
        )
    }
})
