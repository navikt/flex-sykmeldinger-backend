package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.innhenting.ArbeidsforholdInnhentingService
import no.nav.helse.flex.arbeidsforhold.innhenting.RegistrertePersonerForArbeidsforhold
import no.nav.helse.flex.arbeidsforhold.innhenting.SynkroniserteArbeidsforhold
import no.nav.helse.flex.serialisertTilString
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.kafka.support.Acknowledgment

class AaregHendelserListenerTest {
    fun arbeidsforholdInnhentingService(): ArbeidsforholdInnhentingService =
        mock {
            on { synkroniserArbeidsforholdForPerson(any()) } doReturn SynkroniserteArbeidsforhold()
        }

    @Test
    fun `burde synkronisere eksisterende persons arbeidsforhold`() {
        val registrertePersonerForArbeidsforhold: RegistrertePersonerForArbeidsforhold =
            mock {
                on { erPersonRegistrert("fnr_med_sykmelding") } doReturn true
            }
        val listener = AaregHendelserConsumer(registrertePersonerForArbeidsforhold, mock())
        listener.skalSynkroniseres("fnr_med_sykmelding") `should be` true
    }

    @Test
    fun `burde ikke synkronisere ny persons arbeidsforhold`() {
        val registrertePersonerForArbeidsforhold: RegistrertePersonerForArbeidsforhold =
            mock {
                on { erPersonRegistrert("fnr_uten_sykmelding") } doReturn false
            }
        val listener = AaregHendelserConsumer(registrertePersonerForArbeidsforhold, mock())
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
    fun `håndterer aaregHendelse dersom person er registert`() {
        val registrertePersonerForArbeidsforhold: RegistrertePersonerForArbeidsforhold =
            mock {
                on { erPersonRegistrert("fnr_med_sykmelding") } doReturn true
            }
        val arbeidsforholdInnhentingService = arbeidsforholdInnhentingService()
        val listener = AaregHendelserConsumer(registrertePersonerForArbeidsforhold, arbeidsforholdInnhentingService)
        val hendelse = lagArbeidsforholdHendelse()

        listener.handterHendelse(hendelse)
        verify(arbeidsforholdInnhentingService).synkroniserArbeidsforholdForPerson("fnr_med_sykmelding")
    }

    @ParameterizedTest
    @ValueSource(strings = ["Ansettelsesdetaljer", "Ansettelsesperiode"])
    fun `burde behandle aaregHendelse som er opprettet`(entitetsendringString: String) {
        val entitetsendringer = listOf(Entitetsendring.valueOf(entitetsendringString))
        val hendelse =
            lagArbeidsforholdHendelse(
                endringstype = Endringstype.Endring,
                entitetsendringer = entitetsendringer,
            )
        var handtering = AaregHendelserConsumer.avgjorHendelseshandtering(hendelse)
        handtering `should be equal to` AaregHendelseHandtering.OPPRETT_OPPDATER
    }

    @ParameterizedTest
    @ValueSource(strings = ["Ansettelsesdetaljer", "Ansettelsesperiode"])
    fun `burde behandle aaregHendelse som er endret`(entitetsendringString: String) {
        val entitetsendringer = listOf(Entitetsendring.valueOf(entitetsendringString))
        val hendelse =
            lagArbeidsforholdHendelse(
                endringstype = Endringstype.Endring,
                entitetsendringer = entitetsendringer,
            )
        var handtering = AaregHendelserConsumer.avgjorHendelseshandtering(hendelse)
        handtering `should be equal to` AaregHendelseHandtering.OPPRETT_OPPDATER
    }

    @Test
    fun `burde behandle aaregHendelse som er slettet`() {
        val hendelse =
            lagArbeidsforholdHendelse(
                endringstype = Endringstype.Sletting,
            )
        var handtering = AaregHendelserConsumer.avgjorHendelseshandtering(hendelse)
        handtering `should be equal to` AaregHendelseHandtering.SLETT
    }

    @Test
    fun `burde ikke behandle aaregHendelse som er endret med uviktig entitetsendring`() {
        val hendelse =
            lagArbeidsforholdHendelse(
                endringstype = Endringstype.Endring,
                entitetsendringer = listOf(Entitetsendring.Permittering),
            )
        var handtering = AaregHendelserConsumer.avgjorHendelseshandtering(hendelse)
        handtering `should be equal to` AaregHendelseHandtering.IGNORER
    }
}

fun lagArbeidsforholdHendelse(
    fnr: String = "fnr_med_sykmelding",
    endringstype: Endringstype = Endringstype.Opprettelse,
    entitetsendringer: List<Entitetsendring> = listOf(Entitetsendring.Ansettelsesdetaljer),
): ArbeidsforholdHendelse {
    return ArbeidsforholdHendelse(
        id = 1L,
        endringstype = endringstype,
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
        entitetsendringer = entitetsendringer,
    )
}
