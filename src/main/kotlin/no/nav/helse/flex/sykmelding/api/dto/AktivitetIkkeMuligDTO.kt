package no.nav.helse.flex.sykmelding.api.dto

data class AktivitetIkkeMuligDTO(
    val medisinskArsak: MedisinskArsakDTO?,
    val arbeidsrelatertArsak: ArbeidsrelatertArsakDTO?,
)
