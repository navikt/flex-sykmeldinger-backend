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
    fun `logger ikke KafkaErrorHandlerException dersom spesifisert`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = KafkaErrorHandlerException(skalLogges = false, cause = RuntimeException()),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )
        logListAppender.eventerUtenMarkers().shouldBeEmpty()
    }

    @Test
    fun `logger kun årsak av KafkaErrorHandlerException`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = KafkaErrorHandlerException(cause = RuntimeException("Årsak melding")),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )
        logListAppender.eventerUtenMarkers().first().run {
            message shouldNotContain "KafkaErrorHandlerException"
            message shouldContain "RuntimeException"
        }
        logListAppender.eventerMedMarker(LogMarker.SECURE_LOGS).first().run {
            throwableProxy.message shouldBeEqualTo "Årsak melding"
        }
    }

    @Test
    fun `logger 'insecureMessage' i KafkaErrorHandlerException`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = KafkaErrorHandlerException(insecureMessage = "sikker beskjed", cause = RuntimeException()),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )
        logListAppender.eventerUtenMarkers().first().run {
            message shouldStartWith "sikker beskjed"
        }
    }

    @Test
    fun `logger 'message' kun til secure logs`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = RuntimeException("Feil melding"),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )
        logListAppender.eventerUtenMarkers().first().run {
            message shouldNotContain "Feil melding"
        }
        logListAppender.eventerMedMarker(LogMarker.SECURE_LOGS).first().run {
            message shouldContain "Feil melding"
        }
    }

    @Test
    fun `logger også til secure logs`() {
        AivenKafkaErrorHandler.loggFeilende(
            thrownException = RuntimeException(),
            records = mutableListOf(Testdata.lagConsumerRecord()),
        )
        logListAppender.eventerMedMarker(LogMarker.SECURE_LOGS).shouldHaveSingleItem()
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
