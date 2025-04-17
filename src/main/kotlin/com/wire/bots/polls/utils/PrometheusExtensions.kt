package com.wire.bots.polls.utils

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.util.concurrent.TimeUnit

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
 * Register http call.
 *
 */
fun MeterRegistry.httpCall(requestMetric: RequestMetric) {
    val duration = requestMetric.responseTime - requestMetric.requestTime
    val tags = mapOf(
        "method" to requestMetric.method,
        "url" to requestMetric.url,
        "response_code" to requestMetric.responseCode.toString()
    ).toTags()

    timer("http_calls", tags).record(duration, TimeUnit.MILLISECONDS)
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
