package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import java.util.*

class NarmesteLederRepositoryFake : NarmesteLederRepository {
    val lagretNarmesteLeder: MutableMap<String, NarmesteLeder> = mutableMapOf()

    private fun lagId(): String = UUID.randomUUID().toString()

    override fun <S : NarmesteLeder?> save(entity: S & Any): S & Any {
        val entityWithId =
            if (entity.id == null) {
                entity.copy(id = lagId())
            } else {
                entity
            }
        lagretNarmesteLeder[entityWithId.id!!] = entityWithId
        @Suppress("UNCHECKED_CAST")
        return entityWithId as (S & Any)
    }

    override fun <S : NarmesteLeder> saveAll(entities: MutableIterable<S>): MutableIterable<S> {
        entities.forEach {
            save(it)
        }
        return entities
    }

    override fun findByNarmesteLederId(narmesteLederId: UUID): NarmesteLeder? =
        lagretNarmesteLeder.values.find { it.narmesteLederId == narmesteLederId }

    override fun findAllByBrukerFnr(fnr: String): List<NarmesteLeder> = lagretNarmesteLeder.values.filter { it.brukerFnr == fnr }

    override fun findById(id: String): Optional<NarmesteLeder> = Optional.ofNullable(lagretNarmesteLeder.values.find { it.id == id })

    override fun existsById(id: String): Boolean = lagretNarmesteLeder.values.any { it.id == id }

    override fun findAll(): MutableIterable<NarmesteLeder> = lagretNarmesteLeder.values

    override fun findAllById(ids: MutableIterable<String>): MutableIterable<NarmesteLeder> =
        lagretNarmesteLeder.values.filter { it.id in ids }.toMutableList()

    override fun count(): Long = lagretNarmesteLeder.size.toLong()

    override fun deleteById(id: String) {
        lagretNarmesteLeder.remove(id)
    }

    override fun delete(entity: NarmesteLeder) {
        lagretNarmesteLeder.remove(entity.id)
    }

    override fun deleteAllById(ids: MutableIterable<String>) {
        ids.forEach { deleteById(it) }
    }

    override fun deleteAll(entities: MutableIterable<NarmesteLeder>) {
        entities.forEach { delete(it) }
    }

    override fun deleteAll() {
        lagretNarmesteLeder.clear()
    }
}
