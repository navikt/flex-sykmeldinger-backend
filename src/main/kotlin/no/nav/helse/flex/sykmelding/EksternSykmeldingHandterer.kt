package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.gateways.SykmeldingBrukernotifikasjonProducer
import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import no.nav.helse.flex.gateways.SykmeldingNotifikasjonStatus
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelsePubliserer
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
    private val sykmeldingBrukernotifikasjonProducer: SykmeldingBrukernotifikasjonProducer,
    private val nowFactory: Supplier<Instant>,
    private val sykmeldingHendelsePubliserer: SykmeldingHendelsePubliserer,
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
            val oppdatertSykmelding = oppdaterSykmelding(eksisterendeSykmelding, eksternSykmeldingMelding, tidspunkt = nowFactory.get())
            sykmeldingRepository.save(oppdatertSykmelding)
            log.info("Sykmelding oppdatert: ${eksisterendeSykmelding.sykmeldingId}")
        } else {
            val sykmelding = lagNySykmelding(eksternSykmeldingMelding, tidspunkt = nowFactory.get())
            sykmeldingRepository.save(sykmelding)
            sykmeldingStatusHandterer.prosesserSykmeldingStatuserFraBuffer(sykmelding.sykmeldingId)
            arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(sykmelding.pasientFnr).also {
                log.info("Synkroniserer arbeidsforhold ved sykmelding mottatt: ${it.toLogString()}")
            }
            sykmeldingHendelsePubliserer.publiserSisteHendelse(sykmelding)
            sykmeldingBrukernotifikasjonProducer.produserSykmeldingBrukernotifikasjon(lagSykemldingNotifikasjon(sykmelding)).also {
                log.info("Brukernotifikasjon produsert for sykmelding med id ${sykmelding.sykmeldingId}")
            }
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

    companion object {
        fun lagNySykmelding(
            eksternSykmeldingMelding: EksternSykmeldingMelding,
            tidspunkt: Instant,
        ): Sykmelding {
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
                                lokaltOpprettet = tidspunkt,
                            ),
                        ),
                    opprettet = tidspunkt,
                    sykmeldingGrunnlagOppdatert = tidspunkt,
                    validationOppdatert = tidspunkt,
                    hendelseOppdatert = tidspunkt,
                )
            return sykmelding
        }

        fun oppdaterSykmelding(
            eksisterendeSykmelding: Sykmelding,
            eksternSykmeldingMelding: EksternSykmeldingMelding,
            tidspunkt: Instant,
        ): Sykmelding {
            var oppdatertSykmelding = eksisterendeSykmelding

            if (eksisterendeSykmelding.sykmeldingGrunnlag != eksternSykmeldingMelding.sykmelding) {
                oppdatertSykmelding =
                    oppdatertSykmelding.copy(
                        sykmeldingGrunnlag = eksternSykmeldingMelding.sykmelding,
                        sykmeldingGrunnlagOppdatert = tidspunkt,
                    )
            }
            if (eksisterendeSykmelding.validation != eksternSykmeldingMelding.validation) {
                oppdatertSykmelding =
                    oppdatertSykmelding.copy(
                        validation = eksternSykmeldingMelding.validation,
                        validationOppdatert = tidspunkt,
                    )
            }
            return oppdatertSykmelding
        }

        fun lagSykemldingNotifikasjon(sykmelding: Sykmelding): SykmeldingNotifikasjon =
            SykmeldingNotifikasjon(
                sykmeldingId = sykmelding.sykmeldingId,
                status =
                    when (sykmelding.validation.status) {
                        RuleType.INVALID -> SykmeldingNotifikasjonStatus.INVALID
                        RuleType.PENDING -> SykmeldingNotifikasjonStatus.MANUAL_PROCESSING
                        RuleType.OK -> SykmeldingNotifikasjonStatus.OK
                    },
                mottattDato =
                    sykmelding.sykmeldingGrunnlag.metadata.mottattDato
                        .toLocalDateTime(),
                fnr = sykmelding.pasientFnr,
            )
    }
}
