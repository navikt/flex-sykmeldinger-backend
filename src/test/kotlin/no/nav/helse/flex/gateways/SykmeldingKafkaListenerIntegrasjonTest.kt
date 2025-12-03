package no.nav.helse.flex.gateways

import no.nav.helse.flex.sykmelding.tsm.SykmeldingType
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Duration

class SykmeldingKafkaListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @ParameterizedTest
    @EnumSource(SykmeldingType::class)
    fun `lagrer sykmelding fra kafka for ulike typer`(sykmeldingType: SykmeldingType) {
        slettDatabase()
        val sykmeldingId = "1"
        val melding =
            when (sykmeldingType) {
                SykmeldingType.XML ->
                    lagEksternSykmeldingMelding(
                        sykmelding = lagSykmeldingGrunnlag(id = sykmeldingId),
                        validation = lagValidation(),
                    ).serialisertTilString()

                SykmeldingType.PAPIR ->
                    lagEksternSykmeldingMelding(
                        sykmelding = lagSykmeldingGrunnlag(id = sykmeldingId),
                        validation = lagValidation(),
                    ).serialisertTilString()

                SykmeldingType.DIGITAL ->
                    lagEksternSykmeldingMelding(
                        sykmelding = lagDigitalSykmeldingGrunnlag(id = sykmeldingId),
                        validation = lagValidation(),
                    ).serialisertTilString()

                SykmeldingType.UTENLANDSK ->
                    lagEksternSykmeldingMelding(
                        sykmelding = lagUtenlandskSykmeldingGrunnlag(id = sykmeldingId),
                        validation = lagValidation(),
                    ).serialisertTilString()
            }

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDING_TOPIC,
                    null,
                    sykmeldingId,
                    melding,
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).until {
            sykmeldingRepository.findBySykmeldingId(sykmeldingId) != null
        }
        sykmeldingRepository.findBySykmeldingId(sykmeldingId).shouldNotBeNull()
    }

    @Test
    fun `burde tombstone sykmelding fra kafka`() {
        val topic = SYKMELDING_TOPIC
        sykmeldingRepository.save(
            lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")),
        )

        val key = "1"
        val value = null
        kafkaProducer
            .send(ProducerRecord(topic, null, key, value))
            .get()

        await().atMost(Duration.ofSeconds(2)).until {
            sykmeldingRepository.findBySykmeldingId("1") == null
        }

        sykmeldingRepository.findBySykmeldingId("1").shouldBeNull()
    }
}
