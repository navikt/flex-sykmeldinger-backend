package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.arbeidsforhold.innhenting.SynkroniserteArbeidsforhold
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforhold
import no.nav.helse.flex.serialisertTilString
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

class AaregHendelserListenerTest {
    @Test
    fun `burde synkronisere eksisterende persons arbeidsforhold`() {
        val arbeidsforholdRepository: ArbeidsforholdRepository =
            mock {
                on { getAllByFnr("fnr_med_sykmelding") } doReturn listOf(lagArbeidsforhold(fnr = "fnr_med_sykmelding"))
            }
        val listener = AaregHendelserConsumer(arbeidsforholdRepository, mock())
        listener.skalSynkroniseres("fnr_med_sykmelding") `should be` true
    }

    @Test
    fun `burde ikke synkronisere ny persons arbeidsforhold`() {
        val arbeidsforholdRepository: ArbeidsforholdRepository =
            mock {
                on { getAllByFnr("fnr_uten_sykmelding") } doReturn emptyList()
            }
        val listener = AaregHendelserConsumer(arbeidsforholdRepository, mock())
        listener.skalSynkroniseres("fnr_uten_sykmelding") `should be` false
    }

    @Test
    fun `tar imot aaregHendelse og kaster feil når hendelse har feil format`() {
        val acknowledgment = mock<Acknowledgment> {}
        val listener = AaregHendelserConsumer(mock(), mock())
        val record: ConsumerRecord<String, String> =
            ConsumerRecord(
                "topic",
                1,
                1L,
                "key",
                "{}",
            )
        invoking {
            listener.listen(record, acknowledgment)
        } `should throw` MismatchedInputException::class
    }

    @Test
    fun `tar imot aaregHendelse og acker når det er riktig format`() {
        val acknowledgment = mock<Acknowledgment> {}
        val listener = AaregHendelserConsumer(mock(), mock())
        val record: ConsumerRecord<String, String> =
            ConsumerRecord(
                "topic",
                1,
                1L,
                "key",
                lagArbeidsforholdHendelse().serialisertTilString(),
            )
        listener.listen(record, acknowledgment)
        verify(acknowledgment).acknowledge()
    }

    @Test
    fun `håndterer aaregHendelse som skal synkroniseres`() {
        val arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService =
            mock {
                on { synkroniserArbeidsforholdForPerson(any()) } doReturn SynkroniserteArbeidsforhold()
            }
        val arbeidsforholdRepository: ArbeidsforholdRepository =
            mock {
                on { getAllByFnr("fnr_med_sykmelding") } doReturn listOf(lagArbeidsforhold(fnr = "fnr_med_sykmelding"))
            }
        val listener = AaregHendelserConsumer(arbeidsforholdRepository, arbeidsforholdInnhentingService)
        val hendelse = lagArbeidsforholdHendelse()

        listener.handterHendelse(hendelse)
        verify(arbeidsforholdInnhentingService).synkroniserArbeidsforholdForPerson("fnr_med_sykmelding")
    }
}

fun lagArbeidsforholdHendelse(fnr: String = "fnr_med_sykmelding"): ArbeidsforholdHendelse {
    return ArbeidsforholdHendelse(
        id = 1L,
        endringstype = Endringstype.Endring,
        arbeidsforhold =
            ArbeidsforholdKafka(
                navArbeidsforholdId = 1,
                arbeidstaker =
                    Arbeidstaker(
                        identer =
                            listOf(
                                Ident(
                                    type = IdentType.FOLKEREGISTERIDENT,
                                    ident = fnr,
                                    gjeldende = true,
                                ),
                            ),
                    ),
            ),
        entitetsendringer = listOf(Entitetsendring.Ansettelsesdetaljer),
    )
}
