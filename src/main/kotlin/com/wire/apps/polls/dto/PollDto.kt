package com.wire.apps.polls.dto

import com.wire.integrations.jvm.model.WireMessage

typealias Option = WireMessage.Composite.Button

data class PollDto(
    val question: Question,
    val options: List<Option>
)

data class Question(
    val body: String,
    val mentions: List<WireMessage.Text.Mention>
)
