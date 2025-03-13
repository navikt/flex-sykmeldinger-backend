package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.SporsmalTag
import no.nav.helse.flex.sykmelding.domain.Svar
import no.nav.helse.flex.sykmelding.domain.Svartype

fun lagSporsmalListe(
    sporsmal: List<Sporsmal> =
        listOf(
            Sporsmal(
                tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
                sporsmalstekst = "Er opplysningene riktige?",
                svartype = Svartype.JA_NEI,
                svar = listOf(Svar(verdi = "JA")),
            ),
            Sporsmal(
                tag = SporsmalTag.ARBEIDSSITUASJON,
                sporsmalstekst = "Hva er din arbeidssituasjon?",
                svartype = Svartype.RADIO,
                svar = listOf(Svar(verdi = "ARBEIDSTAKER")),
            ),
        ),
): List<Sporsmal> = sporsmal
