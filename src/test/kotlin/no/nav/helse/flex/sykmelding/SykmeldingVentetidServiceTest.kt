package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.gateways.syketilfelle.FomTomPeriode
import no.nav.helse.flex.gateways.syketilfelle.SammeVentetidPeriode
import no.nav.helse.flex.gateways.syketilfelle.SammeVentetidResponse
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.BrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SyketilfelleClientFake
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SykmeldingVentetidServiceTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingVentetidService: SykmeldingVentetidService

    @Autowired
    lateinit var syketilfelleClient: SyketilfelleClientFake

    @AfterEach
    fun ryddOpp() {
        slettDatabase()
        syketilfelleClient.reset()
    }

    @Nested
    inner class ErForsteSykmeldingMedSammeVentetidOgArbeidssituasjon {
        @Test
        fun `burde returnere true dersom ingen andre sykmeldinger har samme ventetid`() {
            val sykmelding = lagreSykmelding(id = "1")

            val result =
                sykmeldingVentetidService.erForsteSykmeldingMedSammeVentetidOgArbeidssituasjon(
                    sykmelding = sykmelding,
                    arbeidssituasjon = Arbeidssituasjon.ANNET,
                )

            result `should be equal to` true
        }

        @Test
        fun `burde returnere true dersom denne sykmeldingen har tidligst fom blant sykmeldinger med samme ventetid`() {
            val tidligsteSykmelding =
                lagreSykmelding(
                    id = "1",
                    fom = LocalDate.parse("2021-01-01"),
                    tom = LocalDate.parse("2021-01-10"),
                    brukerSvar = lagArbeidsledigBrukerSvar(),
                )
            lagreSykmelding(
                id = "2",
                fom = LocalDate.parse("2021-01-05"),
                tom = LocalDate.parse("2021-01-15"),
                brukerSvar = lagArbeidsledigBrukerSvar(),
            )
            settPerioderMedSammeVentetid("1", "2")

            val result =
                sykmeldingVentetidService.erForsteSykmeldingMedSammeVentetidOgArbeidssituasjon(
                    sykmelding = tidligsteSykmelding,
                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG,
                )

            result `should be equal to` true
        }

        @Test
        fun `burde returnere false dersom en annen sykmelding med samme ventetid og arbeidssituasjon har tidligere fom`() {
            lagreSykmelding(
                id = "1",
                fom = LocalDate.parse("2021-01-01"),
                tom = LocalDate.parse("2021-01-10"),
                brukerSvar = lagArbeidsledigBrukerSvar(),
            )
            val sendreSykmelding =
                lagreSykmelding(
                    id = "2",
                    fom = LocalDate.parse("2021-01-05"),
                    tom = LocalDate.parse("2021-01-15"),
                    brukerSvar = lagArbeidsledigBrukerSvar(),
                )
            settPerioderMedSammeVentetid("1", "2")

            val result =
                sykmeldingVentetidService.erForsteSykmeldingMedSammeVentetidOgArbeidssituasjon(
                    sykmelding = sendreSykmelding,
                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG,
                )

            result `should be equal to` false
        }

        @Test
        fun `burde ignorere sykmeldinger med annen arbeidssituasjon ved sammenligning`() {
            val sykmelding =
                lagreSykmelding(
                    id = "1",
                    fom = LocalDate.parse("2021-01-05"),
                    tom = LocalDate.parse("2021-01-15"),
                    brukerSvar = lagAnnetArbeidssituasjonBrukerSvar(),
                )
            lagreSykmelding(
                id = "2",
                fom = LocalDate.parse("2021-01-01"),
                tom = LocalDate.parse("2021-01-10"),
                brukerSvar = lagArbeidsledigBrukerSvar(),
            )
            settPerioderMedSammeVentetid("1", "2")

            val result =
                sykmeldingVentetidService.erForsteSykmeldingMedSammeVentetidOgArbeidssituasjon(
                    sykmelding = sykmelding,
                    arbeidssituasjon = Arbeidssituasjon.ANNET,
                )

            result `should be equal to` true
        }
    }

    private fun lagreSykmelding(
        id: String,
        fnr: String = "fnr",
        fom: LocalDate = LocalDate.parse("2021-01-01"),
        tom: LocalDate = LocalDate.parse("2021-01-10"),
        brukerSvar: BrukerSvar? = null,
    ) = sykmeldingRepository.save(
        lagSykmelding(
            sykmeldingGrunnlag =
                lagSykmeldingGrunnlag(
                    id = id,
                    pasient = lagPasient(fnr = fnr),
                    aktiviteter = listOf(lagAktivitetIkkeMulig(fom, tom)),
                ),
            hendelser =
                listOf(
                    lagSykmeldingHendelse(
                        status = if (brukerSvar != null) HendelseStatus.SENDT_TIL_NAV else HendelseStatus.APEN,
                        brukerSvar = brukerSvar,
                    ),
                ),
        ),
    )

    private fun settPerioderMedSammeVentetid(vararg ressursIder: String) {
        syketilfelleClient.setPerioderMedSammeVentetid(
            SammeVentetidResponse(
                ventetidPerioder =
                    ressursIder.map {
                        SammeVentetidPeriode(
                            ressursId = it,
                            ventetid = FomTomPeriode(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-01-16")),
                        )
                    },
            ),
        )
    }
}
