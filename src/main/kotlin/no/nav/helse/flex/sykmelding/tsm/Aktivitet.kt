package no.nav.helse.flex.sykmelding.tsm

import java.time.LocalDate

enum class AktivitetType {
    AKTIVITET_IKKE_MULIG,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    GRADERT,
    REISETILSKUDD,
}

sealed interface Aktivitet {
    val fom: LocalDate
    val tom: LocalDate
    val type: AktivitetType
}

data class Behandlingsdager(
    val antallBehandlingsdager: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Aktivitet {
    override val type = AktivitetType.BEHANDLINGSDAGER
}

data class Gradert(
    val grad: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisetilskudd: Boolean,
) : Aktivitet {
    override val type = AktivitetType.GRADERT
}

data class Reisetilskudd(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Aktivitet {
    override val type = AktivitetType.REISETILSKUDD
}

data class Avventende(
    val innspillTilArbeidsgiver: String,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Aktivitet {
    override val type = AktivitetType.AVVENTENDE
}

data class AktivitetIkkeMulig(
    val medisinskArsak: MedisinskArsak?,
    val arbeidsrelatertArsak: ArbeidsrelatertArsak?,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Aktivitet {
    override val type = AktivitetType.AKTIVITET_IKKE_MULIG
}
