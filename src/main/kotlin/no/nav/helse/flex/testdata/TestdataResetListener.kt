package no.nav.helse.flex.testdata

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.sykmelding.SykmeldingRepository
import no.nav.helse.flex.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("test", "testdatareset")
class TestdataResetListener(
    val narmesteLederRepository: NarmesteLederRepository,
    val arbeidsforholdRepository: ArbeidsforholdRepository,
    val sykmeldingRepository: SykmeldingRepository,
) {
    val log = logger()

    @WithSpan
    @KafkaListener(
        topics = [TESTDATA_RESET_TOPIC],
        id = "flex-sykmeldinger-backend-testdatareset-v1",
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val fnr = cr.value()
        log.info("Mottok testdata reset.")
        slettTestdata(fnr)
        acknowledgment.acknowledge()
    }

    private fun slettTestdata(fnr: String) {
        narmesteLederRepository.deleteAll(narmesteLederRepository.findAllByBrukerFnrIn(listOf(fnr)))
        arbeidsforholdRepository.deleteAll(arbeidsforholdRepository.getAllByFnrIn(listOf(fnr)))
        sykmeldingRepository.findAllByPersonIdenter(PersonIdenter(originalIdent = fnr)).forEach {
            sykmeldingRepository.delete(it)
        }
    }
}

const val TESTDATA_RESET_TOPIC = "flex.testdata-reset"
