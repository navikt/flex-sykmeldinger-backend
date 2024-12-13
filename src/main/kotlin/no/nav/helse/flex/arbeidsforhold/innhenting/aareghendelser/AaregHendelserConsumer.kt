package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.arbeidsforhold.innhenting.RegistrertePersonerForArbeidsforhold
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

enum class AaregHendelseHandtering {
    OPPRETT_OPPDATER,
    SLETT,
    IGNORER,
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
        log.info("Mottok ${consumerRecords.count()} aareg hendelse records")

        var totalByteSize = 0
        val time =
            measureTimeMillis {
                consumerRecords.forEach { consumerRecord ->
                    try {
                        totalByteSize += consumerRecord.serializedValueSize()
                        val record = consumerRecord.value()
                        val hendelse: ArbeidsforholdHendelse = objectMapper.readValue(record)
                        handterHendelse(hendelse)
                    } catch (e: Exception) {
                        log.error("Klarte ikke prosessere record med key: ${consumerRecord.key()}. Dette vil bli retryet.")
                        throw e
                    }
                }
            }

        log.info("Prossesserte ${consumerRecords.count()} records, med størrelse $totalByteSize bytes, iløpet av $time millisekunder")
    }

    fun handterHendelse(hendelse: ArbeidsforholdHendelse) {
        val fnr = hendelse.arbeidsforhold.arbeidstaker.getFnr()

        if (!skalSynkroniseres(fnr)) {
            return
        }

        val hendelseHandtering = avgjorHendelseshandtering(hendelse)

        when (hendelseHandtering) {
            AaregHendelseHandtering.OPPRETT_OPPDATER -> {
                opprettEllerEndreArbeidsforhold(fnr)
            }

            AaregHendelseHandtering.SLETT -> {
                val navArbeidsforholdId = hendelse.arbeidsforhold.navArbeidsforholdId
                arbeidsforholdInnhentingService.slettArbeidsforhold(navArbeidsforholdId)
            }

            else -> {}
        }
    }

    private fun opprettEllerEndreArbeidsforhold(fnr: String) {
        val resultat = arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(fnr)
        log.info(
            "Arbeidsforhold endret: Opprettet ${resultat.skalOpprettes.count()}. " +
                "Oppdaterte ${resultat.skalOppdateres.count()}. " +
                "Slettet ${resultat.skalSlettes.count()}.",
        )
    }

    fun skalSynkroniseres(fnr: String): Boolean {
        return registrertePersonerForArbeidsforhold.erPersonRegistrert(fnr)
    }

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

        private fun harGyldigEndringstype(arbeidsforholdHendelse: ArbeidsforholdHendelse): Boolean {
            return arbeidsforholdHendelse.entitetsendringer.any { endring ->
                endring == Entitetsendring.Ansettelsesdetaljer ||
                    endring == Entitetsendring.Ansettelsesperiode
            }
        }
    }
}
