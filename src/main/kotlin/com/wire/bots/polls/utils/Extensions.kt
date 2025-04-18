package com.wire.bots.polls.utils

import mu.KLogging
import org.slf4j.MDC

/**
 * Creates URL from [this] as base and [path] as path
 */
infix fun String.appendPath(path: String) =
    "${dropLastWhile { it == '/' }}/${path.dropWhile { it == '/' }}"

/**
 * Creates logger with given name.
 */
fun createLogger(name: String) = KLogging().logger("com.wire.$name")

/**
 * Includes value to current MDC under the key.
 */
inline fun mdc(
    key: String,
    value: () -> String?
) = mdc(key, value())

/**
 * Includes value to current MDC under the key if the key is not null.
 */
fun mdc(
    key: String,
    value: String?
) {
    if (value != null) {
        MDC.put(key, value)
    }
}
