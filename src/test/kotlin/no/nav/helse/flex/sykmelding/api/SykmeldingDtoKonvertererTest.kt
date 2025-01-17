package no.nav.helse.flex.sykmelding.api

import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.time.Instant

class SykmeldingDtoKonvertererTest {
    @Test
    fun `burde konvertere`() {
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                statuser =
                    listOf(
                        SykmeldingStatus(
                            status = "NY",
                            opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                        ),
                    ),
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                oppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
            )

        val konverterer = SykmeldingDtoKonverterer()

        konverterer.konverter(sykmelding) `should be equal to` SykmeldingDto(sykmeldingId = "1")
    }
}
