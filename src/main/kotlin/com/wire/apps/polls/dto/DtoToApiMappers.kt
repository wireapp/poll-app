package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.common.Mention
import com.wire.apps.polls.dto.common.Text
import com.wire.apps.polls.dto.common.toWireMention
import com.wire.apps.polls.utils.RESULTS_BUTTON_ID
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import java.util.UUID
import pw.forst.katlib.newLine

/**
 * Creates message for poll.
 */
fun newPoll(
    conversationId: QualifiedId,
    body: String,
    buttons: List<Option>,
    mentions: List<Mention>
): WireMessage.Composite {
    val text = WireMessage.Text.create(
        conversationId = conversationId,
        text = body,
        mentions = mentions.map { it.toWireMention() }
    )
    return WireMessage.Composite(
        id = UUID.randomUUID(),
        conversationId = conversationId,
        sender = QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString()),
        items = listOf(text) + buttons.map { it.toWireButton() }
    )
}

private val showResultsButton = WireMessage.Button(
    text = "show results",
    id = RESULTS_BUTTON_ID
)

fun createPollOverview(conversationId: QualifiedId) =
    WireMessage.Composite.create(
        conversationId = conversationId,
        text = PollVoteCountProgress.new(),
        buttonList = listOf(showResultsButton)
    )

fun updatePollOverviewProgressBar(
    conversationId: QualifiedId,
    overviewMessageId: String,
    voteCountProgress: String
) = WireMessage.CompositeEdited.create(
    replacingMessageId = UUID.fromString(overviewMessageId),
    conversationId = conversationId,
    text = voteCountProgress,
    buttonList = listOf(showResultsButton)
)

/**
 * TODO replace below function with the commented out.
 * Clients need to implement rendering of a Composite Items in any order and not only:
 * One [Text]
 * Followed by one or more [Button]s
 *
 * As defined in the protobuf definition:
 * https://github.com/wireapp/generic-message-proto/blob/master/proto/messages.proto#L75-L86
 */
fun updatePollOverviewResults(
    conversationId: QualifiedId,
    overviewMessageId: String,
    stats: Text,
    voteCountProgress: String
) = WireMessage.CompositeEdited.create(
    replacingMessageId = UUID.fromString(overviewMessageId),
    conversationId = conversationId,
    text = voteCountProgress +
        newLine +
        stats.data,
    buttonList = emptyList()
)

// fun appendPollOverviewWithResults(
//     pollId: String,
//     stats: Text
// ): WireMessage.CompositeEdited {
//     val voteProgressMessage = WireMessage.Text.create(
//         conversationId = conversationId,
//         text = voteCountProgress + newLine
//     )
//     val statsMessage = statsMessage(
//         conversationId = conversationId,
//         text = stats
//     )
//     return WireMessage.CompositeEdited(
//         replacingMessageId = UUID.fromString(pollId),
//         conversationId = conversationId,
//         id = UUID.randomUUID(),
//         sender = QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString()),
//         newItems = listOf(voteProgressMessage, statsMessage)
//     )
// }

// /**
//  * Creates stats (result of the poll) message.
//  */
// fun statsMessage(
//     conversationId: QualifiedId,
//     text: Text
// ) = WireMessage.Text.create(
//     conversationId = conversationId,
//     text = text.data,
//     mentions = text.mentions.map { it.toWireMention() }
// )

/**
 * Creates text WireMessage for non-database bot interactions
 */
fun textMessage(
    conversationId: QualifiedId,
    text: String
): WireMessage.Text = WireMessage.Text.create(conversationId, text)
