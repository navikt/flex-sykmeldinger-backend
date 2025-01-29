package no.nav.helse.flex.sykmelding.api

import SykmeldingDtoKonverterer
import no.nav.helse.flex.sykmelding.api.dto.AnnenFraverGrunnDTO
import no.nav.helse.flex.sykmelding.api.dto.AnnenFraversArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsgiverDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsrelatertArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsrelatertArsakTypeDTO
import no.nav.helse.flex.sykmelding.api.dto.DiagnoseDTO
import no.nav.helse.flex.sykmelding.api.dto.KontaktMedPasientDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskArsakTypeDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskVurderingDTO
import no.nav.helse.flex.sykmelding.api.dto.PasientDTO
import no.nav.helse.flex.sykmelding.api.dto.SporsmalSvarDTO
import no.nav.helse.flex.sykmelding.api.dto.SvarRestriksjonDTO
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingStatusDTO
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.AktivitetIkkeMulig
import no.nav.helse.flex.sykmelding.domain.ArbeidsrelatertArsak
import no.nav.helse.flex.sykmelding.domain.ArbeidsrelatertArsakType
import no.nav.helse.flex.sykmelding.domain.EnArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.FlereArbeidsgivere
import no.nav.helse.flex.sykmelding.domain.IngenArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.MedisinskArsak
import no.nav.helse.flex.sykmelding.domain.MedisinskArsakType
import no.nav.helse.flex.sykmelding.domain.Navn
import no.nav.helse.flex.sykmelding.domain.Pasient
import no.nav.helse.flex.sykmelding.domain.SporsmalSvar
import no.nav.helse.flex.sykmelding.domain.SvarRestriksjon
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

