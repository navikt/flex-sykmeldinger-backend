package no.nav.helse.flex.sykmelding.tsm

import java.time.LocalDate

data class MedisinskVurdering(
    val hovedDiagnose: DiagnoseInfo?,
    val biDiagnoser: List<DiagnoseInfo>?,
    val svangerskap: Boolean,
    val yrkesskade: Yrkesskade?,
    val skjermetForPasient: Boolean,
    val syketilfelletStartDato: LocalDate?,
    val annenFraversArsak: AnnenFraverArsak?,
    val annenFravarsgrunn: AnnenFravarArsakType?, // For digital sykmelding
)

data class DiagnoseInfo(
    val system: DiagnoseSystem,
    val kode: String,
    val tekst: String?,
)

enum class DiagnoseSystem {
    ICPC2,
    ICD10,
    ICPC2B,
    PHBU,
    UGYLDIG,
}

data class Yrkesskade(
    val yrkesskadeDato: LocalDate?,
)

data class MedisinskArsak(
    val beskrivelse: String?,
    val arsak: List<MedisinskArsakType>,
)

enum class MedisinskArsakType {
    TILSTAND_HINDRER_AKTIVITET,
    AKTIVITET_FORVERRER_TILSTAND,
    AKTIVITET_FORHINDRER_BEDRING,
    ANNET,
}

data class ArbeidsrelatertArsak(
    val beskrivelse: String?,
    val arsak: List<ArbeidsrelatertArsakType>,
)

enum class ArbeidsrelatertArsakType {
    MANGLENDE_TILRETTELEGGING,
    ANNET,
}

data class AnnenFraverArsak(
    val beskrivelse: String?,
    val arsak: List<AnnenFravarArsakType>?,
)

enum class AnnenFravarArsakType {
    GODKJENT_HELSEINSTITUSJON,
    BEHANDLING_FORHINDRER_ARBEID,
    ARBEIDSRETTET_TILTAK,
    MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND,
    NODVENDIG_KONTROLLUNDENRSOKELSE,
    SMITTEFARE,
    ABORT,
    UFOR_GRUNNET_BARNLOSHET,
    DONOR,
    BEHANDLING_STERILISERING,
}
