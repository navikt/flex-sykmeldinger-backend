package no.nav.helse.flex.tsmsykmeldingstatus

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.tsmsykmeldingstatus.dto.*
import no.nav.helse.flex.utils.objectMapper
import java.time.LocalDate

object StatusSporsmalListeKonverterer {
    private val DEFAULT_BRUKER_SVAR =
        BrukerSvarKafkaDTO(
            erOpplysningeneRiktige = lagErOpplysningeneRiktigeSporsmal(svar = JaEllerNei.JA),
            arbeidssituasjon =
                FormSporsmalSvar(
                    sporsmaltekst = "Jeg er sykmeldt som",
                    svar = ArbeidssituasjonDTO.ANNET,
                ),
            uriktigeOpplysninger = null,
            arbeidsgiverOrgnummer = null,
            riktigNarmesteLeder = null,
            harBruktEgenmelding = null,
            egenmeldingsperioder = null,
            harForsikring = null,
            egenmeldingsdager = null,
            harBruktEgenmeldingsdager = null,
            fisker = null,
        )

    fun konverterSporsmalTilBrukerSvar(
        sporsmal: List<SporsmalKafkaDTO>,
        hendelseStatus: HendelseStatus = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
        arbeidsgiver: ArbeidsgiverStatusKafkaDTO? = null,
    ): BrukerSvarKafkaDTO? {
        if (sporsmal.isEmpty()) {
            return if (hendelseStatus in setOf(HendelseStatus.SENDT_TIL_ARBEIDSGIVER, HendelseStatus.SENDT_TIL_NAV)) {
                DEFAULT_BRUKER_SVAR
            } else {
                null
            }
        }
        return BrukerSvarKafkaDTO(
            // Dersom bruker har sendt inn, har brukeren bekreftet at opplysningene er riktige
            erOpplysningeneRiktige = lagErOpplysningeneRiktigeSporsmal(svar = JaEllerNei.JA),
            uriktigeOpplysninger = null,
            arbeidssituasjon =
                konverterArbeidssituasjonTilArbeidssituasjon(sporsmal)
                    ?: throw IllegalArgumentException("Arbeidssituasjon er påkrevd i bruker svar, men ikke funnet i sporsmal"),
            arbeidsgiverOrgnummer =
                arbeidsgiver?.let {
                    lagArbeidsgiverOrgnummerSporsmal(svar = it.orgnummer)
                },
            riktigNarmesteLeder = konverterNyNarmesteLederTilRiktigNarmesteLeder(sporsmal),
            harBruktEgenmelding = konverterFravaerTilHarBruktEgenmelding(sporsmal),
            egenmeldingsperioder = konverterPerioderTilEgenmeldingsperioder(sporsmal),
            harForsikring = konverterForsikringTilHarForsikring(sporsmal),
            harBruktEgenmeldingsdager = null,
            egenmeldingsdager = konverterEgenmeldingsdagerTilEgenmeldingsdager(sporsmal),
            fisker = null,
        )
    }

    internal fun lagErOpplysningeneRiktigeSporsmal(svar: JaEllerNei): FormSporsmalSvar<JaEllerNei> =
        FormSporsmalSvar(
            sporsmaltekst = "Stemmer opplysningene?",
            svar = svar,
        )

    internal fun lagArbeidsgiverOrgnummerSporsmal(svar: String): FormSporsmalSvar<String> =
        FormSporsmalSvar(
            sporsmaltekst = "Velg arbeidsgiver",
            svar = svar,
        )

