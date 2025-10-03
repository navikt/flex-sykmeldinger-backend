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
            try {
                logger.info("Synkroniserer spesifikt arbeidsforhold")
                val sykmelding =
                    sykmeldingRepository.findBySykmeldingId("d838219b-8c6f-4684-b62b-75e43a9e39d6")
                        ?: error("Fant ikke sykmelding")
                arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(fnr = sykmelding.pasientFnr)
            } catch (e: Exception) {
                logger.error("Klarte ikke synkronisere spesifikt arbeidsforhold: ${e.message}")
            }
        }
    }
}
