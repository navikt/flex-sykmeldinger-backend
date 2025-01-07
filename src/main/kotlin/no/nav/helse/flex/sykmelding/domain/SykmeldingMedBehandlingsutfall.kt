package no.nav.helse.flex.sykmelding.domain
import java.time.LocalDate

data class SykmeldingMedBehandlingsutfall(
    val sykmelding: Sykmelding,
    val behandlingsutfall: Behandlingsutfall,
)

// OK, MANUAL_PROCESSING, INVALID
data class Behandlingsutfall(
    val status: String,
)

data class TidligereArbeidsgiverDTO(
    val orgnummer: String,
    val orgNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
)
