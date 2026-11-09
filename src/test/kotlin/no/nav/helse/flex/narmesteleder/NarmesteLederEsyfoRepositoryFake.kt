package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLederEsyfo
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake
import no.nav.helse.flex.testutils.uuidIdGenerator
import java.util.UUID

class NarmesteLederEsyfoRepositoryFake :
    AbstractCrudRepositoryFake<NarmesteLederEsyfo, String>(
        getEntityId = { it.id },
        setEntityId = { leder, id -> leder.copy(id = id) },
        lagId = uuidIdGenerator(),
    ),
    NarmesteLederEsyfoRepository {
    override fun findByNarmesteLederId(narmesteLederId: UUID) = entities.values.find { it.narmesteLederId == narmesteLederId }

    override fun findAllByBrukerFnrIn(identer: List<String>) = entities.values.filter { it.brukerFnr in identer }

    override fun findAllByBrukerFnrInAndAktivTomIsNull(identer: List<String>) =
        entities.values.filter { it.brukerFnr in identer && it.aktivTom == null }

    override fun deleteByNarmesteLederId(narmesteLederId: UUID) {
        findByNarmesteLederId(narmesteLederId)?.let { delete(it) }
    }
}
