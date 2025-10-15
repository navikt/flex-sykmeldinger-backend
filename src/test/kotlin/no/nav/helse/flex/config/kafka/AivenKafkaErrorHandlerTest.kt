package no.nav.helse.flex.config.kafka

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.helse.flex.utils.LogMarker
import no.nav.helse.flex.utils.logger
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

    @Nested
    inner class HandterKafkaErrorHandlerException {
        @Test
        fun `logger insecureMessage`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = KafkaErrorHandlerException(insecureMessage = "Usikker melding"),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().first().run {
                message shouldStartWith "Usikker melding"
            }
        }

        @Test
        fun `logger kjede av insecureMessage`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException =
                    KafkaErrorHandlerException(
                        insecureMessage = "Første melding",
                        cause =
                            KafkaErrorHandlerException(
                                insecureMessage = "Annen melding",
                            ),
                    ),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().first().run {
                message shouldContain "Første melding"
                message shouldContain "Annen melding"
            }
        }

        @Test
        fun `logger årsak type`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = KafkaErrorHandlerException(cause = RuntimeException()),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().first().run {
                message shouldNotContain "KafkaErrorHandlerException"
                message shouldContain "RuntimeException"
            }
        }

        @Test
        fun `logger årsak type neders i kjede`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException =
                    KafkaErrorHandlerException(
                        cause =
                            KafkaErrorHandlerException(cause = RuntimeException()),
                    ),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().first().run {
                message shouldNotContain "KafkaErrorHandlerException"
                message shouldContain "RuntimeException"
            }
        }

        @Test
        fun `logger årsak melding til secure logs`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = KafkaErrorHandlerException(cause = RuntimeException("Årsak melding")),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerMedMarker(LogMarker.SECURE_LOGS).first().run {
                throwableProxy.message shouldBeEqualTo "Årsak melding"
            }
        }

        @Test
        fun `logger kun årsak av ListenerExecutionFailedException`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = ListenerExecutionFailedException("", RuntimeException("Årsak melding")),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().first().run {
                message shouldNotContain "ListenerExecutionFailedException"
                message shouldContain "RuntimeException"
            }
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
