package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.sykmelding.application.Arbeidssituasjon
import no.nav.helse.flex.sykmelding.application.ArbeidstakerBrukerSvar
import no.nav.helse.flex.tsmsykmeldingstatus.dto.ShortNameKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SporsmalKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SvartypeKafkaDTO
import org.amshove.kluent.*
import org.junit.jupiter.api.Test

class StatusSporsmalListeKonvertererTest {
    @Test
    fun `burde returnere null dersom ingen spørsmål`() {
        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(emptyList()).shouldBeNull()
    }

    @Test
    fun `burde konvertere minimalt antall spørsmål til brukerSvar`() {
        val sporsmals =
            listOf(
                SporsmalKafkaDTO(
                    tekst = "Jeg er sykmeldt som",
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = "ARBEIDSTAKER",
                ),
            )
        val brukerSvar = StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmals)
        brukerSvar
            .shouldNotBeNull()
            .shouldBeInstanceOf<ArbeidstakerBrukerSvar>()
            .run {
                arbeidssituasjon `should be equal to` Arbeidssituasjon.ARBEIDSTAKER
                arbeidssituasjonSporsmal.run {
                    sporsmaltekst `should be equal to` "Jeg er sykmeldt som"
                    svar `should be equal to` Arbeidssituasjon.ARBEIDSTAKER
                }
                arbeidsgiverOrgnummer.run {
                    sporsmaltekst shouldBeEqualTo ""
                    svar shouldBeEqualTo ""
                }
            }
    }
}
