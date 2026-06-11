package com.wire.apps.polls.utils

import io.github.oshai.kotlinlogging.slf4j.toKLogger
import org.slf4j.LoggerFactory

/**
 * Creates logger with given name.
 */
fun createLogger(name: String) = LoggerFactory.getLogger("com.wire.$name").toKLogger()
