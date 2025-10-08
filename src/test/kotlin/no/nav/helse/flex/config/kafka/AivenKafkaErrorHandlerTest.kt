package no.nav.helse.flex.config.kafka

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.flex.utils.LogMarker
import no.nav.helse.flex.utils.logger
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldHaveSingleItem
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.Marker
import ch.qos.logback.classic.Logger as LogbackLogger

class AivenKafkaErrorHandlerTest {
    private val logger = AivenKafkaErrorHandler.logger()
    private val logListAppender =
        ListAppender<ILoggingEvent>()
            .also {
                (logger as? LogbackLogger)?.addAppender(it)
            }.apply {
                start()
            }

    @AfterEach
    fun afterEach() {
        logListAppender.list.clear()
    }

    @Nested
    inner class LoggFeilende {
        @Test
        fun `logger exception`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = RuntimeException(),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().shouldHaveSingleItem()
        }

        @Test
        fun `logger ikke exception KafkaErrorHandlerException dersom spesifisert`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = KafkaErrorHandlerException(skalLogges = false, message = "", cause = RuntimeException()),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().shouldBeEmpty()
        }

        @Test
        fun `logger også til secure logs`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = RuntimeException(),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerMedMarker(LogMarker.SECURE_LOGS).shouldHaveSingleItem()
        }
    }

    @Nested
    inner class LoggFeilendeBatch {
        @Test
        fun `logger exception`() {
            AivenKafkaErrorHandler.loggFeilendeBatch(
                thrownException = RuntimeException(),
                records =
                    listOf(
                        Testdata.lagConsumerRecord(),
                    ).somConsumerRecords(),
            )
            logListAppender.eventerUtenMarkers().shouldHaveSingleItem()
        }

        @Test
        fun `logger ikke exception KafkaErrorHandlerException dersom spesifisert`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = KafkaErrorHandlerException(skalLogges = false, message = "", cause = RuntimeException()),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().shouldBeEmpty()
        }

        @Test
        fun `logger også til secure logs`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = RuntimeException(),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerMedMarker(LogMarker.SECURE_LOGS).shouldHaveSingleItem()
        }
    }

    private fun ListAppender<ILoggingEvent>.eventerUtenMarkers(): List<ILoggingEvent> = this.list.filter { it.markerList.isNullOrEmpty() }

    private fun ListAppender<ILoggingEvent>.eventerMedMarker(marker: Marker): List<ILoggingEvent> =
        this.list.filter {
            it.markerList != null && marker in it.markerList
        }

    private fun Collection<ConsumerRecord<String, String>>.somConsumerRecords(): ConsumerRecords<String, String> =
        ConsumerRecords(
            mapOf(
                TopicPartition("topic", 1) to this.toList(),
            ),
        )

    private object Testdata {
        fun lagConsumerRecord(): ConsumerRecord<String, String> =
            ConsumerRecord(
                "topic",
                1,
                1L,
                "key",
                "{}",
            )
    }
}
