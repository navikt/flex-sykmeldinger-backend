package no.nav.helse.flex.tsmsykmeldingstatus.dto

data class SporsmalKafkaDTO(
    val tekst: String,
    val shortName: ShortNameKafkaDTO,
    val svartype: SvartypeKafkaDTO,
    val svar: String,
)

enum class ShortNameKafkaDTO {
    ARBEIDSSITUASJON,
    NY_NARMESTE_LEDER,
    FRAVAER,
    PERIODE,
    FORSIKRING,
    EGENMELDINGSDAGER,
}

enum class SvartypeKafkaDTO {
    ARBEIDSSITUASJON,
    PERIODER,
    JA_NEI,
    DAGER,
}
