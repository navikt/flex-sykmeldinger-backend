package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.SykmeldingErIkkeDinException
import no.nav.helse.flex.sykmelding.SykmeldingIkkeFunnetException
import no.nav.helse.flex.sykmelding.SykmeldingStatusEndrer
import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingHandterer(
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusEndrer: SykmeldingStatusEndrer,
) {
    private val logger = logger()

    fun hentSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
    ): Sykmelding {
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            logger.warn("Fant ikke sykmelding med id $sykmeldingId")
            throw SykmeldingIkkeFunnetException("Fant ikke sykmelding med id $sykmeldingId")
        }
        if (sykmelding.pasientFnr !in identer.alle()) {
            logger.warn("Person har ikke tilgang til sykmelding")
            throw SykmeldingErIkkeDinException("Person har ikke tilgang til sykmelding")
        }

        return sykmelding
    }

    fun hentAlleSykmeldinger(identer: PersonIdenter): List<Sykmelding> = emptyList()

    @Transactional
    fun sendSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
        arbeidsgiverOrgnummer: String?,
        sporsmalSvar: List<Sporsmal>?,
    ): Sykmelding {
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            logger.warn("Fant ikke sykmelding med id $sykmeldingId")
            throw SykmeldingIkkeFunnetException("Fant ikke sykmelding med id $sykmeldingId")
        }
        if (sykmelding.pasientFnr !in identer.alle()) {
            logger.warn("Person har ikke tilgang til sykmelding")
            throw SykmeldingErIkkeDinException("Person har ikke tilgang til sykmelding")
        }

        val oppdatertSykmelding =
            sykmeldingStatusEndrer.endreStatusTilSendt(
                sykmelding = sykmelding,
                identer = identer,
                arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                sporsmalSvar = sporsmalSvar,
            )

        val lagretSykmelding = sykmeldingRepository.save(oppdatertSykmelding)
        sendSykmeldingKafka(lagretSykmelding)
        return lagretSykmelding
    }

    private fun sendSykmeldingKafka(sykmelding: Sykmelding) {
        logger.info("Ikke implementert: Sykmelding sendt til kafka")
    }
}
