package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon.*
import no.nav.helse.flex.sykmeldinghendelse.FiskerBlad
import no.nav.helse.flex.sykmeldinghendelse.FiskerBrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.FiskerLottOgHyre
import no.nav.helse.flex.sykmeldinghendelse.FrilanserBrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.JordbrukerBrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.NaringsdrivendeBrukerSvar
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import java.time.LocalDate

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

        if (sykmeldingerMedSammeVentetid.isEmpty()) {
            throw RuntimeException(
                "Fant ingen sykmeldinger med samme ventetid for ${sykmelding.sykmeldingId}, selv ikke sykmeldingen selv. Kanskje feil i flex-syketilfelle?",
            )
        }

        val tidligsteSykmelding =
            sykmeldingLeser
                .hentAlleSykmeldingerFraIderFom(sykmeldingerMedSammeVentetid, sykmelding.fom.minusDays(VENTETID_ANTALL_DAGER))
                .filter { it.tilsvarendeVentetidForArbeidssituasjon(arbeidssituasjon) }
                .minByOrNull { it.fom }

        val erForsteSykmelding = tidligsteSykmelding == null || sykmelding.fom <= tidligsteSykmelding.fom
        logger.info(
            "Sykmelding ${sykmelding.sykmeldingId} arbeidssituasjon $arbeidssituasjon er forst: $erForsteSykmelding tidligste eksisterende ${tidligsteSykmelding?.fom}",
        )
        return erForsteSykmelding
    }

    fun finnTidligsteFomForMeldingTilNavDager(
        sykmelding: Sykmelding,
        arbeidssituasjon: Arbeidssituasjon,
        identer: PersonIdenter,
    ): LocalDate? {
        val dagenEtterForrigeSykmeldingSinSisteTom =
            sykmeldingLeser
                .hentAlleSykmeldingerFraIdenterFom(
                    identer = identer,
                    fom = sykmelding.fom.minusDays(VENTETID_ANTALL_DAGER),
                ).filter { it.tom < sykmelding.fom }
                .filter { it.tilsvarendeVentetidForArbeidssituasjon(arbeidssituasjon) }
                .filter { it.sisteHendelse().status == HendelseStatus.SENDT_TIL_NAV }
                .maxByOrNull { it.tom }
                ?.tom
                ?.plusDays(1)

        return if (dagenEtterForrigeSykmeldingSinSisteTom != null) {
            logger.info(
                "Tidligste fom $dagenEtterForrigeSykmeldingSinSisteTom vs ${sykmelding.fom} for sykmelding ${sykmelding.sykmeldingId}",
            )
            dagenEtterForrigeSykmeldingSinSisteTom
        } else {
            logger.info("Tidligste fom ikke funnet ${sykmelding.fom} for sykmelding ${sykmelding.sykmeldingId}")
            null
        }
    }
}

fun Sykmelding.tilsvarendeVentetidForArbeidssituasjon(arbeidssituasjon: Arbeidssituasjon): Boolean =
    this.sisteHendelse().brukerSvar.let { brukerSvar ->
        when (arbeidssituasjon) {
            FISKER,
            NAERINGSDRIVENDE,
            JORDBRUKER,
            ->
                when (brukerSvar) {
                    is NaringsdrivendeBrukerSvar -> true
                    is JordbrukerBrukerSvar -> true
                    is FiskerBrukerSvar -> brukerSvar.blad.svar == FiskerBlad.A && brukerSvar.lottOgHyre.svar == FiskerLottOgHyre.LOTT
                    else -> false
                }

            FRILANSER -> brukerSvar is FrilanserBrukerSvar
            ARBEIDSTAKER,
            ARBEIDSLEDIG,
            PERMITTERT,
            ANNET,
            -> throw IllegalArgumentException("Ventetid er ikke relevant for Arbeidssituasjon $arbeidssituasjon")
        }
    }
