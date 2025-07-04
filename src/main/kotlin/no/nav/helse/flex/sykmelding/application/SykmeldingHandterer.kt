package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.SykmeldingErIkkeDinException
import no.nav.helse.flex.sykmelding.SykmeldingIkkeFunnetException
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingStatusHandterer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Service
class SykmeldingHandterer(
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusEndrer: SykmeldingStatusEndrer,
    private val tilleggsinfoSammenstillerService: TilleggsinfoSammenstillerService,
    private val nowFactory: Supplier<Instant>,
    private val sykmeldingStatusHandterer: SykmeldingStatusHandterer,
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

    @Transactional(rollbackFor = [Exception::class])
    fun sendSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
        brukerSvar: BrukerSvar,
    ): Sykmelding {
        val sykmelding = finnValidertSykmelding(sykmeldingId, identer)
        val tilleggsinfo =
            tilleggsinfoSammenstillerService.sammenstillTilleggsinfo(
                identer = identer,
                brukerSvar = brukerSvar,
                sykmelding = sykmelding,
            )

        val nyStatus =
            when (brukerSvar) {
                is ArbeidstakerBrukerSvar -> {
                    HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                }
                is FiskerBrukerSvar -> {
                    when (brukerSvar.lottOgHyre.svar) {
                        FiskerLottOgHyre.HYRE,
                        FiskerLottOgHyre.BEGGE,
                        -> {
                            HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                        }
                        FiskerLottOgHyre.LOTT -> {
                            HendelseStatus.SENDT_TIL_NAV
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
                    HendelseStatus.SENDT_TIL_NAV
                }
                is UtdatertFormatBrukerSvar -> throw IllegalArgumentException(
                    "Kan ikke sende sykmelding med bruker svar av type ${brukerSvar.type}",
                )
            }

        sykmeldingStatusEndrer.sjekkStatusEndring(sykmelding = sykmelding, nyStatus = nyStatus)

        val oppdatertSykmelding =
            sjekkStatusOgLeggTilHendelse(sykmelding = sykmelding, status = nyStatus, brukerSvar = brukerSvar, tilleggsinfo = tilleggsinfo)

        val lagretSykmelding = sykmeldingRepository.save(oppdatertSykmelding)
        sykmeldingStatusHandterer.sendSykmeldingStatusPaKafka(lagretSykmelding)
        return lagretSykmelding
    }

    @Transactional(rollbackFor = [Exception::class])
    fun avbrytSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
    ): Sykmelding {
        val sykmelding = finnValidertSykmelding(sykmeldingId, identer)
        val oppdatertSykmelding =
            sjekkStatusOgLeggTilHendelse(sykmelding = sykmelding, status = HendelseStatus.AVBRUTT)

        val lagretSykmelding = sykmeldingRepository.save(oppdatertSykmelding)
        sykmeldingStatusHandterer.sendSykmeldingStatusPaKafka(lagretSykmelding)
        return lagretSykmelding
    }

    @Transactional(rollbackFor = [Exception::class])
    fun bekreftAvvistSykmelding(
        sykmeldingId: String,
        identer: PersonIdenter,
    ): Sykmelding {
        val sykmelding = finnValidertSykmelding(sykmeldingId, identer)

        val oppdatertSykmelding =
            sjekkStatusOgLeggTilHendelse(sykmelding = sykmelding, status = HendelseStatus.BEKREFTET_AVVIST)

        val lagretSykmelding = sykmeldingRepository.save(oppdatertSykmelding)
        sykmeldingStatusHandterer.sendSykmeldingStatusPaKafka(lagretSykmelding)
        return lagretSykmelding
    }

    private fun sjekkStatusOgLeggTilHendelse(
        sykmelding: Sykmelding,
        status: HendelseStatus,
        brukerSvar: BrukerSvar? = null,
        tilleggsinfo: Tilleggsinfo? = null,
    ): Sykmelding {
        sykmeldingStatusEndrer.sjekkStatusEndring(sykmelding = sykmelding, nyStatus = status)
        val now = nowFactory.get()
        return sykmelding.leggTilHendelse(
            SykmeldingHendelse(
                status = status,
                brukerSvar = brukerSvar,
                tilleggsinfo = tilleggsinfo,
                source = SykmeldingHendelse.LOKAL_SOURCE,
                hendelseOpprettet = now,
                lokaltOpprettet = now,
            ).also {
                logger.info("Legger til hendelse ${it.status} på sykmelding ${sykmelding.sykmeldingId}")
            },
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
