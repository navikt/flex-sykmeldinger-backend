package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.producers.sykmelding.SykmeldingProducer
import no.nav.helse.flex.sykmelding.SykmeldingErIkkeDinException
import no.nav.helse.flex.sykmelding.SykmeldingIkkeFunnetException
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingHandterer(
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusEndrer: SykmeldingStatusEndrer,
    private val sykmeldingProducer: SykmeldingProducer,
) {
    private val logger = logger()

    fun hentSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
    ): Sykmelding = finnValidertSykmelding(sykmeldingId, identer)

    fun hentAlleSykmeldinger(identer: PersonIdenter): List<Sykmelding> {
        val sykmeldinger = sykmeldingRepository.findAllByPersonIdenter(identer)
        return sykmeldinger
    }

    @Transactional
    fun sendSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
        arbeidssituasjonBrukerInfo: ArbeidssituasjonBrukerInfo,
        sporsmalSvar: List<Sporsmal>?,
    ): Sykmelding {
        val sykmelding = finnValidertSykmelding(sykmeldingId, identer)

        val oppdatertSykmelding =
            when (arbeidssituasjonBrukerInfo) {
                is ArbeidstakerBrukerInfo -> {
                    sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                        sykmelding = sykmelding,
                        identer = identer,
                        arbeidsgiverOrgnummer = arbeidssituasjonBrukerInfo.arbeidsgiverOrgnummer,
                        sporsmalSvar = sporsmalSvar,
                    )
                }
                is ArbeidsledigBrukerInfo -> {
                    sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
                        sykmelding = sykmelding,
                        identer = identer,
                        arbeidsledigFraOrgnummer = arbeidssituasjonBrukerInfo.arbeidsledigFraOrgnummer,
                        sporsmalSvar = sporsmalSvar,
                    )
                }
                is PermittertBrukerInfo -> {
                    sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
                        sykmelding = sykmelding,
                        identer = identer,
                        arbeidsledigFraOrgnummer = arbeidssituasjonBrukerInfo.arbeidsledigFraOrgnummer,
                        sporsmalSvar = sporsmalSvar,
                    )
                }
                is FiskerBrukerInfo -> {
                    when (arbeidssituasjonBrukerInfo.lottOgHyre) {
                        FiskerLottOgHyre.HYRE,
                        FiskerLottOgHyre.BEGGE,
                        -> {
                            requireNotNull(
                                arbeidssituasjonBrukerInfo.arbeidsgiverOrgnummer,
                            ) { "arbeidsgiverOrgnummer må være satt dersom fisker med LOTT" }

                            sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                                sykmelding = sykmelding,
                                identer = identer,
                                arbeidsgiverOrgnummer = arbeidssituasjonBrukerInfo.arbeidsgiverOrgnummer,
                                sporsmalSvar = sporsmalSvar,
                            )
                        }
                        FiskerLottOgHyre.LOTT -> {
                            sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
                                sykmelding = sykmelding,
                                identer = identer,
                                arbeidsledigFraOrgnummer = null,
                                sporsmalSvar = sporsmalSvar,
                            )
                        }
                    }
                }
                is FrilanserBrukerInfo,
                is JordbrukerBrukerInfo,
                is NaringsdrivendeBrukerInfo,
                is AnnetArbeidssituasjonBrukerInfo,
                -> {
                    sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
                        sykmelding = sykmelding,
                        identer = identer,
                        arbeidsledigFraOrgnummer = null,
                        sporsmalSvar = sporsmalSvar,
                    )
                }
            }

        val lagretSykmelding = sykmeldingRepository.save(oppdatertSykmelding)
        sendSykmeldingKafka(lagretSykmelding)
        return lagretSykmelding
    }

    @Transactional
    fun avbrytSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
    ): Sykmelding {
        val sykmelding = finnValidertSykmelding(sykmeldingId, identer)

        val oppdatertSykmelding = sykmeldingStatusEndrer.endreStatusTilAvbrutt(sykmelding = sykmelding)

        val lagretSykmelding = sykmeldingRepository.save(oppdatertSykmelding)
        sendSykmeldingKafka(lagretSykmelding)
        return lagretSykmelding
    }

    @Transactional
    fun bekreftAvvistSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
    ): Sykmelding {
        val sykmelding = finnValidertSykmelding(sykmeldingId, identer)

        val oppdatertSykmelding = sykmeldingStatusEndrer.endreStatusTilBekreftetAvvist(sykmelding = sykmelding)

        val lagretSykmelding = sykmeldingRepository.save(oppdatertSykmelding)
        sendSykmeldingKafka(lagretSykmelding)
        return lagretSykmelding
    }

    private fun sendSykmeldingKafka(sykmelding: Sykmelding) {
        sykmeldingProducer.sendSykmelding(sykmelding)
    }

    private fun finnValidertSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
    ): Sykmelding {
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            logger.warn("Fant ikke sykmeldingen")
            throw SykmeldingIkkeFunnetException("Fant ikke sykmelding med id $sykmeldingId")
        }
        if (sykmelding.pasientFnr !in identer.alle()) {
            logger.warn("Person har ikke tilgang til sykmelding")
            throw SykmeldingErIkkeDinException("Person har ikke tilgang til sykmelding")
        }
        return sykmelding
    }
}
