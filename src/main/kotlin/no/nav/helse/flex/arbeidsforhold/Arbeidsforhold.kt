package no.nav.helse.flex.arbeidsforhold

import org.springframework.data.annotation.Id
import java.time.Instant
import java.time.LocalDate

data class Arbeidsforhold(
    @Id
    val id: String? = null,
    val fnr: String,
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val orgnavn: String,
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val arbeidsforholdType: ArbeidsforholdType?,
    val opprettet: Instant = Instant.now(),
)

enum class ArbeidsforholdType {
    FORENKLET_OPPGJOERSORDNING,
    FRILANSER_OPPDRAGSTAKER_HONORAR_PERSONER_MM,
    MARITIMT_ARBEIDSFORHOLD,
    ORDINAERT_ARBEIDSFORHOLD,
    PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD,
}
