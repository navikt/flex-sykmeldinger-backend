package no.nav.helse.flex.sykmelding.tsm

import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class DigitalSykmeldingHjelpereTest {
    @Test
    fun `mapper utdypendeSporsmal til utdypendeOpplysninger`() {
        val utdypendeSporsmal =
            listOf(
                UtdypendeSporsmal(
                    type = Sporsmalstype.MEDISINSK_OPPSUMMERING,
                    svar = "svar 1",
                    sporsmal = "sporsmal",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                    svar = "svar 2",
                    sporsmal = "sporsmal",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                    svar = "svar 3",
                    sporsmal = "sporsmal",
                ),
            )

        val expectedUtdypendeOpplysninger =
            mapOf(
                "6.3" to
                    mapOf(
                        "6.3.1" to
                            SporsmalSvar(
                                svar = "svar 1",
                                sporsmal = "sporsmal",
                                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                            ),
                        "6.3.2" to
                            SporsmalSvar(
                                svar = "svar 2",
                                sporsmal = "sporsmal",
                                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                            ),
                        "6.3.3" to
                            SporsmalSvar(
                                svar = "svar 3",
                                sporsmal = "sporsmal",
                                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                            ),
                    ),
            )

        DigitalSykmeldingHjelpere.toUtdypendeOpplysninger(utdypendeSporsmal) `should be equal to` expectedUtdypendeOpplysninger
    }
}
