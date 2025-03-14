package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.utils.objectMapper
import java.time.LocalDate

data class Sporsmal(
    val id: String? = null,
    val tag: SporsmalTag,
    val sporsmalstekst: String? = null,
    val svartype: Svartype,
    val undersporsmal: List<Sporsmal> = emptyList(),
    val svar: List<Svar> = emptyList(),
) {
    val forsteSvarVerdi: String?
        get() = svar.firstOrNull()?.verdi

    fun perioderSvar(): List<PeriodeSvar> {
        require(svartype == Svartype.PERIODER) { "Spørsmålet er ikke av typen PERIODER" }
        return svar.map { objectMapper.readValue(it.verdi) }
    }
}

data class PeriodeSvar(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class Svar(
    val id: String? = null,
    val verdi: String,
)

enum class SporsmalTag {
    GRUPPE_AV_UNDERSPORSMAL,
    ER_OPPLYSNINGENE_RIKTIGE,
    URIKTIGE_OPPLYSNINGER,
    ARBEIDSSITUASJON,
    ARBEIDSGIVER_ORGNUMMER,
    ARBEIDSLEDIG_FRA_ORGNUMMER,
    RIKTIG_NARMESTE_LEDER,
    HAR_BRUKT_EGENMELDING,
    EGENMELDINGSPERIODER,
    HAR_FORSIKRING,
    EGENMELDINGSDAGER,
    HAR_BRUKT_EGENMELDINGSDAGER,
    FISKER,
    FISKER__BLAD,
    FISKER__LOTT_OG_HYRE,
}

enum class Svartype {
    JA_NEI,
    CHECKBOX,
    CHECKBOX_GRUPPE,
    CHECKBOX_PANEL,
    DATO,
    PERIODE,
    PERIODER,
    TIMER,
    FRITEKST,
    LAND,
    COMBOBOX_SINGLE,
    COMBOBOX_MULTI,
    IKKE_RELEVANT,
    GRUPPE_AV_UNDERSPORSMAL,
    BEKREFTELSESPUNKTER,
    OPPSUMMERING,
    PROSENT,
    RADIO_GRUPPE,
    RADIO_GRUPPE_TIMER_PROSENT,
    RADIO_GRUPPE_UKEKALENDER,
    RADIO,
    TALL,
    INFO_BEHANDLINGSDAGER,
    KVITTERING,
    DATOER,
    BELOP,
    KILOMETER,
}
