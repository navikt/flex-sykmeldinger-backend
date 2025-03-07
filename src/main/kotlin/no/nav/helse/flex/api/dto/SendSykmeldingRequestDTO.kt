package no.nav.helse.flex.api.dto

import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.SporsmalTag
import no.nav.helse.flex.sykmelding.domain.Svar
import no.nav.helse.flex.sykmelding.domain.Svartype

data class SendSykmeldingRequestDTO(
    val erOpplysningeneRiktige: String,
    val arbeidsgiverOrgnummer: String?,
    val arbeidssituasjon: Arbeidssituasjon,
    val harEgenmeldingsdager: String?,
    val riktigNarmesteLeder: String?,
    val arbeidsledig: Arbeidsledig?,
) {
    fun tilSporsmalListe(): List<Sporsmal> {
        val sporsmal = mutableListOf<Sporsmal>()
        sporsmal.add(
            Sporsmal(
                tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
                svartype = Svartype.JA_NEI,
                svar = listOf(Svar(verdi = konverterJaNeiSvar(erOpplysningeneRiktige))),
            ),
        )
        arbeidsgiverOrgnummer?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
                    svartype = Svartype.FRITEKST,
                    svar = listOf(Svar(verdi = it)),
                ),
            )
        }
        sporsmal.add(
            Sporsmal(
                tag = SporsmalTag.ARBEIDSSITUASJON,
                svartype = Svartype.RADIO,
                svar = listOf(Svar(verdi = arbeidssituasjon.name)),
            ),
        )
        harEgenmeldingsdager?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = konverterJaNeiSvar(it))),
                ),
            )
        }
        riktigNarmesteLeder?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.RIKTIG_NARMESTE_LEDER,
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = konverterJaNeiSvar(it))),
                ),
            )
        }
        arbeidsledig?.arbeidsledigFraOrgnummer?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSLEDIG_FRA_ORGNUMMER,
                    svartype = Svartype.FRITEKST,
                    svar = listOf(Svar(verdi = it)),
                ),
            )
        }
        return sporsmal
    }

    private fun konverterJaNeiSvar(svar: String): String =
        when (svar) {
            "YES" -> "JA"
            "NO" -> "NEI"
            else -> svar
        }
}

data class Arbeidsledig(
    val arbeidsledigFraOrgnummer: String? = null,
)

enum class Arbeidssituasjon {
    ARBEIDSTAKER,
    FRILANSER,
    NAERINGSDRIVENDE,
    FISKER,
    JORDBRUKER,
    ARBEIDSLEDIG,
    ANNET,
}
