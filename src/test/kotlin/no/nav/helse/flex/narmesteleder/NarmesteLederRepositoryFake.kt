package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake
import java.util.*

class NarmesteLederRepositoryFake :
    AbstractCrudRepositoryFake<NarmesteLeder>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
    ),
    NarmesteLederRepository {
    override fun findByNarmesteLederId(narmesteLederId: UUID): NarmesteLeder? =
        entities.values.find { it.narmesteLederId == narmesteLederId }

    override fun findAllByBrukerFnr(fnr: String): List<NarmesteLeder> = entities.values.filter { it.brukerFnr == fnr }
}
