package com.wire.bots.polls

import com.wire.bots.polls.setup.init
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, 8080, module = Application::init).start()
}
