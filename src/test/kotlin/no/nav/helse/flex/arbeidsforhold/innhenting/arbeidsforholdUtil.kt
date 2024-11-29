package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import java.time.LocalDate

fun lagArbeidsforhold(
    id: String? = null,
    arbeidsforholdId: String = "arbeidsforholdId",
    fnr: String = "00000000001",
    orgnummer: String = "org",
    juridiskOrgnummer: String = "org",
    orgnavn: String = "Org",
    fom: LocalDate = LocalDate.parse("2020-01-01"),
    tom: LocalDate? = null,
    arbeidsforholdType: ArbeidsforholdType? = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
): Arbeidsforhold {
    return Arbeidsforhold(
        id = id,
        arbeidsforholdId = arbeidsforholdId,
        fnr = fnr,
        orgnummer = orgnummer,
        juridiskOrgnummer = juridiskOrgnummer,
        orgnavn = orgnavn,
        fom = fom,
        tom = tom,
        arbeidsforholdType = arbeidsforholdType,
    )
}
