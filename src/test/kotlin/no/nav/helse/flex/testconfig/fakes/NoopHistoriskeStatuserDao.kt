package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.tsmsykmeldingstatus.HistoriskeStatuserDao
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import java.time.Instant

class NoopHistoriskeStatuserDao : HistoriskeStatuserDao {
    override fun lesNesteSykmeldingIderForBehandling(): List<String> {
        TODO("Not implemented")
    }

    override fun lesAlleBehandledeSykmeldingIder(): List<String> {
        TODO("Not implemented")
    }

    override fun lesAlleStatuserForSykmelding(sykmeldingId: String): List<SykmeldingStatusKafkaDTO> {
        TODO("Not implemented")
    }

    override fun settAlleStatuserForSykmeldingLest(
        sykmeldingId: String,
        tidspunkt: Instant,
    ) {
        TODO("Not implemented")
    }
}
