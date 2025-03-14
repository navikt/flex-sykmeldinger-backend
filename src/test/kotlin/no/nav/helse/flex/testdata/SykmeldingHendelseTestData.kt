package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.Arbeidsgiver
import no.nav.helse.flex.sykmelding.domain.ArbeidstakerInfo
import no.nav.helse.flex.sykmelding.domain.NarmesteLeder

fun lagArbeidstakerInfo(arbeidsgiver: Arbeidsgiver = lagArbeidsgiver()): ArbeidstakerInfo =
    ArbeidstakerInfo(
        arbeidsgiver = arbeidsgiver,
    )

fun lagArbeidsgiver(
    orgnummer: String = "orgnummer",
    juridiskOrgnummer: String = "juridiskOrgnummer",
    orgnavn: String = "orgnavn",
    erAktivtArbeidsforhold: Boolean = true,
    narmesteLeder: NarmesteLeder? = null,
): Arbeidsgiver =
    Arbeidsgiver(
        orgnummer = orgnummer,
        juridiskOrgnummer = juridiskOrgnummer,
        orgnavn = orgnavn,
        erAktivtArbeidsforhold = erAktivtArbeidsforhold,
        narmesteLeder = narmesteLeder,
    )
