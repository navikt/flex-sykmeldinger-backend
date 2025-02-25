package no.nav.helse.flex.sykmelding.domain.tsm.values

data class Kontaktinfo(
    val type: KontaktinfoType,
    val value: String,
)

enum class KontaktinfoType {
    TELEFONSVARER,
    NODNUMMER,
    FAX_TELEFAKS,
    HJEMME_ELLER_UKJENT,
    HOVEDTELEFON,
    FERIETELEFON,
    MOBILTELEFON,
    PERSONSOKER,
    ARBEIDSPLASS_SENTRALBORD,
    ARBEIDSPLASS_DIREKTENUMMER,
    ARBEIDSPLASS,
    TLF,
    IKKE_OPPGITT,
    UGYLDIG,
}
