package no.nav.helse.flex.sykmelding.logikk

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.logger
import no.nav.helse.flex.sykmelding.domain.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingLagrer(
    private val sykmeldingRepository: ISykmeldingRepository,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
    private val nowFactory: Supplier<Instant>,
) {
    val log = logger()

    @Transactional
    fun lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfallMelding) {
        if (sykmeldingRepository.findBySykmeldingId(sykmeldingMedBehandlingsutfall.sykmelding.id) != null) {
            log.info("Sykmelding ${sykmeldingMedBehandlingsutfall.sykmelding.id} finnes fra før")
        } else {
            val sykmelding = sykmeldingFactory(sykmeldingMedBehandlingsutfall)
            sykmeldingRepository.save(sykmelding)
            arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(sykmelding.pasientFnr)
            log.info("Sykmelding ${sykmeldingMedBehandlingsutfall.sykmelding.id} lagret")
        }
    }

    private fun sykmeldingFactory(sykmeldingMedBehandlingsutfallMelding: SykmeldingMedBehandlingsutfallMelding): Sykmelding =
        Sykmelding(
            sykmeldingGrunnlag = sykmeldingMedBehandlingsutfallMelding.sykmelding,
            statuser =
                listOf(
                    SykmeldingHendelse(
                        status = HendelseStatus.APEN,
                        opprettet = nowFactory.get(),
                    ),
                ),
            opprettet = nowFactory.get(),
            oppdatert = nowFactory.get(),
        )
}
