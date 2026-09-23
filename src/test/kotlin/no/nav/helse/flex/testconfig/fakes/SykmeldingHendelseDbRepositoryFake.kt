package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.sykmelding.SykmeldingHendelseDbRecord
import no.nav.helse.flex.sykmelding.SykmeldingHendelseDbRepository
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake
import no.nav.helse.flex.testutils.uuidIdGenerator

class SykmeldingHendelseDbRepositoryFake :
    AbstractCrudRepositoryFake<SykmeldingHendelseDbRecord, String>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
        lagId = uuidIdGenerator(),
    ),
    SykmeldingHendelseDbRepository {
    override fun findAllBySykmeldingId(sykmeldingUuid: String): List<SykmeldingHendelseDbRecord> =
        this.entities.values.filter { it.sykmeldingId == sykmeldingUuid }

    override fun findAllBySykmeldingIdIn(sykmeldingUuid: Collection<String>): List<SykmeldingHendelseDbRecord> =
        this.entities.values.filter {
            it.sykmeldingId in sykmeldingUuid
        }
}
