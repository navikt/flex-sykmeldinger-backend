package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.time.Instant

class SykmeldingStatusKafkaDTOKonvertererTest {
    val konverterer = SykmeldingStatusKafkaDTOKonverterer()

    @Test
    fun `burde ha riktig sykmeldingId`() {
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))
        val dto = konverterer.konverter(sykmelding)
        dto.sykmeldingId `should be equal to` "1"
    }

    @Test
    fun `burde ha riktig timestamp`() {
        val sykmelding =
            lagSykmelding().leggTilStatus(
                SykmeldingHendelse(opprettet = Instant.parse("2021-01-01T00:00:00Z"), status = HendelseStatus.SENDT_TIL_NAV),
            )
        val dto = konverterer.konverter(sykmelding)

        dto.timestamp `should be equal to` Instant.parse("2021-01-01T00:00:00Z").tilNorgeOffsetDateTime()
    }

    @Test
    fun `burde ha riktig statusEvent`() {
        for ((sykmeldingStatus, expectedDtoStatusEvent) in listOf(
            HendelseStatus.APEN to "APEN",
            HendelseStatus.AVBRUTT to "AVBRUTT",
            HendelseStatus.SENDT_TIL_NAV to "BEKREFTET",
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER to "SENDT",
            HendelseStatus.BEKREFTET_AVVIST to "BEKREFTET",
            HendelseStatus.UTGATT to "UTGATT",
        )) {
            val sykmelding = lagSykmelding().leggTilStatus(SykmeldingHendelse(status = sykmeldingStatus, opprettet = Instant.now()))
            val dto = konverterer.konverter(sykmelding)

            dto.statusEvent `should be equal to` expectedDtoStatusEvent
        }
    }
}
