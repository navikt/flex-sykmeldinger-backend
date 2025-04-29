package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.sykmeldinghendelsebuffer.BuffretSykmeldingHendelseDbRecord
import no.nav.helse.flex.sykmeldinghendelsebuffer.BuffretSykmeldingHendelseRepository
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake

class BuffretSykmeldingHendelseRepositoryFake :
    AbstractCrudRepositoryFake<BuffretSykmeldingHendelseDbRecord>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
    ),
    BuffretSykmeldingHendelseRepository {
    override fun findAllBySykmeldingId(sykmeldingId: String): List<BuffretSykmeldingHendelseDbRecord> =
        entities.values.filter { it.sykmeldingId == sykmeldingId }

    override fun deleteAllBySykmeldingId(sykmeldingId: String): List<BuffretSykmeldingHendelseDbRecord> {
        val records = findAllBySykmeldingId(sykmeldingId)
        deleteAll(records.toMutableList())
        return records
    }

    override fun aquireAdvisoryLock(lockKey: Int) {
        return
    }
}
