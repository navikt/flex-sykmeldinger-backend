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
                "kafkaKey" to cr.key(),
                "kafkaTopic" to cr.topic(),
                "kafkaPartition" to cr.partition(),
                "kafkaOffset" to cr.offset(),
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
                "kafkaRecordCount" to recordCount,
                "kafkaTopics" to topics,
                "kafkaPartitions" to partitions,
                "kafkaFirstOffset" to firstOffset,
                "kafkaLastOffset" to lastOffset,
            )
        return withMdcVars(mdcVars) {
            block()
        }
    }

    private fun <R> withMdcVars(
        mdcVars: Map<String, Any?>,
        block: () -> R,
    ): R {
        mdcVars.forEach { (k, v) ->
            MDC.put(k, v.toString())
        }

        return try {
            block()
        } finally {
            mdcVars.keys.forEach { k ->
                MDC.remove(k)
            }
        }
    }
}
