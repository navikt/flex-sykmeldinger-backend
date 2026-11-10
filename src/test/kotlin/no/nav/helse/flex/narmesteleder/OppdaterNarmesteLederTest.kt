package no.nav.helse.flex.narmesteleder

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.arbeidsgiverdetaljer.ArbeidsgiverDetaljerService
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import no.nav.helse.flex.utils.serialisertTilString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class OppdaterNarmesteLederTest : FakesTestOppsett() {
    @Autowired
    lateinit var oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder

    @Autowired
    lateinit var pdlClient: PdlClientFake

    @Autowired
    lateinit var arbeidsgiverDetaljerService: ArbeidsgiverDetaljerService

    @AfterEach
    fun afterEach() {
        slettDatabase()
        pdlClient.reset()
    }

    @Test
    fun `aktiv melding oppretter leder med PDL-navn`() {
        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
        pdlClient.setFormatertNavn("Leder Navnesen")

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding.serialisertTilString())

        val lagret = narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldNotBeNull()
        lagret.narmesteLederNavn shouldBe "Leder Navnesen"
        lagret.aktivTom.shouldBeNull()
        lagret.brukerFnr shouldBe melding.fnr
    }

    @Test
    fun `aktiv melding oppdaterer samme rad også ved eldre timestamp eller duplikat`() {
        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding.serialisertTilString())
        val lagret = narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldNotBeNull()
        val eldre = melding.copy(orgnummer = "888888", timestamp = melding.timestamp.minusSeconds(1))

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(eldre.serialisertTilString())
        val oppdatert = narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldNotBeNull()
        oppdatert.id shouldBe lagret.id
        oppdatert.orgnummer shouldBe "888888"
        oppdatert.timestamp shouldBe eldre.timestamp.toInstant().truncatedTo(java.time.temporal.ChronoUnit.MICROS)
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(eldre.serialisertTilString())
        narmesteLederRepository.count() shouldBe 1
    }

    @Test
    fun `inaktiv melding sletter eksisterende rad uten PDL-oppslag`() {
        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding.serialisertTilString())
        pdlClient.setFormatertNavn(IllegalStateException("PDL skal ikke kalles ved avkrefting"))

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(
            melding.copy(aktivTom = LocalDate.now(), timestamp = melding.timestamp.minusSeconds(1)).serialisertTilString(),
        )

        narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldBeNull()
        narmesteLederRepository.count() shouldBe 0
    }

    @Test
    fun `inaktiv melding uten eksisterende rad ignoreres uten PDL`() {
        pdlClient.setFormatertNavn(IllegalStateException("PDL skal ikke kalles for ukjent inaktiv leder"))
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(
            lagNarmesteLederLeesah(UUID.randomUUID(), aktivTom = LocalDate.now()).serialisertTilString(),
        )
        narmesteLederRepository.count() shouldBe 0
    }

    @Test
    fun `ny aktiv melding etter avkrefting oppretter leder på nytt`() {
        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding.serialisertTilString())
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding.copy(aktivTom = LocalDate.now()).serialisertTilString())
        narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldBeNull()

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding.serialisertTilString())

        narmesteLederRepository
            .findByNarmesteLederId(melding.narmesteLederId)
            .shouldNotBeNull()
            .aktivTom
            .shouldBeNull()
        narmesteLederRepository.count() shouldBe 1
    }

    @Test
    fun `avkrefting fjerner lederen fra eksisterende ledertabell og arbeidsgiverdetaljene`() {
        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
        arbeidsforholdRepository.save(lagArbeidsforhold(fnr = melding.fnr, orgnummer = melding.orgnummer))
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding.serialisertTilString())
        arbeidsgiverDetaljerService
            .hentArbeidsgiverDetaljerForPerson(PersonIdenter(melding.fnr))
            .single()
            .naermesteLeder
            .shouldNotBeNull()

        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding.copy(aktivTom = LocalDate.now()).serialisertTilString())

        narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldBeNull()
        arbeidsgiverDetaljerService
            .hentArbeidsgiverDetaljerForPerson(PersonIdenter(melding.fnr))
            .single()
            .naermesteLeder
            .shouldBeNull()
    }
}
