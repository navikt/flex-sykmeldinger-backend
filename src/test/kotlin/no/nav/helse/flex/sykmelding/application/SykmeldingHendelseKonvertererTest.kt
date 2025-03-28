package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagBrukerSvarKafkaDto
import no.nav.helse.flex.testdata.lagStatus
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SykmeldingHendelseKonvertererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingHendelseKonverterer: SykmeldingHendelseKonverterer

    @Test
    fun `burde konvertere status til sykmelding hendelse`() {
        val status =
            lagStatus(
                sykmeldingId = "1",
                fnr = "fnr",
                brukerSvarKafkaDTO = lagBrukerSvarKafkaDto(ArbeidssituasjonKafkaDTO.ARBEIDSTAKER),
                statusEvent = "APEN",
                source = "tsm",
            )
        sykmeldingHendelseKonverterer.konverterStatusTilSykmeldingHendelse(status).`should not be null`()
    }

    @Test
    fun `burde feile uten brukerSvar`() {
        val status =
            lagStatus(
                sykmeldingId = "1",
                fnr = "fnr",
                brukerSvarKafkaDTO = null,
                statusEvent = "APEN",
                source = "tsm",
            )
        invoking { sykmeldingHendelseKonverterer.konverterStatusTilSykmeldingHendelse(status) } `should throw` IllegalStateException::class
    }

    @Test
    fun `burde konvertere status til hendelse status`() {
        sykmeldingHendelseKonverterer.konverterStatusTilHendelseStatus("APEN") `should be equal to` HendelseStatus.APEN
        sykmeldingHendelseKonverterer.konverterStatusTilHendelseStatus("BEKREFTET") `should be equal to` HendelseStatus.SENDT_TIL_NAV
        sykmeldingHendelseKonverterer.konverterStatusTilHendelseStatus("SENDT") `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
        sykmeldingHendelseKonverterer.konverterStatusTilHendelseStatus("AVBRUTT") `should be equal to` HendelseStatus.AVBRUTT
        sykmeldingHendelseKonverterer.konverterStatusTilHendelseStatus("UTGATT") `should be equal to` HendelseStatus.UTGATT
    }

    @ParameterizedTest
    @EnumSource(ArbeidssituasjonKafkaDTO::class)
    fun `burde konvertere BrukerSvarKafkaDto til BrukerSvar`(arbeidssituasjonKafkaDTO: ArbeidssituasjonKafkaDTO) {
        val brukerSvarKafkaDTO = lagBrukerSvarKafkaDto(arbeidssituasjonKafkaDTO)

        val konvertert = sykmeldingHendelseKonverterer.konverterBrukerSvarKafkaDtoTilBrukerSvar(brukerSvarKafkaDTO)
        konvertert.uriktigeOpplysninger?.svar `should be equal to` listOf(UriktigeOpplysning.PERIODE)
        konvertert.erOpplysningeneRiktige.svar `should be equal to` true
        konvertert.arbeidssituasjonSporsmal.svar.name `should be equal to` arbeidssituasjonKafkaDTO.name

        when (arbeidssituasjonKafkaDTO) {
            ArbeidssituasjonKafkaDTO.ARBEIDSTAKER -> {
                val arbeidstakerBrukerSvar = konvertert as? ArbeidstakerBrukerSvar
                arbeidstakerBrukerSvar.`should not be null`()
                arbeidstakerBrukerSvar.arbeidsgiverOrgnummer.svar `should be equal to` "123456789"
                arbeidstakerBrukerSvar.riktigNarmesteLeder.svar `should be equal to` true
                arbeidstakerBrukerSvar.harEgenmeldingsdager.svar `should be equal to` true
                arbeidstakerBrukerSvar.egenmeldingsdager?.svar `should be equal to` listOf(LocalDate.parse("2021-01-01"))
            }
            ArbeidssituasjonKafkaDTO.FRILANSER -> {
                val frilanserBrukerSvar = konvertert as? FrilanserBrukerSvar
                frilanserBrukerSvar.`should not be null`()
                frilanserBrukerSvar.harBruktEgenmelding.svar `should be equal to` true
                frilanserBrukerSvar.egenmeldingsperioder?.svar `should be equal to`
                    listOf(
                        Egenmeldingsperiode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05")),
                        Egenmeldingsperiode(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15")),
                    )
                frilanserBrukerSvar.harForsikring.svar `should be equal to` true
            }
            ArbeidssituasjonKafkaDTO.NAERINGSDRIVENDE -> {
                val naeringsdrivendeBrukerSvar = konvertert as? NaringsdrivendeBrukerSvar
                naeringsdrivendeBrukerSvar.`should not be null`()
                naeringsdrivendeBrukerSvar.harBruktEgenmelding.svar `should be equal to` true
                naeringsdrivendeBrukerSvar.egenmeldingsperioder?.svar `should be equal to`
                    listOf(
                        Egenmeldingsperiode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05")),
                        Egenmeldingsperiode(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15")),
                    )
                naeringsdrivendeBrukerSvar.harForsikring.svar `should be equal to` true
            }
            ArbeidssituasjonKafkaDTO.FISKER -> {
                val fiskerBrukerSvar = konvertert as? FiskerBrukerSvar
                fiskerBrukerSvar.`should not be null`()
                fiskerBrukerSvar.lottOgHyre.svar `should be equal to` FiskerLottOgHyre.LOTT
                fiskerBrukerSvar.blad.svar `should be equal to` FiskerBlad.A
            }
            ArbeidssituasjonKafkaDTO.JORDBRUKER -> {
                val jordbrukerBrukerSvar = konvertert as? JordbrukerBrukerSvar
                jordbrukerBrukerSvar.`should not be null`()
                jordbrukerBrukerSvar.harBruktEgenmelding.svar `should be equal to` true
                jordbrukerBrukerSvar.egenmeldingsperioder?.svar `should be equal to`
                    listOf(
                        Egenmeldingsperiode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05")),
                        Egenmeldingsperiode(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15")),
                    )
                jordbrukerBrukerSvar.harForsikring.svar `should be equal to` true
            }
            ArbeidssituasjonKafkaDTO.ARBEIDSLEDIG -> {
                val arbeidsledigBrukerSvar = konvertert as? ArbeidsledigBrukerSvar
                arbeidsledigBrukerSvar.`should not be null`()
                arbeidsledigBrukerSvar.arbeidsledigFraOrgnummer?.svar `should be equal to` "123456789"
            }
            ArbeidssituasjonKafkaDTO.ANNET -> {
                val annetBrukerSvar = konvertert as? AnnetArbeidssituasjonBrukerSvar
                annetBrukerSvar.`should not be null`()
            }
        }
    }
}
