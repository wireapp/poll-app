package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.newPoll
import com.wire.apps.polls.setup.configureContainer
import com.wire.integrations.jvm.service.WireApplicationManager
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import utils.Stub
import kotlin.test.Ignore

class MessagesHandlingServiceTest {
    val userCommunicationService = mockk<UserCommunicationService>(relaxed = true)
    val pollService = mockk<PollService>(relaxed = true)
    val manager = mockk<WireApplicationManager>()

    val testModule = DI.Module("testModule") {
        bind<UserCommunicationService>(overrides = true) with singleton { userCommunicationService }
        bind<PollService>(overrides = true) with singleton { pollService }
    }

    val di = DI {
        configureContainer()
        import(testModule, allowOverride = true)
    }

    val messagesHandlingService by di.instance<MessagesHandlingService>()

    @Test
    fun `handleText terminates when usersInput is null`() =
        runTest {
            val usersInput = null

            messagesHandlingService.handleText(manager, usersInput)

            verify { pollService wasNot Called }
            verify { userCommunicationService wasNot Called }
        }

    @Test
    fun `handleText calls sendStatsForLatest when user inputs poll stats`() =
        runTest {
            val usersInput = Stub.userInput("/poll stats")

            messagesHandlingService.handleText(manager, usersInput)

            coVerify(exactly = 1) { pollService.sendStatsForLatest(any(), any()) }
            coVerify(exactly = 0) { pollService.createPoll(any(), any()) }
            verify { userCommunicationService wasNot Called }
        }

    @Test
    fun `handleText calls createPoll when user input starts will poll`() =
        runTest {
            val usersInput = Stub.userInput("/poll question yes no")

            messagesHandlingService.handleText(manager, usersInput)

            coVerify(exactly = 1) { pollService.createPoll(any(), any()) }
            verify { userCommunicationService wasNot Called }
        }

    @Test
    fun `handleText calls sendVersion when user inputs poll version`() =
        runTest {
            val usersInput = Stub.userInput("/poll version")

            messagesHandlingService.handleText(manager, usersInput)

            coVerify(exactly = 1) { userCommunicationService.sendVersion(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Test
    fun `handleText calls sendHelp when user inputs poll help`() =
        runTest {
            val usersInput = Stub.userInput("/poll help")

            messagesHandlingService.handleText(manager, usersInput)

            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Test
    fun `handleText calls goodApp when user inputs good app`() =
        runTest {
            val usersInput = Stub.userInput("good app")

            messagesHandlingService.handleText(manager, usersInput)

            coVerify(exactly = 1) { userCommunicationService.goodApp(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Test
    fun `handleText ignores message unrelated to app`() =
        runTest {
            val usersInput = Stub.userInput("how is everyone?")

            messagesHandlingService.handleText(manager, usersInput)

            verify { pollService wasNot Called }
            verify { userCommunicationService wasNot Called }
        }

    @Ignore("Should enter createPoll and invoke pollNotParsedFallback to send usage")
    @ParameterizedTest
    @ValueSource(
        strings = [
            "/poll execute",
            "/poll statssss",
            "/poll help me",
            "/poll version pls"
        ]
    )
    fun `handleText calls createPoll to instruct users how to create poll`(command: String) =
        runTest {
            val newPollPath = "com.wire.apps.polls.dto.FactoriesKt"
            mockkStatic(newPollPath)
            val repository = mockk<PollRepository>(relaxed = true)
            val proxySenderService = mockk<ProxySenderService>(relaxed = true)

            val usersInput = Stub.userInput(command)

            messagesHandlingService.handleText(manager, usersInput)

            coVerify { pollService.createPoll(any(), any()) }
            coVerify(exactly = 0) {
                newPoll(
                    conversationId = any(),
                    body = any(),
                    buttons = any(),
                    mentions = any()
                )
                repository.savePoll(
                    poll = any(),
                    pollId = any(),
                    userId = any(),
                    userDomain = any(),
                    conversationId = any()
                )
                proxySenderService.send(any(), any())
            }
            confirmVerified(repository, proxySenderService)
            unmockkStatic(newPollPath)
        }

    @Ignore("ignore case")
    @Test
    fun `handleText ignores case when matching command`() =
        runTest {
            val usersInput = Stub.userInput("/Poll help")

            messagesHandlingService.handleText(manager, usersInput)

            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Ignore("ignore whitespaces")
    @Test
    fun `handleText ignores additional whitespaces`() =
        runTest {
            val usersInput = Stub.userInput(" /poll  help ")

            messagesHandlingService.handleText(manager, usersInput)

            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            verify { pollService wasNot Called }
        }

    @AfterEach
    fun `Confirm that only specified functions were called`() {
        confirmVerified(userCommunicationService, pollService)
    }
}
