package no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient

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
    val yrke: Kodeverksentitet,
    val avtaltStillingsprosent: Int,
    val permisjonsprosent: Int? = null,
    val permitteringsprosent: Int? = null,
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
)

enum class ArbeidsstedType {
    Underenhet,
    Person,
}

data class Opplysningspliktig(
    val type: String,
    val identer: List<Ident>,
)

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
