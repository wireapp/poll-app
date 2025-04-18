package com.wire.bots.polls.setup

import com.wire.bots.polls.dao.PollRepository
import com.wire.bots.polls.parser.InputParser
import com.wire.bots.polls.parser.PollFactory
import com.wire.bots.polls.parser.PollValidation
import com.wire.bots.polls.services.AuthService
import com.wire.bots.polls.services.ConversationService
import com.wire.bots.polls.services.MessagesHandlingService
import com.wire.bots.polls.services.PollService
import com.wire.bots.polls.services.ProxySenderService
import com.wire.bots.polls.services.StatsFormattingService
import com.wire.bots.polls.services.UserCommunicationService
import com.wire.bots.polls.utils.createLogger
import io.ktor.client.HttpClient
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KLogger
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

fun DI.MainBuilder.configureContainer() {
    bind<PollValidation>() with singleton { PollValidation() }

    bind<HttpClient>() with singleton { createHttpClient(instance()) }

    bind<ProxySenderService>() with singleton {
        ProxySenderService(
            client = instance(),
            config = instance()
        )
    }

    bind<PrometheusMeterRegistry>() with singleton {
        PrometheusMeterRegistry(PrometheusConfig.DEFAULT).apply {
            with(this.config()) {
                commonTags("application", "poll-bot")
            }
        }
    }

    bind<InputParser>() with singleton { InputParser() }

    bind<PollFactory>() with singleton { PollFactory(instance(), instance()) }

    bind<PollRepository>() with singleton { PollRepository() }

    bind<PollService>() with
        singleton {
            PollService(
                instance(),
                instance(),
                instance(),
                instance(),
                instance(),
                instance()
            )
        }

    bind<UserCommunicationService>() with
        singleton { UserCommunicationService(instance(), instance("version")) }

    bind<ConversationService>() with singleton { ConversationService(instance(), instance()) }

    bind<MessagesHandlingService>() with
        singleton { MessagesHandlingService(instance(), instance()) }

    bind<AuthService>() with singleton {
        AuthService(proxyToken = instance("proxy-auth"))
    }

    bind<StatsFormattingService>() with singleton { StatsFormattingService(instance()) }

    bind<KLogger>("routing-logger") with singleton { createLogger("Routing") }
    bind<KLogger>("install-logger") with singleton { createLogger("KtorStartup") }
}
