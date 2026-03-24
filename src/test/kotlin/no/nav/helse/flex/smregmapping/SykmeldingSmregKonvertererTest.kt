package no.nav.helse.flex.smregmapping

import no.nav.helse.flex.sykmelding.tsm.ArbeidsrelatertArsak
import no.nav.helse.flex.sykmelding.tsm.ArbeidsrelatertArsakType
import no.nav.helse.flex.sykmelding.tsm.AvsenderSystem
import no.nav.helse.flex.sykmelding.tsm.AvsenderSystemNavn
import no.nav.helse.flex.sykmelding.tsm.Reason
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmelding.tsm.ValidationResult
import no.nav.helse.flex.sykmelding.tsm.values.Kontaktinfo
import no.nav.helse.flex.sykmelding.tsm.values.KontaktinfoType
import no.nav.helse.flex.sykmelding.tsm.values.Navn
import no.nav.helse.flex.sykmelding.tsm.values.PersonId
import no.nav.helse.flex.sykmelding.tsm.values.PersonIdType
import no.nav.helse.flex.testdata.lagAddresse
import no.nav.helse.flex.testdata.lagAktivitetAvventende
import no.nav.helse.flex.testdata.lagAktivitetBehandlingsdager
import no.nav.helse.flex.testdata.lagAktivitetGradert
import no.nav.helse.flex.testdata.lagAktivitetIkkeMulig
import no.nav.helse.flex.testdata.lagAktivitetReisetilskudd
import no.nav.helse.flex.testdata.lagArbeidsgiverInfoEnArbeidsgiver
import no.nav.helse.flex.testdata.lagBehandler
import no.nav.helse.flex.testdata.lagIkkeDigitalMedisinskVurdering
import no.nav.helse.flex.testdata.lagNorskSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagPrognose
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingMetadata
import no.nav.helse.flex.testdata.lagTilbakedatering
import no.nav.helse.flex.testdata.lagUtenlandskInfo
import no.nav.helse.flex.testdata.lagUtenlandskSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagValidationInvalidRule
import no.nav.helse.flex.testdata.lagValidationOkRule
import no.nav.helse.flex.testdata.lagValidationPendingRule
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

private val OFFSET_DATE_TIME_1 = OffsetDateTime.parse("2021-01-01T00:00:00.00Z")
private val OFFSET_DATE_TIME_2 = OffsetDateTime.parse("2022-01-01T00:00:00.00Z")
private val OFFSET_DATE_TIME_3 = OffsetDateTime.parse("2023-01-01T00:00:00.00Z")
private val OFFSET_DATE_TIME_4 = OffsetDateTime.parse("2024-01-01T00:00:00.00Z")

private val LOCAL_DATE_1 = LocalDate.parse("2020-01-01")
private val LOCAL_DATE_2 = LocalDate.parse("2020-01-02")

