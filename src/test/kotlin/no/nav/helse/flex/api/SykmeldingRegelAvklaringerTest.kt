package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.PeriodetypeDTO
import no.nav.helse.flex.api.dto.SykmeldingsperiodeDTO
import no.nav.helse.flex.sykmelding.tsm.AnnenFravarArsakType
import no.nav.helse.flex.sykmelding.tsm.AnnenFraverArsak
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagIkkeDigitalMedisinskVurdering
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SykmeldingRegelAvklaringerTest : FakesTestOppsett() {
    @Autowired
    private lateinit var sykmeldingRegelAvklaringer: SykmeldingRegelAvklaringer

    @Autowired
    lateinit var sykmeldingDtoKonverterer: SykmeldingDtoKonverterer

    @Test
    fun `burde konvertere harRedusertArbeidsgiverperiode innenfor covid periode`() {
        val medisinskVurdering =
            sykmeldingDtoKonverterer
                .konverterMedisinskVurdering(
                    lagIkkeDigitalMedisinskVurdering(
                        hovedDiagnoseKode = "R991",
                        annenFraverArsak =
                            AnnenFraverArsak(
                                beskrivelse = "beskrivelse",
                                arsak = listOf(AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON),
                            ),
                    ),
                )

        sykmeldingRegelAvklaringer
            .harRedusertArbeidsgiverperiode(
                hovedDiagnose = medisinskVurdering.hovedDiagnose,
                biDiagnoser = medisinskVurdering.biDiagnoser,
                sykmeldingsperioder =
                    listOf(
                        SykmeldingsperiodeDTO(
                            fom = LocalDate.parse("2021-01-01"),
                            tom = LocalDate.parse("2021-01-31"),
                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                            reisetilskudd = false,
                            gradert = null,
                            behandlingsdager = null,
                            innspillTilArbeidsgiver = null,
                            aktivitetIkkeMulig = null,
                        ),
                    ),
                annenFraversArsakDTO = medisinskVurdering.annenFraversArsak,
            ).`should be true`()
    }

    @Test
    fun `burde konvertere harRedusertArbeidsgiverperiode utenfor covid periode`() {
        val medisinskVurdering =
            sykmeldingDtoKonverterer
                .konverterMedisinskVurdering(
                    lagIkkeDigitalMedisinskVurdering(
                        hovedDiagnoseKode = "R991",
                        annenFraverArsak =
                            AnnenFraverArsak(
                                beskrivelse = "beskrivelse",
                                arsak = listOf(AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON),
                            ),
                    ),
                )

        sykmeldingRegelAvklaringer
            .harRedusertArbeidsgiverperiode(
                hovedDiagnose = medisinskVurdering.hovedDiagnose,
                biDiagnoser = medisinskVurdering.biDiagnoser,
                sykmeldingsperioder =
                    listOf(
                        SykmeldingsperiodeDTO(
                            fom = LocalDate.parse("2024-01-01"),
                            tom = LocalDate.parse("2024-01-31"),
                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                            reisetilskudd = false,
                            gradert = null,
                            behandlingsdager = null,
                            innspillTilArbeidsgiver = null,
                            aktivitetIkkeMulig = null,
                        ),
                    ),
                annenFraversArsakDTO = medisinskVurdering.annenFraversArsak,
            ).`should be false`()
    }
}
