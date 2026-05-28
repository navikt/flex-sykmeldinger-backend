package no.nav.helse.flex.sykmeldinghendelse

import no.nav.helse.flex.api.SykmeldingOptInService
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.SykmeldingErIkkeDinException
import no.nav.helse.flex.sykmelding.UgyldigOptinException
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykepengesoknadBackendClientFake
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingOptInServiceTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingOptInService: SykmeldingOptInService

    @Autowired
    lateinit var sykepengesoknadBackendClient: SykepengesoknadBackendClientFake

    @AfterEach
    fun cleanUp() {
        slettDatabase()
        sykepengesoknadBackendClient.reset()
    }

    @Nested
    inner class BehandleOptIn {
        @Test
        fun `burde kalle opprettOptIn for naeringsdrivende`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    hendelser =
                        listOf(
                            lagSykmeldingHendelse(
                                status = HendelseStatus.SENDT_TIL_NAV,
                                brukerSvar = lagNaringsdrivendeBrukerSvar(),
                            ),
                        ),
                ),
            )

            sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr"))

            sykepengesoknadBackendClient.antallOpprettOptInKall() `should be equal to` 1
        }

        @Test
        fun `burde kalle opprettOptIn for frilanser`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    hendelser =
                        listOf(
                            lagSykmeldingHendelse(
                                status = HendelseStatus.SENDT_TIL_NAV,
                                brukerSvar = lagFrilanserBrukerSvar(),
                            ),
                        ),
                ),
            )

            sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr"))

            sykepengesoknadBackendClient.antallOpprettOptInKall() `should be equal to` 1
        }

        @Test
        fun `burde sende riktig sykmeldingId i kafkamelding`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    hendelser =
                        listOf(
                            lagSykmeldingHendelse(
                                status = HendelseStatus.SENDT_TIL_NAV,
                                brukerSvar = lagNaringsdrivendeBrukerSvar(),
                            ),
                        ),
                ),
            )

            sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr"))

            sykepengesoknadBackendClient.opprettOptInRequests
                .first()
                .kafkaMetadata.sykmeldingId `should be equal to` "1"
        }
    }

    @Nested
    inner class UgyldigStatus {
        @Test
        fun `burde kaste UgyldigOptinException naar status er APEN`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.APEN)),
                ),
            )

            val kall = { sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr")) }

            kall shouldThrow UgyldigOptinException::class
        }

        @Test
        fun `burde kaste UgyldigOptinException naar status er AVBRUTT`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    hendelser = listOf(lagSykmeldingHendelse(status = HendelseStatus.AVBRUTT)),
                ),
            )

            val kall = { sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr")) }

            kall shouldThrow UgyldigOptinException::class
        }
    }

    @Nested
    inner class Tilgangskontroll {
        @Test
        fun `burde kaste SykmeldingErIkkeDinException naar bruker ikke eier sykmeldingen`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "riktig-fnr")),
                    hendelser =
                        listOf(
                            lagSykmeldingHendelse(
                                status = HendelseStatus.SENDT_TIL_NAV,
                                brukerSvar = lagNaringsdrivendeBrukerSvar(),
                            ),
                        ),
                ),
            )

            val kall = { sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("feil-fnr")) }

            kall shouldThrow SykmeldingErIkkeDinException::class
        }

        @Test
        fun `burde ikke kalle opprettOptIn naar bruker ikke eier sykmeldingen`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "riktig-fnr")),
                    hendelser =
                        listOf(
                            lagSykmeldingHendelse(
                                status = HendelseStatus.SENDT_TIL_NAV,
                                brukerSvar = lagNaringsdrivendeBrukerSvar(),
                            ),
                        ),
                ),
            )

            runCatching { sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("feil-fnr")) }

            sykepengesoknadBackendClient.antallOpprettOptInKall() `should be equal to` 0
        }
    }

    @Nested
    inner class UgyldigArbeidssituasjon {
        @Test
        fun `burde kaste UgyldigOptinException naar arbeidssituasjon er ARBEIDSTAKER`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    hendelser =
                        listOf(
                            lagSykmeldingHendelse(
                                status = HendelseStatus.SENDT_TIL_NAV,
                                brukerSvar = lagArbeidsledigBrukerSvar(),
                            ),
                        ),
                ),
            )

            val kall = { sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr")) }

            kall shouldThrow UgyldigOptinException::class
        }

        @Test
        fun `burde ikke kalle opprettOptIn ved ugyldig arbeidssituasjon`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    hendelser =
                        listOf(
                            lagSykmeldingHendelse(
                                status = HendelseStatus.SENDT_TIL_NAV,
                                brukerSvar = lagArbeidsledigBrukerSvar(),
                            ),
                        ),
                ),
            )

            runCatching { sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr")) }

            sykepengesoknadBackendClient.antallOpprettOptInKall() `should be equal to` 0
        }
    }
}
