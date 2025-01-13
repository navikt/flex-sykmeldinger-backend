package no.nav.helse.flex.sykmelding.domain

import kotlin.collections.filter
import kotlin.collections.find

class SykmeldingRepositoryFake : ISykmeldingRepository {
    private val lagretSykmelding: MutableList<Sykmelding> = mutableListOf()

    override fun save(sykmelding: Sykmelding) {
        lagretSykmelding.add(sykmelding)
    }

    override fun findBySykmeldingId(id: String): Sykmelding? {
        return lagretSykmelding.find { it.sykmeldingGrunnlag.id == id }
    }

    override fun findAllByFnr(fnr: String): List<Sykmelding> {
        return lagretSykmelding.filter { it.sykmeldingGrunnlag.pasient.fnr == fnr }
    }

    override fun findAll(): List<Sykmelding> {
        return lagretSykmelding
    }

    override fun deleteAll() {
        lagretSykmelding.clear()
    }
}
