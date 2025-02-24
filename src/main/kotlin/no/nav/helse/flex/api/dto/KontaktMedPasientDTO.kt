package no.nav.helse.flex.api.dto

import java.time.LocalDate

data class KontaktMedPasientDTO(
    val kontaktDato: LocalDate?,
    val begrunnelseIkkeKontakt: String?,
)
