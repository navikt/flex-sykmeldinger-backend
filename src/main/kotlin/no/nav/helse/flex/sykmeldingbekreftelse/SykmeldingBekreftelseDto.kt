package no.nav.helse.flex.sykmeldingbekreftelse

import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmelding.tsm.SykmeldingGrunnlag
import java.time.Instant

data class SykmeldingBekreftelseDto(
    val sykmeldingId: String,
    val timestamp: Instant,
    val sykmelding: SykmeldingGrunnlag,
    val validering: SykmeldingValideringDto,
    val bekreftelse: BekreftelseDto,
)

data class SykmeldingValideringDto(
    val status: RuleType,
    val timestamp: Instant,
    val forsteStatus: RuleType,
)

enum class BekreftelseStatusDto {
    APEN,
    BEKREFTET,
    BEKREFTET_AVVIST,
    AVBRUTT,
}

data class BekreftelseDto(
    val status: BekreftelseStatusDto,
    val brukersituasjon: BrukersituasjonDto? = null,
) {
    init {
        if (status == BekreftelseStatusDto.BEKREFTET) {
            requireNotNull(brukersituasjon) { "Må ha brukersituasjon når status er $status" }
        }
    }
}
