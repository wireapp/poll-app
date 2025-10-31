package com.wire.apps.polls

import com.wire.apps.polls.setup.init
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080
    ) {
        init()
    }.start(wait = true)
}
