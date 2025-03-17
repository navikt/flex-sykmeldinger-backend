package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.sykmelding.domain.AnnetArbeidssituasjonTilleggsinfo
import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.Tilleggsinfo

class TilleggsinfoSammenstillerService {
    fun sammenstillTilleggsinfo(sporsmal: List<Sporsmal>): Tilleggsinfo = AnnetArbeidssituasjonTilleggsinfo
}
