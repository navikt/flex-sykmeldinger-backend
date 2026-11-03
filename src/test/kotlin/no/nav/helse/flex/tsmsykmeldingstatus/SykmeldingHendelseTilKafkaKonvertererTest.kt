package no.nav.helse.flex.tsmsykmeldingstatus

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.sykmeldinghendelse.*
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.ShortNameKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SporsmalKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SvartypeKafkaDTO
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Instant
import java.time.LocalDate

class SykmeldingHendelseTilKafkaKonvertererTest {
    @TestFactory
    fun `konverterer sykmeldingId riktig`() =
        mapOf(
            "1" to "1",
            "2" to "2",
        ).map { (sykmeldingId, kafkaSykmeldingId) ->
            DynamicTest.dynamicTest("konverterer $sykmeldingId til $kafkaSykmeldingId") {
                SykmeldingHendelseTilKafkaKonverterer
                    .konverterSykmeldingHendelseTilKafkaDTO(
                        sykmeldingId = sykmeldingId,
                        sykmeldingHendelse = lagSykmeldingHendelse(),
                    ).run {
                        this.sykmeldingId shouldBe kafkaSykmeldingId
                    }
            }
        }

    @TestFactory
    fun `konverterer timestamp riktig`() =
        mapOf(
            Instant.parse("2021-01-01T00:00:00.00Z") to Instant.parse("2021-01-01T00:00:00.00Z").tilNorgeOffsetDateTime(),
            Instant.parse("2022-02-02T00:00:00.00Z") to Instant.parse("2022-02-02T00:00:00.00Z").tilNorgeOffsetDateTime(),
        ).map { (hendelseOpprettet, kafkaTimestamp) ->
            DynamicTest.dynamicTest("konverterer $hendelseOpprettet til $kafkaTimestamp") {
                SykmeldingHendelseTilKafkaKonverterer
                    .konverterSykmeldingHendelseTilKafkaDTO(
                        sykmeldingId = "_",
                        sykmeldingHendelse = lagSykmeldingHendelse(hendelseOpprettet = hendelseOpprettet),
                    ).run {
                        this.timestamp shouldBe kafkaTimestamp
                    }
            }
        }

