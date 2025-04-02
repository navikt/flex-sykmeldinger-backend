package no.nav.helse.flex.testdata

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.producers.sykmeldingstatus.KafkaMetadataDTO
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import java.time.LocalDate
import java.time.OffsetDateTime

fun lagSykmeldingStatusKafkaMessageDTO(
    sykmeldingId: String = "1",
    fnr: String = "fnr",
    brukerSvarKafkaDTO: BrukerSvarKafkaDTO? = lagBrukerSvarKafkaDto(ArbeidssituasjonDTO.ARBEIDSTAKER),
    statusEvent: String = "SENDT",
    source: String = "syfosmaltinn",
): SykmeldingStatusKafkaMessageDTO =
    SykmeldingStatusKafkaMessageDTO(
        kafkaMetadata =
            KafkaMetadataDTO(
                sykmeldingId = sykmeldingId,
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                fnr = fnr,
                source = source,
            ),
        event =
            SykmeldingStatusKafkaDTO(
                sykmeldingId = sykmeldingId,
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                statusEvent = statusEvent,
                arbeidsgiver = null,
                sporsmals = null,
                brukerSvar = brukerSvarKafkaDTO,
                tidligereArbeidsgiver = null,
            ),
    )

fun lagBrukerSvarKafkaDto(arbeidssituasjonKafkaDTO: ArbeidssituasjonDTO) =
    BrukerSvarKafkaDTO(
        erOpplysningeneRiktige =
            FormSporsmalSvar(
                sporsmaltekst = "Er opplysningene riktige?",
                svar = JaEllerNei.JA,
            ),
        uriktigeOpplysninger =
            FormSporsmalSvar(
                sporsmaltekst = "Er det noen uriktige opplysninger?",
                svar = listOf(UriktigeOpplysningerType.PERIODE),
            ),
        arbeidssituasjon =
            FormSporsmalSvar(
                sporsmaltekst = "Hva er din arbeidssituasjon?",
                svar = arbeidssituasjonKafkaDTO,
            ),
        arbeidsgiverOrgnummer =
            FormSporsmalSvar(
                sporsmaltekst = "Hva er arbeidsgiverens organisasjonsnummer?",
                svar = "123456789",
            ),
        riktigNarmesteLeder =
            FormSporsmalSvar(
                sporsmaltekst = "Er dette riktig nærmeste leder?",
                svar = JaEllerNei.JA,
            ),
        harBruktEgenmelding =
            FormSporsmalSvar(
                sporsmaltekst = "Har du brukt egenmelding?",
                svar = JaEllerNei.JA,
            ),
        egenmeldingsperioder =
            FormSporsmalSvar(
                sporsmaltekst = "Hvilke egenmeldingsperioder har du hatt?",
                svar =
                    listOf(
                        EgenmeldingsperiodeFormDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-05"),
                        ),
                        EgenmeldingsperiodeFormDTO(
                            fom = LocalDate.parse("2025-01-10"),
                            tom = LocalDate.parse("2025-01-15"),
                        ),
                    ),
            ),
        harForsikring =
            FormSporsmalSvar(
                sporsmaltekst = "Har du forsikring?",
                svar = JaEllerNei.JA,
            ),
        egenmeldingsdager =
            FormSporsmalSvar(
                sporsmaltekst = "Hvilke egenmeldingsdager har du hatt?",
                svar = listOf(LocalDate.parse("2021-01-01")),
            ),
        harBruktEgenmeldingsdager =
            FormSporsmalSvar(
                sporsmaltekst = "Har du brukt egenmeldingsdager?",
                svar = JaEllerNei.JA,
            ),
        fisker =
            FiskereSvarKafkaDTO(
                blad = FormSporsmalSvar("Hvilket blad?", Blad.A),
                lottOgHyre = FormSporsmalSvar("Lott eller Hyre?", LottOgHyre.LOTT),
            ),
    )
