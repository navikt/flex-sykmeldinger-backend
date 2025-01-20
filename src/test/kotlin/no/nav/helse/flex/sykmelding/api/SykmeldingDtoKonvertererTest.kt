package no.nav.helse.flex.sykmelding.api

import SykmeldingDtoKonverterer
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsgiverDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsgiverStatusDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsledigFraOrgnummer
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsrelatertArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsrelatertArsakTypeDTO
import no.nav.helse.flex.sykmelding.api.dto.Arbeidssituasjon
import no.nav.helse.flex.sykmelding.api.dto.Egenmeldingsperiode
import no.nav.helse.flex.sykmelding.api.dto.JaEllerNei
import no.nav.helse.flex.sykmelding.api.dto.MedisinskArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskArsakTypeDTO
import no.nav.helse.flex.sykmelding.api.dto.PasientDTO
import no.nav.helse.flex.sykmelding.api.dto.ShortNameDTO
import no.nav.helse.flex.sykmelding.api.dto.SporsmalDTO
import no.nav.helse.flex.sykmelding.api.dto.SporsmalSvar
import no.nav.helse.flex.sykmelding.api.dto.SvarDTO
import no.nav.helse.flex.sykmelding.api.dto.SvartypeDTO
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingFormResponse
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingStatusDTO
import no.nav.helse.flex.sykmelding.api.dto.UriktigeOpplysningerType
import no.nav.helse.flex.sykmelding.domain.AktivitetIkkeMulig
import no.nav.helse.flex.sykmelding.domain.AnnenFraverArsak
import no.nav.helse.flex.sykmelding.domain.ArbeidsrelatertArsak
import no.nav.helse.flex.sykmelding.domain.ArbeidsrelatertArsakType
import no.nav.helse.flex.sykmelding.domain.Behandler
import no.nav.helse.flex.sykmelding.domain.BistandNav
import no.nav.helse.flex.sykmelding.domain.EnArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.ErIArbeid
import no.nav.helse.flex.sykmelding.domain.FlereArbeidsgivere
import no.nav.helse.flex.sykmelding.domain.IngenArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.Kontaktinfo
import no.nav.helse.flex.sykmelding.domain.KontaktinfoType
import no.nav.helse.flex.sykmelding.domain.MedisinskArsak
import no.nav.helse.flex.sykmelding.domain.MedisinskArsakType
import no.nav.helse.flex.sykmelding.domain.MedisinskVurdering
import no.nav.helse.flex.sykmelding.domain.Navn
import no.nav.helse.flex.sykmelding.domain.Pasient
import no.nav.helse.flex.sykmelding.domain.PersonId
import no.nav.helse.flex.sykmelding.domain.PersonIdType
import no.nav.helse.flex.sykmelding.domain.Prognose
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

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

    @Test
    fun `burde konvertere status`() {
        val status =
            SykmeldingStatus(
                status = "NY",
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                sporsmalSvar =
                    PGobject().apply {
                        type = "json"
                        value = ""
                    },
            )

        val forventetStatus =
            SykmeldingStatusDTO(
                statusEvent = "NY",
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00.00Z"),
                arbeidsgiver =
                    ArbeidsgiverStatusDTO(
                        orgnummer = "orgnr",
                        juridiskOrgnummer = "jur orgnr",
                        orgNavn = "Orgnavn",
                    ),
                sporsmalOgSvarListe =
                    listOf(
                        SporsmalDTO(
                            tekst = "JA/NEI spørsmål",
                            shortName = ShortNameDTO.NY_NARMESTE_LEDER,
                            svar =
                                SvarDTO(
                                    svarType = SvartypeDTO.JA_NEI,
                                    svar = "JA",
                                ),
                        ),
                    ),
                brukerSvar =
                    SykmeldingFormResponse(
                        erOpplysningeneRiktige = SporsmalSvar("Spørsmål", JaEllerNei.JA),
                        uriktigeOpplysninger = SporsmalSvar("", listOf(UriktigeOpplysningerType.ANDRE_OPPLYSNINGER)),
                        arbeidssituasjon = SporsmalSvar("", Arbeidssituasjon.ARBEIDSTAKER),
                        arbeidsgiverOrgnummer = SporsmalSvar("", "000"),
                        arbeidsledig =
                            ArbeidsledigFraOrgnummer(
                                arbeidsledigFraOrgnummer = SporsmalSvar("", "000"),
                            ),
                        riktigNarmesteLeder = SporsmalSvar("", JaEllerNei.JA),
                        harBruktEgenmelding = SporsmalSvar("", JaEllerNei.JA),
                        egenmeldingsperioder =
                            SporsmalSvar(
                                "",
                                listOf(
                                    Egenmeldingsperiode(
                                        fom = LocalDate.parse("2021-01-01"),
                                        tom = LocalDate.parse("2021-01-01"),
                                    ),
                                ),
                            ),
                        harForsikring = TODO(),
                        egenmeldingsdager = TODO(),
                        harBruktEgenmeldingsdager = TODO(),
                        fisker = TODO(),
                    ),
            )

        val konverterer = SykmeldingDtoKonverterer()

        konverterer.konverterSykmeldingStatus(status) `should be equal to` forventetStatus
    }

    @Test
    fun `burde konvertere medisinsk vurdering`() {
        val konverterer = SykmeldingDtoKonverterer()

        val medisinskVurdering =
            MedisinskVurdering(
                hovedDiagnose = TODO(),
                biDiagnoser = TODO(),
                svangerskap = TODO(),
                yrkesskade = TODO(),
                skjermetForPasient = TODO(),
                syketilfelletStartDato = TODO(),
                annenFraversArsak = TODO(),
            )

        val konvertertMedisinsk = konverterer.konverterMedisinskVurdering(medisinskVurdering)
    }

    @Test
    fun `burde konvertere annen fraværsårsak`() {
        val konverterer = SykmeldingDtoKonverterer()

        val annenFraverArsak =
            AnnenFraverArsak(
                beskrivelse = "",
                arsak = null,
            )

        val konvertertArsak = konverterer.konverterAnnenFraversArsak(annenFraverArsak)
        konvertertArsak?.grunn `should be equal to` emptyList()
    }

    @Test
    fun `burde konvertere prognose`() {
        val konverterer = SykmeldingDtoKonverterer()
        val prognose =
            Prognose(
                arbeidsforEtterPeriode = true,
                hensynArbeidsplassen = "",
                arbeid =
                    ErIArbeid(
                        egetArbeidPaSikt = true,
                        annetArbeidPaSikt = false,
                        arbeidFOM = LocalDate.parse("2021-01-01"),
                        vurderingsdato = LocalDate.parse("2021-01-01"),
                    ),
            )
        val konverterePrognose = konverterer.konverterPrognose(prognose)
        konverterePrognose.erIkkeIArbeid.`should be null`()
        konverterePrognose.erIArbeid?.egetArbeidPaSikt `should be` true
    }

    @Test
    fun `burde konvertere bistand Nav til melding til nav`() {
        val konverterer = SykmeldingDtoKonverterer()
        val bistandNav =
            BistandNav(
                bistandUmiddelbart = true,
                beskrivBistand = "",
            )
        val konvertertTilMelding = konverterer.konverterMeldingTilNAV(bistandNav)
        konvertertTilMelding.bistandUmiddelbart `should be` true
    }

    @Test
    fun `burde konvertere behandler`() {
        val konverterer = SykmeldingDtoKonverterer()
        val behandler =
            Behandler(
                navn =
                    Navn(
                        fornavn = "behandler",
                        mellomnavn = null,
                        etternavn = "",
                    ),
                adresse = null,
                ids =
                    listOf(
                        PersonId(
                            id = "",
                            type = PersonIdType.FNR,
                        ),
                    ),
                kontaktinfo =
                    listOf(
                        Kontaktinfo(
                            type = KontaktinfoType.TLF,
                            value = "",
                        ),
                    ),
            )

        val konvertereBehandler = konverterer.konverterBehandler(behandler)
        konvertereBehandler.fornavn `should be equal to` "behandler"
    }
}
