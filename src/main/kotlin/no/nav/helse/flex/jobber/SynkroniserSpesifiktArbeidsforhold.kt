package no.nav.helse.flex.jobber

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.sykmelding.SykmeldingRepository
import no.nav.helse.flex.utils.logger
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class SynkroniserSpesifiktArbeidsforhold(
    private val sykmeldingRepository: SykmeldingRepository,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
    private val environmentToggles: EnvironmentToggles,
) : ApplicationRunner {
    private val logger = logger()

    override fun run(args: ApplicationArguments?) {
        if (environmentToggles.isProduction()) {
            logger.info("Starter synkronisering av arbeidsforhold")
            setOf("166cb0f3-6077-4df9-a9f8-bf121675e703", "bbad1471-b4fc-426d-b648-2c7063c4450a").forEach {
                try {
                    logger.info("Synkroniserer spesifikt arbeidsforhold for sykmelding $it")
                    val sykmelding =
                        sykmeldingRepository.findBySykmeldingId(it)
                            ?: error("Fant ikke sykmelding")
                    arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(fnr = sykmelding.pasientFnr)
                    logger.info("Ferdig synkronisert arbeidsforhold for sykmelding $it")
                } catch (e: Exception) {
                    logger.error("Klarte ikke synkronisere spesifikt arbeidsforhold: ${e.message}")
                }
            }
        }
    }
}
