package no.nav.helse.flex.tsmsykmeldingstatus

import com.nhaarman.mockitokotlin2.times
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.tsmsykmeldingstatus.dto.ArbeidsgiverStatusKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.BrukerSvarKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SporsmalKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.TidligereArbeidsgiverKafkaDTO
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import java.time.LocalDateTime

class ImporterteHistoriskeStatuserProsessorTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var importerteHistoriskeStatuserProsessor: ImporterteHistoriskeStatuserProsessor

    @Test
    fun `burde prosessere en status`() {
        insertStatus(
            sykmeldingId = "1",
        )
        val resultat = importerteHistoriskeStatuserProsessor.prosesserNesteSykmeldingStatuser()
        resultat shouldBeEqualTo ImporterteHistoriskeStatuserProsessor.Resultat.OK
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
