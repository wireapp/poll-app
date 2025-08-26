package com.wire.apps.polls.dto

import com.wire.integrations.jvm.model.QualifiedId

sealed interface ButtonPressed {
    /**
     * Represents user clicking on "show results" button
     */
    data class ResultsRequest(
        val pollId: String,
        val userId: QualifiedId
    ) : ButtonPressed

    /**
     * Represents which option did user choose in Poll.
     */
    data class PollVote(
        val pollId: String,
        val index: Int,
        val userId: QualifiedId
    ) : ButtonPressed
}
