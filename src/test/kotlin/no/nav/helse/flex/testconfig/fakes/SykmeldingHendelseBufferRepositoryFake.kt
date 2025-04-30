package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.sykmeldinghendelsebuffer.SykmeldingHendelseBufferDbRecord
import no.nav.helse.flex.sykmeldinghendelsebuffer.SykmeldingHendelseBufferRepository
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake

class SykmeldingHendelseBufferRepositoryFake :
    AbstractCrudRepositoryFake<SykmeldingHendelseBufferDbRecord>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
    ),
    SykmeldingHendelseBufferRepository {
    override fun findAllBySykmeldingId(sykmeldingId: String): List<SykmeldingHendelseBufferDbRecord> =
        entities.values.filter { it.sykmeldingId == sykmeldingId }

    override fun deleteAllBySykmeldingId(sykmeldingId: String): List<SykmeldingHendelseBufferDbRecord> {
        val records = findAllBySykmeldingId(sykmeldingId)
        deleteAll(records.toMutableList())
        return records
    }
}
