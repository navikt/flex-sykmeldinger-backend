package no.nav.helse.flex.sykmeldinghendelse

import no.nav.helse.flex.sykmelding.tsm.AvsenderSystem
import no.nav.helse.flex.sykmelding.tsm.AvsenderSystemNavn
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldContainIgnoringCase
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingStatusEndrerTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusEndrer: SykmeldingStatusEndrer

    private val standardStatusEndringCaser =
        listOf(
            StatusEndringCase(
                til = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                tillattFra = listOf(HendelseStatus.APEN, HendelseStatus.SENDT_TIL_NAV, HendelseStatus.AVBRUTT),
                feilerFra = listOf(HendelseStatus.UTGATT, HendelseStatus.SENDT_TIL_ARBEIDSGIVER, HendelseStatus.BEKREFTET_AVVIST),
            ),
            StatusEndringCase(
                til = HendelseStatus.SENDT_TIL_NAV,
                tillattFra = listOf(HendelseStatus.APEN, HendelseStatus.SENDT_TIL_NAV, HendelseStatus.AVBRUTT),
                feilerFra = listOf(HendelseStatus.UTGATT, HendelseStatus.SENDT_TIL_ARBEIDSGIVER, HendelseStatus.BEKREFTET_AVVIST),
            ),
            StatusEndringCase(
                til = HendelseStatus.AVBRUTT,
                tillattFra = listOf(HendelseStatus.APEN, HendelseStatus.SENDT_TIL_NAV, HendelseStatus.AVBRUTT),
                feilerFra = listOf(HendelseStatus.UTGATT, HendelseStatus.SENDT_TIL_ARBEIDSGIVER, HendelseStatus.BEKREFTET_AVVIST),
            ),
            StatusEndringCase(
                til = HendelseStatus.BEKREFTET_AVVIST,
                tillattFra = listOf(),
                feilerFra =
                    listOf(
                        HendelseStatus.UTGATT,
                        HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        HendelseStatus.AVBRUTT,
                        HendelseStatus.BEKREFTET_AVVIST,
                        HendelseStatus.SENDT_TIL_NAV,
                    ),
            ),
            StatusEndringCase(
                til = HendelseStatus.UTGATT,
                tillattFra = listOf(HendelseStatus.APEN, HendelseStatus.AVBRUTT),
                feilerFra =
                    listOf(
                        HendelseStatus.UTGATT,
                        HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        HendelseStatus.BEKREFTET_AVVIST,
                        HendelseStatus.SENDT_TIL_NAV,
                    ),
            ),
        )

    @TestFactory
    fun `burde endre status ved normal sykmelding`() =
        standardStatusEndringCaser
            .flatMap { case ->
                case.tillattFra.map { sisteStatus ->
                    val tilStatus = case.til
                    DynamicTest.dynamicTest("$sisteStatus -> $tilStatus") {
                        val sykmelding =
                            lagSykmelding(
                                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                                hendelser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                            )
                        sykmeldingStatusEndrer.sjekkStatusEndring(sykmelding, nyStatus = tilStatus)
                    }
                }
            }

    @TestFactory
    fun `burde ikke endre status ved normal sykmelding`() =
        standardStatusEndringCaser
            .flatMap { case ->
                case.feilerFra.map { sisteStatus ->
                    val tilStatus = case.til
                    DynamicTest.dynamicTest("$sisteStatus -> $tilStatus") {
                        val sykmelding =
                            lagSykmelding(
                                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                                hendelser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                            )
                        invoking {
                            sykmeldingStatusEndrer.sjekkStatusEndring(
                                sykmelding,
                                nyStatus = tilStatus,
                            )
                        }.shouldThrow(UgyldigSykmeldingStatusException::class)
                            .exceptionMessage
                            .shouldContainIgnoringCase(tilStatus.name)
                            .shouldContainIgnoringCase(sisteStatus.name)
                    }
                }
            }

    @TestFactory
    fun `burde ikke endre status fra APEN dersom sykmelding er avvist, til`() =
        listOf(
            HendelseStatus.APEN,
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
            HendelseStatus.SENDT_TIL_NAV,
            HendelseStatus.AVBRUTT,
        ).map { nyStatus ->
            DynamicTest.dynamicTest(nyStatus.name) {
                val sykmelding =
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                        validation = lagValidation(status = RuleType.INVALID),
                        hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.APEN)),
                    )
                invoking {
                    sykmeldingStatusEndrer.sjekkStatusEndring(
                        sykmelding,
                        nyStatus = nyStatus,
                    )
                }.shouldThrow(UgyldigSykmeldingStatusException::class)
                    .exceptionMessage
                    .shouldContainIgnoringCase(nyStatus.name)
                    .shouldContainIgnoringCase("avvist")
            }
        }

    @TestFactory
    fun `burde endre status fra APEN dersom sykmelding er avvist, til`() =
        listOf(
            HendelseStatus.BEKREFTET_AVVIST,
            HendelseStatus.UTGATT,
        ).map { nyStatus ->
            DynamicTest.dynamicTest(nyStatus.name) {
                val sykmelding =
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                        validation = lagValidation(status = RuleType.INVALID),
                    )
                sykmeldingStatusEndrer.sjekkStatusEndring(
                    sykmelding,
                    nyStatus = nyStatus,
                )
            }
        }

    @Test
    fun `burde aldri endre status dersom sykmelding er egenmeldt`() {
        HendelseStatus.entries.forEach { tilStatus ->
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            metadata =
                                lagSykmeldingMetadata(
                                    avsenderSystem =
                                        AvsenderSystem(
                                            navn = AvsenderSystemNavn.EGENMELDT,
                                            versjon = "1.0.0",
                                        ),
                                ),
                        ),
                )
            invoking {
                sykmeldingStatusEndrer.sjekkStatusEndring(sykmelding, nyStatus = tilStatus)
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase("egenmeldt")
        }
    }
}

private data class StatusEndringCase(
    val til: HendelseStatus,
    val tillattFra: List<HendelseStatus>,
    val feilerFra: List<HendelseStatus>,
)