    internal fun konverterArbeidssituasjonTilArbeidssituasjon(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<ArbeidssituasjonDTO>? {
        val originaltArbeidssituasjonSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.ARBEIDSSITUASJON }
                ?: return null

        require(originaltArbeidssituasjonSporsmal.svartype == SvartypeKafkaDTO.ARBEIDSSITUASJON) {
            "Ugyldig svartype for ARBEIDSSITUASJON: ${originaltArbeidssituasjonSporsmal.svartype}"
        }

        val arbeidssituasjon = ArbeidssituasjonDTO.valueOf(originaltArbeidssituasjonSporsmal.svar)

        val gyldigeArbeidssituasjoner =
            setOf(
                ArbeidssituasjonDTO.ARBEIDSTAKER,
                ArbeidssituasjonDTO.FRILANSER,
                ArbeidssituasjonDTO.NAERINGSDRIVENDE,
                ArbeidssituasjonDTO.ARBEIDSLEDIG,
                ArbeidssituasjonDTO.PERMITTERT,
                ArbeidssituasjonDTO.ANNET,
            )
        require(
            arbeidssituasjon in gyldigeArbeidssituasjoner,
        ) { "Ugyldig arbeidssituasjon i sporsmal liste: $arbeidssituasjon. Forventer en av: $gyldigeArbeidssituasjoner" }

        return FormSporsmalSvar(
            sporsmaltekst = originaltArbeidssituasjonSporsmal.tekst,
            svar = arbeidssituasjon,
        )
    }

    internal fun konverterNyNarmesteLederTilRiktigNarmesteLeder(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<JaEllerNei>? {
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

        require(originaltNyNarmesteLederSporsmal.svartype == SvartypeKafkaDTO.JA_NEI) {
            "Ugyldig svartype for NY_NARMESTE_LEDER: ${originaltNyNarmesteLederSporsmal.svartype}"
        }

        val originaltSvar: JaEllerNei = konverterTilJaEllerNei(originaltNyNarmesteLederSporsmal.svar)

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

    internal fun konverterPerioderTilEgenmeldingsperioder(
        sporsmal: List<SporsmalKafkaDTO>,
    ): FormSporsmalSvar<List<EgenmeldingsperiodeFormDTO>>? {
        val originaltSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.PERIODE }
                ?: return null

        require(originaltSporsmal.svartype == SvartypeKafkaDTO.PERIODER) {
            "Ugyldig svartype for PERIODE: ${originaltSporsmal.svartype}"
        }

        val svarSerialisert = originaltSporsmal.svar
        val svarDeserialisert: List<EgenmeldingsperiodeKafkaDTO> = objectMapper.readValue(svarSerialisert)
        val svar = svarDeserialisert.map { it.tilEgenmeldingsperiodeFormDTO() }

        return FormSporsmalSvar(
            sporsmaltekst = originaltSporsmal.tekst,
            svar = svar,
        )
    }

    internal fun konverterFravaerTilHarBruktEgenmelding(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<JaEllerNei>? {
        val originaltSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.FRAVAER }
                ?: return null

        require(originaltSporsmal.svartype == SvartypeKafkaDTO.JA_NEI) {
            "Ugyldig svartype for FRAVAER: ${originaltSporsmal.svartype}"
        }

        val svar = konverterTilJaEllerNei(originaltSporsmal.svar)

        return FormSporsmalSvar(
            sporsmaltekst = originaltSporsmal.tekst,
            svar = svar,
        )
    }

    internal fun konverterEgenmeldingsdagerTilEgenmeldingsdager(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<List<LocalDate>>? {
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

    internal fun konverterForsikringTilHarForsikring(sporsmal: List<SporsmalKafkaDTO>): FormSporsmalSvar<JaEllerNei>? {
        val originaltSporsmal =
            sporsmal.firstOrNull { it.shortName == ShortNameKafkaDTO.FORSIKRING }
                ?: return null

        require(originaltSporsmal.svartype == SvartypeKafkaDTO.JA_NEI) {
            "Ugyldig svartype for FORSIKRING: ${originaltSporsmal.svartype}"
        }

        return FormSporsmalSvar(
            sporsmaltekst = originaltSporsmal.tekst,
            svar = konverterTilJaEllerNei(originaltSporsmal.svar),
        )
    }

    internal fun konverterTilJaEllerNei(svar: String): JaEllerNei =
        when (svar) {
            "JA" -> JaEllerNei.JA
            "NEI" -> JaEllerNei.NEI
            else -> throw IllegalArgumentException("Ugyldig JaEllerNei svar: $svar")
        }
}
