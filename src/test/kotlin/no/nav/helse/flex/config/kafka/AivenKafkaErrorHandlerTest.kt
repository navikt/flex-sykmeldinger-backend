package no.nav.helse.flex.config.kafka

import no.nav.helse.flex.testutils.fangLogger
import no.nav.helse.flex.utils.LogMarker
import no.nav.helse.flex.utils.logger
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.kafka.listener.ListenerExecutionFailedException

class AivenKafkaErrorHandlerTest {
    @Test
    fun `logger exception`() {
        fangLogger(AivenKafkaErrorHandler.logger()) {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = RuntimeException(),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
        }.logEventerUtenMarkers().shouldHaveSingleItem()
    }

    @Test
    fun `logger 'message' kun til secure logs`() {
        fangLogger(AivenKafkaErrorHandler.logger()) {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = RuntimeException("Feil melding"),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
        }.run {
            logEventerUtenMarkers().first().run {
                message shouldNotContain "Feil melding"
            }
            logEventerMedMarker(LogMarker.SECURE_LOGS).first().run {
                message shouldContain "Feil melding"
            }
        }
    }

    @Test
    fun `logger også til secure logs`() {
        fangLogger(AivenKafkaErrorHandler.logger()) {
            AivenKafkaErrorHandler.loggFeilende(
                thrownException = RuntimeException(),
                records = mutableListOf(Testdata.lagConsumerRecord()),
            )
        }.logEventerMedMarker(LogMarker.SECURE_LOGS).shouldHaveSingleItem()
    }

    @Nested
    inner class HandterKafkaErrorHandlerException {
        @Test
        fun `logger insensitiveMessage`() {
            fangLogger(AivenKafkaErrorHandler.logger()) {
                AivenKafkaErrorHandler.loggFeilende(
                    thrownException = KafkaErrorHandlerException(insensitiveMessage = "Usikker melding"),
                    records = mutableListOf(Testdata.lagConsumerRecord()),
                )
            }.logEventerUtenMarkers().first().run {
                message shouldStartWith "Usikker melding"
            }
        }

        @Test
        fun `logger kjede av insensitiveMessage`() {
            fangLogger(AivenKafkaErrorHandler.logger()) {
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
            }.logEventerUtenMarkers().first().run {
                message shouldContain "Første melding"
                message shouldContain "Annen melding"
            }
        }

        @Test
        fun `logger årsak type`() {
            fangLogger(AivenKafkaErrorHandler.logger()) {
                AivenKafkaErrorHandler.loggFeilende(
                    thrownException = KafkaErrorHandlerException(cause = RuntimeException()),
                    records = mutableListOf(Testdata.lagConsumerRecord()),
                )
            }.logEventerUtenMarkers().first().run {
                message shouldNotContain "KafkaErrorHandlerException"
                message shouldContain "RuntimeException"
            }
        }

        @Test
        fun `logger årsak type neders i kjede`() {
            fangLogger(AivenKafkaErrorHandler.logger()) {
                AivenKafkaErrorHandler.loggFeilende(
                    thrownException =
                        KafkaErrorHandlerException(
                            cause =
                                KafkaErrorHandlerException(cause = RuntimeException()),
                        ),
                    records = mutableListOf(Testdata.lagConsumerRecord()),
                )
            }.logEventerUtenMarkers().first().run {
                message shouldNotContain "KafkaErrorHandlerException"
                message shouldContain "RuntimeException"
            }
        }

        @Test
        fun `logger årsak melding til secure logs`() {
            fangLogger(AivenKafkaErrorHandler.logger()) {
                AivenKafkaErrorHandler.loggFeilende(
                    thrownException = KafkaErrorHandlerException(cause = RuntimeException("Årsak melding")),
                    records = mutableListOf(Testdata.lagConsumerRecord()),
                )
            }.logEventerMedMarker(LogMarker.SECURE_LOGS).first().run {
                throwableProxy.message shouldBeEqualTo "Årsak melding"
            }
        }

        @Test
        fun `logger kun årsak av ListenerExecutionFailedException`() {
            fangLogger(AivenKafkaErrorHandler.logger()) {
                AivenKafkaErrorHandler.loggFeilende(
                    thrownException = ListenerExecutionFailedException("", RuntimeException("Årsak melding")),
                    records = mutableListOf(Testdata.lagConsumerRecord()),
                )
            }.logEventerUtenMarkers().first().run {
                message shouldNotContain "ListenerExecutionFailedException"
                message shouldContain "RuntimeException"
            }
        }
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
