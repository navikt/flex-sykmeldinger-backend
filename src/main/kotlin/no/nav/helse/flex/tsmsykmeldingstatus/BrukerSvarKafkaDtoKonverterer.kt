package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.api.dto.JaEllerNei
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.BrukerSvarKafkaDTO
import java.time.LocalDate

object BrukerSvarKafkaDtoKonverterer {
    fun tilBrukerSvar(brukerSvarKafkaDTO: BrukerSvarKafkaDTO): BrukerSvar {
        val alleBrukerSvar = tilAlleBrukerSvar(brukerSvarKafkaDTO)

        return when (brukerSvarKafkaDTO.arbeidssituasjon.svar) {
            ArbeidssituasjonDTO.ARBEIDSTAKER -> {
                requireNotNull(alleBrukerSvar.arbeidsgiverOrgnummer) { "Arbeidsgiver orgnummer er påkrevd for ARBEIDSTAKER" }
                ArbeidstakerBrukerSvar(
                    erOpplysningeneRiktige = alleBrukerSvar.erOpplysningeneRiktige,
                    arbeidssituasjon = alleBrukerSvar.arbeidssituasjon,
                    uriktigeOpplysninger = alleBrukerSvar.uriktigeOpplysninger,
                    arbeidsgiverOrgnummer = alleBrukerSvar.arbeidsgiverOrgnummer,
                    riktigNarmesteLeder = alleBrukerSvar.riktigNarmesteLeder,
                    harEgenmeldingsdager = alleBrukerSvar.harEgenmeldingsdager,
                    egenmeldingsdager = alleBrukerSvar.egenmeldingsdager,
                )
            }
            ArbeidssituasjonDTO.FISKER -> {
                requireNotNull(alleBrukerSvar.lottOgHyre) { "Lott eller hyre er påkrevd for FISKER" }
                requireNotNull(alleBrukerSvar.blad) { "Blad er påkrevd for FISKER" }
                FiskerBrukerSvar(
                    erOpplysningeneRiktige = alleBrukerSvar.erOpplysningeneRiktige,
                    arbeidssituasjon = alleBrukerSvar.arbeidssituasjon,
                    lottOgHyre = alleBrukerSvar.lottOgHyre,
                    blad = alleBrukerSvar.blad,
                    arbeidsgiverOrgnummer = alleBrukerSvar.arbeidsgiverOrgnummer,
                    riktigNarmesteLeder = alleBrukerSvar.riktigNarmesteLeder,
                    harEgenmeldingsdager = alleBrukerSvar.harEgenmeldingsdager,
                    egenmeldingsdager = alleBrukerSvar.egenmeldingsdager,
                    harBruktEgenmelding = alleBrukerSvar.harBruktEgenmelding,
                    egenmeldingsperioder = alleBrukerSvar.egenmeldingsperioder,
                    harForsikring = alleBrukerSvar.harForsikring,
                    uriktigeOpplysninger = alleBrukerSvar.uriktigeOpplysninger,
                )
            }

            ArbeidssituasjonDTO.FRILANSER -> {
                FrilanserBrukerSvar(
                    erOpplysningeneRiktige = alleBrukerSvar.erOpplysningeneRiktige,
                    arbeidssituasjon = alleBrukerSvar.arbeidssituasjon,
                    harBruktEgenmelding = alleBrukerSvar.harBruktEgenmelding,
                    egenmeldingsperioder = alleBrukerSvar.egenmeldingsperioder,
                    harForsikring = alleBrukerSvar.harForsikring,
                    uriktigeOpplysninger = alleBrukerSvar.uriktigeOpplysninger,
                )
            }
            ArbeidssituasjonDTO.NAERINGSDRIVENDE -> {
                NaringsdrivendeBrukerSvar(
                    erOpplysningeneRiktige = alleBrukerSvar.erOpplysningeneRiktige,
                    arbeidssituasjon = alleBrukerSvar.arbeidssituasjon,
                    harBruktEgenmelding = alleBrukerSvar.harBruktEgenmelding,
                    egenmeldingsperioder = alleBrukerSvar.egenmeldingsperioder,
                    harForsikring = alleBrukerSvar.harForsikring,
                    uriktigeOpplysninger = alleBrukerSvar.uriktigeOpplysninger,
                )
            }
            ArbeidssituasjonDTO.JORDBRUKER -> {
                JordbrukerBrukerSvar(
                    erOpplysningeneRiktige = alleBrukerSvar.erOpplysningeneRiktige,
                    arbeidssituasjon = alleBrukerSvar.arbeidssituasjon,
                    uriktigeOpplysninger = alleBrukerSvar.uriktigeOpplysninger,
                    harBruktEgenmelding = alleBrukerSvar.harBruktEgenmelding,
                    egenmeldingsperioder = alleBrukerSvar.egenmeldingsperioder,
                    harForsikring = alleBrukerSvar.harForsikring,
                )
            }
            ArbeidssituasjonDTO.ARBEIDSLEDIG -> {
                ArbeidsledigBrukerSvar(
                    erOpplysningeneRiktige = alleBrukerSvar.erOpplysningeneRiktige,
                    arbeidssituasjon = alleBrukerSvar.arbeidssituasjon,
                    arbeidsledigFraOrgnummer = alleBrukerSvar.arbeidsgiverOrgnummer,
                    uriktigeOpplysninger = alleBrukerSvar.uriktigeOpplysninger,
                )
            }
            ArbeidssituasjonDTO.PERMITTERT -> {
                PermittertBrukerSvar(
                    erOpplysningeneRiktige = alleBrukerSvar.erOpplysningeneRiktige,
                    arbeidssituasjon = alleBrukerSvar.arbeidssituasjon,
                    arbeidsledigFraOrgnummer = alleBrukerSvar.arbeidsgiverOrgnummer,
                    uriktigeOpplysninger = alleBrukerSvar.uriktigeOpplysninger,
                )
            }
            ArbeidssituasjonDTO.ANNET -> {
                AnnetArbeidssituasjonBrukerSvar(
                    erOpplysningeneRiktige = alleBrukerSvar.erOpplysningeneRiktige,
                    arbeidssituasjon = alleBrukerSvar.arbeidssituasjon,
                    uriktigeOpplysninger = alleBrukerSvar.uriktigeOpplysninger,
                )
            }
        }
    }

