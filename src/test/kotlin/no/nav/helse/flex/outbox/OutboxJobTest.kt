package no.nav.helse.flex.outbox

import no.nav.helse.flex.gateways.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.gateways.SYKMELDING_BRUKERNOTIFIKASJON_TOPIC
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.ventPåRecords
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingNotifikasjon
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OutboxJobTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var outboxService: OutboxService

    @Autowired
    lateinit var outboxJob: OutboxJob

    @Autowired
    lateinit var sykmeldingStatusConsumer: KafkaConsumer<String, String>

    @AfterEach
    fun cleanUp() {
        outboxDbRepository.deleteAll()
        slettDatabase()
    }

    @Test
    fun `burde prosessere alle usendte outbox-rader av ulike typer for flere fnr til det ikke er noe igjen`() {
        sykmeldingStatusConsumer.subscribe(listOf(SYKMELDINGSTATUS_TOPIC, SYKMELDING_BRUKERNOTIFIKASJON_TOPIC))

        val fnr1 = "fnr-1"
        val fnr2 = "fnr-2"

        lagreSykmeldingHendelseOutbox(fnr = fnr1, sykmeldingId = "sykmelding-1")
        lagreSykmeldingHendelseOutbox(fnr = fnr2, sykmeldingId = "sykmelding-2")
        lagreBrukernotifikasjonOutbox(fnr = fnr1, sykmeldingId = "sykmelding-1")
        lagreBrukernotifikasjonOutbox(fnr = fnr2, sykmeldingId = "sykmelding-2")

        outboxDbRepository.findAll().toList().also { usendte ->
            usendte shouldHaveSize 4
            usendte.all { it.sendtTimestamp == null } shouldBeEqualTo true
        }

        outboxJob.kjørJobb()

        outboxDbRepository.findAll().toList().also { sendte ->
            sendte shouldHaveSize 4
            sendte.all { it.sendtTimestamp != null } shouldBeEqualTo true
        }

        sykmeldingStatusConsumer.ventPåRecords(antall = 4) shouldHaveSize 4
    }

    private fun lagreSykmeldingHendelseOutbox(
        fnr: String,
        sykmeldingId: String,
    ) {
        val lagretSykmelding =
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = sykmeldingId, pasient = lagPasient(fnr = fnr)),
                ),
            )
        val hendelseId = lagretSykmelding.sisteHendelse().databaseId.shouldNotBeNull()

        outboxService.outboxSykmeldingHendelse(
            fnr = fnr,
            sykmeldingId = sykmeldingId,
            sykmeldingHendelseId = hendelseId,
        )
    }

    private fun lagreBrukernotifikasjonOutbox(
        fnr: String,
        sykmeldingId: String,
    ) {
        outboxService.outboxSykmeldingBrukernotifikasjon(
            lagSykmeldingNotifikasjon(sykmeldingId = sykmeldingId, fnr = fnr),
        )
    }
}
