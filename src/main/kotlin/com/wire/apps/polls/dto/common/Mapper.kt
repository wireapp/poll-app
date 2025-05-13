package com.wire.apps.polls.dto.common

import com.wire.apps.polls.dto.Option
import com.wire.apps.polls.dto.PollAction
import com.wire.apps.polls.dto.UsersInput
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import io.ktor.features.BadRequestException
import java.util.UUID

fun mapToUsersInput(wireMessage: WireMessage.Text): UsersInput? {
    val sender = wireMessage.sender
    sender ?: throw BadRequestException("Sender must be set for text messages.")
    val conversationId = wireMessage.conversationId
    val text = wireMessage.text ?: return null
    val mentions = mapToDtoMentions(wireMessage.mentions)

    return UsersInput(
        sender = sender,
        conversationId = conversationId,
        text = text,
        mentions = mentions
    )
}

private fun mapToDtoMentions(mentions: List<WireMessage.Text.Mention>): List<Mention> =
    mentions.map {
        Mention(
            userId = it.userId?.id.toString(),
            userDomain = it.userId?.domain.toString(),
            offset = it.offset,
            length = it.length
        )
    }

fun mapToWireMentions(mentions: List<Mention>): List<WireMessage.Text.Mention> =
    mentions.map {
        WireMessage.Text.Mention(
            userId = QualifiedId(
                id = UUID.fromString(it.userId),
                domain = it.userDomain
            ),
            offset = it.offset,
            length = it.length
        )
    }

fun mapToPollAction(wireMessage: WireMessage.ButtonAction): PollAction {
    val sender = wireMessage.sender
    sender ?: throw BadRequestException("Sender must be set for text messages.")

    return PollAction(
        pollId = wireMessage.referencedMessageId,
        optionId = wireMessage.buttonId.toInt(),
        userId = sender
    )
}

fun mapToWireButtons(buttons: List<Option>): List<WireMessage.Composite.Button> =
    buttons.map {
        WireMessage.Composite.Button(
            text = it.content,
            id = it.optionOrder.toString()
        )
    }
