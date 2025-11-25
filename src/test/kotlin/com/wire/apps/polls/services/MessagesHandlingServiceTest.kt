package com.wire.apps.polls.services

import com.wire.apps.polls.setup.configureContainer
import com.wire.apps.polls.setup.metrics.UsageMetrics
import com.wire.sdk.service.WireApplicationManager
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
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
import com.wire.apps.polls.utils.Stub

class MessagesHandlingServiceTest {
    val userCommunicationService = mockk<UserCommunicationService>(relaxed = true)
    val pollService = mockk<PollService>(relaxed = true)
    val manager = mockk<WireApplicationManager>()
    val usageMetrics = mockk<UsageMetrics>(relaxed = true)

    val testModule = DI.Module("testModule") {
        bind<UserCommunicationService>(overrides = true) with singleton { userCommunicationService }
        bind<PollService>(overrides = true) with singleton { pollService }
        bind<UsageMetrics>(overrides = true) with singleton { usageMetrics }
    }

    val di = DI {
        configureContainer()
        import(testModule, allowOverride = true)
    }

    val messagesHandlingService by di.instance<MessagesHandlingService>()

    @AfterEach
    fun `confirm that only specified functions were called`() {
        confirmVerified(userCommunicationService, pollService)
    }

    @Test
    fun `handleUserCommand calls createPoll when user input starts will poll`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/poll question yes no")

            // act
            messagesHandlingService.handleUserCommand(manager, usersInput)

            // assert
            coVerify(exactly = 1) { usageMetrics.onCreatePollCommand() }
            coVerify(exactly = 1) { pollService.createPoll(any(), any()) }
            verify { userCommunicationService wasNot Called }
        }

    @Test
    fun `handleUserCommand calls sendVersion when user inputs poll version`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/poll version")

            // act
            messagesHandlingService.handleUserCommand(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.sendVersion(any(), any()) }
            verify { pollService wasNot Called }
            verify { usageMetrics wasNot Called }

        }

    @Test
    fun `handleUserCommand calls sendHelp when user inputs poll help`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/poll help")

            // act
            messagesHandlingService.handleUserCommand(manager, usersInput)

            // assert
            coVerify(exactly = 1) { usageMetrics.onHelpCommand() }
            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Test
    fun `handleUserCommand calls goodApp when user inputs good app`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("good app")

            // act
            messagesHandlingService.handleUserCommand(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.goodApp(any(), any()) }
            verify { pollService wasNot Called }
            verify { usageMetrics wasNot Called }
        }

    @Test
    fun `handleUserCommand ignores message unrelated to app`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("how is everyone?")

            // act
            messagesHandlingService.handleUserCommand(manager, usersInput)

            // assert
            verify { pollService wasNot Called }
            verify { userCommunicationService wasNot Called }
            verify { usageMetrics wasNot Called }
        }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/poll execute",
            "/poll stats",
            "/poll help me",
            "/poll version pls"
        ]
    )
    fun `handleUserCommand calls createPoll to instruct users how to create poll`(command: String) =
        runTest {
            // arrange
            val usersInput = Stub.userInput(command)

            // act
            messagesHandlingService.handleUserCommand(manager, usersInput)

            // assert
            coVerify { pollService.createPoll(any(), any()) }
            coVerify(exactly = 1) { usageMetrics.onCreatePollCommand() }
        }

    @Test
    fun `handleUserCommand ignores case when matching command`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/Poll help")

            // act
            messagesHandlingService.handleUserCommand(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            coVerify(exactly = 1) { usageMetrics.onHelpCommand() }
            verify { pollService wasNot Called }
        }

    @Test
    fun `handleUserCommand ignores additional whitespaces`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput(" /poll  help ")

            // act
            messagesHandlingService.handleUserCommand(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            coVerify(exactly = 1) { usageMetrics.onHelpCommand() }
            verify { pollService wasNot Called }
        }
}
