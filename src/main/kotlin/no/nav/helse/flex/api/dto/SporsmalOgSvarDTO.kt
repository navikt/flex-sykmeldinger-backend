package no.nav.helse.flex.api.dto

data class SporsmalOgSvarDTO(
    val tekst: String,
    val shortName: no.nav.helse.flex.api.dto.ShortNameDTO,
    val svartype: no.nav.helse.flex.api.dto.SvartypeDTO,
    val svar: String,
)
