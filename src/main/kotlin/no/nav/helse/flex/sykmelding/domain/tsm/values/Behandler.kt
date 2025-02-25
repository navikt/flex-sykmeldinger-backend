package no.nav.helse.flex.sykmelding.domain.tsm.values

data class Behandler(
    val navn: Navn,
    val adresse: Adresse?,
    val ids: List<PersonId>,
    val kontaktinfo: List<Kontaktinfo>,
)
