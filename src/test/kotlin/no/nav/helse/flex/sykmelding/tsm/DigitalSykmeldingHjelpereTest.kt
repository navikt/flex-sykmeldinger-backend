package no.nav.helse.flex.sykmelding.tsm

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveKey
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class DigitalSykmeldingHjelpereTest {
    @Test
    fun `konverterer alle statiske utdypendeSporsmal til utdypendeOpplysninger`() {
        val utdypendeSporsmal =
            listOf(
                UtdypendeSporsmal(
                    type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                    svar = "svar 6.3.2",
                    sporsmal = "sporsmal 6.3.2",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                    svar = "svar 6.3.3",
                    sporsmal = "sporsmal 6.3.3",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID,
                    svar = "svar 6.4.3",
                    sporsmal = "sporsmal 6.4.3",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.UAVKLARTE_FORHOLD,
                    svar = "svar 6.4.4",
                    sporsmal = "sporsmal 6.4.4",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.FORVENTET_HELSETILSTAND_UTVIKLING,
                    svar = "svar 6.5.3",
                    sporsmal = "sporsmal 6.5.3",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.MEDISINSKE_HENSYN,
                    svar = "svar 6.5.4",
                    sporsmal = "sporsmal 6.5.4",
                ),
            )
        DigitalSykmeldingHjelpere.tilBakoverkompatibelUtdypendeOpplysninger(utdypendeSporsmal).run {
            this["6.3"].shouldNotBeNull().run {
                this["6.3.2"].shouldNotBeNull().run {
                    sporsmal shouldBeEqualTo "sporsmal 6.3.2"
                    svar shouldBeEqualTo "svar 6.3.2"
                    restriksjoner shouldBeEqualTo listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                }
                this["6.3.3"].shouldNotBeNull().run {
                    sporsmal shouldBeEqualTo "sporsmal 6.3.3"
                    svar shouldBeEqualTo "svar 6.3.3"
                    restriksjoner shouldBeEqualTo listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                }
            }
            this["6.4"].shouldNotBeNull().run {
                this["6.4.3"].shouldNotBeNull().run {
                    sporsmal shouldBeEqualTo "sporsmal 6.4.3"
                    svar shouldBeEqualTo "svar 6.4.3"
                    restriksjoner shouldBeEqualTo listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                }
                this["6.4.4"].shouldNotBeNull().run {
                    sporsmal shouldBeEqualTo "sporsmal 6.4.4"
                    svar shouldBeEqualTo "svar 6.4.4"
                    restriksjoner shouldBeEqualTo listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                }
            }
            this["6.5"].shouldNotBeNull().run {
                this["6.5.3"].shouldNotBeNull().run {
                    sporsmal shouldBeEqualTo "sporsmal 6.5.3"
                    svar shouldBeEqualTo "svar 6.5.3"
                    restriksjoner shouldBeEqualTo listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                }
                this["6.5.4"].shouldNotBeNull().run {
                    sporsmal shouldBeEqualTo "sporsmal 6.5.4"
                    svar shouldBeEqualTo "svar 6.5.4"
                    restriksjoner shouldBeEqualTo listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER)
                }
            }
        }
    }

    @TestFactory
    fun `konverterer MEDISINSK_OPPSUMMERING basert på andre spørsmål`() =
        mapOf(
            lagUtdypendeSporsmal(type = Sporsmalstype.MEDISINSKE_HENSYN) to ("6.5" to "6.5.1"),
            lagUtdypendeSporsmal(type = Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID) to ("6.4" to "6.4.1"),
            lagUtdypendeSporsmal(type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID) to ("6.3" to "6.3.1"),
        ).map { (annetSporsmal, forventetNotasjoner) ->
            DynamicTest.dynamicTest(annetSporsmal.type.name) {
                val sporsmals =
                    listOf(
                        UtdypendeSporsmal(
                            type = Sporsmalstype.MEDISINSK_OPPSUMMERING,
                            sporsmal = "sporsmal 1",
                            svar = "svar 1",
                        ),
                        annetSporsmal,
                    )
                val utdypendeOpplysninger = DigitalSykmeldingHjelpere.tilBakoverkompatibelUtdypendeOpplysninger(sporsmals)

                val (hovedSporsmalNotasjon, sporsmalNotasjon) = forventetNotasjoner
                utdypendeOpplysninger[hovedSporsmalNotasjon]
                    .shouldNotBeNull()[sporsmalNotasjon]
                    .shouldNotBeNull()
                    .shouldBeEqualTo(
                        SporsmalSvar(
                            sporsmal = "sporsmal 1",
                            svar = "svar 1",
                            restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                        ),
                    )
                utdypendeOpplysninger.shouldHaveKey(hovedSporsmalNotasjon)
                utdypendeOpplysninger[hovedSporsmalNotasjon]!!.shouldHaveKey(sporsmalNotasjon)
            }
        }

    @TestFactory
    fun `UTFORDRINGER_MED_ARBEID burde ha riktig spørsmål notasjon basert på andre spørsmål`() =
        mapOf(
            lagUtdypendeSporsmal(type = Sporsmalstype.MEDISINSKE_HENSYN) to ("6.5" to "6.5.2"),
            lagUtdypendeSporsmal(type = Sporsmalstype.BEHANDLING_OG_FREMTIDIG_ARBEID) to ("6.4" to "6.4.2"),
            lagUtdypendeSporsmal(type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID) to ("6.3" to "6.3.2"),
        ).map { (annetSporsmal, forventetNotasjoner) ->
            DynamicTest.dynamicTest(annetSporsmal.type.name) {
                val sporsmals =
                    listOf(
                        lagUtdypendeSporsmal(type = Sporsmalstype.UTFORDRINGER_MED_ARBEID),
                        annetSporsmal,
                    )
                val utdypendeOpplysninger = DigitalSykmeldingHjelpere.tilBakoverkompatibelUtdypendeOpplysninger(sporsmals)

                val (hovedSporsmalNotasjon, sporsmalNotasjon) = forventetNotasjoner
                utdypendeOpplysninger.shouldHaveKey(hovedSporsmalNotasjon)
                utdypendeOpplysninger[hovedSporsmalNotasjon]!!.shouldHaveKey(sporsmalNotasjon)
            }
        }
}

private fun lagUtdypendeSporsmal(
    type: Sporsmalstype = Sporsmalstype.MEDISINSK_OPPSUMMERING,
    svar: String = "test default svar",
    skjermetForArbeidsgiver: Boolean = true,
    sporsmal: String? = "test default sporsmal",
) = UtdypendeSporsmal(
    svar = svar,
    type = type,
    skjermetForArbeidsgiver = skjermetForArbeidsgiver,
    sporsmal = sporsmal,
)
