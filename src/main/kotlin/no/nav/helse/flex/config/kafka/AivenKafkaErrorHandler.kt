package no.nav.helse.flex.config.kafka

import no.nav.helse.flex.utils.errorSecure
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.listener.DefaultErrorHandler
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
        loggFeilendeBatch(
            thrownException = thrownException,
            records = data,
            listenerId = container.listenerId,
            listenerTopics = consumer.listTopics().keys,
        )
        super.handleBatch(thrownException, data, consumer, container, invokeListener)
    }

    companion object {
        private val log = slf4jLogger()

        internal fun loggFeilende(
            thrownException: Exception,
            records: MutableList<ConsumerRecord<*, *>>,
            listenerId: String? = null,
            listenerTopics: Collection<String> = emptySet(),
        ) {
            if (!skalExceptionLogges(thrownException)) {
                return
            }
            val failingRecord = records.firstOrNull()
            if (failingRecord == null) {
                log.errorSecure(
                    "Feil ved kafka listener: " +
                        mapOf(
                            "listenerId" to listenerId,
                            "listenerTopics" to listenerTopics,
                        ),
                    secureThrowable = thrownException,
                )
            } else {
                log.errorSecure(
                    "Feil ved prossesseringen av kafka hendelse: " +
                        mapOf(
                            "topic" to failingRecord.topic(),
                            "key" to failingRecord.key(),
                            "partition" to failingRecord.partition(),
                            "offset" to failingRecord.offset(),
                        ),
                    secureThrowable = thrownException,
                )
            }
        }

        internal fun loggFeilendeBatch(
            thrownException: Exception,
            records: ConsumerRecords<*, *>,
            listenerId: String? = null,
            listenerTopics: Collection<String> = emptySet(),
        ) {
            if (!skalExceptionLogges(thrownException)) {
                return
            }
            if (records.isEmpty) {
                log.errorSecure(
                    "Feil ved batch kafka listener: " +
                        mapOf(
                            "listenerId" to listenerId,
                            "listenerTopics" to listenerTopics,
                        ),
                    secureThrowable = thrownException,
                )
            } else {
                log.errorSecure(
                    "Feil ved batch prossesseringen av kafka hendelser: " +
                        mapOf(
                            "topics" to records.map { it.topic() }.distinct(),
                            "antallRecords" to records.count(),
                            "forsteOffset" to records.first().offset(),
                            "forsteKey" to records.first().key(),
                        ),
                    secureThrowable = thrownException,
                )
            }
        }

        private fun skalExceptionLogges(ex: Exception): Boolean =
            when (ex) {
                is KafkaErrorHandlerException -> ex.skalLogges
                else -> true
            }
    }
}
