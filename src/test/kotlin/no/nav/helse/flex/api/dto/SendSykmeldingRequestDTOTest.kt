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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER,
                arbeidsgiverOrgnummer = "orgnr",
                harEgenmeldingsdager = YesOrNoDTO.YES,
                riktigNarmesteLeder = YesOrNoDTO.YES,
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.FRILANSER,
                egenmeldingsperioder =
                    listOf(
                        EgenmeldingsperiodeDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-02"),
                        ),
                    ),
                harBruktEgenmelding = YesOrNoDTO.YES,
                harForsikring = YesOrNoDTO.YES,
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.NAERINGSDRIVENDE,
                egenmeldingsperioder =
                    listOf(
                        EgenmeldingsperiodeDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-02"),
                        ),
                    ),
                harBruktEgenmelding = YesOrNoDTO.YES,
                harForsikring = YesOrNoDTO.YES,
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.FISKER,
                egenmeldingsperioder =
                    listOf(
                        EgenmeldingsperiodeDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-02"),
                        ),
                    ),
                fisker =
                    FiskerDTO(
                        blad = Blad.A,
                        lottOgHyre = LottOgHyre.LOTT,
                    ),
                harBruktEgenmelding = YesOrNoDTO.YES,
                harForsikring = YesOrNoDTO.YES,
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.FISKER,
                arbeidsgiverOrgnummer = "orgnr",
                harEgenmeldingsdager = YesOrNoDTO.YES,
                riktigNarmesteLeder = YesOrNoDTO.YES,
                egenmeldingsdager = listOf(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-02")),
                fisker =
                    FiskerDTO(
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.FISKER,
                arbeidsgiverOrgnummer = "orgnr",
                harEgenmeldingsdager = YesOrNoDTO.YES,
                riktigNarmesteLeder = YesOrNoDTO.YES,
                egenmeldingsdager = listOf(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-02")),
                fisker =
                    FiskerDTO(
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.JORDBRUKER,
                egenmeldingsperioder =
                    listOf(
                        EgenmeldingsperiodeDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-02"),
                        ),
                    ),
                harBruktEgenmelding = YesOrNoDTO.YES,
                harForsikring = YesOrNoDTO.YES,
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG,
                arbeidsledig =
                    ArbeidsledigDTO(
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
                arbeidssituasjon = Arbeidssituasjon.PERMITTERT,
                arbeidsledig =
                    ArbeidsledigDTO(
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
                erOpplysningeneRiktige = YesOrNoDTO.YES,
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
