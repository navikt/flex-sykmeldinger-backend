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

    @Scheduled(fixedDelay = 0)
    fun run() {
        if (erFerdig) {
            return
        }

        try {
            val result = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
            if (result.status == HistoriskeStatuserProsessor.ResultatStatus.FERDIG) {
                erFerdig = true
                log.info("ImporterHistoriskeStatuserJobb ferdig")
            } else if (result.status == HistoriskeStatuserProsessor.ResultatStatus.PROV_IGJEN) {
                log.warn("ImporterHistoriskeStatuserJobb må kjøre på nytt, går fint om dette ikke skjer mange ganger på rad")
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
