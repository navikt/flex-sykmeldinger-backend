package no.nav.helse.flex.sykmelding.tsm.values

data class PersonId(
    val id: String,
    val type: PersonIdType,
)

enum class PersonIdType {
    FNR,
    DNR,
    HNR,
    HPR,
    HER,
    PNR,
    SEF,
    DKF,
    SSN,
    FPN,
    XXX,
    DUF,
    IKKE_OPPGITT,
    UGYLDIG,
}
