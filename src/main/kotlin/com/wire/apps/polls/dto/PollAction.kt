package com.wire.apps.polls.dto

import com.wire.integrations.jvm.model.QualifiedId

sealed interface PollAction {
    /**
     * Represents user clicking on "show results" button
     */
    data class ShowResultsAction(
        val pollId: String,
        val userId: QualifiedId
    ) : PollAction

    /**
     * Represents which option did user choose in Poll.
     */
    data class VoteAction(
        val pollId: String,
        val optionIndex: Int,
        val userId: QualifiedId
    ) : PollAction
}
