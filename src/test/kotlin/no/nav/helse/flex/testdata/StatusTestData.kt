package no.nav.helse.flex.testdata

import no.nav.helse.flex.producers.sykmeldingstatus.KafkaMetadataDTO
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import java.time.LocalDate
import java.time.OffsetDateTime

fun lagStatus(
    sykmeldingId: String = "1",
    fnr: String = "fnr",
    brukerSvarKafkaDTO: BrukerSvarKafkaDTO = lagBrukerSvarKafkaDto(ArbeidssituasjonKafkaDTO.ARBEIDSTAKER),
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
                erSvarOppdatering = false,
                tidligereArbeidsgiver = null,
            ),
    )

fun lagBrukerSvarKafkaDto(arbeidssituasjonKafkaDTO: ArbeidssituasjonKafkaDTO) =
    BrukerSvarKafkaDTO(
        erOpplysningeneRiktige =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Er opplysningene riktige?",
                svar = JaEllerNeiKafkaDTO.JA,
            ),
        uriktigeOpplysninger =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Er det noen uriktige opplysninger?",
                svar = listOf(UriktigeOpplysningerTypeKafkaDTO.PERIODE),
            ),
        arbeidssituasjon =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Hva er din arbeidssituasjon?",
                svar = arbeidssituasjonKafkaDTO,
            ),
        arbeidsgiverOrgnummer =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Hva er arbeidsgiverens organisasjonsnummer?",
                svar = "123456789",
            ),
        riktigNarmesteLeder =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Er dette riktig nærmeste leder?",
                svar = JaEllerNeiKafkaDTO.JA,
            ),
        harBruktEgenmelding =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Har du brukt egenmelding?",
                svar = JaEllerNeiKafkaDTO.JA,
            ),
        egenmeldingsperioder =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Hvilke egenmeldingsperioder har du hatt?",
                svar =
                    listOf(
                        EgenmeldingsperiodeKafkaDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-05"),
                        ),
                        EgenmeldingsperiodeKafkaDTO(
                            fom = LocalDate.parse("2025-01-10"),
                            tom = LocalDate.parse("2025-01-15"),
                        ),
                    ),
            ),
        harForsikring =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Har du forsikring?",
                svar = JaEllerNeiKafkaDTO.JA,
            ),
        egenmeldingsdager =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Hvilke egenmeldingsdager har du hatt?",
                svar = listOf(LocalDate.parse("2021-01-01")),
            ),
        harBruktEgenmeldingsdager =
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Har du brukt egenmeldingsdager?",
                svar = JaEllerNeiKafkaDTO.JA,
            ),
        fisker =
            FiskereSvarKafkaDTO(
                blad = SporsmalSvarKafkaDTO("Hvilket blad?", BladKafkaDTO.A),
                lottOgHyre = SporsmalSvarKafkaDTO("Lott eller Hyre?", LottOgHyreKafkaDTO.LOTT),
            ),
    )
