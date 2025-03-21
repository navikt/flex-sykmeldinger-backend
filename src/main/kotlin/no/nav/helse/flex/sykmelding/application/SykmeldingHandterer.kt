package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaDTOKonverterer
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusProducer
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
    private val sykmeldingStatusProducer: SykmeldingStatusProducer,
    private val tilleggsinfoSammenstillerService: TilleggsinfoSammenstillerService,
) {
    private val logger = logger()
    private val sykmeldingStatusKafkaDTOKonverterer = SykmeldingStatusKafkaDTOKonverterer()

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
        brukerSvar: BrukerSvar,
        sporsmalSvar: List<Sporsmal>?,
    ): Sykmelding {
        val sykmelding = finnValidertSykmelding(sykmeldingId, identer)
        val tilleggsinfo =
            tilleggsinfoSammenstillerService.sammenstillTilleggsinfo(
                identer = identer,
                brukerSvar = brukerSvar,
                sykmelding = sykmelding,
            )

        val oppdatertSykmelding =
            when (brukerSvar) {
                is ArbeidstakerBrukerSvar -> {
                    sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                        sykmelding = sykmelding,
                        identer = identer,
                        arbeidsgiverOrgnummer = brukerSvar.arbeidsgiverOrgnummer.svar,
                        sporsmalSvar = sporsmalSvar,
                    )
                }
                is FiskerBrukerSvar -> {
                    when (brukerSvar.lottOgHyre.svar) {
                        FiskerLottOgHyre.HYRE,
                        FiskerLottOgHyre.BEGGE,
                        -> {
                            val arbeidstaker = brukerSvar.somArbeidstaker()

                            sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                                sykmelding = sykmelding,
                                identer = identer,
                                arbeidsgiverOrgnummer = arbeidstaker.arbeidsgiverOrgnummer.svar,
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
                is ArbeidsledigBrukerSvar,
                is PermittertBrukerSvar,
                is FrilanserBrukerSvar,
                is JordbrukerBrukerSvar,
                is NaringsdrivendeBrukerSvar,
                is AnnetArbeidssituasjonBrukerSvar,
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
        sykmeldingStatusProducer.produserSykmeldingStatus(
            fnr = sykmelding.pasientFnr,
            sykmelingstatusDTO = sykmeldingStatusKafkaDTOKonverterer.konverter(sykmelding),
        )
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
