package no.nav.helse.flex.sykmelding

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingLeserTest : FakesTestOppsett() {
    @Autowired
    private lateinit var sykmeldingLeser: SykmeldingLeser

    @AfterEach
    fun reset() {
        sykmeldingRepository.deleteAll()
    }

    @Nested
    inner class HentSykmelding {
        @Test
        fun `burde hente en sykmelding`() {
            sykmeldingRepository.save(
                lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr"))),
            )
            shouldNotThrow<Exception> {
                sykmeldingLeser.hentSykmelding("1", identer = PersonIdenter("fnr"))
            }
        }

        @Test
        fun `burde feile dersom sykmelding ikke finnes`() {
            shouldThrow<SykmeldingIkkeFunnetException> {
                sykmeldingLeser.hentSykmelding("1", identer = PersonIdenter("fnr"))
            }
        }

        @Test
        fun `burde feile dersom feil identer`() {
            sykmeldingRepository.save(
                lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr"))),
            )
            shouldThrow<SykmeldingErIkkeDinException> {
                sykmeldingLeser.hentSykmelding("1", identer = PersonIdenter("annet_fnr"))
            }
        }
    }

    @Nested
    inner class HentAlleSykmeldinger {
        @Test
        fun `burde hente alle sykmeldinger`() {
            sykmeldingRepository.save(
                lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr"))),
            )
            sykmeldingRepository.save(
                lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", pasient = lagPasient(fnr = "fnr"))),
            )

            val sykmeldinger = sykmeldingLeser.hentAlleSykmeldinger(identer = PersonIdenter("fnr"))

            sykmeldinger shouldHaveSize 2
        }

        @Test
        fun `burde ikke hente sykmeldinger for andre identer`() {
            sykmeldingRepository.save(
                lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr"))),
            )
            sykmeldingRepository.save(
                lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", pasient = lagPasient(fnr = "fnr_2"))),
            )

            val sykmeldinger = sykmeldingLeser.hentAlleSykmeldinger(identer = PersonIdenter("fnr"))

            sykmeldinger shouldHaveSize 1
        }
    }
}
