package no.nav.helse.flex.narmesteleder

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class NarmesteLederLesemodus(
    @param:Value("\${narmeste-leder.lesemodus:GAMMEL}") val modus: String,
    @param:Value("\${narmeste-leder.esyfo-consumer-aktiv:false}") consumerAktiv: Boolean,
) {
    init {
        require(modus == "GAMMEL" || modus == "ESYFO") { "Ukjent lesemodus for nærmeste leder" }
        require(modus != "ESYFO" || consumerAktiv) { "ESYFO-lesing krever aktiv ESYFO-consumer" }
    }
}
