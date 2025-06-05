package no.nav.helse.flex.tsmsykmeldingstatus

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.BrukerSvarKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.ShortNameKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SporsmalKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SvartypeKafkaDTO
import no.nav.helse.flex.utils.objectMapper
import java.time.LocalDate

object StatusSporsmalListeKonverterer {
    //    val erOpplysningeneRiktige: FormSporsmalSvar<JaEllerNei>,
//    val uriktigeOpplysninger: FormSporsmalSvar<List<UriktigeOpplysningerType>>?,
//    val arbeidssituasjon: FormSporsmalSvar<ArbeidssituasjonDTO>,
//    val arbeidsgiverOrgnummer: FormSporsmalSvar<String>?,
//    val riktigNarmesteLeder: FormSporsmalSvar<JaEllerNei>?,
//    val harBruktEgenmelding: FormSporsmalSvar<JaEllerNei>?,
//    val egenmeldingsperioder: FormSporsmalSvar<List<EgenmeldingsperiodeFormDTO>>?,
//    val harForsikring: FormSporsmalSvar<JaEllerNei>?,
//    val egenmeldingsdager: FormSporsmalSvar<List<LocalDate>>?,
//    val harBruktEgenmeldingsdager: FormSporsmalSvar<JaEllerNei>?,
//    val fisker: FiskereSvarKafkaDTO?,

    fun konverterSporsmalTilBrukerSvar(sporsmal: List<SporsmalKafkaDTO>): BrukerSvarKafkaDTO? {
        if (sporsmal.isEmpty()) return null

        return BrukerSvarKafkaDTO(
            erOpplysningeneRiktige = lagErOpplysningeneRiktige(),
            uriktigeOpplysninger = null,
            arbeidssituasjon =
                konverterTilArbeidssituasjon(sporsmal)
                    ?: throw IllegalArgumentException("Arbeidssituasjon er påkrevd i bruker svar, men ikke funnet i sporsmal"),
            // TODO
            arbeidsgiverOrgnummer = null,
            riktigNarmesteLeder = konverterTilRiktigNarmesteLeder(sporsmal),
            // TODO
            harBruktEgenmelding = null,
            egenmeldingsperioder = konverterTilEgenmeldingsperioder(sporsmal),
            // TODO
            harForsikring = null,
            egenmeldingsdager = konverterTilEgenmeldingsdager(sporsmal),
            harBruktEgenmeldingsdager = konverterTilHarBruktEgenmeldingsdager(sporsmal),
            // Fisker finnes ikke i tidligere representasjon
            fisker = null,
        )
    }

    internal fun lagErOpplysningeneRiktige(): FormSporsmalSvar<JaEllerNei> {
        // Dersom bruker har sendt inn, har brukeren bekreftet at opplysningene er riktige
        return FormSporsmalSvar(
            // TODO: Blir det feil å si at brukeren har svart på dette spørsmålet? Kan vi evt sette spørsmålstekst til tom streng?
            sporsmaltekst = "Er opplysningene riktige?",
            svar = JaEllerNei.JA,
        )
    }

    internal fun konverterTilArbeidssituasjon(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<ArbeidssituasjonDTO>? {
        val originaltArbeidssituasjonSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.ARBEIDSSITUASJON }
                ?: return null

        val arbeidssituasjon =
            when (originaltArbeidssituasjonSporsmal.svar) {
                "ARBEIDSTAKER" -> ArbeidssituasjonDTO.ARBEIDSTAKER
                "FRILANSER" -> ArbeidssituasjonDTO.FRILANSER
                "NAERINGSDRIVENDE" -> ArbeidssituasjonDTO.NAERINGSDRIVENDE
                "ARBEIDSLEDIG" -> ArbeidssituasjonDTO.ARBEIDSLEDIG
                "PERMITTERT" -> ArbeidssituasjonDTO.PERMITTERT
                "ANNET" -> ArbeidssituasjonDTO.ANNET
                else -> throw IllegalArgumentException("Ugyldig arbeidssituasjon: ${originaltArbeidssituasjonSporsmal.svar}")
            }

        return FormSporsmalSvar(
            sporsmaltekst = originaltArbeidssituasjonSporsmal.tekst,
            svar = arbeidssituasjon,
        )
    }

