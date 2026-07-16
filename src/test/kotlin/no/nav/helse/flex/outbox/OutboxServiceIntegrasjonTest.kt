package no.nav.helse.flex.outbox

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.gateways.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.gateways.SYKMELDING_BRUKERNOTIFIKASJON_TOPIC
import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.ventPåRecords
import no.nav.helse.flex.testdata.lagOutboxDbRecord
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingHendelsePayload
import no.nav.helse.flex.testdata.lagSykmeldingNotifikasjon
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.tilPsqlJson
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class OutboxServiceIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var outboxService: OutboxService

    @Autowired
    lateinit var sykmeldingStatusConsumer: KafkaConsumer<String, String>

    @Autowired
    lateinit var transactionManager: PlatformTransactionManager

    private val transactionTemplate by lazy { TransactionTemplate(transactionManager) }

    @AfterEach
    fun cleanUp() {
        outboxDbRepository.deleteAll()
        slettDatabase()
    }

    private val timestamp = LocalDateTime.of(2024, 6, 1, 12, 0).toInstant(ZoneOffset.UTC)

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
            .ventPåRecords(antall = 1)
            .shouldHaveSingleItem()
            .run {
                key() shouldBeEqualTo "sykmelding-1"
                value().shouldNotBeNull()
            }
    }

    @Test
    fun `burde lagre outbox-rad med outboxSykmeldingBrukernotifikasjon og publisere den til kafka`() {
        sykmeldingStatusConsumer.subscribe(listOf(SYKMELDING_BRUKERNOTIFIKASJON_TOPIC))

        val fnr = "fnr-1"
        val notifikasjon = lagSykmeldingNotifikasjon(sykmeldingId = "sykmelding-1", fnr = fnr)

        outboxService.outboxSykmeldingBrukernotifikasjon(notifikasjon)

        outboxDbRepository.findAll().toList().also { usendte ->
            usendte shouldHaveSize 1
            usendte.first().type shouldBeEqualTo OutboxType.SYKMELDING_BRUKERNOTIFIKASJON
            usendte.first().fnr shouldBeEqualTo fnr
            usendte.first().sendtTimestamp.shouldBeNull()
        }

        outboxService.sendUsendteForEldsteLedigeFnr()

        outboxDbRepository.findAll().toList().also { sendte ->
            sendte shouldHaveSize 1
            sendte.first().sendtTimestamp.shouldNotBeNull()
        }

        sykmeldingStatusConsumer
            .ventPåRecords(antall = 1)
            .shouldHaveSingleItem()
            .run {
                key() shouldBeEqualTo "sykmelding-1"
                objectMapper.readValue<SykmeldingNotifikasjon>(value()) shouldBeEqualTo notifikasjon
            }
    }

    @Test
    fun `sendUsendteForEldsteLedigeFnr returnerer true når noe ble prosessert`() {
        sykmeldingStatusConsumer.subscribe(listOf(SYKMELDING_BRUKERNOTIFIKASJON_TOPIC))

        outboxService.outboxSykmeldingBrukernotifikasjon(
            lagSykmeldingNotifikasjon(sykmeldingId = "sykmelding-1", fnr = "fnr-1"),
        )

        val bleProsessert = outboxService.sendUsendteForEldsteLedigeFnr()

        bleProsessert shouldBeEqualTo true
        sykmeldingStatusConsumer.ventPåRecords(antall = 1)
    }

    @Test
    fun `sendUsendteForEldsteLedigeFnr returnerer false når det ikke er noe å sende`() {
        val bleProsessert = outboxService.sendUsendteForEldsteLedigeFnr()

        bleProsessert shouldBeEqualTo false
    }

    @Test
    fun `burde velge eldste ledige fnr og returnere radene i opprettet-rekkefolge`() {
        lagreUsendtOutboxRad(fnr = "fnr-nyest", opprettetTimestamp = timestamp.minusSeconds(10))
        val eldste = lagreUsendtOutboxRad(fnr = "fnr-eldst", opprettetTimestamp = timestamp.minusSeconds(60))
        val nestEldste = lagreUsendtOutboxRad(fnr = "fnr-eldst", opprettetTimestamp = timestamp.minusSeconds(30))

        val rader = outboxDbRepository.finnOgLasUsendteForEldsteLedigeFnr()

        rader.map { it.fnr }.distinct() shouldBeEqualTo listOf("fnr-eldst")
        rader.map { it.id } shouldBeEqualTo listOf(eldste.id, nestEldste.id)
    }

    @Test
    fun `advisory lock hindrer at samme fnr plukkes av to samtidige transaksjoner`() {
        lagreUsendtOutboxRad(fnr = "fnr-eldst", opprettetTimestamp = timestamp.minusSeconds(60))
        lagreUsendtOutboxRad(fnr = "fnr-nyest", opprettetTimestamp = timestamp.minusSeconds(30))

        val transaksjon1HarLast = CountDownLatch(1)
        val slippTransaksjon1 = CountDownLatch(1)
        val fnrLastAvTransaksjon1 = mutableListOf<String>()

        val traad =
            thread {
                transactionTemplate.execute {
                    val rader = outboxDbRepository.finnOgLasUsendteForEldsteLedigeFnr()
                    fnrLastAvTransaksjon1.addAll(rader.map { it.fnr })
                    transaksjon1HarLast.countDown()
                    // Holder transaksjonen (og dermed advisory-låsen) åpen til vi slipper den
                    slippTransaksjon1.await(3, TimeUnit.SECONDS)
                }
            }

        transaksjon1HarLast.await(3, TimeUnit.SECONDS) shouldBeEqualTo true

        val fnrLastAvTransaksjon2 =
            transactionTemplate.execute {
                outboxDbRepository.finnOgLasUsendteForEldsteLedigeFnr().map { it.fnr }
            }!!

        slippTransaksjon1.countDown()
        traad.join()

        fnrLastAvTransaksjon1.distinct() shouldBeEqualTo listOf("fnr-eldst")
        fnrLastAvTransaksjon2.distinct() shouldBeEqualTo listOf("fnr-nyest")
    }

    private fun lagreUsendtOutboxRad(
        fnr: String,
        opprettetTimestamp: Instant,
    ): OutboxDbRecord =
        outboxDbRepository.save(
            lagOutboxDbRecord(
                fnr = fnr,
                payload =
                    lagSykmeldingHendelsePayload(
                        sykmeldingId = "sykmelding-$fnr",
                        sykmeldingHendelseId = "hendelse-$fnr",
                    ).tilPsqlJson(),
                opprettetTimestamp = opprettetTimestamp,
            ),
        )
}
