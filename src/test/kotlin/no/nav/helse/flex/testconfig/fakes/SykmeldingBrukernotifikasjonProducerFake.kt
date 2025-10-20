package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.gateways.SykmeldingBrukernotifikasjonProducer
import no.nav.helse.flex.gateways.SykmeldingNotifikasjon

class SykmeldingBrukernotifikasjonProducerFake : SykmeldingBrukernotifikasjonProducer {
    private val notifikasjoner = mutableListOf<SykmeldingNotifikasjon>()

    fun hentSykmeldingBrukernotifikasjoner(): List<SykmeldingNotifikasjon> = notifikasjoner.toList()

    fun reset() {
        notifikasjoner.clear()
    }

    override fun produserSykmeldingBrukernotifikasjon(sykmeldingNotifikasjon: SykmeldingNotifikasjon) {
        notifikasjoner.add(sykmeldingNotifikasjon)
    }
}
