package no.nav.helse.flex.tsmsykmeldingstatus.dto

data class ArbeidsgiverStatusKafkaDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String? = null,
    val orgNavn: String,
)

data class TidligereArbeidsgiverKafkaDTO(
    val orgNavn: String?,
    val orgnummer: String?,
    val sykmeldingsId: String,
)
