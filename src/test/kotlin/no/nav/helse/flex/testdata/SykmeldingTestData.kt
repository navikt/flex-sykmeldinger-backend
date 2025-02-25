package no.nav.helse.flex.testdata

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.*
import java.time.Instant
import java.time.LocalDate

fun lagSykmelding(
    sykmeldingGrunnlag: ISykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
    statuser: List<SykmeldingHendelse> =
        listOf(
            lagSykmeldingHendelse(),
        ),
): Sykmelding =
    Sykmelding(
        sykmeldingGrunnlag = sykmeldingGrunnlag,
        statuser = statuser,
        opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
        oppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
    )

fun lagSykmeldingHendelse(
    status: HendelseStatus = HendelseStatus.APEN,
    sporsmalSvar: List<Sporsmal>? = null,
    arbeidstakerInfo: ArbeidstakerInfo? = null,
) = SykmeldingHendelse(
    status = status,
    opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
    sporsmalSvar = sporsmalSvar,
    arbeidstakerInfo = arbeidstakerInfo,
)

fun lagSykmeldingSporsmalSvarDto(arbeidsgiverOrgnummer: String = "123456789"): SykmeldingSporsmalSvarDto =
    SykmeldingSporsmalSvarDto(
        erOpplysningeneRiktige =
            FormSporsmalSvar(
                sporsmaltekst = "Er opplysningene riktige?",
                svar = JaEllerNei.JA,
            ),
        uriktigeOpplysninger =
            FormSporsmalSvar(
                sporsmaltekst = "Hvilke opplysninger er uriktige?",
                svar =
                    listOf(
                        UriktigeOpplysningerType.PERIODE,
                        UriktigeOpplysningerType.DIAGNOSE,
                    ),
            ),
        arbeidssituasjon =
            FormSporsmalSvar(
                sporsmaltekst = "Hva er din arbeidssituasjon?",
                svar = Arbeidssituasjon.ARBEIDSTAKER,
            ),
        arbeidsgiverOrgnummer =
            FormSporsmalSvar(
                sporsmaltekst = "Hva er arbeidsgiverens orgnummer?",
                svar = arbeidsgiverOrgnummer,
            ),
        arbeidsledig =
            ArbeidsledigFraOrgnummer(
                arbeidsledigFraOrgnummer =
                    FormSporsmalSvar(
                        sporsmaltekst = "Hva er orgnummeret du er arbeidsledig fra?",
                        svar = "987654321",
                    ),
            ),
        riktigNarmesteLeder =
            FormSporsmalSvar(
                sporsmaltekst = "Er dette riktig nærmeste leder?",
                svar = JaEllerNei.JA,
            ),
        harBruktEgenmelding =
            FormSporsmalSvar(
                sporsmaltekst = "Har du brukt egenmelding?",
                svar = JaEllerNei.NEI,
            ),
        egenmeldingsperioder =
            FormSporsmalSvar(
                sporsmaltekst = "Hvilke egenmeldingsperioder har du hatt?",
                svar =
                    listOf(
                        Egenmeldingsperiode(
                            fom = LocalDate.parse("2023-01-01"),
                            tom = LocalDate.parse("2023-01-05"),
                        ),
                    ),
            ),
        harForsikring =
            FormSporsmalSvar(
                sporsmaltekst = "Har du forsikring?",
                svar = JaEllerNei.JA,
            ),
        egenmeldingsdager =
            FormSporsmalSvar(
                sporsmaltekst = "Hvilke egenmeldingsdager har du hatt?",
                svar =
                    listOf(
                        LocalDate.parse("2023-01-02"),
                        LocalDate.parse("2023-01-03"),
                    ),
            ),
        harBruktEgenmeldingsdager =
            FormSporsmalSvar(
                sporsmaltekst = "Har du brukt egenmeldingsdager?",
                svar = JaEllerNei.NEI,
            ),
        fisker =
            FiskerSvar(
                blad =
                    FormSporsmalSvar(
                        sporsmaltekst = "Hvilket blad?",
                        svar = Blad.A,
                    ),
                lottOgHyre =
                    FormSporsmalSvar(
                        sporsmaltekst = "Lott og hyre?",
                        svar = LottOgHyre.LOTT,
                    ),
            ),
    )
