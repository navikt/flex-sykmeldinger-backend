package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.ArbeidsgiverStatusKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.BrukerSvarKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SporsmalKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.TidligereArbeidsgiverKafkaDTO
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch

class HistoriskeStatuserProsessorTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var historiskeStatuserProsessor: HistoriskeStatuserProsessor

    @Autowired
    lateinit var historiskeStatuserDao: HistoriskeStatuserDao

    @Autowired
    lateinit var txTemplate: TransactionTemplate

    @AfterEach
    fun afterEach() {
        super.slettDatabase()
        slettStatusInnlesingFraDatabase()
    }

    @Test
    fun `burde prosessere en status`() {
        insertStatus(sykmeldingId = "1")
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 1

        historiskeStatuserDao.lesNesteSykmeldingIderForBehandling() shouldHaveSize 0
        historiskeStatuserDao.lesAlleBehandledeSykmeldingIder() shouldHaveSize 1
    }

    @Test
    fun `burde prosessere alle statuser for samme sykmelding`() {
        insertStatus(sykmeldingId = "1", event = "APEN")
        insertStatus(sykmeldingId = "1", event = "SENDT")
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 2

        historiskeStatuserDao.lesNesteSykmeldingIderForBehandling() shouldHaveSize 0
        historiskeStatuserDao.lesAlleBehandledeSykmeldingIder() shouldHaveSize 1
    }

    @Test
    fun `burde prosessere status fra forskjellige sykmeldinger etter hverandre`() {
        insertStatus(sykmeldingId = "1")
        insertStatus(sykmeldingId = "2")
        historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser().run {
            status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        }
        historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser().run {
            status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        }
        historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser().run {
            status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.FERDIG
        }
        historiskeStatuserDao.lesNesteSykmeldingIderForBehandling() shouldHaveSize 0
        historiskeStatuserDao.lesAlleBehandledeSykmeldingIder() shouldHaveSize 2
    }

    @Test
    fun `burde returnere status FERDIG dersom alle prosessert`() {
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.FERDIG
    }

    @Test
    fun `burde prosessere neste statuser dersom første blir prosessert av parallelle prosesser`() {
        insertStatus(sykmeldingId = "1")
        insertStatus(sykmeldingId = "2")

        val otherProcessProcessedLatch = CountDownLatch(1)
        val otherProcessCompleteLatch = CountDownLatch(1)

        val otherProcess =
            Thread {
                txTemplate.execute {
                    historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
                    otherProcessProcessedLatch.countDown()
                    otherProcessCompleteLatch.await()
                }
            }
        otherProcess.start()
        otherProcessProcessedLatch.await()

        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK

        otherProcessCompleteLatch.countDown()
        otherProcess.join()

        val resultatEtter = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultatEtter.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.FERDIG
    }

    @Test
    fun `burde returnere status PROV_IGJEN dersom alle statuser er låst av parallelle prosesser`() {
        insertStatus(sykmeldingId = "1")

        val lockThreadProcessedLatch = CountDownLatch(1)
        val lockThreadCompleteLatch = CountDownLatch(1)

        val lockThread =
            Thread {
                txTemplate.execute {
                    historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
                    lockThreadProcessedLatch.countDown()
                    lockThreadCompleteLatch.await()
                }
            }
        lockThread.start()
        lockThreadProcessedLatch.await()

        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.PROV_IGJEN

        lockThreadCompleteLatch.countDown()
        lockThread.join()
    }

    @Test
    fun `burde legge til status SENDT`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
            ),
        )
        insertStatus(
            sykmeldingId = "1",
            event = "SENDT",
            alleSporsmal = lagBrukerSvarKafkaDto(arbeidssituasjonKafkaDTO = ArbeidssituasjonDTO.ARBEIDSTAKER),
            arbeidsgiver = lagArbeidsgiverStatusKafkaDTO(),
        )
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallLagtTil shouldBeEqualTo 1

        sykmeldingRepository
            .findBySykmeldingId("1")
            .shouldNotBeNull()
            .hendelser
            .shouldHaveSize(2)
    }

    @Test
    fun `burde legge til status BEKREFTET`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
            ),
        )
        insertStatus(
            sykmeldingId = "1",
            event = "BEKREFTET",
            alleSporsmal = lagBrukerSvarKafkaDto(arbeidssituasjonKafkaDTO = ArbeidssituasjonDTO.ANNET),
        )
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallLagtTil shouldBeEqualTo 1

        sykmeldingRepository
            .findBySykmeldingId("1")
            .shouldNotBeNull()
            .hendelser
            .shouldHaveSize(2)
    }

    @Test
    fun `burde legge til status AVBRUTT`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
            ),
        )
        insertStatus(
            sykmeldingId = "1",
            event = "AVBRUTT",
        )
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallLagtTil shouldBeEqualTo 1

        sykmeldingRepository
            .findBySykmeldingId("1")
            .shouldNotBeNull()
            .hendelser
            .shouldHaveSize(2)
    }

    @Test
    fun `burde ikke legge til dupliserte statuser`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
            ),
        )
        insertStatus(
            sykmeldingId = "1",
            event = "BEKREFTET",
            alleSporsmal = lagBrukerSvarKafkaDto(arbeidssituasjonKafkaDTO = ArbeidssituasjonDTO.ANNET),
        )
        insertStatus(
            sykmeldingId = "1",
            event = "BEKREFTET",
            alleSporsmal = lagBrukerSvarKafkaDto(arbeidssituasjonKafkaDTO = ArbeidssituasjonDTO.ANNET),
        )
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 2
        resultat.antallLagtTil shouldBeEqualTo 1
    }

    @Test
    fun `burde ikke legge til status dersom sykmelding ikke finnes`() {
        insertStatus(
            sykmeldingId = "1",
            event = "BEKREFTET",
            alleSporsmal = lagBrukerSvarKafkaDto(arbeidssituasjonKafkaDTO = ArbeidssituasjonDTO.ANNET),
        )
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallLagtTil shouldBeEqualTo 0
    }

    @Test
    fun `burde ikke prosessere statuser som er nyere enn 2020-05-01`() {
        insertStatus(
            sykmeldingId = "1",
            timestamp = LocalDateTime.parse("2020-05-02T00:00:00"),
        )
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.FERDIG
        resultat.antallProsessert shouldBeEqualTo 0
    }

    @Test
    fun `burde prosessere alle statuser for sykmelding dersom første er eldre enn 2020-05-01`() {
        insertStatus(
            sykmeldingId = "1",
            timestamp = LocalDateTime.parse("2020-05-01T00:00:00"),
        )
        insertStatus(
            sykmeldingId = "1",
            timestamp = LocalDateTime.parse("2020-05-02T00:00:00"),
        )
        val resultat = historiskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 2
    }

    private fun slettStatusInnlesingFraDatabase() {
        jdbcTemplate.update(
            """
            TRUNCATE temp_tsm_historisk_sykmeldingstatus;
            TRUNCATE temp_tsm_historisk_sykmeldingstatus_innlesing;
            """.trimIndent(),
            emptyMap<String, Any?>(),
        )
    }

    private fun insertStatus(
        sykmeldingId: String,
        timestamp: LocalDateTime = LocalDateTime.parse("2020-01-01T00:00:00"),
        event: String = "APEN",
        arbeidsgiver: ArbeidsgiverStatusKafkaDTO? = null,
        sporsmal: List<SporsmalKafkaDTO>? = null,
        tidligereArbeidsgiver: TidligereArbeidsgiverKafkaDTO? = null,
        alleSporsmal: BrukerSvarKafkaDTO? = null,
    ) {
        val inserter: SimpleJdbcInsert =
            SimpleJdbcInsert(jdbcTemplate.jdbcTemplate)
                .withTableName("TEMP_TSM_HISTORISK_SYKMELDINGSTATUS")
                .usingGeneratedKeyColumns("lest_inn")

        inserter.execute(
            mapOf(
                "sykmelding_id" to sykmeldingId,
                "event" to event,
                "timestamp" to timestamp,
                "arbeidsgiver" to toPgJsonb(arbeidsgiver),
                "sporsmal" to toPgJsonb(sporsmal),
                "alle_sprosmal" to toPgJsonb(alleSporsmal),
                "tidligere_arbeidsgiver" to toPgJsonb(tidligereArbeidsgiver),
            ).toMutableMap(),
        )
    }
}

private fun toPgJsonb(value: Any?): PGobject? {
    if (value == null) {
        return null
    }
    val pgObject = PGobject()
    pgObject.type = "jsonb"
    pgObject.value = value.serialisertTilString()
    return pgObject
}
