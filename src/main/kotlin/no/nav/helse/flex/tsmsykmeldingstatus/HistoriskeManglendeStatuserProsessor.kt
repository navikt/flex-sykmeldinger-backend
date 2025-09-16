package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.sykmelding.domain.SykmeldingRepository
import no.nav.helse.flex.tsmsykmeldingstatus.dto.StatusEventKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HistoriskeManglendeStatuserProsessor(
    private val historiskeStatuserDao: HistoriskeStatuserDao,
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingHendelseFraKafkaKonverterer: SykmeldingHendelseFraKafkaKonverterer,
) {
    private val log = logger()

    enum class ResultatStatus {
        OK,
        FERDIG,
    }

    data class Resultat(
        val status: ResultatStatus,
        val antallProsessert: Int = 0,
        val antallLagtTil: Int = 0,
    )

    @Transactional(rollbackFor = [Exception::class])
    fun prosesser(): Resultat {
        val statuser =
            historiskeStatuserDao.lesAlleTempResterendeSykmeldingstatuserFraTsm()

        var statuserLagtTil = 0
        for (status in statuser) {
            val lagtTil = leggTilStatus(status)
            if (lagtTil) {
                statuserLagtTil++
            }
        }
        return Resultat(status = ResultatStatus.FERDIG, antallLagtTil = statuserLagtTil, antallProsessert = statuser.size)
    }

    private fun leggTilStatus(status: SykmeldingStatusKafkaDTO): Boolean {
        val sykmeldingId = status.sykmeldingId

        if (status.statusEvent in
            setOf(
                StatusEventKafkaDTO.SLETTET,
                StatusEventKafkaDTO.APEN,
            )
        ) {
            log.debug("Ignorerer importert status ${status.statusEvent} for sykmelding $sykmeldingId")
            return false
        }

        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            log.warn(
                "Fant ikke sykmelding for importert status, sykmelding: $sykmeldingId, status: ${status.statusEvent}. Hopper over status",
            )
            return false
        } else {
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
