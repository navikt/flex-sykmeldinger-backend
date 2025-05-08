package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO.*
import no.nav.helse.flex.api.dto.JaEllerNei
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.producers.sykmeldingstatus.dto.BrukerSvarKafkaDTO
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.springframework.stereotype.Component

@Component
class SykmeldingHendelseKonverterer {
    private val log = logger()

    fun konverterStatusTilSykmeldingHendelse(
        sykmelding: Sykmelding,
        status: SykmeldingStatusKafkaMessageDTO,
    ): SykmeldingHendelse {
        val hendelseStatus = konverterStatusTilHendelseStatus(status.event.statusEvent, sykmelding.erAvvist)
        when (hendelseStatus) {
            HendelseStatus.SENDT_TIL_NAV,
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
            -> {
                if (status.event.brukerSvar == null) {
                    throw IllegalStateException("Brukersvar er påkrevd, men er null i status: ${status.serialisertTilString()}")
                        .also {
                            log.error(it.message)
                        }
                }
            }
            else -> {}
        }
        return SykmeldingHendelse(
            status = hendelseStatus,
            brukerSvar = status.event.brukerSvar?.let { konverterBrukerSvarKafkaDtoTilBrukerSvar(it) },
            // TODO: Burde mappe tilleggsinfo fra status
            tilleggsinfo = null,
            opprettet = status.event.timestamp.toInstant(),
        )
    }

    internal fun konverterStatusTilHendelseStatus(
        status: String,
        erAvvist: Boolean,
    ): HendelseStatus =
        when (status) {
            "APEN" -> HendelseStatus.APEN
            "AVBRUTT" -> HendelseStatus.AVBRUTT
            "BEKREFTET" -> {
                when (erAvvist) {
                    true -> HendelseStatus.BEKREFTET_AVVIST
                    false -> HendelseStatus.SENDT_TIL_NAV
                }
            }
            "SENDT" -> HendelseStatus.SENDT_TIL_ARBEIDSGIVER
            "UTGATT" -> HendelseStatus.UTGATT
            else -> throw IllegalArgumentException("Ukjent status: $status")
        }

    internal fun konverterBrukerSvarKafkaDtoTilBrukerSvar(brukerSvarKafkaDTO: BrukerSvarKafkaDTO): BrukerSvar {
        val erOpplysningeneRiktige =
            brukerSvarKafkaDTO.erOpplysningeneRiktige.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar =
                        when (it.svar) {
                            JaEllerNei.JA -> true
                            JaEllerNei.NEI -> false
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
                            PERMITTERT -> Arbeidssituasjon.PERMITTERT
                        },
                )
            }

        val uriktigeOpplysninger =
            brukerSvarKafkaDTO.uriktigeOpplysninger?.let { uriktigeOpplysninger ->
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
                            JaEllerNei.JA -> true
                            JaEllerNei.NEI -> false
                        },
                )
            }
        val harEgenmeldingsdager =
            brukerSvarKafkaDTO.harBruktEgenmeldingsdager?.let {
                SporsmalSvar(
                    sporsmaltekst = it.sporsmaltekst,
                    svar =
                        when (it.svar) {
                            JaEllerNei.JA -> true
                            JaEllerNei.NEI -> false
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
                            JaEllerNei.JA -> true
                            JaEllerNei.NEI -> false
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
                            JaEllerNei.JA -> true
                            JaEllerNei.NEI -> false
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
            PERMITTERT -> {
                PermittertBrukerSvar(
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
