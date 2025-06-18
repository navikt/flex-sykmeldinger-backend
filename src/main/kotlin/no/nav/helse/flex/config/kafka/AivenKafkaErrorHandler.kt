package no.nav.helse.flex.config.kafka

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
        if (failingRecord != null) {
            log.error(
                "Feil i prossesseringen av record med offset: ${failingRecord.offset()}, key: ${failingRecord.key()} på topic ${failingRecord.topic()}",
                thrownException,
            )
        } else {
            log.error("Feil i listener uten noen records", thrownException)
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
            log.error("Feil i batch listener uten noen records", thrownException)
        } else {
            val alleTopics = data.map { it.topic() }.distinct()
            val antallRecords = data.count()
            val forsteOffset = data.first().offset()
            val forsteKey = data.first().key()
            val topicStr =
                if (alleTopics.size == 1) {
                    "topic: ${alleTopics.first()}"
                } else {
                    "topics: [${alleTopics.joinToString(", ")}]"
                }
            log.error(
                "Feil i batch prossesseringen av record på $topicStr, " +
                    "første offset: $forsteOffset, antall records: $antallRecords, førsteKey: $forsteKey",
                thrownException,
            )
        }
        super.handleBatch(thrownException, data, consumer, container, invokeListener)
    }
}
