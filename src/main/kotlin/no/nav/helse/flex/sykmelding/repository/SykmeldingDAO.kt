package no.nav.helse.flex.sykmelding.db

import no.nav.helse.flex.logger
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfall
import no.nav.helse.flex.objectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tilOsloInstant
import java.time.Instant
import java.time.LocalDateTime

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingDbRecord, String> {
    fun findBySykmeldingId(sykmeldingId: String): SykmeldingDbRecord?
    fun findByFnrIn(fnrs: List<String>): List<SykmeldingDbRecord>
}

@Repository
interface SykmeldingStatusRepository : CrudRepository<SykmeldingStatusDbRecord, String> {
    fun findBySykmeldingIdOrderByTimestampDesc(sykmeldingId: String): List<SykmeldingStatusDbRecord>
    fun findBySykmeldingIdIn(sykmeldingIds: Set<String>): List<SykmeldingStatusDbRecord>
}

@Service
@Transactional
class SykmeldingDAO(
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingStatusRepository: SykmeldingStatusRepository,
) {
    val log = logger()

    fun finnSykmeldinger(identer: List<String>): List<SykmeldingMedBehandlingsutfall> {
        return sykmeldingRepository.findByFnrIn(identer)
            .map { it.tilSykmeldingMedBehandlingsutfall() }
    }

    fun finnSykmelding(sykmeldingId: String): SykmeldingMedBehandlingsutfall? {
        return sykmeldingRepository.findBySykmeldingId(sykmeldingId)
            ?.tilSykmeldingMedBehandlingsutfall()
    }

    fun lagreSykmelding(sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfall) {
        val dbRecord = SykmeldingDbRecord(
            sykmeldingId = sykmeldingMedBehandlingsutfall.sykmelding.id,
            fnr = sykmeldingMedBehandlingsutfall.sykmelding.pasient.fnr,
            behandlingsutfall = objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.behandlingsutfall),
            sykmelding = objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding),
            opprettet = Instant.now()
        )
        sykmeldingRepository.save(dbRecord)
    }

    fun bekreftSykmelding(sykmeldingId: String, bekreftetDato: LocalDateTime) {
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
            ?: throw IllegalArgumentException("Fant ikke sykmelding med id $sykmeldingId")

        sykmeldingRepository.save(sykmelding.copy(bekreftetDato = bekreftetDato.tilOsloInstant()))
    }
}

data class SykmeldingDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val fnr: String,
    val behandlingsutfall: String, // JSONB
    val sykmelding: String, // JSONB
    val opprettet: Instant,
    val bekreftetDato: Instant? = null,
    val sendtDato: Instant? = null,
    val utgatt: Instant? = null,
    val avbrutt: Instant? = null,
) {
    fun tilSykmeldingMedBehandlingsutfall(): SykmeldingMedBehandlingsutfall {
        return SykmeldingMedBehandlingsutfall(
            behandlingsutfall = objectMapper.readValue(behandlingsutfall, no.nav.helse.flex.sykmelding.domain.Behandlingsutfall::class.java),
            sykmelding = objectMapper.readValue(sykmelding, no.nav.helse.flex.sykmelding.domain.Sykmelding::class.java)
        )
    }
}

data class SykmeldingStatusDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val timestamp: Instant,
    val status: String,
    val arbeidsgiver: String?, // JSONB
    val sporsmal: String?, // JSONB
    val opprettet: Instant
)
