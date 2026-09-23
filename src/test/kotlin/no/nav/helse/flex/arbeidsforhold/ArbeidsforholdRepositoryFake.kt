package no.nav.helse.flex.arbeidsforhold

import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake
import no.nav.helse.flex.testutils.uuidIdGenerator

class ArbeidsforholdRepositoryFake :
    AbstractCrudRepositoryFake<Arbeidsforhold, String>(
        getEntityId = { it.id },
        setEntityId = { entity, id -> entity.copy(id = id) },
        lagId = uuidIdGenerator(),
        uniqueConstraints =
            listOf(
                Arbeidsforhold::navArbeidsforholdId,
            ),
    ),
    ArbeidsforholdRepository {
    override fun findByNavArbeidsforholdId(navArbeidsforholdId: String): Arbeidsforhold? =
        entities.values.find {
            it.navArbeidsforholdId ==
                navArbeidsforholdId
        }

    override fun deleteByNavArbeidsforholdId(navArbeidsforholdId: String) {
        val key = entities.keys.find { entities[it]?.navArbeidsforholdId == navArbeidsforholdId }
        entities.remove(key)
    }

    override fun getAllByFnrIn(identer: List<String>): List<Arbeidsforhold> = entities.values.filter { it.fnr in identer }
}
