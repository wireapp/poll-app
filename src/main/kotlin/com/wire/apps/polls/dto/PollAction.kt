package com.wire.apps.polls.dto

import com.wire.integrations.jvm.model.QualifiedId

/**
 * Represents poll vote from the user.
 */
data class PollAction(
    val pollId: String,
    val optionId: Int,
    val userId: QualifiedId
)
