package no.nav.helse.flex.narmesteleder

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.TESTDATA_RESET_TOPIC
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class NarmesteLederRepositoryIntegrasjonTest : IntegrasjonTestOppsett() {
    @Test
    fun `Repository oppdaterer etter id uavhengig av timestamp`() {
        val opprinnelig = lagNarmesteLeder()
        val lagret = narmesteLederRepository.save(opprinnelig)
        lagret.id.shouldNotBeNull()
        val eldre = lagret.copy(orgnummer = "888888", timestamp = lagret.timestamp.minusSeconds(1))
        narmesteLederRepository.save(eldre)
        narmesteLederRepository.findByNarmesteLederId(lagret.narmesteLederId) shouldBe eldre
        narmesteLederRepository.delete(eldre)
        narmesteLederRepository.findByNarmesteLederId(lagret.narmesteLederId) shouldBe null
    }

    @Test
    fun `burde hente arbeidsforhold ved bruker fnr`() {
        val narmesteLeder = narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = "1"))
        narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = "2"))
        narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = "3"))

        val alleNarmesteLedere = narmesteLederRepository.findAllByBrukerFnrIn(listOf("1", "2"))
        alleNarmesteLedere shouldHaveSize 2

        val lagretNarmesteLeder = alleNarmesteLedere.first()
        narmesteLeder shouldBe lagretNarmesteLeder
    }

    @Test
    fun `Testdata-reset sletter nærmeste ledere bare for oppgitt ident`() {
        val fnr = "reset-test"
        val annetFnr = "reset-annen"
        narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = fnr))
        narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = fnr))
        val annen = narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = annetFnr))

        narmesteLederRepository.findAllByBrukerFnrIn(listOf(fnr)) shouldHaveSize 2
        kafkaProducer.send(ProducerRecord(TESTDATA_RESET_TOPIC, null, fnr, fnr)).get()

        await().atMost(10, TimeUnit.SECONDS).until {
            narmesteLederRepository.findAllByBrukerFnrIn(listOf(fnr)).isEmpty()
        }
        narmesteLederRepository.findAllByBrukerFnrIn(listOf(annetFnr)) shouldBe listOf(annen)
    }
}
