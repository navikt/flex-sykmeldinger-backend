package no.nav.helse.flex.sykmeldinghendelsebuffer

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.lagKafkaMetadataDTO
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaDTO
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaMessageDTO
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingHendelseBufferTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var sykmeldingHendelseBuffer: SykmeldingHendelseBuffer

    @Test
    fun `burde legge til sykmeldinghendelse`() {
        val kafkaMelding =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event = lagSykmeldingStatusKafkaDTO(statusEvent = "TEST_STATUS"),
            )
        sykmeldingHendelseBuffer.leggTil(kafkaMelding)
        sykmeldingHendelseBuffer.hentOgFjernAlle(sykmeldingId = "1").run {
            this shouldHaveSize 1
            this.first().run {
                event.statusEvent shouldBeEqualTo "TEST_STATUS"
            }
        }
    }
}
