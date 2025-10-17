package no.nav.helse.flex.testutils

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.Logger
import org.slf4j.Marker
import ch.qos.logback.classic.Logger as LogbackLogger

class LoggLytter(
    logger: Logger,
) {
    private val logListAppender = ListAppender<ILoggingEvent>()

    init {
        (logger as? LogbackLogger)?.addAppender(logListAppender)
        logListAppender.start()
    }

    fun logEventer(): List<ILoggingEvent> = logListAppender.list.filter { it.markerList.isNullOrEmpty() }

    fun logEventerUtenMarkers(): List<ILoggingEvent> = logEventer().filter { it.markerList.isNullOrEmpty() }

    fun logEventerMedMarker(marker: Marker): List<ILoggingEvent> =
        logEventer().filter {
            it.markerList != null && marker in it.markerList
        }
}
