package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake
import no.nav.helse.flex.testutils.uuidIdGenerator
import java.util.*

class NarmesteLederRepositoryFake :
    AbstractCrudRepositoryFake<NarmesteLeder, String>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
        lagId = uuidIdGenerator(),
    ),
    NarmesteLederRepository {
    override fun findByNarmesteLederId(narmesteLederId: UUID): NarmesteLeder? =
        entities.values.find { it.narmesteLederId == narmesteLederId }

    override fun findAllByBrukerFnrIn(identer: List<String>): List<NarmesteLeder> = entities.values.filter { it.brukerFnr in identer }
}
