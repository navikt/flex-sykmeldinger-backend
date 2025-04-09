package no.nav.helse.flex.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

object LogMarker {
    val SECURE_LOGS: Marker = MarkerFactory.getMarker("SECURE_LOG")
}
