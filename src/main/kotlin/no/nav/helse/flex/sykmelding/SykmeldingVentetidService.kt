package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon.*
import no.nav.helse.flex.sykmeldinghendelse.FiskerBlad
import no.nav.helse.flex.sykmeldinghendelse.FiskerBrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.FiskerLottOgHyre
import no.nav.helse.flex.sykmeldinghendelse.FrilanserBrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.JordbrukerBrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.NaringsdrivendeBrukerSvar
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service

const val VENTETID_ANTALL_DAGER = 16L

@Service
class SykmeldingVentetidService(
    private val sykmeldingLeser: SykmeldingLeser,
    private val syketilfelleClient: SyketilfelleClient,
) {
    private val logger = logger()

    fun erForsteSykmeldingMedSammeVentetidOgArbeidssituasjon(
        sykmelding: Sykmelding,
        arbeidssituasjon: Arbeidssituasjon,
    ): Boolean {
        val sykmeldingerMedSammeVentetid =
            syketilfelleClient
                .getPerioderMedSammeVentetid(sykmelding.sykmeldingId)
                .ventetidPerioder
                .map { it.ressursId }

        val tidligsteSykmelding =
            sykmeldingLeser
                .hentAlleSykmeldingerFraIderFom(sykmeldingerMedSammeVentetid, sykmelding.fom.minusDays(VENTETID_ANTALL_DAGER))
                .filter { it.tilsvarendeVentetidForArbeidssituasjon(arbeidssituasjon) }
                .minByOrNull { it.fom }

        val erForsteSykmelding = if (tidligsteSykmelding != null) sykmelding.fom <= tidligsteSykmelding.fom else true
        logger.info(
            "Sykmelding ${sykmelding.sykmeldingId} arbeidssituasjon $arbeidssituasjon er forst: $erForsteSykmelding tidligste eksisterende ${tidligsteSykmelding?.fom}",
        )
        return erForsteSykmelding
    }

    companion object {
        fun Sykmelding.tilsvarendeVentetidForArbeidssituasjon(arbeidssituasjon: Arbeidssituasjon): Boolean =
            when (arbeidssituasjon) {
                FISKER,
                NAERINGSDRIVENDE,
                ->
                    when (val brukerSvar = this.sisteHendelse().brukerSvar) {
                        is NaringsdrivendeBrukerSvar -> true
                        is FiskerBrukerSvar -> brukerSvar.blad.svar == FiskerBlad.A && brukerSvar.lottOgHyre.svar == FiskerLottOgHyre.LOTT
                        else -> false
                    }
                FRILANSER -> this.sisteHendelse().brukerSvar is FrilanserBrukerSvar
                JORDBRUKER -> this.sisteHendelse().brukerSvar is JordbrukerBrukerSvar
                ARBEIDSTAKER,
                ARBEIDSLEDIG,
                PERMITTERT,
                ANNET,
                -> throw IllegalArgumentException("Ventetid er ikke relevant for Arbeidssituasjon $arbeidssituasjon")
            }
    }
}
