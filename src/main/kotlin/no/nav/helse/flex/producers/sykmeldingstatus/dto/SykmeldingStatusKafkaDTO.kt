package no.nav.helse.flex.producers.sykmeldingstatus.dto

import java.time.OffsetDateTime

data class SykmeldingStatusKafkaDTO(
    val sykmeldingId: String,
    val timestamp: OffsetDateTime,
    val statusEvent: String,
    val arbeidsgiver: ArbeidsgiverStatusKafkaDTO? = null,
    val sporsmals: List<SporsmalKafkaDTO>? = null,
    val brukerSvar: BrukerSvarKafkaDTO? = null,
    val erSvarOppdatering: Boolean? = null,
    val tidligereArbeidsgiver: TidligereArbeidsgiverKafkaDTO? = null,
)

object StatusEventKafkaDTO {
    const val APEN = "APEN"
    const val AVBRUTT = "AVBRUTT"
    const val UTGATT = "UTGATT"
    const val SENDT = "SENDT"
    const val BEKREFTET = "BEKREFTET"
    const val SLETTET = "SLETTET"
}
