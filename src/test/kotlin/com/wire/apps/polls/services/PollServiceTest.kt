package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.UsersInput
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
    val pollRepository = mockk<PollRepository>(relaxed = true)
    val conversationService = mockk<ConversationService>()
    val manager = mockk<WireApplicationManager>()
    val userCommunicationService = mockk<UserCommunicationService>(relaxed = true)

    val testModule = DI.Module("testModule") {
        bind<PollRepository>(overrides = true) with singleton { pollRepository }
        bind<ConversationService>(overrides = true) with singleton { conversationService }
        bind<UserCommunicationService>(overrides = true) with singleton { userCommunicationService }
    }

    val di = DI {
        configureContainer()
        import(testModule, allowOverride = true)
    }

    val pollService by di.instance<PollService>()

    @Nested
    inner class CreatePollTest {
        @Test
        fun `when input is valid, then save the poll and send it with initial overview`() =
            runTest {
                // arrange
                val usersInput = UsersInput(
                    sender = Stub.id(),
                    conversationId = CONVERSATION_ID,
                    text = "/poll \"Question\" \"Answer\"",
                    mentions = emptyList()
                )
                coEvery { pollRepository.getOverviewMessageId(any()) } returns null

                // act
                pollService.createPoll(manager, usersInput)

                // assert
                coVerify {
                    pollRepository.savePoll(
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
                    userCommunicationService.sendInitialPollOverview(
                        manager = manager,
                        conversationId = usersInput.conversationId
                    )
                    pollRepository.getOverviewMessageId(any())
                    pollRepository.setOverviewMessageId(any(), any())
                }
                confirmVerified(pollRepository, userCommunicationService)
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
                    pollRepository.savePoll(
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
                confirmVerified(pollRepository, userCommunicationService)
            }
    }

    @Nested
    inner class PollActionTest {
        private val voteAction = PollAction.VoteAction(
            pollId = POLL_ID,
            optionIndex = 0,
            userId = Stub.id()
        )

        @BeforeEach
        fun `set up group size`() {
            // arrange
            every {
                conversationService.getNumberOfConversationMembers(manager, CONVERSATION_ID)
            } returns GROUP_SIZE
            coEvery { pollRepository.isPollMessage(POLL_ID) } returns true
        }

        @Test
        fun `when someone voted, then save vote`() =
            runTest {
                // act
                pollService.processVoteAction(
                    manager = manager,
                    voteAction = voteAction,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify(exactly = 1) {
                    pollRepository.saveVote(voteAction)
                }
            }

        @Test
        fun `when everyone in the conversation voted, then results are set to visible`() =
            runTest {
                // arrange
                coEvery { pollRepository.votingUsers(any()).size } returns GROUP_SIZE

                // act
                pollService.processVoteAction(
                    manager = manager,
                    voteAction = voteAction,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify {
                    pollRepository.setResultVisibilityToTrue(POLL_ID)
                }
            }

        @Test
        fun `when results are set to visible, then poll overview should be updated with stats`() =
            runTest {
                // arrange
                coEvery { pollRepository.isResultVisible(POLL_ID) } returns true

                // act
                pollService.processVoteAction(
                    manager = manager,
                    voteAction = voteAction,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify {
                    userCommunicationService.updatePollResults(
                        manager = manager,
                        conversationId = CONVERSATION_ID,
                        overviewMessageId = any(),
                        voteCountProgress = any(),
                        pollId = POLL_ID
                    )
                }
            }

        @Test
        fun `when it is not the last vote, then don't send stats`() =
            runTest {
                // arrange
                coEvery { pollRepository.votingUsers(any()).size } returns 1

                // act
                pollService.processVoteAction(
                    manager = manager,
                    voteAction = voteAction,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify(exactly = 1) {
                    userCommunicationService.updatePollProgressBar(
                        manager = manager,
                        conversationId = CONVERSATION_ID,
                        overviewMessageId = any(),
                        voteCountProgress = any()
                    )
                }
            }

        @Test
        fun `when someone voted, then update poll overview message`() =
            runTest {
                // arrange
                val stubOverviewId = "overview id"
                coEvery {
                    pollRepository.getOverviewMessageId(POLL_ID)
                } returns stubOverviewId
                coEvery { pollRepository.votingUsers(any()).size } returns 1

                // act
                pollService.processVoteAction(
                    manager = manager,
                    voteAction = voteAction,
                    conversationId = CONVERSATION_ID
                )

                coVerify {
                    userCommunicationService.updatePollProgressBar(
                        manager = manager,
                        overviewMessageId = stubOverviewId,
                        conversationId = CONVERSATION_ID,
                        voteCountProgress = any()
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
                coEvery { pollRepository.isResultVisible(POLL_ID) } returns true

                // act
                pollService.refreshOverview(
                    manager = manager,
                    pollId = POLL_ID,
                    conversationId = CONVERSATION_ID
                )

                // assert
                coVerify {
                    userCommunicationService.updatePollResults(
                        manager = manager,
                        overviewMessageId = any(),
                        conversationId = CONVERSATION_ID,
                        voteCountProgress = any(),
                        pollId = POLL_ID
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
