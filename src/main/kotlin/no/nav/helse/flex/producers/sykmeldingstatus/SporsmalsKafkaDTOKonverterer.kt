package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.domain.ArbeidstakerInfo
import no.nav.helse.flex.utils.objectMapper

class SporsmalsKafkaDTOKonverterer {
    fun konverterTilSporsmals(
        brukerSvar: BrukerSvarKafkaDTO,
        arbeidstakerInfo: ArbeidstakerInfo? = null,
        sykmeldingId: String? = null,
    ): List<SporsmalKafkaDTO> =
        listOfNotNull(
            brukerSvar.tilArbeidssituasjonSporsmal(),
            brukerSvar.tilFravarSporsmal(),
            brukerSvar.tilPeriodeSporsmal(),
            brukerSvar.tilRiktigNarmesteLederSporsmal(arbeidstakerInfo, sykmeldingId),
            brukerSvar.tilForsikringSporsmal(),
            brukerSvar.tilEgenmeldingsdager(),
        )

    private fun BrukerSvarKafkaDTO.tilEgenmeldingsdager(): SporsmalKafkaDTO? {
        if (egenmeldingsdager == null) return null

        return SporsmalKafkaDTO(
            tekst = egenmeldingsdager.sporsmaltekst,
            shortName = ShortNameKafkaDTO.EGENMELDINGSDAGER,
            svartype = SvartypeKafkaDTO.DAGER,
            svar = objectMapper.writeValueAsString(egenmeldingsdager.svar),
        )
    }

    private fun BrukerSvarKafkaDTO.tilArbeidssituasjonSporsmal(): SporsmalKafkaDTO {
        // In the old sporsmal and svar list, fiskere should be mapped to ARBEIDSTAKER or
        // NAERINGSDRIVENDE, dependening on whether or not they are working on lott or hyre.
        val normalisertSituasjon: ArbeidssituasjonKafkaDTO =
            when (arbeidssituasjon.svar) {
                ArbeidssituasjonKafkaDTO.FISKER -> {
                    val isHyre = fisker?.lottOgHyre?.svar == LottOgHyreKafkaDTO.HYRE

                    if (isHyre) ArbeidssituasjonKafkaDTO.ARBEIDSTAKER else ArbeidssituasjonKafkaDTO.NAERINGSDRIVENDE
                }
                ArbeidssituasjonKafkaDTO.JORDBRUKER -> ArbeidssituasjonKafkaDTO.NAERINGSDRIVENDE
                else -> arbeidssituasjon.svar
            }

        return SporsmalKafkaDTO(
            tekst = arbeidssituasjon.sporsmaltekst,
            shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
            svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
            svar = normalisertSituasjon.name,
        )
    }

    private fun BrukerSvarKafkaDTO.tilFravarSporsmal(): SporsmalKafkaDTO? {
        if (harBruktEgenmelding != null) {
            return SporsmalKafkaDTO(
                tekst = harBruktEgenmelding.sporsmaltekst,
                shortName = ShortNameKafkaDTO.FRAVAER,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar = harBruktEgenmelding.svar.name,
            )
        }
        return null
    }

    private fun BrukerSvarKafkaDTO.tilPeriodeSporsmal(): SporsmalKafkaDTO? {
        if (egenmeldingsperioder != null) {
            return SporsmalKafkaDTO(
                tekst = egenmeldingsperioder.sporsmaltekst,
                shortName = ShortNameKafkaDTO.PERIODE,
                svartype = SvartypeKafkaDTO.PERIODER,
                svar = objectMapper.writeValueAsString(egenmeldingsperioder.svar),
            )
        }
        return null
    }

    private fun BrukerSvarKafkaDTO.tilRiktigNarmesteLederSporsmal(
        arbeidstakerInfo: ArbeidstakerInfo?,
        sykmeldingId: String? = null,
    ): SporsmalKafkaDTO? {
        // TODO: Trenger erAktivtArbeidsforhold
//        if (arbeidstakerInfo != null && arbeidstakerInfo.arbeidsgiver.aktivtArbeidsforhold == false) {
//            logger.info(
//                "Ber ikke om ny nærmeste leder for arbeidsforhold som ikke er aktivt: $sykmeldingId",
//            )
//            return SporsmalKafkaDTO(
//                tekst = "Skal finne ny nærmeste leder",
//                shortName = ShortNameKafkaDTO.NY_NARMESTE_LEDER,
//                svartype = SvartypeKafkaDTO.JA_NEI,
//                svar = "NEI",
//            )
//        }

        if (riktigNarmesteLeder != null) {
            return SporsmalKafkaDTO(
                tekst = riktigNarmesteLeder.sporsmaltekst,
                shortName = ShortNameKafkaDTO.NY_NARMESTE_LEDER,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar =
                    when (riktigNarmesteLeder.svar) {
                        JaEllerNeiKafkaDTO.JA -> "JA"
                        JaEllerNeiKafkaDTO.NEI -> "NEI"
                    },
            )
        }
        return null
    }

    private fun BrukerSvarKafkaDTO.tilForsikringSporsmal(): SporsmalKafkaDTO? {
        if (harForsikring != null) {
            return SporsmalKafkaDTO(
                tekst = harForsikring.sporsmaltekst,
                shortName = ShortNameKafkaDTO.FORSIKRING,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar = harForsikring.svar.name,
            )
        }

        if (fisker?.blad?.svar == BladKafkaDTO.B && fisker.lottOgHyre.svar != LottOgHyreKafkaDTO.HYRE) {
            return SporsmalKafkaDTO(
                tekst = "Har du forsikring som gjelder for de første 16 dagene av sykefraværet?",
                shortName = ShortNameKafkaDTO.FORSIKRING,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar = "JA",
            )
        }

        return null
    }
}
