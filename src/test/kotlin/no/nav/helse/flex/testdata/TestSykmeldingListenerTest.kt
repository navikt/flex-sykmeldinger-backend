package no.nav.helse.flex.testdata

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.sykmelding.domain.RuleType
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfall
import no.nav.helse.flex.sykmelding.domain.ValidationResult
import no.nav.helse.flex.sykmelding.domain.lagSykmelding
import org.amshove.kluent.`should not be null`
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class TestSykmeldingListenerTest : FellesTestOppsett() {

    @Autowired
    lateinit var testSykmeldingListener: TestSykmeldingListener

    @Test
    fun `burde ikke feil ved riktig kafka melding format`() {
        val sykmelding = lagSykmelding()
        val sykmeldingMedBehandlingsutfall = SykmeldingMedBehandlingsutfall(
            sykmelding = sykmelding,
            validation = ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                rules = listOf()
            )
        )
        kafkaProducer.send(
            ProducerRecord(
                TEST_SYKMELDING_TOPIC,
                null,
                "key",
                sykmeldingMedBehandlingsutfall.serialisertTilString(),
            )
        ).get()

        await().atMost(1, TimeUnit.SECONDS).untilAsserted {
            testSykmeldingListener.sisteSykmeldingMedBehandlingsutfall != null
        }
        testSykmeldingListener.sisteSykmeldingMedBehandlingsutfall.`should not be null`()
    }
}
