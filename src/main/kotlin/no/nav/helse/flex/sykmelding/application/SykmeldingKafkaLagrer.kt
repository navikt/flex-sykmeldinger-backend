package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingKafkaLagrer(
    private val sykmeldingRepository: ISykmeldingRepository,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
    private val nowFactory: Supplier<Instant>,
) {
    val log = logger()

    @Transactional
    fun lagreSykmeldingMedBehandlingsutfall(sykmeldingKafkaRecord: SykmeldingKafkaRecord) {
        val eksisterendeSykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingKafkaRecord.sykmelding.id)
        if (eksisterendeSykmelding != null) {
            val oppdatertSykmelding = oppdaterSykmelding(eksisterendeSykmelding, sykmeldingKafkaRecord)
            sykmeldingRepository.save(oppdatertSykmelding)
        } else {
            val sykmelding = opprettNySykmelding(sykmeldingKafkaRecord)
            sykmeldingRepository.save(sykmelding)
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
        if (eksisterendeSykmelding.meldingsinformasjon != sykmeldingKafkaRecord.metadata) {
            log.error("Meldingsinformasjon kan ikke endres")
            throw RuntimeException("Meldingsinformasjon kan ikke endres")
        }
        return oppdatertSykmelding
    }

    private fun opprettNySykmelding(sykmeldingKafkaRecord: SykmeldingKafkaRecord): Sykmelding {
        val now = nowFactory.get()
        return Sykmelding(
            sykmeldingGrunnlag = sykmeldingKafkaRecord.sykmelding,
            meldingsinformasjon = sykmeldingKafkaRecord.metadata,
            validation = sykmeldingKafkaRecord.validation,
            hendelser =
                listOf(
                    SykmeldingHendelse(
                        status = HendelseStatus.APEN,
                        opprettet = now,
                    ),
                ),
            opprettet = now,
            sykmeldingGrunnlagOppdatert = now,
            validationOppdatert = now,
            hendelseOppdatert = now,
        )
    }
}
