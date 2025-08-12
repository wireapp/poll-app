package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.common.Text
import com.wire.integrations.jvm.model.WireMessage

data class PollDto(
    val question: Text,
    val options: List<Option>
)

data class Option(
    val content: String,
    val optionOrder: Int
)

fun Option.toWireButton(): WireMessage.Button =
    WireMessage.Button(
        text = this.content,
        id = this.optionOrder.toString()
    )
