package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.arbeidsforhold.innhenting.SynkroniserteArbeidsforhold
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforhold
import org.amshove.kluent.`should be`
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
        val listener = AaregHendelserListener(arbeidsforholdRepository, mock())
        listener.skalSynkroniseres("fnr_med_sykmelding") `should be` true
    }

    @Test
    fun `burde ikke synkronisere ny persons arbeidsforhold`() {
        val arbeidsforholdRepository: ArbeidsforholdRepository =
            mock {
                on { getAllByFnr("fnr_uten_sykmelding") } doReturn emptyList()
            }
        val listener = AaregHendelserListener(arbeidsforholdRepository, mock())
        listener.skalSynkroniseres("fnr_uten_sykmelding") `should be` false
    }

    @Test
    fun `tar imot aaregHendelse og acker`() {
        val acknowledgment = mock<Acknowledgment> {}
        val listener = AaregHendelserListener(mock(), mock())
        val record: ConsumerRecord<String, String> =
            ConsumerRecord(
                "topic",
                1,
                1L,
                "key",
                "{}",
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
        val listener = AaregHendelserListener(arbeidsforholdRepository, arbeidsforholdInnhentingService)
        val hendelse =
            ArbeidsforholdHendelse(
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
                                            ident = "fnr_med_sykmelding",
                                            gjeldende = true,
                                        ),
                                    ),
                            ),
                    ),
                entitetsendringer = listOf(Entitetsendring.Ansettelsesdetaljer),
            )

        listener.handterHendelse(hendelse)
        verify(arbeidsforholdInnhentingService).synkroniserArbeidsforholdForPerson("fnr_med_sykmelding")
    }
}
