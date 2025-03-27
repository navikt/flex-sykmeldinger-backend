package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.producers.sykmeldingstatus.dto.ArbeidssituasjonKafkaDTO.*
import no.nav.helse.flex.producers.sykmeldingstatus.dto.BrukerSvarKafkaDTO
import no.nav.helse.flex.producers.sykmeldingstatus.dto.JaEllerNeiKafkaDTO
import no.nav.helse.flex.sykmelding.domain.*
import org.springframework.stereotype.Component

@Component
class SykmeldingHendelseKonverterer {
    fun konverterStatusTilSykmeldingHendelse(status: SykmeldingStatusKafkaMessageDTO): SykmeldingHendelse {
        checkNotNull(status.event.brukerSvar) { "Brukersvar er påkrevd" }
        return SykmeldingHendelse(
            status = konverterStatusTilHendelseStatus(status.event.statusEvent),
            sporsmalSvar = emptyList(),
            arbeidstakerInfo = null,
            brukerSvar = konverterBrukerSvarKafkaDtoTilBrukerSvar(status.event.brukerSvar),
            tilleggsinfo = null,
            opprettet = status.event.timestamp.toInstant(),
        )
    }

    internal fun konverterStatusTilHendelseStatus(status: String): HendelseStatus =
        when (status) {
            "APEN" -> HendelseStatus.APEN
            "AVBRUTT" -> HendelseStatus.AVBRUTT
            "BEKREFTET" -> {
                HendelseStatus.SENDT_TIL_NAV
                // todo HendelseStatus.BEKREFTET_AVVIST
            }
            "SENDT" -> HendelseStatus.SENDT_TIL_ARBEIDSGIVER
            "UTGATT" -> HendelseStatus.UTGATT
            else -> throw IllegalArgumentException("Ukjent status")
        }

    internal fun konverterBrukerSvarKafkaDtoTilBrukerSvar(brukerSvarKafkaDTO: BrukerSvarKafkaDTO): BrukerSvar {
        val erOpplysningeneRiktige =
            brukerSvarKafkaDTO.erOpplysningeneRiktige.let {
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
            brukerSvarKafkaDTO.arbeidssituasjon.let { arbeidssituasjon ->
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

        val uriktigeOpplysninger =
            brukerSvarKafkaDTO.uriktigeOpplysninger.let { uriktigeOpplysninger ->
                checkNotNull(uriktigeOpplysninger) { "Uriktige opplysninger er påkrevd" }
                SporsmalSvar(
                    sporsmaltekst = uriktigeOpplysninger.sporsmaltekst,
                    svar =
                        uriktigeOpplysninger.svar.map {
                            UriktigeOpplysning.valueOf(it.name)
                        },
                )
            }

        val arbeidsgiverOrgnummer =
            brukerSvarKafkaDTO.arbeidsgiverOrgnummer?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar = it.svar,
                )
            }

        val riktigNarmesteLeder =
            brukerSvarKafkaDTO.riktigNarmesteLeder?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar =
                        when (it.svar) {
                            JaEllerNeiKafkaDTO.JA -> true
                            JaEllerNeiKafkaDTO.NEI -> false
                        },
                )
            }
        val harEgenmeldingsdager =
            brukerSvarKafkaDTO.harBruktEgenmeldingsdager?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar =
                        when (it.svar) {
                            JaEllerNeiKafkaDTO.JA -> true
                            JaEllerNeiKafkaDTO.NEI -> false
                        },
                )
            }
        val egenmeldingsdager =
            brukerSvarKafkaDTO.egenmeldingsdager?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar = it.svar,
                )
            }

        val harBruktEgenmelding =
            brukerSvarKafkaDTO.harBruktEgenmelding?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar =
                        when (it.svar) {
                            JaEllerNeiKafkaDTO.JA -> true
                            JaEllerNeiKafkaDTO.NEI -> false
                        },
                )
            }

        val egenmeldingsperioder =
            brukerSvarKafkaDTO.egenmeldingsperioder?.let { perioder ->
                SporsmalSvar(
                    sporsmaltekst = perioder.sporsmaltekst,
                    svar =
                        perioder.svar.map {
                            Egenmeldingsperiode(
                                fom = it.fom,
                                tom = it.tom,
                            )
                        },
                )
            }

        val harForsikring =
            brukerSvarKafkaDTO.harForsikring?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar =
                        when (it.svar) {
                            JaEllerNeiKafkaDTO.JA -> true
                            JaEllerNeiKafkaDTO.NEI -> false
                        },
                )
            }

        val lottOgHyre =
            brukerSvarKafkaDTO.fisker?.lottOgHyre?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar = FiskerLottOgHyre.valueOf(it.svar.name),
                )
            }

        val blad =
            brukerSvarKafkaDTO.fisker?.blad?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar = FiskerBlad.valueOf(it.svar.name),
                )
            }

        return when (brukerSvarKafkaDTO.arbeidssituasjon.svar) {
            ARBEIDSTAKER -> {
                ArbeidstakerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer ?: throw IllegalStateException("Arbeidsgiver orgnummer er påkrevd"),
                    riktigNarmesteLeder = riktigNarmesteLeder ?: throw IllegalStateException("Riktig nærmeste leder er påkrevd"),
                    harEgenmeldingsdager = harEgenmeldingsdager ?: throw IllegalStateException("Har egenmeldingsdager er påkrevd"),
                    egenmeldingsdager = egenmeldingsdager,
                )
            }
            FISKER -> {
                FiskerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    lottOgHyre = lottOgHyre ?: throw IllegalStateException("Lott eller hyre er påkrevd"),
                    blad = blad ?: throw IllegalStateException("Blad er påkrevd"),
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                    riktigNarmesteLeder = riktigNarmesteLeder,
                    harEgenmeldingsdager = harEgenmeldingsdager,
                    egenmeldingsdager = egenmeldingsdager,
                    harBruktEgenmelding = harBruktEgenmelding,
                    egenmeldingsperioder = egenmeldingsperioder,
                    harForsikring = harForsikring,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                )
            }

            FRILANSER -> {
                FrilanserBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    harBruktEgenmelding = harBruktEgenmelding ?: throw IllegalStateException("Har brukt egenmelding er påkrevd"),
                    egenmeldingsperioder = egenmeldingsperioder,
                    harForsikring = harForsikring ?: throw IllegalStateException("Har forsikring er påkrevd"),
                    uriktigeOpplysninger = uriktigeOpplysninger,
                )
            }
            NAERINGSDRIVENDE -> {
                NaringsdrivendeBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    harBruktEgenmelding = harBruktEgenmelding ?: throw IllegalStateException("Har brukt egenmelding er påkrevd"),
                    egenmeldingsperioder = egenmeldingsperioder,
                    harForsikring = harForsikring ?: throw IllegalStateException("Har forsikring er påkrevd"),
                    uriktigeOpplysninger = uriktigeOpplysninger,
                )
            }
            JORDBRUKER -> {
                JordbrukerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                    harBruktEgenmelding = harBruktEgenmelding ?: throw IllegalStateException("Har brukt egenmelding er påkrevd"),
                    egenmeldingsperioder = egenmeldingsperioder,
                    harForsikring = harForsikring ?: throw IllegalStateException("Har forsikring er påkrevd"),
                )
            }
            ARBEIDSLEDIG -> {
                ArbeidsledigBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    arbeidsledigFraOrgnummer = arbeidsgiverOrgnummer,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                )
            }
            ANNET -> {
                AnnetArbeidssituasjonBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                )
            }
        }
    }
}
