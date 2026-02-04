package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.MedisinskVurderingDTO
import no.nav.helse.flex.api.dto.MeldingTilNavDTO
import no.nav.helse.flex.api.dto.SporsmalSvarDTO
import no.nav.helse.flex.testdata.lagSykmeldingDto
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class SykmeldingDtoReglerTest {
    @Test
    fun `burde ikke skjermes for pasient dersom ikke spesifisert`() {
        val sykmeldingDto =
            lagSykmeldingDto(
                skjermesForPasient = false,
                medisinskVurdering = VILKARLIG_MEDISINSK_VURDERING,
                utdypendeOpplysninger = VILKARLIG_UTDYPENDE_OPPLYSNINGER,
                tiltakNAV = "tiltak nav",
                andreTiltak = "andre tiltak",
                meldingTilNAV = VILKARLIG_MELDING_TIL_NAV,
            )
        val resultat = SykmeldingDtoRegler.skjermForPasientDersomSpesifisert(sykmeldingDto)
        resultat.run {
            medisinskVurdering.shouldNotBeNull()
            utdypendeOpplysninger.shouldNotBeEmpty()
            tiltakNAV.shouldNotBeNull()
            andreTiltak.shouldNotBeNull()
            meldingTilNAV.shouldNotBeNull()
        }
    }

    @Test
    fun `burde skermes for pasient dersom spesifisert`() {
        val sykmeldingDto =
            lagSykmeldingDto(
                skjermesForPasient = true,
                medisinskVurdering = VILKARLIG_MEDISINSK_VURDERING,
                utdypendeOpplysninger = VILKARLIG_UTDYPENDE_OPPLYSNINGER,
                tiltakNAV = "tiltak nav",
                andreTiltak = "andre tiltak",
                meldingTilNAV = VILKARLIG_MELDING_TIL_NAV,
            )
        val resultat = SykmeldingDtoRegler.skjermForPasientDersomSpesifisert(sykmeldingDto)
        resultat.run {
            medisinskVurdering.shouldBeNull()
            utdypendeOpplysninger.shouldBeEmpty()
            tiltakNAV.shouldBeNull()
            andreTiltak.shouldBeNull()
            meldingTilNAV.shouldBeNull()
        }
    }
}

val VILKARLIG_MEDISINSK_VURDERING =
    MedisinskVurderingDTO(
        hovedDiagnose = null,
        biDiagnoser = emptyList(),
        annenFraversArsak = null,
        svangerskap = false,
        yrkesskade = false,
        yrkesskadeDato = null,
    )

val VILKARLIG_UTDYPENDE_OPPLYSNINGER =
    mapOf(
        "1" to mapOf("1.1" to SporsmalSvarDTO(sporsmal = null, svar = "JA", restriksjoner = emptyList())),
    )

val VILKARLIG_MELDING_TIL_NAV =
    MeldingTilNavDTO(
        bistandUmiddelbart = true,
        beskrivBistand = "beskrivelse",
    )
