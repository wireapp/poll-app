package com.wire.apps.polls.dto.common

import com.wire.integrations.jvm.model.QualifiedId

data class Mention(
    val userId: QualifiedId? = null,
    val offset: Int,
    val length: Int
)
