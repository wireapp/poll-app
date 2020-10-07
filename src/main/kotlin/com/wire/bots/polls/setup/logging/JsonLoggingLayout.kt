package com.wire.bots.polls.setup.logging


import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.LayoutBase
import pw.forst.tools.katlib.createJson
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


/**
 * Layout logging into jsons.
 */
class JsonLoggingLayout : LayoutBase<ILoggingEvent>() {

    private companion object {
        val dateTimeFormatter: DateTimeFormatter =
            DateTimeFormatter.ISO_DATE_TIME
                .withZone(ZoneOffset.UTC)
    }

    override fun doLayout(event: ILoggingEvent): String {
        val finalMap: MutableMap<String, Any> = mutableMapOf(
            "@timestamp" to formatTime(event),
            "message" to event.formattedMessage,
            "logger" to event.loggerName,
            "level" to event.level.levelStr,
            "thread_name" to event.threadName
        )

        finalMap.includeMdc(event, INFRA_REQUEST)
        finalMap.includeMdc(event, APP_REQUEST)
        finalMap.includeMdc(event, USER_ID)

        // if this was an exception, include necessary data
        if (event.throwableProxy != null) {
            finalMap["exception"] = exception(event.throwableProxy)
        }

        return createJson(finalMap) + CoreConstants.LINE_SEPARATOR
    }

    private fun MutableMap<String, Any>.includeMdc(event: ILoggingEvent, mdcKey: String, mapKey: String = mdcKey) {
        event.mdcPropertyMap[mdcKey]?.also { mdcValue -> this[mapKey] = mdcValue }
    }

    private fun exception(proxy: IThrowableProxy) = mapOf(
        "message" to proxy.message,
        "class" to proxy.className,
        "stacktrace" to ThrowableProxyUtil.asString(proxy)
    )

    private fun formatTime(event: ILoggingEvent): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(event.timeStamp))
}
