package no.nav.helse.flex.listeners.aareghendelser

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.arbeidsforhold.innhenting.RegistrertePersonerForArbeidsforhold
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

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

    @KafkaListener(
        topics = ["\${AAREG_HENDELSE_TOPIC}"],
        containerFactory = "aivenKafkaBatchListenerContainerFactory",
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(consumerRecords: ConsumerRecords<String, String>) {
        val timeMs =
            measureTimeMillis {
                val rawRecords = consumerRecords.map(RawHendelse::fraConsumerRecord)
                try {
                    handterHendelser(rawRecords)
                } catch (e: Exception) {
                    val message = "Klarte ikke prosessere aareg hendelser. Dette vil bli retryet. Keys: ${rawRecords.map { it.key }}"
                    log.errorSecure(
                        message,
                        secureMessage = e.message ?: "",
                        secureThrowable = e,
                    )
                    throw RuntimeException(message)
                }
            }
        val totalByteSize = consumerRecords.sumOf { it.serializedValueSize() }
        if (timeMs >= 10_000 || totalByteSize >= 20_000) {
            log.warn(
                "Unormal prosessering av Aareg hendelser. Antall: ${consumerRecords.count()}" +
                    ", tid: $timeMs ms, totalByteSize: $totalByteSize bytes",
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
                        throw RuntimeException("Feil aareg hendelse format, rå hendelse: ${hendelse.value}", e)
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
        for (fnr in personerFnr) {
            synkroniserForPerson(fnr)
        }
    }

    internal fun synkroniserForPerson(fnr: String) {
        if (!skalSynkroniseres(fnr)) {
            return
        }
        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(fnr)
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
