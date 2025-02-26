package no.nav.helse.flex.arbeidsforhold

import no.nav.helse.flex.arbeidsforhold.innhenting.EksterntArbeidsforhold
import no.nav.helse.flex.arbeidsforhold.innhenting.IdenterOgEksterneArbeidsforhold
import no.nav.helse.flex.clients.pdl.PersonIdenter
import java.time.Instant
import java.time.LocalDate

fun lagArbeidsforhold(
    id: String? = null,
    navArbeidsforholdId: String = "navArbeidsforholdId",
    fnr: String = "00000000001",
    orgnummer: String = "org",
    juridiskOrgnummer: String = "org",
    orgnavn: String = "Org",
    fom: LocalDate = LocalDate.parse("2020-01-01"),
    tom: LocalDate? = null,
    arbeidsforholdType: ArbeidsforholdType? = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
    opprettet: Instant = Instant.parse("2020-01-01T00:00:00Z"),
): Arbeidsforhold =
    Arbeidsforhold(
        id = id,
        navArbeidsforholdId = navArbeidsforholdId,
        fnr = fnr,
        orgnummer = orgnummer,
        juridiskOrgnummer = juridiskOrgnummer,
        orgnavn = orgnavn,
        fom = fom,
        tom = tom,
        arbeidsforholdType = arbeidsforholdType,
        opprettet = opprettet,
    )

fun lagEksterntArbeidsforhold(
    navArbeidsforholdId: String = "navArbeidsforholdId",
    orgnummer: String = "orgnummer",
    juridiskOrgnummer: String = "jorgnummer",
    orgnavn: String = "Orgnavn",
    fom: LocalDate = LocalDate.parse("2020-01-01"),
    tom: LocalDate? = null,
    arbeidsforholdType: ArbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
): EksterntArbeidsforhold =
    EksterntArbeidsforhold(
        navArbeidsforholdId = navArbeidsforholdId,
        orgnummer = orgnummer,
        juridiskOrgnummer = juridiskOrgnummer,
        orgnavn = orgnavn,
        fom = fom,
        tom = tom,
        arbeidsforholdType = arbeidsforholdType,
    )

fun lagIdenterOgEksterneArbeidsforhold(
    identer: PersonIdenter = PersonIdenter(originalIdent = "00000000001"),
    eksterneArbeidsforhold: List<EksterntArbeidsforhold> = listOf(lagEksterntArbeidsforhold()),
): IdenterOgEksterneArbeidsforhold =
    IdenterOgEksterneArbeidsforhold(
        identer = identer,
        eksterneArbeidsforhold = eksterneArbeidsforhold,
    )
