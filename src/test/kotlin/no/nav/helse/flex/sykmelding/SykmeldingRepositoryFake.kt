package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import java.util.*

class SykmeldingRepositoryFake : ISykmeldingRepository {
    private val lagretSykmelding: MutableMap<String, Sykmelding> = mutableMapOf()

    private fun lagId(): String = UUID.randomUUID().toString()

    override fun save(sykmelding: Sykmelding) {
        val sykmeldingMedId =
            if (sykmelding.databaseId == null) {
                sykmelding.copy(databaseId = lagId())
            } else {
                sykmelding
            }
        lagretSykmelding[sykmeldingMedId.databaseId!!] = sykmeldingMedId
    }

    override fun findBySykmeldingId(id: String): Sykmelding? = lagretSykmelding.values.find { it.sykmeldingGrunnlag.id == id }

    override fun findAllByFnr(fnr: String): List<Sykmelding> = lagretSykmelding.values.filter { it.sykmeldingGrunnlag.pasient.fnr == fnr }

    override fun findAll(): List<Sykmelding> = lagretSykmelding.values.toList()

    override fun deleteAll() {
        lagretSykmelding.clear()
    }
}
