package no.nav.helse.flex.arbeidsgiverdetaljer

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.arbeidsgiverdetaljer.ArbeidsgiverDetaljerService.Companion.filtrerInnenPeriode
import no.nav.helse.flex.arbeidsgiverdetaljer.domain.ArbeidsgiverDetaljer
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import org.amshove.kluent.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

val HVILKENSOMHELST_DAG: LocalDate = LocalDate.parse("2020-01-01")

class ArbeidsgiverDetaljerHenterServiceTest {
    @Nested
    inner class SammenstillArbeidsgiverDetaljer {
        @Test
        fun `burde sammenstille en fra arbeidsforhold med riktig data`() {
            val alleArbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold =
                        listOf(
                            lagArbeidsforhold(
                                orgnummer = "orgnr",
                                juridiskOrgnummer = "jurorgnr",
                                orgnavn = "Orgnavn",
                                fom = LocalDate.parse("2020-01-01"),
                                tom = LocalDate.parse("2020-01-02"),
                            ),
                        ),
                    narmesteLedere = emptyList(),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            alleArbeidsgiverDetaljer.size `should be equal to` 1

            val arbeidsgiverDetaljer = alleArbeidsgiverDetaljer.first()
            arbeidsgiverDetaljer.orgnummer `should be equal to` "orgnr"
            arbeidsgiverDetaljer.juridiskOrgnummer `should be equal to` "jurorgnr"
            arbeidsgiverDetaljer.navn `should be equal to` "Orgnavn"
            arbeidsgiverDetaljer.fom `should be equal to` LocalDate.parse("2020-01-01")
            arbeidsgiverDetaljer.tom `should be equal to` LocalDate.parse("2020-01-02")
        }

        @Test
        fun `burde sammenstille flere fra flere arbeidsforhold`() {
            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold =
                        listOf(
                            lagArbeidsforhold(orgnummer = "org1"),
                            lagArbeidsforhold(orgnummer = "org2"),
                        ),
                    narmesteLedere = emptyList(),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer.size `should be equal to` 2
        }

        @Test
        fun `burde returnere tom liste dersom arbeidsforhold ikke finnes`() {
            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold = emptyList(),
                    narmesteLedere = emptyList(),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer `should be equal to` emptyList()
        }

        @Test
        fun `burde sammenstille fra arbeidsforhold for freelansere`() {
            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold =
                        listOf(
                            lagArbeidsforhold(arbeidsforholdType = ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_HONORAR_PERSONER_MM),
                        ),
                    narmesteLedere = emptyList(),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer `should be equal to` emptyList()
        }

        @Test
        fun `burde fjerne duplikate arbeidsforhold`() {
            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold =
                        listOf(
                            lagArbeidsforhold(orgnummer = "org"),
                            lagArbeidsforhold(orgnummer = "org"),
                        ),
                    narmesteLedere = emptyList(),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer.size `should be equal to` 1
        }

        @Test
        fun `burde bruke siste av duplikate arbeidsforhold`() {
            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold =
                        listOf(
                            lagArbeidsforhold(orgnavn = "eldre1", orgnummer = "org", fom = LocalDate.parse("2020-01-01")),
                            lagArbeidsforhold(orgnavn = "nyere", orgnummer = "org", fom = LocalDate.parse("2020-01-02")),
                            lagArbeidsforhold(orgnavn = "eldre2", orgnummer = "org", fom = LocalDate.parse("2020-01-01")),
                        ),
                    narmesteLedere = emptyList(),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer.size `should be equal to` 1
            arbeidsgiverDetaljer.first().navn == "nyere"
        }

        @Nested
        inner class AktivtArbeidsforhold {
            private fun sammenstillArbeidsgiverDetaljerProxy(
                arbeidsforhold: Arbeidsforhold,
                idag: LocalDate,
            ): List<ArbeidsgiverDetaljer> =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold = listOf(arbeidsforhold),
                    narmesteLedere = emptyList(),
                    idagProvider = { idag },
                )

            @Test
            fun `start tidligere uten slutt`() {
                sammenstillArbeidsgiverDetaljerProxy(
                    arbeidsforhold = lagArbeidsforhold(fom = LocalDate.parse("2019-12-31"), tom = null),
                    idag = LocalDate.parse("2020-01-01"),
                ).first().aktivtArbeidsforhold.`should be true`()
            }

            @Test
            fun `start tidligere slutt idag`() {
                sammenstillArbeidsgiverDetaljerProxy(
                    arbeidsforhold =
                        lagArbeidsforhold(
                            fom = LocalDate.parse("2019-12-31"),
                            tom = LocalDate.parse("2020-01-01"),
                        ),
                    idag = LocalDate.parse("2020-01-01"),
                ).first().aktivtArbeidsforhold.`should be true`()
            }

            @Test
            fun `start idag uten slutt`() {
                sammenstillArbeidsgiverDetaljerProxy(
                    arbeidsforhold =
                        lagArbeidsforhold(fom = LocalDate.parse("2020-01-01"), tom = null),
                    idag = LocalDate.parse("2020-01-01"),
                ).first().aktivtArbeidsforhold.`should be true`()
            }

            @Test
            fun `start idag slutt idag`() {
                sammenstillArbeidsgiverDetaljerProxy(
                    arbeidsforhold =
                        lagArbeidsforhold(
                            fom = LocalDate.parse("2020-01-01"),
                            tom = LocalDate.parse("2020-01-01"),
                        ),
                    idag = LocalDate.parse("2020-01-01"),
                ).first().aktivtArbeidsforhold.`should be true`()
            }

            @Test
            fun `start idag slutt senere`() {
                sammenstillArbeidsgiverDetaljerProxy(
                    arbeidsforhold =
                        lagArbeidsforhold(
                            fom = LocalDate.parse("2020-01-01"),
                            tom = LocalDate.parse("2020-01-02"),
                        ),
                    idag = LocalDate.parse("2020-01-01"),
                ).first().aktivtArbeidsforhold.`should be true`()
            }

            @Test
            fun `start tidligere slutt tidligere`() {
                sammenstillArbeidsgiverDetaljerProxy(
                    arbeidsforhold =
                        lagArbeidsforhold(
                            fom = LocalDate.parse("2019-12-31"),
                            tom = LocalDate.parse("2019-12-31"),
                        ),
                    idag = LocalDate.parse("2020-01-01"),
                ).first().aktivtArbeidsforhold.`should be false`()
            }

            @Test
            fun `start senere uten slutt`() {
                sammenstillArbeidsgiverDetaljerProxy(
                    arbeidsforhold =
                        lagArbeidsforhold(fom = LocalDate.parse("2020-01-02"), tom = null),
                    idag = LocalDate.parse("2020-01-01"),
                ).first().aktivtArbeidsforhold.`should be false`()
            }

            @Test
            fun `start senere slutt senere`() {
                sammenstillArbeidsgiverDetaljerProxy(
                    arbeidsforhold =
                        lagArbeidsforhold(
                            fom = LocalDate.parse("2020-01-02"),
                            tom = LocalDate.parse("2020-01-02"),
                        ),
                    idag = LocalDate.parse("2020-01-01"),
                ).first().aktivtArbeidsforhold.`should be false`()
            }
        }

