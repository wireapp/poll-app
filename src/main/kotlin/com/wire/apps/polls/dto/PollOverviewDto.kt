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
        const val RESULTS_BUTTON_PREFIX = "show_results"

        private val button = WireMessage.Button(
            text = "show results",
            id = RESULTS_BUTTON_PREFIX
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
        statsMessage: Text?
    ): WireMessage.CompositeEdited =
        if (statsMessage == null) {
            logger.debug { "Updating statistics in conversation $conversationId" }
            refreshProgressBarOnly(overviewMessageId)
        } else {
            logger.debug { "Updating progress bar in conversation $conversationId" }
            appendUpdatedResults(overviewMessageId, statsMessage)
        }

    private fun refreshProgressBarOnly(overviewMessageId: String) =
        WireMessage.CompositeEdited.create(
            replacingMessageId = UUID.fromString(overviewMessageId),
            conversationId = conversationId,
            text = voteCountProgress,
            buttonList = listOf(button)
        )

    private fun appendUpdatedResults(
        overviewMessageId: String,
        statsMessage: Text
    ) = WireMessage.CompositeEdited.create(
        replacingMessageId = UUID.fromString(overviewMessageId),
        conversationId = conversationId,
        text = voteCountProgress +
            newLine +
            // TODO add mentions with correct offset
            statsMessage.data,
        buttonList = emptyList()
    )
}
