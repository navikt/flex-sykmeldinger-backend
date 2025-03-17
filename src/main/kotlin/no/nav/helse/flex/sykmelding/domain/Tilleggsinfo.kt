package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.application.Arbeidssituasjon

sealed interface Tilleggsinfo {
    val arbeidssituasjon: Arbeidssituasjon
}

data class ArbeidstakerTilleggsinfo(
    val arbeidsgiver: Arbeidsgiver,
) : Tilleggsinfo {
    override val arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER
}

data class ArbeidsledigTilleggsinfo(
    val tidligereArbeidsgiver: Arbeidsgiver? = null,
) : Tilleggsinfo {
    override val arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG
}

data class PermittertTilleggsinfo(
    val tidligereArbeidsgiver: Arbeidsgiver? = null,
) : Tilleggsinfo {
    override val arbeidssituasjon = Arbeidssituasjon.PERMITTERT
}

data class FiskerTilleggsinfo(
    val arbeidsgiver: Arbeidsgiver? = null,
) : Tilleggsinfo {
    override val arbeidssituasjon = Arbeidssituasjon.FISKER
}

data object FrilanserTilleggsinfo : Tilleggsinfo {
    override val arbeidssituasjon = Arbeidssituasjon.FRILANSER
}

data object JordbrukerTilleggsinfo : Tilleggsinfo {
    override val arbeidssituasjon = Arbeidssituasjon.JORDBRUKER
}

data object NaringsdrivendeTilleggsinfo : Tilleggsinfo {
    override val arbeidssituasjon = Arbeidssituasjon.NAERINGSDRIVENDE
}

data object AnnetArbeidssituasjonTilleggsinfo : Tilleggsinfo {
    override val arbeidssituasjon = Arbeidssituasjon.ANNET
}

data class Arbeidsgiver(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val orgnavn: String,
    val erAktivtArbeidsforhold: Boolean,
    val narmesteLeder: NarmesteLeder?,
)

data class NarmesteLeder(
    val navn: String,
)
