package no.nav.helse.flex.gateways.aareghendelser

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdSynkronisering
import no.nav.helse.flex.arbeidsforhold.innhenting.RegistrertePersonerForArbeidsforhold
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.utils.KafkaKonsumerUtils
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

enum class AaregHendelseHandtering {
    OPPRETT_OPPDATER,
    SLETT,
    IGNORER,
}

data class RawHendelse(
    val key: String = "",
    val value: String,
) {
    companion object {
        fun fraConsumerRecord(cr: ConsumerRecord<String, String>): RawHendelse = RawHendelse(key = cr.key(), value = cr.value())
    }
}

@Component
class AaregHendelserConsumer(
    private val registrertePersonerForArbeidsforhold: RegistrertePersonerForArbeidsforhold,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
) {
    val log = logger()

    @WithSpan
    @KafkaListener(
        topics = ["\${AAREG_HENDELSE_TOPIC}"],
        containerFactory = "aivenKafkaBatchListenerContainerFactory",
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(consumerRecords: ConsumerRecords<String, String>) =
        KafkaKonsumerUtils.medMdcMetadata(consumerRecords) {
            val rawRecords = consumerRecords.map(RawHendelse.Companion::fraConsumerRecord)
            try {
                handterHendelser(rawRecords)
            } catch (e: Exception) {
                log.errorSecure(
                    message = "Feil ved håndtering av aareg notifikasjon, exception: ${e::class.simpleName}. Dette vil bli retryet",
                    secureThrowable = e,
                )
                throw KafkaErrorHandlerException(
                    errorHandlerLoggingEnabled = false,
                    cause = e,
                )
            }
        }

    internal fun handterHendelser(hendelser: List<RawHendelse>) {
        val arbeidsforholdHendelser =
            hendelser.mapNotNull { hendelse ->
                try {
                    objectMapper.readValue<ArbeidsforholdHendelse>(hendelse.value)
                } catch (e: Exception) {
                    if (e is MismatchedInputException && e.stringPath().endsWith("arbeidstaker.identer")) {
                        log.warn(
                            "Aareg hendelse inneholder ikke fnr. " +
                                "Dette er antagelig en bug i formindelse med Aareg endringer rundt 21.05.2025. " +
                                "Ignorerer hendelse: ${hendelse.key}",
                        )
                        null
                    } else {
                        throw KafkaErrorHandlerException(
                            message = "Feil ved deserialisering",
                            cause = e,
                        )
                    }
                }
            }
        handterArbeidsforholdHendelser(arbeidsforholdHendelser)
    }

    internal fun handterArbeidsforholdHendelser(arbeidsforholdHendelser: Collection<ArbeidsforholdHendelse>) {
        val aktuelleArbeidsforholdHendelser =
            arbeidsforholdHendelser.filter {
                avgjorHendelseshandtering(it) != AaregHendelseHandtering.IGNORER
            }

        val personerFnr = aktuelleArbeidsforholdHendelser.map { it.arbeidsforhold.arbeidstaker.getFnr() }.distinct()
        val personerFnrBatcher = personerFnr.chunked(10)
        val (synkroniserteArbeidsforhold, varighet) =
            measureTimedValue {
                personerFnrBatcher
                    .flatMap { fnrBatch ->
                        ventPaAlle(fnrBatch.map { synkroniserForPerson(it) })
                    }.fold(ArbeidsforholdSynkronisering.INGEN, ArbeidsforholdSynkronisering::plus)
            }

        if (!synkroniserteArbeidsforhold.erIngen()) {
            log.info(
                "Synkronisert arbeidsforhold ved aareg notifikasjon. " +
                    mapOf(
                        "personer" to personerFnr.size,
                        "tidMs" to varighet.inWholeMilliseconds,
                        "detaljer" to synkroniserteArbeidsforhold.toLogString(),
                    ),
            )
        }
        if (varighet > (2.seconds * personerFnr.size.coerceAtLeast(2))) {
            log.warn(
                "Tok unormalt lang tid å synkronisere arbeidsforhold. " +
                    mapOf("personer" to personerFnr.size, "tidMs" to varighet.inWholeMilliseconds),
            )
        }
    }

    internal fun synkroniserForPerson(fnr: String): CompletableFuture<ArbeidsforholdSynkronisering> {
        if (!skalSynkroniseres(fnr)) {
            return CompletableFuture.completedFuture(ArbeidsforholdSynkronisering.INGEN)
        }
        return arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPersonAsync(fnr)
    }

    fun skalSynkroniseres(fnr: String): Boolean = registrertePersonerForArbeidsforhold.erPersonRegistrert(fnr)

    private fun JsonMappingException.stringPath(): String = this.path.joinToString(".") { it.fieldName ?: it.index.toString() }

    companion object {
        internal fun avgjorHendelseshandtering(hendelse: ArbeidsforholdHendelse): AaregHendelseHandtering {
            return when (hendelse.endringstype) {
                Endringstype.Opprettelse, Endringstype.Endring -> {
                    return if (harGyldigEndringstype(hendelse)) {
                        AaregHendelseHandtering.OPPRETT_OPPDATER
                    } else {
                        AaregHendelseHandtering.IGNORER
                    }
                }

                Endringstype.Sletting -> {
                    return AaregHendelseHandtering.SLETT
                }

                Endringstype.UKJENT -> {
                    AaregHendelseHandtering.IGNORER
                }
            }
        }

        private fun harGyldigEndringstype(arbeidsforholdHendelse: ArbeidsforholdHendelse): Boolean =
            arbeidsforholdHendelse.entitetsendringer.any { endring ->
                endring == Entitetsendring.Ansettelsesdetaljer ||
                    endring == Entitetsendring.Ansettelsesperiode
            }
    }
}

private fun <T> ventPaAlle(futures: List<CompletableFuture<T>>): List<T> {
    CompletableFuture.allOf(*futures.toTypedArray()).join()
    return futures.map { it.get() }
}
