package no.nav.helse.flex.testdata

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TestDataResetListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    private val fnr = "11111111111"
    private val annetFnr = "22222222222"

    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @Test
    fun `burde slette testdata kun for angitt fnr`() {
        lagreTestdata(fnr)
        lagreTestdata(annetFnr)

        sendTestdataReset(fnr)

        await().atMost(10, TimeUnit.SECONDS).until {
            narmesteLederRepository.findAllByBrukerFnrIn(listOf(fnr)).isEmpty() &&
                arbeidsforholdRepository.getAllByFnrIn(listOf(fnr)).isEmpty() &&
                sykmeldingRepository.findAllByPersonIdenter(PersonIdenter(fnr)).isEmpty()
        }

        narmesteLederRepository.findAllByBrukerFnrIn(listOf(fnr)).shouldBeEmpty()
        arbeidsforholdRepository.getAllByFnrIn(listOf(fnr)).shouldBeEmpty()
        sykmeldingRepository.findAllByPersonIdenter(PersonIdenter(fnr)).shouldBeEmpty()

        narmesteLederRepository.findAllByBrukerFnrIn(listOf(annetFnr)).size shouldBe 1
        arbeidsforholdRepository.getAllByFnrIn(listOf(annetFnr)).size shouldBe 1
        sykmeldingRepository.findAllByPersonIdenter(PersonIdenter(annetFnr)).size shouldBe 1
    }

    private fun lagreTestdata(fnr: String) {
        narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = fnr))
        arbeidsforholdRepository.save(
            lagArbeidsforhold(navArbeidsforholdId = "arbeidsforhold-$fnr", fnr = fnr),
        )
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "sykmelding-$fnr",
                        pasient = lagPasient(fnr = fnr),
                    ),
            ),
        )
    }

    private fun sendTestdataReset(fnr: String) {
        kafkaProducer
            .send(
                ProducerRecord(
                    TESTDATA_RESET_TOPIC,
                    null,
                    fnr,
                    fnr,
                ),
            ).get()
    }
}
