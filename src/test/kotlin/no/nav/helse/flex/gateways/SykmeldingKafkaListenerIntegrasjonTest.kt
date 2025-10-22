package no.nav.helse.flex.gateways

import no.nav.helse.flex.sykmelding.EksternSykmeldingMelding
import no.nav.helse.flex.sykmelding.tsm.SykmeldingType
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.lagDigitalSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagEksternSykmeldingMelding
import no.nav.helse.flex.testdata.lagPapirSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagUtenlandskSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagValidation
import no.nav.helse.flex.testdata.lagXMLSykmeldingGrunnlag
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Duration

class SykmeldingKafkaListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    data class SykmeldingTestCase(
        val typeNavn: SykmeldingType,
        val lagEksternSykmelding: (sykmeldingId: String) -> String,
    )

    @TestFactory
    fun `lagrer sykmelding fra kafka for ulike typer`() =
        listOf(
            SykmeldingTestCase(SykmeldingType.XML) { id ->
                EksternSykmeldingMelding(
                    sykmelding = lagXMLSykmeldingGrunnlag(id = id),
                    validation = lagValidation(),
                ).serialisertTilString()
            },
            SykmeldingTestCase(SykmeldingType.UTENLANDSK) { id ->
                lagEksternSykmeldingMelding(
                    sykmelding = lagUtenlandskSykmeldingGrunnlag(id = id),
                    validation = lagValidation(),
                ).serialisertTilString()
            },
            SykmeldingTestCase(SykmeldingType.PAPIR) { id ->
                lagEksternSykmeldingMelding(
                    sykmelding = lagPapirSykmeldingGrunnlag(id = id),
                    validation = lagValidation(),
                ).serialisertTilString()
            },
            SykmeldingTestCase(SykmeldingType.DIGITAL) { id ->
                lagEksternSykmeldingMelding(
                    sykmelding = lagDigitalSykmeldingGrunnlag(id = id),
                    validation = lagValidation(),
                ).serialisertTilString()
            },
        ).map { testCase ->
            DynamicTest.dynamicTest("burde lagre ${testCase.typeNavn} sykmelding fra kafka") {
                slettDatabase()
                val sykmeldingId = "1"
                val melding = testCase.lagEksternSykmelding(sykmeldingId)

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
        }

    @Test
    fun `burde tombstone sykmelding fra kafka`() {
        val topic = SYKMELDING_TOPIC
        sykmeldingRepository.save(
            lagSykmelding(sykmeldingGrunnlag = lagXMLSykmeldingGrunnlag(id = "1")),
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
