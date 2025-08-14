package com.wire.apps.polls.setup.conf

import java.util.UUID

data class SDKConfiguration(
    val appId: UUID,
    val appToken: String,
    val apiHostUrl: String,
    val cryptoPassword: String
)
