package no.nav.helse.flex.listeners

import no.nav.helse.flex.sykmelding.SykmeldingKafkaRecord
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagValidation
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.invoking
import org.amshove.kluent.`should not throw`
import org.amshove.kluent.`should throw`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingListenerFakeTest : FakesTestOppsett() {
    @Autowired
    lateinit var environmentToggles: EnvironmentTogglesFake

    @Autowired
    lateinit var aaregClientFake: AaregClientFake

    @Autowired
    private lateinit var sykmeldingListener: SykmeldingListener

    @AfterEach
    fun reset() {
        slettDatabase()
        environmentToggles.reset()
        aaregClientFake.reset()
    }

    @Test
    fun `burde ignorerere feil i dev`() {
        environmentToggles.setEnvironment("dev")
        aaregClientFake.setArbeidsforholdoversikt(RuntimeException())

        val kafkaMelding =
            SykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        invoking {
            sykmeldingListener.listen(
                cr =
                    ConsumerRecord(
                        SYKMELDING_TOPIC,
                        0,
                        0,
                        "1",
                        kafkaMelding.serialisertTilString(),
                    ),
                acknowledgment = { },
            )
        }.`should not throw`(Exception::class)
    }

    @Test
    fun `burde ikke ignorerere feil i prod`() {
        environmentToggles.setEnvironment("prod")
        aaregClientFake.setArbeidsforholdoversikt(RuntimeException())

        val kafkaMelding =
            SykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        invoking {
            sykmeldingListener.listen(
                cr =
                    ConsumerRecord(
                        SYKMELDING_TOPIC,
                        0,
                        0,
                        "1",
                        kafkaMelding.serialisertTilString(),
                    ),
                acknowledgment = { },
            )
        }.`should throw`(Exception::class)
    }
}
