package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO.*
import no.nav.helse.flex.api.dto.JaEllerNei
import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.ArbeidsgiverStatusKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.BrukerSvarKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.TidligereArbeidsgiverKafkaDTO
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingHendelseFraKafkaKonverterer(
    private val nowFactory: Supplier<Instant>,
) {
    fun konverterSykmeldingHendelseFraKafkaDTO(
        status: SykmeldingStatusKafkaDTO,
        erSykmeldingAvvist: Boolean = false,
        source: String? = null,
    ): SykmeldingHendelse {
        val hendelseStatus = konverterStatusTilHendelseStatus(status.statusEvent, erAvvist = erSykmeldingAvvist)
        when (hendelseStatus) {
            HendelseStatus.SENDT_TIL_NAV,
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
            -> {
                requireNotNull(status.brukerSvar) {
                    "Brukersvar er påkrevd for SENDT_TIL_NAV og SENDT_TIL_ARBEIDSGIVER"
                }
            }
            else -> {}
        }
        val brukerSvar = status.brukerSvar?.let { konverterBrukerSvarKafkaDtoTilBrukerSvar(it) }
        val tilleggsinfo =
            brukerSvar?.let { brukerSvar ->
                konverterTilTilleggsinfo(
                    arbeidssituasjon = brukerSvar.arbeidssituasjon,
                    arbeidsgiver = status.arbeidsgiver,
                    tidligereArbeidsgiver = status.tidligereArbeidsgiver,
                )
            }

        return SykmeldingHendelse(
            status = hendelseStatus,
            brukerSvar = status.brukerSvar?.let { konverterBrukerSvarKafkaDtoTilBrukerSvar(it) },
            tilleggsinfo = tilleggsinfo,
            source = source,
            hendelseOpprettet = status.timestamp.toInstant(),
            lokaltOpprettet = nowFactory.get(),
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
                requireNotNull(arbeidsgiverOrgnummer) { "Arbeidsgiver orgnummer er påkrevd for ARBEIDSTAKER" }
                ArbeidstakerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                    riktigNarmesteLeder = riktigNarmesteLeder,
                    harEgenmeldingsdager = harEgenmeldingsdager,
                    egenmeldingsdager = egenmeldingsdager,
                )
            }
            FISKER -> {
                requireNotNull(lottOgHyre) { "Lott eller hyre er påkrevd for FISKER" }
                requireNotNull(blad) { "Blad er påkrevd for FISKER" }
                FiskerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    lottOgHyre = lottOgHyre,
                    blad = blad,
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
                    harBruktEgenmelding = harBruktEgenmelding,
                    egenmeldingsperioder = egenmeldingsperioder,
                    harForsikring = harForsikring,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                )
            }
            NAERINGSDRIVENDE -> {
                NaringsdrivendeBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    harBruktEgenmelding = harBruktEgenmelding,
                    egenmeldingsperioder = egenmeldingsperioder,
                    harForsikring = harForsikring,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                )
            }
            JORDBRUKER -> {
                JordbrukerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige,
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    uriktigeOpplysninger = uriktigeOpplysninger,
                    harBruktEgenmelding = harBruktEgenmelding,
                    egenmeldingsperioder = egenmeldingsperioder,
                    harForsikring = harForsikring,
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

    internal fun konverterTilTilleggsinfo(
        arbeidssituasjon: Arbeidssituasjon,
        arbeidsgiver: ArbeidsgiverStatusKafkaDTO? = null,
        tidligereArbeidsgiver: TidligereArbeidsgiverKafkaDTO? = null,
    ): Tilleggsinfo =
        when (arbeidssituasjon) {
            Arbeidssituasjon.ARBEIDSTAKER -> {
                requireNotNull(arbeidsgiver) { "Arbeidsgiver er påkrevd for arbeidstaker" }
                ArbeidstakerTilleggsinfo(arbeidsgiver = konverterArbeidsgiver(arbeidsgiver))
            }
            Arbeidssituasjon.ARBEIDSLEDIG -> {
                ArbeidsledigTilleggsinfo(
                    tidligereArbeidsgiver = tidligereArbeidsgiver?.let { konverterTidligereArbeidsgiver(it) },
                )
            }
            Arbeidssituasjon.PERMITTERT -> {
                PermittertTilleggsinfo(
                    tidligereArbeidsgiver = tidligereArbeidsgiver?.let { konverterTidligereArbeidsgiver(it) },
                )
            }
            Arbeidssituasjon.FISKER ->
                FiskerTilleggsinfo(
                    arbeidsgiver = arbeidsgiver?.let { konverterArbeidsgiver(it) },
                )
            Arbeidssituasjon.FRILANSER -> FrilanserTilleggsinfo
            Arbeidssituasjon.NAERINGSDRIVENDE -> NaringsdrivendeTilleggsinfo
            Arbeidssituasjon.JORDBRUKER -> JordbrukerTilleggsinfo
            Arbeidssituasjon.ANNET -> AnnetArbeidssituasjonTilleggsinfo
        }

    internal fun konverterArbeidsgiver(arbeidsgiver: ArbeidsgiverStatusKafkaDTO): Arbeidsgiver {
        requireNotNull(arbeidsgiver.juridiskOrgnummer) { "Arbeidsgiver juridiskOrgnummer er påkrevd" }
        return Arbeidsgiver(
            orgnummer = arbeidsgiver.orgnummer,
            juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
            orgnavn = arbeidsgiver.orgNavn,
            // TODO: Hvordan finner vi ut av dette?
            erAktivtArbeidsforhold = true,
            // TODO: Trenger vi dette?
            narmesteLeder = null,
        )
    }

    internal fun konverterTidligereArbeidsgiver(tidligereArbeidsgiver: TidligereArbeidsgiverKafkaDTO): TidligereArbeidsgiver {
        requireNotNull(tidligereArbeidsgiver.orgnummer) { "Tidligere arbeidsgiver orgnummer er påkrevd" }
        requireNotNull(tidligereArbeidsgiver.orgNavn) { "Tidligere arbeidsgiver orgnavn er påkrevd" }
        return TidligereArbeidsgiver(
            orgnummer = tidligereArbeidsgiver.orgnummer,
            orgNavn = tidligereArbeidsgiver.orgNavn,
        )
    }
}
