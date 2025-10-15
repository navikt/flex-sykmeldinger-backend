package no.nav.helse.flex.sykmeldinghendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.testdata.lagSporsmalSvar
import no.nav.helse.flex.utils.objectMapper
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class BrukerSvarTest {
    @Test
    fun `burde deserialisere SporsmalSvar`() {
        val sporsmalSvar =
            SporsmalSvar(
                sporsmaltekst = "Hvordan har du det?",
                svar = "Bra",
            )
        val json = objectMapper.writeValueAsString(sporsmalSvar)
        val deserialisert: SporsmalSvar<String> = objectMapper.readValue(json)
        sporsmalSvar `should be equal to` deserialisert
    }

    @TestFactory
    fun `burde opprette fisker som naringsdrivende med minimalt besvarte spørsmål`() =
        listOf(FiskerLottOgHyre.LOTT, FiskerLottOgHyre.BEGGE)
            .map { lottOgHyre ->
                DynamicTest.dynamicTest("lottOgHyre: ${lottOgHyre.name}") {
                    FiskerBrukerSvar(
                        erOpplysningeneRiktige = lagSporsmalSvar(svar = true),
                        arbeidssituasjon = lagSporsmalSvar(svar = Arbeidssituasjon.FISKER),
                        lottOgHyre = lagSporsmalSvar(svar = lottOgHyre),
                        blad = lagSporsmalSvar(svar = FiskerBlad.B),
                    ).run {
                        erSomArbeidstaker.shouldBeFalse()
                    }
                }
            }

    @TestFactory
    fun `burde opprette fisker som arbeidstaker med minimalt besvarte spørsmål`() =
        listOf(FiskerLottOgHyre.HYRE, FiskerLottOgHyre.BEGGE)
            .map { lottOgHyre ->
                DynamicTest.dynamicTest("lottOgHyre: ${lottOgHyre.name}") {
                    FiskerBrukerSvar(
                        erOpplysningeneRiktige = lagSporsmalSvar(svar = true),
                        arbeidssituasjon = lagSporsmalSvar(svar = Arbeidssituasjon.FISKER),
                        lottOgHyre = lagSporsmalSvar(svar = lottOgHyre),
                        blad = lagSporsmalSvar(svar = FiskerBlad.B),
                        arbeidsgiverOrgnummer = lagSporsmalSvar(svar = "orgnr"),
                    ).run {
                        erSomArbeidstaker.shouldBeTrue()
                    }
                }
            }
}
