package no.nav.helse.flex.sykmelding.application

import java.time.LocalDate

sealed interface BrukerSvar {
    val arbeidssituasjon: Arbeidssituasjon
    val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>
    val erOpplysningeneRiktige: SporsmalSvar<Boolean>
    val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>?
}

data class ArbeidstakerBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>,
    val riktigNarmesteLeder: SporsmalSvar<Boolean>,
    val harEgenmeldingsdager: SporsmalSvar<Boolean>,
    val egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER
}

data class ArbeidsledigBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG
}

data class PermittertBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.PERMITTERT
}

data class FiskerBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>,
    val lottOgHyre: SporsmalSvar<FiskerLottOgHyre>,
    val blad: SporsmalSvar<FiskerBlad>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>? = null,
    val riktigNarmesteLeder: SporsmalSvar<Boolean>? = null,
    val harEgenmeldingsdager: SporsmalSvar<Boolean>? = null,
    val egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    val harBruktEgenmelding: SporsmalSvar<Boolean>? = null,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.FISKER

    private val erArbeidstaker = lottOgHyre.svar in setOf(FiskerLottOgHyre.HYRE, FiskerLottOgHyre.BEGGE)

    fun somArbeidstaker(): ArbeidstakerBrukerSvar {
        require(erArbeidstaker) { "Fisker er ikke arbeidstaker" }
        return ArbeidstakerBrukerSvar(
            erOpplysningeneRiktige = erOpplysningeneRiktige,
            arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
            arbeidsgiverOrgnummer = requireNotNull(arbeidsgiverOrgnummer) { "Fisker som er arbeidstaker må ha satt arbeidsgiverOrgnummer" },
            riktigNarmesteLeder = requireNotNull(riktigNarmesteLeder) { "Fisker som er arbeidstaker må ha satt riktigNarmesteLeder" },
            harEgenmeldingsdager = requireNotNull(harEgenmeldingsdager) { "Fisker som er arbeidstaker må ha satt harEgenmeldingsdager" },
            egenmeldingsdager = egenmeldingsdager,
            uriktigeOpplysninger = uriktigeOpplysninger,
        )
    }

    fun somNaringsdrivende(): NaringsdrivendeBrukerSvar {
        require(!erArbeidstaker) { "Fisker er ikke naringsdrivende" }
        return NaringsdrivendeBrukerSvar(
            erOpplysningeneRiktige = erOpplysningeneRiktige,
            arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
            harBruktEgenmelding = requireNotNull(harBruktEgenmelding) { "Fisker som er næringsdrivende må ha satt harBruktEgenmelding" },
            egenmeldingsperioder = egenmeldingsperioder,
            harForsikring = requireNotNull(harForsikring) { "Fisker som er næringsdrivende må ha satt harForsikring" },
            uriktigeOpplysninger = uriktigeOpplysninger,
        )
    }

    init {
        val erArbeidstaker = runCatching { somArbeidstaker() }.isSuccess
        val erNaringsdrivende = runCatching { somNaringsdrivende() }.isSuccess
        if (!erArbeidstaker && !erNaringsdrivende) {
            throw IllegalArgumentException("Fisker må ha satt felter tilknyttet arbeidsgiver eller næringsdrivende")
        }
    }
}

data class FrilanserBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>,
    val harBruktEgenmelding: SporsmalSvar<Boolean>,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.FRILANSER
}

data class JordbrukerBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>,
    val harBruktEgenmelding: SporsmalSvar<Boolean>,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.JORDBRUKER
}

data class NaringsdrivendeBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>,
    val harBruktEgenmelding: SporsmalSvar<Boolean>,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.NAERINGSDRIVENDE
}

data class AnnetArbeidssituasjonBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon>,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.ANNET
}

data class SporsmalSvar<T>(
    val sporsmaltekst: String,
    val svar: T,
)

enum class FiskerBlad {
    A,
    B,
}

enum class FiskerLottOgHyre {
    LOTT,
    HYRE,
    BEGGE,
}

data class Egenmeldingsperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

enum class UriktigeOpplysning {
    ANDRE_OPPLYSNINGER,
    ARBEIDSGIVER,
    DIAGNOSE,
    PERIODE,
    SYKMELDINGSGRAD_FOR_HOY,
    SYKMELDINGSGRAD_FOR_LAV,
}
