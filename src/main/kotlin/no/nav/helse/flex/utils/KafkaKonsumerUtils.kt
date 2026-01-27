package no.nav.helse.flex.utils

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.MDC

object KafkaKonsumerUtils {
    fun medMdcMetadata(
        cr: ConsumerRecord<*, *>,
        block: () -> Unit,
    ) {
        val mdcVars =
            mapOf(
                "kafkaKey" to cr.key(),
                "kafkaTopic" to cr.topic(),
                "kafkaPartition" to cr.partition(),
                "kafkaOffset" to cr.offset(),
            )
        withMdcVars(mdcVars) {
            block()
        }
    }

    fun medMdcMetadata(
        cr: ConsumerRecords<*, *>,
        block: () -> Unit,
    ) {
        if (cr.isEmpty) {
            block()
            return
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
        withMdcVars(mdcVars) {
            block()
        }
    }

    private fun withMdcVars(
        mdcVars: Map<String, Any?>,
        block: () -> Unit,
    ) {
        mdcVars.forEach { (k, v) ->
            MDC.put(k, v.toString())
        }

        block()

        mdcVars.keys.forEach { k ->
            MDC.remove(k)
        }
    }
}
