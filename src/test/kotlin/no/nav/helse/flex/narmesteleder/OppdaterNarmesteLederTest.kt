package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.FakesTestOppsett
import no.nav.helse.flex.notFoundDispatcher
import no.nav.helse.flex.pdl.lagGetPersonResponseData
import no.nav.helse.flex.pdl.lagGraphQlResponse
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.simpleDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class OppdaterNarmesteLederTest : FakesTestOppsett() {
    @Autowired
    lateinit var oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder

    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

    @BeforeAll
    fun beforeAll() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher { lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Supreme", etternavn = "Leader")) }
    }

    @AfterAll
    fun afterAll() {
        pdlMockWebServer.dispatcher = notFoundDispatcher
    }

    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @Test
    fun `Oppretter ny nærmeste leder hvis den ikke finnes fra før og er aktiv`() {
        val narmesteLederId = UUID.randomUUID()
        narmesteLederRepository.findByNarmesteLederId(narmesteLederId).shouldBeNull()

        val narmesteLederLeesah = lagNarmesteLederLeesah(narmesteLederId)
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(narmesteLederLeesah.serialisertTilString())

        val narmesteLeder = narmesteLederRepository.findByNarmesteLederId(narmesteLederId)
        narmesteLeder.shouldNotBeNull()
        narmesteLeder.orgnummer `should be equal to` narmesteLederLeesah.orgnummer
        narmesteLeder.narmesteLederNavn `should be equal to` "Supreme Leader"
    }

    @Test
    fun `Ignorerer melding om ny nærmeste leder hvis den ikke finnes fra før og er inaktiv`() {
        val narmesteLederId = UUID.randomUUID()
        val narmesteLederLeesah = lagNarmesteLederLeesah(narmesteLederId, aktivTom = LocalDate.now())

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(narmesteLederLeesah.serialisertTilString())

        narmesteLederRepository.findByNarmesteLederId(narmesteLederId).shouldBeNull()
    }

    @Test
    fun `Oppdaterer nærmeste leder hvis den finnes fra før og er aktiv`() {
        val narmesteLederId = UUID.randomUUID()
        val narmesteLederLeesah = lagNarmesteLederLeesah(narmesteLederId, orgnummer = "999999")
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(narmesteLederLeesah.serialisertTilString())

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(
            lagNarmesteLederLeesah(
                narmesteLederId,
                orgnummer = "888888",
            ).serialisertTilString(),
        )

        val oppdatertNl = narmesteLederRepository.findByNarmesteLederId(narmesteLederId)!!
        oppdatertNl.orgnummer `should be equal to` "888888"
    }

    @Test
    fun `Sletter nærmeste leder hvis den finnes fra før og er inaktiv`() {
        val narmesteLederId = UUID.randomUUID()
        val narmesteLederLeesah = lagNarmesteLederLeesah(narmesteLederId)

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(narmesteLederLeesah.serialisertTilString())

        narmesteLederRepository.findByNarmesteLederId(narmesteLederId).shouldNotBeNull()

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(
            lagNarmesteLederLeesah(
                narmesteLederId,
                aktivTom = LocalDate.now(),
            ).serialisertTilString(),
        )

        narmesteLederRepository.findByNarmesteLederId(narmesteLederId).shouldBeNull()
    }
}
