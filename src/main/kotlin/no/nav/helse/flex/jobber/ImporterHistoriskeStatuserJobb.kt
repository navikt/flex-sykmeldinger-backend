package no.nav.helse.flex.jobber

import no.nav.helse.flex.tsmsykmeldingstatus.HistoriskeStatuserProsessor
import no.nav.helse.flex.utils.logger
import org.springframework.scheduling.annotation.Scheduled

// @Component
class ImporterHistoriskeStatuserJobb(
    private val historiskeStatuserProsessor: HistoriskeStatuserProsessor,
) {
    private val log = logger()
    private var erFerdig = false
    private var exceptionCount: Int = 0
    private var batchAntallProsessert: Int = 0
    private var batchAntallLagtTil: Int = 0

    @Scheduled(fixedDelay = 1, initialDelay = 10_000)
    fun run() {
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
