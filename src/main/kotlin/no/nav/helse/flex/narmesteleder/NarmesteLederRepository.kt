package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface NarmesteLederRepository : CrudRepository<NarmesteLeder, String> {
    fun findByNarmesteLederId(narmesteLederId: UUID): NarmesteLeder?

    fun findAllByBrukerFnrIn(identer: List<String>): List<NarmesteLeder>
}
