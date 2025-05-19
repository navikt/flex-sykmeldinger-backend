package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.sykmelding.domain.SykmeldingDbRecord
import no.nav.helse.flex.sykmelding.domain.SykmeldingDbRepository
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake

class SykmeldingDbRepositoryFake :
    AbstractCrudRepositoryFake<SykmeldingDbRecord>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
    ),
    SykmeldingDbRepository {
    override fun findAllByFnrIn(identer: List<String>): List<SykmeldingDbRecord> = this.entities.values.filter { it.fnr in identer }

    override fun findBySykmeldingId(sykmeldingUuid: String): SykmeldingDbRecord? =
        this.entities.values.find {
            it.sykmeldingId ==
                sykmeldingUuid
        }
}
