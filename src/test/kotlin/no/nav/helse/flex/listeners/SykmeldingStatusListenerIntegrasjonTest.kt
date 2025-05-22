package no.nav.helse.flex.listeners

import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.Duration

class SykmeldingStatusListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @Test
    fun `burde lagre hendelse fra kafka`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val kafkamelding =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1", fnr = "fnr"),
                event = lagSykmeldingStatusKafkaDTO(statusEvent = "SENDT"),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDINGSTATUS_TOPIC,
                    null,
                    "1",
                    kafkamelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            sykmeldingRepository.findBySykmeldingId("1")?.sisteHendelse()?.status shouldBeEqualTo HendelseStatus.SENDT_TIL_ARBEIDSGIVER
        }
    }

    @Test
    fun `burde ikke lagre hendelse fra kafka dersom sykmelding ikke finnes`() {
        val kafkamelding =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1", fnr = "fnr"),
                event = lagSykmeldingStatusKafkaDTO(statusEvent = "SENDT"),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDINGSTATUS_TOPIC,
                    null,
                    "1",
                    kafkamelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            sykmeldingRepository.findBySykmeldingId("1") shouldBeEqualTo null
        }
    }
}
