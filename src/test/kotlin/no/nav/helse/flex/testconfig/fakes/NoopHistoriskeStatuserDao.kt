package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.tsmsykmeldingstatus.HistoriskeStatuserDao
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import java.time.Instant

class NoopHistoriskeStatuserDao : HistoriskeStatuserDao {
    override fun lesAlleStatuserEldstTilNyest(
        fraTimestamp: Instant,
        tilTimestamp: Instant,
        antall: Int,
    ): List<SykmeldingStatusKafkaDTO> {
        TODO("Not yet implemented")
    }

    override fun oppdaterCheckpointStatusTimestamp(statusTimestamp: Instant) {
        TODO("Not yet implemented")
    }

    override fun lesCheckpointStatusTimestamp(): Instant? {
        TODO("Not yet implemented")
    }

    override fun lesAlleMedId(sykmeldingIder: Iterable<String>): List<SykmeldingStatusKafkaDTO> {
        TODO("Not yet implemented")
    }
}
