package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforhold
import no.nav.helse.flex.serialisertTilString
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import java.time.Duration

class AaregHendelserListenerIntegrasjonsTest : FellesTestOppsett() {
    @Value("\${AAREG_HENDELSE_TOPIC}")
    lateinit var aaregTopic: String

    @Test
    fun `listener settes opp korrekt`() {
        arbeidsforholdRepository.save(lagArbeidsforhold())
        val record: ProducerRecord<String, String> =
            ProducerRecord(
                aaregTopic,
                null,
                "key",
                lagArbeidsforholdHendelse(fnr = "fnr").serialisertTilString(),
            )
        kafkaProducer.send(record).get()

        await().atMost(Duration.ofSeconds(5)).until {
            arbeidsforholdRepository.getAllByFnr("fnr").count() == 1
        }
    }
}
