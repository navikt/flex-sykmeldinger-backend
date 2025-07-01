package no.nav.helse.flex.jobber

import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.config.LeaderElection
import no.nav.helse.flex.utils.errorSecure
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
    private val leaderElection: LeaderElection,
) {
    private val log = logger()
    private var erFerdig = false
    private var exceptionCount: Int = 0
    private var batchAntallProsessert: Int = 0

    @Scheduled(fixedDelay = 1, initialDelay = 1000 * 120)
    fun run() {
        val erLeder =
            try {
                leaderElection.isLeader()
            } catch (e: Exception) {
                log.error(
                    "Feil ved sjekk av leder i LesArbeidsforholdTilAlleMedSykmelding",
                    e,
                )
                false
            }

        if (!erLeder) {
            log.info("LesArbeidsforholdTilAlleMedSykmelding er ikke leder, hopper over kjøring")
            Thread.sleep(10_000)
            return
        }
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
                log.errorSecure(
                    message = "LesArbeidsforholdTilAlleMedSykmelding har feilet mer enn 500 ganger, stopper jobben",
                    secureThrowable = ex,
                )
                erFerdig = true
            } else {
                log.errorSecure(
                    message = "LesArbeidsforholdTilAlleMedSykmelding feilet, prøver igjen",
                    secureThrowable = ex,
                )
                Thread.sleep(1000)
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
