package no.nav.helse.flex.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.util.UUID

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

object LogMarker {
    val SECURE_LOGS: Marker = MarkerFactory.getMarker("SECURE_LOG")
}

fun Logger.errorSecure(
    message: String,
    secureMessage: String = "",
    secureThrowable: Throwable? = null,
) {
    val secureLogId = UUID.randomUUID().toString().take(8)
    this.error("$message (SecureLogId: $secureLogId)")
    this.error(LogMarker.SECURE_LOGS, "$message (SecureLogId: $secureLogId) $secureMessage", secureThrowable)
}
