package no.nav.helse.flex.sykmelding.api

import no.nav.helse.flex.sykmelding.domain.Sykmelding

class SykmeldingDtoKonverterer {
    fun konverter(sykmelding: Sykmelding): SykmeldingDto = SykmeldingDto(sykmeldingId = sykmelding.sykmeldingId)
}
