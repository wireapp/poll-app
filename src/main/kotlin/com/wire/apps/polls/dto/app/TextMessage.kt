package com.wire.apps.polls.dto.app

import com.wire.apps.polls.dto.common.Text

internal data class TextMessage(
    val text: Text,
    override val type: String = "text"
) : BotMessage
