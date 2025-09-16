package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.Tilleggsinfo
import no.nav.helse.flex.sykmelding.domain.TilleggsinfoType
import no.nav.helse.flex.sykmelding.domain.UtdatertFormatTilleggsinfo
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingHendelse
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant

class HistoriskeManglendeStatuserProsessorTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var historiskeManglendeStatuserProsessor: HistoriskeManglendeStatuserProsessor

    @Test
    fun `burde prosessere én`() {
        val forsteHendelseOpprettet = Instant.parse("2017-11-05T00:00:00Z")
        val andreHendelseOpprettet = Instant.parse("2017-11-06T00:00:00Z")

        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(hendelseOpprettet = forsteHendelseOpprettet),
                    ),
            ),
        )

        jdbcTemplate.update(
            """
            INSERT INTO temp_resterende_sykmeldingstatuser_fra_tsm (
                id,
                sykmelding_id,
                event,
                timestamp,
                bruker_svar,
                sporsmal_liste,
                arbeidsgiver,
                tidligere_arbeidsgiver,
                lokalt_opprettet,
                source
            ) VALUES (
                '9dda6a42-26fd-4861-8afd-e6349ab173a8',
                '1',
                'SENDT',
                '$andreHendelseOpprettet',
                null,
                '[{"svartype":"ARBEIDSSITUASJON","svar":"ARBEIDSTAKER","shortname":"ARBEIDSSITUASJON","tekst":"Jeg er sykmeldt fra"}]',
                '{"orgnummer":"00000000","navn":"NAVN","juridisk_orgnummer":"11111111"}',
                null,
                '2025-09-16T15:11:37.694893Z',
                'importert-historisk-status'
            )
            """.trimIndent(),
        )

        historiskeManglendeStatuserProsessor.prosesser()

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().sisteHendelse().run {
            status shouldBeEqualTo HendelseStatus.SENDT_TIL_ARBEIDSGIVER
            tilleggsinfo.shouldBeInstanceOf<UtdatertFormatTilleggsinfo>().run {
                arbeidsgiver.shouldNotBeNull().orgnavn shouldBeEqualTo "NAVN"
            }
        }

        jdbcTemplate.update(
            """
            TRUNCATE TABLE temp_resterende_sykmeldingstatuser_fra_tsm
            """.trimIndent(),
        )
    }
}
