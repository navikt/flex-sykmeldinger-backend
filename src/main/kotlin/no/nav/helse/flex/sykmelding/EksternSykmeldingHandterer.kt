package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingStatusHandterer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Component
class EksternSykmeldingHandterer(
    private val sykmeldingRepository: ISykmeldingRepository,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
    private val sykmeldingStatusHandterer: SykmeldingStatusHandterer,
    private val nowFactory: Supplier<Instant>,
) {
    val log = logger()

    @Transactional(rollbackFor = [Exception::class])
    fun lagreSykmeldingFraKafka(
        sykmeldingId: String,
        eksternSykmeldingMelding: EksternSykmeldingMelding?,
    ) {
        if (eksternSykmeldingMelding == null) {
            slettSykmelding(sykmeldingId = sykmeldingId)
        } else {
            opprettEllerOppdaterSykmelding(eksternSykmeldingMelding)
        }
    }

    internal fun opprettEllerOppdaterSykmelding(eksternSykmeldingMelding: EksternSykmeldingMelding) {
        val eksisterendeSykmelding = sykmeldingRepository.findBySykmeldingId(eksternSykmeldingMelding.sykmelding.id)
        if (eksisterendeSykmelding != null) {
            val oppdatertSykmelding = oppdaterSykmelding(eksisterendeSykmelding, eksternSykmeldingMelding)
            sykmeldingRepository.save(oppdatertSykmelding)
            log.info("Sykmelding oppdatert: ${eksisterendeSykmelding.sykmeldingId}")
        } else {
            val sykmelding = opprettNySykmelding(eksternSykmeldingMelding)
            sykmeldingRepository.save(sykmelding)
            sykmeldingStatusHandterer.prosesserSykmeldingStatuserFraBuffer(sykmelding.sykmeldingId)
            arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(sykmelding.pasientFnr)
            log.info("Sykmelding lagret: ${eksternSykmeldingMelding.sykmelding.id}")
        }
    }

    internal fun slettSykmelding(sykmeldingId: String) {
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            log.warn("Prøver å slette sykmelding $sykmeldingId som ikke finnes, hopper over")
            return
        } else {
            sykmeldingRepository.delete(sykmelding)
            log.info("Sykmelding slettet: $sykmeldingId")
        }
    }

    private fun oppdaterSykmelding(
        eksisterendeSykmelding: Sykmelding,
        eksternSykmeldingMelding: EksternSykmeldingMelding,
    ): Sykmelding {
        var oppdatertSykmelding = eksisterendeSykmelding

        if (eksisterendeSykmelding.sykmeldingGrunnlag != eksternSykmeldingMelding.sykmelding) {
            oppdatertSykmelding =
                oppdatertSykmelding.copy(
                    sykmeldingGrunnlag = eksternSykmeldingMelding.sykmelding,
                    sykmeldingGrunnlagOppdatert = nowFactory.get(),
                )
        }
        if (eksisterendeSykmelding.validation != eksternSykmeldingMelding.validation) {
            oppdatertSykmelding =
                oppdatertSykmelding.copy(
                    validation = eksternSykmeldingMelding.validation,
                    validationOppdatert = nowFactory.get(),
                )
        }
        return oppdatertSykmelding
    }

    private fun opprettNySykmelding(eksternSykmeldingMelding: EksternSykmeldingMelding): Sykmelding {
        val now = nowFactory.get()
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = eksternSykmeldingMelding.sykmelding,
                validation = eksternSykmeldingMelding.validation,
                hendelser =
                    listOf(
                        SykmeldingHendelse(
                            status = HendelseStatus.APEN,
                            source = SykmeldingHendelse.LOKAL_SOURCE,
                            hendelseOpprettet =
                                eksternSykmeldingMelding.sykmelding.metadata.mottattDato
                                    .toInstant(),
                            lokaltOpprettet = now,
                        ),
                    ),
                opprettet = now,
                sykmeldingGrunnlagOppdatert = now,
                validationOppdatert = now,
                hendelseOppdatert = now,
            )
        return sykmelding
    }
}
