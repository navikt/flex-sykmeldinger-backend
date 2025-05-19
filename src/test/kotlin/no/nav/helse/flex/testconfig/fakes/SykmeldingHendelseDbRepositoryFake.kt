package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake

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
