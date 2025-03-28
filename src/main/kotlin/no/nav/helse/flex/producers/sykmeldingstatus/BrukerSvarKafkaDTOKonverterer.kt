package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.application.Arbeidssituasjon
import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.SporsmalTag
import no.nav.helse.flex.sykmelding.domain.Svartype
import java.time.LocalDate

class BrukerSvarKafkaDTOKonverterer {
    fun konverterTilBrukerSvar(sporsmal: List<Sporsmal>): BrukerSvarKafkaDTO =
        BrukerSvarKafkaDTO(
            erOpplysningeneRiktige =
                sporsmal.find { it.tag == SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE }?.let { s ->
                    FormSporsmalSvar(
                        sporsmaltekst = s.konverterSporsmalstekst(),
                        svar = s.konverterTilJaEllerNei(),
                    )
                } ?: throw IllegalArgumentException("Mangler spørsmål om opplysningene er riktige"),
            arbeidssituasjon =
                sporsmal.findByTag(SporsmalTag.ARBEIDSSITUASJON)?.let(::konverterArbeidssituasjon)
                    ?: throw IllegalArgumentException("Mangler spørsmål om arbeidssituasjon"),
            uriktigeOpplysninger =
                sporsmal.findByTag(SporsmalTag.URIKTIGE_OPPLYSNINGER)?.let(::konverterUriktigeOpplysninger),
            arbeidsgiverOrgnummer =
                sporsmal.findByTag(SporsmalTag.ARBEIDSGIVER_ORGNUMMER)?.let(::konverterArbeidsgiverOrgnummer),
            riktigNarmesteLeder =
                sporsmal.findByTag(SporsmalTag.RIKTIG_NARMESTE_LEDER)?.let(::konverterRiktigNarmesteLeder),
            harBruktEgenmelding =
                sporsmal.findByTag(SporsmalTag.HAR_BRUKT_EGENMELDING)?.let(::konverterHarBruktEgenmelding),
            egenmeldingsperioder =
                sporsmal.findByTag(SporsmalTag.EGENMELDINGSPERIODER)?.let(::konverterEgenmeldingsperioder),
            harForsikring =
                sporsmal.findByTag(SporsmalTag.HAR_FORSIKRING)?.let(::konverterHarForsikring),
            egenmeldingsdager =
                sporsmal.findByTag(SporsmalTag.EGENMELDINGSDAGER)?.let(::konverterEgenmeldingsdager),
            harBruktEgenmeldingsdager =
                sporsmal.findByTag(SporsmalTag.HAR_BRUKT_EGENMELDINGSDAGER)?.let(::konverterHarBruktEgenmeldingsdager),
            fisker =
                sporsmal.findByTag(SporsmalTag.FISKER)?.let(::konverterFisker),
        )

    private fun Iterable<Sporsmal>.findByTag(tag: SporsmalTag): Sporsmal? = this.find { it.tag == tag }

    private fun Sporsmal.konverterTilJaEllerNei(): JaEllerNei {
        require(this.svartype == Svartype.JA_NEI) { "Kan kun konvertere JA_NEI spørsmål" }
        val svarVerdi = this.forsteSvarVerdi
        requireNotNull(svarVerdi) { "Må ha et svar" }
        return when (svarVerdi) {
            "JA" -> JaEllerNei.JA
            "NEI" -> JaEllerNei.NEI
            else -> throw IllegalArgumentException("Ukjent JA_NEI svar: $svarVerdi")
        }
    }

