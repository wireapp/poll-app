package com.wire.apps.polls.dto.common

data class Text(
    val data: String,
    val mentions: List<Mention>
)
