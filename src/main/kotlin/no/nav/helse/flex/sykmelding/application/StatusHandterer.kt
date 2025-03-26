package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.producers.sykmeldingstatus.STATUS_LEESAH_SOURCE
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service

@Service
class StatusHandterer(
    private val sykmeldingHendelseKonverterer: SykmeldingHendelseKonverterer,
    private val sykmeldingRepository: ISykmeldingRepository,
) {
    private val log = logger()

    fun handterStatus(status: SykmeldingStatusKafkaMessageDTO) {
        val hendelse: SykmeldingHendelse = sykmeldingHendelseKonverterer.konverterStatusTilSykmeldingHendelse(status)
        if (status.kafkaMetadata.source != STATUS_LEESAH_SOURCE) {
            log.info(
                "Håndterer hendelse ${hendelse.status} for sykmelding ${status.kafkaMetadata.sykmeldingId}, " +
                    "fra source ${status.kafkaMetadata.source}",
            )
            sykmeldingRepository
                .findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
                ?.let { sykmeldingRepository.save(it.leggTilHendelse(hendelse)) }
                ?: publiserPaRetryTopic(hendelse).also {
                    log.info(
                        "Fant ikke sykmelding med id ${status.kafkaMetadata.sykmeldingId}, " +
                            "publiserer hendelse på retry topic",
                    )
                }
        } else {
            log.info("Hendelse er fra flex-sykmeldinger-backend, ignorerer")
        }
    }

    fun publiserPaRetryTopic(hendelse: SykmeldingHendelse) {
        // TODO
    }
}
