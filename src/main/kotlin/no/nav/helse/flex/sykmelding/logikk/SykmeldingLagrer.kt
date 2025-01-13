package no.nav.helse.flex.sykmelding.logikk

import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SykmeldingLagrer(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    fun lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfallMelding) {
        sykmeldingRepository.save(
            Sykmelding(
                sykmeldingGrunnlag = sykmeldingMedBehandlingsutfall.sykmelding,
                statuser =
                    listOf(
                        SykmeldingStatus(
                            status = "NY",
                            sporsmalSvar = null,
                            timestamp = Instant.now(),
                        ),
                    ),
            ),
        )
    }
}
