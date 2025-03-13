package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.Arbeidsgiver
import no.nav.helse.flex.sykmelding.domain.ArbeidstakerInfo

fun lagArbeidstakerInfo(
    arbeidsgiver: Arbeidsgiver =
        Arbeidsgiver(
            orgnummer = "orgnummer",
            juridiskOrgnummer = "juridiskOrgnummer",
            orgnavn = "orgnavn",
        ),
): ArbeidstakerInfo =
    ArbeidstakerInfo(
        arbeidsgiver = arbeidsgiver,
    )
