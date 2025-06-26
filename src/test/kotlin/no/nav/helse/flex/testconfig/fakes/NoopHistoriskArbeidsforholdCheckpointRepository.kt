package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.jobber.HistoriskArbeidsforholdCheckpoint
import no.nav.helse.flex.jobber.HistoriskArbeidsforholdCheckpointRepository
import java.util.Optional

class NoopHistoriskArbeidsforholdCheckpointRepository : HistoriskArbeidsforholdCheckpointRepository {
    override fun findTop1000ByHentetArbeidsforholdIs(hentetArbeidsforhold: Boolean): List<HistoriskArbeidsforholdCheckpoint> {
        TODO("Not yet implemented")
    }

    override fun <S : HistoriskArbeidsforholdCheckpoint?> save(entity: S & Any): S & Any {
        TODO("Not yet implemented")
    }

    override fun <S : HistoriskArbeidsforholdCheckpoint?> saveAll(entities: Iterable<S?>): Iterable<S?> {
        TODO("Not yet implemented")
    }

    override fun findById(id: String): Optional<HistoriskArbeidsforholdCheckpoint?> {
        TODO("Not yet implemented")
    }

    override fun existsById(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun findAll(): Iterable<HistoriskArbeidsforholdCheckpoint?> {
        TODO("Not yet implemented")
    }

    override fun findAllById(ids: Iterable<String?>): Iterable<HistoriskArbeidsforholdCheckpoint?> {
        TODO("Not yet implemented")
    }

    override fun count(): Long {
        TODO("Not yet implemented")
    }

    override fun deleteById(id: String) {
        TODO("Not yet implemented")
    }

    override fun delete(entity: HistoriskArbeidsforholdCheckpoint) {
        TODO("Not yet implemented")
    }

    override fun deleteAllById(ids: Iterable<String?>) {
        TODO("Not yet implemented")
    }

    override fun deleteAll(entities: Iterable<HistoriskArbeidsforholdCheckpoint?>) {
        TODO("Not yet implemented")
    }

    override fun deleteAll() {
        TODO("Not yet implemented")
    }
}