        @Test
        fun `burde velge aktivt arbeidsforhold ved duplikate`() {
            val idag = LocalDate.parse("2020-01-01")
            val nyereArbeidsforhold =
                lagArbeidsforhold(orgnavn = "nyere", fom = LocalDate.parse("2020-01-02"), tom = null)
            val aktivtArbeidsforhold =
                lagArbeidsforhold(orgnavn = "aktivt", fom = LocalDate.parse("2020-01-01"), tom = null)

            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold =
                        listOf(
                            nyereArbeidsforhold,
                            aktivtArbeidsforhold,
                        ),
                    narmesteLedere = emptyList(),
                    idagProvider = { idag },
                )

            arbeidsgiverDetaljer.size `should be equal to` 1
            arbeidsgiverDetaljer.first().navn == "aktivt"
        }

        @Test
        fun `burde inkludere narmeste leder med riktig data`() {
            val narmesteLeder = lagNarmesteLeder(orgnummer = "org1")
            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold = listOf(lagArbeidsforhold(orgnummer = "org1")),
                    narmesteLedere = listOf(narmesteLeder),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer.size `should be equal to` 1
            arbeidsgiverDetaljer
                .first()
                .naermesteLeder
                .shouldNotBeNull()
                .`should be equal to`(narmesteLeder)
        }

        @Test
        fun `burde ikke inkludere narmeste leder med manglende navn`() {
            var narmesteLeder = lagNarmesteLeder(orgnummer = "org1", narmesteLederNavn = null)

            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold = listOf(lagArbeidsforhold(orgnummer = "org1")),
                    narmesteLedere = listOf(narmesteLeder),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer.size `should be equal to` 1
            arbeidsgiverDetaljer
                .first()
                .naermesteLeder
                .shouldBeNull()
        }

        @Test
        fun `burde ikke inkludere narmeste leder dersom finnes i annet arbeidsforhold`() {
            val narmesteLeder = lagNarmesteLeder(orgnummer = "org1")
            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold = listOf(lagArbeidsforhold(orgnummer = "org1")),
                    narmesteLedere = listOf(narmesteLeder),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer.size `should be equal to` 1
            arbeidsgiverDetaljer
                .first()
                .naermesteLeder
                .shouldNotBeNull()
                .`should be equal to`(narmesteLeder)
        }

        @Test
        fun `burde inkludere siste narmeste ledere dersom flere finnes`() {
            val narmesteLederTidligere1 = lagNarmesteLeder(orgnummer = "org1", aktivFom = LocalDate.parse("2019-12-31"))
            val narmesteLederSeneste = lagNarmesteLeder(orgnummer = "org1", aktivFom = LocalDate.parse("2020-01-01"))
            val narmesteLederTidligere2 = lagNarmesteLeder(orgnummer = "org1", aktivFom = LocalDate.parse("2019-12-31"))

            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljerService.sammenstillArbeidsgiverDetaljer(
                    arbeidsforhold = listOf(lagArbeidsforhold(orgnummer = "org1")),
                    narmesteLedere =
                        listOf(
                            narmesteLederTidligere1,
                            narmesteLederSeneste,
                            narmesteLederTidligere2,
                        ),
                    idagProvider = { HVILKENSOMHELST_DAG },
                )

            arbeidsgiverDetaljer.size `should be equal to` 1
            arbeidsgiverDetaljer
                .first()
                .naermesteLeder
                .shouldNotBeNull()
                .`should be equal to`(narmesteLederSeneste)
        }
    }

    @Nested
    inner class ArbeidsforholdFiltrerInnenPeriode {
        @Test
        fun `burde returner arbeidsforhold som overlapper med periode`() {
            val arbeidsforhold =
                listOf(
                    lagArbeidsforhold(fom = LocalDate.parse("2020-01-01"), tom = LocalDate.parse("2020-01-02")),
                    lagArbeidsforhold(fom = LocalDate.parse("2020-01-02"), tom = LocalDate.parse("2020-01-03")),
                    lagArbeidsforhold(fom = LocalDate.parse("2020-01-03"), tom = LocalDate.parse("2020-01-04")),
                    lagArbeidsforhold(fom = LocalDate.parse("2020-01-03"), tom = null),
                )
            val periode = LocalDate.parse("2020-01-02") to LocalDate.parse("2020-01-03")
            arbeidsforhold.filtrerInnenPeriode(periode).size `should be equal to` 4
        }

        @Test
        fun `burde filtrere ut arbeidsforhold som ikke overlapper med periode`() {
            val arbeidsforhold =
                listOf(
                    lagArbeidsforhold(fom = LocalDate.parse("2020-01-01"), tom = LocalDate.parse("2020-01-01")),
                    lagArbeidsforhold(fom = LocalDate.parse("2020-01-04"), tom = LocalDate.parse("2020-01-05")),
                    lagArbeidsforhold(fom = LocalDate.parse("2020-01-05"), tom = null),
                )
            val periode = LocalDate.parse("2020-01-02") to LocalDate.parse("2020-01-03")
            arbeidsforhold.filtrerInnenPeriode(periode).size `should be equal to` 0
        }
    }
}
