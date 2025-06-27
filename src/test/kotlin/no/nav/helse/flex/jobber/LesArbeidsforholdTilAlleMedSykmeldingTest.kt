package no.nav.helse.flex.jobber

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class LesArbeidsforholdTilAlleMedSykmeldingTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var historiskArbeidsforholdCheckpointRepository: HistoriskArbeidsforholdCheckpointRepository

    @Autowired
    lateinit var lesArbeidsforholdTilAlleMedSykmelding: LesArbeidsforholdTilAlleMedSykmelding

    @BeforeAll
    fun cleanup() {
        historiskArbeidsforholdCheckpointRepository.deleteAll()
    }

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun `burde hente arbeidsforhold for alle fnr`() {
        val historiskArbeidsforholdCheckpointRepositoryCustomInsert = HistoriskArbeidsforholdCheckpointRepositoryCustomInsert(jdbcTemplate)

        historiskArbeidsforholdCheckpointRepositoryCustomInsert.insertNew(HistoriskArbeidsforholdCheckpoint("123", false))
        historiskArbeidsforholdCheckpointRepositoryCustomInsert.insertNew(HistoriskArbeidsforholdCheckpoint("456", false))

        val processed = lesArbeidsforholdTilAlleMedSykmelding.lesArbeidsforholdPerFnr()
        assertEquals(2, processed)

        val all = historiskArbeidsforholdCheckpointRepository.findAll().toList()
        assertEquals(2, all.size)
        all.forEach { assertEquals(true, it.hentetArbeidsforhold) }
    }

    @Test
    fun `burde returnere 0 når alle fnr er behandlet`() {
        val processed = lesArbeidsforholdTilAlleMedSykmelding.lesArbeidsforholdPerFnr()
        assertEquals(0, processed)
    }
}

class HistoriskArbeidsforholdCheckpointRepositoryCustomInsert(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun insertNew(checkpoint: HistoriskArbeidsforholdCheckpoint) {
        val sql = "INSERT INTO temp_historisk_arbeidsforhold_checkpoint (fnr, hentet_arbeidsforhold) VALUES (:fnr, :hentetArbeidsforhold)"
        jdbcTemplate.update(sql, mapOf("fnr" to checkpoint.fnr, "hentetArbeidsforhold" to checkpoint.hentetArbeidsforhold))
    }
}
