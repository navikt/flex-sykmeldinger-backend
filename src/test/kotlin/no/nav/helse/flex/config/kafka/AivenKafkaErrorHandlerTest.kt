package no.nav.helse.flex.config.kafka

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import no.nav.helse.flex.utils.LogMarker
import no.nav.helse.flex.utils.logger
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.MDC
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

    @Test
    fun `logger trace_id og span_id fra OpenTelemetry context til MDC`() {
        val testTraceId = "0af7651916cd43dd8448eb211c80319c"
        val testSpanId = "b7ad6b7169203331"

        val spanContext =
            SpanContext.create(
                testTraceId,
                testSpanId,
                TraceFlags.getSampled(),
                TraceState.getDefault(),
            )

        val span = Span.wrap(spanContext)
        val context = Context.current().with(span)

        context.makeCurrent().use {
            AivenKafkaErrorHandler.medTraceContext {
                MDC.get("trace_id") shouldBeEqualTo testTraceId
                MDC.get("span_id") shouldBeEqualTo testSpanId
            }
        }

        MDC.get("trace_id").shouldBeNull()
        MDC.get("span_id").shouldBeNull()
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

    @Nested
    inner class HandterKafkaErrorHandlerException {
        @Test
        fun `logger insensitiveMessage`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = KafkaErrorHandlerException(insensitiveMessage = "Usikker melding"),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
            logListAppender.eventerUtenMarkers().first().run {
                message shouldStartWith "Usikker melding"
            }
        }

        @Test
        fun `logger kjede av insensitiveMessage`() {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException =
                    KafkaErrorHandlerException(
                        insensitiveMessage = "Første melding",
                        cause =
                            KafkaErrorHandlerException(
                                insensitiveMessage = "Annen melding",
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
