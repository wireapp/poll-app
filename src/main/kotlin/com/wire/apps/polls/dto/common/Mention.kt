package com.wire.apps.polls.dto.common

data class Mention(
    val userId: String,
    val userDomain: String,
    val offset: Int,
    val length: Int
)
