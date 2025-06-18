package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.config.AdvisoryLock
import no.nav.helse.flex.sykmelding.domain.SykmeldingRepository
import no.nav.helse.flex.tsmsykmeldingstatus.dto.StatusEventKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Service
class HistoriskeStatuserProsessor(
    private val historiskeStatuserDao: HistoriskeStatuserDao,
    private val advisoryLock: AdvisoryLock,
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingHendelseFraKafkaKonverterer: SykmeldingHendelseFraKafkaKonverterer,
    private val nowFactory: Supplier<Instant>,
) {
    private val log = logger()

    enum class ResultatStatus {
        OK,
        PROV_IGJEN,
        FERDIG,
    }

    data class Resultat(
        val status: ResultatStatus,
        val antallProsessert: Int = 0,
        val antallLagtTil: Int = 0,
    )

    companion object {
        const val SERVICE_LOCK_KEY = "ImporterteHistoriskeStatuserProsessor"
    }

    @Transactional(rollbackFor = [Exception::class])
    fun prosesserNesteSykmeldingStatuser(): Resultat {
        val sykmeldingIder: List<String> =
            historiskeStatuserDao.lesNesteSykmeldingIderForBehandling()
        if (sykmeldingIder.isEmpty()) {
            return Resultat(status = ResultatStatus.FERDIG)
        }

        val sykmeldingId =
            sykmeldingIder.firstOrNull { id ->
                advisoryLock.tryAcquire(SERVICE_LOCK_KEY, id)
            }
        if (sykmeldingId == null) {
            return Resultat(status = ResultatStatus.PROV_IGJEN)
        }

        val statuser = historiskeStatuserDao.lesAlleStatuserForSykmelding(sykmeldingId)
        var statuserLagtTil = 0
        for (status in statuser) {
            val lagtTil = leggTilStatus(status)
            if (lagtTil) {
                statuserLagtTil++
            }
        }
        historiskeStatuserDao.settAlleStatuserForSykmeldingLest(
            sykmeldingId,
            tidspunkt = nowFactory.get(),
        )
        return Resultat(status = ResultatStatus.OK, antallLagtTil = statuserLagtTil, antallProsessert = statuser.size)
    }

    private fun leggTilStatus(status: SykmeldingStatusKafkaDTO): Boolean {
        val sykmeldingId = status.sykmeldingId
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            log.warn(
                "Fant ikke sykmelding for importert status, sykmelding: $sykmeldingId, status: ${status.statusEvent}. Hopper over status",
            )
            return false
        } else {
            when (val statusEvent = status.statusEvent) {
                StatusEventKafkaDTO.SLETTET,
                StatusEventKafkaDTO.APEN,
                -> {
                    log.debug("Ignorerer importert status $statusEvent for sykmelding '$sykmeldingId'")
                    return false
                }
                else -> {
                    val hendelse =
                        sykmeldingHendelseFraKafkaKonverterer.konverterSykmeldingHendelseFraKafkaDTO(
                            status = status,
                            erSykmeldingAvvist = sykmelding.erAvvist,
                            source = "importert-historisk-status",
                        )
                    if (SykmeldingStatusHandterer.finnesDuplikatHendelsePaaSykmelding(
                            sykmelding = sykmelding,
                            sykmeldingHendelse = hendelse,
                        )
                    ) {
                        log.info(
                            "Importert sykmelding hendelse eksisterer allerede, sykmelding: $sykmeldingId, status: ${hendelse.status}. " +
                                "Lagrer ikke hendelse",
                        )
                        return false
                    }
                    val oppdatertSykmelding = sykmelding.leggTilHendelse(hendelse)
                    sykmeldingRepository.save(oppdatertSykmelding)
                    log.info("Importert sykmelding hendelse lagret, sykmelding: $sykmeldingId, status: ${hendelse.status}")
                    return true
                }
            }
        }
    }
}
