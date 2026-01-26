package no.nav.helse.flex.jobber

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.arbeidsforhold.manuellsynk.TempSynkroniserArbeidsforholdRepository
import no.nav.helse.flex.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.collections.chunked
import kotlin.collections.map

@Component
class SynkroniserArbeidsforholdJobb(
    private val tempSynkroniserArbeidsforholdRepository: TempSynkroniserArbeidsforholdRepository,
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
) {
    val log = logger()

    @Transactional(rollbackFor = [Exception::class])
    @Scheduled(initialDelay = 10_000, fixedDelay = 100, timeUnit = TimeUnit.MILLISECONDS)
    fun kjørJobb() {
        log.info("Starter SynkroniserArbeidsforholdJobb")
        val alleArbeidsforhold = tempSynkroniserArbeidsforholdRepository.findNextBatch(500)
        val personerFnrBatcher = alleArbeidsforhold.chunked(10)

        personerFnrBatcher.forEach { fnrBatch ->
            val tasks =
                fnrBatch.map {
                    arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPersonAsync(fnr = it.fnr)
                }
            CompletableFuture.allOf(*tasks.toTypedArray()).join()
        }

        tempSynkroniserArbeidsforholdRepository.saveAll(
            alleArbeidsforhold.map {
                it.copy(lest = true)
            },
        )

        log.info("Hentet arbeidsforhold for ${alleArbeidsforhold.size} personer")
    }
}
