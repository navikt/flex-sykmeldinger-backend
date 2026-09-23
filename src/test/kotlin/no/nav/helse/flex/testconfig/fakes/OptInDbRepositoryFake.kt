package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.optin.OptInDbRecord
import no.nav.helse.flex.optin.OptInDbRepository
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake
import no.nav.helse.flex.testutils.sekvensIdGenerator

class OptInDbRepositoryFake :
    AbstractCrudRepositoryFake<OptInDbRecord, Long>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
        lagId = sekvensIdGenerator(),
    ),
    OptInDbRepository {
    override fun findAllBySykmeldingId(sykmeldingId: String): List<OptInDbRecord> =
        entities.values.filter { it.sykmeldingId == sykmeldingId }
}
