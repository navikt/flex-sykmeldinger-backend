package no.nav.helse.flex.sykmelding.api.dto

data class TidligereArbeidsgiverDTO(
    val orgNavn: String,
    val orgnummer: String,
    val sykmeldingsId: String,
)

data class TidligereArbeidsgiver(
    val orgNavn: String,
    val orgnummer: String,
)
