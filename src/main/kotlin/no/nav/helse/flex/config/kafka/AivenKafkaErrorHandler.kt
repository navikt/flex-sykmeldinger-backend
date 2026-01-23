package no.nav.helse.flex.config.kafka

import io.opentelemetry.api.trace.Span
import no.nav.helse.flex.utils.errorSecure
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.MDC
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.ListenerExecutionFailedException
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.util.backoff.ExponentialBackOff
import no.nav.helse.flex.utils.logger as slf4jLogger

@Component
class AivenKafkaErrorHandler :
    DefaultErrorHandler(
        null,
        ExponentialBackOff(1000L, 1.5).also {
            // 8 minutter, som er mindre enn max.poll.interval.ms på 10 minutter.
            it.maxInterval = 60_000L * 8
        },
    ) {
    // Bruker aliased logger for unngå kollisjon med CommonErrorHandler.logger(): LogAccessor.

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        medTraceContext {
            loggFeilende(
                thrownException = thrownException,
                records = records,
                listenerId = container.listenerId,
                listenerTopics = consumer.listTopics().keys,
            )
            super.handleRemaining(thrownException, records, consumer, container)
        }
    }

    override fun handleBatch(
        thrownException: Exception,
        data: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable,
    ) {
        medTraceContext {
            loggFeilende(
                thrownException = thrownException,
                records = data.toList(),
                listenerId = container.listenerId,
                listenerTopics = consumer.listTopics().keys,
            )
            super.handleBatch(thrownException, data, consumer, container, invokeListener)
        }
    }

    companion object {
        private val log = slf4jLogger()

        fun loggFeilende(
            thrownException: Exception,
            records: Collection<ConsumerRecord<*, *>>,
            listenerId: String? = null,
            listenerTopics: Collection<String> = emptySet(),
        ) {
            val relevantCauseException = findRelevantCauseException(thrownException)

            if (records.isEmpty()) {
                val message =
                    composeInsensitiveMessage(
                        throwable = thrownException,
                        defaultMessage = "Feil ved kafka listener",
                        messageSeparator = " -- ",
                    )
                log.errorSecure(
                    "$message: " +
                        mapOf(
                            "listenerId" to listenerId,
                            "listenerTopics" to listenerTopics,
                            "exceptionType" to relevantCauseException::class.simpleName,
                        ),
                    secureMessage = relevantCauseException.message ?: "",
                    secureThrowable = relevantCauseException,
                )
            } else {
                val message =
                    composeInsensitiveMessage(
                        throwable = thrownException,
                        defaultMessage = "Feil ved kafka hendelse",
                        messageSeparator = " -- ",
                    )
                log.errorSecure(
                    "$message: " +
                        mapOf(
                            "topic" to records.map { it.topic() }.distinct().nullOrSingleOrList(),
                            "exceptionType" to relevantCauseException::class.simpleName,
                            "antall" to records.count(),
                            "key" to records.map { it.key() }.limitWithEllipsis(4).nullOrSingleOrList(),
                            "offset" to records.firstOrNull()?.offset(),
                            "partition" to records.map { it.partition() }.distinct().nullOrSingleOrList(),
                            "listenerId" to listenerId,
                        ),
                    secureMessage = relevantCauseException.message ?: "",
                    secureThrowable = relevantCauseException,
                )
            }
        }

        inline fun <T> medTraceContext(block: () -> T): T {
            val currentSpan = Span.current()
            val spanContext = currentSpan.spanContext

            if (spanContext.isValid) {
                MDC.put("trace_id", spanContext.traceId)
                MDC.put("span_id", spanContext.spanId)
            }

            try {
                return block()
            } finally {
                if (spanContext.isValid) {
                    MDC.remove("trace_id")
                    MDC.remove("span_id")
                }
            }
        }

        private fun findRelevantCauseException(exception: Throwable): Throwable =
            when (exception) {
                is ListenerExecutionFailedException,
                is KafkaErrorHandlerException,
                -> exception.cause?.let { findRelevantCauseException(it) } ?: exception
                else -> exception
            }

        private fun composeInsensitiveMessage(
            throwable: Throwable,
            defaultMessage: String,
            messageSeparator: String = " -- ",
        ): String {
            val rootThrowable: Throwable? =
                if (throwable is ListenerExecutionFailedException) {
                    throwable.cause
                } else {
                    throwable
                }

            val messageParts = mutableListOf<String?>()
            var nextThrowable: Throwable? = rootThrowable

            val skipDefaultMessage = rootThrowable is KafkaErrorHandlerException && rootThrowable.message != null
            if (!skipDefaultMessage) {
                messageParts.add(defaultMessage)
            }

            while (nextThrowable != null) {
                if (nextThrowable is KafkaErrorHandlerException) {
                    messageParts.add(nextThrowable.message)
                } else {
                    messageParts.add(nextThrowable::class.simpleName)
                }
                nextThrowable = nextThrowable.cause
            }
            return messageParts.filterNotNull().joinToString(messageSeparator)
        }

        private fun List<*>.nullOrSingleOrList(): Any? =
            when (this.size) {
                0 -> null
                1 -> this.first()
                else -> this.toList()
            }

        private fun List<*>.limitWithEllipsis(n: Int): List<*> =
            if (size > n) {
                this.take(n).toList() + "..."
            } else {
                this.toList()
            }
    }
}
