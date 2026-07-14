package no.nav.helse.flex.outbox

import no.nav.helse.flex.gateways.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.ventPåRecords
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSingleItem
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class OutboxServiceIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var outboxService: OutboxService

    @Autowired
    lateinit var outboxDbRepository: OutboxDbRepository

    @Autowired
    lateinit var sykmeldingStatusConsumer: KafkaConsumer<String, String>

    @AfterEach
    fun cleanUp() {
        outboxDbRepository.deleteAll()
        slettDatabase()
    }

    @Test
    fun `burde lagre outbox-rad med outboxSykmeldingHendelse og markere den som sendt`() {
        sykmeldingStatusConsumer.subscribe(listOf(SYKMELDINGSTATUS_TOPIC))

        val fnr = "fnr-1"
        val lagretSykmelding =
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "sykmelding-1", pasient = lagPasient(fnr = fnr)),
                ),
            )
        val hendelseId = lagretSykmelding.sisteHendelse().databaseId.shouldNotBeNull()

        outboxService.outboxSykmeldingHendelse(
            fnr = fnr,
            sykmeldingId = "sykmelding-1",
            sykmeldingHendelseId = hendelseId,
        )

        outboxDbRepository.findAll().toList().also { usendte ->
            usendte shouldHaveSize 1
            usendte.first().type shouldBeEqualTo OutboxType.SYKMELDING_HENDELSE
            usendte.first().sendtTimestamp.shouldBeNull()
        }

        outboxService.sendUsendteForEldsteLedigeFnr()

        outboxDbRepository.findAll().toList().also { sendte ->
            sendte shouldHaveSize 1
            sendte.first().sendtTimestamp.shouldNotBeNull()
        }

        sykmeldingStatusConsumer
            .ventPåRecords(antall = 1, duration = Duration.ofSeconds(10))
            .shouldHaveSingleItem()
            .run {
                key() shouldBeEqualTo "sykmelding-1"
                value().shouldNotBeNull()
            }
    }
}
