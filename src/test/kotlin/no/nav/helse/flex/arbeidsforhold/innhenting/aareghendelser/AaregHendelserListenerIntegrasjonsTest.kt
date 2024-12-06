package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.serialisertTilString
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import java.time.Duration

class AaregHendelserListenerIntegrasjonsTest : FellesTestOppsett() {
    @Value("\${AAREG_HENDELSE_TOPIC}")
    lateinit var aaregTopic: String

    @AfterEach
    fun rivNed() {
        slettDatabase()
    }

    @Test
    fun `burde lese arbeidsforhold hendelse, og lagre endret arbeidsforhold fra aareg + erege`() {
        val record: ProducerRecord<String, String> =
            ProducerRecord(
                aaregTopic,
                null,
                "key",
                lagArbeidsforholdHendelse(fnr = "_").serialisertTilString(),
            )
        kafkaProducer.send(record).get()

        await().atMost(Duration.ofSeconds(5)).until {
            arbeidsforholdRepository.findAll().count() >= 1
        }
    }
}
