package com.wire.apps.polls.services

import com.wire.apps.polls.dto.PollVoteCountProgress
import com.wire.apps.polls.services.UserCommunicationService.FallbackMessageType
import com.wire.apps.polls.setup.configureContainer
import com.wire.apps.polls.utils.Stub
import com.wire.integrations.jvm.service.WireApplicationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlin.getValue
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

class UserCommunicationServiceTest {
    val statsFormattingService = mockk<StatsFormattingService>()
    val proxySenderService = mockk<ProxySenderService>(relaxed = true)
    val manager = mockk<WireApplicationManager>()
    val testModule = DI.Module("testModule") {
        bind<StatsFormattingService>(overrides = true) with singleton { statsFormattingService }
        bind<ProxySenderService>(overrides = true) with singleton { proxySenderService }
        bind<String>("version") with singleton { "test" }
    }

    val di = DI {
        configureContainer()
        import(testModule, allowOverride = true)
    }

    val userCommunicationService by di.instance<UserCommunicationService>()

    @Test
    fun `when stats formatting fails, then inform user that it failed`() =
        runTest {
            // arrange
            coEvery {
                statsFormattingService.formatStats(
                    pollId = POLL_ID,
                    conversationMembers = any()
                )
            } returns null
            val userCommunicationServiceSpy = spyk(userCommunicationService)

            // act
            val result = userCommunicationServiceSpy.updatePollResults(
                manager = manager,
                conversationId = Stub.id(),
                overviewMessageId = "test-overview-id",
                voteCountProgress = PollVoteCountProgress.initial(),
                pollId = POLL_ID
            )

            // assert
            assertNull(result)
            coVerify {
                userCommunicationServiceSpy.sendFallbackMessage(
                    manager = manager,
                    conversationId = any(),
                    messageType = FallbackMessageType.MISSING_DATA
                )
            }
        }

    private companion object {
        const val POLL_ID = "test-poll-id"
    }
}
