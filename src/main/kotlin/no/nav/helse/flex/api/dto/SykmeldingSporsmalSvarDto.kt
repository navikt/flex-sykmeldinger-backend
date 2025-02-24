package no.nav.helse.flex.api.dto

import java.time.LocalDate

data class SykmeldingSporsmalSvarDto(
    val erOpplysningeneRiktige:
        _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<_root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei>,
    val uriktigeOpplysninger:
        _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<List<_root_ide_package_.no.nav.helse.flex.api.dto.UriktigeOpplysningerType>>?,
    val arbeidssituasjon:
        _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<_root_ide_package_.no.nav.helse.flex.api.dto.Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<String>?,
    val arbeidsledig: _root_ide_package_.no.nav.helse.flex.api.dto.ArbeidsledigFraOrgnummer?,
    val riktigNarmesteLeder:
        _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<_root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei>?,
    val harBruktEgenmelding:
        _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<_root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei>?,
    val egenmeldingsperioder:
        _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<List<_root_ide_package_.no.nav.helse.flex.api.dto.Egenmeldingsperiode>>?,
    val harForsikring:
        _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<_root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei>?,
    val egenmeldingsdager: _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<List<LocalDate>>?,
    val harBruktEgenmeldingsdager:
        _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<_root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei>?,
    val fisker: _root_ide_package_.no.nav.helse.flex.api.dto.FiskerSvar?,
)

data class FormSporsmalSvar<T>(
    val sporsmaltekst: String,
    val svar: T,
)

data class ArbeidsledigFraOrgnummer(
    val arbeidsledigFraOrgnummer: _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<String>?,
)

data class FiskerSvar(
    val blad: _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<_root_ide_package_.no.nav.helse.flex.api.dto.Blad>,
    val lottOgHyre: _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar<_root_ide_package_.no.nav.helse.flex.api.dto.LottOgHyre>,
)

enum class Blad {
    A,
    B,
}

enum class LottOgHyre {
    LOTT,
    HYRE,
    BEGGE,
}

data class Egenmeldingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)

enum class JaEllerNei {
    JA,
    NEI,
}

enum class UriktigeOpplysningerType {
    PERIODE,
    SYKMELDINGSGRAD_FOR_HOY,
    SYKMELDINGSGRAD_FOR_LAV,
    ARBEIDSGIVER,
    DIAGNOSE,
    ANDRE_OPPLYSNINGER,
}

enum class Arbeidssituasjon {
    ARBEIDSTAKER,
    FRILANSER,
    NAERINGSDRIVENDE,
    FISKER,
    JORDBRUKER,
    ARBEIDSLEDIG,
    ANNET,
}
