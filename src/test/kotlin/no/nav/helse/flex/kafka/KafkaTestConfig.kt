package no.nav.helse.flex.kafka

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility.await
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class KafkaTestConfig(
    private val aivenKafkaConfig: AivenKafkaConfig,
) {
    @Bean
    fun kafkaConsumer() = KafkaConsumer<String, String>(consumerConfig("sykmelding-group-id"))

    private fun consumerConfig(groupId: String) =
        mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ) + aivenKafkaConfig.commonConfig()
}

fun <K, V> Consumer<K, V>.lesFraTopics(
    vararg topics: String,
    ventetid: Duration = Duration.ofMillis(100),
) {
    this.subscribeHvisIkkeSubscribed(*topics)
    this.hentProduserteRecords(ventetid)
    this.unsubscribe()
}

fun <K, V> Consumer<K, V>.subscribeHvisIkkeSubscribed(vararg topics: String) {
    if (this.subscription().isEmpty()) {
        this.subscribe(listOf(*topics))
    }
}

fun <K, V> Consumer<K, V>.hentProduserteRecords(duration: Duration = Duration.ofMillis(100)): List<ConsumerRecord<K, V>> =
    this.poll(duration).also { this.commitSync() }.iterator().asSequence().toList()

fun <K, V> Consumer<K, V>.ventPåRecords(
    antall: Int,
    duration: Duration = Duration.ofSeconds(1),
): List<ConsumerRecord<K, V>> {
    val factory =
        if (antall == 0) {
            // Må vente fullt ut, ikke opp til en tid siden vi vil se at ingen blir produsert
            await().during(duration)
        } else {
            await().atMost(duration)
        }

    val alle = mutableListOf<ConsumerRecord<K, V>>()
    factory.until {
        alle.addAll(this.hentProduserteRecords())
        alle.size == antall
    }
    return alle
}
