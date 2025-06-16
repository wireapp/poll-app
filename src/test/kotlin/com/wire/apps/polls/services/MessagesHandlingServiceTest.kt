package com.wire.apps.polls.services

import com.wire.apps.polls.setup.configureContainer
import com.wire.integrations.jvm.service.WireApplicationManager
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

    @AfterEach
    fun `confirm that only specified functions were called`() {
        confirmVerified(userCommunicationService, pollService)
    }

    @Test
    fun `handleText terminates when usersInput is null`() =
        runTest {
            // arrange
            val usersInput = null

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            verify { pollService wasNot Called }
            verify { userCommunicationService wasNot Called }
        }

    @Test
    fun `handleText calls sendStatsForLatest when user inputs poll stats`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/poll stats")

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            coVerify(exactly = 1) { pollService.sendStatsForLatest(any(), any()) }
            coVerify(exactly = 0) { pollService.createPoll(any(), any()) }
            verify { userCommunicationService wasNot Called }
        }

    @Test
    fun `handleText calls createPoll when user input starts will poll`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/poll question yes no")

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            coVerify(exactly = 1) { pollService.createPoll(any(), any()) }
            verify { userCommunicationService wasNot Called }
        }

    @Test
    fun `handleText calls sendVersion when user inputs poll version`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/poll version")

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.sendVersion(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Test
    fun `handleText calls sendHelp when user inputs poll help`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/poll help")

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Test
    fun `handleText calls goodApp when user inputs good app`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("good app")

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.goodApp(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Test
    fun `handleText ignores message unrelated to app`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("how is everyone?")

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            verify { pollService wasNot Called }
            verify { userCommunicationService wasNot Called }
        }

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
            // arrange
            val usersInput = Stub.userInput(command)

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            coVerify { pollService.createPoll(any(), any()) }
        }

    @Test
    fun `handleText ignores case when matching command`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput("/Poll help")

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            verify { pollService wasNot Called }
        }

    @Ignore("ignore whitespaces")
    @Test
    fun `handleText ignores additional whitespaces`() =
        runTest {
            // arrange
            val usersInput = Stub.userInput(" /poll  help ")

            // act
            messagesHandlingService.handleText(manager, usersInput)

            // assert
            coVerify(exactly = 1) { userCommunicationService.sendHelp(any(), any()) }
            verify { pollService wasNot Called }
        }
}
