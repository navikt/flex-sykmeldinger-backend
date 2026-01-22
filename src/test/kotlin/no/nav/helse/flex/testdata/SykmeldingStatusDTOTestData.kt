package no.nav.helse.flex.testdata

import no.nav.helse.flex.api.dto.*
import java.time.LocalDate
import java.time.OffsetDateTime

fun lagSykmeldingStatusDTO(
    statusEvent: String = "APEN",
    timestamp: OffsetDateTime = OffsetDateTime.now(),
    arbeidsgiver: ArbeidsgiverStatusDTO? = null,
    brukerSvar: SykmeldingSporsmalSvarDto? = null,
): SykmeldingStatusDTO =
    SykmeldingStatusDTO(
        statusEvent = statusEvent,
        timestamp = timestamp,
        arbeidsgiver = arbeidsgiver,
        brukerSvar = brukerSvar,
    )

fun lagSykmeldingSporsmalSvarDto(
    arbeidssituasjon: FormSporsmalSvar<ArbeidssituasjonDTO> = lagFormSporsmalSvar(ArbeidssituasjonDTO.ARBEIDSTAKER),
    riktigNarmesteLeder: FormSporsmalSvar<JaEllerNei>? = null,
    fisker: FiskerSvar? = null,
): SykmeldingSporsmalSvarDto =
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
        arbeidssituasjon = arbeidssituasjon,
        arbeidsgiverOrgnummer =
            FormSporsmalSvar(
                sporsmaltekst = "Hva er arbeidsgiverens orgnummer?",
                svar = "orgnummer",
            ),
        arbeidsledig =
            ArbeidsledigFraOrgnummer(
                arbeidsledigFraOrgnummer =
                    FormSporsmalSvar(
                        sporsmaltekst = "Hva er orgnummeret du er arbeidsledig fra?",
                        svar = "987654321",
                    ),
            ),
        riktigNarmesteLeder = riktigNarmesteLeder,
        sykFoerSykmeldingen =
            FormSporsmalSvar(
                sporsmaltekst = "Var du syk før sykmeldingen?",
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
                        EgenmeldingsperiodeFormDTO(
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
        fisker = fisker,
    )

fun lagFiskerSvar(
    blad: FormSporsmalSvar<Blad> = lagFormSporsmalSvar(Blad.A),
    lottOgHyre: FormSporsmalSvar<LottOgHyre> = lagFormSporsmalSvar(LottOgHyre.LOTT),
) = FiskerSvar(
    blad = blad,
    lottOgHyre = lottOgHyre,
)

fun <T> lagFormSporsmalSvar(
    svar: T,
    sporsmalstekst: String = "<ukjent spørsmål>",
) = FormSporsmalSvar(
    sporsmaltekst = sporsmalstekst,
    svar = svar,
)
