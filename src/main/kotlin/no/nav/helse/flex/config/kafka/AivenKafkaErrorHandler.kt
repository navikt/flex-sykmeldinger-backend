package no.nav.helse.flex.config.kafka

import no.nav.helse.flex.utils.errorSecure
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
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
        loggFeilende(
            thrownException = thrownException,
            records = records,
            listenerId = container.listenerId,
            listenerTopics = consumer.listTopics().keys,
        )
        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleBatch(
        thrownException: Exception,
        data: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable,
    ) {
        loggFeilende(
            thrownException = thrownException,
            records = data.toList(),
            listenerId = container.listenerId,
            listenerTopics = consumer.listTopics().keys,
        )
        super.handleBatch(thrownException, data, consumer, container, invokeListener)
    }

    companion object {
        private val log = slf4jLogger()

        fun loggFeilende(
            thrownException: Exception,
            records: Collection<ConsumerRecord<*, *>>,
            listenerId: String? = null,
            listenerTopics: Collection<String> = emptySet(),
        ) {
            val kafkaErrorHandlerException = findKafkaErrorHandlerException(thrownException)
            if (kafkaErrorHandlerException != null && !kafkaErrorHandlerException.errorHandlerLoggingEnabled) {
                return
            }
            if (records.isEmpty()) {
                log.errorSecure(
                    "Feil ved kafka listener: " +
                        mapOf(
                            "listenerId" to listenerId,
                            "listenerTopics" to listenerTopics,
                            "exceptionType" to thrownException::class.simpleName,
                        ),
                    secureMessage = thrownException.message ?: "",
                    secureThrowable = thrownException,
                )
            } else {
                log.errorSecure(
                    "Feil ved konsumering av kafka hendelse: " +
                        mapOf(
                            "topic" to records.map { it.topic() }.distinct().nullOrSingleOrList(),
                            "exceptionType" to thrownException::class.simpleName,
                            "antall" to records.count(),
                            "key" to records.map { it.key() }.limitWithEllipsis(4).nullOrSingleOrList(),
                            "offset" to records.firstOrNull()?.offset(),
                            "partition" to records.map { it.partition() }.distinct().nullOrSingleOrList(),
                            "listenerId" to listenerId,
                        ),
                    secureMessage = thrownException.message ?: "",
                    secureThrowable = thrownException,
                )
            }
        }

        private fun findKafkaErrorHandlerException(exception: Throwable): KafkaErrorHandlerException? =
            when (exception) {
                is KafkaErrorHandlerException -> exception
                is ListenerExecutionFailedException -> exception.cause?.let { findKafkaErrorHandlerException(it) }
                else -> null
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
