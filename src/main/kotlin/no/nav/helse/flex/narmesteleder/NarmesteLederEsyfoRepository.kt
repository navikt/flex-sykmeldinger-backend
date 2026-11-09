package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLederEsyfo
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface NarmesteLederEsyfoRepository : CrudRepository<NarmesteLederEsyfo, String> {
    fun findByNarmesteLederId(narmesteLederId: UUID): NarmesteLederEsyfo?

    fun findAllByBrukerFnrIn(identer: List<String>): List<NarmesteLederEsyfo>

    fun findAllByBrukerFnrInAndAktivTomIsNull(identer: List<String>): List<NarmesteLederEsyfo>

    fun deleteByNarmesteLederId(narmesteLederId: UUID)
}
