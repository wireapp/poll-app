package com.wire.apps.polls.setup

import com.wire.apps.polls.dao.PollRepository
import com.wire.apps.polls.dto.PollActionMapper
import com.wire.apps.polls.parser.InputParser
import com.wire.apps.polls.parser.PollFactory
import com.wire.apps.polls.parser.PollValidation
import com.wire.apps.polls.services.ConversationService
import com.wire.apps.polls.services.MessagesHandlingService
import com.wire.apps.polls.services.PollService
import com.wire.apps.polls.services.ProxySenderService
import com.wire.apps.polls.services.StatsFormattingService
import com.wire.apps.polls.services.UserCommunicationService
import com.wire.apps.polls.utils.createLogger
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KLogger
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

fun DI.MainBuilder.configureContainer() {
    bind<PollValidation>() with singleton { PollValidation() }

    bind<ProxySenderService>() with singleton { ProxySenderService() }

    bind<PrometheusMeterRegistry>() with singleton {
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
            with(this.config()) {
                commonTags("application", "poll-app")
            }
        }
    }

    bind<InputParser>() with singleton { InputParser() }

    bind<PollFactory>() with singleton { PollFactory(instance(), instance()) }

    bind<PollRepository>() with singleton { PollRepository() }

    bind<PollService>() with
        singleton {
            PollService(
                factory = instance(),
                pollRepository = instance(),
                conversationService = instance(),
                userCommunicationService = instance(),
                statsFormattingService = instance()
            )
        }

    bind<UserCommunicationService>() with
        singleton { UserCommunicationService(instance(), instance("version")) }

    bind<ConversationService>() with singleton { ConversationService() }

    bind<PollActionMapper>() with singleton { PollActionMapper(instance()) }

    bind<MessagesHandlingService>() with
        singleton { MessagesHandlingService(instance(), instance()) }

    bind<StatsFormattingService>() with singleton { StatsFormattingService(instance()) }

    bind<KLogger>("routing-logger") with singleton { createLogger("Routing") }
    bind<KLogger>("install-logger") with singleton { createLogger("KtorStartup") }
}
