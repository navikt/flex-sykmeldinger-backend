package no.nav.helse.flex.config.kafka

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.flex.utils.LogMarker
import no.nav.helse.flex.utils.logger
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.slf4j.Marker
import org.springframework.kafka.listener.ListenerExecutionFailedException
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

    @TestFactory
    fun `burde ikke logge dersom KafkaErrorHandlerException med errorHandlerLoggingEnabled = false`() =
        listOf(
            KafkaErrorHandlerException(errorHandlerLoggingEnabled = false),
            ListenerExecutionFailedException(
                "",
                KafkaErrorHandlerException(errorHandlerLoggingEnabled = false),
            ),
        ).map { ex ->
            DynamicTest.dynamicTest(ex::class.simpleName) {
                AivenKafkaErrorHandler.loggFeilende(
                    thrownException = ex,
                    records = mutableListOf(Testdata.lagConsumerRecord()),
                )
                logListAppender.list.shouldBeEmpty()
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
