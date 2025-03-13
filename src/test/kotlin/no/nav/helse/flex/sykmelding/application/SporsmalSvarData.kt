package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.api.dto.Blad
import no.nav.helse.flex.api.dto.LottOgHyre
import no.nav.helse.flex.api.dto.YesOrNo
import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.SporsmalTag
import no.nav.helse.flex.sykmelding.domain.Svar
import no.nav.helse.flex.sykmelding.domain.Svartype

fun lagSporsmalSvarFiskerMedHyre() =
    listOf(
        Sporsmal(
            tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSSITUASJON,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "FISKER")),
        ),
        Sporsmal(
            tag = SporsmalTag.FISKER,
            svartype = Svartype.GRUPPE_AV_UNDERSPORSMAL,
            undersporsmal =
                listOf(
                    Sporsmal(
                        tag = SporsmalTag.FISKER__BLAD,
                        svartype = Svartype.RADIO,
                        svar = listOf(Svar(verdi = Blad.A.name)),
                    ),
                    Sporsmal(
                        tag = SporsmalTag.FISKER__LOTT_OG_HYRE,
                        svartype = Svartype.RADIO,
                        svar = listOf(Svar(verdi = LottOgHyre.HYRE.name)),
                    ),
                ),
            svar = emptyList(),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "orgnr")),
        ),
        Sporsmal(
            tag = SporsmalTag.RIKTIG_NARMESTE_LEDER,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.EGENMELINGSDAGER,
            svartype = Svartype.DATOER,
            svar = listOf(Svar(verdi = "2021-01-01"), Svar(verdi = "2021-01-02")),
        ),
    )

fun lagSporsmalSvarFiskerMedLott() =
    listOf(
        Sporsmal(
            tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSSITUASJON,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "JORDBRUKER")),
        ),
        Sporsmal(
            tag = SporsmalTag.FISKER,
            svartype = Svartype.GRUPPE_AV_UNDERSPORSMAL,
            undersporsmal =
                listOf(
                    Sporsmal(
                        tag = SporsmalTag.FISKER__BLAD,
                        svartype = Svartype.RADIO,
                        svar = listOf(Svar(verdi = Blad.A.name)),
                    ),
                    Sporsmal(
                        tag = SporsmalTag.FISKER__LOTT_OG_HYRE,
                        svartype = Svartype.RADIO,
                        svar = listOf(Svar(verdi = LottOgHyre.LOTT.name)),
                    ),
                ),
            svar = emptyList(),
        ),
        Sporsmal(
            tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.EGENMELDINGSPERIODER,
            svartype = Svartype.PERIODER,
            svar = listOf(Svar(verdi = "{fom: 2021-01-01, tom: 2021-01-02}")),
        ),
        Sporsmal(
            tag = SporsmalTag.HAR_FORSIKRING,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
    )

fun lagSporsmalSvarFiskerMedLottOgHyre() =
    listOf(
        Sporsmal(
            tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSSITUASJON,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "FISKER")),
        ),
        Sporsmal(
            tag = SporsmalTag.FISKER,
            svartype = Svartype.GRUPPE_AV_UNDERSPORSMAL,
            undersporsmal =
                listOf(
                    Sporsmal(
                        tag = SporsmalTag.FISKER__BLAD,
                        svartype = Svartype.RADIO,
                        svar = listOf(Svar(verdi = Blad.A.name)),
                    ),
                    Sporsmal(
                        tag = SporsmalTag.FISKER__LOTT_OG_HYRE,
                        svartype = Svartype.RADIO,
                        svar = listOf(Svar(verdi = LottOgHyre.BEGGE.name)),
                    ),
                ),
            svar = emptyList(),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "orgnr")),
        ),
        Sporsmal(
            tag = SporsmalTag.RIKTIG_NARMESTE_LEDER,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.EGENMELINGSDAGER,
            svartype = Svartype.DATOER,
            svar = listOf(Svar(verdi = "2021-01-01"), Svar(verdi = "2021-01-02")),
        ),
    )

fun lagSporsmalSvarSelvstendigNaringsdrivende(arbeidssituasjon: String = "NAERINGSDRIVENDE") =
    listOf(
        Sporsmal(
            tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSSITUASJON,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = arbeidssituasjon)),
        ),
        Sporsmal(
            tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.EGENMELDINGSPERIODER,
            svartype = Svartype.PERIODER,
            svar = listOf(Svar(verdi = "{fom: 2021-01-01, tom: 2021-01-02}")),
        ),
        Sporsmal(
            tag = SporsmalTag.HAR_FORSIKRING,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
    )

fun lagSporsmalSvarJordbruker() = lagSporsmalSvarSelvstendigNaringsdrivende(arbeidssituasjon = "JORDBRUKER")

fun lagSporsmalSvarFrilanser() = lagSporsmalSvarSelvstendigNaringsdrivende(arbeidssituasjon = "FRILANSER")

fun lagSporsmalSvarArbeidsledig(arbeidssituasjon: String = "ARBEIDSLEDIG") =
    listOf(
        Sporsmal(
            tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSSITUASJON,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = arbeidssituasjon)),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSLEDIG_FRA_ORGNUMMER,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "orgnr")),
        ),
    )

fun lagSporsmalSvarPermittert() = lagSporsmalSvarArbeidsledig(arbeidssituasjon = "PERMITTERT")

fun lagSporsmalSvarArbeidstaker() =
    listOf(
        Sporsmal(
            tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSSITUASJON,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "ARBEIDSTAKER")),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "orgnr")),
        ),
        Sporsmal(
            tag = SporsmalTag.RIKTIG_NARMESTE_LEDER,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.HAR_BRUKT_EGENMELINGSDAGER,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.EGENMELINGSDAGER,
            svartype = Svartype.DATOER,
            svar = listOf(Svar(verdi = "2021-01-01"), Svar(verdi = "2021-01-02")),
        ),
    )

fun lagSporsmalSvarAnnet() =
    listOf(
        Sporsmal(
            tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
            svartype = Svartype.JA_NEI,
            svar = listOf(Svar(verdi = YesOrNo.YES.name)),
        ),
        Sporsmal(
            tag = SporsmalTag.ARBEIDSSITUASJON,
            svartype = Svartype.RADIO,
            svar = listOf(Svar(verdi = "ANNET")),
        ),
    )
