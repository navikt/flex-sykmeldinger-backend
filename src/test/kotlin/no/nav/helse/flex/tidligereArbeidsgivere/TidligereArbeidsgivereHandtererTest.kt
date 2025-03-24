package no.nav.helse.flex.tidligereArbeidsgivere

import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidligereArbeidsgivereHandtererTest {
    @Test
    fun `burde finne tidligere arbeidsgivere når aktivitet (periode) er overlappende`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
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
    fun `burde finne tidligere arbeidsgivere når aktivitet (periode) er kant i kant`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
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
    fun `burde finne tidligere arbeidsgivere når aktivitet (periode) er kant i kant med helg`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-06"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-13"),
                                    tom = LocalDate.parse("2025-01-17"),
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
    fun `burde ikke finne tidligere arbeidsgivere når gamle sykmeldinger er før ny`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-20"),
                                    tom = LocalDate.parse("2025-01-30"),
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

        tidligereArbeidsgivere.size `should be equal to` 0
    }

    @Test
    fun `burde kun returnere unike arbeidsgivere`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )

        val gammelSykmeldingMedSammeArbeidsgiver =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-19"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-20"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
                hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV)),
            )
        val alleSykmeldinger = listOf(gammelSykmelding, gammelSykmeldingMedSammeArbeidsgiver, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 1
    }

    @Test
    fun `burde finne alle tidligere arbeidsgivere`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )

        val annenGammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Annen gammel jobb",
                                            orgnummer = "annetGammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-17"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
                hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV)),
            )
        val alleSykmeldinger = listOf(gammelSykmelding, annenGammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 2
    }

    @Test
    fun `burde kun returnere arbeidsgiver fra tidligere valgt`() {
        TODO()
    }

    @Test
    fun `burde returnere arbeidsgivere fra to parallele sykmeldingsforløp`() {
        TODO()
    }

    @Test
    fun `burde ikke returnere når det er lik fom`() {
        TODO()
    }

    @Test
    fun `burde ikke returnere _fremtidig_ arbeidsgiver`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )

        val gjeldendeSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
                hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV)),
            )

        val fremtidigSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-17"),
                                    tom = LocalDate.parse("2025-01-30"),
                                ),
                            ),
                    ),
                hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER)),
            )
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
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )

        val annenGammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Annen gammel jobb",
                                            orgnummer = "AnnetGammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
                hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_NAV)),
            )
        val alleSykmeldinger = listOf(gammelSykmelding, annenGammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 2
    }

    @Test
    fun `burde returnere begge arbeidsgivere der en sykmelding _omringer_ en annen`() {
        val gammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-01"),
                                    tom = LocalDate.parse("2025-01-10"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Gammel jobb",
                                            orgnummer = "gammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )

        val annenGammelSykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "2",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-05"),
                                    tom = LocalDate.parse("2025-01-07"),
                                ),
                            ),
                    ),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo =
                                lagArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnavn = "Annen gammel jobb",
                                            orgnummer = "AnnetGammeltOrgnummer",
                                            erAktivtArbeidsforhold = false,
                                        ),
                                ),
                        ),
                    ),
            )
        val nySykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "3",
                        aktiviteter =
                            listOf(
                                lagAktivitetIkkeMulig(
                                    fom = LocalDate.parse("2025-01-11"),
                                    tom = LocalDate.parse("2025-01-20"),
                                ),
                            ),
                    ),
                hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER)),
            )
        val alleSykmeldinger = listOf(gammelSykmelding, annenGammelSykmelding, nySykmelding)

        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger,
                nySykmelding.sykmeldingId,
            )

        tidligereArbeidsgivere.size `should be equal to` 2
    }
}
