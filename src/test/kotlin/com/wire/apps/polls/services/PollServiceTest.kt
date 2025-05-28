package com.wire.apps.polls.services

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.Option
import com.wire.apps.polls.dto.PollDto
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.dto.newPoll
import com.wire.apps.polls.parser.PollFactory
import com.wire.apps.polls.setup.configureContainer
import com.wire.apps.polls.utils.Stub
import com.wire.integrations.jvm.model.WireMessage
import com.wire.integrations.jvm.service.WireApplicationManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.util.UUID

class PollServiceTest {
    val factory = mockk<PollFactory>()
    val proxySenderService = mockk<ProxySenderService>(relaxed = true)
    val repository = mockk<PollRepository>(relaxed = true)
    val conversationService = mockk<ConversationService>()
    val userCommunicationService = mockk<UserCommunicationService>()
    val statsFormattingService = mockk<StatsFormattingService>()
    val manager = mockk<WireApplicationManager>()

    val testModule = DI.Module("testModule") {
        bind<PollFactory>(overrides = true) with singleton { factory }
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

    @Test
    fun `createPoll creates and sends poll with valid input`() =
        runTest {
            val usersInput = Stub.userInput("/poll \"question\" \"answer\"")
            val pollDto = PollDto(Text("question", emptyList()), listOf(Option("answer", 0)))
            every { factory.forUserInput(usersInput) } returns pollDto
            val message = mockk<WireMessage.Composite> {
                every { id } returns UUID.fromString("41b876e9-5387-463a-ab4d-555e1f603d41")
                every { id.toString() } returns "41b876e9-5387-463a-ab4d-555e1f603d41"
            }
            coEvery { repository.savePoll(any(), any(), any(), any(), any()) } returns message.id.toString()
            mockkStatic(::newPoll)
            every {
                newPoll(
                    conversationId = usersInput.conversationId,
                    body = pollDto.question.data,
                    buttons = pollDto.options,
                    mentions = pollDto.question.mentions
                )
            } returns message

            pollService.createPoll(manager, usersInput)

            verify { factory.forUserInput(usersInput) }
            verify { newPoll(
                conversationId = usersInput.conversationId,
                body = pollDto.question.data,
                buttons = pollDto.options,
                mentions = pollDto.question.mentions
            ) }
            coVerify {
                repository.savePoll(
                    poll = pollDto,
                    pollId = message.id.toString(),
                    userId = usersInput.sender.id.toString(),
                    userDomain = usersInput.sender.domain,
                    conversationId = usersInput.conversationId.id.toString()
                )
                proxySenderService.send(manager, message) }
        }
}
