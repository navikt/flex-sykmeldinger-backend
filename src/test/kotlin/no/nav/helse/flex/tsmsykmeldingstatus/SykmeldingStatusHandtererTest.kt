package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.sykmelding.SykmeldingHendelseException
import no.nav.helse.flex.sykmeldinghendelse.ArbeidstakerTilleggsinfo
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.AdvisoryLockFake
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

@Suppress("ktlint:standard:max-line-length")
class SykmeldingStatusHandtererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusHandterer: SykmeldingStatusHandterer

    @Autowired
    lateinit var sykmeldingStatusBuffer: SykmeldingStatusBuffer

    @Autowired
    lateinit var advisoryLockFake: AdvisoryLockFake

    @Autowired
    lateinit var aaregClient: AaregClientFake

    @AfterEach
    fun cleanUp() {
        slettDatabase()
        advisoryLockFake.reset()
        aaregClient.reset()
    }

    @Test
    fun `burde lagre hendelse på sykmelding`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be true`()
        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        sykmelding.`should not be null`()
        sykmelding.hendelser.size shouldBeEqualTo 2
        sykmelding.sisteHendelse().status shouldBeEqualTo HendelseStatus.SENDT_TIL_ARBEIDSGIVER
    }

    @Test
    fun `burde ikke lagre hendelse dersom sykmelding ikke finnes`() {
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be false`()
        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        sykmelding.`should be null`()
    }

    @Test
    fun `burde ignorere APEN status`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            hendelser.shouldHaveSize(1)
        }
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "APEN",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            hendelser.shouldHaveSize(1)
        }
    }

    @Test
    fun `burde returnere true ved APEN status`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "APEN",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).shouldBeTrue()
    }

    @Test
    fun `burde ignorere SLETTET status`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            hendelser.shouldHaveSize(1)
        }
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SLETTET",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            hendelser.shouldHaveSize(1)
        }
    }

    @Test
    fun `burde returnere true ved SLETTET status`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SLETTET",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be true`()
    }

    @Test
    fun `burde sammenstille data til SykmeldingStatusKafkaMessageDTO`() {
        val sykmeldingStatusKafkaDTO: SykmeldingStatusKafkaDTO = lagSykmeldingStatusKafkaMessageDTO().event
        val sammenstillSykmeldingStatusKafkaMessageDTO =
            SykmeldingStatusHandterer.sammenstillSykmeldingStatusKafkaMessageDTO(
                fnr = "fnr",
                sykmeldingStatusKafkaDTO = sykmeldingStatusKafkaDTO,
            )
        sammenstillSykmeldingStatusKafkaMessageDTO.kafkaMetadata.`should not be null`()
        sammenstillSykmeldingStatusKafkaMessageDTO.event.brukerSvar.`should not be null`()
    }

    @Test
    fun `burde buffre status dersom sykmelding ikke finnes`() {
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        val buffredeHendelser = sykmeldingStatusBuffer.kikkPaaAlleFor("1")
        buffredeHendelser.size shouldBeEqualTo 1
        buffredeHendelser.first().kafkaMetadata.sykmeldingId shouldBeEqualTo "1"
    }

    @Test
    fun `burde ikke buffre status dersom sykmelding finnes`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        val buffredeHendelser = sykmeldingStatusBuffer.kikkPaaAlleFor("1")
        buffredeHendelser.size shouldBeEqualTo 0
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `handterSykmeldingStatus og prosesserSykmeldingStatuserFraBuffer burde synkronisere ved buffer lås`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        sykmeldingStatusHandterer.handterSykmeldingStatus(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
        )
        advisoryLockFake.lockCount() `should be equal to` 1
        sykmeldingStatusHandterer.prosesserSykmeldingStatuserFraBuffer(sykmeldingId = "1")
        advisoryLockFake.lockCount() `should be equal to` 2
    }

    @Test
    fun `burde deduplisere statuser`() {
        sykmeldingRepository.save(
            lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")).leggTilHendelse(
                lagSykmeldingHendelse(
                    status = HendelseStatus.SENDT_TIL_NAV,
                    hendelseOpprettet = Instant.parse("2025-07-01T12:00:00Z"),
                ),
            ),
        )
        sykmeldingStatusHandterer
            .handterSykmeldingStatus(
                lagSykmeldingStatusKafkaMessageDTO(
                    kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                    event =
                        lagSykmeldingStatusKafkaDTO(
                            statusEvent = "BEKREFTET",
                            timestamp = OffsetDateTime.parse("2025-07-01T12:00:00+00:00"),
                        ),
                ),
            ).`should be false`()

        val lagretSykmelding = sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
        lagretSykmelding.hendelser.filter { it.status == HendelseStatus.SENDT_TIL_NAV } shouldHaveSize 1
    }

    @Test
    fun `burde deduplisere statuser med litt ulik timestamp`() {
        sykmeldingRepository.save(
            lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")).leggTilHendelse(
                lagSykmeldingHendelse(
                    status = HendelseStatus.SENDT_TIL_NAV,
                    hendelseOpprettet = Instant.parse("2025-07-01T12:00:00Z"),
                ),
            ),
        )
        sykmeldingStatusHandterer
            .handterSykmeldingStatus(
                lagSykmeldingStatusKafkaMessageDTO(
                    kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                    event =
                        lagSykmeldingStatusKafkaDTO(
                            statusEvent = "BEKREFTET",
                            timestamp = OffsetDateTime.parse("2025-07-01T12:00:01+00:00"),
                        ),
                ),
            ).`should be false`()

        val lagretSykmelding = sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
        lagretSykmelding.hendelser.filter { it.status == HendelseStatus.SENDT_TIL_NAV } shouldHaveSize 1
    }

    @Test
    fun `burde feile når ny status er eldre enn siste hendelse, og de er fra forskjellige systemer`() {
        val sykmelding =
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
            ).leggTilHendelse(
                lagSykmeldingHendelse(
                    status = HendelseStatus.AVBRUTT,
                    hendelseOpprettet = Instant.parse("2025-07-01T17:00:00Z"),
                ),
            )

        sykmeldingRepository.save(sykmelding)
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata =
                    lagKafkaMetadataDTO(
                        sykmeldingId = "1",
                    ),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "AVBRUTT",
                        timestamp = OffsetDateTime.parse("2025-07-01T12:00:00+00:00"),
                    ),
            )
        invoking {
            sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        } `should throw` SykmeldingHendelseException::class
    }

    @Test
    fun `burde akseptere at ny status er eldre enn siste hendelse, dersom begge er fra samme system`() {
        val sykmelding =
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
            ).leggTilHendelse(
                lagSykmeldingHendelse(
                    status = HendelseStatus.AVBRUTT,
                    hendelseOpprettet = Instant.parse("2025-07-01T17:00:00Z"),
                    source = "source",
                ),
            )

        sykmeldingRepository.save(sykmelding)
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata =
                    lagKafkaMetadataDTO(
                        sykmeldingId = "1",
                        source = "source",
                    ),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "AVBRUTT",
                        timestamp = OffsetDateTime.parse("2025-07-01T12:00:00+00:00"),
                    ),
            )

        sykmeldingStatusHandterer.handterSykmeldingStatus(status).shouldBeTrue()
    }

    @Test
    fun `burde ignorere når sykmeldingens siste hendelse er APEN, selv om den er laget etter kafka-status`() {
        val sykmelding =
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.APEN,
                            hendelseOpprettet = Instant.parse("2025-01-01T12:00:00Z"),
                        ),
                    ),
            )
        sykmeldingRepository.save(sykmelding)
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata =
                    lagKafkaMetadataDTO(
                        sykmeldingId = "1",
                        timestamp = OffsetDateTime.parse("2024-01-01T12:00:00+00:00"),
                    ),
            )
        invoking {
            sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        } `should not throw` SykmeldingHendelseException::class
    }

    @Test
    fun `burde korrigere manglende juridiskOrgnummer`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr")),
            ),
        )

        aaregClient.setArbeidsforholdoversikt(
            arbeidsforhold =
                lagArbeidsforholdOversiktResponse(
                    arbeidsforholdoversikter =
                        listOf(
                            lagArbeidsforholdOversikt(
                                arbeidstakerIdenter = listOf("fnr"),
                                arbeidsstedOrgnummer = "org-nr",
                                opplysningspliktigOrgnummer = "juridisk-org-nr",
                            ),
                        ),
                ),
            fnr = "fnr",
        )

        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata =
                    lagKafkaMetadataDTO(
                        sykmeldingId = "1",
                    ),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SENDT",
                        sykmeldingId = "1",
                        arbeidsgiver =
                            lagArbeidsgiverStatusKafkaDTO(
                                orgnummer = "org-nr",
                                juridiskOrgnummer = null,
                            ),
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        val oppdatertSykmelding = sykmeldingRepository.findBySykmeldingId("1")
        oppdatertSykmelding.shouldNotBeNull().sisteHendelse().tilleggsinfo.shouldBeInstanceOf<ArbeidstakerTilleggsinfo>().run {
            arbeidsgiver.juridiskOrgnummer shouldBeEqualTo "juridisk-org-nr"
        }
    }

    @Test
    fun `burde ikke korrigere juridiskOrgnummer dersom satt`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr")),
            ),
        )

        aaregClient.setArbeidsforholdoversikt(
            arbeidsforhold =
                lagArbeidsforholdOversiktResponse(
                    arbeidsforholdoversikter =
                        listOf(
                            lagArbeidsforholdOversikt(
                                arbeidstakerIdenter = listOf("fnr"),
                                arbeidsstedOrgnummer = "org-nr",
                                opplysningspliktigOrgnummer = "oppdatert-juridisk-orgnr",
                            ),
                        ),
                ),
            fnr = "fnr",
        )

        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata =
                    lagKafkaMetadataDTO(
                        sykmeldingId = "1",
                    ),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SENDT",
                        sykmeldingId = "1",
                        arbeidsgiver =
                            lagArbeidsgiverStatusKafkaDTO(
                                orgnummer = "originalt-juridisk-orgnr",
                                juridiskOrgnummer = null,
                            ),
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        val oppdatertSykmelding = sykmeldingRepository.findBySykmeldingId("1")
        oppdatertSykmelding.shouldNotBeNull().sisteHendelse().tilleggsinfo.shouldBeInstanceOf<ArbeidstakerTilleggsinfo>().run {
            arbeidsgiver.juridiskOrgnummer shouldBeEqualTo "originalt-juridisk-orgnr"
        }
    }
}
