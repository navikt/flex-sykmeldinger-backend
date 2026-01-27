package no.nav.helse.flex.config.kafka

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.flex.utils.LogMarker
import no.nav.helse.flex.utils.logger
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
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

    @Test
    fun `logger exception`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = RuntimeException(),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )
        logListAppender.eventerUtenMarkers().shouldHaveSingleItem()
    }

    @Test
    fun `logger 'message' kun til team logs`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = RuntimeException("Feil melding"),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )
        logListAppender.eventerUtenMarkers().first().run {
            message shouldNotContain "Feil melding"
        }
        logListAppender.eventerMedMarker(LogMarker.TEAM_LOG).first().run {
            message shouldContain "Feil melding"
        }
    }

    @Test
    fun `logger også til team logs`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = RuntimeException(),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )
        logListAppender.eventerMedMarker(LogMarker.TEAM_LOG).shouldHaveSingleItem()
    }

    @Test
    fun `logger uten trace_id og span_id når det ikke finnes gyldig OpenTelemetry span`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = RuntimeException("Test exception"),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )

        logListAppender.eventerUtenMarkers().first().run {
            mdcPropertyMap.containsKey("trace_id").shouldBeFalse()
            mdcPropertyMap.containsKey("span_id").shouldBeFalse()
        }
    }

    private fun ListAppender<ILoggingEvent>.eventerUtenMarkers(): List<ILoggingEvent> = this.list.filter { it.markerList.isNullOrEmpty() }

    private fun ListAppender<ILoggingEvent>.eventerMedMarker(marker: Marker): List<ILoggingEvent> =
        this.list.filter {
            it.markerList != null && marker in it.markerList
        }

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
