package com.wire.apps.polls.dto

import com.wire.integrations.jvm.model.WireMessage

data class PollDto(
    val question: Question,
    val options: List<WireMessage.Composite.Button>
)

data class Question(
    val body: String,
    val mentions: List<WireMessage.Text.Mention>
)
