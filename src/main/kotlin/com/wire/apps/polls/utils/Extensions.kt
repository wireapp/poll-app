package com.wire.apps.polls.utils

import mu.KLogging

/**
 * Creates logger with given name.
 */
fun createLogger(name: String) = KLogging().logger("com.wire.$name")
