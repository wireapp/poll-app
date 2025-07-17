package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollParticipation
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.services.VotingCount.update
import com.wire.apps.polls.setup.configureContainer
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
import com.wire.apps.polls.utils.Stub
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class StatsFormattingServiceTest {
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
    val testGroupSize = 5

    @Test
    fun `formatStats returns null when poll doesn't exist`() =
        runTest {
            // arrange
            coEvery { pollRepository.getPollQuestion(testPollId) } returns null
            coEvery { pollRepository.stats(testPollId) } returns emptyMap()

            // act
            val result = statsFormattingService.formatStats(
                pollId = testPollId,
                conversationMembers = testGroupSize
            )

            // assert
            result.shouldBeNull()
        }

    @Test
    fun `formatStats returns null when stats are empty`() =
        runTest {
            // arrange
            val pollQuestion = Text(data = "What's your favorite color?", mentions = emptyList())
            coEvery { pollRepository.getPollQuestion(testPollId) } returns pollQuestion
            coEvery { pollRepository.stats(testPollId) } returns emptyMap()

            // act
            val result = statsFormattingService.formatStats(
                pollId = testPollId,
                conversationMembers = testGroupSize
            )

            // assert
            result.shouldBeNull()
        }

    @Test
    fun `formatStats properly formats poll results based on max votes`() =
        runTest {
            // arrange
            val pollQuestion = Text(data = "What's your favorite color?", mentions = emptyList())
            val stats = mapOf(
                Pair(1, "Red") to 3,
                Pair(2, "Blue") to 1,
                Pair(3, "Green") to 0
            )

            coEvery { pollRepository.getPollQuestion(testPollId) } returns pollQuestion
            coEvery { pollRepository.stats(testPollId) } returns stats

            // act
            val result = statsFormattingService.formatStats(
                pollId = testPollId,
                conversationMembers = testGroupSize
            )

            // assert
            result.shouldNotBeNull()
            result.data.shouldBe(
                "**Results** for poll *\"What's your favorite color?\"*$newLine" +
                    "🟢🟢🟢⚪⚪ **Red** (3)$newLine" +
                    "🟢⚪⚪⚪⚪ *Blue* (1)$newLine" +
                    "⚪⚪⚪⚪⚪ *Green* (0)"
            )
        }

    @Test
    fun `formatStats handles mentions correctly`() =
        runTest {
            // arrange
            val userId1 = Stub.id()
            val userId2 = Stub.id()
            val userName1 = "@messi"
            val userName2 = "@ronaldo"
            val question = "who is the best? $userName1 or $userName2"

            val pollQuestion = Text(
                data = question,
                mentions = listOf(
                    Stub.mention(text = question, userName = userName1, qualifiedId = userId1),
                    Stub.mention(text = question, userName = userName2, qualifiedId = userId2)
                )
            )
            val stats = mapOf(
                Pair(1, "first") to 2,
                Pair(2, "second") to 1
            )

            coEvery { pollRepository.getPollQuestion(testPollId) } returns pollQuestion
            coEvery { pollRepository.stats(testPollId) } returns stats

            // act
            val result = statsFormattingService.formatStats(
                pollId = testPollId,
                conversationMembers = testGroupSize
            )

            // assert
            result.shouldNotBeNull()

            val resultText = result.data
            result.mentions shouldContainExactly listOf(
                Stub.mention(
                    text = resultText,
                    userName = userName1,
                    qualifiedId = userId1
                ),
                Stub.mention(
                    text = resultText,
                    userName = userName2,
                    qualifiedId = userId2
                )
            )
        }

    @Test
    fun `formatStats respects conversation members limit`() =
        runTest {
            // arrange
            val pollQuestion = Text(data = "Do you like cookies?", mentions = emptyList())
            val stats = mapOf(
                Pair(1, "Yes") to 2,
                Pair(2, "No") to 1
            )

            coEvery { pollRepository.getPollQuestion(testPollId) } returns pollQuestion
            coEvery { pollRepository.stats(testPollId) } returns stats

            // act
            val result = statsFormattingService.formatStats(
                pollId = testPollId,
                conversationMembers = 3
            )

            // assert
            result.shouldNotBeNull()
            result.data.shouldBe(
                "**Results** for poll *\"Do you like cookies?\"*$newLine" +
                    "🟢🟢⚪ **Yes** (2)$newLine" +
                    "🟢⚪⚪ *No* (1)"
            )
        }

    @Nested
    inner class ParticipationFormattingTest {
        @Test
        fun `when in initial stage, then bar should be empty with 0 percent`() {
            // act
            val votingCount = VotingCount.new()

            // assert
            votingCount.shouldBe(
                "⚪⚪⚪⚪⚪⚪⚪⚪⚪⚪ 0%"
            )
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
        fun `when n user voted out of ten, then bar should have n green dots`(n: Int) {
            // arrange
            val totalMembers = 10
            val pollParticipation = PollParticipation(n, totalMembers)

            // act
            val votingCount = pollParticipation.update()

            // assert
            votingCount.shouldBe(
                "🟢".repeat(n) + "⚪".repeat(totalMembers - n) + " ${n}0%"
            )
        }

        @Test
        fun `when n user voted out of 473, then bar should have correct percentage`() {
            // arrange
            val votesCast = listOf(
                23,
                36,
                118,
                212,
                237,
                355,
                473
            )
            val totalMembers = 473

            // act
            val results = votesCast.map { voted ->
                PollParticipation(voted, totalMembers).update()
            }

            // assert
            val expected = listOf(
                "⚪⚪⚪⚪⚪⚪⚪⚪⚪⚪ 5%",
                "🟢⚪⚪⚪⚪⚪⚪⚪⚪⚪ 8%",
                "🟢🟢⚪⚪⚪⚪⚪⚪⚪⚪ 25%",
                "🟢🟢🟢🟢⚪⚪⚪⚪⚪⚪ 45%",
                "🟢🟢🟢🟢🟢⚪⚪⚪⚪⚪ 50%",
                "🟢🟢🟢🟢🟢🟢🟢🟢⚪⚪ 75%",
                "🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢 100%"
            )
            expected.forEachIndexed { index, testCase ->
                results[index].shouldBe(expected[index])
            }
        }
    }
}
