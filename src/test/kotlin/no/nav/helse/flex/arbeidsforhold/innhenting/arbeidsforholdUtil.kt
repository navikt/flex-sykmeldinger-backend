package no.nav.helse.flex.arbeidsforhold.innhenting

import net.bytebuddy.asm.Advice.Local
import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

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
    opprettet: Instant = Instant.parse("2020-01-01T00:00:00Z"),
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
        opprettet = opprettet,
    )
}

fun lagEksterntArbeidsforhold(
    arbeidsforholdId: String = "arbeidsforholdId",
    fnr: String = "fnr",
    orgnummer: String = "orgnummer",
    juridiskOrgnummer: String = "jorgnummer",
    orgnavn: String = "Orgnavn",
    fom: LocalDate = LocalDate.parse("2020-01-01"),
    tom: LocalDate? = null,
    arbeidsforholdType: ArbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
): EksterntArbeidsforhold {
    return EksterntArbeidsforhold(
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
