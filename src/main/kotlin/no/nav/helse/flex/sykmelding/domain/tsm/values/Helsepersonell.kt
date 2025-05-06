package no.nav.helse.flex.sykmelding.domain.tsm.values

import java.time.LocalDate

data class Helsepersonell(
    val ids: List<PersonId>,
    val navn: Navn?,
    val fodselsdato: LocalDate?,
    val kjonn: Kjonn?,
    val nasjonalitet: String?,
    val adresse: Adresse?,
    val kontaktinfo: List<Kontaktinfo>,
    val helsepersonellKategori: HelsepersonellKategori,
    val rolleTilPasient: RolleTilPasient,
)

data class Sykmelder(
    val ids: List<PersonId>,
    val helsepersonellKategori: HelsepersonellKategori,
)

enum class HelsepersonellKategori {
    HELSESEKRETAR,
    KIROPRAKTOR,
    LEGE,
    MANUELLTERAPEUT,
    TANNLEGE,
    FYSIOTERAPEUT,
    SYKEPLEIER,
    HJELPEPLEIER,
    HELSEFAGARBEIDER,
    USPESIFISERT,
    JORDMOR,
    AUDIOGRAF,
    NAPRAPAT,
    AMBULANSEARBEIDER,
    PSYKOLOG,
    FOTTERAPEUT,
    TANNHELSESEKRETAR,
    UGYLDIG,
    IKKE_OPPGITT,
}

enum class RolleTilPasient {
    JOURNALANSVARLIG,
    FASTLEGE,
    IKKE_OPPGITT,
}

enum class Kjonn {
    MANN,
    KVINNE,
    USPESIFISERT,
    IKKE_OPPGITT,
    UGYLDIG,
}
