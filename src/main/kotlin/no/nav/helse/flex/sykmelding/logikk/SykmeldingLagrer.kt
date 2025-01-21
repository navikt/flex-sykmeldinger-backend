package no.nav.helse.flex.sykmelding.logikk

import no.nav.helse.flex.logger
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingLagrer(
    private val sykmeldingRepository: SykmeldingRepository,
    private val nowFactory: Supplier<Instant>,
) {
    val log = logger()

    fun lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfallMelding) {
        if (sykmeldingRepository.findBySykmeldingId(sykmeldingMedBehandlingsutfall.sykmelding.id) != null) {
            log.info("Sykmelding ${sykmeldingMedBehandlingsutfall.sykmelding.id} finnes fra før")
        } else {
            sykmeldingRepository.save(sykmeldingFactory(sykmeldingMedBehandlingsutfall))
            log.info("Sykmelding ${sykmeldingMedBehandlingsutfall.sykmelding.id} lagret")
        }
    }

    private fun sykmeldingFactory(sykmeldingMedBehandlingsutfallMelding: SykmeldingMedBehandlingsutfallMelding): Sykmelding =
        Sykmelding(
            sykmeldingGrunnlag = sykmeldingMedBehandlingsutfallMelding.sykmelding,
            statuser =
                listOf(
                    SykmeldingStatus(
                        status = "APEN",
                        sporsmalSvar = null,
                        opprettet = nowFactory.get(),
                    ),
                ),
            opprettet = nowFactory.get(),
            oppdatert = nowFactory.get(),
        )
}