class SykmeldingSmregKonvertererTest {
    @Test
    fun `burde konvertere sykmelding med maksimal data`() {
        val sykmelding =
            lagNorskSykmeldingGrunnlag(
                id = "id-1",
                metadata =
                    lagSykmeldingMetadata(
                        mottattDato = OFFSET_DATE_TIME_1,
                        genDato = OFFSET_DATE_TIME_2,
                        behandletTidspunkt = OFFSET_DATE_TIME_3,
                    ),
                medisinskVurdering =
                    lagIkkeDigitalMedisinskVurdering(
                        syketilfelleStartDato = LOCAL_DATE_1,
                    ),
                tilbakedatering = lagTilbakedatering(kontaktDato = LOCAL_DATE_2),
                aktiviteter = listOf(lagAktivitetIkkeMulig()),
                arbeidsgiver =
                    lagArbeidsgiverInfoEnArbeidsgiver(
                        meldingTilArbeidsgiver = "melding til arbeidsgiver 1",
                        tiltakArbeidsplassen = "tiltak arbeidsplassen 1",
                        navn = "arbeidsgiver navn 1",
                        yrkesbetegnelse = "yrkesbetegnelse 1",
                    ),
                prognose =
                    lagPrognose(
                        arbeidsforEtterPeriode = true,
                        hensynArbeidsplassen = "hensyn arbeidsplassen 1",
                    ),
                behandler =
                    lagBehandler(
                        navn =
                            Navn(
                                fornavn = "fornavn 1",
                                mellomnavn = "mellomnavn 1",
                                etternavn = "etternavn 1",
                            ),
                    ),
            )

        val result = SykmeldingSmregKonverterer.konverterSykmelding(sykmeldingGrunnlag = sykmelding)

        result.run {
            id shouldBeEqualTo "id-1"
            mottattTidspunkt shouldBeEqualTo OFFSET_DATE_TIME_1
            syketilfelleStartDato shouldBeEqualTo LOCAL_DATE_1
            signaturDato shouldBeEqualTo OFFSET_DATE_TIME_2
            behandletTidspunkt shouldBeEqualTo OFFSET_DATE_TIME_3
            egenmeldt shouldBeEqualTo false
            papirsykmelding shouldBeEqualTo false
            harRedusertArbeidsgiverperiode shouldBeEqualTo false
            tiltakArbeidsplassen shouldBeEqualTo "tiltak arbeidsplassen 1"
            meldingTilArbeidsgiver shouldBeEqualTo "melding til arbeidsgiver 1"
            kontaktMedPasient shouldBeEqualTo
                KontaktMedPasientSmregDto(
                    kontaktDato = LOCAL_DATE_2,
                )

            arbeidsgiver shouldBeEqualTo
                ArbeidsgiverSmregDto(
                    navn = "arbeidsgiver navn 1",
                    yrkesbetegnelse = "yrkesbetegnelse 1",
                )

            prognose.shouldNotBeNull() shouldBeEqualTo
                PrognoseSmregDto(
                    arbeidsforEtterPeriode = true,
                    hensynArbeidsplassen = "hensyn arbeidsplassen 1",
                )
            behandler.shouldNotBeNull()
            utenlandskSykmelding.shouldBeNull()
            sykmeldingsperioder shouldHaveSize 1
            merknader.shouldBeNull()
        }
    }

    @Test
    fun `burde konvertere sykmelding med minimal data`() {
        val sykmelding =
            lagNorskSykmeldingGrunnlag(
                id = "id-2",
                metadata =
                    lagSykmeldingMetadata(
                        mottattDato = OFFSET_DATE_TIME_2,
                        genDato = OFFSET_DATE_TIME_1,
                        behandletTidspunkt = null,
                    ),
                medisinskVurdering =
                    lagIkkeDigitalMedisinskVurdering(
                        syketilfelleStartDato = null,
                    ),
                tilbakedatering = null,
                aktiviteter = emptyList(),
                arbeidsgiver =
                    lagArbeidsgiverInfoEnArbeidsgiver(
                        meldingTilArbeidsgiver = null,
                        tiltakArbeidsplassen = null,
                        navn = null,
                        yrkesbetegnelse = null,
                    ),
                prognose = null,
                behandler = lagBehandler(),
            )

        val result = SykmeldingSmregKonverterer.konverterSykmelding(sykmeldingGrunnlag = sykmelding)

        result.run {
            id shouldBeEqualTo "id-2"
            mottattTidspunkt shouldBeEqualTo OFFSET_DATE_TIME_2
            syketilfelleStartDato.shouldBeNull()
            signaturDato shouldBeEqualTo OFFSET_DATE_TIME_1
            behandletTidspunkt shouldBeEqualTo OFFSET_DATE_TIME_1
            egenmeldt shouldBeEqualTo false
            papirsykmelding shouldBeEqualTo false
            harRedusertArbeidsgiverperiode shouldBeEqualTo false
            tiltakArbeidsplassen.shouldBeNull()
            meldingTilArbeidsgiver.shouldBeNull()
            kontaktMedPasient shouldBeEqualTo KontaktMedPasientSmregDto(kontaktDato = null)
            arbeidsgiver shouldBeEqualTo ArbeidsgiverSmregDto(navn = null, yrkesbetegnelse = null)
            prognose.shouldBeNull()
            behandler.shouldNotBeNull()
            utenlandskSykmelding.shouldBeNull()
            sykmeldingsperioder.shouldBeEmpty()
            merknader.shouldBeNull()
        }
    }

    @Test
    fun `burde konvertere egenmeldt`() {
        val konvertert =
            SykmeldingSmregKonverterer
                .konverterSykmelding(
                    lagNorskSykmeldingGrunnlag(
                        metadata =
                            lagSykmeldingMetadata(
                                avsenderSystem =
                                    AvsenderSystem(
                                        navn = AvsenderSystemNavn.EGENMELDT,
                                        versjon = "",
                                    ),
                            ),
                    ),
                )
        konvertert.egenmeldt.shouldBeTrue()
    }

