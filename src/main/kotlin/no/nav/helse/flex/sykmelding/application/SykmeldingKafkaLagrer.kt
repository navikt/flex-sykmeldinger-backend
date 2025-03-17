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
        if (sykmeldingRepository.findBySykmeldingId(sykmeldingKafkaRecord.sykmelding.id) != null) {
            log.info("Sykmelding ${sykmeldingKafkaRecord.sykmelding.id} finnes fra før")
        } else {
            val sykmelding = sykmeldingFactory(sykmeldingKafkaRecord)
            sykmeldingRepository.save(sykmelding)
            arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(sykmelding.pasientFnr)
            log.info("Sykmelding ${sykmeldingKafkaRecord.sykmelding.id} lagret")
        }
    }

    private fun sykmeldingFactory(sykmeldingKafkaRecord: SykmeldingKafkaRecord): Sykmelding =
        Sykmelding(
            sykmeldingGrunnlag = sykmeldingKafkaRecord.sykmelding,
            meldingsinformasjon = sykmeldingKafkaRecord.metadata,
            validation = sykmeldingKafkaRecord.validation,
            hendelser =
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
