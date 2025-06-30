package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.api.dto.JaEllerNei
import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.*
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
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
        val brukerSvar =
            when {
                status.brukerSvar != null -> konverterBrukerSvarKafkaDtoTilBrukerSvar(status.brukerSvar)
                status.sporsmals != null -> {
                    val brukerSvarKafkaDto =
                        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(
                            sporsmal = status.sporsmals,
                            hendelseStatus = hendelseStatus,
                            arbeidsgiver = status.arbeidsgiver,
                        )
                    brukerSvarKafkaDto?.let(::konverterBrukerSvarKafkaDtoTilBrukerSvar)
                }
                else -> null
            }
        if (hendelseStatus in setOf(HendelseStatus.SENDT_TIL_NAV, HendelseStatus.SENDT_TIL_ARBEIDSGIVER)) {
            requireNotNull(brukerSvar) {
                "Brukersvar er påkrevd for SENDT_TIL_NAV og SENDT_TIL_ARBEIDSGIVER"
            }
        }

        val tilleggsinfo =
            brukerSvar?.let {
                konverterTilTilleggsinfo(
                    arbeidssituasjon = it.arbeidssituasjon.svar,
                    arbeidsgiver = status.arbeidsgiver,
                    tidligereArbeidsgiver = status.tidligereArbeidsgiver,
                )
            }

        return SykmeldingHendelse(
            status = hendelseStatus,
            brukerSvar = brukerSvar,
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
        val alleBrukerSvar = konverterBrukerSvarKafkaDtoTilAlleBrukerSvar(brukerSvarKafkaDTO)

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

    internal fun konverterBrukerSvarKafkaDtoTilAlleBrukerSvar(brukerSvarKafkaDTO: BrukerSvarKafkaDTO): AlleBrukerSvar =
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
