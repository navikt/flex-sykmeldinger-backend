package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.application.Arbeidssituasjon
import no.nav.helse.flex.sykmelding.domain.*
import org.springframework.stereotype.Component

@Component
class SykmeldingStatusKafkaDTOKonverterer {
    fun konverter(sykmelding: Sykmelding): SykmeldingStatusKafkaDTO =
        SykmeldingStatusKafkaDTO(
            sykmeldingId = sykmelding.sykmeldingId,
            timestamp = sykmelding.sisteStatus().opprettet.tilNorgeOffsetDateTime(),
            statusEvent =
                when (sykmelding.sisteStatus().status) {
                    HendelseStatus.APEN -> StatusEventKafkaDTO.APEN
                    HendelseStatus.AVBRUTT -> StatusEventKafkaDTO.AVBRUTT
                    HendelseStatus.SENDT_TIL_NAV -> StatusEventKafkaDTO.BEKREFTET
                    HendelseStatus.SENDT_TIL_ARBEIDSGIVER -> StatusEventKafkaDTO.SENDT
                    HendelseStatus.BEKREFTET_AVVIST -> StatusEventKafkaDTO.BEKREFTET
                    HendelseStatus.UTGATT -> StatusEventKafkaDTO.UTGATT
                },
            arbeidsgiver = null,
            // For bakoverkompatabilitet. Consumere burde bruke `brukerSvar`
            sporsmals = null,
            brukerSvar = sykmelding.sisteStatus().sporsmalSvar?.let(::konverterTilBrukerSvar),
            erSvarOppdatering = null,
            tidligereArbeidsgiver = null,
        )

    private fun konverterTilBrukerSvar(sporsmal: List<Sporsmal>): BrukerSvarKafkaDTO =
        BrukerSvarKafkaDTO(
            erOpplysningeneRiktige =
                sporsmal.find { it.tag == SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE }?.let { s ->
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = s.konverterSporsmalstekst(),
                        svar = s.konverterTilJaEllerNei(),
                    )
                } ?: throw IllegalArgumentException("Mangler spørsmål om opplysningene er riktige"),
            arbeidssituasjon =
                sporsmal.find { it.tag == SporsmalTag.ARBEIDSSITUASJON }?.let { s ->
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = s.konverterSporsmalstekst(),
                        svar = s.konverterArbeidssituasjon(),
                    )
                } ?: throw IllegalArgumentException("Mangler spørsmål om arbeidssituasjon"),
            uriktigeOpplysninger =
                sporsmal.find { it.tag == SporsmalTag.URIKTIGE_OPPLYSNINGER }?.let(::konverterUriktigeOpplysninger)
                    ?: throw IllegalArgumentException("Mangler spørsmål om arbeidssituasjon"),
            arbeidsgiverOrgnummer = null,
            riktigNarmesteLeder = null,
            harBruktEgenmelding = null,
            egenmeldingsperioder = null,
            harForsikring = null,
            egenmeldingsdager = null,
            harBruktEgenmeldingsdager = null,
            fisker = null,
        )

    private fun Sporsmal.konverterSporsmalstekst(): String {
        requireNotNull(this.sporsmalstekst)
        return this.sporsmalstekst
    }

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

    private fun Sporsmal.konverterArbeidssituasjon(): ArbeidssituasjonKafkaDTO {
        val svarVerdi = this.forsteSvarVerdi
        requireNotNull(svarVerdi) { "Må ha et svar" }

        val svarArbeidssituasjon =
            try {
                enumValueOf<Arbeidssituasjon>(svarVerdi)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Ukjent arbeidssituasjon: $svarVerdi")
            }
        return when (svarArbeidssituasjon) {
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
}
