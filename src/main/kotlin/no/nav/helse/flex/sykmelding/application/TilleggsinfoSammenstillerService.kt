package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.arbeidsgiverdetaljer.ArbeidsgiverDetaljerService
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.tidligereArbeidsgivere.TidligereArbeidsgivereHandterer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Component

@Component
class TilleggsinfoSammenstillerService(
    private val arbeidsgiverDetaljerService: ArbeidsgiverDetaljerService,
    private val sykmeldingRepository: ISykmeldingRepository,
) {
    val log = logger()

    fun sammenstillTilleggsinfo(
        identer: PersonIdenter,
        sykmelding: Sykmelding,
        brukerSvar: BrukerSvar,
    ): Tilleggsinfo =
        when (brukerSvar) {
            is ArbeidstakerBrukerSvar -> {
                sammenstillArbeidstakerTilleggsinfo(
                    identer = identer,
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            }
            is ArbeidsledigBrukerSvar -> {
                sammenstillArbeidsledigTilleggsinfo(
                    identer = identer,
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            }
            is PermittertBrukerSvar -> {
                sammenstillPermittertTilleggsinfo(
                    identer = identer,
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            }
            is FiskerBrukerSvar -> {
                sammenstillFiskerTilleggsinfo(
                    identer = identer,
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            }
            is FrilanserBrukerSvar -> FrilanserTilleggsinfo
            is JordbrukerBrukerSvar -> JordbrukerTilleggsinfo
            is NaringsdrivendeBrukerSvar -> NaringsdrivendeTilleggsinfo
            is AnnetArbeidssituasjonBrukerSvar -> AnnetArbeidssituasjonTilleggsinfo
            is UtdatertFormatBrukerSvar -> throw IllegalArgumentException(
                "Kan ikke sammenstille tilleggsinfo for bruker svar av type: ${brukerSvar.type}",
            )
        }

    fun sammenstillArbeidsledigTilleggsinfo(
        identer: PersonIdenter,
        sykmelding: Sykmelding,
        brukerSvar: ArbeidsledigBrukerSvar,
    ): ArbeidsledigTilleggsinfo {
        val valgtArbeidsledigFraOrgnummer = brukerSvar.arbeidsledigFraOrgnummer?.svar ?: return ArbeidsledigTilleggsinfo()

        val valgtTidligereArbeidsgiver =
            finnValgtTidligereArbeidsgiver(
                valgtArbeidsledigFraOrgnummer = valgtArbeidsledigFraOrgnummer,
                identer = identer,
                sykmelding = sykmelding,
            )

        return ArbeidsledigTilleggsinfo(
            tidligereArbeidsgiver = valgtTidligereArbeidsgiver,
        )
    }

    fun sammenstillPermittertTilleggsinfo(
        identer: PersonIdenter,
        sykmelding: Sykmelding,
        brukerSvar: PermittertBrukerSvar,
    ): PermittertTilleggsinfo {
        val valgtArbeidsledigFraOrgnummer = brukerSvar.arbeidsledigFraOrgnummer?.svar ?: return PermittertTilleggsinfo()

        val valgtTidligereArbeidsgiver =
            finnValgtTidligereArbeidsgiver(
                valgtArbeidsledigFraOrgnummer = valgtArbeidsledigFraOrgnummer,
                identer = identer,
                sykmelding = sykmelding,
            )

        return PermittertTilleggsinfo(
            tidligereArbeidsgiver = valgtTidligereArbeidsgiver,
        )
    }

    private fun finnValgtTidligereArbeidsgiver(
        valgtArbeidsledigFraOrgnummer: String,
        identer: PersonIdenter,
        sykmelding: Sykmelding,
    ): TidligereArbeidsgiver {
        val tidligereArbeidsgivere =
            TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(
                alleSykmeldinger = sykmeldingRepository.findAllByPersonIdenter(identer),
                gjeldendeSykmeldingId = sykmelding.sykmeldingId,
            )
        return tidligereArbeidsgivere.find { it.orgnummer == valgtArbeidsledigFraOrgnummer }
            ?: throw KunneIkkeFinneTilleggsinfoException(
                "Fant ikke tidligere arbeidsgiver med orgnummer fra bruker svar: '$valgtArbeidsledigFraOrgnummer' " +
                    "for sykmeldingId ${sykmelding.sykmeldingId}",
            ).also {
                log.error(it.message)
            }
    }

    fun sammenstillArbeidstakerTilleggsinfo(
        identer: PersonIdenter,
        sykmelding: Sykmelding,
        brukerSvar: ArbeidstakerBrukerSvar,
    ): Tilleggsinfo {
        val arbeidsgiverOrgnummer = brukerSvar.arbeidsgiverOrgnummer.svar
        val arbeidsgiver =
            finnArbeidsgiver(
                identer = identer,
                sykmelding = sykmelding,
                arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
            )
        return ArbeidstakerTilleggsinfo(
            arbeidsgiver = arbeidsgiver,
        )
    }

    fun sammenstillFiskerTilleggsinfo(
        identer: PersonIdenter,
        sykmelding: Sykmelding,
        brukerSvar: FiskerBrukerSvar,
    ): FiskerTilleggsinfo {
        when (brukerSvar.lottOgHyre.svar) {
            FiskerLottOgHyre.HYRE,
            FiskerLottOgHyre.BEGGE,
            -> {
                requireNotNull(
                    brukerSvar.arbeidsgiverOrgnummer,
                ) { "arbeidsgiverOrgnummer må være satt dersom fisker med LOTT, for sykmelding: ${sykmelding.sykmeldingId}" }

                val arbeidsgiver =
                    finnArbeidsgiver(
                        identer = identer,
                        sykmelding = sykmelding,
                        arbeidsgiverOrgnummer = brukerSvar.arbeidsgiverOrgnummer.svar,
                    )
                return FiskerTilleggsinfo(
                    arbeidsgiver = arbeidsgiver,
                )
            }
            FiskerLottOgHyre.LOTT -> {
                return FiskerTilleggsinfo()
            }
        }
    }

    private fun finnArbeidsgiver(
        identer: PersonIdenter,
        sykmelding: Sykmelding,
        arbeidsgiverOrgnummer: String,
    ): Arbeidsgiver {
        val arbeidsgiverDetaljer =
            arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(
                identer = identer,
                periode = sykmelding.fom to sykmelding.tom,
            )
        val valgtArbeidsforhold = arbeidsgiverDetaljer.find { it.orgnummer == arbeidsgiverOrgnummer }
        if (valgtArbeidsforhold == null) {
            throw KunneIkkeFinneTilleggsinfoException(
                "Fant ikke arbeidsgiver med orgnummer $arbeidsgiverOrgnummer" +
                    "for sykmeldingId ${sykmelding.sykmeldingId}",
            ).also {
                log.error(it.message)
            }
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
                                // TODO: Håndter nærmeste leder som ikke har navn
                                ?: throw IllegalArgumentException("Mangler narmeste leder navn"),
                    )
                },
        )
    }
}
