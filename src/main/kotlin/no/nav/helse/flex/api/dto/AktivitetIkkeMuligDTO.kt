package no.nav.helse.flex.api.dto

data class AktivitetIkkeMuligDTO(
    val medisinskArsak: no.nav.helse.flex.api.dto.MedisinskArsakDTO?,
    val arbeidsrelatertArsak: no.nav.helse.flex.api.dto.ArbeidsrelatertArsakDTO?,
)
