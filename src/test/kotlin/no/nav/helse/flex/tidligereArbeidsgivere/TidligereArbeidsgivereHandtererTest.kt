package no.nav.helse.flex.tidligereArbeidsgivere

import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidligereArbeidsgivereHandtererTest {
    private val arbeidsgiver1 = "Arbeidsgiver 1"
    private val arbeidsgiver1Orgnummer = "Arbeidsgiver 1 orgnummer"
    private val arbeidsgiver2 = "Arbeidsgiver 2"
    private val arbeidsgiver2Orgnummer = "Arbeidsgiver 2 orgnummer"

    @Test
    fun `burde finne tidligere arbeidsgivere når aktivitet (periode) er overlappende`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-10"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
                hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV)),
            )
        val alleSykmeldinger = listOf(gammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }

    @Test
    fun `burde finne tidligere arbeidsgivere når tidligere sykmelding er type permittert`() {
        val gammelPermitertSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        tilleggsinfo =
                            lagPermittertTilleggsinfo(
                                tidligereArbeidsgiver =
                                    TidligereArbeidsgiver(
                                        orgNavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                    ),
                            ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-10"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger = listOf(gammelPermitertSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }

    @Test
    fun `burde finne tidligere arbeidsgivere når tidligere sykmelding er type arbeidsledig`() {
        val gammelPermitertSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        tilleggsinfo =
                            lagArbeidsledigTilleggsinfo(
                                tidligereArbeidsgiver =
                                    TidligereArbeidsgiver(
                                        orgNavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-10"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger = listOf(gammelPermitertSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }

    @Test
    fun `burde finne tidligere arbeidsgivere når aktivitet (periode) er kant i kant`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger = listOf(gammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }

    @Test
    fun `burde finne tidligere arbeidsgivere når aktivitet (periode) er kant i kant med helg`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-06"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-13"),
                                    tom = LocalDate.parse("2025-01-17"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger = listOf(gammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }

    @Test
    fun `burde ikke finne tidligere arbeidsgivere når gamle sykmeldinger er før ny`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-20"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger = listOf(gammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 0
    }

    @Test
    fun `burde kun returnere unike arbeidsgivere`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val gammelSykmeldingMedSammeArbeidsgiver =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-19"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-20"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger = listOf(gammelSykmelding, gammelSykmeldingMedSammeArbeidsgiver, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }

    @Test
    fun `burde kun returnere arbeidsgiver fra tidligere valgt`() {
        val sykmeldingForArbeidsgiver1 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val sykmeldingForArbeidsgiver2 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver2,
                                        orgnummer = arbeidsgiver2Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val tidligereArbeidsledigSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        tilleggsinfo =
                            lagArbeidsledigTilleggsinfo(
                                tidligereArbeidsgiver =
                                    TidligereArbeidsgiver(
                                        orgNavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                    ),
                            ),
                    ),
            )

        val nyArbeidsledigSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "4",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-21"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger =
            listOf(
                sykmeldingForArbeidsgiver1,
                sykmeldingForArbeidsgiver2,
                tidligereArbeidsledigSykmelding,
                nyArbeidsledigSykmelding,
            )

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nyArbeidsledigSykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
        tidligereArbeidsgivere.first().let {
            it.orgNavn `should be equal to` arbeidsgiver1
            it.orgnummer `should be equal to` arbeidsgiver1Orgnummer
        }
    }

    @Test
    fun `burde returnere arbeidsgivere fra to parallele sykmeldingsforløp`() {
        val sykmeldingForArbeidsgiver1 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val sykmeldingForArbeidsgiver2 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver2,
                                        orgnummer = arbeidsgiver2Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val tidligereArbeidsledigSykmeldingForArbeidsgiver1 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        tilleggsinfo =
                            lagArbeidsledigTilleggsinfo(
                                tidligereArbeidsgiver =
                                    TidligereArbeidsgiver(
                                        orgNavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                    ),
                            ),
                    ),
            )

        val tidligereArbeidsledigSykmeldingForArbeidsgiver2 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "4",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        tilleggsinfo =
                            lagArbeidsledigTilleggsinfo(
                                tidligereArbeidsgiver =
                                    TidligereArbeidsgiver(
                                        orgNavn = arbeidsgiver2,
                                        orgnummer = arbeidsgiver2Orgnummer,
                                    ),
                            ),
                    ),
            )

        val nyArbeidsledigSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "5",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-21"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger =
            listOf(
                sykmeldingForArbeidsgiver1,
                sykmeldingForArbeidsgiver2,
                tidligereArbeidsledigSykmeldingForArbeidsgiver1,
                tidligereArbeidsledigSykmeldingForArbeidsgiver2,
                nyArbeidsledigSykmelding,
            )

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nyArbeidsledigSykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 2
    }

    @Test
    fun `burde returnere arbeidsgivere fra to _overlappende_ sykmeldingsforløp`() {
        val sykmeldingForArbeidsgiver1 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val sykmeldingForArbeidsgiver2 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-05"),
                                    tom = LocalDate.parse("2025-01-15"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver2,
                                        orgnummer = arbeidsgiver2Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val tidligereArbeidsledigSykmeldingForArbeidsgiver1 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        tilleggsinfo =
                            lagArbeidsledigTilleggsinfo(
                                tidligereArbeidsgiver =
                                    TidligereArbeidsgiver(
                                        orgNavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                    ),
                            ),
                    ),
            )

        val tidligereArbeidsledigSykmeldingForArbeidsgiver2 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "4",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-16"),
                                    tom = LocalDate.parse("2025-01-25"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        tilleggsinfo =
                            lagArbeidsledigTilleggsinfo(
                                tidligereArbeidsgiver =
                                    TidligereArbeidsgiver(
                                        orgNavn = arbeidsgiver2,
                                        orgnummer = arbeidsgiver2Orgnummer,
                                    ),
                            ),
                    ),
            )

        val nyArbeidsledigSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "5",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-21"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger =
            listOf(
                sykmeldingForArbeidsgiver1,
                sykmeldingForArbeidsgiver2,
                tidligereArbeidsledigSykmeldingForArbeidsgiver1,
                tidligereArbeidsledigSykmeldingForArbeidsgiver2,
                nyArbeidsledigSykmelding,
            )

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nyArbeidsledigSykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 2
    }

    @Test
    fun `burde ikke returnere når det er lik fom`() {
        val gammelSykmelding1 =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger = listOf(gammelSykmelding1, nySykmelding)
        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 0
    }

    @Test
    fun `burde ikke returnere _fremtidig_ arbeidsgiver`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val gjeldendeSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val fremtidigSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-17"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER))

        val alleSykmeldinger = listOf(gammelSykmelding, gjeldendeSykmelding, fremtidigSykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                gjeldendeSykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }

    @Test
    fun `burde returnere begge arbeidsgivere som er kant i kant med identiske datoer`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val annenGammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver2,
                                        orgnummer = arbeidsgiver2Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV))

        val alleSykmeldinger = listOf(gammelSykmelding, annenGammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 2
    }

    @Test
    fun `burde kun returnere en arbeidsgiver der en sykmelding har periode inni en annen`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver1,
                                        orgnummer = arbeidsgiver1Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val annenGammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-05"),
                                    tom = LocalDate.parse("2025-01-07"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        tilleggsinfo =
                            lagArbeidstakerTilleggsinfo(
                                arbeidsgiver =
                                    lagArbeidsgiver(
                                        orgnavn = arbeidsgiver2,
                                        orgnummer = arbeidsgiver2Orgnummer,
                                        erAktivtArbeidsforhold = false,
                                    ),
                            ),
                    ),
            )

        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagXMLSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
            ).leggTilHendelse(sykmeldingHendelse = lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER))

        val alleSykmeldinger = listOf(gammelSykmelding, annenGammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }
}