class SykmeldingDtoKonvertererTest {
    @Test
    fun `burde konvertere med riktig id`() {
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                statuser =
                    listOf(
                        SykmeldingStatus(
                            status = StatusEvent.APEN,
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
    fun `burde konvertere arbeidsgiver, en arbeidsgiver har ikke info`() {
        val konverterer = SykmeldingDtoKonverterer()

        konverterer.konverterArbeidsgiver(
            EnArbeidsgiver(
                meldingTilArbeidsgiver = "_",
                tiltakArbeidsplassen = "_",
            ),
        ) `should be equal to`
            ArbeidsgiverDTO(
                navn = null,
                stillingsprosent = null,
            )
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
        val konverterer = SykmeldingDtoKonverterer()

        konverterer.konverterArbeidsgiver(IngenArbeidsgiver()) `should be equal to` null
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
    fun `burde konvertere status NY`() {
        val status =
            SykmeldingStatus(
                status = StatusEvent.APEN,
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                sporsmalSvar =
                    PGobject().apply {
                        type = "json"
                        value = ""
                    },
            )

        val forventetStatus =
            SykmeldingStatusDTO(
                statusEvent = "APEN",
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00.00Z"),
                arbeidsgiver = null,
                sporsmalOgSvarListe = emptyList(),
                brukerSvar = null,
            )

        val konverterer = SykmeldingDtoKonverterer()

        konverterer.konverterSykmeldingStatus(status) `should be equal to` forventetStatus
    }

    @Test
    fun `burde konvertere medisinsk vurdering`() {
        val konverterer = SykmeldingDtoKonverterer()

        val medisinskVurdering =
            MedisinskVurdering(
                hovedDiagnose =
                    DiagnoseInfo(
                        system = DiagnoseSystem.ICD10,
                        kode = "kode",
                    ),
                biDiagnoser =
                    listOf(
                        DiagnoseInfo(
                            system = DiagnoseSystem.ICPC2,
                            kode = "bi diagnose",
                        ),
                    ),
                svangerskap = true,
                yrkesskade =
                    Yrkesskade(
                        yrkesskadeDato = LocalDate.parse("2021-01-01"),
                    ),
                skjermetForPasient = false,
                syketilfelletStartDato = LocalDate.parse("2021-01-01"),
                annenFraversArsak =
                    AnnenFraverArsak(
                        beskrivelse = "beskrivelse",
                        arsak = listOf(AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON),
                    ),
            )

        val konvertertMedisinskVurdering =
            konverterer.konverterMedisinskVurdering(medisinskVurdering) `should be equal to`
                MedisinskVurderingDTO(
                    hovedDiagnose =
                        DiagnoseDTO(
                            kode = "kode",
                            system = "ICD10",
                            // TODO
                            tekst = null,
                        ),
                    biDiagnoser =
                        listOf(
                            DiagnoseDTO(
                                kode = "bi diagnose",
                                system = "ICPC2",
                                // TODO
                                tekst = null,
                            ),
                        ),
                    annenFraversArsak =
                        AnnenFraversArsakDTO(
                            beskrivelse = "beskrivelse",
                            grunn = listOf(AnnenFraverGrunnDTO.GODKJENT_HELSEINSTITUSJON),
                        ),
                    svangerskap = true,
                    yrkesskade = true,
                    yrkesskadeDato = LocalDate.parse("2021-01-01"),
                )

        konverterer.konverterMedisinskVurdering(medisinskVurdering) `should be equal to` konvertertMedisinskVurdering
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

    @Test
    fun `burde konvertere tiltak arbeidsplassen`() {
        val konverterer = SykmeldingDtoKonverterer()

        konverterer
            .konverterTiltakArbeidsplassen(
                EnArbeidsgiver(
                    meldingTilArbeidsgiver = "_",
                    tiltakArbeidsplassen = "tiltak",
                ),
            ).shouldBeEqualTo("tiltak")

        konverterer.konverterTiltakArbeidsplassen(
            FlereArbeidsgivere(
                meldingTilArbeidsgiver = "_",
                tiltakArbeidsplassen = "tiltak",
                navn = "_",
                yrkesbetegnelse = "_",
                stillingsprosent = 0,
            ),
        ) `should be equal to` "tiltak"

        konverterer.konverterTiltakArbeidsplassen(
            IngenArbeidsgiver(),
        ) `should be equal to` null
    }

    @Test
    fun `burde konvertere kontakt med pasient, ingen kontakt`() {
        val konverterer = SykmeldingDtoKonverterer()
        konverterer.konverterKontaktMedPasient() `should be equal to`
            KontaktMedPasientDTO(
                kontaktDato = null,
                begrunnelseIkkeKontakt = null,
            )
    }

    @Nested
    inner class UtdypendeOpplysningerTest {
        private val konverterer = SykmeldingDtoKonverterer()

        @Test
        fun `burde håndtere én opplysning`() {
            konverterer.konverterUtdypendeOpplysninger(
                mapOf(
                    "a" to
                        mapOf(
                            "b" to
                                SporsmalSvar(
                                    sporsmal = "spørsmål",
                                    svar = "svar",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                ),
            ) `should be equal to`
                mapOf(
                    "a" to
                        mapOf(
                            "b" to
                                SporsmalSvarDTO(
                                    sporsmal = "spørsmål",
                                    svar = "svar",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                )
        }

        @Test
        fun `burde håndtere restriksjoner`() {
            konverterer.konverterUtdypendeOpplysninger(
                mapOf(
                    "a" to
                        mapOf(
                            "b" to
                                SporsmalSvar(
                                    sporsmal = "",
                                    svar = "",
                                    restriksjoner =
                                        listOf(
                                            SvarRestriksjon.SKJERMET_FOR_NAV,
                                            SvarRestriksjon.SKJERMET_FOR_PASIENT,
                                            SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER,
                                        ),
                                ),
                        ),
                ),
            ) `should be equal to`
                mapOf(
                    "a" to
                        mapOf(
                            "b" to
                                SporsmalSvarDTO(
                                    sporsmal = "",
                                    svar = "",
                                    restriksjoner =
                                        listOf(
                                            SvarRestriksjonDTO.SKJERMET_FOR_NAV,
                                            SvarRestriksjonDTO.SKJERMET_FOR_PASIENT,
                                            SvarRestriksjonDTO.SKJERMET_FOR_ARBEIDSGIVER,
                                        ),
                                ),
                        ),
                )
        }

        @Test
        fun `burde håndtere ingen opplysning`() {
            konverterer.konverterUtdypendeOpplysninger(null) `should be equal to` emptyMap()
            konverterer.konverterUtdypendeOpplysninger(emptyMap()) `should be equal to` emptyMap()
        }

        @Test
        fun `burde håndtere flere opplysningskategorier`() {
            konverterer.konverterUtdypendeOpplysninger(
                mapOf(
                    "a" to
                        mapOf(
                            "a1" to
                                SporsmalSvar(
                                    sporsmal = "spørsmål a1",
                                    svar = "svar a1",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                    "b" to
                        mapOf(
                            "b1" to
                                SporsmalSvar(
                                    sporsmal = "spørsmål b1",
                                    svar = "svar b1",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                ),
            ) `should be equal to`
                mapOf(
                    "a" to
                        mapOf(
                            "a1" to
                                SporsmalSvarDTO(
                                    sporsmal = "spørsmål a1",
                                    svar = "svar a1",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                    "b" to
                        mapOf(
                            "b1" to
                                SporsmalSvarDTO(
                                    sporsmal = "spørsmål b1",
                                    svar = "svar b1",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                )
        }
    }
}