    private fun konverterArbeidssituasjon(sporsmal: Sporsmal): FormSporsmalSvar<ArbeidssituasjonDTO> {
        val svarVerdi = sporsmal.forsteSvarVerdi
        requireNotNull(svarVerdi) { "Må ha et svar" }

        val svarArbeidssituasjon =
            try {
                enumValueOf<Arbeidssituasjon>(svarVerdi)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Ukjent arbeidssituasjon: $svarVerdi")
            }
        val arbeidssituasjon =
            when (svarArbeidssituasjon) {
                Arbeidssituasjon.ARBEIDSTAKER -> ArbeidssituasjonDTO.ARBEIDSTAKER
                Arbeidssituasjon.FRILANSER -> ArbeidssituasjonDTO.FRILANSER
                Arbeidssituasjon.ARBEIDSLEDIG -> ArbeidssituasjonDTO.ARBEIDSLEDIG
                Arbeidssituasjon.ANNET -> ArbeidssituasjonDTO.ANNET
                // TODO: Skal vi støtte PERMITTERT?
                Arbeidssituasjon.PERMITTERT -> ArbeidssituasjonDTO.ARBEIDSLEDIG
                Arbeidssituasjon.FISKER -> ArbeidssituasjonDTO.FISKER
                Arbeidssituasjon.NAERINGSDRIVENDE -> ArbeidssituasjonDTO.NAERINGSDRIVENDE
                Arbeidssituasjon.JORDBRUKER -> ArbeidssituasjonDTO.JORDBRUKER
            }

        return FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = arbeidssituasjon,
        )
    }

    private fun konverterUriktigeOpplysninger(sporsmal: Sporsmal): FormSporsmalSvar<List<UriktigeOpplysningerType>> {
        val svar =
            if (sporsmal.svar.isEmpty()) {
                emptyList()
            } else {
                sporsmal.svar.map { s ->
                    try {
                        enumValueOf<UriktigeOpplysningerType>(s.verdi)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException("Ukjent Uriktig Opplysning: ${s.verdi}")
                    }
                }
            }

        return FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = svar,
        )
    }

    private fun konverterArbeidsgiverOrgnummer(sporsmal: Sporsmal): FormSporsmalSvar<String> {
        val svarVerdi = sporsmal.forsteSvarVerdi
        requireNotNull(svarVerdi) { "Må ha et svar" }
        return FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = svarVerdi,
        )
    }

    private fun konverterRiktigNarmesteLeder(sporsmal: Sporsmal): FormSporsmalSvar<JaEllerNei> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = sporsmal.konverterTilJaEllerNei(),
        )

    private fun konverterHarBruktEgenmelding(sporsmal: Sporsmal): FormSporsmalSvar<JaEllerNei> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = sporsmal.konverterTilJaEllerNei(),
        )

    private fun konverterEgenmeldingsperioder(sporsmal: Sporsmal): FormSporsmalSvar<List<EgenmeldingsperiodeFormDTO>> {
        val perioder = sporsmal.perioderSvar().map { EgenmeldingsperiodeFormDTO(it.fom, it.tom) }
        return FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = perioder,
        )
    }

    private fun konverterHarForsikring(sporsmal: Sporsmal): FormSporsmalSvar<JaEllerNei> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = sporsmal.konverterTilJaEllerNei(),
        )

    private fun konverterEgenmeldingsdager(sporsmal: Sporsmal): FormSporsmalSvar<List<LocalDate>> {
        val dager = sporsmal.svar.map { LocalDate.parse(it.verdi) }
        return FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = dager,
        )
    }

    private fun konverterHarBruktEgenmeldingsdager(sporsmal: Sporsmal): FormSporsmalSvar<JaEllerNei> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = sporsmal.konverterTilJaEllerNei(),
        )

    private fun konverterFisker(sporsmal: Sporsmal): FiskereSvarKafkaDTO {
        val blad =
            sporsmal.undersporsmal.findByTag(SporsmalTag.FISKER__BLAD)?.let { s ->
                FormSporsmalSvar(
                    sporsmaltekst = s.konverterSporsmalstekst(),
                    svar = enumValueOf<Blad>(s.forsteSvarVerdi!!),
                )
            } ?: throw IllegalArgumentException("Mangler spørsmål om blad")

        val lottOgHyre =
            sporsmal.undersporsmal.findByTag(SporsmalTag.FISKER__LOTT_OG_HYRE)?.let { s ->
                FormSporsmalSvar(
                    sporsmaltekst = s.konverterSporsmalstekst(),
                    svar = enumValueOf<LottOgHyre>(s.forsteSvarVerdi!!),
                )
            } ?: throw IllegalArgumentException("Mangler spørsmål om lott og hyre")

        return FiskereSvarKafkaDTO(blad = blad, lottOgHyre = lottOgHyre)
    }

    private fun Sporsmal.konverterSporsmalstekst(): String {
        // TODO: kreves når vi får spørsmåltekst fra frontend
//        requireNotNull(this.sporsmalstekst)
        return this.sporsmalstekst ?: ""
    }
}
