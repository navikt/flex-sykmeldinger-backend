package no.nav.helse.flex.testdata

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.*
import java.time.Instant

fun lagSykmelding(
    sykmeldingGrunnlag: ISykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
    meldingsinformasjon: Meldingsinformasjon = lagMeldingsinformasjonEnkel(),
    validation: ValidationResult = lagValidation(),
    hendelser: List<SykmeldingHendelse> =
        listOf(
            lagSykmeldingHendelse(),
        ),
    opprettet: Instant = Instant.parse("2021-01-01T00:00:00.00Z"),
    hendelseOppdatert: Instant = Instant.parse("2021-01-01T00:00:00.00Z"),
    sykmeldingGrunnlagOppdatert: Instant = Instant.parse("2021-01-01T00:00:00.00Z"),
    validationOppdatert: Instant = Instant.parse("2021-01-01T00:00:00.00Z"),
): Sykmelding =
    Sykmelding(
        sykmeldingGrunnlag = sykmeldingGrunnlag,
        meldingsinformasjon = meldingsinformasjon,
        validation = validation,
        hendelser = hendelser,
        opprettet = opprettet,
        hendelseOppdatert = hendelseOppdatert,
        sykmeldingGrunnlagOppdatert = sykmeldingGrunnlagOppdatert,
        validationOppdatert = validationOppdatert,
    )