    internal fun tilAlleBrukerSvar(brukerSvarKafkaDTO: BrukerSvarKafkaDTO): AlleBrukerSvar =
        AlleBrukerSvar(
            erOpplysningeneRiktige =
                brukerSvarKafkaDTO.erOpplysningeneRiktige.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = it.svar.tilBoolean(),
                    )
                },
            arbeidssituasjon =
                brukerSvarKafkaDTO.arbeidssituasjon.let { arbeidssituasjon ->
                    SporsmalSvar(
                        sporsmaltekst = arbeidssituasjon.sporsmaltekst,
                        svar =
                            when (arbeidssituasjon.svar) {
                                ArbeidssituasjonDTO.ARBEIDSTAKER -> Arbeidssituasjon.ARBEIDSTAKER
                                ArbeidssituasjonDTO.FRILANSER -> Arbeidssituasjon.FRILANSER
                                ArbeidssituasjonDTO.NAERINGSDRIVENDE -> Arbeidssituasjon.NAERINGSDRIVENDE
                                ArbeidssituasjonDTO.FISKER -> Arbeidssituasjon.FISKER
                                ArbeidssituasjonDTO.JORDBRUKER -> Arbeidssituasjon.JORDBRUKER
                                ArbeidssituasjonDTO.ARBEIDSLEDIG -> Arbeidssituasjon.ARBEIDSLEDIG
                                ArbeidssituasjonDTO.ANNET -> Arbeidssituasjon.ANNET
                                ArbeidssituasjonDTO.PERMITTERT -> Arbeidssituasjon.PERMITTERT
                            },
                    )
                },
            arbeidsgiverOrgnummer =
                brukerSvarKafkaDTO.arbeidsgiverOrgnummer?.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = it.svar,
                    )
                },
            riktigNarmesteLeder =
                brukerSvarKafkaDTO.riktigNarmesteLeder?.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = it.svar.tilBoolean(),
                    )
                },
            harEgenmeldingsdager =
                brukerSvarKafkaDTO.harBruktEgenmeldingsdager?.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = it.svar.tilBoolean(),
                    )
                },
            egenmeldingsdager =
                brukerSvarKafkaDTO.egenmeldingsdager?.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = it.svar,
                    )
                },
            harBruktEgenmelding =
                brukerSvarKafkaDTO.harBruktEgenmelding?.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = it.svar.tilBoolean(),
                    )
                },
            egenmeldingsperioder =
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
                },
            harForsikring =
                brukerSvarKafkaDTO.harForsikring?.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = it.svar.tilBoolean(),
                    )
                },
            uriktigeOpplysninger =
                brukerSvarKafkaDTO.uriktigeOpplysninger?.let { uriktigeOpplysninger ->
                    SporsmalSvar(
                        sporsmaltekst = uriktigeOpplysninger.sporsmaltekst,
                        svar =
                            uriktigeOpplysninger.svar.map {
                                UriktigeOpplysning.valueOf(it.name)
                            },
                    )
                },
            lottOgHyre =
                brukerSvarKafkaDTO.fisker?.lottOgHyre?.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = FiskerLottOgHyre.valueOf(it.svar.name),
                    )
                },
            blad =
                brukerSvarKafkaDTO.fisker?.blad?.let {
                    SporsmalSvar(
                        sporsmaltekst = it.sporsmaltekst,
                        svar = FiskerBlad.valueOf(it.svar.name),
                    )
                },
        )
}

internal data class AlleBrukerSvar(
    val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>? = null,
    val riktigNarmesteLeder: SporsmalSvar<Boolean>? = null,
    val harBruktEgenmelding: SporsmalSvar<Boolean>? = null,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>? = null,
    val harEgenmeldingsdager: SporsmalSvar<Boolean>? = null,
    val egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    val lottOgHyre: SporsmalSvar<FiskerLottOgHyre>? = null,
    val blad: SporsmalSvar<FiskerBlad>? = null,
    val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
)

private fun JaEllerNei.tilBoolean(): Boolean =
    when (this) {
        JaEllerNei.JA -> true
        JaEllerNei.NEI -> false
    }
