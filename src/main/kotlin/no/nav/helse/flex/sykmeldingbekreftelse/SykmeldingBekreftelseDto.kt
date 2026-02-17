package no.nav.helse.flex.sykmeldingbekreftelse

import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmelding.tsm.SykmeldingGrunnlag
import java.time.Instant

data class SykmeldingBekreftelseDto(
    val sykmeldingId: String,
    val timestamp: Instant,
    val sykmelding: SykmeldingGrunnlag,
    val validering: SykmeldingValideringDto,
    val brukerbekreftelse: BrukerbekreftelseDto,
)

data class SykmeldingValideringDto(
    val status: RuleType,
)

enum class BrukerbekreftelseStatusDto {
    APEN,
    BEKREFTET,
    BEKREFTET_AVVIST,
    AVBRUTT,
}

data class BrukerbekreftelseDto(
    val status: BrukerbekreftelseStatusDto,
    val brukersituasjon: BrukersituasjonDto? = null,
) {
    init {
        if (status == BrukerbekreftelseStatusDto.BEKREFTET) {
            requireNotNull(brukersituasjon) { "Må ha brukersituasjon når status er $status" }
        }
    }
}
