package com.wire.apps.polls.dto.common

import com.wire.integrations.jvm.model.WireMessage

data class Text(
    val data: String,
    val mentions: List<WireMessage.Text.Mention>
)