    internal fun konverterTilRiktigNarmesteLeder(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<JaEllerNei>? {
        // Spørsmålshistorikk:
        //   - 29.04.2020 (ved opprettelse): Tekst "Skal finne ny nærmeste leder".decf3
        //       JA betyr at bruker ikke har riktig nærmeste leder.
        //   - 27.05.2021 (ved oppdatering): Tekst "Er det Navn Navnesen som skal følge deg opp på jobben mens du er syk?".
        //       Svar blir endret JA->NEI, NEI->JA, for å beholde original konsistens, selv om spørsmålsteksten er endret.
        //
        // Vi flipper JA/NEI, og beholder sporsmalsteksten. Da vil 2020 utgaven bli feil formulert, men 2021 utgaven vil se riktig ut.

        val originaltNyNarmesteLederSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.NY_NARMESTE_LEDER }
                ?: return null

        val originaltSvar: JaEllerNei =
            when (originaltNyNarmesteLederSporsmal.svar) {
                "JA" -> JaEllerNei.JA
                "NEI" -> JaEllerNei.NEI
                else -> throw IllegalArgumentException("Ugyldig svar på narmeste leder: ${originaltNyNarmesteLederSporsmal.svar}")
            }

        val flippetSvar =
            when (originaltSvar) {
                JaEllerNei.JA -> JaEllerNei.NEI
                JaEllerNei.NEI -> JaEllerNei.JA
            }

        return FormSporsmalSvar(
            sporsmaltekst = originaltNyNarmesteLederSporsmal.tekst,
            svar = flippetSvar,
        )
    }

    data class EgenmeldingsperiodeKafkaDTO(
        val fom: LocalDate,
        val tom: LocalDate,
    ) {
        fun tilEgenmeldingsperiodeFormDTO() = EgenmeldingsperiodeFormDTO(fom = fom, tom = tom)
    }

    internal fun konverterTilEgenmeldingsperioder(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<List<EgenmeldingsperiodeFormDTO>>? {
        val originaltSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.PERIODE }
                ?: return null

        val svarSerialisert = originaltSporsmal.svar
        val svarDeserialisert: List<EgenmeldingsperiodeKafkaDTO> = objectMapper.readValue(svarSerialisert)
        val svar = svarDeserialisert.map { it.tilEgenmeldingsperiodeFormDTO() }

        return FormSporsmalSvar(
            sporsmaltekst = originaltSporsmal.tekst,
            svar = svar,
        )
    }

    internal fun konverterTilHarBruktEgenmeldingsdager(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<JaEllerNei>? {
        val originaltSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.FRAVAER }
                ?: return null

        val svar = konverterTilJaEllerNei(originaltSporsmal.svar)

        return FormSporsmalSvar(
            sporsmaltekst = originaltSporsmal.tekst,
            svar = svar,
        )
    }

    internal fun konverterTilEgenmeldingsdager(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<List<LocalDate>>? {
        val originaltSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.EGENMELDINGSDAGER }
                ?: return null

        require(originaltSporsmal.svartype == SvartypeKafkaDTO.DAGER) {
            "Ugyldig svartype for EGENMELDINGSDAGER: ${originaltSporsmal.svartype}"
        }

        val svar: List<LocalDate> = objectMapper.readValue(originaltSporsmal.svar)

        return FormSporsmalSvar(
            sporsmaltekst = originaltSporsmal.tekst,
            svar = svar,
        )
    }

