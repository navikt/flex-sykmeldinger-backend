package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service

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
                .hentAlleSykmeldingerFraIderFom(sykmeldingerMedSammeVentetid, null)
                .filter {
                    it
                        .sisteHendelse()
                        .brukerSvar
                        ?.arbeidssituasjon
                        ?.svar == arbeidssituasjon
                }.minByOrNull { it.fom }

        val erForsteSykmelding = if (tidligsteSykmelding != null) sykmelding.fom <= tidligsteSykmelding.fom else true
        logger.info(
            "Sykmelding ${sykmelding.sykmeldingId} arbeidssituasjon $arbeidssituasjon er forst: $erForsteSykmelding tidligste eksisterende ${tidligsteSykmelding?.fom}",
        )
        return erForsteSykmelding
    }
}
