package com.wire.bots.polls.setup.errors

import io.ktor.http.HttpStatusCode

data class RomanUnavailableException(
    val status: HttpStatusCode,
    val body: String,
    override val message: String = "Error in communication with Roman."
) : Exception(message)
