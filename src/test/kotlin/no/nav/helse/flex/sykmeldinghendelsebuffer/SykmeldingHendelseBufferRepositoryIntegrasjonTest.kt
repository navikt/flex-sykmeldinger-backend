package no.nav.helse.flex.sykmeldinghendelsebuffer

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

class SykmeldingHendelseBufferRepositoryIntegrasjonTest : IntegrasjonTestOppsett() {
    @AfterEach
    fun afterEach() {
        sykmeldingHendelseBufferRepository.deleteAll()
    }

    @Test
    fun `burde lagre og hente`() {
        val record =
            SykmeldingHendelseBufferDbRecord(
                sykmeldingId = "1",
                sykmeldingStatusOpprettet = Instant.parse("2023-01-01T00:00:00Z"),
                sykmeldingStatusKafkaMessage =
                    PGobject().apply {
                        type = "json"
                        value = """{"key": "val"}"""
                    },
                lokaltOpprettet = Instant.parse("2024-01-01T00:00:00Z"),
            )

        val opprettetRecord = sykmeldingHendelseBufferRepository.save(record)
        opprettetRecord.copy(id = null) `should be equal to` record

        val lagretRecord = sykmeldingHendelseBufferRepository.findById(opprettetRecord.id!!).getOrNull()
        lagretRecord
            .shouldNotBeNull()
            .copy(id = null) `should be equal to` record
    }

    @Test
    fun `test findAllBySykmeldingId`() {
        listOf(
            lagSykmeldingHendelseBufferDbRecord(sykmeldingId = "1"),
            lagSykmeldingHendelseBufferDbRecord(sykmeldingId = "1"),
            lagSykmeldingHendelseBufferDbRecord(sykmeldingId = "2"),
        ).forEach { sykmeldingHendelseBufferRepository.save(it) }

        val records = sykmeldingHendelseBufferRepository.findAllBySykmeldingId("1")
        records.size `should be equal to` 2
    }

    private fun lagSykmeldingHendelseBufferDbRecord(sykmeldingId: String = "1"): SykmeldingHendelseBufferDbRecord =
        SykmeldingHendelseBufferDbRecord(
            sykmeldingId = sykmeldingId,
            sykmeldingStatusOpprettet = Instant.parse("2023-01-01T00:00:00Z"),
            sykmeldingStatusKafkaMessage =
                PGobject().apply {
                    type = "json"
                    value = """{"key": "val"}"""
                },
            lokaltOpprettet = Instant.parse("2024-01-01T00:00:00Z"),
        )
}
