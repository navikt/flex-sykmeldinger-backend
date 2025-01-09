package no.nav.helse.flex.sykmelding.domain

class SykmeldingRepositoryFake : ISykmeldingRepository {
    private val lagretSykmelding: MutableList<FlexSykmelding> = mutableListOf()

    override fun save(sykmelding: FlexSykmelding) {
        lagretSykmelding.add(sykmelding)
    }

    override fun findBySykmeldingUuid(id: String): FlexSykmelding? {
        return lagretSykmelding.find { it.sykmelding.id == id }
    }

    override fun findAllByFnr(fnr: String): List<FlexSykmelding> {
        return lagretSykmelding.filter { it.sykmelding.pasient.fnr == fnr }
    }
}
