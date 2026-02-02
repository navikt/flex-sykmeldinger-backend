package no.nav.helse.flex.utils

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.MDC

object KafkaKonsumerUtils {
    fun <R> medMdcMetadata(
        cr: ConsumerRecord<*, *>,
        block: () -> R,
    ): R {
        val mdcVars =
            mapOf(
                "kafka-key" to cr.key(),
                "kafka-topic" to cr.topic(),
                "kafka-partition" to cr.partition(),
                "kafka-offset" to cr.offset(),
            )
        return withMdcVars(mdcVars) {
            block()
        }
    }

    fun <R> medMdcMetadata(
        cr: ConsumerRecords<*, *>,
        block: () -> R,
    ): R {
        if (cr.isEmpty) {
            return block()
        }
        val recordCount = cr.count()
        val topics = cr.map { it.topic() }.distinct()
        val partitions = cr.map { it.partition() }.distinct()
        val offsets = cr.map { it.offset() }
        val firstOffset = offsets.min()
        val lastOffset = offsets.max()

        val mdcVars =
            mapOf(
                "kafka-count" to recordCount,
                "kafka-topics" to topics,
                "kafka-partitions" to partitions,
                "kafka-offset-first" to firstOffset,
                "kafka-offset-last" to lastOffset,
            )
        return withMdcVars(mdcVars) {
            block()
        }
    }

    private fun <R> withMdcVars(
        mdcVars: Map<String, Any?>,
        block: () -> R,
    ): R =
        try {
            mdcVars.forEach { (k, v) ->
                MDC.put(k, v.toString())
            }
            block()
        } finally {
            mdcVars.keys.forEach { k ->
                MDC.remove(k)
            }
        }
}
