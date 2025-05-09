package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingStatusHandterer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingKafkaLagrer(
    private val sykmeldingRepository: ISykmeldingRepository,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
    private val sykmeldingStatusHandterer: SykmeldingStatusHandterer,
    private val nowFactory: Supplier<Instant>,
) {
    val log = logger()

    @Transactional(rollbackFor = [Exception::class])
    fun lagreSykmeldingMedBehandlingsutfall(sykmeldingKafkaRecord: SykmeldingKafkaRecord) {
        val eksisterendeSykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingKafkaRecord.sykmelding.id)
        if (eksisterendeSykmelding != null) {
            val oppdatertSykmelding = oppdaterSykmelding(eksisterendeSykmelding, sykmeldingKafkaRecord)
            sykmeldingRepository.save(oppdatertSykmelding)
        } else {
            val sykmelding = opprettNySykmelding(sykmeldingKafkaRecord)
            sykmeldingRepository.save(sykmelding)
            sykmeldingStatusHandterer.prosesserSykmeldingStatuserFraBuffer(sykmelding.sykmeldingId)
            arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(sykmelding.pasientFnr)
            log.info("Sykmelding ${sykmeldingKafkaRecord.sykmelding.id} lagret")
        }
    }

    private fun oppdaterSykmelding(
        eksisterendeSykmelding: Sykmelding,
        sykmeldingKafkaRecord: SykmeldingKafkaRecord,
    ): Sykmelding {
        var oppdatertSykmelding = eksisterendeSykmelding
        log.info("Sykmelding ${eksisterendeSykmelding.sykmeldingId} finnes fra før, oppdaterer den")
        if (eksisterendeSykmelding.sykmeldingGrunnlag != sykmeldingKafkaRecord.sykmelding) {
            oppdatertSykmelding =
                oppdatertSykmelding.copy(
                    sykmeldingGrunnlag = sykmeldingKafkaRecord.sykmelding,
                    sykmeldingGrunnlagOppdatert = nowFactory.get(),
                )
        }
        if (eksisterendeSykmelding.validation != sykmeldingKafkaRecord.validation) {
            oppdatertSykmelding =
                oppdatertSykmelding.copy(
                    validation = sykmeldingKafkaRecord.validation,
                    validationOppdatert = nowFactory.get(),
                )
        }
        return oppdatertSykmelding
    }

    private fun opprettNySykmelding(sykmeldingKafkaRecord: SykmeldingKafkaRecord): Sykmelding {
        val now = nowFactory.get()
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = sykmeldingKafkaRecord.sykmelding,
                validation = sykmeldingKafkaRecord.validation,
                hendelser =
                    listOf(
                        SykmeldingHendelse(
                            status = HendelseStatus.APEN,
                            source = SykmeldingHendelse.LOKAL_SOURCE,
                            hendelseOpprettet = now,
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
