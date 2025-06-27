package no.nav.helse.flex.jobber

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.utils.logger
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Component
class LesArbeidsforholdTilAlleMedSykmelding(
    private val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService,
    private val historiskArbeidsforholdCheckpointRepository: HistoriskArbeidsforholdCheckpointRepository,
) {
    private val log = logger()
    private var erFerdig = false
    private var exceptionCount: Int = 0
    private var batchAntallProsessert: Int = 0

    @Scheduled(fixedDelay = 1, initialDelay = 10_000)
    fun run() {
        if (erFerdig) {
            Thread.sleep(100)
            return
        }

        try {
            batchAntallProsessert += lesArbeidsforholdPerFnr()

            if (batchAntallProsessert >= 10_000) {
                log.info(
                    "LesArbeidsforholdTilAlleMedSykmelding prosessert batch: $batchAntallProsessert",
                )
                batchAntallProsessert = 0
            }
        } catch (ex: Exception) {
            exceptionCount++
            if (exceptionCount > 500) {
                log.error("LesArbeidsforholdTilAlleMedSykmelding har feilet mer enn 500 ganger, stopper jobben", ex)
                erFerdig = true
            } else {
                log.warn("LesArbeidsforholdTilAlleMedSykmelding feilet, prøver igjen", ex)
            }
        }
    }

    fun lesArbeidsforholdPerFnr(): Int {
        val unikeFnr =
            historiskArbeidsforholdCheckpointRepository
                .findTop1000ByHentetArbeidsforholdIs(false)
                .map { it.fnr }
                .toSet()
        if (unikeFnr.isEmpty()) {
            erFerdig = true
            log.info("Ferdig med jobben")
            return 0
        }

        unikeFnr.forEach {
            arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(it)
            historiskArbeidsforholdCheckpointRepository.save(
                HistoriskArbeidsforholdCheckpoint(
                    fnr = it,
                    hentetArbeidsforhold = true,
                ),
            )
        }
        return unikeFnr.size
    }
}

@Table("temp_historisk_arbeidsforhold_checkpoint")
class HistoriskArbeidsforholdCheckpoint(
    @Id
    val fnr: String,
    val hentetArbeidsforhold: Boolean,
)

interface HistoriskArbeidsforholdCheckpointRepository : CrudRepository<HistoriskArbeidsforholdCheckpoint, String> {
    fun findTop1000ByHentetArbeidsforholdIs(hentetArbeidsforhold: Boolean): List<HistoriskArbeidsforholdCheckpoint>
}

@Repository
interface HistoriskArbeidsforholdCheckpointRepositoryDb : HistoriskArbeidsforholdCheckpointRepository