    @Test
    fun `burde ha utenlandsk sykmelding`() {
        val sykmelding =
            lagUtenlandskSykmeldingGrunnlag(
                utenlandskInfo = lagUtenlandskInfo(land = "land 1"),
            )
        val konvertert = SykmeldingSmregKonverterer.konverterSykmelding(sykmelding)
        konvertert.utenlandskSykmelding.shouldNotBeNull().land shouldBeEqualTo "land 1"
    }

    @Test
    fun `burde ha med merknader for validation rules`() {
        val sykmelding = lagSykmeldingGrunnlag()
        val validation =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OFFSET_DATE_TIME_1,
                rules =
                    listOf(
                        lagValidationPendingRule(),
                        lagValidationInvalidRule(),
                    ),
            )
        val konvertert = SykmeldingSmregKonverterer.konverterSykmelding(sykmeldingGrunnlag = sykmelding, validation = validation)
        konvertert.merknader.shouldNotBeNull() shouldHaveSize 2
    }

    @Test
    fun `test konverter sykmeldingsperiode fra aktivitet ikke mulig`() {
        val aktivitet =
            lagAktivitetIkkeMulig(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                arbeidsrelatertArsak =
                    ArbeidsrelatertArsak(
                        beskrivelse = "arbeidsrelatert årsak 1",
                        arsak = listOf(ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING),
                    ),
            )
        val konvertert = SykmeldingSmregKonverterer.konverterSykmeldingsperiode(aktivitet)
        konvertert shouldBeEqualTo
            SykmeldingsperiodeSmregDto(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                gradert = null,
                behandlingsdager = null,
                innspillTilArbeidsgiver = null,
                type = PeriodetypeSmregDto.AKTIVITET_IKKE_MULIG,
                aktivitetIkkeMulig =
                    AktivitetIkkeMuligSmregDto(
                        arbeidsrelatertArsak =
                            ArbeidsrelatertArsakSmregDto(
                                beskrivelse = "arbeidsrelatert årsak 1",
                                arsak = listOf(ArbeidsrelatertArsakTypeSmregDto.MANGLENDE_TILRETTELEGGING),
                            ),
                    ),
                reisetilskudd = false,
            )
    }

    @Test
    fun `test konverter sykmeldingsperiode fra aktivitet behandlingsdager`() {
        val aktivitet =
            lagAktivitetBehandlingsdager(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                antallBehandlingsdager = 10,
            )
        val konvertert = SykmeldingSmregKonverterer.konverterSykmeldingsperiode(aktivitet)
        konvertert shouldBeEqualTo
            SykmeldingsperiodeSmregDto(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                gradert = null,
                behandlingsdager = 10,
                innspillTilArbeidsgiver = null,
                type = PeriodetypeSmregDto.BEHANDLINGSDAGER,
                aktivitetIkkeMulig = null,
                reisetilskudd = false,
            )
    }

    @Test
    fun `test konverter sykmeldingsperiode fra aktivitet gradert`() {
        val aktivitet =
            lagAktivitetGradert(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                grad = 10,
                reisetilskudd = true,
            )
        val konvertert = SykmeldingSmregKonverterer.konverterSykmeldingsperiode(aktivitet)
        konvertert shouldBeEqualTo
            SykmeldingsperiodeSmregDto(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                gradert =
                    GradertSmregDto(
                        grad = 10,
                        reisetilskudd = true,
                    ),
                behandlingsdager = null,
                innspillTilArbeidsgiver = null,
                type = PeriodetypeSmregDto.GRADERT,
                aktivitetIkkeMulig = null,
                reisetilskudd = false,
            )
    }

    @Test
    fun `test konverter sykmeldingsperiode fra aktivitet reisetilskudd`() {
        val aktivitet =
            lagAktivitetReisetilskudd(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
            )
        val konvertert = SykmeldingSmregKonverterer.konverterSykmeldingsperiode(aktivitet)
        konvertert shouldBeEqualTo
            SykmeldingsperiodeSmregDto(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                gradert = null,
                behandlingsdager = null,
                innspillTilArbeidsgiver = null,
                type = PeriodetypeSmregDto.REISETILSKUDD,
                aktivitetIkkeMulig = null,
                reisetilskudd = true,
            )
    }

    @Test
    fun `test konverter sykmeldingsperiode fra aktivitet avventende`() {
        val aktivitet =
            lagAktivitetAvventende(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                innspillTilArbeidsgiver = "innspill 1",
            )
        val konvertert = SykmeldingSmregKonverterer.konverterSykmeldingsperiode(aktivitet)
        konvertert shouldBeEqualTo
            SykmeldingsperiodeSmregDto(
                fom = LOCAL_DATE_1,
                tom = LOCAL_DATE_2,
                gradert = null,
                behandlingsdager = null,
                innspillTilArbeidsgiver = "innspill 1",
                type = PeriodetypeSmregDto.AVVENTENDE,
                aktivitetIkkeMulig = null,
                reisetilskudd = false,
            )
    }

    @Test
    fun `test konverter merknader maksimal`() {
        val validationRules =
            listOf(
                lagValidationPendingRule(
                    name = "TILBAKEDATERING_UNDER_BEHANDLING",
                    reason =
                        Reason(
                            sykmeldt = "pending begrunnelse",
                            sykmelder = "",
                        ),
                ),
                lagValidationInvalidRule(
                    name = "TILBAKEDATERING_UGYLDIG_TILBAKEDATERING",
                    reason =
                        Reason(
                            sykmeldt = "invalid begrunnelse",
                            sykmelder = "",
                        ),
                ),
                lagValidationOkRule(
                    name = "TILBAKEDATERING_DELVIS_GODKJENT",
                ),
            )
        val merknader = SykmeldingSmregKonverterer.konverterMerknader(validationRules)
        merknader.shouldHaveSize(3)
        merknader[0] shouldBeEqualTo
            MerknadSmregDto(
                type = MerknadtypeSmregDto.UNDER_BEHANDLING.name,
                beskrivelse = "pending begrunnelse",
            )
        merknader[1] shouldBeEqualTo
            MerknadSmregDto(
                type = MerknadtypeSmregDto.UGYLDIG_TILBAKEDATERING.name,
                beskrivelse = "invalid begrunnelse",
            )
        merknader[2] shouldBeEqualTo
            MerknadSmregDto(
                type = MerknadtypeSmregDto.DELVIS_GODKJENT.name,
                beskrivelse = null,
            )
    }

    @Test
    fun `test konverter merknader minimal`() {}

    @Test
    fun `test konverter behandler maksimal`() {
        SykmeldingSmregKonverterer.konverterBehandler(
            lagBehandler(
                navn =
                    Navn(
                        fornavn = "fornavn 1",
                        mellomnavn = "mellomnavn 1",
                        etternavn = "etternavn 1",
                    ),
                adresse =
                    lagAddresse(
                        gateadresse = "gate 1",
                        postnummer = "1",
                        kommune = "kommune 1",
                        postboks = "postboks 1",
                        land = "land 1",
                    ),
                ids = listOf(PersonId(type = PersonIdType.HPR, id = "hpr-1")),
                kontaktinfo = listOf(Kontaktinfo(type = KontaktinfoType.TLF, value = "tlf-1")),
            ),
        ) shouldBeEqualTo
            BehandlerSmregDto(
                fornavn = "fornavn 1",
                mellomnavn = "mellomnavn 1",
                etternavn = "etternavn 1",
                hpr = "hpr-1",
                adresse =
                    AdresseSmregDto(
                        gate = "gate 1",
                        postnummer = 1,
                        kommune = "kommune 1",
                        postboks = "postboks 1",
                        land = "land 1",
                    ),
                tlf = "tlf-1",
            )
    }

    @Test
    fun `test konverter behandler minimal`() {
        SykmeldingSmregKonverterer.konverterBehandler(
            lagBehandler(
                navn =
                    Navn(
                        fornavn = "fornavn 1",
                        mellomnavn = null,
                        etternavn = "etternavn 1",
                    ),
                adresse = null,
                ids = emptyList(),
                kontaktinfo = emptyList(),
            ),
        ) shouldBeEqualTo
            BehandlerSmregDto(
                fornavn = "fornavn 1",
                mellomnavn = null,
                etternavn = "etternavn 1",
                hpr = null,
                adresse =
                    AdresseSmregDto(
                        gate = null,
                        postnummer = null,
                        kommune = null,
                        postboks = null,
                        land = null,
                    ),
                tlf = null,
            )
    }
}
