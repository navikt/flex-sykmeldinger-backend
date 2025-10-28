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
                    sykmeldingRepository.findBySykmeldingId("a65d0de2-7903-4622-a418-f810c0d9fbff")
                        ?: error("Fant ikke sykmelding")
                arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(fnr = sykmelding.pasientFnr)
            } catch (e: Exception) {
                logger.error("Klarte ikke synkronisere spesifikt arbeidsforhold: ${e.message}")
            }
        }
    }
}
