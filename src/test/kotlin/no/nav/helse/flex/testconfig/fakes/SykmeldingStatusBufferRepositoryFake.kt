package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.sykmeldingstatusbuffer.SykmeldingStatusBufferDbRecord
import no.nav.helse.flex.sykmeldingstatusbuffer.SykmeldingStatusBufferRepository
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake

class SykmeldingStatusBufferRepositoryFake :
    AbstractCrudRepositoryFake<SykmeldingStatusBufferDbRecord>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
    ),
    SykmeldingStatusBufferRepository {
    override fun findAllBySykmeldingId(sykmeldingId: String): List<SykmeldingStatusBufferDbRecord> =
        entities.values.filter { it.sykmeldingId == sykmeldingId }
}
