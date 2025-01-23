package no.nav.helse.flex.arbeidsforhold

import java.util.*

class ArbeidsforholdRepositoryFake : ArbeidsforholdRepository {
    private val lagretArbeidsforhold: MutableList<Arbeidsforhold> = mutableListOf()

    private fun lagId(): String = UUID.randomUUID().toString()

    override fun findByNavArbeidsforholdId(navArbeidsforholdId: String): Arbeidsforhold? =
        lagretArbeidsforhold.find {
            it.navArbeidsforholdId ==
                navArbeidsforholdId
        }

    override fun getAllByFnr(fnr: String): List<Arbeidsforhold> = lagretArbeidsforhold.filter { it.fnr == fnr }

    override fun deleteByNavArbeidsforholdId(navArbeidsforholdId: String) {
        lagretArbeidsforhold.removeIf { it.navArbeidsforholdId == navArbeidsforholdId }
    }

    override fun <S : Arbeidsforhold> save(entity: S): S {
        val entityWithId = entity.copy(id = lagId())
        lagretArbeidsforhold.add(entityWithId)
        @Suppress("UNCHECKED_CAST")
        return entityWithId as S
    }

    override fun <S : Arbeidsforhold> saveAll(entities: Iterable<S>): Iterable<S> {
        entities.forEach { save(it) }
        return entities
    }

    override fun findById(id: String): Optional<Arbeidsforhold> =
        Optional.ofNullable(
            lagretArbeidsforhold.find {
                it.navArbeidsforholdId == id
            },
        )

    override fun existsById(id: String): Boolean = lagretArbeidsforhold.any { it.navArbeidsforholdId == id }

    override fun findAll(): Iterable<Arbeidsforhold> = lagretArbeidsforhold

    override fun findAllById(ids: Iterable<String>): Iterable<Arbeidsforhold> =
        lagretArbeidsforhold
            .filter {
                it.navArbeidsforholdId in ids
            }.toMutableList()

    override fun count(): Long = lagretArbeidsforhold.size.toLong()

    override fun deleteById(id: String) {
        lagretArbeidsforhold.removeIf { it.navArbeidsforholdId == id }
    }

    override fun delete(entity: Arbeidsforhold) {
        lagretArbeidsforhold.remove(entity)
    }

    override fun deleteAllById(ids: Iterable<String>) {
        lagretArbeidsforhold.removeIf { it.navArbeidsforholdId in ids }
    }

    override fun deleteAll(entities: Iterable<Arbeidsforhold>) {
        entities.forEach { lagretArbeidsforhold.remove(it) }
    }

    override fun deleteAll() {
        lagretArbeidsforhold.clear()
    }
}
