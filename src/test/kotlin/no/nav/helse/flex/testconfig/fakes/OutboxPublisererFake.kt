package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.gateways.SykmeldingBrukernotifikasjonProducer
import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import no.nav.helse.flex.outbox.OutboxPubliserer
import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelsePubliserer

class OutboxPublisererFake(
    private val sykmeldingLeser: SykmeldingLeser,
    private val sykmeldingHendelsePubliserer: SykmeldingHendelsePubliserer,
    private val sykmeldingBrukernotifikasjonProducer: SykmeldingBrukernotifikasjonProducer,
) : OutboxPubliserer {
    override fun outboxSykmeldingHendelse(
        fnr: String,
        sykmeldingId: String,
        sykmeldingHendelseId: String,
    ) {
        val sykmelding = sykmeldingLeser.hentSykmelding(sykmeldingId)!!
        sykmeldingHendelsePubliserer.publiserSisteHendelse(sykmelding)
    }

    override fun outboxSykmeldingBrukernotifikasjon(sykmeldingNotifikasjon: SykmeldingNotifikasjon) {
        sykmeldingBrukernotifikasjonProducer.produserSykmeldingBrukernotifikasjon(sykmeldingNotifikasjon)
    }

    override fun sendUsendteForEldsteLedigeFnr() {
        TODO("Not yet implemented")
    }
}
