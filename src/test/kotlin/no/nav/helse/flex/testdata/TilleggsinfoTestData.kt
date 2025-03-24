package no.nav.helse.flex.testdata

import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.*

fun lagArbeidstakerTilleggsinfo(arbeidsgiver: Arbeidsgiver = lagArbeidsgiver()): ArbeidstakerTilleggsinfo =
    ArbeidstakerTilleggsinfo(
        arbeidsgiver = arbeidsgiver,
    )

fun lagArbeidsledigTilleggsinfo(
    tidligereArbeidsgiver: TidligereArbeidsgiver? =
        TidligereArbeidsgiver(
            orgNavn = "orgnavn",
            orgnummer = "orgnr",
        ),
): ArbeidsledigTilleggsinfo =
    ArbeidsledigTilleggsinfo(
        tidligereArbeidsgiver = tidligereArbeidsgiver,
    )

fun lagPermittertTilleggsinfo(
    tidligereArbeidsgiver: TidligereArbeidsgiver? =
        TidligereArbeidsgiver(
            orgNavn = "orgnavn",
            orgnummer = "orgnr",
        ),
): PermittertTilleggsinfo =
    PermittertTilleggsinfo(
        tidligereArbeidsgiver = tidligereArbeidsgiver,
    )

fun lagFiskerTilleggsinfo(arbeidsgiver: Arbeidsgiver? = lagArbeidsgiver()): FiskerTilleggsinfo =
    FiskerTilleggsinfo(
        arbeidsgiver = arbeidsgiver,
    )

fun lagFrilanserTilleggsinfo(): FrilanserTilleggsinfo = FrilanserTilleggsinfo

fun lagJordbrukerTilleggsinfo(): JordbrukerTilleggsinfo = JordbrukerTilleggsinfo

fun lagNaringsdrivendeTilleggsinfo(): NaringsdrivendeTilleggsinfo = NaringsdrivendeTilleggsinfo

fun lagAnnetArbeidssituasjonTilleggsinfo(): AnnetArbeidssituasjonTilleggsinfo = AnnetArbeidssituasjonTilleggsinfo

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
