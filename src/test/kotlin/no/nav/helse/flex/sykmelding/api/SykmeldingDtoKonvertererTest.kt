package no.nav.helse.flex.sykmelding.api

import no.nav.helse.flex.sykmelding.api.dto.ArbeidsgiverDTO
import no.nav.helse.flex.sykmelding.api.dto.PasientDTO
import no.nav.helse.flex.sykmelding.domain.EnArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.Navn
import no.nav.helse.flex.sykmelding.domain.Pasient
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.time.Instant

class SykmeldingDtoKonvertererTest {
    @Test
    fun `burde konvertere`() {
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                statuser =
                    listOf(
                        SykmeldingStatus(
                            status = "NY",
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
    fun `burde konvertere arbeidsgiver, en arbeidsgiver`() {
        val arbeidsgiver =
            EnArbeidsgiver(
                meldingTilArbeidsgiver = "melding",
                tiltakArbeidsplassen = "tiltak",
            )

        val forventetArbeidsgiver =
            ArbeidsgiverDTO(
                navn = TODO(),
                stillingsprosent = TODO(),
            )
    }
}
