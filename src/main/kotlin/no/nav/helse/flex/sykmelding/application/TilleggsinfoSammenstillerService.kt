package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.virksomhet.VirksomhetHenterService
import org.springframework.stereotype.Component

@Component
class TilleggsinfoSammenstillerService(
    private val virksomhetHenterService: VirksomhetHenterService,
) {
    fun sammenstillTilleggsinfo(
        identer: PersonIdenter,
        sporsmal: List<Sporsmal>,
        sykmelding: Sykmelding,
    ): Tilleggsinfo? {
        val arbeidssituasjonString = sporsmal.findWithMal(SporsmalMaler.ARBEIDSSITUASJON).svar()
        val arbeidssituasjon = enumValueOf<Arbeidssituasjon>(arbeidssituasjonString)

        return when (arbeidssituasjon) {
            Arbeidssituasjon.ARBEIDSTAKER -> {
                return sammenstillArbeidstakerTilleggsinfo(
                    identer = identer,
                    sykmelding = sykmelding,
                    sporsmal = sporsmal,
                )
            }
            Arbeidssituasjon.ARBEIDSLEDIG,
            Arbeidssituasjon.PERMITTERT,
            Arbeidssituasjon.FISKER,
            Arbeidssituasjon.FRILANSER,
            Arbeidssituasjon.JORDBRUKER,
            Arbeidssituasjon.NAERINGSDRIVENDE,
            Arbeidssituasjon.ANNET,
            -> null
        }
    }

    fun sammenstillArbeidstakerTilleggsinfo(
        identer: PersonIdenter,
        sykmelding: Sykmelding,
        sporsmal: List<Sporsmal>,
    ): Tilleggsinfo {
        val arbeidsgiverOrgnummer = sporsmal.findWithMal(SporsmalMaler.ARBEIDSGIVER_ORGNUMMER).svar()
        val arbeidsgiver = finnArbeidsgiver(identer, sykmelding, arbeidsgiverOrgnummer)

        return ArbeidstakerTilleggsinfo(
            arbeidsgiver = arbeidsgiver,
        )
    }

    fun sammenstillFiskerTilleggsinfo() {
//        when (arbeidssituasjonBrukerInfo.lottOgHyre) {
//            FiskerLottOgHyre.HYRE,
//            FiskerLottOgHyre.BEGGE,
//                -> {
//                requireNotNull(
//                    arbeidssituasjonBrukerInfo.arbeidsgiverOrgnummer,
//                ) { "arbeidsgiverOrgnummer må være satt dersom fisker med LOTT" }
//
//                sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
//                    sykmelding = sykmelding,
//                    identer = identer,
//                    arbeidsgiverOrgnummer = arbeidssituasjonBrukerInfo.arbeidsgiverOrgnummer,
//                    sporsmalSvar = sporsmalSvar,
//                )
//            }
//            FiskerLottOgHyre.LOTT -> {
//                sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
//                    sykmelding = sykmelding,
//                    identer = identer,
//                    arbeidsledigFraOrgnummer = null,
//                    sporsmalSvar = sporsmalSvar,
//                )
//            }
//        }
    }

    private fun finnArbeidsgiver(
        identer: PersonIdenter,
        sykmelding: Sykmelding,
        arbeidsgiverOrgnummer: String,
    ): Arbeidsgiver {
        val virksomheter =
            virksomhetHenterService.hentVirksomheterForPersonInnenforPeriode(
                identer = identer,
                periode = sykmelding.fom to sykmelding.tom,
            )
        val valgtArbeidsforhold = virksomheter.find { it.orgnummer == arbeidsgiverOrgnummer }
        if (valgtArbeidsforhold == null) {
            // TODO: Exceptions should be mapped to a HTTP error code
            throw IllegalArgumentException("Fant ikke arbeidsgiver med orgnummer $arbeidsgiverOrgnummer")
        }
        return Arbeidsgiver(
            orgnummer = valgtArbeidsforhold.orgnummer,
            juridiskOrgnummer = valgtArbeidsforhold.juridiskOrgnummer,
            orgnavn = valgtArbeidsforhold.navn,
            erAktivtArbeidsforhold = valgtArbeidsforhold.aktivtArbeidsforhold,
            narmesteLeder =
                valgtArbeidsforhold.naermesteLeder?.let {
                    NarmesteLeder(
                        navn =
                            it.narmesteLederNavn
                                // TODO: Exceptions should be mapped to a HTTP error code
                                ?: throw IllegalArgumentException("Mangler narmeste leder navn"),
                    )
                },
        )
    }
}

private fun Iterable<Sporsmal>.findByTag(tag: SporsmalTag): Sporsmal =
    find { it.tag == tag }
        ?: throw IllegalArgumentException("Mangler $tag spørsmål")
