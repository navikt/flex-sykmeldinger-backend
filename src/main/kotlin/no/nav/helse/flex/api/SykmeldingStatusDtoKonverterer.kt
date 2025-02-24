package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.api.dto.ArbeidsgiverStatusDTO
import no.nav.helse.flex.api.dto.SykmeldingStatusDTO
import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.SporsmalTag
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.utils.objectMapper
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class SykmeldingStatusDtoKonverterer {
    fun konverterSykmeldingStatus(status: SykmeldingHendelse): SykmeldingStatusDTO =
        SykmeldingStatusDTO(
            // TODO
            statusEvent = status.status.name,
            timestamp = status.opprettet.atOffset(ZoneOffset.UTC),
            sporsmalOgSvarListe = emptyList(),
            arbeidsgiver =
                status.arbeidstakerInfo?.arbeidsgiver?.let { arbeidsgiver ->
                    ArbeidsgiverStatusDTO(
                        orgnummer = arbeidsgiver.orgnummer,
                        juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
                        orgNavn = arbeidsgiver.orgnavn,
                    )
                },
            brukerSvar = status.sporsmalSvar?.let { konverterSykmeldingSporsmal(it) },
        )

    internal fun konverterSykmeldingSporsmal(sporsmal: List<Sporsmal>): no.nav.helse.flex.api.dto.SykmeldingSporsmalSvarDto {
        fun hentSporsmal(
            sporsmal: List<Sporsmal>,
            tag: SporsmalTag,
        ): Sporsmal? = sporsmal.find { it.tag == tag }

        fun <T> Sporsmal.tilSvar(mapper: (verdi: String) -> T): no.nav.helse.flex.api.dto.FormSporsmalSvar<T>? {
            val forsteSvarVerdi: String? = this.forsteSvarVerdi
            return if (forsteSvarVerdi == null) {
                null
            } else {
                _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                    sporsmaltekst = this.sporsmalstekst ?: "",
                    svar = mapper(forsteSvarVerdi),
                )
            }
        }

        fun Sporsmal.tilJaNeiSvar(): no.nav.helse.flex.api.dto.FormSporsmalSvar<no.nav.helse.flex.api.dto.JaEllerNei>? =
            this.tilSvar {
                enumValueOf(it)
            }

        fun <T> Sporsmal.tilSvarListe(mapper: (verdi: String) -> T): no.nav.helse.flex.api.dto.FormSporsmalSvar<List<T>>? =
            if (svar.isEmpty()) {
                null
            } else {
                _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                    sporsmaltekst = this.sporsmalstekst ?: "",
                    svar = svar.map { mapper(it.verdi) },
                )
            }

        return _root_ide_package_.no.nav.helse.flex.api.dto.SykmeldingSporsmalSvarDto(
            erOpplysningeneRiktige =
                hentSporsmal(sporsmal, SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE)?.tilJaNeiSvar()
                    ?: error("ER_OPPLYSNINGENE_RIKTIGE må ha svar"),
            uriktigeOpplysninger =
                hentSporsmal(
                    sporsmal,
                    SporsmalTag.URIKTIGE_OPPLYSNINGER,
                )?.tilSvarListe { enumValueOf<no.nav.helse.flex.api.dto.UriktigeOpplysningerType>(it) },
            arbeidssituasjon =
                hentSporsmal(sporsmal, SporsmalTag.ARBEIDSSITUASJON)?.tilSvar { enumValueOf(it) }
                    ?: error("ARBEIDSSITUASJON må ha svar"),
            arbeidsgiverOrgnummer = hentSporsmal(sporsmal, SporsmalTag.ARBEIDSGIVER_ORGNUMMER)?.tilSvar { it },
            arbeidsledig =
                hentSporsmal(sporsmal, SporsmalTag.ARBEIDSLEDIG_FRA_ORGNUMMER)?.let { sp ->
                    _root_ide_package_.no.nav.helse.flex.api.dto.ArbeidsledigFraOrgnummer(
                        arbeidsledigFraOrgnummer = sp.tilSvar { it },
                    )
                },
            riktigNarmesteLeder = hentSporsmal(sporsmal, SporsmalTag.RIKTIG_NARMESTE_LEDER)?.tilJaNeiSvar(),
            harBruktEgenmelding = hentSporsmal(sporsmal, SporsmalTag.HAR_BRUKT_EGENMELDING)?.tilJaNeiSvar(),
            egenmeldingsperioder =
                hentSporsmal(sporsmal, SporsmalTag.EGENMELDINGSPERIODER)?.tilSvarListe {
                    val periode: Periode = objectMapper.readValue(it)
                    _root_ide_package_.no.nav.helse.flex.api.dto.Egenmeldingsperiode(
                        fom = periode.fom,
                        tom = periode.tom,
                    )
                },
            harForsikring = hentSporsmal(sporsmal, SporsmalTag.HAR_FORSIKRING)?.tilJaNeiSvar(),
            egenmeldingsdager =
                hentSporsmal(
                    sporsmal,
                    SporsmalTag.EGENMELINGSDAGER,
                )?.tilSvarListe { LocalDate.parse(it) },
            harBruktEgenmeldingsdager = hentSporsmal(sporsmal, SporsmalTag.HAR_BRUKT_EGENMELINGSDAGER)?.tilJaNeiSvar(),
            fisker =
                hentSporsmal(sporsmal, SporsmalTag.FISKER)?.let { sp ->
                    _root_ide_package_.no.nav.helse.flex.api.dto.FiskerSvar(
                        blad =
                            hentSporsmal(sp.undersporsmal, SporsmalTag.FISKER__BLAD)?.tilSvar { enumValueOf(it) }
                                ?: error("FISKER__BLAD må ha svar"),
                        lottOgHyre =
                            hentSporsmal(
                                sp.undersporsmal,
                                SporsmalTag.FISKER__LOTT_OG_HYRE,
                            )?.tilSvar { enumValueOf(it) }
                                ?: error("FISKER__LOTT_OG_HYRE må ha svar"),
                    )
                },
        )
    }

    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
