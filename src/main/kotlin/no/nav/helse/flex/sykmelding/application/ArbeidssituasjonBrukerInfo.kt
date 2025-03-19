package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.api.dto.Egenmeldingsperiode
import java.time.LocalDate

// val erOpplysningeneRiktige: YesOrNo,
// val arbeidssituasjon: Arbeidssituasjon,
// val arbeidsgiverOrgnummer: String? = null,
// val harEgenmeldingsdager: YesOrNo? = null,
// val riktigNarmesteLeder: YesOrNo? = null,
// val arbeidsledig: Arbeidsledig? = null,
// val egenmeldingsdager: List<LocalDate>? = null,
// val egenmeldingsperioder: List<EgenmeldingsperiodeDTO>? = null,
// val fisker: Fisker? = null,
// val harBruktEgenmelding: YesOrNo? = null,
// val harForsikring: YesOrNo? = null,
// val uriktigeOpplysninger: List<UriktigeOpplysning>? = null,

sealed interface BrukerSvar {
    val erOpplysningeneRiktige: Boolean
    val arbeidssituasjon: Arbeidssituasjon
    val uriktigeOpplysninger: List<UriktigeOpplysning>?
}

data class ArbeidstakerBrukerSvar(
    override val erOpplysningeneRiktige: Boolean,
    val arbeidsgiverOrgnummer: String,
    val riktigNarmesteLeder: Boolean,
    val harEgenmeldingsdager: Boolean,
    val egenmeldingsdager: List<LocalDate>? = null,
    override val uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER
}

data class ArbeidsledigBrukerSvar(
    val arbeidsledigFraOrgnummer: String? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG
}

data class PermittertBrukerSvar(
    val arbeidsledigFraOrgnummer: String? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.PERMITTERT
}

data class FiskerBrukerSvar(
    override val erOpplysningeneRiktige: Boolean,
    val lottOgHyre: FiskerLottOgHyre,
    val blad: FiskerBlad,
    val arbeidsgiverOrgnummer: String? = null,
    val riktigNarmesteLeder: Boolean? = null,
    val harEgenmeldingsdager: Boolean? = null,
    val egenmeldingsdager: List<LocalDate>? = null,
    val harBruktEgenmelding: Boolean? = null,
    val egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    val harForsikring: Boolean? = null,
    override val uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.FISKER

    private val erArbeidstaker = lottOgHyre in setOf(FiskerLottOgHyre.HYRE, FiskerLottOgHyre.BEGGE)

    fun somArbeidstaker(): ArbeidstakerBrukerSvar {
        require(erArbeidstaker) { "Fisker er ikke arbeidstaker" }
        return ArbeidstakerBrukerSvar(
            erOpplysningeneRiktige = erOpplysningeneRiktige,
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
    override val erOpplysningeneRiktige: Boolean,
    val harBruktEgenmelding: Boolean,
    val egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    val harForsikring: Boolean,
    override val uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.FRILANSER
}

data class JordbrukerBrukerSvar(
    override val erOpplysningeneRiktige: Boolean,
    val harBruktEgenmelding: Boolean,
    val egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    val harForsikring: Boolean,
    override val uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.JORDBRUKER
}

data class NaringsdrivendeBrukerSvar(
    override val erOpplysningeneRiktige: Boolean,
    val harBruktEgenmelding: Boolean,
    val egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    val harForsikring: Boolean,
    override val uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.NAERINGSDRIVENDE
}

data class AnnetArbeidssituasjonBrukerSvar(
    override val erOpplysningeneRiktige: Boolean,
    override val uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
) : BrukerSvar {
    override val arbeidssituasjon = Arbeidssituasjon.ANNET
}

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
