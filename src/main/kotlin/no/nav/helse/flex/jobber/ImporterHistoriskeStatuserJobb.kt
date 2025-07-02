package no.nav.helse.flex.jobber

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.LeaderElection
import no.nav.helse.flex.tsmsykmeldingstatus.HistoriskeStatuserProsessor
import no.nav.helse.flex.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ImporterHistoriskeStatuserJobb(
    private val historiskeStatuserProsessor: HistoriskeStatuserProsessor,
    private val leaderElection: LeaderElection,
) {
    private val log = logger()
    private var erFerdig = false
    private var exceptionCount: Int = 0
    private var batchAntallProsessert: Int = 0
    private var batchAntallLagtTil: Int = 0

    @WithSpan
    @Scheduled(fixedDelay = 1, initialDelay = 1000 * 120)
    fun run() {
        val erLeder =
            try {
                leaderElection.isLeader()
            } catch (e: Exception) {
                log.error(
                    "Feil ved sjekk av leder i ImporterHistoriskeStatuserJobb",
                    e,
                )
                false
            }

        if (!erLeder) {
            log.info("ImporterHistoriskeStatuserJobb er ikke leder, hopper over kjøring")
            Thread.sleep(10_000)
            return
        }
        if (erFerdig) {
            Thread.sleep(100)
            return
        }

        try {
            val result = historiskeStatuserProsessor.prosesserNesteBatch(antall = 1000)
            if (result.status == HistoriskeStatuserProsessor.ResultatStatus.FERDIG) {
                erFerdig = true
                log.info("ImporterHistoriskeStatuserJobb ferdig")
            }
            batchAntallProsessert += result.antallProsessert
            batchAntallLagtTil += result.antallLagtTil
            if (batchAntallProsessert >= 10_000) {
                log.info(
                    "ImporterHistoriskeStatuserJobb prosessert batch: " +
                        "antallProsessert=$batchAntallProsessert, antallLagtTil=$batchAntallLagtTil",
                )
                batchAntallProsessert = 0
                batchAntallLagtTil = 0
            }
        } catch (ex: Exception) {
            exceptionCount++
            if (exceptionCount > 500) {
                log.error("ImporterHistoriskeStatuserJobb har feilet mer enn 500 ganger, stopper jobben", ex)
                erFerdig = true
            } else {
                log.warn("ImporterHistoriskeStatuserJobb feilet, prøver igjen", ex)
            }
        }
    }
}
