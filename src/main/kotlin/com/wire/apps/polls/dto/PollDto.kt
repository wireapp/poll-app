package com.wire.apps.polls.dto

import com.wire.apps.polls.dto.Option.Companion.OPTION_BUTTON_PREFIX
import com.wire.apps.polls.dto.common.Text
import com.wire.integrations.jvm.model.WireMessage

data class PollDto(
    val question: Text,
    val options: List<Option>
)

data class Option(
    val content: String,
    val optionOrder: Int
) {
    companion object {
        const val OPTION_BUTTON_PREFIX = "poll_option"
    }
}

fun Option.toWireButton(): WireMessage.Button =
    WireMessage.Button(
        text = this.content,
        id = OPTION_BUTTON_PREFIX + this.optionOrder
    )
