package no.nav.helse.flex.api.dto

import no.nav.helse.flex.sykmelding.application.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SendSykmeldingRequestDTOTest {
    @Test
    fun `burde mappe en arbeistaker`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER,
                arbeidsgiverOrgnummer = "orgnr",
                harEgenmeldingsdager = YesOrNo.YES,
                riktigNarmesteLeder = YesOrNo.YES,
                egenmeldingsdager = listOf(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-02")),
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarArbeidstaker()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe en frilanser`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.FRILANSER,
                egenmeldingsperioder =
                    listOf(
                        EgenmeldingsperiodeDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-02"),
                        ),
                    ),
                harBruktEgenmelding = YesOrNo.YES,
                harForsikring = YesOrNo.YES,
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarFrilanser()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe en selvstendig næringsdrivende`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.NAERINGSDRIVENDE,
                egenmeldingsperioder =
                    listOf(
                        EgenmeldingsperiodeDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-02"),
                        ),
                    ),
                harBruktEgenmelding = YesOrNo.YES,
                harForsikring = YesOrNo.YES,
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarSelvstendigNaringsdrivende()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe en fisker med lott`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.FISKER,
                egenmeldingsperioder =
                    listOf(
                        EgenmeldingsperiodeDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-02"),
                        ),
                    ),
                fisker =
                    Fisker(
                        blad = Blad.A,
                        lottOgHyre = LottOgHyre.LOTT,
                    ),
                harBruktEgenmelding = YesOrNo.YES,
                harForsikring = YesOrNo.YES,
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarFiskerMedLott()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe en fisker med hyre`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.FISKER,
                arbeidsgiverOrgnummer = "orgnr",
                harEgenmeldingsdager = YesOrNo.YES,
                riktigNarmesteLeder = YesOrNo.YES,
                egenmeldingsdager = listOf(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-02")),
                fisker =
                    Fisker(
                        blad = Blad.A,
                        lottOgHyre = LottOgHyre.HYRE,
                    ),
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarFiskerMedHyre()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe en fisker med lott og hyre`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.FISKER,
                arbeidsgiverOrgnummer = "orgnr",
                harEgenmeldingsdager = YesOrNo.YES,
                riktigNarmesteLeder = YesOrNo.YES,
                egenmeldingsdager = listOf(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-02")),
                fisker =
                    Fisker(
                        blad = Blad.A,
                        lottOgHyre = LottOgHyre.BEGGE,
                    ),
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarFiskerMedLottOgHyre()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe en jordbruker`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.JORDBRUKER,
                egenmeldingsperioder =
                    listOf(
                        EgenmeldingsperiodeDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-02"),
                        ),
                    ),
                harBruktEgenmelding = YesOrNo.YES,
                harForsikring = YesOrNo.YES,
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarJordbruker()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe en arbeidsledig`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG,
                arbeidsledig =
                    Arbeidsledig(
                        arbeidsledigFraOrgnummer = "orgnr",
                    ),
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarArbeidsledig()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe en permittert`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.PERMITTERT,
                arbeidsledig =
                    Arbeidsledig(
                        arbeidsledigFraOrgnummer = "orgnr",
                    ),
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarPermittert()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }

    @Test
    fun `burde mappe annet`() {
        val requestDTO =
            SendSykmeldingRequestDTO(
                erOpplysningeneRiktige = YesOrNo.YES,
                arbeidssituasjon = Arbeidssituasjon.ANNET,
            )
        val konvertertSporsmalListe = requestDTO.tilSporsmalListe()
        val eksisterendeSporsmalListe = lagSporsmalSvarAnnet()
        konvertertSporsmalListe.size `should be equal to` eksisterendeSporsmalListe.size

        konvertertSporsmalListe.forEach { sporsmal ->
            eksisterendeSporsmalListe `should contain` sporsmal
        }
    }
}
