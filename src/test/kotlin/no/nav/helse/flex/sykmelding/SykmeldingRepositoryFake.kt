package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.Sykmelding

class SykmeldingRepositoryFake : ISykmeldingRepository {
    private val lagretSykmelding: MutableList<Sykmelding> = mutableListOf()

    override fun save(sykmelding: Sykmelding) {
        lagretSykmelding.add(sykmelding)
    }

    override fun findBySykmeldingId(id: String): Sykmelding? = lagretSykmelding.find { it.sykmeldingGrunnlag.id == id }

    override fun findAllByFnr(fnr: String): List<Sykmelding> = lagretSykmelding.filter { it.sykmeldingGrunnlag.pasient.fnr == fnr }

    override fun findAll(): List<Sykmelding> = lagretSykmelding

    override fun deleteAll() {
        lagretSykmelding.clear()
    }
}
