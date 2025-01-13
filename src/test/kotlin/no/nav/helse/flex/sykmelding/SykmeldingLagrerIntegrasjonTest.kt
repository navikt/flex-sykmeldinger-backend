package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.kafka.SYKMELDING_TOPIC
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.Duration

class SykmeldingLagrerIntegrasjonTest : FellesTestOppsett() {

    @Test
    fun `burde lagre sykmelding fra kafka`() {
        val kafkaMelding = SykmeldingMedBehandlingsutfallMelding(
            sykmelding = lagSykmeldingGrunnlag(id = "1"),
            validation = lagValidation(),
        )
        kafkaProducer.send(
            ProducerRecord(
                SYKMELDING_TOPIC,
                null,
                kafkaMelding.sykmelding.id,
                kafkaMelding.serialisertTilString()
            )
        ).get()

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            sykemeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
        }
    }

}
