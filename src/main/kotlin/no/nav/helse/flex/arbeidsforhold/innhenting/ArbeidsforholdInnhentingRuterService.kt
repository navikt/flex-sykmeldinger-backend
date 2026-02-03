package no.nav.helse.flex.arbeidsforhold.innhenting

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class ArbeidsforholdInnhentingRuterService(
    private val registrertePersonerForArbeidsforhold: RegistrertePersonerForArbeidsforhold,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
) {
    @Async("aaregHendelserKafkaTaskExecutor")
    internal fun synkroniserForPersonAsync(fnr: String): CompletableFuture<ArbeidsforholdSynkronisering> =
        if (!skalSynkroniseres(fnr)) {
            CompletableFuture.completedFuture(ArbeidsforholdSynkronisering.INGEN)
        } else {
            arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPersonFuture(fnr)
        }

    fun skalSynkroniseres(fnr: String): Boolean = registrertePersonerForArbeidsforhold.erPersonRegistrert(fnr)
}
