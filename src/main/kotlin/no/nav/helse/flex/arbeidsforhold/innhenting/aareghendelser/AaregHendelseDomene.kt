package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

data class ArbeidsforholdHendelse(
    val id: Long,
    val endringstype: Endringstype,
    val arbeidsforhold: ArbeidsforholdKafka,
    val entitetsendringer: List<Entitetsendring>,
)

enum class Entitetsendring {
    Ansettelsesdetaljer,
    Ansettelsesperiode,
    Permisjon,
    Permittering,
    TimerMedTimeloenn,
    Utenlandsopphold,

    @JsonEnumDefaultValue
    UKJENT,
}

enum class Endringstype {
    Opprettelse,
    Endring,
    Sletting,

    @JsonEnumDefaultValue
    UKJENT,
}

data class ArbeidsforholdKafka(
    val navArbeidsforholdId: String,
    val arbeidstaker: Arbeidstaker,
)

data class Arbeidstaker(
    val identer: List<Ident>,
) {
    fun getFnr(): String {
        return identer.first { it.type == IdentType.FOLKEREGISTERIDENT && it.gjeldende }.ident
    }
}

data class Ident(
    val type: IdentType,
    val ident: String,
    val gjeldende: Boolean,
)

enum class IdentType {
    FOLKEREGISTERIDENT,
    ORGANISASJONSNUMMER,
    AKTORID,

    @JsonEnumDefaultValue
    UKJENT,
}
