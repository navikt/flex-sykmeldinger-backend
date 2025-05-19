package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.sykmelding.domain.*
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

class SykmeldingHendelseDbRepositoryFake :
    AbstractCrudRepositoryFake<SykmeldingHendelseDbRecord>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
    ),
    SykmeldingHendelseDbRepository {
    override fun findAllBySykmeldingId(sykmeldingUuid: String): List<SykmeldingHendelseDbRecord> =
        this.entities.values.filter { it.sykmeldingId == sykmeldingUuid }

    override fun findAllBySykmeldingIdIn(sykmeldingUuid: Collection<String>): List<SykmeldingHendelseDbRecord> =
        this.entities.values.filter {
            it.sykmeldingId in sykmeldingUuid
        }
}
