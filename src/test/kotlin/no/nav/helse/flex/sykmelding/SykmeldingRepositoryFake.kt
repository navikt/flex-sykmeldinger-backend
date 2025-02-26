package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import java.util.*

class SykmeldingRepositoryFake : ISykmeldingRepository {
    private val lagretSykmelding: MutableMap<String, Sykmelding> = mutableMapOf()

    private fun lagId(): String = UUID.randomUUID().toString()

    override fun save(sykmelding: Sykmelding): Sykmelding {
        val sykmeldingMedId =
            if (sykmelding.databaseId == null) {
                sykmelding.copy(databaseId = lagId())
            } else {
                sykmelding
            }
        lagretSykmelding[sykmeldingMedId.databaseId!!] = sykmeldingMedId
        return sykmeldingMedId
    }

    override fun findBySykmeldingId(id: String): Sykmelding? = lagretSykmelding.values.find { it.sykmeldingGrunnlag.id == id }

    override fun findAllByPersonIdenter(identer: PersonIdenter): List<Sykmelding> =
        lagretSykmelding.values.filter {
            it.sykmeldingGrunnlag.pasient.fnr in
                identer.alle()
        }

    override fun findAll(): List<Sykmelding> = lagretSykmelding.values.toList()

    override fun deleteAll() {
        lagretSykmelding.clear()
    }
}
