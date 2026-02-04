package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.SykmeldingDTO

object SykmeldingDtoRegler {
    fun skjermForPasientDersomSpesifisert(sykmeldingDto: SykmeldingDTO): SykmeldingDTO =
        if (sykmeldingDto.skjermesForPasient) {
            sykmeldingDto.copy(
                medisinskVurdering = null,
                utdypendeOpplysninger = emptyMap(),
                tiltakNAV = null,
                andreTiltak = null,
                meldingTilNAV = null,
            )
        } else {
            sykmeldingDto
        }
}
