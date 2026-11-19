package no.nav.helse.flex.outbox

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.helse.flex.gateways.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.gateways.SYKMELDING_BRUKERNOTIFIKASJON_TOPIC
import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.hentProduserteRecords
import no.nav.helse.flex.testconfig.ventPåRecords
import no.nav.helse.flex.testdata.lagOutboxDbRecord
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingHendelsePayload
import no.nav.helse.flex.testdata.lagSykmeldingNotifikasjon
import no.nav.helse.flex.utils.fraPsqlJson
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.tilPsqlJson
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.module.kotlin.readValue
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentLinkedQueue
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
            usendte.first().type shouldBe OutboxType.SYKMELDING_HENDELSE
            usendte.first().sendtTimestamp.shouldBeNull()
        }

        outboxService.sendEldsteUsendte()

        outboxDbRepository.findAll().toList().also { sendte ->
            sendte shouldHaveSize 1
            sendte.first().sendtTimestamp.shouldNotBeNull()
        }

        sykmeldingStatusConsumer
            .ventPåRecords(antall = 1)
            .shouldBeSingle()
            .run {
                key() shouldBe "sykmelding-1"
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
            usendte.first().type shouldBe OutboxType.SYKMELDING_BRUKERNOTIFIKASJON
            usendte.first().fnr shouldBe fnr
            usendte.first().sendtTimestamp.shouldBeNull()
        }

        outboxService.sendEldsteUsendte()

        outboxDbRepository.findAll().toList().also { sendte ->
            sendte shouldHaveSize 1
            sendte.first().sendtTimestamp.shouldNotBeNull()
        }

        sykmeldingStatusConsumer
            .ventPåRecords(antall = 1)
            .shouldBeSingle()
            .run {
                key() shouldBe "sykmelding-1"
                objectMapper.readValue<SykmeldingNotifikasjon>(value()) shouldBe notifikasjon
            }
    }

    @Test
    fun `sendEldsteUsendte returnerer true når noe ble prosessert`() {
        sykmeldingStatusConsumer.subscribe(listOf(SYKMELDING_BRUKERNOTIFIKASJON_TOPIC))

        outboxService.outboxSykmeldingBrukernotifikasjon(
            lagSykmeldingNotifikasjon(sykmeldingId = "sykmelding-1", fnr = "fnr-1"),
        )

        val bleProsessert = outboxService.sendEldsteUsendte()

        bleProsessert shouldBe true
        sykmeldingStatusConsumer.ventPåRecords(antall = 1)
    }

    @Test
    fun `sendEldsteUsendte returnerer false når det ikke er noe å sende`() {
        val bleProsessert = outboxService.sendEldsteUsendte()

        bleProsessert shouldBe false
    }

    @Test
    fun `burde utsette rad med backoff når sending feiler`() {
        // Sykmeldingen finnes ikke, så sendingen feiler.
        val rad = lagreUsendtOutboxRad(fnr = "fnr-x", opprettetTimestamp = timestamp)
        val førForsøk = Instant.now()

        outboxService.sendEldsteUsendte() shouldBe true

        outboxDbRepository.findById(rad.id!!).get().also {
            it.sendtTimestamp.shouldBeNull()
            it.antallForsok shouldBe 1
            it.sisteFeil shouldStartWith "${IllegalStateException::class.java.name}: Fant ikke sykmelding sykmelding-fnr-x"
            it.nesteForsokTimestamp.shouldNotBeNull().isAfter(førForsøk) shouldBe true
        }
        outboxService.sendEldsteUsendte() shouldBe false
    }

    @Test
    fun `burde øke antall forsøk når en rad feiler på nytt`() {
        val nå = Instant.now()
        val rad =
            lagreUsendtOutboxRad(
                fnr = "fnr-x",
                opprettetTimestamp = timestamp,
                nesteForsokTimestamp = nå.minusSeconds(1),
                antallForsok = 3,
            )

        outboxService.sendEldsteUsendte() shouldBe true

        outboxDbRepository.findById(rad.id!!).get().also {
            it.antallForsok shouldBe 4
            it.nesteForsokTimestamp.shouldNotBeNull().isAfter(nå.plusSeconds(7)) shouldBe true
        }
    }

    @Test
    fun `rad som venter på nytt forsøk burde bare blokkere sitt eget fnr`() {
        lagreUsendtOutboxRad(
            fnr = "fnr-x",
            opprettetTimestamp = timestamp.minusSeconds(60),
            nesteForsokTimestamp = Instant.now().plusSeconds(3600),
        )
        lagreUsendtOutboxRad(fnr = "fnr-x", opprettetTimestamp = timestamp.minusSeconds(50))
        val y1 = lagreUsendtOutboxRad(fnr = "fnr-y", opprettetTimestamp = timestamp.minusSeconds(30))

        transactionTemplate.execute { outboxDbRepository.finnEldsteLedigeRadOgLas() }?.id shouldBe y1.id

        outboxDbRepository.deleteById(y1.id!!)
        transactionTemplate.execute { outboxDbRepository.finnEldsteLedigeRadOgLas() }.shouldBeNull()
    }

    @Test
    fun `burde plukke rad igjen når ventetiden er over`() {
        val x1 =
            lagreUsendtOutboxRad(
                fnr = "fnr-x",
                opprettetTimestamp = timestamp,
                nesteForsokTimestamp = Instant.now().minusSeconds(1),
            )

        transactionTemplate.execute { outboxDbRepository.finnEldsteLedigeRadOgLas() }?.id shouldBe x1.id
    }

    @Test
    fun `burde låse eldste usendte rad`() {
        lagreUsendtOutboxRad(fnr = "fnr-nyest", opprettetTimestamp = timestamp.minusSeconds(10))
        val eldste = lagreUsendtOutboxRad(fnr = "fnr-eldst", opprettetTimestamp = timestamp.minusSeconds(60))
        lagreUsendtOutboxRad(fnr = "fnr-eldst", opprettetTimestamp = timestamp.minusSeconds(30))

        val rad = transactionTemplate.execute { outboxDbRepository.finnEldsteLedigeRadOgLas() }

        rad?.id shouldBe eldste.id
    }

    @Test
    fun `burde sende én rad per kall, eldste først på tvers av fnr`() {
        sykmeldingStatusConsumer.subscribe(listOf(SYKMELDING_BRUKERNOTIFIKASJON_TOPIC))
        listOf("a1" to "fnr-a", "b1" to "fnr-b", "a2" to "fnr-a").forEach { (sykmeldingId, fnr) ->
            outboxService.outboxSykmeldingBrukernotifikasjon(lagSykmeldingNotifikasjon(sykmeldingId = sykmeldingId, fnr = fnr))
        }

        outboxService.sendEldsteUsendte() shouldBe true
        sendteSykmeldingIder() shouldBe setOf("a1")

        outboxService.sendEldsteUsendte() shouldBe true
        sendteSykmeldingIder() shouldBe setOf("a1", "b1")

        outboxService.sendEldsteUsendte() shouldBe true
        sendteSykmeldingIder() shouldBe setOf("a1", "b1", "a2")

        outboxService.sendEldsteUsendte() shouldBe false
        sykmeldingStatusConsumer.ventPåRecords(antall = 3)
    }

    @Test
    fun `låst rad burde hindre at nyere rader for samme fnr plukkes av en annen transaksjon`() {
        val x1 = lagreUsendtOutboxRad(fnr = "fnr-x", opprettetTimestamp = timestamp.minusSeconds(60))
        lagreUsendtOutboxRad(fnr = "fnr-x", opprettetTimestamp = timestamp.minusSeconds(50))
        val y1 = lagreUsendtOutboxRad(fnr = "fnr-y", opprettetTimestamp = timestamp.minusSeconds(30))

        var låstAvTransaksjon1: OutboxDbRecord? = null
        val låstAvTransaksjon2 =
            medÅpenTransaksjonIAnnenTråd(
                iTråd = { låstAvTransaksjon1 = outboxDbRepository.finnEldsteLedigeRadOgLas() },
                iMellomtiden = { transactionTemplate.execute { outboxDbRepository.finnEldsteLedigeRadOgLas() } },
            )

        låstAvTransaksjon1?.id shouldBe x1.id
        låstAvTransaksjon2?.id shouldBe y1.id
    }

    @Test
    fun `burde ikke returnere noen rad når eldste rad for eneste fnr er låst`() {
        val x1 = lagreUsendtOutboxRad(fnr = "fnr-x", opprettetTimestamp = timestamp.minusSeconds(60))
        lagreUsendtOutboxRad(fnr = "fnr-x", opprettetTimestamp = timestamp.minusSeconds(50))

        var låstAvTransaksjon1: OutboxDbRecord? = null
        val låstAvTransaksjon2 =
            medÅpenTransaksjonIAnnenTråd(
                iTråd = { låstAvTransaksjon1 = outboxDbRepository.finnEldsteLedigeRadOgLas() },
                iMellomtiden = { transactionTemplate.execute { outboxDbRepository.finnEldsteLedigeRadOgLas() } },
            )

        låstAvTransaksjon1?.id shouldBe x1.id
        låstAvTransaksjon2.shouldBeNull()
    }

    @Test
    fun `samme rad sendes ikke flere ganger når flere instanser sender samtidig`() {
        sykmeldingStatusConsumer.subscribe(listOf(SYKMELDING_BRUKERNOTIFIKASJON_TOPIC))
        val antallRader = 300
        val antallInstanser = 4
        repeat(antallRader) { i ->
            outboxService.outboxSykmeldingBrukernotifikasjon(
                lagSykmeldingNotifikasjon(sykmeldingId = "duplikat-test-$i", fnr = "fnr-$i"),
            )
        }

        val start = CountDownLatch(1)
        val feil = ConcurrentLinkedQueue<Throwable>()
        val traader =
            (1..antallInstanser).map {
                thread {
                    try {
                        start.await()
                        while (outboxService.sendEldsteUsendte()) {
                            // Fortsetter til det ikke er flere ledige fnr
                        }
                    } catch (e: Throwable) {
                        feil.add(e)
                    }
                }
            }
        start.countDown()
        traader.forEach { it.join(TimeUnit.SECONDS.toMillis(60)) }

        feil.shouldBeEmpty()
        outboxDbRepository.findAll().filter { it.sendtTimestamp == null }.shouldBeEmpty()

        val sendteSykmeldingIder = mutableListOf<String>()
        await().atMost(Duration.ofSeconds(30)).during(Duration.ofSeconds(2)).until {
            sendteSykmeldingIder.addAll(
                sykmeldingStatusConsumer
                    .hentProduserteRecords()
                    .map { it.key() }
                    .filter { it.startsWith("duplikat-test-") },
            )
            sendteSykmeldingIder.size >= antallRader
        }

        sendteSykmeldingIder.groupingBy { it }.eachCount().filterValues { it > 1 } shouldBe emptyMap()
        sendteSykmeldingIder shouldHaveSize antallRader
    }

    private fun <T> medÅpenTransaksjonIAnnenTråd(
        iTråd: () -> Unit,
        iMellomtiden: () -> T,
    ): T {
        val harLås = CountDownLatch(1)
        val slipp = CountDownLatch(1)
        val tråd =
            thread {
                transactionTemplate.executeWithoutResult {
                    iTråd()
                    harLås.countDown()
                    // Holder transaksjonen, og dermed radlåsen, åpen til testen slipper den.
                    slipp.await(5, TimeUnit.SECONDS)
                }
            }

        harLås.await(5, TimeUnit.SECONDS) shouldBe true
        try {
            return iMellomtiden()
        } finally {
            slipp.countDown()
            tråd.join()
        }
    }

    private fun sendteSykmeldingIder(): Set<String> =
        outboxDbRepository
            .findAll()
            .filter { it.sendtTimestamp != null }
            .map { it.payload.fraPsqlJson<SykmeldingNotifikasjon>()!!.sykmeldingId }
            .toSet()

    private fun lagreUsendtOutboxRad(
        fnr: String,
        opprettetTimestamp: Instant,
        nesteForsokTimestamp: Instant? = null,
        antallForsok: Int = 0,
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
            ).copy(nesteForsokTimestamp = nesteForsokTimestamp, antallForsok = antallForsok),
        )
}
