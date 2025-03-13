package no.nav.helse.flex.producers.sykmeldingstatus

import com.nhaarman.mockitokotlin2.*
import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldThrow
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
                lagSykmeldingHendelse(opprettet = Instant.parse("2021-01-01T00:00:00Z")),
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

    @Test
    fun `burde bruke BrukerSvarKafkaDTOKonverterer`() {
        val brukerSvarKonvertererSpy = spy(BrukerSvarKafkaDTOKonverterer())

        val konverterer = SykmeldingStatusKafkaDTOKonverterer(brukerSvarKonvertererSpy)
        val sykmelding = lagSykmelding().leggTilStatus(lagSykmeldingHendelse(sporsmalSvar = emptyList()))
        runCatching {
            konverterer.konverter(sykmelding)
        }

        verify(brukerSvarKonvertererSpy, times(1)).konverterTilBrukerSvar(emptyList())
    }

    @Test
    fun `burde bruke SporsmalsKafkaDTOKonverter`() {
        val sporsmalsKonverter = spy(SporsmalsKafkaDTOKonverterer())

        val konverterer = SykmeldingStatusKafkaDTOKonverterer(sporsmalsKafkaDTOKonverterer = sporsmalsKonverter)

        val sporsmal = lagSporsmalListe()
        val arbeidstakerInfo = lagArbeidstakerInfo()
        val sykmelding =
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "id"),
            ).leggTilStatus(lagSykmeldingHendelse(sporsmalSvar = sporsmal, arbeidstakerInfo = arbeidstakerInfo))

        konverterer.konverter(sykmelding)

        verify(sporsmalsKonverter, times(1)).konverterTilSporsmals(
            brukerSvar = any(),
            arbeidstakerInfo = eq(arbeidstakerInfo),
            sykmeldingId = eq("id"),
        )
    }

    enum class A {
        A,
    }

    @Test
    fun `enumValueOf burde kaste IllegalArgumentException ved ugjylidig verdi`() {
        invoking {
            enumValueOf<A>("B")
        }.shouldThrow(IllegalArgumentException::class)
    }
}
