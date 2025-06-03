package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.setup.configureContainer
import com.wire.apps.polls.utils.Stub
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
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
import kotlin.test.Ignore

class PollServiceTest {
    val repository = mockk<PollRepository>(relaxed = true)
    val conversationService = mockk<ConversationService>()
    val manager = mockk<WireApplicationManager>()
    val proxySenderService = mockk<ProxySenderService> {
        coEvery { send(manager, any()) } just Runs
    }
    val userCommunicationService = mockk<UserCommunicationService>()
    val statsFormattingService = mockk<StatsFormattingService>()

    val testModule = DI.Module("testModule") {
        bind<ProxySenderService>(overrides = true) with singleton { proxySenderService }
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
                    proxySenderService.send(manager, any())
                }
                confirmVerified(repository, proxySenderService)
            }

        @Test
        fun `when input is invalid, then send usage and terminate flow`() =
            runTest {
                // arrange
                val usersInput = Stub.userInput("/poll \"question without options\"")
                coEvery {
                    userCommunicationService.reactionToWrongCommand(manager, any())
                } just Runs
                val pollServiceSpy = spyk(pollService, recordPrivateCalls = true)

                // act
                pollServiceSpy.createPoll(manager, usersInput)

                // assert
                coVerify { userCommunicationService.reactionToWrongCommand(manager, any()) }
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
                    proxySenderService.send(manager, any())
                }
                confirmVerified(repository, proxySenderService, userCommunicationService)
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
                    proxySenderService.send(
                        manager,
                        ofType(WireMessage.ButtonActionConfirmation::class)
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
                        conversationId = any(),
                        conversationMembers = any()
                    )
                } returns WireMessage.Text.create(CONVERSATION_ID, "stats for test poll")

                // act
                pollService.pollAction(
                    manager = manager,
                    pollAction = pollAction,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify {
                    proxySenderService.send(
                        manager,
                        ofType(WireMessage.Text::class)
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
                    proxySenderService.send(
                        manager,
                        ofType(WireMessage.Text::class)
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
                val statsMessage = WireMessage.Text.create(
                    CONVERSATION_ID,
                    "stats for test poll"
                )
                coEvery {
                    statsFormattingService.formatStats(
                        pollId = any(),
                        conversationId = any(),
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
                    proxySenderService.send(
                        manager,
                        statsMessage
                    )
                }
            }

        @Ignore("inform user that poll was not found")
        @Test
        fun `when stats formatting fails, then inform user that it failed`() =
            runTest {
                // arrange
                coEvery {
                    statsFormattingService.formatStats(
                        pollId = any(),
                        conversationId = any(),
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
                    proxySenderService.send(
                        manager,
                        ofType(WireMessage.Text::class)
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
