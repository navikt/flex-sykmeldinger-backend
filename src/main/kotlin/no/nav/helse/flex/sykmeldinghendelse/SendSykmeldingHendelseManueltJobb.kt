package no.nav.helse.flex.sykmeldinghendelse

import no.nav.helse.flex.config.LeaderElection
import no.nav.helse.flex.sykmelding.SykmeldingRepository
import no.nav.helse.flex.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class SendSykmeldingHendelseManueltJobb(
    private val leaderElection: LeaderElection,
    private val sykmeldingHendelsePubliserer: SykmeldingHendelsePubliserer,
    private val sykmeldingRepository: SykmeldingRepository,
) {
    private val log = logger()

    @Scheduled(fixedDelay = 3_600, initialDelay = 3, timeUnit = TimeUnit.MINUTES)
    fun sendSykmeldingHendelseManuelt() {
        if (leaderElection.isLeader()) {
            val sykmelding1 =
                sykmeldingRepository.findBySykmeldingId("8f2be0fa-7c0c-4333-9287-09d7ae558355")
            val sykmelding2 =
                sykmeldingRepository.findBySykmeldingId("ccc44045-5312-4acc-aa5b-5fa51470b8a0")

            sykmeldingHendelsePubliserer.publiserSisteHendelse(sykmelding1!!)
            log.info("Sendt sykmeldinghendelse manuelt for sykmelding 8f2be0fa-7c0c-4333-9287-09d7ae558355")

            sykmeldingHendelsePubliserer.publiserSisteHendelse(sykmelding2!!)
            log.info("Sendt sykmeldinghendelse manuelt for sykmelding ccc44045-5312-4acc-aa5b-5fa51470b8a0")
        }
    }
}
