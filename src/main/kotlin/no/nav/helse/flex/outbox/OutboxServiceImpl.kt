package no.nav.helse.flex.outbox

import no.nav.helse.flex.gateways.SykmeldingBrukernotifikasjonProducer
import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelsePubliserer
import no.nav.helse.flex.utils.fraPsqlJson
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.tilPsqlJson
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OutboxServiceImpl(
    private val outboxDbRepository: OutboxDbRepository,
    private val sykmeldingLeser: SykmeldingLeser,
    private val sykmeldingHendelsePubliserer: SykmeldingHendelsePubliserer,
    private val sykmeldingBrukernotifikasjonProducer: SykmeldingBrukernotifikasjonProducer,
) : OutboxService {
    private val logger = logger()

    override fun outboxSykmeldingHendelse(
        fnr: String,
        sykmeldingId: String,
        sykmeldingHendelseId: String,
    ) {
        val outboxRecord =
            OutboxDbRecord(
                type = OutboxType.SYKMELDING_HENDELSE,
                fnr = fnr,
                payload =
                    SykmeldingHendelsePayload(
                        sykmeldingId = sykmeldingId,
                        sykmeldingHendelseId = sykmeldingHendelseId,
                    ).tilPsqlJson(),
                opprettetTimestamp = Instant.now(),
                sendtTimestamp = null,
            )
        outboxDbRepository.save(outboxRecord)
    }

    override fun outboxSykmeldingBrukernotifikasjon(sykmeldingNotifikasjon: SykmeldingNotifikasjon) {
        val outboxRecord =
            OutboxDbRecord(
                type = OutboxType.SYKMELDING_BRUKERNOTIFIKASJON,
                fnr = sykmeldingNotifikasjon.fnr,
                payload = sykmeldingNotifikasjon.tilPsqlJson(),
                opprettetTimestamp = Instant.now(),
                sendtTimestamp = null,
            )
        outboxDbRepository.save(outboxRecord)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun sendUsendteForEldsteLedigeFnr(): Boolean {
        val fnr = outboxDbRepository.lasEldsteLedigeFnr() ?: return false
        // Henter radene i en ny spørring etter at låsen er tatt, så vi ser det andre instanser har sendt.
        outboxDbRepository.finnUsendteForFnr(fnr).forEach { record ->
            if (outboxDbRepository.markerSomSendt(record.id!!, Instant.now()) == 0) {
                logger.warn("Outbox-rad ${record.type} ${record.id} er allerede sendt, hopper over")
                return@forEach
            }
            sendOutboxRecord(record)
        }
        return true
    }

    private fun sendOutboxRecord(record: OutboxDbRecord) {
        when (record.type) {
            OutboxType.SYKMELDING_HENDELSE -> sendSykmeldingHendelse(record)
            OutboxType.SYKMELDING_BRUKERNOTIFIKASJON -> sendSykmeldingBrukernotifikasjon(record)
        }
    }

    private fun sendSykmeldingHendelse(record: OutboxDbRecord) {
        val payload =
            record.payload.fraPsqlJson<SykmeldingHendelsePayload>()
                ?: error("Mangler payload på outbox-rad ${record.id}")
        val sykmelding =
            sykmeldingLeser.hentSykmelding(payload.sykmeldingId)
                ?: error(
                    "Fant ikke sykmelding ${payload.sykmeldingId} for outbox-rad ${record.id}. Kan ha blitt slettet av tombstone. Burde kanskje bare returnere i dette senarioet.",
                )
        val hendelse =
            sykmelding.hendelser.firstOrNull { it.databaseId == payload.sykmeldingHendelseId }
                ?: error("Fant ikke hendelse ${payload.sykmeldingHendelseId} på sykmelding ${payload.sykmeldingId}")

        sykmeldingHendelsePubliserer.publiserHendelse(
            fnr = record.fnr,
            sykmeldingId = sykmelding.sykmeldingId,
            sykmeldingHendelse = hendelse,
        )
        logger.info(
            "Sendte outbox-rad ${record.type} ${record.id} for sykmelding ${payload.sykmeldingId} og hendelse ${payload.sykmeldingHendelseId}",
        )
    }

    private fun sendSykmeldingBrukernotifikasjon(record: OutboxDbRecord) {
        val notifikasjon =
            record.payload.fraPsqlJson<SykmeldingNotifikasjon>()
                ?: error("Mangler payload på outbox-rad ${record.id}")

        sykmeldingBrukernotifikasjonProducer.produserSykmeldingBrukernotifikasjon(notifikasjon)
        logger.info("Sendte outbox-rad ${record.type} ${record.id} for sykmelding ${notifikasjon.sykmeldingId}")
    }
}

class SykmeldingHendelsePayload(
    val sykmeldingId: String,
    val sykmeldingHendelseId: String,
)
