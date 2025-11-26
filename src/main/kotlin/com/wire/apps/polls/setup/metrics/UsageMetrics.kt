package com.wire.apps.polls.setup.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry

class UsageMetrics(
    registry: MeterRegistry
) {
    private val helpCommandCounter: Counter = Counter
        .builder("pollapp_help_commands_total")
        .description("Number of Poll help commands received")
        .register(registry)

    private val createPollCommandCounter: Counter = Counter
        .builder("pollapp_create_poll_commands_total")
        .description("Number of Poll creation commands received")
        .register(registry)

    private val deletePollCommandCounter: Counter = Counter
        .builder("pollapp_delete_poll_commands_total")
        .description("Number of Poll deletion commands received")
        .register(registry)

    fun onHelpCommand() {
        helpCommandCounter.increment()
    }

    fun onCreatePollCommand() {
        createPollCommandCounter.increment()
    }

    fun onDeletePollCommand() {
        deletePollCommandCounter.increment()
    }
}
