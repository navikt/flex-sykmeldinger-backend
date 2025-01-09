package no.nav.helse.flex.sykmelding.logikk

import no.nav.helse.flex.sykmelding.domain.FlexSykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfall
import no.nav.helse.flex.sykmelding.domain.SykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SykmeldingLagrer(
    private val sykmeldingRepository: SykmeldingRepository,
) {
    fun lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfall) {
        sykmeldingRepository.save(
            FlexSykmelding(
                sykmelding = sykmeldingMedBehandlingsutfall.sykmelding,
                validation = sykmeldingMedBehandlingsutfall.validation,
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
