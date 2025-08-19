package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.common.Text
import com.wire.integrations.jvm.model.QualifiedId
import com.wire.integrations.jvm.model.WireMessage
import java.util.UUID
import pw.forst.katlib.newLine

data class PollOverviewDto(
    val conversationId: QualifiedId,
    val voteCountProgress: String = PollVoteCountProgress.new()
) {
    companion object {
        private val button = WireMessage.Button(text = "show results")
    }

    fun initial() =
        WireMessage.Composite.create(
            conversationId = conversationId,
            text = voteCountProgress,
            buttonList = listOf(button)
        )

    fun updateWithHiddenResults(overviewMessageId: String) =
        WireMessage.CompositeEdited.create(
            replacingMessageId = UUID.fromString(overviewMessageId),
            conversationId = conversationId,
            text = voteCountProgress,
            buttonList = listOf(button)
        )

    fun updateWithOpenResults(
        overviewMessageId: String,
        stats: Text
    ) = WireMessage.CompositeEdited.create(
        replacingMessageId = UUID.fromString(overviewMessageId),
        conversationId = conversationId,
        text = voteCountProgress +
            newLine +
            stats,
        buttonList = emptyList()
    )
}
