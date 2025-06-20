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
import java.sql.Timestamp
import java.time.Instant

class HistoriskeStatuserProsessorTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var historiskeStatuserProsessor: HistoriskeStatuserProsessor

    @Autowired
    lateinit var historiskeStatuserDao: HistoriskeStatuserDao

    @AfterEach
    fun afterEach() {
        super.slettDatabase()
        slettStatusInnlesingFraDatabase()
    }

    @Test
    fun `burde prosessere en status`() {
        insertStatus(sykmeldingId = "1")
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 1

        historiskeStatuserDao.lesCheckpointStatusTimestamp().shouldNotBeNull()
    }

    @Test
    fun `burde prosessere flere statuser`() {
        insertStatus(sykmeldingId = "1", event = "APEN")
        insertStatus(sykmeldingId = "1", event = "SENDT")
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 2
    }

    @Test
    fun `burde prosessere maks antall`() {
        insertStatus(sykmeldingId = "1", event = "APEN")
        insertStatus(sykmeldingId = "1", event = "SENDT")
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch(antall = 1)
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 1
    }

    @Test
    fun `burde oppdatere checkpoint timestamp`() {
        insertStatus(sykmeldingId = "1", timestamp = Instant.parse("2020-01-01T00:00:00Z"))
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch(antall = 1)
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 1

        historiskeStatuserDao.lesCheckpointStatusTimestamp().shouldNotBeNull() shouldBeEqualTo Instant.parse("2020-01-01T00:00:00Z")
    }

    @Test
    fun `burde starte på checkpoint timestamp`() {
        insertStatus(sykmeldingId = "1", timestamp = Instant.parse("2018-01-01T00:00:00Z"))
        insertStatus(sykmeldingId = "2", timestamp = Instant.parse("2019-01-01T00:00:00Z"))
        historiskeStatuserProsessor.prosesserNesteBatch(antall = 1)
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch(antall = 2)
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallProsessert shouldBeEqualTo 1
    }

    @Test
    fun `burde returnere status FERDIG dersom alle prosessert`() {
        insertStatus(sykmeldingId = "1")
        historiskeStatuserProsessor.prosesserNesteBatch(antall = 1)
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch(antall = 1)
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.FERDIG
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
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch()
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
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch()
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
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch()
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
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch()
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
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.OK
        resultat.antallLagtTil shouldBeEqualTo 0
    }

    @Test
    fun `burde ikke prosessere statuser som er nyere enn 2020-05-01`() {
        insertStatus(
            sykmeldingId = "1",
            timestamp = Instant.parse("2020-05-02T00:00:00Z"),
        )
        val resultat = historiskeStatuserProsessor.prosesserNesteBatch()
        resultat.status shouldBeEqualTo HistoriskeStatuserProsessor.ResultatStatus.FERDIG
        resultat.antallProsessert shouldBeEqualTo 0
    }

    private fun slettStatusInnlesingFraDatabase() {
        jdbcTemplate.update(
            """
            TRUNCATE temp_tsm_historisk_sykmeldingstatus;
            TRUNCATE temp_tsm_historisk_sykmeldingstatus_checkpoint;
            """.trimIndent(),
            emptyMap<String, Any?>(),
        )
    }

    private fun insertStatus(
        sykmeldingId: String,
        timestamp: Instant = Instant.parse("2020-01-01T00:00:00Z"),
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
                "timestamp" to Timestamp.from(timestamp),
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