    @Test
    fun `konverterer hendelse med status BEKREFT_AVVIST riktig`() {
        val hendelse =
            lagSykmeldingHendelse(
                status = HendelseStatus.BEKREFTET_AVVIST,
            )
        val sykmeldingStatusKafkaDTO =
            SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                sykmeldingId = "_",
                sykmeldingHendelse = hendelse,
            )
        sykmeldingStatusKafkaDTO.statusEvent shouldBe "BEKREFTET"
        sykmeldingStatusKafkaDTO.sporsmals.shouldBeNull()
        sykmeldingStatusKafkaDTO.brukerSvar.shouldBeNull()
    }

    @TestFactory
    fun `konverterer hendelse med status APEN og AVBRUTT riktig`() =
        mapOf(
            HendelseStatus.APEN to "APEN",
            HendelseStatus.AVBRUTT to "AVBRUTT",
        ).map { (status, kafkaStatus) ->
            DynamicTest.dynamicTest("konverterer hendelse med status $status") {
                val sykmeldingStatusKafkaDTO =
                    SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                        sykmeldingId = "_",
                        sykmeldingHendelse =
                            lagSykmeldingHendelse(status = status),
                    )

                sykmeldingStatusKafkaDTO.statusEvent shouldBe kafkaStatus
                sykmeldingStatusKafkaDTO.sporsmals.shouldBeNull()
                sykmeldingStatusKafkaDTO.brukerSvar.shouldBeNull()
            }
        }

    @Nested
    inner class `Konverterer hendelse med status SENDT_TIL_ARBEIDSGIVER og SENDT_TIL_NAV` {
        @Test
        fun `konverterer arbeidstaker riktig`() {
            val sykmeldingStatusKafkaDTO =
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "_",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
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

            sykmeldingStatusKafkaDTO.statusEvent shouldBe "SENDT"
            sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.brukerSvar.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.arbeidsgiver.shouldNotBeNull().run {
                orgnummer shouldBe "orgnr"
                juridiskOrgnummer shouldBe "juridiskOrgnr"
                orgNavn shouldBe "orgnavn"
            }
        }

        @Test
        fun `konverterer fisker med hyre riktig`() {
            val sykmeldingStatusKafkaDTO =
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "_",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            brukerSvar = lagFiskerHyreBrukerSvar(),
                            tilleggsinfo =
                                FiskerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnummer = "orgnr",
                                            juridiskOrgnummer = "juridiskOrgnr",
                                            orgnavn = "orgnavn",
                                        ),
                                ),
                        ),
                )

            sykmeldingStatusKafkaDTO.statusEvent shouldBe "SENDT"
            sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.brukerSvar.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.arbeidsgiver.shouldNotBeNull().run {
                orgnummer shouldBe "orgnr"
                juridiskOrgnummer shouldBe "juridiskOrgnr"
                orgNavn shouldBe "orgnavn"
            }
        }

        @Test
        fun `konverterer fisker med lott riktig`() {
            val sykmeldingStatusKafkaDTO =
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "_",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_NAV,
                            brukerSvar = lagFiskerLottBrukerSvar(),
                            tilleggsinfo = lagFiskerTilleggsinfo(arbeidsgiver = null),
                        ),
                )

            sykmeldingStatusKafkaDTO.statusEvent shouldBe "BEKREFTET"
            sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.brukerSvar.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.arbeidsgiver.shouldBeNull()
        }

        @Test
        fun `konverterer fisker med både lott og hyre riktig`() {
            val sykmeldingStatusKafkaDTO =
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "_",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_NAV,
                            brukerSvar =
                                lagFiskerHyreBrukerSvar(lottOgHyre = lagSporsmalSvar(FiskerLottOgHyre.BEGGE)),
                            tilleggsinfo =
                                FiskerTilleggsinfo(
                                    arbeidsgiver =
                                        lagArbeidsgiver(
                                            orgnummer = "orgnr",
                                            juridiskOrgnummer = "juridiskOrgnr",
                                            orgnavn = "orgnavn",
                                        ),
                                ),
                        ),
                )

            sykmeldingStatusKafkaDTO.statusEvent shouldBe "BEKREFTET"
            sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.brukerSvar.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.arbeidsgiver.shouldNotBeNull().run {
                orgnummer shouldBe "orgnr"
                juridiskOrgnummer shouldBe "juridiskOrgnr"
                orgNavn shouldBe "orgnavn"
            }
        }

        @Test
        fun `konverterer arbeidsledig riktig`() {
            val sykmeldingStatusKafkaDTO =
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "_",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_NAV,
                            brukerSvar =
                                lagArbeidsledigBrukerSvar(),
                            tilleggsinfo =
                                lagArbeidsledigTilleggsinfo(
                                    tidligereArbeidsgiver =
                                        TidligereArbeidsgiver(
                                            orgNavn = "orgnavn",
                                            orgnummer = "orgnr",
                                        ),
                                ),
                        ),
                )

            sykmeldingStatusKafkaDTO.statusEvent shouldBe "BEKREFTET"
            sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.brukerSvar.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.arbeidsgiver.shouldBeNull()
            sykmeldingStatusKafkaDTO.tidligereArbeidsgiver.shouldNotBeNull().run {
                orgnummer shouldBe "orgnr"
                orgNavn shouldBe "orgnavn"
            }
        }

        @Test
        fun `konverterer permittert riktig`() {
            val sykmeldingStatusKafkaDTO =
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "_",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_NAV,
                            brukerSvar = lagPermittertBrukerSvar(),
                            tilleggsinfo =
                                lagPermittertTilleggsinfo(
                                    tidligereArbeidsgiver =
                                        TidligereArbeidsgiver(
                                            orgNavn = "orgnavn",
                                            orgnummer = "orgnr",
                                        ),
                                ),
                        ),
                )

            sykmeldingStatusKafkaDTO.statusEvent shouldBe "BEKREFTET"
            sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.brukerSvar.shouldNotBeNull()
            sykmeldingStatusKafkaDTO.arbeidsgiver.shouldBeNull()
            sykmeldingStatusKafkaDTO.tidligereArbeidsgiver.shouldNotBeNull().run {
                orgnummer shouldBe "orgnr"
                orgNavn shouldBe "orgnavn"
            }
        }

        @TestFactory
        fun `konverterer enkle arbeidsforhold riktig`() =
            mapOf(
                "naringsdrivende" to
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        brukerSvar = lagNaringsdrivendeBrukerSvar(),
                        tilleggsinfo = lagNaringsdrivendeTilleggsinfo(),
                    ),
                "frilanser" to
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        brukerSvar = lagFrilanserBrukerSvar(),
                        tilleggsinfo = lagFrilanserTilleggsinfo(),
                    ),
                "jordbruker" to
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        brukerSvar = lagJordbrukerBrukerSvar(),
                        tilleggsinfo = lagJordbrukerTilleggsinfo(),
                    ),
                "annet-arbeidssituasjon" to
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_NAV,
                        brukerSvar = lagAnnetArbeidssituasjonBrukerSvar(),
                        tilleggsinfo = lagAnnetArbeidssituasjonTilleggsinfo(),
                    ),
            ).map { (testNavn, hendelse) ->
                DynamicTest.dynamicTest(testNavn) {
                    val sykmeldingStatusKafkaDTO =
                        SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                            sykmeldingId = "_",
                            sykmeldingHendelse = hendelse,
                        )

                    sykmeldingStatusKafkaDTO.statusEvent shouldBe "BEKREFTET"
                    sykmeldingStatusKafkaDTO.sporsmals.shouldNotBeNull()
                    sykmeldingStatusKafkaDTO.brukerSvar.shouldNotBeNull()
                    sykmeldingStatusKafkaDTO.arbeidsgiver.shouldBeNull()
                    sykmeldingStatusKafkaDTO.tidligereArbeidsgiver.shouldBeNull()
                }
            }

        @Test
        fun `Burde ikke godta hendelser med UtdatertFormatTilleggsinfo`() {
            shouldThrow<IllegalArgumentException> {
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "1",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                            brukerSvar = lagArbeidstakerBrukerSvar(),
                            tilleggsinfo = lagUtdatertFormatTilleggsinfo(),
                        ),
                )
            }
        }

        @Test
        fun `Burde ikke godta hendelser med UtdatertFormatBrukerSvar`() {
            shouldThrow<IllegalArgumentException> {
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingId = "1",
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                            brukerSvar = lagUtdatertFormatBrukerSvar(),
                        ),
                )
            }
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
                    sykFoerSykmeldingen =
                        FormSporsmalSvar(
                            sporsmaltekst = "Var du syk før sykmeldingen?",
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
                sporsmaltekst shouldBe "Er opplysningene riktige?"
                svar shouldBe JaEllerNei.JA
            }
            brukerSvar.uriktigeOpplysninger.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Er det noen uriktige opplysninger?"
                svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
            }

            brukerSvar.erOpplysningeneRiktige.run {
                sporsmaltekst shouldBe "Er opplysningene riktige?"
                svar shouldBe JaEllerNei.JA
            }
            brukerSvar.arbeidssituasjon.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Hva er din arbeidssituasjon?"
                svar shouldBe ArbeidssituasjonDTO.ARBEIDSTAKER
            }
            brukerSvar.arbeidsgiverOrgnummer.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Hva er arbeidsgiverens organisasjonsnummer?"
                svar shouldBe "123456789"
            }
            brukerSvar.riktigNarmesteLeder.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Er dette riktig nærmeste leder?"
                svar shouldBe JaEllerNei.JA
            }
            brukerSvar.sykFoerSykmeldingen.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Var du syk før sykmeldingen?"
                svar shouldBe JaEllerNei.JA
            }
            brukerSvar.harBruktEgenmelding.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Har du brukt egenmelding?"
                svar shouldBe JaEllerNei.JA
            }
            brukerSvar.egenmeldingsperioder.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Hvilke egenmeldingsperioder har du hatt?"
                svar shouldBe
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
                sporsmaltekst shouldBe "Har du forsikring?"
                svar shouldBe JaEllerNei.JA
            }
            brukerSvar.egenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Hvilke egenmeldingsdager har du hatt?"
                svar shouldBe listOf(LocalDate.parse("2021-01-01"))
            }
            brukerSvar.harBruktEgenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst shouldBe "Har du brukt egenmeldingsdager?"
                svar shouldBe JaEllerNei.JA
            }
            brukerSvar.fisker.shouldNotBeNull().run {
                blad.run {
                    sporsmaltekst shouldBe "Hvilket blad?"
                    svar shouldBe Blad.A
                }
                lottOgHyre.run {
                    sporsmaltekst shouldBe "Lott eller Hyre?"
                    svar shouldBe LottOgHyre.LOTT
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
                tekst shouldBe "Hva er din arbeidssituasjon?"
                svartype shouldBe SvartypeKafkaDTO.ARBEIDSSITUASJON
                svar shouldBe "ARBEIDSTAKER"
            }
            sporsmals.finnSporsmal(ShortNameKafkaDTO.FRAVAER).shouldNotBeNull().run {
                tekst shouldBe "Har du brukt egenmelding?"
                svartype shouldBe SvartypeKafkaDTO.JA_NEI
                svar shouldBe "JA"
            }
            sporsmals.finnSporsmal(ShortNameKafkaDTO.PERIODE).shouldNotBeNull().run {
                tekst shouldBe "Hvilke egenmeldingsperioder har du hatt?"
                svartype shouldBe SvartypeKafkaDTO.PERIODER
                svar shouldBe """[{"fom":"2025-01-01","tom":"2025-01-05"}]"""
            }
            sporsmals.finnSporsmal(ShortNameKafkaDTO.NY_NARMESTE_LEDER).shouldNotBeNull().run {
                tekst shouldBe "Er dette riktig nærmeste leder?"
                svartype shouldBe SvartypeKafkaDTO.JA_NEI
                svar shouldBe "NEI"
            }
            sporsmals.finnSporsmal(ShortNameKafkaDTO.FORSIKRING).shouldNotBeNull().run {
                tekst shouldBe "Har du forsikring?"
                svartype shouldBe SvartypeKafkaDTO.JA_NEI
                svar shouldBe "JA"
            }
        }

        @Test
        fun `arbeidssituasjon FISKER burde bli FISKER dersom HYRE`() {
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
                .svar shouldBe "FISKER"
        }

        @Test
        fun `arbeidssituasjon FISKER burde bli NAERINGSDRIVENDE dersom LOTT`() {
            val sporsmalSvar =
                lagSykmeldingSporsmalSvarDto(
                    arbeidssituasjon = lagFormSporsmalSvar(ArbeidssituasjonDTO.FISKER),
                    fisker =
                        lagFiskerSvar(
                            lottOgHyre = lagFormSporsmalSvar(LottOgHyre.LOTT),
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
                .svar shouldBe "NAERINGSDRIVENDE"
        }

        @Test
        fun `arbeidssituasjon FISKER burde bli FISKER dersom LOTT OG HYRE`() {
            val sporsmalSvar =
                lagSykmeldingSporsmalSvarDto(
                    arbeidssituasjon = lagFormSporsmalSvar(ArbeidssituasjonDTO.FISKER),
                    fisker =
                        lagFiskerSvar(
                            lottOgHyre = lagFormSporsmalSvar(LottOgHyre.BEGGE),
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
                .svar shouldBe "FISKER"
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
                .svar shouldBe "NAERINGSDRIVENDE"
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
                .svar shouldBe "NEI"
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
                .svar shouldBe "NEI"
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
                .svar shouldBe "NEI"
        }

        private fun List<SporsmalKafkaDTO>.finnSporsmal(shortName: ShortNameKafkaDTO): SporsmalKafkaDTO? =
            this.find {
                it.shortName ==
                    shortName
            }
    }
}
