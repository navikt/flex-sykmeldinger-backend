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
    val log = slf4jLogger()

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        val failingRecord = records.firstOrNull()
        if (failingRecord == null) {
            log.errorSecure(
                "Feil ved kafka listener: " +
                    mapOf(
                        "listenerId" to container.listenerId,
                        "listenerTopics" to consumer.listTopics().keys,
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

        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleBatch(
        thrownException: Exception,
        data: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable,
    ) {
        if (data.isEmpty) {
            log.errorSecure(
                "Feil ved batch kafka listener: " +
                    mapOf(
                        "listenerId" to container.listenerId,
                        "listenerTopics" to consumer.listTopics().keys,
                    ),
                secureThrowable = thrownException,
            )
        } else {
            log.errorSecure(
                "Feil ved batch prossesseringen av kafka hendelser: " +
                    mapOf(
                        "topics" to data.map { it.topic() }.distinct(),
                        "antallRecords" to data.count(),
                        "forsteOffset" to data.first().offset(),
                        "forsteKey" to data.first().key(),
                    ),
                secureThrowable = thrownException,
            )
        }
        super.handleBatch(thrownException, data, consumer, container, invokeListener)
    }
}
