package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.arbeidsforhold.manuellsynk.SynkroniserArbeidsforhold
import no.nav.helse.flex.arbeidsforhold.manuellsynk.TempSynkroniserArbeidsforholdRepository
import no.nav.helse.flex.testutils.AbstractCrudRepositoryFake

class TempSynkroniserArbeidsforholdRepositoryFake :
    AbstractCrudRepositoryFake<SynkroniserArbeidsforhold>(
        getEntityId = { it.id },
        setEntityId = { s, id -> s.copy(id = id) },
    ),
    TempSynkroniserArbeidsforholdRepository {
    override fun findNextBatch(batchSize: Int): List<SynkroniserArbeidsforhold> =
        entities.values
            .filter { !it.lest }
            .take(batchSize)
}
