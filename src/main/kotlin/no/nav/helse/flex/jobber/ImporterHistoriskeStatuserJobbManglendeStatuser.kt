package no.nav.helse.flex.jobber

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.LeaderElection
import no.nav.helse.flex.tsmsykmeldingstatus.HistoriskeStatuserProsessorManglendeStatuser
import no.nav.helse.flex.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ImporterHistoriskeStatuserJobbManglendeStatuser(
    private val historiskeStatuserProsessorManglendeStatuser: HistoriskeStatuserProsessorManglendeStatuser,
    private val leaderElection: LeaderElection,
) {
    private val log = logger()
    private var erFerdig = false
    private var exceptionCount: Int = 0

    @WithSpan
    @Scheduled(fixedDelay = 1, initialDelay = 1000 * 120)
    fun run() {
        val erLeder =
            try {
                leaderElection.isLeader()
            } catch (e: Exception) {
                log.error(
                    "Feil ved sjekk av leder i ImporterHistoriskeStatuserManglendeStatuserJobb",
                    e,
                )
                false
            }

        if (!erLeder) {
            log.info("ImporterHistoriskeStatuserManglendeStatuserJobb er ikke leder, hopper over kjøring")
            Thread.sleep(10_000)
            return
        }
        if (erFerdig) {
            Thread.sleep(100)
            return
        }

        try {
            val result = historiskeStatuserProsessorManglendeStatuser.prosesser()
            if (result.status == HistoriskeStatuserProsessorManglendeStatuser.ResultatStatus.FERDIG) {
                erFerdig = true
                log.info(
                    "ImporterHistoriskeStatuserManglendeStatuserJobb ferdig. Antall prosessert totalt:" +
                        " ${result.antallProsessert}, antall lagt til totalt: ${result.antallLagtTil}",
                )
            }
        } catch (ex: Exception) {
            exceptionCount++
            if (exceptionCount > 500) {
                log.error("ImporterHistoriskeStatuserManglendeStatuserJobb har feilet mer enn 500 ganger, stopper jobben", ex)
                erFerdig = true
            } else {
                log.warn("ImporterHistoriskeStatuserManglendeStatuserJobb feilet, prøver igjen", ex)
            }
        }
    }
}
