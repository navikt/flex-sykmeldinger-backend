package no.nav.helse.flex.sykmelding.model

import java.time.LocalDate

// Models used across API and service layers
data class SendSykmeldingValues(
    val arbeidsgiverOrgnummer: String?,
    val arbeidsgiverNavn: String?,
    val startDato: LocalDate?,
    val arbeidssituasjon: String,
    val harEgenmeldingsdager: Boolean,
    val egenmeldingsdager: List<LocalDate>?,
)

data class ChangeStatusRequest(
    val status: String,
)
