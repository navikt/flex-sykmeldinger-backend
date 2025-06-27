package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.ShortNameKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SporsmalKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SvartypeKafkaDTO
import org.amshove.kluent.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.LocalDate

class SykmeldingHendelseTilKafkaKonvertererTest {
    @Test
    fun `Mapper status BEKREFT_AVVIST riktig`() {
        val hendelse =
            lagSykmeldingHendelse(
                status = HendelseStatus.BEKREFTET_AVVIST,
                hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
            )
        val sykmeldingStatusKafkaDTO =
            SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                sykmeldingId = "1",
                sykmeldingHendelse = hendelse,
            )

        sykmeldingStatusKafkaDTO.sykmeldingId shouldBeEqualTo "1"
        sykmeldingStatusKafkaDTO.timestamp shouldBeEqualTo Instant.parse("2021-01-01T00:00:00.00Z").tilNorgeOffsetDateTime()
        sykmeldingStatusKafkaDTO.statusEvent shouldBeEqualTo "BEKREFTET"
        sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull().shouldHaveSize(0)
        sykmeldingStatusKafkaDTO.brukerSvar.shouldBeNull()
        sykmeldingStatusKafkaDTO.arbeidsgiver shouldBeEqualTo null
    }

    @Test
    fun `Mapper status AVBRUTT riktig`() {
        val sykmeldingStatusKafkaDTO =
            SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                sykmeldingId = "1",
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.AVBRUTT,
                        hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                    ),
            )

        sykmeldingStatusKafkaDTO.sykmeldingId shouldBeEqualTo "1"
        sykmeldingStatusKafkaDTO.timestamp shouldBeEqualTo Instant.parse("2021-01-01T00:00:00.00Z").tilNorgeOffsetDateTime()
        sykmeldingStatusKafkaDTO.statusEvent shouldBeEqualTo "AVBRUTT"
        sykmeldingStatusKafkaDTO.sporsmals.shouldBeNull()
        sykmeldingStatusKafkaDTO.brukerSvar.shouldBeNull()
        sykmeldingStatusKafkaDTO.arbeidsgiver.shouldBeNull()
    }

    @Test
    fun `Mapper status APEN riktig`() {
        val sykmeldingStatusKafkaDTO =
            SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                sykmeldingId = "1",
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.APEN,
                        hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                    ),
            )

        sykmeldingStatusKafkaDTO.sykmeldingId shouldBeEqualTo "1"
        sykmeldingStatusKafkaDTO.timestamp shouldBeEqualTo Instant.parse("2021-01-01T00:00:00.00Z").tilNorgeOffsetDateTime()
        sykmeldingStatusKafkaDTO.statusEvent shouldBeEqualTo "APEN"
        sykmeldingStatusKafkaDTO.sporsmals.shouldBeNull()
        sykmeldingStatusKafkaDTO.brukerSvar.shouldBeNull()
        sykmeldingStatusKafkaDTO.arbeidsgiver.shouldBeNull()
    }

    @Nested
    inner class StatusSendtTilArbeidsgiver {
        @Test
        fun `Mapper arbeidstaker riktig`() {
            val sykmeldingStatusKafkaDTO =
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "1",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                            brukerSvar = lagArbeidstakerBrukerSvar(),
                            tilleggsinfo =
                                ArbeidstakerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnummer = "orgnr",
                                            juridiskOrgnummer = "juridiskOrgnr",
                                            orgnavn = "orgnavn",
                                        ),
                                ),
                        ),
                )

            sykmeldingStatusKafkaDTO.sykmeldingId shouldBeEqualTo "1"
            sykmeldingStatusKafkaDTO.timestamp shouldBeEqualTo Instant.parse("2021-01-01T00:00:00.00Z").tilNorgeOffsetDateTime()
            sykmeldingStatusKafkaDTO.statusEvent shouldBeEqualTo "SENDT"
            sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.brukerSvar.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.arbeidsgiver.shouldNotBeNull().run {
                orgnummer shouldBeEqualTo "orgnr"
                juridiskOrgnummer shouldBeEqualTo "juridiskOrgnr"
                orgNavn shouldBeEqualTo "orgnavn"
            }
        }

        @Test
        fun `Mapper UtdatertFormatTilleggsinfo til arbeidstaker riktig`() {
            val sykmeldingStatusKafkaDTO =
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "1",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                            brukerSvar = lagArbeidstakerBrukerSvar(),
                            tilleggsinfo =
                                lagUtdatertFormatTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnummer = "orgnr",
                                            juridiskOrgnummer = "juridiskOrgnr",
                                            orgnavn = "orgnavn",
                                        ),
                                ),
                        ),
                )
            sykmeldingStatusKafkaDTO.arbeidsgiver.shouldNotBeNull().run {
                orgnummer shouldBeEqualTo "orgnr"
                juridiskOrgnummer shouldBeEqualTo "juridiskOrgnr"
                orgNavn shouldBeEqualTo "orgnavn"
            }
        }

        @Test
        fun `Burde ikke godta UtdatertFormatBrukerSvar`() {
            invoking {
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "1",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                            brukerSvar = lagUtdatertFormatBrukerSvar(),
                        ),
                )
            } shouldThrow IllegalArgumentException::class
        }
    }

    @Nested
    inner class KonverterTilBrukerSvarKafkaDTO {
        @Test
        fun `burde konvertere alle svar`() {
            val sprosmalSvarDto =
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige =
                        FormSporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = JaEllerNei.JA,
                        ),
                    arbeidssituasjon =
                        FormSporsmalSvar(
                            sporsmaltekst = "Hva er din arbeidssituasjon?",
                            svar = ArbeidssituasjonDTO.ARBEIDSTAKER,
                        ),
                    uriktigeOpplysninger =
                        FormSporsmalSvar(
                            sporsmaltekst = "Er det noen uriktige opplysninger?",
                            svar = listOf(UriktigeOpplysningerType.PERIODE),
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
                                    EgenmeldingsperiodeFormDTO(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05")),
                                    EgenmeldingsperiodeFormDTO(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15")),
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
                        FiskerSvar(
                            blad =
                                FormSporsmalSvar(
                                    sporsmaltekst = "Hvilket blad?",
                                    svar = Blad.A,
                                ),
                            lottOgHyre =
                                FormSporsmalSvar(
                                    sporsmaltekst = "Lott eller Hyre?",
                                    svar = LottOgHyre.LOTT,
                                ),
                        ),
                )

            val brukerSvar =
                SykmeldingHendelseTilKafkaKonverterer.konverterTilBrukerSvarKafkaDTO(
                    sykmeldingSporsmalSvarDto = sprosmalSvarDto,
                )

            brukerSvar.erOpplysningeneRiktige.run {
                sporsmaltekst shouldBeEqualTo "Er opplysningene riktige?"
                svar shouldBeEqualTo JaEllerNei.JA
            }
            brukerSvar.uriktigeOpplysninger.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Er det noen uriktige opplysninger?"
                svar shouldBeEqualTo listOf(UriktigeOpplysningerType.PERIODE)
            }

            brukerSvar.erOpplysningeneRiktige.run {
                sporsmaltekst shouldBeEqualTo "Er opplysningene riktige?"
                svar shouldBeEqualTo JaEllerNei.JA
            }
            brukerSvar.arbeidssituasjon.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Hva er din arbeidssituasjon?"
                svar shouldBeEqualTo ArbeidssituasjonDTO.ARBEIDSTAKER
            }
            brukerSvar.arbeidsgiverOrgnummer.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Hva er arbeidsgiverens organisasjonsnummer?"
                svar shouldBeEqualTo "123456789"
            }
            brukerSvar.riktigNarmesteLeder.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Er dette riktig nærmeste leder?"
                svar shouldBeEqualTo JaEllerNei.JA
            }
            brukerSvar.harBruktEgenmelding.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Har du brukt egenmelding?"
                svar shouldBeEqualTo JaEllerNei.JA
            }
            brukerSvar.egenmeldingsperioder.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Hvilke egenmeldingsperioder har du hatt?"
                svar shouldBeEqualTo
                    listOf(
                        EgenmeldingsperiodeFormDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-05"),
                        ),
                        EgenmeldingsperiodeFormDTO(
                            fom = LocalDate.parse("2025-01-10"),
                            tom = LocalDate.parse("2025-01-15"),
                        ),
                    )
            }
            brukerSvar.harForsikring.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Har du forsikring?"
                svar shouldBeEqualTo JaEllerNei.JA
            }
            brukerSvar.egenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Hvilke egenmeldingsdager har du hatt?"
                svar shouldBeEqualTo listOf(LocalDate.parse("2021-01-01"))
            }
            brukerSvar.harBruktEgenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Har du brukt egenmeldingsdager?"
                svar shouldBeEqualTo JaEllerNei.JA
            }
            brukerSvar.fisker.shouldNotBeNull().run {
                blad.run {
                    sporsmaltekst shouldBeEqualTo "Hvilket blad?"
                    svar shouldBeEqualTo Blad.A
                }
                lottOgHyre.run {
                    sporsmaltekst shouldBeEqualTo "Lott eller Hyre?"
                    svar shouldBeEqualTo LottOgHyre.LOTT
                }
            }
        }
    }

    @Nested
    inner class KonverterTilSporsmalsKafkaDto {
        @Test
        fun `burde konvertere alle svar`() {
            val sprosmalSvarDto =
                SykmeldingSporsmalSvarDto(
                    // Brukes ikke for gammelt sykmeldingsformat
                    erOpplysningeneRiktige =
                        FormSporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = JaEllerNei.JA,
                        ),
                    arbeidssituasjon =
                        FormSporsmalSvar(
                            sporsmaltekst = "Hva er din arbeidssituasjon?",
                            svar = ArbeidssituasjonDTO.ARBEIDSTAKER,
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
                                    EgenmeldingsperiodeFormDTO(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05")),
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
                    arbeidsgiverOrgnummer = null,
                    uriktigeOpplysninger = null,
                    harBruktEgenmeldingsdager = null,
                    fisker = null,
                )

            val sporsmals =
                SykmeldingHendelseTilKafkaKonverterer.konverterTilSporsmalsKafkaDto(
                    sporsmalSvarDto = sprosmalSvarDto,
                    harAktivtArbeidsforhold = null,
                )
            sporsmals shouldHaveSize 6

            sporsmals.finnSporsmal(ShortNameKafkaDTO.ARBEIDSSITUASJON).shouldNotBeNull().run {
                tekst shouldBeEqualTo "Hva er din arbeidssituasjon?"
                svartype shouldBeEqualTo SvartypeKafkaDTO.ARBEIDSSITUASJON
                svar shouldBeEqualTo "ARBEIDSTAKER"
            }
            sporsmals.finnSporsmal(ShortNameKafkaDTO.FRAVAER).shouldNotBeNull().run {
                tekst shouldBeEqualTo "Har du brukt egenmelding?"
                svartype shouldBeEqualTo SvartypeKafkaDTO.JA_NEI
                svar shouldBeEqualTo "JA"
            }
            sporsmals.finnSporsmal(ShortNameKafkaDTO.PERIODE).shouldNotBeNull().run {
                tekst shouldBeEqualTo "Hvilke egenmeldingsperioder har du hatt?"
                svartype shouldBeEqualTo SvartypeKafkaDTO.PERIODER
                svar shouldBeEqualTo """[{"fom":"2025-01-01","tom":"2025-01-05"}]"""
            }
            sporsmals.finnSporsmal(ShortNameKafkaDTO.NY_NARMESTE_LEDER).shouldNotBeNull().run {
                tekst shouldBeEqualTo "Er dette riktig nærmeste leder?"
                svartype shouldBeEqualTo SvartypeKafkaDTO.JA_NEI
                svar shouldBeEqualTo "NEI"
            }
            sporsmals.finnSporsmal(ShortNameKafkaDTO.FORSIKRING).shouldNotBeNull().run {
                tekst shouldBeEqualTo "Har du forsikring?"
                svartype shouldBeEqualTo SvartypeKafkaDTO.JA_NEI
                svar shouldBeEqualTo "JA"
            }
        }

        @Test
        fun `arbeidssituasjon FISKER burde bli ARBEIDSTAKER dersom HYRE`() {
            val sporsmalSvar =
                lagSykmeldingSporsmalSvarDto(
                    arbeidssituasjon = lagFormSporsmalSvar(ArbeidssituasjonDTO.FISKER),
                    fisker =
                        lagFiskerSvar(
                            lottOgHyre = lagFormSporsmalSvar(LottOgHyre.HYRE),
                        ),
                )

            val sporsmalListe =
                SykmeldingHendelseTilKafkaKonverterer.konverterTilSporsmalsKafkaDto(
                    sporsmalSvar,
                    harAktivtArbeidsforhold = null,
                )

            sporsmalListe
                .finnSporsmal(ShortNameKafkaDTO.ARBEIDSSITUASJON)
                .shouldNotBeNull()
                .svar shouldBeEqualTo "ARBEIDSTAKER"
        }

        @ParameterizedTest
        @EnumSource(LottOgHyre::class, names = ["LOTT", "BEGGE"])
        fun `arbeidssituasjon FISKER burde bli NAERINGSDRIVENDE dersom`(lottOgHyre: LottOgHyre) {
            val sporsmalSvar =
                lagSykmeldingSporsmalSvarDto(
                    arbeidssituasjon = lagFormSporsmalSvar(ArbeidssituasjonDTO.FISKER),
                    fisker =
                        lagFiskerSvar(
                            lottOgHyre = lagFormSporsmalSvar(lottOgHyre),
                        ),
                )

            val sporsmalListe =
                SykmeldingHendelseTilKafkaKonverterer.konverterTilSporsmalsKafkaDto(
                    sporsmalSvar,
                    harAktivtArbeidsforhold = null,
                )

            sporsmalListe
                .finnSporsmal(ShortNameKafkaDTO.ARBEIDSSITUASJON)
                .shouldNotBeNull()
                .svar shouldBeEqualTo "NAERINGSDRIVENDE"
        }

        @Test
        fun `arbeidssituasjon JORDBRUKER burde alltid bli NAERINGSDRIVENDE`() {
            val sporsmalSvar =
                lagSykmeldingSporsmalSvarDto(
                    arbeidssituasjon = lagFormSporsmalSvar(ArbeidssituasjonDTO.JORDBRUKER),
                )

            val sporsmalListe =
                SykmeldingHendelseTilKafkaKonverterer.konverterTilSporsmalsKafkaDto(
                    sporsmalSvar,
                    harAktivtArbeidsforhold = null,
                )

            sporsmalListe
                .finnSporsmal(ShortNameKafkaDTO.ARBEIDSSITUASJON)
                .shouldNotBeNull()
                .svar shouldBeEqualTo "NAERINGSDRIVENDE"
        }

        @Test
        fun `riktigNarmesteLeder svar burde ha motsatt svar`() {
            val sporsmalSvar =
                lagSykmeldingSporsmalSvarDto(
                    riktigNarmesteLeder =
                        FormSporsmalSvar(
                            sporsmaltekst = "Er dette riktig nærmeste leder?",
                            svar = JaEllerNei.JA,
                        ),
                )

            val sporsmalListe =
                SykmeldingHendelseTilKafkaKonverterer.konverterTilSporsmalsKafkaDto(
                    sporsmalSvar,
                    harAktivtArbeidsforhold = null,
                )

            sporsmalListe
                .finnSporsmal(ShortNameKafkaDTO.NY_NARMESTE_LEDER)
                .shouldNotBeNull()
                .svar shouldBeEqualTo "NEI"
        }

        @Test
        fun `riktigNarmesteLeder blir alltid satt dersom ikke harAktivtArbeidsforhold`() {
            val sporsmalSvar =
                lagSykmeldingSporsmalSvarDto(
                    arbeidssituasjon = lagFormSporsmalSvar(ArbeidssituasjonDTO.ARBEIDSTAKER),
                    riktigNarmesteLeder =
                        FormSporsmalSvar(
                            sporsmaltekst = "Er dette riktig nærmeste leder?",
                            svar = JaEllerNei.NEI,
                        ),
                )

            val sporsmalListe =
                SykmeldingHendelseTilKafkaKonverterer.konverterTilSporsmalsKafkaDto(
                    sporsmalSvar,
                    harAktivtArbeidsforhold = false,
                )
            sporsmalListe
                .finnSporsmal(ShortNameKafkaDTO.NY_NARMESTE_LEDER)
                .shouldNotBeNull()
                .svar shouldBeEqualTo "NEI"
        }

        @Test
        fun `riktigNarmesteLeder spørsmål blir ignorert dersom ikke harAktivtArbeidsforhold`() {
            val sporsmalSvar =
                lagSykmeldingSporsmalSvarDto(
                    arbeidssituasjon = lagFormSporsmalSvar(ArbeidssituasjonDTO.ARBEIDSTAKER),
                    riktigNarmesteLeder =
                        FormSporsmalSvar(
                            sporsmaltekst = "Er dette riktig nærmeste leder?",
                            svar = JaEllerNei.JA,
                        ),
                )

            val sporsmalListe =
                SykmeldingHendelseTilKafkaKonverterer.konverterTilSporsmalsKafkaDto(
                    sporsmalSvar,
                    harAktivtArbeidsforhold = false,
                )
            sporsmalListe
                .finnSporsmal(ShortNameKafkaDTO.NY_NARMESTE_LEDER)
                .shouldNotBeNull()
                .svar shouldBeEqualTo "NEI"
        }

        private fun List<SporsmalKafkaDTO>.finnSporsmal(shortName: ShortNameKafkaDTO): SporsmalKafkaDTO? =
            this.find {
                it.shortName ==
                    shortName
            }
    }
}
