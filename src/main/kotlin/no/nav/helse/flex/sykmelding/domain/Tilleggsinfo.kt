package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.databind.module.SimpleModule
import no.nav.helse.flex.sykmelding.application.Arbeidssituasjon
import no.nav.helse.flex.utils.addPolymorphicDeserializer

sealed interface Tilleggsinfo {
    val arbeidssituasjon: Arbeidssituasjon

    companion object {
        val deserializerModule =
            SimpleModule().addPolymorphicDeserializer<Tilleggsinfo, Arbeidssituasjon>(Tilleggsinfo::arbeidssituasjon) {
                when (it) {
                    Arbeidssituasjon.ARBEIDSTAKER -> ArbeidstakerTilleggsinfo::class
                    Arbeidssituasjon.FRILANSER -> FrilanserTilleggsinfo::class
                    Arbeidssituasjon.NAERINGSDRIVENDE -> NaringsdrivendeTilleggsinfo::class
                    Arbeidssituasjon.ARBEIDSLEDIG -> ArbeidsledigTilleggsinfo::class
                    Arbeidssituasjon.ANNET -> AnnetArbeidssituasjonTilleggsinfo::class
                    Arbeidssituasjon.PERMITTERT -> PermittertTilleggsinfo::class
                    Arbeidssituasjon.FISKER -> FiskerTilleggsinfo::class
                    Arbeidssituasjon.JORDBRUKER -> JordbrukerTilleggsinfo::class
                }
            }
    }
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
