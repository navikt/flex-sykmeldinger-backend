package no.nav.helse.flex.clients.aareg

import java.time.LocalDate

data class ArbeidsforholdRequest(
    val arbeidstakerId: String,
    val arbeidsforholdtyper: List<String>,
    val arbeidsforholdstatuser: List<String>,
)

data class ArbeidsforholdoversiktResponse(
    val arbeidsforholdoversikter: List<ArbeidsforholdOversikt>,
)

data class ArbeidsforholdOversikt(
    val navArbeidsforholdId: String,
    val type: Kodeverksentitet,
    val arbeidstaker: Arbeidstaker,
    val arbeidssted: Arbeidssted,
    val opplysningspliktig: Opplysningspliktig,
    val startdato: LocalDate,
    val sluttdato: LocalDate? = null,
)

data class Kodeverksentitet(
    val kode: String,
    val beskrivelse: String,
)

data class Arbeidstaker(
    val identer: List<Ident>,
)

data class Arbeidssted(
    val type: ArbeidsstedType,
    val identer: List<Ident>,
) {
    fun finnOrgnummer(): String =
        this.identer
            .firstOrNull { it.type == IdentType.ORGANISASJONSNUMMER }
            ?.ident
            ?: throw IllegalStateException("Arbeidssted mangler organisasjonsnummer")
}

enum class ArbeidsstedType {
    Underenhet,
    Person,
}

data class Opplysningspliktig(
    val type: String,
    val identer: List<Ident>,
) {
    fun finnOrgnummer(): String =
        this.identer
            .firstOrNull { it.type == IdentType.ORGANISASJONSNUMMER }
            ?.ident
            ?: throw IllegalStateException("Opplysningspliktig mangler organisasjonsnummer")
}

data class Ident(
    val type: IdentType,
    val ident: String,
    val gjeldende: Boolean? = null,
)

enum class IdentType {
    AKTORID,
    FOLKEREGISTERIDENT,
    ORGANISASJONSNUMMER,
}
