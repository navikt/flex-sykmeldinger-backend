package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.databind.module.SimpleModule
import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.utils.addPolymorphicDeserializer

enum class TilleggsinfoType {
    ARBEIDSTAKER,
    ARBEIDSLEDIG,
    PERMITTERT,
    FISKER,
    FRILANSER,
    NAERINGSDRIVENDE,
    JORDBRUKER,
    ANNET,
    UTDATERT_FORMAT,
}

sealed interface Tilleggsinfo {
    val type: TilleggsinfoType

    companion object {
        val deserializerModule =
            SimpleModule().addPolymorphicDeserializer(Tilleggsinfo::type) {
                when (it) {
                    TilleggsinfoType.ARBEIDSTAKER -> ArbeidstakerTilleggsinfo::class
                    TilleggsinfoType.FRILANSER -> FrilanserTilleggsinfo::class
                    TilleggsinfoType.NAERINGSDRIVENDE -> NaringsdrivendeTilleggsinfo::class
                    TilleggsinfoType.ARBEIDSLEDIG -> ArbeidsledigTilleggsinfo::class
                    TilleggsinfoType.ANNET -> AnnetArbeidssituasjonTilleggsinfo::class
                    TilleggsinfoType.PERMITTERT -> PermittertTilleggsinfo::class
                    TilleggsinfoType.FISKER -> FiskerTilleggsinfo::class
                    TilleggsinfoType.JORDBRUKER -> JordbrukerTilleggsinfo::class
                    TilleggsinfoType.UTDATERT_FORMAT -> UtdatertFormatTilleggsinfo::class
                }
            }
    }
}

data class ArbeidstakerTilleggsinfo(
    val arbeidsgiver: Arbeidsgiver,
) : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.ARBEIDSTAKER
}

data class ArbeidsledigTilleggsinfo(
    val tidligereArbeidsgiver: TidligereArbeidsgiver? = null,
) : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.ARBEIDSLEDIG
}

data class PermittertTilleggsinfo(
    val tidligereArbeidsgiver: TidligereArbeidsgiver? = null,
) : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.PERMITTERT
}

data class FiskerTilleggsinfo(
    val arbeidsgiver: Arbeidsgiver? = null,
) : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.FISKER
}

data object FrilanserTilleggsinfo : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.FRILANSER
}

data object JordbrukerTilleggsinfo : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.JORDBRUKER
}

data object NaringsdrivendeTilleggsinfo : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.NAERINGSDRIVENDE
}

data object AnnetArbeidssituasjonTilleggsinfo : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.ANNET
}

data class UtdatertFormatTilleggsinfo(
    val arbeidsgiver: UtdatertFormatArbeidsgiver? = null,
    val tidligereArbeidsgiver: TidligereArbeidsgiver? = null,
) : Tilleggsinfo {
    override val type: TilleggsinfoType = TilleggsinfoType.UTDATERT_FORMAT
}

data class UtdatertFormatArbeidsgiver(
    val orgnummer: String,
    val juridiskOrgnummer: String?,
    val orgnavn: String,
)

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
