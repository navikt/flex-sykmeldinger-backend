package no.nav.helse.flex.arbeidsforhold.menuellsynk

import no.nav.helse.flex.arbeidsforhold.manuellsynk.SynkroniserArbeidsforhold
import no.nav.helse.flex.arbeidsforhold.manuellsynk.TempSynkroniserArbeidsforholdRepository
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TempSynkroniserArbeidsforholdRepositoryTest : IntegrasjonTestOppsett() {
    @Autowired
    private lateinit var tempSynkroniserArbeidsforholdRepository: TempSynkroniserArbeidsforholdRepository

    @AfterEach
    fun cleanUp() {
        tempSynkroniserArbeidsforholdRepository.deleteAll()
    }

    @Test
    fun `burde lagre og lese neste batch`() {
        tempSynkroniserArbeidsforholdRepository.saveAll(
            listOf(
                SynkroniserArbeidsforhold(fnr = "1"),
                SynkroniserArbeidsforhold(fnr = "2"),
            ),
        )

        tempSynkroniserArbeidsforholdRepository
            .findNextBatch(1)
            .shouldHaveSize(1)
            .first()
            .run {
                id.shouldNotBeNull()
            }
    }

    @Test
    fun `burde kun lese batch uten leste`() {
        tempSynkroniserArbeidsforholdRepository.saveAll(
            listOf(
                SynkroniserArbeidsforhold(fnr = "1", lest = true),
                SynkroniserArbeidsforhold(fnr = "2"),
            ),
        )

        tempSynkroniserArbeidsforholdRepository
            .findNextBatch(2)
            .shouldHaveSize(1)
            .first()
            .run {
                fnr shouldBeEqualTo "2"
            }
    }
}
