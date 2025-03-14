package no.nav.helse.flex.producers.sykmeldingstatus

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
                    SporsmalSvarKafkaDTO(
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

    private fun Sporsmal.konverterTilJaEllerNei(): JaEllerNeiKafkaDTO {
        require(this.svartype == Svartype.JA_NEI) { "Kan kun konvertere JA_NEI spørsmål" }
        val svarVerdi = this.forsteSvarVerdi
        requireNotNull(svarVerdi) { "Må ha et svar" }
        return when (svarVerdi) {
            "JA" -> JaEllerNeiKafkaDTO.JA
            "NEI" -> JaEllerNeiKafkaDTO.NEI
            else -> throw IllegalArgumentException("Ukjent JA_NEI svar: $svarVerdi")
        }
    }

    private fun konverterArbeidssituasjon(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<ArbeidssituasjonKafkaDTO> {
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
                Arbeidssituasjon.ARBEIDSTAKER -> ArbeidssituasjonKafkaDTO.ARBEIDSTAKER
                Arbeidssituasjon.FRILANSER -> ArbeidssituasjonKafkaDTO.FRILANSER
                Arbeidssituasjon.ARBEIDSLEDIG -> ArbeidssituasjonKafkaDTO.ARBEIDSLEDIG
                Arbeidssituasjon.ANNET -> ArbeidssituasjonKafkaDTO.ANNET
                // TODO: Skal vi støtte PERMITTERT?
                Arbeidssituasjon.PERMITTERT -> ArbeidssituasjonKafkaDTO.ARBEIDSLEDIG
                Arbeidssituasjon.FISKER -> ArbeidssituasjonKafkaDTO.FISKER
                Arbeidssituasjon.NAERINGSDRIVENDE -> ArbeidssituasjonKafkaDTO.NAERINGSDRIVENDE
                Arbeidssituasjon.JORDBRUKER -> ArbeidssituasjonKafkaDTO.JORDBRUKER
            }

        return SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = arbeidssituasjon,
        )
    }

    private fun konverterUriktigeOpplysninger(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<List<UriktigeOpplysningerTypeKafkaDTO>> {
        val svar =
            if (sporsmal.svar.isEmpty()) {
                emptyList()
            } else {
                sporsmal.svar.map { s ->
                    try {
                        enumValueOf<UriktigeOpplysningerTypeKafkaDTO>(s.verdi)
                    } catch (e: IllegalArgumentException) {
                        throw IllegalArgumentException("Ukjent Uriktig Opplysning: ${s.verdi}")
                    }
                }
            }

        return SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = svar,
        )
    }

    private fun konverterArbeidsgiverOrgnummer(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<String> {
        val svarVerdi = sporsmal.forsteSvarVerdi
        requireNotNull(svarVerdi) { "Må ha et svar" }
        return SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = svarVerdi,
        )
    }

    private fun konverterRiktigNarmesteLeder(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO> =
        SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = sporsmal.konverterTilJaEllerNei(),
        )

    private fun konverterHarBruktEgenmelding(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO> =
        SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = sporsmal.konverterTilJaEllerNei(),
        )

    private fun konverterEgenmeldingsperioder(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<List<EgenmeldingsperiodeKafkaDTO>> {
        val perioder = sporsmal.perioderSvar().map { EgenmeldingsperiodeKafkaDTO(it.fom, it.tom) }
        return SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = perioder,
        )
    }

    private fun konverterHarForsikring(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO> =
        SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = sporsmal.konverterTilJaEllerNei(),
        )

    private fun konverterEgenmeldingsdager(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<List<LocalDate>> {
        val dager = sporsmal.svar.map { LocalDate.parse(it.verdi) }
        return SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = dager,
        )
    }

    private fun konverterHarBruktEgenmeldingsdager(sporsmal: Sporsmal): SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO> =
        SporsmalSvarKafkaDTO(
            sporsmaltekst = sporsmal.konverterSporsmalstekst(),
            svar = sporsmal.konverterTilJaEllerNei(),
        )

    private fun konverterFisker(sporsmal: Sporsmal): FiskereSvarKafkaDTO {
        val blad =
            sporsmal.undersporsmal.findByTag(SporsmalTag.FISKER__BLAD)?.let { s ->
                SporsmalSvarKafkaDTO(
                    sporsmaltekst = s.konverterSporsmalstekst(),
                    svar = enumValueOf<BladKafkaDTO>(s.forsteSvarVerdi!!),
                )
            } ?: throw IllegalArgumentException("Mangler spørsmål om blad")

        val lottOgHyre =
            sporsmal.undersporsmal.findByTag(SporsmalTag.FISKER__LOTT_OG_HYRE)?.let { s ->
                SporsmalSvarKafkaDTO(
                    sporsmaltekst = s.konverterSporsmalstekst(),
                    svar = enumValueOf<LottOgHyreKafkaDTO>(s.forsteSvarVerdi!!),
                )
            } ?: throw IllegalArgumentException("Mangler spørsmål om lott og hyre")

        return FiskereSvarKafkaDTO(blad = blad, lottOgHyre = lottOgHyre)
    }

    private fun Sporsmal.konverterSporsmalstekst(): String {
        requireNotNull(this.sporsmalstekst)
        return this.sporsmalstekst
    }
}
