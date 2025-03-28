package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.producers.sykmeldingstatus.STATUS_LEESAH_SOURCE
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatusEndrer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service

@Service
class StatusHandterer(
    private val sykmeldingHendelseKonverterer: SykmeldingHendelseKonverterer,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusEndrer: SykmeldingStatusEndrer,
) {
    private val log = logger()

    fun handterStatus(status: SykmeldingStatusKafkaMessageDTO): Boolean {
        val hendelse: SykmeldingHendelse = sykmeldingHendelseKonverterer.konverterStatusTilSykmeldingHendelse(status)
        if (status.kafkaMetadata.source != STATUS_LEESAH_SOURCE) {
            log.info(
                "Håndterer hendelse ${hendelse.status} for sykmelding ${status.kafkaMetadata.sykmeldingId}, " +
                    "fra source ${status.kafkaMetadata.source}",
            )
            val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
            if (sykmelding != null) {
                sykmeldingStatusEndrer.sjekkStatusEndring(sykmelding = sykmelding, nyStatus = hendelse.status)
                sykmeldingRepository.save(sykmelding.leggTilHendelse(hendelse))
            } else {
                publiserPaRetryTopic(hendelse).also {
                    log.info(
                        "Fant ikke sykmelding med id ${status.kafkaMetadata.sykmeldingId}, " +
                            "publiserer hendelse på retry topic",
                    )
                    return false
                }
            }
            log.info("Hendelse ${hendelse.status} for sykmelding ${status.kafkaMetadata.sykmeldingId} lagret")
            return true
        } else {
            log.info("Hendelse er fra flex-sykmeldinger-backend, ignorerer")
            return false
        }
    }

    fun publiserPaRetryTopic(hendelse: SykmeldingHendelse) {
        // TODO
    }
}
