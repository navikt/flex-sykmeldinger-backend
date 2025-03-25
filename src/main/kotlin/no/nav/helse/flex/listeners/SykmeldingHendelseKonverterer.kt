package no.nav.helse.flex.listeners

import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.producers.sykmeldingstatus.dto.ArbeidssituasjonKafkaDTO.*
import no.nav.helse.flex.producers.sykmeldingstatus.dto.JaEllerNeiKafkaDTO
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.domain.*
import org.springframework.stereotype.Component

@Component
class SykmeldingHendelseKonverterer {
    fun konverterStatusTilSykmeldingHendelse(status: SykmeldingStatusKafkaMessageDTO): SykmeldingHendelse =
        SykmeldingHendelse(
            status =
                when (status.event.statusEvent) {
                    "APEN" -> HendelseStatus.APEN
                    "AVBRUTT" -> HendelseStatus.AVBRUTT
                    "BEKREFTET" -> HendelseStatus.SENDT_TIL_NAV
                    "SENDT" -> HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    // todo: håndter bekreftet avvist
                    // "BEKREFTET_AVVIST" -> HendelseStatus.BEKREFTET_AVVIST
                    "UTGATT" -> HendelseStatus.UTGATT
                    else -> throw IllegalArgumentException("Ukjent status")
                },
            sporsmalSvar = emptyList(),
            arbeidstakerInfo =
                ArbeidstakerInfo(
                    arbeidsgiver =
                        Arbeidsgiver(
                            orgnummer = TODO(),
                            juridiskOrgnummer = TODO(),
                            orgnavn = TODO(),
                            erAktivtArbeidsforhold = TODO(),
                            narmesteLeder = TODO(),
                        ),
                ),
            brukerSvar = TODO(),
            tilleggsinfo = TODO(),
            opprettet = TODO(),
        )

    internal fun konverterSykmeldingSporsmalSvarDtoTilBrukerSvar(sykmeldingSporsmalSvarDto: SykmeldingStatusKafkaMessageDTO): BrukerSvar {
        val brukerSvar = sykmeldingSporsmalSvarDto.event.brukerSvar
        checkNotNull(brukerSvar)

        val erOpplysningeneRiktige =
            brukerSvar.erOpplysningeneRiktige.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar =
                        when (it.svar) {
                            JaEllerNeiKafkaDTO.JA -> true
                            JaEllerNeiKafkaDTO.NEI -> false
                        },
                )
            }

        val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> =
            brukerSvar.arbeidssituasjon.let { arbeidssituasjon ->
                SporsmalSvar(
                    sporsmaltekst = arbeidssituasjon.sporsmaltekst,
                    svar =
                        when (arbeidssituasjon.svar) {
                            ARBEIDSTAKER -> Arbeidssituasjon.ARBEIDSTAKER
                            FRILANSER -> Arbeidssituasjon.FRILANSER
                            NAERINGSDRIVENDE -> Arbeidssituasjon.NAERINGSDRIVENDE
                            FISKER -> Arbeidssituasjon.FISKER
                            JORDBRUKER -> Arbeidssituasjon.JORDBRUKER
                            ARBEIDSLEDIG -> Arbeidssituasjon.ARBEIDSLEDIG
                            ANNET -> Arbeidssituasjon.ANNET
                        },
                )
            }

        when (sykmeldingSporsmalSvarDto.event.brukerSvar.arbeidssituasjon.svar) {
            ARBEIDSTAKER -> {
                return ArbeidstakerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    arbeidsgiverOrgnummer =
                        brukerSvar.arbeidsgiverOrgnummer.let {
                            checkNotNull(it) { "Arbeidsgiver orgnummer er påkrevd" }
                            SporsmalSvar(
                                sporsmaltekst = it.sporsmaltekst,
                                svar = it.svar,
                            )
                        },
                    riktigNarmesteLeder =
                        brukerSvar.riktigNarmesteLeder.let {
                            checkNotNull(it) { "Riktig nærmeste leder er påkrevd" }
                            SporsmalSvar(
                                sporsmaltekst = it.sporsmaltekst,
                                svar =
                                    when (it.svar) {
                                        JaEllerNeiKafkaDTO.JA -> true
                                        JaEllerNeiKafkaDTO.NEI -> false
                                    },
                            )
                        },
                    harEgenmeldingsdager =
                        brukerSvar.harBruktEgenmeldingsdager.let {
                            checkNotNull(it) { "Har brukt egenmeldingsdager er påkrevd" }
                            SporsmalSvar(
                                sporsmaltekst = it.sporsmaltekst,
                                svar =
                                    when (it.svar) {
                                        JaEllerNeiKafkaDTO.JA -> true
                                        JaEllerNeiKafkaDTO.NEI -> false
                                    },
                            )
                        },
                    egenmeldingsdager =
                        brukerSvar.egenmeldingsdager.let {
                            checkNotNull(it) { "Egenmeldingsdager er påkrevd" }
                            SporsmalSvar(
                                sporsmaltekst = it.sporsmaltekst,
                                svar = it.svar,
                            )
                        },
                    uriktigeOpplysninger = TODO(),
                )
            }

            FRILANSER,
            NAERINGSDRIVENDE,
            FISKER,
            JORDBRUKER,
            ARBEIDSLEDIG,
            ANNET,
            -> {
                TODO()
            }
        }
    }
}
