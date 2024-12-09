package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

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
}

enum class Endringstype {
    Opprettelse,
    Endring,
    Sletting,
}

data class ArbeidsforholdKafka(
    val navArbeidsforholdId: Int,
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
}
