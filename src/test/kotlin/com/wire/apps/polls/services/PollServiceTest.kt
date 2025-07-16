package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.services.UserCommunicationService.FallbackMessageType.MISSING_DATA
import com.wire.apps.polls.services.UserCommunicationService.FallbackMessageType.WRONG_COMMAND
import com.wire.apps.polls.setup.configureContainer
import com.wire.apps.polls.utils.Stub
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.service.WireApplicationManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

class PollServiceTest {
    val repository = mockk<PollRepository>(relaxed = true)
    val conversationService = mockk<ConversationService>()
    val manager = mockk<WireApplicationManager>()
    val userCommunicationService = mockk<UserCommunicationService>(relaxed = true)
    val statsFormattingService = mockk<StatsFormattingService>()

    val testModule = DI.Module("testModule") {
        bind<PollRepository>(overrides = true) with singleton { repository }
        bind<ConversationService>(overrides = true) with singleton { conversationService }
        bind<UserCommunicationService>(overrides = true) with singleton { userCommunicationService }
        bind<StatsFormattingService>(overrides = true) with singleton { statsFormattingService }
    }

    val di = DI {
        configureContainer()
        import(testModule, allowOverride = true)
    }

    val pollService by di.instance<PollService>()

    @Nested
    inner class CreatePollTest {
        @Test
        fun `when input is valid, then save the poll and send it`() =
            runTest {
                // arrange
                val usersInput = Stub.userInput("/poll \"Question\" \"Answer\"")

                // act
                pollService.createPoll(manager, usersInput)

                // assert
                coVerify {
                    repository.savePoll(
                        poll = any(),
                        pollId = any(),
                        userId = usersInput.sender.id.toString(),
                        userDomain = usersInput.sender.domain,
                        conversationId = any()
                    )
                    userCommunicationService.sendPoll(
                        manager = manager,
                        conversationId = usersInput.conversationId,
                        poll = any()
                    )
                }
                confirmVerified(repository, userCommunicationService)
            }

        @Test
        fun `when input is invalid, then send usage and terminate flow`() =
            runTest {
                // arrange
                val usersInput = Stub.userInput("/poll \"question without options\"")
                coEvery {
                    userCommunicationService.sendFallbackMessage(
                        manager = manager,
                        conversationId = usersInput.conversationId,
                        messageType = WRONG_COMMAND
                    )
                } just Runs
                val pollServiceSpy = spyk(pollService, recordPrivateCalls = true)

                // act
                pollServiceSpy.createPoll(manager, usersInput)

                // assert
                coVerify {
                    userCommunicationService.sendFallbackMessage(
                        manager = manager,
                        conversationId = usersInput.conversationId,
                        messageType = WRONG_COMMAND
                    )
                }
                verify {
                    pollServiceSpy["pollNotParsedFallback"](manager, any<QualifiedId>(), usersInput)
                }
                coVerify(exactly = 0) {
                    repository.savePoll(
                        poll = any(),
                        pollId = any(),
                        userId = any(),
                        userDomain = any(),
                        conversationId = any()
                    )
                    userCommunicationService.sendPoll(
                        manager = manager,
                        conversationId = usersInput.conversationId,
                        poll = any()
                    )
                }
                confirmVerified(repository, userCommunicationService)
            }
    }

    @Nested
    inner class PollActionTest {
        private val pollAction = PollAction(
            pollId = POLL_ID,
            optionId = 0,
            userId = Stub.id()
        )

        @BeforeEach
        fun `set up group size`() {
            // arrange
            every {
                conversationService.getNumberOfConversationMembers(manager, CONVERSATION_ID)
            } returns GROUP_SIZE
        }

        @Test
        fun `when someone voted, then register vote and send confirmation`() =
            runTest {
                // act
                pollService.pollAction(
                    manager = manager,
                    pollAction = pollAction,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify(exactly = 1) {
                    repository.vote(pollAction)
                    userCommunicationService.sendButtonConfirmation(
                        manager = manager,
                        pollAction = any(),
                        conversationId = CONVERSATION_ID
                    )
                }
            }

        @Test
        fun `when everyone in the conversation voted, then send the stats`() =
            runTest {
                // arrange
                coEvery { repository.votingUsers(any()).size } returns GROUP_SIZE
                coEvery {
                    statsFormattingService.formatStats(
                        pollId = any(),
                        conversationMembers = any()
                    )
                } returns Text("stats for test poll", emptyList())

                // act
                pollService.pollAction(
                    manager = manager,
                    pollAction = pollAction,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify {
                    userCommunicationService.sendStats(
                        manager = manager,
                        conversationId = CONVERSATION_ID,
                        text = any()
                    )
                }
            }

        @Test
        fun `when it is not the last vote, then don't send stats`() =
            runTest {
                // arrange
                coEvery { repository.votingUsers(any()).size } returns 1

                // act
                pollService.pollAction(
                    manager = manager,
                    pollAction = pollAction,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify(exactly = 0) {
                    userCommunicationService.sendStats(
                        manager = manager,
                        conversationId = CONVERSATION_ID,
                        text = any()
                    )
                }
            }
    }

    @Nested
    inner class SendStatsTest {
        @Test
        fun `when stats formatting is successful, then send stats`() =
            runTest {
                // arrange
                val statsMessage = Text(
                    "stats for test poll",
                    emptyList()
                )
                coEvery {
                    statsFormattingService.formatStats(
                        pollId = any(),
                        conversationMembers = any()
                    )
                } returns statsMessage

                // act
                pollService.sendStats(
                    manager = manager,
                    pollId = POLL_ID,
                    conversationId = CONVERSATION_ID,
                    conversationMembers = GROUP_SIZE
                )

                // assert
                coVerify {
                    userCommunicationService.sendStats(
                        manager = manager,
                        conversationId = CONVERSATION_ID,
                        text = statsMessage
                    )
                }
            }

        @Test
        fun `when stats formatting fails, then inform user that it failed`() =
            runTest {
                // arrange
                coEvery {
                    statsFormattingService.formatStats(
                        pollId = any(),
                        conversationMembers = any()
                    )
                } returns null

                // act
                pollService.sendStats(
                    manager = manager,
                    pollId = POLL_ID,
                    conversationId = CONVERSATION_ID,
                    conversationMembers = GROUP_SIZE
                )

                // assert
                coVerify {
                    userCommunicationService.sendFallbackMessage(
                        manager = manager,
                        conversationId = CONVERSATION_ID,
                        messageType = MISSING_DATA
                    )
                }
            }
    }

    companion object {
        private val CONVERSATION_ID = Stub.id()
        private const val POLL_ID = "test-poll-id"
        private const val GROUP_SIZE = 5
    }
}
