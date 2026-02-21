package no.nav.helse.flex.smregmapping

import no.nav.helse.flex.sykmeldingbekreftelse.SykmeldingBekreftelseDto
import org.springframework.stereotype.Component

@Component
class SykmeldingBekreftelseKonsument {
    fun handterSykmeldingBekreftelse(
        sykmeldingId: String,
        sykmeldingBekreftelse: SykmeldingBekreftelseDto?,
    ) {
    }
}
