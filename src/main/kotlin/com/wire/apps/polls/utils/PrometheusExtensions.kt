package com.wire.apps.polls.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag

/**
 * Registers exception in the prometheus metrics.
 */
fun MeterRegistry.countException(
    exception: Throwable,
    additionalTags: Map<String, String> = emptyMap()
) {
    val baseTags = mapOf(
        "type" to exception.javaClass.name,
        "message" to (exception.message ?: "No message.")
    )
    val tags = (baseTags + additionalTags).toTags()
    counter("exceptions", tags).increment()
}

/**
 * Convert map to the logging tags.
 */
private fun Map<String, String>.toTags() = map { (key, value) -> Tag(key, value) }

/**
 * Because original implementation is not handy.
 */
private data class Tag(
    private val k: String,
    private val v: String
) : Tag {
    override fun getKey(): String = k

    override fun getValue(): String = v
}
