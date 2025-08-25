package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.common.Text
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import java.util.UUID
import mu.KLogging
import pw.forst.katlib.newLine

data class PollOverviewDto(
    val conversationId: QualifiedId,
    val voteCountProgress: String = PollVoteCountProgress.new()
) {
    companion object : KLogging() {
        const val RESULTS_BUTTON_ID = "show_results"

        private val button = WireMessage.Button(
            text = "show results",
            id = RESULTS_BUTTON_ID
        )
    }

    fun createInitialMessage() =
        WireMessage.Composite.create(
            conversationId = conversationId,
            text = voteCountProgress,
            buttonList = listOf(button)
        )

    fun update(
        overviewMessageId: String,
        stats: Text?
    ): WireMessage.CompositeEdited =
        if (stats == null) {
            logger.debug { "Updating statistics in conversation $conversationId" }
            refreshProgressBarOnly(overviewMessageId)
        } else {
            logger.debug { "Updating progress bar in conversation $conversationId" }
            appendUpdatedResults(overviewMessageId, stats)
        }

    private fun refreshProgressBarOnly(overviewMessageId: String) =
        WireMessage.CompositeEdited.create(
            replacingMessageId = UUID.fromString(overviewMessageId),
            conversationId = conversationId,
            text = voteCountProgress,
            buttonList = listOf(button)
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
    private fun appendUpdatedResults(
        overviewMessageId: String,
        stats: Text
    ) = WireMessage.CompositeEdited.create(
        replacingMessageId = UUID.fromString(overviewMessageId),
        conversationId = conversationId,
        text = voteCountProgress +
            newLine +
            stats.data,
        buttonList = emptyList()
    )
//    private fun appendUpdatedResults(
//        overviewMessageId: String,
//        stats: Text
//    ): WireMessage.CompositeEdited {
//        val voteProgressMessage = WireMessage.Text.create(
//            conversationId = conversationId,
//            text = voteCountProgress + newLine
//        )
//        val statsMessage = statsMessage(
//            conversationId = conversationId,
//            text = stats
//        )
//        return WireMessage.CompositeEdited(
//            replacingMessageId = UUID.fromString(overviewMessageId),
//            conversationId = conversationId,
//            id = UUID.randomUUID(),
//            sender = QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString()),
//            newItems = listOf(voteProgressMessage, statsMessage)
//        )
//    }
}
