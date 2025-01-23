package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import java.util.*

class NarmesteLederRepositoryFake : NarmesteLederRepository {
    val lagretNarmesteLeder: MutableList<NarmesteLeder> = mutableListOf()

    private fun lagId(): String = UUID.randomUUID().toString()

    override fun findByNarmesteLederId(narmesteLederId: UUID): NarmesteLeder? =
        lagretNarmesteLeder.find { it.narmesteLederId == narmesteLederId }

    override fun <S : NarmesteLeder?> save(entity: S & Any): S & Any {
        val entityWithId = entity.copy(id = lagId())
        lagretNarmesteLeder.add(entityWithId)
        @Suppress("UNCHECKED_CAST")
        return entityWithId as (S & Any)
    }

    override fun <S : NarmesteLeder> saveAll(entities: MutableIterable<S>): MutableIterable<S> {
        entities.forEach {
            val entityWithId = it.copy(id = lagId())
            lagretNarmesteLeder.add(entityWithId)
        }
        return entities
    }

    override fun findById(id: String): Optional<NarmesteLeder> = Optional.ofNullable(lagretNarmesteLeder.find { it.id == id })

    override fun existsById(id: String): Boolean = lagretNarmesteLeder.any { it.id == id }

    override fun findAll(): MutableIterable<NarmesteLeder> = lagretNarmesteLeder

    override fun findAllById(ids: MutableIterable<String>): MutableIterable<NarmesteLeder> =
        lagretNarmesteLeder.filter { it.id in ids }.toMutableList()

    override fun count(): Long = lagretNarmesteLeder.size.toLong()

    override fun deleteById(id: String) {
        lagretNarmesteLeder.removeIf { it.id == id }
    }

    override fun delete(entity: NarmesteLeder) {
        lagretNarmesteLeder.remove(entity)
    }

    override fun deleteAllById(ids: MutableIterable<String>) {
        lagretNarmesteLeder.removeIf { it.id in ids }
    }

    override fun deleteAll(entities: MutableIterable<NarmesteLeder>) {
        entities.forEach { lagretNarmesteLeder.remove(it) }
    }

    override fun deleteAll() {
        lagretNarmesteLeder.clear()
    }
}
