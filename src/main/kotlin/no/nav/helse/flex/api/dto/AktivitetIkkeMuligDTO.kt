package no.nav.helse.flex.api.dto

data class AktivitetIkkeMuligDTO(
    val medisinskArsak: List<MedisinskArsakDTO>,
    val arbeidsrelatertArsak: List<ArbeidsrelatertArsakDTO>,
)
