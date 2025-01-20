package no.nav.helse.flex.sykmelding.api

import SykmeldingDtoKonverterer
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsgiverDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsrelatertArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsrelatertArsakTypeDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskArsakTypeDTO
import no.nav.helse.flex.sykmelding.api.dto.PasientDTO
import no.nav.helse.flex.sykmelding.domain.AktivitetIkkeMulig
import no.nav.helse.flex.sykmelding.domain.ArbeidsrelatertArsak
import no.nav.helse.flex.sykmelding.domain.ArbeidsrelatertArsakType
import no.nav.helse.flex.sykmelding.domain.EnArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.MedisinskArsak
import no.nav.helse.flex.sykmelding.domain.MedisinskArsakType
import no.nav.helse.flex.sykmelding.domain.FlereArbeidsgivere
import no.nav.helse.flex.sykmelding.domain.IngenArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.Navn
import no.nav.helse.flex.sykmelding.domain.Pasient
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class SykmeldingDtoKonvertererTest {
    @Test
    fun `burde konvertere`() {
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                statuser =
                    listOf(
                        SykmeldingStatus(
                            status = "NY",
                            opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                        ),
                    ),
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                oppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
            )

        val konverterer = SykmeldingDtoKonverterer()

        val dto = konverterer.konverter(sykmelding)
        dto.id `should be equal to` "1"
    }

    @Test
    fun `burde konvertere pasient`() {
        val pasient =
            Pasient(
                navn = Navn("fornavn", "mellomnavn", "etternavn"),
                navKontor = null,
                navnFastlege = null,
                fnr = "fnr",
                kontaktinfo = emptyList(),
            )

        val konverterer = SykmeldingDtoKonverterer()

        val pasientDto = konverterer.konverterPasient(pasient)
        pasientDto `should be equal to`
            PasientDTO(
                fnr = "fnr",
                fornavn = "fornavn",
                mellomnavn = "mellomnavn",
                etternavn = "etternavn",
                overSyttiAar = null,
            )
    }

    @Test
    fun `burde konvertere arbeidsgiver, en arbeidsgiver`() {
        val arbeidsgiver =
            EnArbeidsgiver(
                meldingTilArbeidsgiver = "melding",
                tiltakArbeidsplassen = "tiltak",
            )

        val forventetArbeidsgiver =
            ArbeidsgiverDTO(
                navn = null,
                stillingsprosent = null,
            )

        val konverterer = SykmeldingDtoKonverterer()

        konverterer.konverterArbeidsgiver(arbeidsgiver) `should be equal to` forventetArbeidsgiver

        error("TODO")
    }

    @Test
    fun `burde konvertere arbeidsgiver, flere arbeidsgiver`() {
        val arbeidsgiver =
            FlereArbeidsgivere(
                navn = "Navn",
                yrkesbetegnelse = "_",
                stillingsprosent = 50,
                meldingTilArbeidsgiver = "_",
                tiltakArbeidsplassen = "_",
            )

        val forventetArbeidsgiver =
            ArbeidsgiverDTO(
                navn = "Navn",
                stillingsprosent = 50,
            )

        val konverterer = SykmeldingDtoKonverterer()

        konverterer.konverterArbeidsgiver(arbeidsgiver) `should be equal to` forventetArbeidsgiver
    }

    @Test
    fun `burde konvertere arbeidsgiver, ingen arbeidsgiver`() {
        val arbeidsgiver =
            IngenArbeidsgiver()

        val forventetArbeidsgiver = null

        val konverterer = SykmeldingDtoKonverterer()

        konverterer.konverterArbeidsgiver(arbeidsgiver) `should be equal to` forventetArbeidsgiver
    }

    @Test
    fun `burde konvertere aktivitet til en periode`() {
        val konverterer = SykmeldingDtoKonverterer()

        val aktivitet =
            AktivitetIkkeMulig(
                medisinskArsak =
                    MedisinskArsak(
                        beskrivelse = "",
                        arsak = MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING,
                    ),
                arbeidsrelatertArsak =
                    ArbeidsrelatertArsak(
                        beskrivelse = "",
                        arsak = ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING,
                    ),
                fom = LocalDate.parse("2021-01-01"),
                tom = LocalDate.parse("2021-01-21"),
            )

        val periode = konverterer.konverterSykmeldingsperiode(aktivitet)
        periode.fom `should be equal to` LocalDate.parse("2021-01-01")
        periode.tom `should be equal to` LocalDate.parse("2021-01-21")
        periode.aktivitetIkkeMulig?.let {
            it.medisinskArsak `should be equal to`
                MedisinskArsakDTO(
                    beskrivelse = "",
                    arsak = listOf(MedisinskArsakTypeDTO.AKTIVITET_FORHINDRER_BEDRING),
                )
            it.arbeidsrelatertArsak `should be equal to`
                ArbeidsrelatertArsakDTO(
                    beskrivelse = "",
                    arsak = listOf(ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING),
                )
        }
    }
}
