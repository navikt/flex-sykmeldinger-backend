package no.nav.helse.flex.sykmelding.api

import no.nav.helse.flex.FakesTestOppsett
import no.nav.helse.flex.sykmelding.api.dto.Arbeidssituasjon
import no.nav.helse.flex.sykmelding.api.dto.FormSporsmalSvar
import no.nav.helse.flex.sykmelding.api.dto.JaEllerNei
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingSporsmalSvarDto
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingStatusDTO
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.SporsmalTag
import no.nav.helse.flex.sykmelding.domain.Svar
import no.nav.helse.flex.sykmelding.domain.Svartype
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.OffsetDateTime

class SykmeldingStatusDtoKonvertererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusDtoKonverterer: SykmeldingStatusDtoKonverterer

    @Test
    fun `burde konvertere status NY`() {
        val status =
            SykmeldingHendelse(
                status = HendelseStatus.APEN,
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
            )

        val forventetStatus =
            SykmeldingStatusDTO(
                statusEvent = "APEN",
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00.00Z"),
                arbeidsgiver = null,
                sporsmalOgSvarListe = emptyList(),
                brukerSvar = null,
            )

        sykmeldingStatusDtoKonverterer.konverterSykmeldingStatus(status) `should be equal to` forventetStatus
    }

    @Test
    fun `burde konvertere liste med spørsmål til gammelt format`() {
        val sporsmalOgSvarListe =
            listOf(
                Sporsmal(
                    tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
                    sporsmalstekst = "tekst",
                    svartype = Svartype.JA_NEI,
                    svar =
                        listOf(
                            Svar(
                                verdi = "JA",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSSITUASJON,
                    sporsmalstekst = "tekst",
                    svartype = Svartype.JA_NEI,
                    svar =
                        listOf(
                            Svar(
                                verdi = "ARBEIDSTAKER",
                            ),
                        ),
                ),
            )

        val forventetSporsmalSvarDto =
            SykmeldingSporsmalSvarDto(
                erOpplysningeneRiktige =
                    FormSporsmalSvar(
                        sporsmaltekst = "tekst",
                        svar = JaEllerNei.JA,
                    ),
                arbeidssituasjon =
                    FormSporsmalSvar(
                        sporsmaltekst = "tekst",
                        svar = Arbeidssituasjon.ARBEIDSTAKER,
                    ),
                uriktigeOpplysninger = null,
                arbeidsgiverOrgnummer = null,
                arbeidsledig = null,
                riktigNarmesteLeder = null,
                harBruktEgenmelding = null,
                egenmeldingsperioder = null,
                harForsikring = null,
                egenmeldingsdager = null,
                harBruktEgenmeldingsdager = null,
                fisker = null,
            )

        sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmal(sporsmalOgSvarListe) `should be equal to` forventetSporsmalSvarDto
    }
}