    internal fun konverterTilJaEllerNei(svar: String): JaEllerNei =
        when (svar) {
            "JA" -> JaEllerNei.JA
            "NEI" -> JaEllerNei.NEI
            else -> throw IllegalArgumentException("Ugyldig JaEllerNei svar: $svar")
        }

//    internal fun konverterSporsmalTilBrukerSvar(sporsmal: List<SporsmalKafkaDTO>): BrukerSvar? {
//        if (sporsmal.isEmpty()) return null
//
//        val originaltArbeidssituasjonSporsmal =
//            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.ARBEIDSSITUASJON }
//                ?: throw IllegalArgumentException("Arbeidssituasjon er påkrevd i bruker svar")
//
//        val originaltNyNarmesteLederSporsmal =
//            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.NY_NARMESTE_LEDER }
//
//        val originaltFravaerSporsmal =
//            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.FRAVAER }
//
//        val originaltPeriodeSporsmal =
//            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.PERIODE }
//
//        val originaltForsikringSporsmal =
//            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.FORSIKRING }
//
//        val originaltEgenmeldingsdagerSporsmal =
//            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.EGENMELDINGSDAGER }
//
// //        ARBEIDSSITUASJON,
// //        NY_NARMESTE_LEDER,
// //        FRAVAER,
// //        PERIODE,
// //        FORSIKRING,
// //        EGENMELDINGSDAGER,
//
//        val arbeidssituasjon =
//            when (originaltArbeidssituasjonSporsmal.svar) {
//                "ARBEIDSTAKER" -> Arbeidssituasjon.ARBEIDSTAKER
//                "FRILANSER" -> Arbeidssituasjon.FRILANSER
//                "NAERINGSDRIVENDE" -> Arbeidssituasjon.NAERINGSDRIVENDE
//                "ARBEIDSLEDIG" -> Arbeidssituasjon.ARBEIDSLEDIG
//                "PERMITTERT" -> Arbeidssituasjon.PERMITTERT
//                "ANNET" -> Arbeidssituasjon.ANNET
//                else -> throw IllegalArgumentException("Ugyldig arbeidssituasjon: ${arbeidssituasjonSporsmal.svar}")
//            }
//
//        val arbeidssituasjonSporsmal =
//            SporsmalSvar(
//                sporsmaltekst = originaltArbeidssituasjonSporsmal.tekst,
//                svar = arbeidssituasjon,
//            )
//        val erOpplysningeneRiktige = SporsmalSvar(sporsmaltekst = "", svar = true)
//        val uriktigeOpplysninger = null
//        // TODO: Hvordan håndtere nærmeste leder?
//        val riktigNarmesteLeder =
//            originaltNyNarmesteLederSporsmal?.let {
//                SporsmalSvar(
//                    sporsmaltekst = it.tekst,
//                    svar =
//                        when (it.svar) {
//                            "JA" -> true
//                            "NEI" -> false
//                            else -> throw IllegalArgumentException("Ugyldig svar på narmeste leder: ${it.svar}")
//                        },
//                )
//            }
//        val egenmeldingsdager
//
//        return when (arbeidssituasjon) {
//            Arbeidssituasjon.ARBEIDSTAKER ->
//                ArbeidstakerBrukerSvar(
//                    erOpplysningeneRiktige = erOpplysningeneRiktige,
//                    arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
//                    uriktigeOpplysninger = uriktigeOpplysninger,
//                    arbeidsgiverOrgnummer =
//                        SporsmalSvar(
//                            sporsmaltekst = "",
//                            svar = "",
//                        ),
//                )
//            Arbeidssituasjon.FRILANSER ->
//                FrilanserBrukerSvar(
//                    erOpplysningeneRiktige = SporsmalSvar(sporsmaltekst = "", svar = true),
//                    arbeidssituasjonSporsmal =
//                        SporsmalSvar(
//                            sporsmaltekst = arbeidssituasjonSporsmal.tekst,
//                            svar = arbeidssituasjon,
//                        ),
//                )
//            Arbeidssituasjon.NAERINGSDRIVENDE -> TODO()
//            Arbeidssituasjon.ARBEIDSLEDIG -> TODO()
//            Arbeidssituasjon.PERMITTERT -> TODO()
//            Arbeidssituasjon.ANNET -> TODO()
//            else -> throw IllegalArgumentException("Ugyldig arbeidssituasjon: $arbeidssituasjon")
//        }
//    }
}
