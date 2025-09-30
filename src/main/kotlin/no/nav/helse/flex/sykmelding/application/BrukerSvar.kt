package no.nav.helse.flex.sykmelding.application

import com.fasterxml.jackson.databind.module.SimpleModule
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.utils.addPolymorphicDeserializer
import java.time.LocalDate

enum class BrukerSvarType {
    ARBEIDSTAKER,
    ARBEIDSLEDIG,
    PERMITTERT,
    FISKER,
    FRILANSER,
    JORDBRUKER,
    NAERINGSDRIVENDE,
    ANNET,
    UTDATERT_FORMAT,
}

sealed interface BrukerSvar {
    val type: BrukerSvarType
    val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>
    val erOpplysningeneRiktige: SporsmalSvar<Boolean>
    val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>?

    companion object {
        val deserializerModule =
            SimpleModule()
                .addPolymorphicDeserializer(BrukerSvar::type) {
                    when (it) {
                        BrukerSvarType.ARBEIDSTAKER -> ArbeidstakerBrukerSvar::class
                        BrukerSvarType.ARBEIDSLEDIG -> ArbeidsledigBrukerSvar::class
                        BrukerSvarType.PERMITTERT -> PermittertBrukerSvar::class
                        BrukerSvarType.FISKER -> FiskerBrukerSvar::class
                        BrukerSvarType.FRILANSER -> FrilanserBrukerSvar::class
                        BrukerSvarType.JORDBRUKER -> JordbrukerBrukerSvar::class
                        BrukerSvarType.NAERINGSDRIVENDE -> NaringsdrivendeBrukerSvar::class
                        BrukerSvarType.ANNET -> AnnetArbeidssituasjonBrukerSvar::class
                        BrukerSvarType.UTDATERT_FORMAT -> UtdatertFormatBrukerSvar::class
                    }
                }
    }
}

data class ArbeidstakerBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>,
    val riktigNarmesteLeder: SporsmalSvar<Boolean>? = null,
    val harEgenmeldingsdager: SporsmalSvar<Boolean>? = null,
    val egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val type = BrukerSvarType.ARBEIDSTAKER
}

data class ArbeidsledigBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val type = BrukerSvarType.ARBEIDSLEDIG
}

data class PermittertBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val type = BrukerSvarType.PERMITTERT
}

data class FiskerBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
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
    override val type = BrukerSvarType.FISKER

    private val erArbeidstaker = lottOgHyre.svar in setOf(FiskerLottOgHyre.HYRE, FiskerLottOgHyre.BEGGE)

    fun valider() {
        if (erArbeidstaker) {
            runCatching { somArbeidstaker() }.onFailure {
                throw IllegalArgumentException("Fisker som arbeidstaker må ha satt felter tilknyttet arbeidsgiver")
            }
        } else {
            runCatching { somNaringsdrivende() }.onFailure {
                throw IllegalArgumentException("Fisker som naringsdrivende må ha satt felter tilknyttet næringsdrivende")
            }
        }
    }

    fun somArbeidstaker(): ArbeidstakerBrukerSvar {
        require(erArbeidstaker) { "Fisker er ikke arbeidstaker" }
        return ArbeidstakerBrukerSvar(
            erOpplysningeneRiktige = erOpplysningeneRiktige,
            arbeidssituasjon = arbeidssituasjon,
            arbeidsgiverOrgnummer = requireNotNull(arbeidsgiverOrgnummer) { "Fisker som er arbeidstaker må ha satt arbeidsgiverOrgnummer" },
            riktigNarmesteLeder = riktigNarmesteLeder,
            harEgenmeldingsdager = harEgenmeldingsdager,
            egenmeldingsdager = egenmeldingsdager,
            uriktigeOpplysninger = uriktigeOpplysninger,
        )
    }

    fun somNaringsdrivende(): NaringsdrivendeBrukerSvar {
        require(!erArbeidstaker) { "Fisker er ikke naringsdrivende" }
        return NaringsdrivendeBrukerSvar(
            erOpplysningeneRiktige = erOpplysningeneRiktige,
            arbeidssituasjon = arbeidssituasjon,
            harBruktEgenmelding = harBruktEgenmelding,
            egenmeldingsperioder = egenmeldingsperioder,
            harForsikring = harForsikring,
            uriktigeOpplysninger = uriktigeOpplysninger,
        )
    }
}

data class FrilanserBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val harBruktEgenmelding: SporsmalSvar<Boolean>? = null,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val type = BrukerSvarType.FRILANSER
}

data class JordbrukerBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val harBruktEgenmelding: SporsmalSvar<Boolean>? = null,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val type = BrukerSvarType.JORDBRUKER
}

data class NaringsdrivendeBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val harBruktEgenmelding: SporsmalSvar<Boolean>? = null,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val type = BrukerSvarType.NAERINGSDRIVENDE
}

data class AnnetArbeidssituasjonBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val type = BrukerSvarType.ANNET
}

data class UtdatertFormatBrukerSvar(
    override val erOpplysningeneRiktige: SporsmalSvar<Boolean>,
    override val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>? = null,
    val riktigNarmesteLeder: SporsmalSvar<Boolean>? = null,
    val harEgenmeldingsdager: SporsmalSvar<Boolean>? = null,
    val egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    val harBruktEgenmelding: SporsmalSvar<Boolean>? = null,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    val harForsikring: SporsmalSvar<Boolean>? = null,
    override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
) : BrukerSvar {
    override val type = BrukerSvarType.UTDATERT_FORMAT
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
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periode: Pair<LocalDate, LocalDate>) : this(fom = periode.first, tom = periode.second)
}

enum class UriktigeOpplysning {
    ANDRE_OPPLYSNINGER,
    ARBEIDSGIVER,
    DIAGNOSE,
    PERIODE,
    SYKMELDINGSGRAD_FOR_HOY,
    SYKMELDINGSGRAD_FOR_LAV,
}
